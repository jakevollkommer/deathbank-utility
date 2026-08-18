package com.deathbanksentinel;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import javax.inject.Inject;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.InfoBoxComponent;

/**
 * Infobox-style indicator with a red background so an active deathbank
 * stands out from ordinary infoboxes.
 */
class DeathbankIndicatorOverlay extends Overlay
{
	private static final Color BACKGROUND = new Color(120, 0, 0, 170);
	private static final Color VERIFIED_COLOR = Color.WHITE;
	private static final Color INFERRED_COLOR = Color.YELLOW;
	private static final Color UNKNOWN_COLOR = Color.ORANGE;
	private static final Dimension BOX_SIZE = new Dimension(40, 40);

	private final DeathbankSentinelPlugin plugin;
	private final DeathbankSentinelConfig config;
	private final BufferedImage icon;

	@Inject
	DeathbankIndicatorOverlay(DeathbankSentinelPlugin plugin, DeathbankSentinelConfig config)
	{
		this.plugin = plugin;
		this.config = config;
		this.icon = plugin.getIndicatorIcon();
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
		box.setImage(icon);
		box.setText(countText(state));
		box.setColor(confidenceColor(state));
		box.setBackgroundColor(BACKGROUND);
		box.setPreferredSize(BOX_SIZE);
		return box.render(graphics);
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

	private static Color confidenceColor(DeathbankState state)
	{
		switch (state.getConfidence())
		{
			case VERIFIED:
				return VERIFIED_COLOR;
			case INFERRED:
				return INFERRED_COLOR;
			default:
				return UNKNOWN_COLOR;
		}
	}
}
