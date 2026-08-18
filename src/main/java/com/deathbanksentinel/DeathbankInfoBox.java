package com.deathbanksentinel;

import java.awt.Color;
import java.awt.image.BufferedImage;
import net.runelite.client.ui.overlay.infobox.InfoBox;
import net.runelite.client.ui.overlay.infobox.InfoBoxPriority;

/**
 * Persistent indicator shown while a deathbank is (or may be) active.
 * No countdown — retrieval services never expire; they only get claimed or wiped.
 */
class DeathbankInfoBox extends InfoBox
{
	private static final Color VERIFIED_COLOR = Color.WHITE;
	private static final Color INFERRED_COLOR = Color.YELLOW;
	private static final Color UNKNOWN_COLOR = Color.ORANGE;

	private final DeathbankSentinelPlugin plugin;

	DeathbankInfoBox(BufferedImage image, DeathbankSentinelPlugin plugin)
	{
		super(image, plugin);
		this.plugin = plugin;
		setPriority(InfoBoxPriority.HIGH);
	}

	@Override
	public String getText()
	{
		DeathbankState state = plugin.getState();
		boolean countIsTrustworthy = state.isItemsVerified() && !state.getItems().isEmpty();
		if (countIsTrustworthy)
		{
			return Integer.toString(state.getItems().size());
		}
		return state.getItems().isEmpty() ? "!" : "~" + state.getItems().size();
	}

	@Override
	public Color getTextColor()
	{
		switch (plugin.getState().getConfidence())
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
