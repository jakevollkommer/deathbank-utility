package com.deathbanksentinel;

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
import net.runelite.api.gameval.NpcID;
import net.runelite.api.gameval.ObjectID;
import net.runelite.api.gameval.VarbitID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.Notifier;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.RuneScapeProfileChanged;
import net.runelite.client.game.ItemManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.ui.overlay.OverlayManager;

@Slf4j
@PluginDescriptor(
	name = "Deathbank Sentinel",
	description = "Tracks item retrieval services (deathbanks) and warns before you lose what's inside",
	tags = {"death", "deathbank", "item", "retrieval", "warning", "uim"}
)
public class DeathbankSentinelPlugin extends Plugin
{
	// Server messages (matched on substring); exact wording re-verified in TESTING.md
	private static final String MSG_RETRIEVAL_SERVICE = "items stored in an item retrieval service";
	private static final String MSG_RETRIEVED_SOME = "retrieved some of your items";
	private static final String MSG_DIED_AGAIN = "You have died again";
	private static final String MSG_LOST_ITEMS = "lost the items";

	// Zulrah has no retrieval interface — the priestess returns items through dialog
	private static final String ZULRAH_DIALOG_LEFT_STUFF = "You left some stuff at Zulrah's shrine";
	private static final String ZULRAH_DIALOG_HOLDING_STUFF = "I've got some stuff you left at the shrine";
	private static final String ZULRAH_DIALOG_RETURNED = "I've returned it to you now";
	private static final String ZULRAH_DIALOG_NOTHING = "I don't have anything for you to collect";
	private static final Set<Integer> ZULRAH_CLAIM_REGIONS = Set.of(8495, 8751);

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

	// Retrieval chests known by object ID; the rest are NPCs (below).
	// TODO from in-game testing: Mimic casket, Sepulchre Mysterious Stranger, Hespori's Arno
	// Raw IDs where the gameval constant lives in package-private ObjectID1
	private static final Set<Integer> RETRIEVAL_OBJECT_IDS = Set.of(
		32656, // TOB_SURFACE_GRAVESTONE_CHEST
		42854, 42858, // NEX_GRAVESTONE_CHEST (+_NOOP)
		46078, 46079, // TOA_LOBBY_GRAVESTONE_CHEST (+_NOOP)
		ObjectID.GARGBOSS_GRAVESTONE_RETRIEVAL
	);
	private static final Set<Integer> RETRIEVAL_NPC_IDS = Set.of(
		NpcID.SNAKEBOSS_PRIEST_1OP, NpcID.SNAKEBOSS_PRIEST_2OPS, // Priestess Zul-Gwenwynig
		NpcID.TORFINN_RELLEKKA, NpcID.TORFINN_UNGAEL,
		NpcID.TORFINN_COLLECT_RELLEKKA, NpcID.TORFINN_COLLECT_UNGAEL,
		NpcID.KAHLITH_ALCHEMICAL_HUNTER, // Orrvor quo Maten (Hydra)
		NpcID.SHURA, NpcID.SHURA_1OP, NpcID.SHURA_2OP,
		NpcID.NIGHTMARE_CHALLENGE_SISTER, NpcID.NIGHTMARE_CHALLENGE_SISTER_1OP, NpcID.NIGHTMARE_CHALLENGE_SISTER_2OP,
		NpcID.FOSSIL_MINEGUARD // Petrified Pete
	);

	private static final String STATE_KEY = "state";
	private static final int LOGIN_RECONCILE_TICKS = 25;
	private static final int DEATH_RESOLVE_MIN_TICKS = 3;
	private static final int DEATH_RESOLVE_TIMEOUT_TICKS = 50;
	private static final int DAMAGE_WARNING_DISPLAY_TICKS = 8;

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
	private DeathbankSentinelConfig config;
	@Inject
	private DeathbankIndicatorOverlay indicatorOverlay;
	@Inject
	private DeathbankChestOverlay chestOverlay;
	@Inject
	private DeathbankWarningOverlay warningOverlay;

	@Getter
	private DeathbankState state = DeathbankState.inactive(Confidence.UNKNOWN);
	@Getter
	private final List<GameObject> retrievalObjects = new ArrayList<>();
	@Getter
	private final List<NPC> retrievalNpcs = new ArrayList<>();

