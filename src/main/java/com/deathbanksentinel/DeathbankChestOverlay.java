package com.deathbanksentinel;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import javax.inject.Inject;
import net.runelite.api.NPC;
import net.runelite.api.Point;
import net.runelite.api.TileObject;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayUtil;
import net.runelite.client.ui.overlay.outline.ModelOutlineRenderer;

/**
 * Outlines retrieval chests and NPCs while a deathbank is active and labels
 * them DEATHBANK, so the claim point visibly signals there is something inside.
 */
class DeathbankChestOverlay extends Overlay
{
	private static final String LABEL = "DEATHBANK";
	private static final int OUTLINE_WIDTH = 4;
	private static final int OUTLINE_FEATHER = 4;
	private static final int OBJECT_LABEL_Z_OFFSET = 130;
	private static final int NPC_LABEL_Z_OFFSET = 40;

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

		graphics.setFont(FontManager.getRunescapeBoldFont());
		Color color = config.highlightColor();
		Color labelColor = new Color(color.getRed(), color.getGreen(), color.getBlue());

		plugin.getRetrievalObjects().forEach(object -> renderObject(graphics, object, color, labelColor));
		plugin.getRetrievalNpcs().forEach(npc -> renderNpc(graphics, npc, color, labelColor));
		return null;
	}

	private void renderObject(Graphics2D graphics, TileObject object, Color color, Color labelColor)
	{
		modelOutlineRenderer.drawOutline(object, OUTLINE_WIDTH, color, OUTLINE_FEATHER);
		renderLabel(graphics, object.getCanvasTextLocation(graphics, LABEL, OBJECT_LABEL_Z_OFFSET), labelColor);
	}

	private void renderNpc(Graphics2D graphics, NPC npc, Color color, Color labelColor)
	{
		modelOutlineRenderer.drawOutline(npc, OUTLINE_WIDTH, color, OUTLINE_FEATHER);
		renderLabel(graphics, npc.getCanvasTextLocation(graphics, LABEL, npc.getLogicalHeight() + NPC_LABEL_Z_OFFSET), labelColor);
	}

	private static void renderLabel(Graphics2D graphics, Point location, Color labelColor)
	{
		if (location == null)
		{
			return;
		}
		OverlayUtil.renderTextLocation(graphics, location, LABEL, labelColor);
	}
}
