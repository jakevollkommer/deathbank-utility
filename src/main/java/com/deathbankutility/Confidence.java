package com.deathbankutility;

/**
 * How the plugin learned the current deathbank state. The UI always shows this
 * tier — an inference is never presented as a fact.
 */
public enum Confidence
{
	/** Confirmed by a server signal: the retrieval interface, or a game message. */
	VERIFIED,
	/** Deduced from a local-player death and region mapping. */
	INFERRED,
	/** Carried over from a previous session with no confirming signal yet. */
	UNKNOWN
}
