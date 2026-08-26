package com.deathbankutility;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import lombok.RequiredArgsConstructor;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.PluginPanel;
import net.runelite.client.util.AsyncBufferedImage;
import net.runelite.client.util.QuantityFormatter;

/**
 * Side panel showing the current deathbank: status, confidence, and the
 * item grid (estimated until verified through the retrieval interface).
 */
class DeathbankPanel extends PluginPanel
{
	@RequiredArgsConstructor
	static class PanelItem
	{
		final String name;
		final int quantity;
		final AsyncBufferedImage image;
	}

	private static final int ITEMS_PER_ROW = 5;

	private final JLabel statusLabel = new JLabel();
	private final JLabel detailLabel = new JLabel();
	private final JLabel contentsLabel = new JLabel();
	private final JPanel itemGrid = new JPanel();

	DeathbankPanel()
	{
		setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

		statusLabel.setFont(FontManager.getRunescapeBoldFont());
		detailLabel.setFont(FontManager.getRunescapeSmallFont());
		contentsLabel.setFont(FontManager.getRunescapeSmallFont());
		contentsLabel.setForeground(ColorScheme.LIGHT_GRAY_COLOR);

		JPanel header = new JPanel(new GridLayout(0, 1, 0, 4));
		header.setBorder(BorderFactory.createEmptyBorder(0, 0, 8, 0));
		header.add(statusLabel);
		header.add(detailLabel);
		header.add(contentsLabel);

		itemGrid.setLayout(new GridLayout(0, ITEMS_PER_ROW, 3, 3));

		JPanel gridWrapper = new JPanel(new BorderLayout());
		gridWrapper.add(itemGrid, BorderLayout.NORTH);

		JLabel disclaimer = new JLabel("<html>Early release: tracking can be wrong. Never treat a missing "
			+ "warning as proof you have no deathbank. "
			+ "Report bugs and request features from the plugin config's Feedback section.</html>");
		disclaimer.setFont(FontManager.getRunescapeSmallFont());
		disclaimer.setForeground(ColorScheme.MEDIUM_GRAY_COLOR);
		disclaimer.setBorder(BorderFactory.createEmptyBorder(12, 0, 0, 0));

		add(header);
		add(gridWrapper);
		add(disclaimer);

		showInactive();
	}

	void update(DeathbankState state, List<PanelItem> items)
	{
		if (!state.isActive())
		{
			showInactive();
			return;
		}

		statusLabel.setText("<html>Deathbank active: " + state.displayLabel() + "</html>");
		statusLabel.setForeground(ColorScheme.PROGRESS_ERROR_COLOR);
		detailLabel.setText("<html>Confidence: " + state.getConfidence().getLabel()
			+ ". Any unsafe death anywhere deletes these items." + feeSentence(state) + "</html>");
		contentsLabel.setText(contentsText(state, items));

		itemGrid.removeAll();
		items.forEach(item -> itemGrid.add(buildItemCell(item)));
		itemGrid.revalidate();
		itemGrid.repaint();
	}

	private void showInactive()
	{
		statusLabel.setText("No active deathbank");
		statusLabel.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		detailLabel.setText("<html>Die at content with an item retrieval service and it will show up here.</html>");
		contentsLabel.setText("");
		itemGrid.removeAll();
		itemGrid.revalidate();
		itemGrid.repaint();
	}

	private static String feeSentence(DeathbankState state)
	{
		if (state.getService() == null)
		{
			return "";
		}
		return " Reclaim fee: " + state.getService().getFeeText() + ".";
	}

	private static String contentsText(DeathbankState state, List<PanelItem> items)
	{
		if (items.isEmpty())
		{
			return "Contents unknown — open the retrieval chest to verify.";
		}
		String basis = state.isItemsVerified()
			? "Verified from the retrieval interface:"
			: "Estimated from your gear at death:";
		return "<html>" + basis + " " + items.size() + " item stacks.</html>";
	}

	private static JLabel buildItemCell(PanelItem item)
	{
		JLabel cell = new JLabel();
		cell.setPreferredSize(new Dimension(36, 32));
		cell.setHorizontalAlignment(JLabel.CENTER);
		cell.setToolTipText(item.name + " x " + QuantityFormatter.formatNumber(item.quantity));
		item.image.addTo(cell);
		return cell;
	}
}
