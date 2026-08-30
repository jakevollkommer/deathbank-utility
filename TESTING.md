# Deathbank Utility — dev client testing criteria

Run via `./gradlew runPlugin` (developer mode). Hespori (25k fee, Farming Guild) is the
budget test dummy for anything requiring a real death. Check items off per build.

## A. Detection signals

- [ ] **A1 — Login message (bank exists).** With items in any deathbank, log in.
      Infobox appears within a tick of the `items stored in an item retrieval service`
      game message; confidence shows *verified*.
      ✅ **CAPTURED 2026-08-27** (ToB deathbank, fresh login), exact text:
      `@mes_hl_red@You have items stored in an item retrieval service. Please visit
      the magical chest outside the Theatre of Blood. If you die again before
      retrieving them, they will be lost.`
      Two findings: the message names the location, so it is parsed to identify the
      service without opening the chest; and it carries an unresolved `@mes_hl_red@`
      formatting macro that `Text.removeTags` does not strip, so all matching runs
      through `sanitize()` which removes `@...@` tokens too.
- [ ] **A2 — Login reconciliation (bank gone).** Claim the bank on another client
      (or mobile), then log in on the dev client with stale "active" saved state.
      After ~15s (25 ticks) with no login message, the infobox clears. This is the
      phantom-bank fix DWMS lacks — the single most important test.
- [ ] **A3 — Reconciliation respects the in-game toggle.** Disable the retrieval
      login warning in OSRS settings (varbit 14194 = 1), log in with saved active
      state. State must NOT clear — it downgrades to *unknown* (orange infobox text).
- [ ] **A4 — Death creates an inferred bank.** Die at Hespori with junk beyond the
      3 kept items. Shortly after respawn the red indicator appears: yellow
      (*inferred*) `~N` count = carried items minus what you kept (post-respawn diff).
- [ ] **A4b — Kept-items edge case.** Die at an IRS carrying 3 or fewer items
      (all kept). No bank is created — indicator stays hidden.
- [ ] **A5 — Safe deaths ignored.** Die in a safe region (NMZ, Clan Wars, CoX) with
      an active deathbank. No wipe is recorded; indicator unchanged.
- [ ] **A6 — Unsafe death wipes.** With an active deathbank, die to a gravestone
      death elsewhere (not at an IRS). Indicator clears (no notification — you
      already know). Also verify the `You have died again ... lost the items`
      message path — record its exact wording here.

## B. Retrieval interface (ground truth)

