# Tab fake-player groups (client-only)

- **Toggle:** Visual / `tabFakePlayersLast`, default off.
- **Order:** ordinary non-spectator (including UNKNOWN), then ordinary spectator, then confirmed FAKE regardless of game mode. Within each group the original Minecraft comparator governs `tabListOrder`, teams and names.
- **Identity:** reuse `PlayerIdentityResolver.resolve(UUID)`; never infer fakes from latency, names or skins. Without a trusted source, entries remain in ordinary groups.
- **Membership:** reorder only vanilla listed players; leave visibility, appearance and the 80-entry cap alone.
- **Spacing:** fixed 3 GUI pixels where groups change *inside the same vanilla Tab column*; no fake PlayerInfo or blank full row. Row pose includes background, face, text, scoreboard and ping. The panel and footer move together; if the total height would exceed the GUI, omit all extra gaps without hiding players.
- **Lifecycle:** render transforms are restored in a method-level `finally`, including exceptional exits. Recompute from live identity on rendering; no stale local fake list. A newly spawned fake may move after its identity packet arrives.
- **Target versions:** 1.21.11, 26.1.2, 26.2, 26.3 only. Other active versions stay unchanged. 26.x uses `GuiGraphicsExtractor` and must be validated separately.

## Runtime verification

Test normal and spectator real players vs survival and spectator fakes; fallback without DDS/Org; late identity/revocation; 1/2/3 groups; 21, 40, 80 players with boundary at a column edge; ping, skins, objective hearts/scores and header/footer; toggling live; and reconnect/switch servers. Confirm Mixin targets at runtime on every supported version. Source tests or a successful commit are not runtime verification.
