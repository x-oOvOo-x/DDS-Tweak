# Silent Auto Eat

> Status: implemented on main. Issue #104 tracks the presentation-boundary refactor needed to compose safely with other inventory render mixins; runtime validation remains required for presentation changes.

Current implementation surface:
- `autoEat` (default off);
- `autoEatHungerThreshold` (default 14);
- `autoEatFoodBlacklist` with the conservative exclusions defined below;
- one client-side state machine using vanilla inventory `SWAP` clicks and normal offhand item use;
- local first-person/HUD presentation masking only while Auto Eat owns the temporary offhand transaction.

## Goal

Add a pure-client automatic eating feature to DDS Tweak that keeps Minecraft's original eating semantics unchanged.

The feature may temporarily move a selected food item into the offhand and start normal offhand item use. DDS Tweak hides only the temporary visual swap/eating hand presentation from the local player.

The guiding rule is:

**vanilla player behavior > Auto Eat > visual hiding**

Visual suppression must never change inventory authority, hand state, eating duration, interruption rules, food effects, or any other gameplay result.

## Required behavior

1. Trigger only when the configured hunger condition is met.
2. Select an eligible food item from the player's inventory according to the configured food policy.
3. Record the current offhand stack/state.
4. Move the selected food into the offhand using normal inventory interaction.
5. Start normal `OFF_HAND` item use.
6. Let vanilla control:
   - eating duration;
   - movement behavior while eating;
   - interruption/cancellation;
   - hunger and saturation restoration;
   - food effects;
   - item consumption;
   - all conflicts with other player actions.
7. On completion or interruption, restore the original offhand stack/state.
8. Abort safely if the inventory/container state changes so the original state can no longer be restored deterministically.

## Visual behavior

During an Auto Eat transaction, DDS Tweak may locally suppress only the temporary presentation caused by the automated offhand swap/use:

- do not visually replace the player's normal offhand item with the food;
- suppress the temporary offhand equip/swap animation;
- suppress the first-person eating hand animation/model caused specifically by Auto Eat.

Do **not** hide or alter gameplay results. Hunger changes, item consumption, food effects, vanilla timing and interruption rules remain real.

Eating sounds and particles should remain vanilla by default. If visual/audio suppression is ever expanded, it must be a separate optional presentation setting and must not affect gameplay state.

### Presentation architecture

The transaction state machine owns the real swap/use/restore lifecycle. Rendering is a separate projection layer:

```text
SilentAutoEat transaction state
        ↓ read-only presentation facts
SilentAutoEatPresentation
        ↓ upstream ItemStack → projected ItemStack
version-specific render adapter
        ↓
vanilla / third-party render consumer
```

Presentation code must preserve the incoming upstream stack for every slot/hand it does not own. In particular, container rendering must not claim `Slot#getItem()` or `Inventory#getItem()` with an exclusive Redirect merely to change what is shown. The adapter should transform the already-resolved stack at the latest stable presentation boundary available in that Minecraft API generation.

For container slots, the shared projection is `projectContainerStack(player, slot, upstream)`. The real `Menu`, `Slot`, inventory contents and server state remain untouched.


## Important gameplay consequence

The offhand is genuinely occupied by food on the server while Auto Eat is active even if the client continues rendering the previous offhand item.

Examples:

- an offhand Totem of Undying is not actually available while food occupies the offhand;
- a shield is not actually available from the offhand during that interval;
- rockets, torches or other offhand items likewise cannot be treated as present.

DDS Tweak must never simulate those original offhand functions while the server considers food to be held there.

## Container and picker behavior

