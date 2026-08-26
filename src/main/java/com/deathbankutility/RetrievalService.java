package com.deathbankutility;

import java.util.Arrays;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.runelite.api.gameval.NpcID;
import net.runelite.api.gameval.ObjectID;

/**
 * The complete list of permanent item retrieval services, per the OSRS Wiki.
 * Each service owns its death regions, its claim point (chest object or NPC),
 * and where claiming happens outside an interface (Zulrah's dialog).
 */
@RequiredArgsConstructor
@Getter
public enum RetrievalService
{
	// Claim point: Priestess Zul-Gwenwynig in Zul-Andra, dialog only (no interface)
	ZULRAH("Zulrah", "Zulrah", "100k (free under 50 KC, always free for UIM)",
		ids(9007, 9008),
		ids(8495, 8751),
		ids(),
		ids(NpcID.SNAKEBOSS_PRIEST_1OP, NpcID.SNAKEBOSS_PRIEST_2OPS)),

	// Claim point: Torfinn on Ungael or in Rellekka
	VORKATH("Vorkath", "Vorkath", "100k",
		ids(9023),
		ids(),
		ids(),
		ids(NpcID.TORFINN_RELLEKKA, NpcID.TORFINN_UNGAEL, NpcID.TORFINN_NO_TRAVEL,
			NpcID.TORFINN_TRAVEL_RELLEKKA, NpcID.TORFINN_TRAVEL_UNGAEL,
			NpcID.TORFINN_COLLECT_RELLEKKA, NpcID.TORFINN_COLLECT_UNGAEL)),

	// Claim point: chest past the frozen door
	NEX("Nex", "Nex", "100k",
		ids(11601),
		ids(),
		ids(42854, 42858), // NEX_GRAVESTONE_CHEST (+_NOOP), gameval class is package-private
		ids()),

	// Claim point: chest north of the Ver Sinhaza bank
	THEATRE_OF_BLOOD("Theatre of Blood", "ToB", "100k (free in Entry Mode)",
		ids(12611, 12612, 12613, 12867, 12869, 13122, 13123, 13125, 13379),
		ids(),
		ids(32656), // TOB_SURFACE_GRAVESTONE_CHEST
		ids()),

	// Claim point: lobby chest
	TOMBS_OF_AMASCUT("Tombs of Amascut", "ToA", "GE-based, up to 500k",
		ids(14160, 14162, 14164, 14674, 14676, 15184, 15186, 15188, 15696, 15698, 15700),
		ids(),
		ids(46078, 46079), // TOA_LOBBY_GRAVESTONE_CHEST (+_NOOP)
		ids()),

	// Claim point: Orrvor quo Maten
	ALCHEMICAL_HYDRA("Alchemical Hydra", "Hydra", "100k",
		ids(5536),
		ids(),
		ids(),
		ids(NpcID.KAHLITH_ALCHEMICAL_HUNTER)),

	// Claim point: magical chest outside the roof entrance
	GROTESQUE_GUARDIANS("Grotesque Guardians", "Gargs", "50k",
		ids(6727),
		ids(),
		ids(ObjectID.GARGBOSS_GRAVESTONE_RETRIEVAL),
		ids()),

	// The Nightmare and Phosani's share a region and can't be told apart at death
	// time; claim points: Shura and Sister Senga
	NIGHTMARE("The Nightmare", "Nightmare", "60k",
		ids(15515),
		ids(),
		ids(),
		ids(NpcID.SHURA, NpcID.SHURA_1OP, NpcID.SHURA_2OP,
			NpcID.NIGHTMARE_CHALLENGE_SISTER, NpcID.NIGHTMARE_CHALLENGE_SISTER_1OP, NpcID.NIGHTMARE_CHALLENGE_SISTER_2OP)),

	// Claim point: Mysterious Stranger in the lobby
	HALLOWED_SEPULCHRE("Hallowed Sepulchre", "Sepulchre", "25k",
		ids(8797, 9051, 9052, 9053, 9054, 9309, 9563, 9565, 9821, 10074, 10075, 10077),
		ids(),
		ids(),
		ids(NpcID.HALLOWED_LOBBY_NPC_1OP, NpcID.HALLOWED_LOBBY_NPC_3OP)),

	// Claim point: Arno at the Farming Guild
	HESPORI("Hespori", "Hespori", "25k",
		ids(5021),
		ids(),
		ids(),
		ids(NpcID.FARMING_GUILD_HESPORI_FARMER)),

	// Claim point: Petrified Pete
	VOLCANIC_MINE("Volcanic Mine", "Volc Mine", "150 numulite",
		ids(15263, 15262),
		ids(),
		ids(),
		ids(NpcID.FOSSIL_MINEGUARD)),

	// Instanced; death region unconfirmed — deaths there are still caught by
	// chat/interface signals. Claim point: the Strange casket
	THE_MIMIC("The Mimic", "Mimic", "90k",
		ids(),
		ids(),
		ids(34733), // TRAIL_MIMIC_ENABLER
		ids());

	private final String displayName;
	private final String shortName;
	private final String feeText;
	/** Regions where an unsafe death routes items into this service. */
	private final Set<Integer> deathRegionIds;
	/** Regions where claiming happens outside an interface (Zulrah's dialog). */
	private final Set<Integer> claimRegionIds;
	private final Set<Integer> claimObjectIds;
	private final Set<Integer> claimNpcIds;

	private static final Set<Integer> ALL_CLAIM_OBJECT_IDS = unionOf(RetrievalService::getClaimObjectIds);
	private static final Set<Integer> ALL_CLAIM_NPC_IDS = unionOf(RetrievalService::getClaimNpcIds);

	static Optional<RetrievalService> fromRegion(int regionId)
	{
		return Arrays.stream(values())
			.filter(service -> service.deathRegionIds.contains(regionId))
			.findFirst();
	}

	/**
	 * Matches a stored service name (our display name, or a retrieval window
	 * title mentioning it) back to a service.
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

	static boolean isClaimObject(int objectId)
	{
		return ALL_CLAIM_OBJECT_IDS.contains(objectId);
	}

	static boolean isClaimNpc(int npcId)
	{
		return ALL_CLAIM_NPC_IDS.contains(npcId);
	}

	private static Set<Integer> ids(int... ids)
	{
		return IntStream.of(ids).boxed().collect(Collectors.toUnmodifiableSet());
	}

	private static Set<Integer> unionOf(Function<RetrievalService, Set<Integer>> idsPerService)
	{
		return Arrays.stream(values())
			.map(idsPerService)
			.flatMap(Set::stream)
			.collect(Collectors.toUnmodifiableSet());
	}
}
