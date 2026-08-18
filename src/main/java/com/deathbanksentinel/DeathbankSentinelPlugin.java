package com.deathbanksentinel;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.inject.Provides;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.inject.Inject;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.Item;
import net.runelite.api.ItemContainer;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.ActorDeath;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.HitsplatApplied;
import net.runelite.api.events.ItemContainerChanged;
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
import net.runelite.client.ui.overlay.infobox.InfoBoxManager;

@Slf4j
@PluginDescriptor(
	name = "Deathbank Sentinel",
	description = "Tracks item retrieval services (deathbanks) and warns before you lose what's inside",
	tags = {"death", "deathbank", "item", "retrieval", "warning", "uim"}
)
public class DeathbankSentinelPlugin extends Plugin
{
	// Server messages (matched after tag stripping); exact wording re-verified in TESTING.md
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

	private static final String STATE_KEY = "state";
	private static final int LOGIN_RECONCILE_TICKS = 25;

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
	private InfoBoxManager infoBoxManager;
	@Inject
	private Notifier notifier;
	@Inject
	private DeathbankSentinelConfig config;

	@Getter
	private DeathbankState state = DeathbankState.inactive(Confidence.UNKNOWN);

	private DeathbankInfoBox infoBox;
	private boolean infoBoxVisible;
	private boolean retrievalWindowOpen;
	private boolean graveWindowOpen;
	private int loginReconcileTicksRemaining = -1;
	private int lastDamageWarnTick = -1;

	@Provides
	DeathbankSentinelConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(DeathbankSentinelConfig.class);
	}

	@Override
	protected void startUp()
	{
		infoBox = new DeathbankInfoBox(itemManager.getImage(ItemID.SKULL), this);
		loadState();
		refreshInfoBox();
	}

	@Override
	protected void shutDown()
	{
		saveState();
		hideInfoBox();
		retrievalWindowOpen = false;
		graveWindowOpen = false;
		loginReconcileTicksRemaining = -1;
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged event)
	{
		boolean freshLogin = event.getGameState() == GameState.LOGGING_IN;
		if (freshLogin)
		{
			loginReconcileTicksRemaining = LOGIN_RECONCILE_TICKS;
		}
	}

	@Subscribe
	public void onRuneScapeProfileChanged(RuneScapeProfileChanged event)
	{
		loadState();
		refreshInfoBox();
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged event)
	{
		if (!DeathbankSentinelConfig.GROUP.equals(event.getGroup()))
		{
			return;
		}
		refreshInfoBox();
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
			notifier.notify(config.wipeNotification(), "Your deathbank was deleted — you died again before collecting it.");
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

		Optional<RetrievalService> service = RetrievalService.fromRegion(regionId);
		if (service.isPresent())
		{
			markInferredActiveFromDeath(service.get());
			return;
		}

		boolean unsafeDeathWipesExistingBank = state.isActive();
		if (unsafeDeathWipesExistingBank)
		{
			markInactive(Confidence.INFERRED);
			notifier.notify(config.wipeNotification(), "You died — the items in your deathbank were likely deleted.");
		}
	}

	@Subscribe
	public void onHitsplatApplied(HitsplatApplied event)
	{
		if (!state.isActive() || event.getActor() != client.getLocalPlayer())
		{
			return;
		}
		if (event.getHitsplat().getAmount() <= 0)
		{
			return;
		}

		int tick = client.getTickCount();
		boolean coolingDown = lastDamageWarnTick != -1 && tick - lastDamageWarnTick < config.damageWarningCooldownTicks();
		if (coolingDown || SAFE_DEATH_REGIONS.contains(currentRegionId()))
		{
			return;
		}

		lastDamageWarnTick = tick;
		notifier.notify(config.damageNotification(), "You are taking damage with items in a deathbank" + serviceSuffix() + " — get safe!");
	}

	@Subscribe
	public void onGameTick(GameTick event)
	{
		tickLoginReconcile();
		tickZulrahDialog();
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
		if (!config.loginReconciliation() || !state.isActive())
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

	private void markInferredActiveFromDeath(RetrievalService service)
	{
		List<DeathbankState.ItemStack> carried = snapshotCarriedItems();
		log.debug("Death in {} region; inferring deathbank with ~{} stacks", service.getDisplayName(), carried.size());
		state = DeathbankState.active(Confidence.INFERRED, service.getDisplayName(), carried, false);
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
		refreshInfoBox();
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

	// --- Infobox ---

	private void refreshInfoBox()
	{
		boolean shouldShow = config.showInfobox() && state.isActive();
		if (!shouldShow)
		{
			hideInfoBox();
			return;
		}

		infoBox.setTooltip(buildTooltip());
		if (infoBoxVisible)
		{
			return;
		}
		infoBoxManager.addInfoBox(infoBox);
		infoBoxVisible = true;
	}

	private void hideInfoBox()
	{
		if (!infoBoxVisible)
		{
			return;
		}
		infoBoxManager.removeInfoBox(infoBox);
		infoBoxVisible = false;
	}

	private String buildTooltip()
	{
		String service = state.getServiceName() != null ? state.getServiceName() : "Unknown location";
		String tier = state.getConfidence().name().toLowerCase();
		String contents = describeContents();
		return "Deathbank active: " + service + " (" + tier + ")</br>" + contents
			+ "</br>Any unsafe death anywhere will delete these items.";
	}

	private String describeContents()
	{
		if (state.getItems().isEmpty())
		{
			return "Contents unknown — open the retrieval chest to verify.";
		}
		String qualifier = state.isItemsVerified() ? "" : "~";
		return qualifier + state.getItems().size() + " item stacks inside.";
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

	private List<DeathbankState.ItemStack> snapshotCarriedItems()
	{
		return toItemStacks(Stream.of(InventoryID.INV, InventoryID.WORN)
			.map(client::getItemContainer)
			.filter(Objects::nonNull)
			.flatMap(container -> Arrays.stream(container.getItems())));
	}

	private static List<DeathbankState.ItemStack> toItemStacks(Stream<Item> items)
	{
		return items
			.filter(item -> item.getId() != -1 && item.getQuantity() > 0)
			.map(item -> new DeathbankState.ItemStack(item.getId(), item.getQuantity()))
			.collect(Collectors.toList());
	}
}
