# Deathbank Sentinel — dev client testing criteria

Run via `./gradlew runPlugin` (developer mode). Hespori (25k fee, Farming Guild) is the
budget test dummy for anything requiring a real death. Check items off per build.

## A. Detection signals

- [ ] **A1 — Login message (bank exists).** With items in any deathbank, log in.
      Infobox appears within a tick of the `items stored in an item retrieval service`
      game message; confidence shows *verified*.
      → While here: **copy the exact full message text into this file** (we match on a
      substring; the full wording + location phrasing is undocumented).
- [ ] **A2 — Login reconciliation (bank gone).** Claim the bank on another client
      (or mobile), then log in on the dev client with stale "active" saved state.
      After ~15s (25 ticks) with no login message, the infobox clears. This is the
      phantom-bank fix DWMS lacks — the single most important test.
- [ ] **A3 — Reconciliation respects the in-game toggle.** Disable the retrieval
      login warning in OSRS settings (varbit 14194 = 1), log in with saved active
      state. State must NOT clear — it downgrades to *unknown* (orange infobox text).
- [ ] **A4 — Death creates an inferred bank.** Die at Hespori with junk. Infobox
      appears immediately: yellow (*inferred*) text, `~N` stack count from the
      pre-death snapshot.
- [ ] **A5 — Safe deaths ignored.** Die in a safe region (NMZ, Clan Wars, CoX) with
      an active deathbank. No wipe is recorded; infobox unchanged.
- [ ] **A6 — Unsafe death wipes.** With an active deathbank, die to a gravestone
      death elsewhere (not at an IRS). Wipe notification fires; infobox clears.
      Also verify the `You have died again ... lost the items` message path — record
      its exact wording here.

## B. Retrieval interface (ground truth)

- [ ] **B1 — Contents verified on open.** Open the retrieval chest (Hespori → Arno).
      Item count in the infobox switches from `~N` yellow to exact `N` white
      (*verified*). Debug log prints the window title — **record the title text per
      service here** (it's how we'll label which bank without region math).
- [ ] **B2 — Partial withdrawal tracked.** Take some items out while the window is
      open; count updates live.
- [ ] **B3 — Emptying clears state.** Withdraw everything; infobox disappears and
      state persists as inactive across a relog.
- [ ] **B4 — 602 vs 672 disambiguation.** Die with a normal gravestone, open the
      grave. The plugin must NOT treat grave contents as a deathbank (debug log
      shows "Ignoring gravestone container update"). Confirm with the widget
      inspector which group the grave uses (672 expected) vs the deathbank chest
      (602 expected) — the two sources disagreed; record the answer here.

## C. Zulrah special case (dialog-only)

- [ ] **C1 — Priestess confirms bank.** With a Zulrah deathbank, talk to
      Zul-Gwenwynig. Dialog mentioning stuff left at the shrine marks state
      verified-active; after "I've returned it to you now", state clears.
- [ ] **C2 — Priestess confirms empty.** "I don't have anything for you to collect"
      clears an active state.

## D. Damage warning

- [ ] **D1 — Fires on damage.** With an active bank, take any hit. Notification
      fires with the configured flash/sound/focus behavior.
- [ ] **D2 — Cooldown respected.** Sustained damage warns once per cooldown window
      (default 50 ticks), not per hit.
- [ ] **D3 — Silent in safe regions.** Taking damage in NMZ with an active bank
      does not warn.
- [ ] **D4 — Force focus works.** Set the notification's request-focus to FORCE,
      unfocus the client, take damage — the client grabs focus.

## E. Persistence & lifecycle

- [ ] **E1 — Survives relog.** Active inferred state persists across logout/login;
      on load, a previously *verified* state is downgraded to *unknown* until a
      fresh signal confirms it (then A1 re-verifies it within seconds).
- [ ] **E2 — Per-profile isolation.** Two accounts don't see each other's state.
- [ ] **E3 — Plugin toggle.** Disabling and re-enabling the plugin restores state
      and infobox without duplicating infoboxes.
- [ ] **E4 — World hop.** Hopping worlds neither clears state nor falsely
      reconciles. Note whether the retrieval login message re-fires on hop — if it
      does, we can reconcile on hops too (record the answer).

## Open data to capture while testing

| Needed | Where to get it | Status |
|---|---|---|
| Exact login warning text | A1 | ☐ |
| Exact died-again wipe message text | A6 | ☐ |
| Retrieval window title per service | B1, each service | ☐ |
| Grave vs deathbank widget group IDs | B4, widget inspector | ☐ |
| Chest/NPC object IDs per service (for phase 2 recoloring) | dev tools object inspector at each chest | ☐ |
| Whether The Mimic / quest services hit the region fallback | die there (cheap quest replays not possible — low priority) | ☐ |
