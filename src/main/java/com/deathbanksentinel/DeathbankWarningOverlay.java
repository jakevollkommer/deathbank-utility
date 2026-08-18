package com.deathbanksentinel;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.TextComponent;

/**
 * Big flashing DEATHBANK WARNING text when taking damage with an active
 * deathbank, in the style of on-screen EAT warnings.
 */
class DeathbankWarningOverlay extends Overlay
{
	private static final String WARNING_TEXT = "DEATHBANK WARNING";
	private static final Color WARNING_COLOR = new Color(255, 30, 30);
	private static final Font WARNING_FONT = FontManager.getRunescapeBoldFont().deriveFont(Font.BOLD, 48f);
	private static final int FLASH_CYCLE_CLIENT_TICKS = 20;

	private final Client client;
	private final DeathbankSentinelPlugin plugin;
	private final DeathbankSentinelConfig config;

	@Inject
	DeathbankWarningOverlay(Client client, DeathbankSentinelPlugin plugin, DeathbankSentinelConfig config)
	{
		this.client = client;
		this.plugin = plugin;
		this.config = config;
		setPosition(OverlayPosition.DYNAMIC);
		setLayer(OverlayLayer.ABOVE_WIDGETS);
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		boolean warningActive = config.showDamageText() && plugin.isDamageWarningActive();
		if (!warningActive)
		{
			return null;
		}

		boolean flashOff = client.getGameCycle() % FLASH_CYCLE_CLIENT_TICKS >= FLASH_CYCLE_CLIENT_TICKS / 2;
		if (flashOff)
		{
			return null;
		}

		graphics.setFont(WARNING_FONT);
		FontMetrics metrics = graphics.getFontMetrics();
		int x = (client.getCanvasWidth() - metrics.stringWidth(WARNING_TEXT)) / 2;
		int y = client.getCanvasHeight() / 4;

		TextComponent text = new TextComponent();
		text.setText(WARNING_TEXT);
		text.setColor(WARNING_COLOR);
		text.setFont(WARNING_FONT);
		text.setOutline(true);
		text.setPosition(new java.awt.Point(x, y));
		return text.render(graphics);
	}
}
