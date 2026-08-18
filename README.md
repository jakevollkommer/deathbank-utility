# Deathbank Sentinel

RuneLite plugin that tracks OSRS item retrieval services (deathbanks — Zulrah, Vorkath,
Nex, ToB, ToA, Hydra, and friends) and gets loud before you lose what's inside.

Items in a retrieval service are deleted on **any unsafe death anywhere** — a fact the
game only mentions in an easily-missed login message. This plugin:

- shows an **infobox** while a deathbank is (or may be) active, with an honest
  confidence tier: *verified* (white) / *inferred* (yellow) / *unknown* (orange)
- **remembers what went in** — estimated from your pre-death inventory, upgraded to
  exact contents whenever you open the retrieval interface
- **reconciles at login**: if the game's retrieval warning is enabled and doesn't
  appear, a stale saved deathbank is cleared instead of lying forever
- **warns on damage taken** while a bank is active (configurable screen flash, sound,
  and client focus request), staying quiet in safe content (NMZ, CoX, LMS, ...)
- notifies when a second unsafe death **wipes** the bank

Unlike prior art, inferences are never presented as facts — every state carries how
the plugin learned it.

Roadmap: retrieval chest/NPC highlighting (phase 2), a side panel with the item grid
and reclaim fee, and optional blocking of dangerous content while a bank is active
(phase 3). Testing criteria for dev-client sign-off live in [TESTING.md](TESTING.md).

## Development

```
./gradlew runPlugin   # launches RuneLite in developer mode with the plugin loaded
./gradlew jar         # builds the sideloadable jar
```
