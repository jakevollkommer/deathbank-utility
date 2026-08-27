package com.deathbankutility;

import com.google.common.base.Strings;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.inject.Provides;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.inject.Inject;
import javax.swing.SwingUtilities;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.GameObject;
import net.runelite.api.GameState;
import net.runelite.api.Item;
import net.runelite.api.ItemContainer;
import net.runelite.api.NPC;
import net.runelite.api.Skill;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.ActorDeath;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.GameObjectDespawned;
import net.runelite.api.events.GameObjectSpawned;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.HitsplatApplied;
import net.runelite.api.events.ItemContainerChanged;
import net.runelite.api.events.NpcDespawned;
import net.runelite.api.events.NpcSpawned;
import net.runelite.api.events.WidgetClosed;
import net.runelite.api.events.WidgetLoaded;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.InventoryID;
import net.runelite.api.gameval.ItemID;
import net.runelite.api.gameval.VarbitID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.Notifier;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.events.RuneScapeProfileChanged;
import net.runelite.client.game.ItemManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.util.LinkBrowser;
import net.runelite.client.util.Text;

@Slf4j
@PluginDescriptor(
	name = "Deathbank Utility",
	description = "Tracks item retrieval services (deathbanks) and warns before you lose what's inside",
	tags = {"jake", "death", "deathbank", "deathpile", "death pile", "bank", "item", "items", "retrieval", "service",
		"gravestone", "grave", "storage", "tracker", "reminder", "alert", "safety", "wipe", "lost",
		"uim", "ultimate ironman", "ironman", "hardcore", "pvm", "boss", "raid",
		"zulrah", "vorkath", "nex", "tob", "toa", "hydra", "nightmare", "hespori", "sepulchre", "mimic", "warning"}
)
public class DeathbankUtilityPlugin extends Plugin
{
	// Server messages (matched on substring after tag stripping); exact wording
	// re-verified in TESTING.md
	private static final String MSG_RETRIEVAL_SERVICE = "items stored in an item retrieval service";
	private static final String MSG_RETRIEVED_SOME = "retrieved some of your items";
	private static final String MSG_DIED_AGAIN = "You have died again";
	private static final String MSG_LOST_ITEMS = "lost the items";

	// Zulrah has no retrieval interface — the priestess returns items through dialog
	private static final String ZULRAH_DIALOG_LEFT_STUFF = "You left some stuff at Zulrah's shrine";
	private static final String ZULRAH_DIALOG_HOLDING_STUFF = "I've got some stuff you left at the shrine";
	private static final String ZULRAH_DIALOG_RETURNED = "I've returned it to you now";
	private static final String ZULRAH_DIALOG_NOTHING = "I don't have anything for you to collect";

	// Deaths here never wipe a deathbank (list from zodaz/item-retrieval-warning, BSD-2)
	private static final Set<Integer> SAFE_DEATH_REGIONS = Set.of(
		7508, 7509, 10322, // Barbarian Assault
		9520, 9620, // Castle Wars
		12889, 13136, 13137, 13138, 13139, 13140, 13141, 13145, 13393, 13394, 13395, 13396, 13397, 13401, // Chambers of Xeric
		12621, 12622, 12623, 13130, 13131, 13133, 13134, 13135, 13386, 13387, 13390,
		13641, 13642, 13643, 13644, 13645, 13646, 13647, 13899, 13900, 14155, 14156, // Clan Wars
		13362, // Emir's Arena
		7499, // Fishing Trawler
		9043, 9806, 10062, // Inferno
		13658, 13659, 13660, 13914, 13915, 13916, 13918, 13919, 13920, 14174, 14175, 14176, 14430, 14431, 14432, // LMS
		13462, 13463, // Mage Training Arena
		9033, // Nightmare Zone
		10536, // Pest Control
		7513, 7514, 7769, 7770, // POH
		8493, 8749, 9005, // Soul Wars
		9551, // Fight Cave
		9552, // Fight Pit
		11854, 11855, 12110, 12111, // Rogues' Den
		12127, 7512, 7768 // Gauntlet (items never enter)
	);

