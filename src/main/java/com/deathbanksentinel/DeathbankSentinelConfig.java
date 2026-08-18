package com.deathbanksentinel;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.Notification;
import net.runelite.client.config.Range;

@ConfigGroup(DeathbankSentinelConfig.GROUP)
public interface DeathbankSentinelConfig extends Config
{
	String GROUP = "deathbanksentinel";

	@ConfigItem(
		keyName = "showInfobox",
		name = "Show infobox",
		description = "Show an infobox while a deathbank is (or may be) active",
		position = 1
	)
	default boolean showInfobox()
	{
		return true;
	}

	@ConfigItem(
		keyName = "damageNotification",
		name = "Warn on damage taken",
		description = "Notify when you take damage while a deathbank is active — configure flash, sound, and focus request here",
		position = 2
	)
	default Notification damageNotification()
	{
		return Notification.ON;
	}

	@Range(min = 5, max = 500)
	@ConfigItem(
		keyName = "damageWarningCooldown",
		name = "Damage warning cooldown",
		description = "Minimum game ticks between damage warnings (100 ticks = 1 minute)",
		position = 3
	)
	default int damageWarningCooldownTicks()
	{
		return 50;
	}

	@ConfigItem(
		keyName = "wipeNotification",
		name = "Warn on suspected wipe",
		description = "Notify when the plugin believes your deathbank was just deleted by an unsafe death",
		position = 4
	)
	default Notification wipeNotification()
	{
		return Notification.ON;
	}

	@ConfigItem(
		keyName = "loginReconciliation",
		name = "Reconcile at login",
		description = "If the game's item-retrieval login warning is enabled and does not appear, clear a stale saved deathbank",
		position = 5
	)
	default boolean loginReconciliation()
	{
		return true;
	}
}
