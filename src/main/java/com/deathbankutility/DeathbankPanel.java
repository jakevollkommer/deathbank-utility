package com.deathbankutility;

import java.awt.Color;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import lombok.RequiredArgsConstructor;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.PluginPanel;
import javax.swing.ImageIcon;
import javax.swing.SwingUtilities;
import net.runelite.api.gameval.ItemID;
import net.runelite.client.game.ItemManager;
import net.runelite.client.util.AsyncBufferedImage;
import net.runelite.client.util.ImageUtil;
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
	// Panel width less our borders, and less the margins Swing's HTML body adds on
	// top of the declared width, which is what pushed text past the panel edge
	private static final int TEXT_WRAP_WIDTH = PANEL_WIDTH - 60;
	private static final Dimension MAX_ROW = new Dimension(PANEL_WIDTH - 20, Integer.MAX_VALUE);
	private static final Color LOOTING_BAG_COLOR = new Color(0xFFD400);

	private final JLabel statusLabel = new JLabel();
	private final JLabel detailLabel = new JLabel();
	private final JLabel contentsLabel = new JLabel();
	private final JPanel itemGrid = new JPanel();
	private final JLabel lootingBagHeading = new JLabel();
	private final JPanel lootingBagGrid = new JPanel();
	private JPanel lootingBagSection;

	DeathbankPanel(ItemManager itemManager)
	{
		// The bag's own icon beside the heading, so the section names itself
		AsyncBufferedImage lootingBagIcon = itemManager.getImage(ItemID.LOOTING_BAG);
		lootingBagIcon.onLoaded(() -> SwingUtilities.invokeLater(() ->
		{
			lootingBagHeading.setIcon(new ImageIcon(ImageUtil.resizeImage(lootingBagIcon, 16, 16)));
			lootingBagHeading.repaint();
		}));

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
		lootingBagGrid.setLayout(new GridLayout(0, ITEMS_PER_ROW, 3, 3));
		lootingBagHeading.setFont(FontManager.getRunescapeSmallFont());
		lootingBagHeading.setForeground(LOOTING_BAG_COLOR);
		lootingBagHeading.setBorder(BorderFactory.createEmptyBorder(0, 0, 5, 0));
		lootingBagHeading.setIconTextGap(5);

		// The looting bag half is boxed and tinted so it reads as its own group to
		// re-bag, rather than more items in the same pile
		lootingBagSection = new JPanel(new BorderLayout());
		lootingBagSection.setBorder(BorderFactory.createCompoundBorder(
			BorderFactory.createEmptyBorder(10, 0, 4, 2),
			BorderFactory.createCompoundBorder(
				BorderFactory.createMatteBorder(1, 1, 1, 1, LOOTING_BAG_COLOR),
				BorderFactory.createEmptyBorder(6, 6, 6, 6))));
		lootingBagSection.add(lootingBagHeading, BorderLayout.NORTH);
		lootingBagSection.add(lootingBagGrid, BorderLayout.CENTER);
		lootingBagSection.setVisible(false);

		JPanel grids = new JPanel();
		grids.setLayout(new BoxLayout(grids, BoxLayout.Y_AXIS));
		grids.add(itemGrid);
		grids.add(lootingBagSection);
		grids.setMaximumSize(MAX_ROW);

		JPanel gridWrapper = new JPanel(new BorderLayout());
		gridWrapper.add(grids, BorderLayout.NORTH);

		// BoxLayout will not stretch a bare JLabel, so the disclaimer needs a
		// full-width wrapper and an explicit wrap width to align with the rest
		JLabel disclaimer = new JLabel(wrapped("Early release: tracking can be wrong. Never treat a missing "
			+ "warning as proof you have no deathbank. Report bugs and request features from the plugin "
			+ "config's Feedback section."));
		disclaimer.setFont(FontManager.getRunescapeSmallFont());
		disclaimer.setForeground(ColorScheme.MEDIUM_GRAY_COLOR);
		disclaimer.setVerticalAlignment(SwingConstants.TOP);

		// Same container type as the header: BorderLayout let the disclaimer force the
		// panel wider than the viewport, which clipped every label on the right
		JPanel footer = new JPanel(new GridLayout(0, 1));
		footer.setBorder(BorderFactory.createEmptyBorder(12, 0, 0, 0));
		footer.add(disclaimer);
		footer.setAlignmentX(LEFT_ALIGNMENT);
		footer.setMaximumSize(MAX_ROW);

		header.setAlignmentX(LEFT_ALIGNMENT);
		header.setMaximumSize(MAX_ROW);
		gridWrapper.setAlignmentX(LEFT_ALIGNMENT);
		gridWrapper.setMaximumSize(MAX_ROW);

		add(header);
		add(gridWrapper);
		add(footer);

		showInactive();
	}

	void update(DeathbankState state, List<PanelItem> items, List<PanelItem> lootingBagItems, boolean awaitingConfirmation)
	{
		if (awaitingConfirmation)
		{
			showChecking();
			return;
		}
		if (!state.isActive())
		{
			showInactive();
			return;
		}

		statusLabel.setText(wrapped("Deathbank active: " + state.displayLabel()));
		statusLabel.setForeground(ColorScheme.PROGRESS_ERROR_COLOR);
		detailLabel.setText(wrapped("Confidence: " + state.getConfidence().getLabel()
			+ ". Any unsafe death anywhere deletes these items." + feeSentence(state)));
		contentsLabel.setText(contentsText(state, items));

		// Split so the looting bag half can be re-bagged as a group rather than
		// picked out of one undifferentiated pile
		itemGrid.removeAll();
		items.forEach(item -> itemGrid.add(buildItemCell(item)));

		lootingBagGrid.removeAll();
		lootingBagItems.forEach(item -> lootingBagGrid.add(buildItemCell(item)));
		lootingBagHeading.setText("From your looting bag");
		lootingBagSection.setVisible(!lootingBagItems.isEmpty());

		itemGrid.revalidate();
		itemGrid.repaint();
		lootingBagGrid.revalidate();
		lootingBagGrid.repaint();
	}

	private void showChecking()
	{
		statusLabel.setText("Checking for a deathbank...");
		statusLabel.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		detailLabel.setText(wrapped("Waiting for the game to confirm whether anything is stored."));
		contentsLabel.setText("");
		clearGrids();
	}

	private void showInactive()
	{
		statusLabel.setText("No active deathbank");
		statusLabel.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		detailLabel.setText(wrapped("Die at content with an item retrieval service and it will show up here. "
			+ "Only a death that actually sends your items there counts."));
		contentsLabel.setText("");
		clearGrids();
	}

	private void clearGrids()
	{
		itemGrid.removeAll();
		lootingBagGrid.removeAll();
		lootingBagSection.setVisible(false);
		itemGrid.revalidate();
		itemGrid.repaint();
		lootingBagGrid.revalidate();
		lootingBagGrid.repaint();
	}

	private static String wrapped(String text)
	{
		return "<html><body style='margin:0;width:" + TEXT_WRAP_WIDTH + "px'>" + text + "</body></html>";
	}

	/**
	 * An estimate cannot include the looting bag unless the client has been shown its
	 * contents, so say so rather than letting the count look complete.
	 */
	private static String unknownLootingBagWarning(DeathbankState state)
	{
		// Contents read from the chest are the whole truth, bag included, so the
		// caveat only applies while the list is still an estimate
		if (state.isItemsVerified() || !state.getLootingBagItems().isEmpty())
		{
			return "";
		}
		return " Anything in your looting bag is not counted here, the game never showed it to the client.";
	}

	private static String describeStacks(int stackCount)
	{
		return stackCount + (stackCount == 1 ? " item stack." : " item stacks.");
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
			return wrapped("Contents unknown, open the retrieval chest to verify.");
		}
		String basis = state.isItemsVerified()
			? "Verified from the retrieval interface:"
			: "Estimated from your gear at death:";
		return wrapped(basis + " " + describeStacks(items.size()) + unknownLootingBagWarning(state));
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
