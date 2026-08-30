package com.deathbankutility;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import javax.inject.Inject;
import net.runelite.api.widgets.WidgetItem;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.client.ui.overlay.WidgetItemOverlay;

/**
 * Marks the items in the retrieval interface that came out of the looting bag,
 * so they can be picked back out and re-bagged without sorting by memory.
 */
class DeathbankLootingBagOverlay extends WidgetItemOverlay
{
	private final DeathbankUtilityPlugin plugin;
	private final DeathbankUtilityConfig config;

	@Inject
	DeathbankLootingBagOverlay(DeathbankUtilityPlugin plugin, DeathbankUtilityConfig config)
	{
		this.plugin = plugin;
		this.config = config;
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
		graphics.setColor(color);
		graphics.draw(bounds);
		graphics.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha() / 4));
		graphics.fill(bounds);
	}
}
