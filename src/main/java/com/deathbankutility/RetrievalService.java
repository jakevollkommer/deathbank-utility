package com.deathbankutility;

import java.util.Arrays;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import lombok.Getter;

/**
 * The complete list of permanent item retrieval services, per the OSRS Wiki.
 * Region IDs identify where an unsafe death routes items into each service.
 */
@Getter
public enum RetrievalService
{
	ZULRAH("Zulrah", "Zulrah", "100k (free under 50 KC, always free for UIM)", 9007, 9008),
	VORKATH("Vorkath", "Vorkath", "100k", 9023),
	NEX("Nex", "Nex", "100k", 11601),
	THEATRE_OF_BLOOD("Theatre of Blood", "ToB", "100k (free in Entry Mode)",
		12611, 12612, 12613, 12867, 12869, 13122, 13123, 13125, 13379),
	TOMBS_OF_AMASCUT("Tombs of Amascut", "ToA", "GE-based, up to 500k",
		14160, 14162, 14164, 14674, 14676, 15184, 15186, 15188, 15696, 15698, 15700),
	ALCHEMICAL_HYDRA("Alchemical Hydra", "Hydra", "100k", 5536),
	GROTESQUE_GUARDIANS("Grotesque Guardians", "Gargs", "50k", 6727),
	// The Nightmare and Phosani's share a region and can't be told apart at death time
	NIGHTMARE("The Nightmare", "Nightmare", "60k", 15515),
	HALLOWED_SEPULCHRE("Hallowed Sepulchre", "Sepulchre", "25k",
		8797, 9051, 9052, 9053, 9054, 9309, 9563, 9565, 9821, 10074, 10075, 10077),
	HESPORI("Hespori", "Hespori", "25k", 5021),
	VOLCANIC_MINE("Volcanic Mine", "Volc Mine", "150 numulite", 15263, 15262),
	// Instanced; region unconfirmed — deaths there are still caught by chat/interface signals
	THE_MIMIC("The Mimic", "Mimic", "90k");

	private final String displayName;
	private final String shortName;
	private final String feeText;
	private final Set<Integer> regionIds;

	RetrievalService(String displayName, String shortName, String feeText, int... regionIds)
	{
		this.displayName = displayName;
		this.shortName = shortName;
		this.feeText = feeText;
		this.regionIds = IntStream.of(regionIds).boxed().collect(Collectors.toUnmodifiableSet());
	}

	static Optional<RetrievalService> fromRegion(int regionId)
	{
		return Arrays.stream(values())
			.filter(service -> service.regionIds.contains(regionId))
			.findFirst();
	}

	/**
	 * Matches a stored service name (our display name, or a retrieval window
	 * title mentioning it) back to a service, for the indicator's short label.
	 */
	static Optional<RetrievalService> fromName(String name)
	{
		if (name == null)
		{
			return Optional.empty();
		}
		String lowered = name.toLowerCase();
		return Arrays.stream(values())
			.filter(service -> lowered.contains(service.displayName.toLowerCase()))
			.findFirst();
	}
}
