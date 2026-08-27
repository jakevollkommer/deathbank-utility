package com.deathbankutility;

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

	private static final int MAX_SHORT_LABEL_CHARS = 10;

	private boolean active;
	private Confidence confidence = Confidence.UNKNOWN;
	private RetrievalService service;
	private String windowTitle;
	private List<ItemStack> items = List.of();
	private boolean itemsVerified;

	static DeathbankState unknown()
	{
		return inactive(Confidence.UNKNOWN);
	}

	static DeathbankState inactive(Confidence confidence)
	{
		DeathbankState state = new DeathbankState();
		state.confidence = confidence;
		return state;
	}

	static DeathbankState active(Confidence confidence, RetrievalService service, String windowTitle,
		List<ItemStack> items, boolean itemsVerified)
	{
		DeathbankState state = new DeathbankState();
		state.active = true;
		state.confidence = confidence;
		state.service = service;
		state.windowTitle = windowTitle;
		state.items = items;
		state.itemsVerified = itemsVerified;
		return state;
	}

	DeathbankState withConfidence(Confidence newConfidence)
	{
		DeathbankState copy = new DeathbankState();
		copy.active = active;
		copy.confidence = newConfidence;
		copy.service = service;
		copy.windowTitle = windowTitle;
		copy.items = items;
		copy.itemsVerified = itemsVerified;
		return copy;
	}

	boolean isLocationKnown()
	{
		return service != null || hasWindowTitle();
	}

	String displayLabel()
	{
		if (service != null)
		{
			return service.getDisplayName();
		}
		return hasWindowTitle() ? windowTitle.trim() : "location unknown";
	}

	String shortLabel()
	{
		if (service != null)
		{
			return service.getShortName();
		}
		return hasWindowTitle() ? truncate(windowTitle.trim()) : "?";
	}

	private boolean hasWindowTitle()
	{
		return windowTitle != null && !windowTitle.trim().isEmpty();
	}

	private static String truncate(String label)
	{
		return label.length() <= MAX_SHORT_LABEL_CHARS ? label : label.substring(0, MAX_SHORT_LABEL_CHARS);
	}
}
