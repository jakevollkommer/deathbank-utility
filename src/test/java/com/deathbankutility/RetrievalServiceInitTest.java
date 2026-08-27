package com.deathbankutility;

import net.runelite.api.gameval.NpcID;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Class-init smoke test for the hardcoded ID registry: a duplicate entry in any
 * Set.of would throw ExceptionInInitializerError at class load, which in the
 * client surfaces as an opaque crash instead of a test failure.
 */
public class RetrievalServiceInitTest
{
	@Test
	public void registryInitializesAndResolves()
	{
		assertEquals(13, RetrievalService.values().length);
		assertTrue(RetrievalService.isClaimNpc(NpcID.FOSSIL_MINEGUARD));
		assertTrue(RetrievalService.isClaimObject(32656));
		assertEquals(RetrievalService.HESPORI, RetrievalService.fromRegion(5021).orElse(null));
		assertEquals(RetrievalService.THEATRE_OF_BLOOD, RetrievalService.fromName("Theatre of Blood Item Retrieval Service").orElse(null));
		// Phosani's must not be swallowed by the plain Nightmare entry, or vice versa
		assertEquals(RetrievalService.PHOSANI, RetrievalService.fromName("Phosani's Nightmare Item Retrieval Service").orElse(null));
		assertEquals(RetrievalService.NIGHTMARE, RetrievalService.fromName("The Nightmare Item Retrieval Service").orElse(null));
		assertEquals(RetrievalService.NIGHTMARE, RetrievalService.fromRegion(15515).orElse(null));
		// Observed in game: window titles and death messages name the claim NPC,
		// not the boss, so aliases have to resolve them
		assertEquals(RetrievalService.NIGHTMARE, RetrievalService.fromName("Shura's Item Retrieval Service").orElse(null));
		assertEquals(RetrievalService.NIGHTMARE, RetrievalService.fromName(
			"Shura has retrieved some of your items. You can collect them from her in the Sisterhood Sanctuary.").orElse(null));
		assertEquals(RetrievalService.VORKATH, RetrievalService.fromName("Torfinn's Item Retrieval Service").orElse(null));
		assertEquals(RetrievalService.HESPORI, RetrievalService.fromName("Arno's Item Retrieval Service").orElse(null));
	}
}
