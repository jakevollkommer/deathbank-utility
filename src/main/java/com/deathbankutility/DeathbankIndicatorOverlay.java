package com.deathbankutility;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.image.BufferedImage;
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
	private static final Color VERIFIED_COLOR = Color.WHITE;
	private static final Color INFERRED_COLOR = Color.YELLOW;
	private static final Color UNKNOWN_COLOR = Color.ORANGE;
	private static final int BOX_SIZE = 40;
	private static final int LABEL_HEIGHT = 14;
	private static final int MAX_LABEL_CHARS = 10;

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

		BufferedImage icon = plugin.getIndicatorIcon();
		InfoBoxComponent box = new InfoBoxComponent();
		box.setImage(icon);
		box.setText(countText(state));
		box.setColor(confidenceColor(state));
		box.setBackgroundColor(BACKGROUND);
		box.setPreferredSize(new Dimension(BOX_SIZE, BOX_SIZE));
		box.render(graphics);

		renderLocationLabel(graphics, state);
		return new Dimension(BOX_SIZE, BOX_SIZE + LABEL_HEIGHT);
	}

	private static void renderLocationLabel(Graphics2D graphics, DeathbankState state)
	{
		graphics.setFont(FontManager.getRunescapeSmallFont());
		String label = locationLabel(state);
		FontMetrics metrics = graphics.getFontMetrics();

		TextComponent text = new TextComponent();
		text.setText(label);
		text.setColor(Color.WHITE);
		text.setPosition(new Point((BOX_SIZE - metrics.stringWidth(label)) / 2, BOX_SIZE + LABEL_HEIGHT - 3));
		text.render(graphics);
	}

	private static String locationLabel(DeathbankState state)
	{
		String serviceName = state.getServiceName();
		return RetrievalService.fromName(serviceName)
			.map(RetrievalService::getShortName)
			.orElseGet(() -> truncate(serviceName));
	}

	private static String truncate(String serviceName)
	{
		if (serviceName == null || serviceName.isEmpty())
		{
			return "?";
		}
		return serviceName.length() <= MAX_LABEL_CHARS ? serviceName : serviceName.substring(0, MAX_LABEL_CHARS);
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