	// Live game messages carry unresolved Jagex formatting macros, e.g.
	// "@mes_hl_red@You have items stored in an item retrieval service...", which
	// Text.removeTags leaves behind. One mid-phrase would break substring matching.
	private static final Pattern MESSAGE_MACRO = Pattern.compile("@[a-zA-Z0-9_]+@");

	private static final String STATE_KEY = "state";
	private static final int LOGIN_RECONCILE_TICKS = 25;
	private static final int DEATH_RESOLVE_MIN_TICKS = 3;
	private static final int DEATH_RESOLVE_TIMEOUT_TICKS = 50;
	private static final int DAMAGE_WARNING_DISPLAY_TICKS = 8;
	// Emptying the bank (discard, or taking the last item) can close the window
	// before the container update lands, so updates stay trusted briefly after close
	private static final int RETRIEVAL_TRUST_GRACE_TICKS = 5;
	// Depth of the widget tree walk used to find the retrieval window's location
	// text; the interface has no TITLE component, so its texts are scanned instead
	private static final int WINDOW_TEXT_DEPTH = 3;

	@Inject
	private Client client;
	@Inject
	private ClientThread clientThread;
	@Inject
	private ConfigManager configManager;
	@Inject
	private Gson gson;
	@Inject
	private ItemManager itemManager;
	@Inject
	private OverlayManager overlayManager;
	@Inject
	private ClientToolbar clientToolbar;
	@Inject
	private Notifier notifier;
	@Inject
	private DeathbankUtilityConfig config;
	@Inject
	private DeathbankIndicatorOverlay indicatorOverlay;
	@Inject
	private DeathbankChestOverlay chestOverlay;
	@Inject
	private DeathbankWarningOverlay warningOverlay;

	@Getter
	private DeathbankState state = DeathbankState.unknown();
	@Getter
	private final List<GameObject> retrievalObjects = new ArrayList<>();
	@Getter
	private final List<NPC> retrievalNpcs = new ArrayList<>();
	@Getter
	private BufferedImage indicatorIcon;

	private DeathbankPanel panel;
	private NavigationButton navButton;
	private boolean retrievalWindowOpen;
	private int retrievalTrustTicksRemaining = -1;
	private boolean graveWindowOpen;
	private int loginReconcileTicksRemaining = -1;
	private int lastDamageWarnTick = -1;
	private int damageWarningUntilTick = -1;

	// A death in IRS content is resolved after respawn by diffing carried items,
	// so the 3 items kept on death never count as banked
	private RetrievalService pendingDeathService;
	private Map<Integer, Integer> pendingDeathSnapshot = Map.of();
	private int pendingDeathTicks;

