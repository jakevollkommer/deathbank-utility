package com.deathbankutility;

import java.awt.Color;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * How the plugin learned the current deathbank state. The UI always shows this
 * tier — an inference is never presented as a fact.
 */
@RequiredArgsConstructor
@Getter
public enum Confidence
{
	/** Confirmed by a server signal: the retrieval interface, or a game message. */
	VERIFIED("Verified", Color.WHITE),
	/** Deduced from a local-player death and region mapping. */
	INFERRED("Inferred", Color.YELLOW),
	/** Carried over from a previous session with no confirming signal yet. */
	UNKNOWN("Unknown", Color.ORANGE);

	private final String label;
	private final Color color;
}
