package com.deathbankutility;

import java.awt.Color;
import java.time.Instant;
import net.runelite.client.config.Alpha;
import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;
import net.runelite.client.config.Notification;
import net.runelite.client.config.Range;

@ConfigGroup(DeathbankUtilityConfig.GROUP)
public interface DeathbankUtilityConfig extends Config
{
	String GROUP = "deathbankutility";

	@ConfigItem(
		keyName = "showIndicator",
		name = "Show indicator",
		description = "Show a red indicator box while a deathbank is active",
		position = 1
	)
	default boolean showIndicator()
	{
		return true;
	}

	@ConfigItem(
		keyName = "highlightRetrievalPoints",
		name = "Highlight retrieval chests",
		description = "Outline retrieval chests and NPCs while your deathbank is active",
		position = 2
	)
	default boolean highlightRetrievalPoints()
	{
		return true;
	}

	@Alpha
	@ConfigItem(
		keyName = "highlightColor",
		name = "Highlight color",
		description = "Outline color for retrieval chests and NPCs holding your items",
		position = 3
	)
	default Color highlightColor()
	{
		return new Color(255, 30, 30, 200);
	}

	@ConfigItem(
		keyName = "highlightLootingBagItems",
		name = "Mark looting bag items",
		description = "Outline the items in the retrieval interface that came out of your looting bag",
		position = 4
	)
	default boolean highlightLootingBagItems()
	{
		return true;
	}

	@Alpha
	@ConfigItem(
		keyName = "lootingBagColor",
		name = "Looting bag color",
		description = "Outline color for items that came out of your looting bag",
		position = 5
	)
	default Color lootingBagColor()
	{
		return new Color(70, 180, 255, 220);
	}

	@ConfigItem(
		keyName = "showDamageText",
		name = "On-screen damage warning",
		description = "Flash DEATHBANK WARNING on screen when you take damage with an active deathbank",
		position = 6
	)
	default boolean showDamageText()
	{
		return true;
	}

	@ConfigItem(
		keyName = "damageNotification",
		name = "Damage notification",
		description = "Additional notification (sound, tray, focus request) when taking damage with an active deathbank",
		position = 7
	)
	default Notification damageNotification()
	{
		return Notification.OFF;
	}

	@Range(min = 5, max = 500)
	@ConfigItem(
		keyName = "damageWarningCooldown",
		name = "Damage warning cooldown",
		description = "Minimum game ticks between damage warnings (100 ticks = 1 minute)",
		position = 8
	)
	default int damageWarningCooldownTicks()
	{
		return 50;
	}

	@ConfigSection(
		name = "Feedback",
		description = "Early release: not feature complete and tracking may be inaccurate. Bug reports and feature requests are very welcome",
		position = 99
	)
	String feedbackSection = "feedbackSection";

	// ConfigPanel renders a label for every config item but only builds an editor
	// for the types it knows about. Instant is stored fine by ConfigManager and has
	// no editor, so this row shows as read-only text.
	@ConfigItem(
		keyName = "earlyReleaseNotice",
		name = "<html><body style='margin:0;width:165px'>Early release: not feature complete, and tracking can be "
			+ "wrong. Never treat a missing warning as proof you have no deathbank. Bug reports and "
			+ "feature requests are very welcome.</body></html>",
		description = "",
		section = feedbackSection,
		position = 0
	)
	default Instant earlyReleaseNotice()
	{
		return Instant.EPOCH;
	}

	@ConfigItem(
		keyName = "suggestButton",
		name = "Report a bug or idea",
		description = "Found a bug or have a feature request? Click the box to open the GitHub issues page",
		section = feedbackSection,
		position = 1
	)
	default boolean suggestButton()
	{
		return false;
	}

	@ConfigItem(
		keyName = "supportButton",
		name = "Buy me a coffee ❤",
		description = "Enjoying the plugin? Click the box to open the Ko-fi page",
		section = feedbackSection,
		position = 2
	)
	default boolean supportButton()
	{
		return false;
	}
}
