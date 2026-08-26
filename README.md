# Deathbank Utility

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

## Early release

This plugin is an early release and not feature complete. Deathbank state is inferred
from game signals and can be wrong in both directions. Never treat a missing warning
as proof you have no deathbank, especially on an ultimate ironman. Bug
reports and feature requests are very welcome on the
[issues page](https://github.com/jakevollkommer/deathbank-utility/issues), also
reachable from the plugin config's Feedback section.

Roadmap: optional blocking of dangerous content while a bank is active, and reclaim
fees in the side panel. Testing criteria for dev-client sign-off live in
[TESTING.md](TESTING.md).

## Development

```
./gradlew runPlugin   # launches RuneLite in developer mode with the plugin loaded
./gradlew jar         # builds the sideloadable jar
```
