package com.deathbanksentinel;

import java.awt.Dimension;
import java.awt.Graphics2D;
import javax.inject.Inject;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.outline.ModelOutlineRenderer;

/**
 * Outlines retrieval chests and NPCs while a deathbank is active, so the
 * claim point visibly signals there is something inside.
 */
class DeathbankChestOverlay extends Overlay
{
	private static final int OUTLINE_WIDTH = 4;
	private static final int OUTLINE_FEATHER = 4;

	private final DeathbankSentinelPlugin plugin;
	private final DeathbankSentinelConfig config;
	private final ModelOutlineRenderer modelOutlineRenderer;

	@Inject
	DeathbankChestOverlay(DeathbankSentinelPlugin plugin, DeathbankSentinelConfig config, ModelOutlineRenderer modelOutlineRenderer)
	{
		this.plugin = plugin;
		this.config = config;
		this.modelOutlineRenderer = modelOutlineRenderer;
		setPosition(OverlayPosition.DYNAMIC);
		setLayer(OverlayLayer.ABOVE_SCENE);
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		boolean shouldHighlight = config.highlightRetrievalPoints() && plugin.getState().isActive();
		if (!shouldHighlight)
		{
			return null;
		}

		plugin.getRetrievalObjects().forEach(object ->
			modelOutlineRenderer.drawOutline(object, OUTLINE_WIDTH, config.highlightColor(), OUTLINE_FEATHER));
		plugin.getRetrievalNpcs().forEach(npc ->
			modelOutlineRenderer.drawOutline(npc, OUTLINE_WIDTH, config.highlightColor(), OUTLINE_FEATHER));
		return null;
	}
}