- [ ] **B1 — Contents verified on open.** Open the retrieval chest (Hespori → Arno).
      Item count in the indicator switches from `~N` yellow to exact `N` white
      (*verified*), and the side panel item grid matches the chest exactly. Debug
      ✅ **CAPTURED 2026-08-27 (ToB)**: the interface has no title component, so the
      plugin scans INFO and FRAME text. Observed texts:
      `Stack count: 1Guide value: 1,444 (approximately)` and
      `Theatre of Blood Item Retrieval Service`. The second names the service, so
      matching on it identifies the bank without region math. Capture the equivalent
      string for the other services as they come up.

      **Confirmed naming rule (2026-08-27):** window titles and death messages are
      named after the *claim NPC*, not the boss, for NPC-based services. Captured:

      | Service | Window title | Death message |
      |---|---|---|
      | Theatre of Blood | `Theatre of Blood Item Retrieval Service` | login: `...visit the magical chest outside the Theatre of Blood...` |
      | The Nightmare | `Shura's Item Retrieval Service` | `Shura has retrieved some of your items. You can collect them from her in the Sisterhood Sanctuary.` |
      | Phosani's Nightmare | `Sister Senga's Item Retrieval Service` | `Sister Senga has retrieved some of your items. You can collect them from her in the Sisterhood Sanctuary.` |

      This is why `RetrievalService` carries name aliases. It also confirms the wiki
      mapping: regular Nightmare uses Shura, Phosani's uses Sister Senga, and the two
      are only distinguishable from this text (they share region 15515).
- [ ] **B2 — Partial withdrawal tracked.** Take some items out while the window is
      open; count updates live.
- [x] **B3 — Emptying clears state.** Withdraw everything; the indicator disappears
      and state persists as inactive across a relog.
- [x] **B3b — Discarding clears state.** ✅ **VERIFIED 2026-08-27.** Discard-All, then
      both confirmations. The emptied window reports `Stack count: 0` and the plugin
      clears immediately ("Retrieval interface reports 0 stacks").

      **What the game does on discard (all confirmed by instrumentation):**
      - no `ItemContainerChanged` fires, and no game message is sent
      - **no varp changes at all** ("Varps changed while retrieval window was open: none")
      - the `Discard-All` click is component 602:10, but it is NOT sufficient evidence:
        two confirmations follow and the second deliberately swaps the option order, so
        the player can still back out
      - after the discard the interface reopens with `Stack count: 0` while the backing
        item container reads **null**, and the title degrades to a generic
        "Item Retrieval Service"

      Therefore emptiness is read from the interface's stack count, never from the
      container. A null container read is also produced by simply closing the window
      with items still inside, so it can never be treated as evidence of an empty bank.
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

## D. Damage warning & visuals

- [ ] **D1 — On-screen text.** With an active bank, take any hit. Big flashing red
      `DEATHBANK WARNING` appears center-screen for ~5s, refreshing while damage
      continues.
- [ ] **D2 — Optional notification cooldown.** Enable the damage notification;
      sustained damage notifies once per cooldown window (default 50 ticks), while
      the on-screen text still shows per hit.
- [ ] **D3 — Silent in safe regions.** Taking damage in NMZ with an active bank
      shows nothing.
- [ ] **D4 — Red indicator.** The indicator box has a red background and stands out
      from normal infoboxes; it can be dragged like any overlay.
- [ ] **D5 — Chest highlight.** With an active bank, the retrieval chest/NPC is
      outlined red (test at ToB chest north of Ver Sinhaza bank; also Nex, ToA,
      Grotesque Guardians chest, Zulrah priestess, Torfinn, Shura/Senga, Orrvor,
      Petrified Pete, Arno, Sepulchre stranger, Mimic casket). All 13 services
      now have IDs — verify each highlights and note any that don't.
- [ ] **D6 — Side panel.** The sidebar chest icon opens the panel: status line,
      confidence, estimated/verified label, item grid with icons and name+quantity
      tooltips. Shows "No active deathbank" when inactive.

## F. Looting bag marking

- [ ] **F1 — Bag items marked.** Open the looting bag (so the client sees its
      contents), die at content with a retrieval service, then open the retrieval
      chest. Items that came out of the bag are outlined in the configured color.
      Verify against the known ordering: bag contents sit after the worn/inventory
      items, with the ammo slot typically the last non-bag slot.
- [ ] **F2 — Stale bag snapshot.** Die without having opened the bag this session.
      Nothing is marked (the client never saw the contents), and the plugin does not
      guess. The panel omits the bag sentence.
- [ ] **F3 — Same item in both places.** Carry an item that is also in the bag (e.g.
      brews). Note whether both copies are marked; matching is by item id, so this is
      expected and should be documented rather than silently wrong.
- [ ] **F4 — Bag emptied by the death.** After dying, the bag is empty, so a second
      death with no re-fill marks nothing.

## G. Mistaken inference (Theatre of Blood)

- [ ] **G1 — Individual ToB death does not leave a phantom bank.** Die inside ToB
      without a team wipe. The plugin infers a bank (items left your possession), but
      when the raid ends and the items are handed back, it clears itself
      ("inferred stacks are back in hand"). Verify the indicator disappears.

## E2. Stale state at login (regression guard)

- [ ] **E0 — No false warning at login.** With saved active state that is no longer
      real (e.g. claimed on another client, or the client was killed before its
      config flushed), logging in must NOT show the indicator. The panel reads
      "Checking for a deathbank..." until the login message either confirms it
      (indicator appears, verified) or the reconcile clears it silently.
      Note: RuneLite flushes config on a timer and on graceful shutdown, so
      `pkill`-ing the dev client loses recent state writes. Close the client
      window instead when a test depends on persistence.

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
| Exact login warning text | A1 | ☑ captured, see A1 |
| Exact died-again wipe message text | A6 | ☐ |
| Retrieval window title per service | B1, each service | ☑ ToB captured, others pending |
| Grave vs deathbank widget group IDs | B4, widget inspector | ☐ |
| Chest/NPC object IDs per service (for phase 2 recoloring) | dev tools object inspector at each chest | ☐ |
| Discard path | resolved: no events, no varps; read the interface stack count | ☑ |
| Whether The Mimic / quest services hit the region fallback | die there (cheap quest replays not possible — low priority) | ☐ |
