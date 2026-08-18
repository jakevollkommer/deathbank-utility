package com.deathbanksentinel;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Everything the plugin believes about the player's deathbank, persisted
 * per RuneScape profile as JSON.
 */
@Data
@NoArgsConstructor
public class DeathbankState
{
	@Data
	@NoArgsConstructor
	@AllArgsConstructor
	public static class ItemStack
	{
		private int id;
		private int quantity;
	}

	private boolean active;
	private Confidence confidence = Confidence.UNKNOWN;
	private String serviceName;
	private List<ItemStack> items = List.of();
	private boolean itemsVerified;
	private long lastVerifiedAtMillis;
	private long lastUpdatedAtMillis;

	static DeathbankState inactive(Confidence confidence)
	{
		DeathbankState state = new DeathbankState();
		state.confidence = confidence;
		state.touch();
		return state;
	}

	static DeathbankState active(Confidence confidence, String serviceName, List<ItemStack> items, boolean itemsVerified)
	{
		DeathbankState state = new DeathbankState();
		state.active = true;
		state.confidence = confidence;
		state.serviceName = serviceName;
		state.items = items;
		state.itemsVerified = itemsVerified;
		state.touch();
		return state;
	}

	void touch()
	{
		lastUpdatedAtMillis = System.currentTimeMillis();
		boolean confirmedByServer = confidence == Confidence.VERIFIED;
		if (confirmedByServer)
		{
			lastVerifiedAtMillis = lastUpdatedAtMillis;
		}
	}
}
