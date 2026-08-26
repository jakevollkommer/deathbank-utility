package com.deathbankutility;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Point;
import javax.inject.Inject;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.InfoBoxComponent;
import net.runelite.client.ui.overlay.components.TextComponent;

/**
 * Infobox-style indicator with a red background so an active deathbank
 * stands out, labeled with where the deathbank is.
 */
class DeathbankIndicatorOverlay extends Overlay
{
	private static final Color BACKGROUND = new Color(120, 0, 0, 170);
	private static final int BOX_SIZE = 40;
	private static final Dimension BOX_DIMENSION = new Dimension(BOX_SIZE, BOX_SIZE);
	private static final int LABEL_HEIGHT = 14;

	private final DeathbankUtilityPlugin plugin;
	private final DeathbankUtilityConfig config;

	@Inject
	DeathbankIndicatorOverlay(DeathbankUtilityPlugin plugin, DeathbankUtilityConfig config)
	{
		this.plugin = plugin;
		this.config = config;
		setPosition(OverlayPosition.TOP_LEFT);
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		DeathbankState state = plugin.getState();
		boolean shouldShow = config.showIndicator() && state.isActive();
		if (!shouldShow)
		{
			return null;
		}

		InfoBoxComponent box = new InfoBoxComponent();
		box.setImage(plugin.getIndicatorIcon());
		box.setText(countText(state));
		box.setColor(state.getConfidence().getColor());
		box.setBackgroundColor(BACKGROUND);
		box.setPreferredSize(BOX_DIMENSION);
		box.render(graphics);

		renderLocationLabel(graphics, state.shortLabel());
		return new Dimension(BOX_SIZE, BOX_SIZE + LABEL_HEIGHT);
	}

	private static void renderLocationLabel(Graphics2D graphics, String label)
	{
		graphics.setFont(FontManager.getRunescapeSmallFont());
		FontMetrics metrics = graphics.getFontMetrics();

		TextComponent text = new TextComponent();
		text.setText(label);
		text.setColor(Color.WHITE);
		text.setPosition(new Point((BOX_SIZE - metrics.stringWidth(label)) / 2, BOX_SIZE + LABEL_HEIGHT - 3));
		text.render(graphics);
	}

	private static String countText(DeathbankState state)
	{
		if (state.getItems().isEmpty())
		{
			return "!";
		}
		String qualifier = state.isItemsVerified() ? "" : "~";
		return qualifier + state.getItems().size();
	}
}
