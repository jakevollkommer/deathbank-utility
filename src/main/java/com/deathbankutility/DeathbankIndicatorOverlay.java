package com.deathbankutility;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.InfoBoxComponent;
import net.runelite.client.ui.overlay.components.TextComponent;
import net.runelite.client.ui.overlay.tooltip.Tooltip;
import net.runelite.client.ui.overlay.tooltip.TooltipManager;

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
	// Bottom-right corner of the box is plain background, clear of the skull
	private static final int COUNT_INSET = 3;

	private final Client client;
	private final DeathbankUtilityPlugin plugin;
	private final DeathbankUtilityConfig config;
	private final TooltipManager tooltipManager;

	@Inject
	DeathbankIndicatorOverlay(Client client, DeathbankUtilityPlugin plugin, DeathbankUtilityConfig config,
		TooltipManager tooltipManager)
	{
		this.client = client;
		this.plugin = plugin;
		this.config = config;
		this.tooltipManager = tooltipManager;
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

		// The component centers its caption over the icon, where the pale skull
		// swallows it, so the count is drawn separately in the clear corner
		InfoBoxComponent box = new InfoBoxComponent();
		box.setImage(plugin.getIndicatorIcon());
		box.setBackgroundColor(BACKGROUND);
		box.setPreferredSize(BOX_DIMENSION);
		box.render(graphics);

		renderCount(graphics, countText(state), state.getConfidence().getColor());
		renderLocationLabel(graphics, state.shortLabel());
		addTooltipWhenHovered(box.getBounds(), state);
		return new Dimension(BOX_SIZE, BOX_SIZE + LABEL_HEIGHT);
	}

	private void addTooltipWhenHovered(Rectangle boxBounds, DeathbankState state)
	{
		Rectangle hitbox = new Rectangle(boxBounds);
		hitbox.translate(getBounds().x, getBounds().y);
		hitbox.height += LABEL_HEIGHT;

		net.runelite.api.Point mouse = client.getMouseCanvasPosition();
		if (!hitbox.contains(mouse.getX(), mouse.getY()))
		{
			return;
		}
		tooltipManager.add(new Tooltip(buildTooltip(state)));
	}

	private static String buildTooltip(DeathbankState state)
	{
		boolean stateIsUncertain = state.getConfidence() == Confidence.UNKNOWN;
		String verb = stateIsUncertain ? "Deathbank possibly active" : "Active deathbank";
		String heading = state.isLocationKnown()
			? verb + ": " + state.displayLabel()
			: verb + " (location unknown, open the chest to identify it)";
		return heading + "</br>" + describeContents(state)
			+ "</br>Any unsafe death anywhere deletes these items.";
	}

	private static String describeContents(DeathbankState state)
	{
		int stackCount = state.getItems().size();
		if (stackCount == 0)
		{
			return "Contents unknown, open the retrieval chest to check.";
		}

		String itemWord = stackCount == 1 ? "item" : "items";
		String basis = state.isItemsVerified() ? "verified" : "estimated";
		String qualifier = state.isItemsVerified() ? "" : "~";
		return qualifier + stackCount + " " + itemWord + " (" + basis + ")";
	}

	private static void renderCount(Graphics2D graphics, String count, Color color)
	{
		graphics.setFont(FontManager.getRunescapeBoldFont());
		FontMetrics metrics = graphics.getFontMetrics();

		TextComponent text = new TextComponent();
		text.setText(count);
		text.setColor(color);
		text.setOutline(true);
		text.setPosition(new Point(BOX_SIZE - COUNT_INSET - metrics.stringWidth(count), BOX_SIZE - COUNT_INSET));
		text.render(graphics);
	}

	private static void renderLocationLabel(Graphics2D graphics, String label)
	{
		graphics.setFont(FontManager.getRunescapeSmallFont());
		FontMetrics metrics = graphics.getFontMetrics();

		TextComponent text = new TextComponent();
		text.setText(label);
		text.setColor(Color.WHITE);
		text.setOutline(true);
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