	@Provides
	DeathbankUtilityConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(DeathbankUtilityConfig.class);
	}

	@Override
	protected void startUp()
	{
		indicatorIcon = itemManager.getImage(ItemID.SKULL);
		panel = new DeathbankPanel();
		navButton = NavigationButton.builder()
			.tooltip("Deathbank Utility")
			.icon(createPanelIcon())
			.priority(7)
			.panel(panel)
			.build();
		clientToolbar.addNavigation(navButton);
		overlayManager.add(indicatorOverlay);
		overlayManager.add(chestOverlay);
		overlayManager.add(warningOverlay);
		loadState();
		updatePanel();
	}

	@Override
	protected void shutDown()
	{
		saveState();
		clientToolbar.removeNavigation(navButton);
		overlayManager.remove(indicatorOverlay);
		overlayManager.remove(chestOverlay);
		overlayManager.remove(warningOverlay);
		retrievalObjects.clear();
		retrievalNpcs.clear();
		retrievalWindowOpen = false;
		graveWindowOpen = false;
		retrievalTrustTicksRemaining = -1;
		loginReconcileTicksRemaining = -1;
		damageWarningUntilTick = -1;
		clearPendingDeath();
	}

	/**
	 * Saved state carried over from a previous session is not asserted until this
	 * session confirms it. A real deathbank announces itself in the login message
	 * within a tick or two; stale state is cleared by the reconcile instead of
	 * being shown as a warning first.
	 */
	boolean isAwaitingLoginConfirmation()
	{
		return loginReconcileTicksRemaining >= 0 && state.getConfidence() == Confidence.UNKNOWN;
	}

	boolean isDamageWarningActive()
	{
		return client.getTickCount() <= damageWarningUntilTick;
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged event)
	{
		if (event.getGameState() == GameState.LOGGING_IN)
		{
			loginReconcileTicksRemaining = LOGIN_RECONCILE_TICKS;
			updatePanel();
		}
		if (event.getGameState() == GameState.LOADING)
		{
			retrievalObjects.clear();
			retrievalNpcs.clear();
		}
	}

	@Subscribe
	public void onRuneScapeProfileChanged(RuneScapeProfileChanged event)
	{
		loadState();
		updatePanel();
	}

	// The config panel cannot host real buttons, so the Feedback "buttons" are checkboxes
	// that act as buttons: any click of the box, tick or untick, opens the link.
	@Subscribe
	public void onConfigChanged(ConfigChanged event)
	{
		if (!DeathbankUtilityConfig.GROUP.equals(event.getGroup()) || event.getNewValue() == null)
		{
			return;
		}

		if ("suggestButton".equals(event.getKey()))
		{
			LinkBrowser.browse("https://github.com/jakevollkommer/deathbank-utility/issues");
			return;
		}

		if ("supportButton".equals(event.getKey()))
		{
			LinkBrowser.browse("https://ko-fi.com/jakevollkommer");
		}
	}

	@Subscribe
	public void onGameObjectSpawned(GameObjectSpawned event)
	{
		if (!RetrievalService.isClaimObject(event.getGameObject().getId()))
		{
			return;
		}
		retrievalObjects.add(event.getGameObject());
	}

	@Subscribe
	public void onGameObjectDespawned(GameObjectDespawned event)
	{
		retrievalObjects.remove(event.getGameObject());
	}

	@Subscribe
	public void onNpcSpawned(NpcSpawned event)
	{
		if (!RetrievalService.isClaimNpc(event.getNpc().getId()))
		{
			return;
		}
		retrievalNpcs.add(event.getNpc());
	}

	@Subscribe
	public void onNpcDespawned(NpcDespawned event)
	{
		retrievalNpcs.remove(event.getNpc());
	}

	@Subscribe
	public void onChatMessage(ChatMessage event)
	{
		if (event.getType() != ChatMessageType.GAMEMESSAGE)
		{
			return;
		}

		String message = sanitize(event.getMessage());
		if (state.isActive())
		{
			log.debug("Game message while deathbank tracked: '{}'", message);
		}
		boolean confirmsBankExists = message.contains(MSG_RETRIEVAL_SERVICE) || message.contains(MSG_RETRIEVED_SOME);
		if (confirmsBankExists)
		{
			// The message names where the items are, e.g. "...at the Theatre of Blood"
			RetrievalService named = RetrievalService.fromName(message).orElse(state.getService());
			log.debug("Retrieval service message: '{}' -> service {}", message, named);
			markVerifiedActive(named, state.getWindowTitle());
			return;
		}

		boolean confirmsBankWiped = message.contains(MSG_DIED_AGAIN) && message.contains(MSG_LOST_ITEMS);
		if (confirmsBankWiped)
		{
			transitionTo(DeathbankState.inactive(Confidence.VERIFIED));
		}
	}

	@Subscribe
	public void onWidgetLoaded(WidgetLoaded event)
	{
		if (event.getGroupId() == InterfaceID.GRAVESTONE_GENERIC)
		{
			graveWindowOpen = true;
			return;
		}
		if (event.getGroupId() != InterfaceID.GRAVESTONE_RETRIEVAL)
		{
			return;
		}
		retrievalWindowOpen = true;
		retrievalTrustTicksRemaining = -1;
		clientThread.invokeLater(this::readRetrievalContainer);
	}

	@Subscribe
	public void onWidgetClosed(WidgetClosed event)
	{
		if (event.getGroupId() == InterfaceID.GRAVESTONE_GENERIC)
		{
			graveWindowOpen = false;
		}
		if (event.getGroupId() == InterfaceID.GRAVESTONE_RETRIEVAL)
		{
			retrievalWindowOpen = false;
			retrievalTrustTicksRemaining = RETRIEVAL_TRUST_GRACE_TICKS;
		}
	}

	@Subscribe
	public void onItemContainerChanged(ItemContainerChanged event)
	{
		if (event.getContainerId() != InventoryID.GRAVESTONE)
		{
			return;
		}
		// The same container backs overworld gravestones; only trust it as deathbank
		// contents while the retrieval window (and not the grave window) is open
		int itemCount = event.getItemContainer().count();
		log.debug("Gravestone container update: {} items, retrievalWindowOpen={}, graveWindowOpen={}, graceTicks={}",
			itemCount, retrievalWindowOpen, graveWindowOpen, retrievalTrustTicksRemaining);

		if (graveWindowOpen)
		{
			return;
		}
		boolean trustworthy = retrievalWindowOpen || retrievalTrustTicksRemaining >= 0;
		if (!trustworthy)
		{
			return;
		}
		applyVerifiedContents(event.getItemContainer().getItems());
	}

	@Subscribe
	public void onActorDeath(ActorDeath event)
	{
		if (event.getActor() != client.getLocalPlayer())
		{
			return;
		}

		int regionId = currentRegionId();
		boolean deathIsSafe = SAFE_DEATH_REGIONS.contains(regionId);
		if (deathIsSafe)
		{
			return;
		}

		// Any unsafe death wipes whatever was already banked
		if (state.isActive())
		{
			transitionTo(DeathbankState.inactive(Confidence.INFERRED));
		}

		Optional<RetrievalService> service = RetrievalService.fromRegion(regionId);
		if (service.isEmpty())
		{
			return;
		}

		pendingDeathService = service.get();
		pendingDeathSnapshot = countCarriedItems();
		pendingDeathTicks = 0;
		log.debug("Death in {} region; will resolve banked items after respawn", pendingDeathService.getDisplayName());
	}

	@Subscribe
	public void onHitsplatApplied(HitsplatApplied event)
	{
		if (!state.isActive() || event.getActor() != client.getLocalPlayer())
		{
			return;
		}
		if (event.getHitsplat().getAmount() <= 0 || SAFE_DEATH_REGIONS.contains(currentRegionId()))
		{
			return;
		}

		int tick = client.getTickCount();
		damageWarningUntilTick = tick + DAMAGE_WARNING_DISPLAY_TICKS;

		boolean coolingDown = lastDamageWarnTick != -1 && tick - lastDamageWarnTick < config.damageWarningCooldownTicks();
		if (coolingDown)
		{
			return;
		}
		lastDamageWarnTick = tick;
		notifier.notify(config.damageNotification(), "You are taking damage with items in a deathbank" + serviceSuffix() + " — get safe!");
	}

	@Subscribe
	public void onGameTick(GameTick event)
	{
		tickRetrievalWindow();
		tickPendingDeath();
		tickLoginReconcile();
		tickZulrahDialog();
	}

	/**
	 * Discarding the bank empties it without sending any container update or game
	 * message, so the open window is polled rather than waited on. Reads after the
	 * window closes are logged only, until it is known whether the client keeps the
	 * container populated on close (an empty read would otherwise be ambiguous).
	 */
	private void tickRetrievalWindow()
	{
		if (retrievalWindowOpen)
		{
			readRetrievalContainer();
			return;
		}
		if (retrievalTrustTicksRemaining < 0)
		{
			return;
		}
		retrievalTrustTicksRemaining--;

		ItemContainer container = client.getItemContainer(InventoryID.GRAVESTONE);
		log.debug("Post-close container read: {}, graceTicks={}",
			container == null ? "null" : container.count() + " items", retrievalTrustTicksRemaining);
	}

	// --- Post-death resolution: diff carried items before vs after respawn ---

	private void tickPendingDeath()
	{
		if (pendingDeathService == null)
		{
			return;
		}
		pendingDeathTicks++;

		boolean respawned = pendingDeathTicks >= DEATH_RESOLVE_MIN_TICKS
			&& client.getBoostedSkillLevel(Skill.HITPOINTS) > 0;
		boolean timedOut = pendingDeathTicks >= DEATH_RESOLVE_TIMEOUT_TICKS;
		if (!respawned && !timedOut)
		{
			return;
		}
		resolvePendingDeath();
	}

	private void resolvePendingDeath()
	{
		RetrievalService service = pendingDeathService;
		Map<Integer, Integer> lost = new HashMap<>(pendingDeathSnapshot);
		countCarriedItems().forEach((id, kept) -> lost.merge(id, -kept, Integer::sum));
		clearPendingDeath();

		List<DeathbankState.ItemStack> banked = lost.entrySet().stream()
			.filter(entry -> entry.getValue() > 0)
			.map(entry -> new DeathbankState.ItemStack(entry.getKey(), entry.getValue()))
			.collect(Collectors.toList());

		if (banked.isEmpty())
		{
			log.debug("Death at {} resolved: all items kept, no deathbank created", service.getDisplayName());
			return;
		}

		// A message may already have confirmed the bank, and it names the service more
		// precisely than the region can (Phosani's and the Nightmare share a region),
		// so recording the items must not downgrade what is already known
		boolean alreadyConfirmed = state.isActive() && state.getConfidence() == Confidence.VERIFIED;
		Confidence confidence = alreadyConfirmed ? Confidence.VERIFIED : Confidence.INFERRED;
		RetrievalService labelled = alreadyConfirmed && state.getService() != null ? state.getService() : service;

		log.debug("Death at {} resolved: ~{} stacks banked, labelled {} ({})",
			service.getDisplayName(), banked.size(), labelled, confidence);
		transitionTo(DeathbankState.active(confidence, labelled, state.getWindowTitle(), banked, false));
	}

	private void clearPendingDeath()
	{
		pendingDeathService = null;
		pendingDeathSnapshot = Map.of();
		pendingDeathTicks = 0;
	}

	// --- Login reconciliation: absence of the retrieval warning proves the bank is gone ---

	private void tickLoginReconcile()
	{
		if (loginReconcileTicksRemaining < 0)
		{
			return;
		}
		loginReconcileTicksRemaining--;
		if (loginReconcileTicksRemaining >= 0)
		{
			return;
		}
		reconcileAfterLogin();
	}

	private void reconcileAfterLogin()
	{
		if (!state.isActive())
		{
			return;
		}

		boolean warningDisabledInGame = client.getVarbitValue(VarbitID.OPTION_ITEM_RETRIEVAL_WARNING_DISABLED) == 1;
		if (warningDisabledInGame)
		{
			log.debug("Login reconcile skipped: in-game retrieval warning is disabled");
			transitionTo(state.withConfidence(Confidence.UNKNOWN));
			return;
		}

		log.debug("Login reconcile: no retrieval message after {} ticks, clearing saved deathbank", LOGIN_RECONCILE_TICKS);
		transitionTo(DeathbankState.inactive(Confidence.VERIFIED));
	}

	// --- Zulrah: dialog-only retrieval, no interface ---

	private void tickZulrahDialog()
	{
		// Widget lookup first: it's a cheap null almost every tick, while the
		// region check allocates instance-aware coordinates
		Widget dialog = client.getWidget(InterfaceID.ChatLeft.TEXT);
		if (dialog == null)
		{
			return;
		}
		if (!RetrievalService.ZULRAH.getClaimRegionIds().contains(currentRegionId()))
		{
			return;
		}

		String text = sanitize(Text.sanitizeMultilineText(dialog.getText()));
		boolean mentionsHeldItems = text.contains(ZULRAH_DIALOG_LEFT_STUFF) || text.contains(ZULRAH_DIALOG_HOLDING_STUFF);
		if (mentionsHeldItems)
		{
			boolean itemsStillHeld = !text.contains(ZULRAH_DIALOG_RETURNED);
			applyZulrahDialogState(itemsStillHeld);
			return;
		}
		if (text.contains(ZULRAH_DIALOG_NOTHING))
		{
			applyZulrahDialogState(false);
		}
	}

	private void applyZulrahDialogState(boolean bankActive)
	{
		// Fires every tick while the dialog is up; only act on an actual change
		boolean alreadyCorrect = state.isActive() == bankActive && state.getConfidence() == Confidence.VERIFIED;
		if (alreadyCorrect)
		{
			return;
		}
		if (bankActive)
		{
			markVerifiedActive(RetrievalService.ZULRAH, null);
			return;
		}
		transitionTo(DeathbankState.inactive(Confidence.VERIFIED));
	}

	// --- Verified contents from the retrieval interface ---

	private void readRetrievalContainer()
	{
		ItemContainer container = client.getItemContainer(InventoryID.GRAVESTONE);
		if (container == null || !retrievalWindowOpen || graveWindowOpen)
		{
			return;
		}
		applyVerifiedContents(container.getItems());
	}

	private void applyVerifiedContents(Item[] items)
	{
		List<DeathbankState.ItemStack> stacks = toItemStacks(items);
		if (stacks.isEmpty())
		{
			if (state.isActive())
			{
				log.debug("Retrieval interface is empty; deathbank cleared");
				transitionTo(DeathbankState.inactive(Confidence.VERIFIED));
			}
			return;
		}

		List<String> windowTexts = retrievalWindowTexts();
		RetrievalService service = firstNamedService(windowTexts).orElse(state.getService());
		boolean unchanged = state.isActive() && state.isItemsVerified()
			&& state.getService() == service && stacks.equals(state.getItems());
		if (unchanged)
		{
			return;
		}

		String locationText = service != null ? null : firstNonBlank(windowTexts).orElse(state.getWindowTitle());
		log.debug("Verified deathbank contents: {} stacks, service {}, window texts {}", stacks.size(), service, windowTexts);
		transitionTo(DeathbankState.active(Confidence.VERIFIED, service, locationText, stacks, true));
	}

	/**
	 * The retrieval interface has no title component, so collect the text from the
	 * components that can name the location and let the caller match it.
	 */
	private List<String> retrievalWindowTexts()
	{
		return Stream.of(InterfaceID.GravestoneRetrieval.INFO, InterfaceID.GravestoneRetrieval.FRAME)
			.map(client::getWidget)
			.filter(Objects::nonNull)
			.flatMap(widget -> widgetTexts(widget, WINDOW_TEXT_DEPTH))
			.distinct()
			.collect(Collectors.toList());
	}

	private static Stream<String> widgetTexts(Widget widget, int depthRemaining)
	{
		String text = sanitize(Strings.nullToEmpty(widget.getText())).trim();
		Stream<String> own = text.isEmpty() ? Stream.empty() : Stream.of(text);
		if (depthRemaining <= 1)
		{
			return own;
		}

		Stream<Widget> children = Stream.of(widget.getStaticChildren(), widget.getDynamicChildren(), widget.getNestedChildren())
			.filter(Objects::nonNull)
			.flatMap(Arrays::stream)
			.filter(Objects::nonNull);
		return Stream.concat(own, children.flatMap(child -> widgetTexts(child, depthRemaining - 1)));
	}

	private static Optional<RetrievalService> firstNamedService(List<String> texts)
	{
		return texts.stream()
			.map(RetrievalService::fromName)
			.filter(Optional::isPresent)
			.map(Optional::get)
			.findFirst();
	}

	private static Optional<String> firstNonBlank(List<String> texts)
	{
		return texts.stream().filter(text -> !text.trim().isEmpty()).findFirst();
	}

	// --- State transitions: every change goes through this one door ---

	private void transitionTo(DeathbankState next)
	{
		// Any server-confirmed state supersedes the login reconcile countdown
		boolean confirmedByServer = next.getConfidence() == Confidence.VERIFIED;
		if (confirmedByServer)
		{
			loginReconcileTicksRemaining = -1;
		}
		state = next;
		saveState();
		updatePanel();
	}

	private void markVerifiedActive(RetrievalService service, String windowTitle)
	{
		boolean alreadyVerifiedActive = state.isActive() && state.getConfidence() == Confidence.VERIFIED;
		// Phosani's and the Nightmare share a region, so a later message naming the
		// specific service still has to be able to correct an already-verified label
		boolean learnsService = service != null && service != state.getService();
		if (alreadyVerifiedActive && !learnsService)
		{
			return;
		}
		// Keep any previously recorded items; they stay flagged as estimates
		// until the retrieval interface confirms them
		transitionTo(DeathbankState.active(Confidence.VERIFIED, service, windowTitle, state.getItems(), state.isItemsVerified()));
	}

	// --- Side panel ---

	private void updatePanel()
	{
		DeathbankState snapshot = state;
		clientThread.invokeLater(() ->
		{
			List<DeathbankPanel.PanelItem> items = snapshot.getItems().stream()
				.map(stack -> new DeathbankPanel.PanelItem(
					itemManager.getItemComposition(stack.getId()).getName(),
					stack.getQuantity(),
					itemManager.getImage(stack.getId(), stack.getQuantity(), stack.getQuantity() > 1)))
				.collect(Collectors.toList());
			boolean awaitingConfirmation = isAwaitingLoginConfirmation();
			SwingUtilities.invokeLater(() -> panel.update(snapshot, items, awaitingConfirmation));
		});
	}

	private static BufferedImage createPanelIcon()
	{
		BufferedImage icon = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = icon.createGraphics();
		g.setColor(new Color(140, 25, 25));
		g.fillRoundRect(1, 3, 14, 11, 4, 4);
		g.setColor(new Color(230, 200, 120));
		g.fillRect(1, 7, 14, 2);
		g.dispose();
		return icon;
	}

	// --- Persistence (per RS profile) ---

	private void loadState()
	{
		DeathbankState saved = parseState(configManager.getRSProfileConfiguration(DeathbankUtilityConfig.GROUP, STATE_KEY));
		// A previous session's certainty is not this session's certainty
		boolean staleClaim = saved.isActive() && saved.getConfidence() == Confidence.VERIFIED;
		state = staleClaim ? saved.withConfidence(Confidence.UNKNOWN) : saved;
	}

	private DeathbankState parseState(String json)
	{
		if (json == null)
		{
			return DeathbankState.unknown();
		}
		try
		{
			DeathbankState parsed = gson.fromJson(json, DeathbankState.class);
			return parsed != null ? parsed : DeathbankState.unknown();
		}
		catch (JsonSyntaxException e)
		{
			log.warn("Discarding unparseable saved deathbank state", e);
			return DeathbankState.unknown();
		}
	}

	private void saveState()
	{
		configManager.setRSProfileConfiguration(DeathbankUtilityConfig.GROUP, STATE_KEY, gson.toJson(state));
	}

	// --- Helpers ---

	private String serviceSuffix()
	{
		return state.isLocationKnown() ? " at " + state.displayLabel() : "";
	}

	private int currentRegionId()
	{
		if (client.getLocalPlayer() == null)
		{
			return -1;
		}
		return WorldPoint.fromLocalInstance(client, client.getLocalPlayer().getLocalLocation()).getRegionID();
	}

	private Map<Integer, Integer> countCarriedItems()
	{
		return Stream.of(InventoryID.INV, InventoryID.WORN)
			.map(client::getItemContainer)
			.filter(Objects::nonNull)
			.flatMap(container -> Arrays.stream(container.getItems()))
			.filter(DeathbankUtilityPlugin::isRealItem)
			.collect(Collectors.toMap(Item::getId, Item::getQuantity, Integer::sum, HashMap::new));
	}

	private static List<DeathbankState.ItemStack> toItemStacks(Item[] items)
	{
		return Arrays.stream(items)
			.filter(DeathbankUtilityPlugin::isRealItem)
			.map(item -> new DeathbankState.ItemStack(item.getId(), item.getQuantity()))
			.collect(Collectors.toList());
	}

	private static String sanitize(String text)
	{
		return MESSAGE_MACRO.matcher(Text.removeTags(text)).replaceAll("").trim();
	}

	private static boolean isRealItem(Item item)
	{
		return item.getId() != -1 && item.getQuantity() > 0;
	}
}
