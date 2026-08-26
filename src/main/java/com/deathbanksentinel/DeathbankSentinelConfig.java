package com.deathbanksentinel;

import java.awt.Color;
import net.runelite.client.config.Alpha;
import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;
import net.runelite.client.config.Notification;
import net.runelite.client.config.Range;

@ConfigGroup(DeathbankSentinelConfig.GROUP)
public interface DeathbankSentinelConfig extends Config
{
	String GROUP = "deathbanksentinel";

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
		keyName = "showDamageText",
		name = "On-screen damage warning",
		description = "Flash DEATHBANK WARNING on screen when you take damage with an active deathbank",
		position = 4
	)
	default boolean showDamageText()
	{
		return true;
	}

	@ConfigItem(
		keyName = "damageNotification",
		name = "Damage notification",
		description = "Additional notification (sound, tray, focus request) when taking damage with an active deathbank",
		position = 5
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
		position = 6
	)
	default int damageWarningCooldownTicks()
	{
		return 50;
	}

	@ConfigSection(
		name = "Feedback",
		description = "Suggestions, bug reports, and support",
		position = 99
	)
	String feedbackSection = "feedbackSection";

	@ConfigItem(
		keyName = "suggestButton",
		name = "Suggest a feature",
		description = "Have an idea or found a bug? Click the box to open the GitHub issues page",
		section = feedbackSection,
		position = 0
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
		position = 1
	)
	default boolean supportButton()
	{
		return false;
	}
}