	private DeathbankPanel panel;
	private NavigationButton navButton;
	private BufferedImage indicatorIcon;
	private boolean retrievalWindowOpen;
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
	DeathbankSentinelConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(DeathbankSentinelConfig.class);
	}

	@Override
	protected void startUp()
	{
		panel = new DeathbankPanel();
		navButton = NavigationButton.builder()
			.tooltip("Deathbank Sentinel")
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
		loginReconcileTicksRemaining = -1;
		damageWarningUntilTick = -1;
		clearPendingDeath();
	}

	boolean isDamageWarningActive()
	{
		return client.getTickCount() <= damageWarningUntilTick;
	}

	BufferedImage getIndicatorIcon()
	{
		if (indicatorIcon == null)
		{
			indicatorIcon = itemManager.getImage(net.runelite.api.gameval.ItemID.SKULL);
		}
		return indicatorIcon;
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged event)
	{
		if (event.getGameState() == GameState.LOGGING_IN)
		{
			loginReconcileTicksRemaining = LOGIN_RECONCILE_TICKS;
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

	@Subscribe
	public void onGameObjectSpawned(GameObjectSpawned event)
	{
		if (!RETRIEVAL_OBJECT_IDS.contains(event.getGameObject().getId()))
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
		if (!RETRIEVAL_NPC_IDS.contains(event.getNpc().getId()))
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

		String message = event.getMessage();
		boolean confirmsBankExists = message.contains(MSG_RETRIEVAL_SERVICE) || message.contains(MSG_RETRIEVED_SOME);
		if (confirmsBankExists)
		{
			loginReconcileTicksRemaining = -1;
			markVerifiedActive(state.getServiceName());
			return;
		}

		boolean confirmsBankWiped = message.contains(MSG_DIED_AGAIN) && message.contains(MSG_LOST_ITEMS);
		if (confirmsBankWiped)
		{
			markInactive(Confidence.VERIFIED);
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
		loginReconcileTicksRemaining = -1;
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
		if (graveWindowOpen)
		{
			log.debug("Ignoring gravestone container update ({} items)", event.getItemContainer().count());
			return;
		}
		if (!retrievalWindowOpen)
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
			markInactive(Confidence.INFERRED);
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
		tickPendingDeath();
		tickLoginReconcile();
		tickZulrahDialog();
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

		log.debug("Death at {} resolved: ~{} stacks banked", service.getDisplayName(), banked.size());
		state = DeathbankState.active(Confidence.INFERRED, service.getDisplayName(), banked, false);
		stateChanged();
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
			downgradeToUnknown();
			return;
		}

		log.debug("Login reconcile: no retrieval message after {} ticks, clearing saved deathbank", LOGIN_RECONCILE_TICKS);
		markInactive(Confidence.VERIFIED);
	}

	// --- Zulrah: dialog-only retrieval, no interface ---

	private void tickZulrahDialog()
	{
		if (!ZULRAH_CLAIM_REGIONS.contains(currentRegionId()))
		{
			return;
		}

		Widget dialog = client.getWidget(InterfaceID.ChatLeft.TEXT);
		if (dialog == null)
		{
			return;
		}

		String text = dialog.getText().replace("<br>", " ");
		boolean mentionsHeldItems = text.contains(ZULRAH_DIALOG_LEFT_STUFF) || text.contains(ZULRAH_DIALOG_HOLDING_STUFF);
		if (mentionsHeldItems)
		{
			boolean itemsJustReturned = text.contains(ZULRAH_DIALOG_RETURNED);
			applyZulrahDialogState(!itemsJustReturned);
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
			markVerifiedActive(RetrievalService.ZULRAH.getDisplayName());
			return;
		}
		markInactive(Confidence.VERIFIED);
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
		List<DeathbankState.ItemStack> stacks = toItemStacks(Arrays.stream(items));
		if (stacks.isEmpty())
		{
			markInactive(Confidence.VERIFIED);
			return;
		}

		String title = retrievalWindowTitle();
		log.debug("Verified deathbank contents: {} stacks, window title '{}'", stacks.size(), title);
		state = DeathbankState.active(Confidence.VERIFIED, title != null ? title : state.getServiceName(), stacks, true);
		stateChanged();
	}

	private String retrievalWindowTitle()
	{
		Widget title = client.getWidget(InterfaceID.GRAVESTONE_RETRIEVAL, 1);
		return title != null ? title.getText() : null;
	}

	// --- State transitions ---

	private void markVerifiedActive(String serviceName)
	{
		boolean alreadyVerifiedActive = state.isActive() && state.getConfidence() == Confidence.VERIFIED;
		if (alreadyVerifiedActive)
		{
			return;
		}
		// Keep any previously recorded items (still an estimate until the interface confirms)
		state = DeathbankState.active(Confidence.VERIFIED, serviceName, state.getItems(), false);
		stateChanged();
	}

	private void markInactive(Confidence confidence)
	{
		state = DeathbankState.inactive(confidence);
		stateChanged();
	}

	private void downgradeToUnknown()
	{
		state.setConfidence(Confidence.UNKNOWN);
		state.touch();
		stateChanged();
	}

	private void stateChanged()
	{
		saveState();
		updatePanel();
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
			SwingUtilities.invokeLater(() -> panel.update(snapshot, items));
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
		String json = configManager.getRSProfileConfiguration(DeathbankSentinelConfig.GROUP, STATE_KEY);
		state = parseState(json);
		// A previous session's certainty is not this session's certainty
		boolean staleClaim = state.isActive() && state.getConfidence() == Confidence.VERIFIED;
		if (staleClaim)
		{
			state.setConfidence(Confidence.UNKNOWN);
		}
	}

	private DeathbankState parseState(String json)
	{
		if (json == null)
		{
			return DeathbankState.inactive(Confidence.UNKNOWN);
		}
		try
		{
			DeathbankState parsed = gson.fromJson(json, DeathbankState.class);
			return parsed != null ? parsed : DeathbankState.inactive(Confidence.UNKNOWN);
		}
		catch (JsonSyntaxException e)
		{
			log.warn("Discarding unparseable saved deathbank state", e);
			return DeathbankState.inactive(Confidence.UNKNOWN);
		}
	}

	private void saveState()
	{
		configManager.setRSProfileConfiguration(DeathbankSentinelConfig.GROUP, STATE_KEY, gson.toJson(state));
	}

	// --- Helpers ---

	private String serviceSuffix()
	{
		return state.getServiceName() != null ? " at " + state.getServiceName() : "";
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
			.filter(item -> item.getId() != -1 && item.getQuantity() > 0)
			.collect(Collectors.toMap(Item::getId, Item::getQuantity, Integer::sum, HashMap::new));
	}

	private static List<DeathbankState.ItemStack> toItemStacks(Stream<Item> items)
	{
		return items
			.filter(item -> item.getId() != -1 && item.getQuantity() > 0)
			.map(item -> new DeathbankState.ItemStack(item.getId(), item.getQuantity()))
			.collect(Collectors.toList());
	}
}
