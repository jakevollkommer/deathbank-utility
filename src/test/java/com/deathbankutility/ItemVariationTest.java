package com.deathbankutility;

import net.runelite.client.game.ItemVariationMapping;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

/**
 * The death diff compares by item variation so that drinking a dose on the tick
 * you die is not mistaken for that dose being banked. That only holds if the
 * mapping really does collapse doses, so it is pinned here.
 */
public class ItemVariationTest
{
	private static final int SARADOMIN_BREW_4 = 6685;
	private static final int SARADOMIN_BREW_3 = 6687;
	private static final int SARADOMIN_BREW_2 = 6689;
	private static final int SARADOMIN_BREW_1 = 6691;
	private static final int SUPER_RESTORE_1 = 3030;
	private static final int SHARK = 385;

	@Test
	public void dosesOfTheSameBrewShareAVariation()
	{
		int brew = ItemVariationMapping.map(SARADOMIN_BREW_4);
		assertEquals(brew, ItemVariationMapping.map(SARADOMIN_BREW_3));
		assertEquals(brew, ItemVariationMapping.map(SARADOMIN_BREW_2));
		assertEquals(brew, ItemVariationMapping.map(SARADOMIN_BREW_1));
	}

	@Test
	public void differentItemsKeepDifferentVariations()
	{
		assertNotEquals(ItemVariationMapping.map(SARADOMIN_BREW_4), ItemVariationMapping.map(SUPER_RESTORE_1));
		assertNotEquals(ItemVariationMapping.map(SARADOMIN_BREW_4), ItemVariationMapping.map(SHARK));
	}
}
