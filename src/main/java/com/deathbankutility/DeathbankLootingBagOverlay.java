package com.deathbankutility;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import javax.inject.Inject;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.widgets.WidgetItem;
import net.runelite.client.game.ItemManager;
import net.runelite.client.ui.overlay.WidgetItemOverlay;

/**
 * Marks the items in the retrieval interface that came out of the looting bag,
 * so they can be picked back out and re-bagged without sorting by memory.
 */
class DeathbankLootingBagOverlay extends WidgetItemOverlay
{
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

		// Outline the item's own shape rather than boxing the slot. Thin sprites like
		// bones are almost all edge, so the outline alone swallows them: the item is
		// drawn back over the top, leaving the colour as a halo around it.
		Rectangle bounds = widgetItem.getCanvasBounds();
		int quantity = widgetItem.getQuantity();
		BufferedImage outline = itemManager.getItemOutline(itemId, quantity, config.lootingBagColor());
		if (outline == null)
		{
			return;
		}

		graphics.drawImage(outline, bounds.x, bounds.y, null);
		graphics.drawImage(itemManager.getImage(itemId, quantity, quantity > 1), bounds.x, bounds.y, null);
	}
}
