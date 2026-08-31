package com.deathbankutility;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import javax.inject.Inject;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.ItemID;
import net.runelite.api.widgets.WidgetItem;
import net.runelite.client.game.ItemManager;
import net.runelite.client.ui.overlay.WidgetItemOverlay;

/**
 * Marks the items in the retrieval interface that came out of the looting bag,
 * so they can be picked back out and re-bagged without sorting by memory.
 */
class DeathbankLootingBagOverlay extends WidgetItemOverlay
{
	private static final int BADGE_SIZE = 11;

	private final DeathbankUtilityPlugin plugin;
	private final DeathbankUtilityConfig config;
	private final ItemManager itemManager;

	@Inject
	DeathbankLootingBagOverlay(DeathbankUtilityPlugin plugin, DeathbankUtilityConfig config, ItemManager itemManager)
	{
		this.plugin = plugin;
		this.config = config;
		this.itemManager = itemManager;
		showOnInterfaces(InterfaceID.GRAVESTONE_RETRIEVAL, InterfaceID.GRAVESTONE_GENERIC);
	}

	@Override
	public void renderItemOverlay(Graphics2D graphics, int itemId, WidgetItem widgetItem)
	{
		if (!config.highlightLootingBagItems() || !plugin.getState().cameFromLootingBag(itemId))
		{
			return;
		}

		Color color = config.lootingBagColor();
		Rectangle bounds = widgetItem.getCanvasBounds();

		// A corner badge of the bag itself, so the marking says what it means rather
		// than relying on the player remembering what the colour stands for
		BufferedImage bagIcon = itemManager.getImage(ItemID.LOOTING_BAG);
		if (bagIcon != null)
		{
			graphics.drawImage(bagIcon, bounds.x + bounds.width - BADGE_SIZE,
				bounds.y + bounds.height - BADGE_SIZE, BADGE_SIZE, BADGE_SIZE, null);
		}

		graphics.setColor(color);
		graphics.drawRect(bounds.x, bounds.y, bounds.width, bounds.height);
	}
}