- Auto Eat stays armed while a normal container screen is open when the current menu exposes real slots backed by the local player's own Inventory.
- Food is still selected only from the local player's Inventory/hotbar; container, Ender, QCA or nested-storage contents are never searched for food.
- The actual current menu slot id is resolved from the player's Inventory-backed Slot before every transaction; unknown/custom menus without a reliable mapping fail closed.
- A non-container screen, a carried cursor stack, menu replacement, container-id replacement or stale slot mapping stops the active transaction and restores only when the original offhand can be recovered deterministically.
- Explicit container mouse, keyboard and wheel input cancels/restores Auto Eat before the player's container interaction is processed.
- Natural consumption may replace the food with a remainder item such as a bowl or bottle. After vanilla completion, that remainder is returned to the original food source slot while the original offhand is restored; interrupted transactions still require the offhand to match the owned food transaction.
- The Auto Eat blacklist reuses the existing DDS Tweak item picker in FOOD mode. Only runtime-edible items are offered as selectable candidates; persisted values remain registry ids.
- FOOD mode filters the creative-tab row to tabs that actually contain eligible food candidates, plus the global "All" tab; unrelated building/redstone/tool tabs are hidden.
- On 1.21.2+ an item must expose both the FOOD and CONSUMABLE data components to count as directly edible. This excludes non-consumable carriers such as fish buckets that may expose food metadata without being edible by the player.
- While the transaction is active, container rendering remaps only the local player's source/offhand Slot presentation: the offhand slot shows the original offhand and the source slot shows the food/result currently owned by Auto Eat. Real Menu/Slot/ItemStack state is never rewritten for presentation.

## Input and interruption policy

Do not introduce custom exemptions that make Auto Eat stronger than normal eating.

- Movement, camera rotation and other actions allowed by vanilla while eating remain allowed.
- Any vanilla action/state that interrupts or conflicts with eating continues to do so.
- Explicit player input takes priority over automation.
- When a conflicting action interrupts eating, Auto Eat must cleanly restore the original offhand state and may retry later only after normal trigger conditions are satisfied again.
- Container/QCA/inventory transitions that make the temporary swap unsafe must cancel rather than attempt speculative restoration.

## Food selection safety

Initial implementation should be conservative.

Default exclusions should include foods that are normally undesirable or unusually valuable for automatic consumption, such as:

- pufferfish;
- rotten flesh;
- spider eye;
- enchanted golden apple;
- golden apple unless explicitly enabled.

Food selection should avoid needless waste. A later implementation may use a simple deterministic policy such as choosing an eligible food that reaches the configured target hunger with the least excess restoration, rather than always choosing the food with the largest hunger value.

## State machine

Suggested transaction states:

```text
IDLE
 -> PREPARE
 -> SWAP_TO_OFFHAND
 -> START_USE
 -> USING
 -> RESTORE_OFFHAND
 -> IDLE
```

Any invalid inventory sync/state transition should go through a safe abort/restore path.

The transaction must have one owner and must not overlap with another Auto Eat transaction.

## Pure-client boundary

This design intentionally stays within normal client/server interaction:

- no server-side hunger modification;
- no direct consumption from an arbitrary inventory slot;
- no CDA requirement;
- no custom packet that asks the server to apply food effects;
- no bypass of normal hand/item-use semantics.

A future DT+CDA mode that performs true server-side background consumption would be a separate feature and is outside this proposal.

## Configuration direction

Proposed settings, exact names to be decided during implementation:

- Auto Eat: off by default.
- Hunger trigger threshold.
- Optional target hunger threshold.
- Food allow/deny policy.
- Optional valuable-food protection.
- Optional presentation controls for eating particles/sounds, separate from core automation.

The first implementation should prefer a small settings surface and deterministic defaults.

## Validation requirements for a future implementation

At minimum, runtime validation should cover:

- walking/running while Auto Eat is active;
- normal vanilla completion timing;
- manual interruption;
- offhand shield;
- offhand Totem of Undying;
- offhand rockets/torches;
- inventory open/close during the transaction;
- QCA/container transitions;
- food stack depletion;
- food moved by server/container sync during the transaction;
- death/dimension change/disconnect;
- repeated trigger after an interrupted eat;
- multiplayer server with no DDS/CDA installed.

## Out of scope for this PR

- production code;
- config keys;
- mixins;
- translations;
- tests;
- version-gating changes;
- CDA protocol changes;
- CI/runtime validation.

Implementation starts only after this design is explicitly approved.
