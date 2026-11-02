# Wired Creator Tools Implementation Summary

## 1. Purpose

This document summarizes the `:wired` work completed in this development cycle.

It is intended as a project-facing summary of:

- what was added
- where it lives
- what is already working
- what is still intentionally left as future work

---

## 2. Main goals completed

The current `:wired` implementation now provides:

1. a dedicated client UI window
2. monitor and inspection tooling
3. room/user/furni/global variable views
4. inline editing for selected values
5. live wired diagnostics from the server
6. error/warning history with details
7. server-side diagnostics configuration through DB settings
8. a server-paged room log with Habbo's four log levels, fed by the engine and by write-to-logs boxes

The emulator is now Polaris (`Gameserver/Emulator`), the client Octane (`Octane`, formerly Nitro-V3)
and the renderer Octane-Renderer (`Octane-Renderer`, formerly Nitro_Render_V3).

---

## 3. Nitro-V3 work (now Octane)

Main file:

- `Octane/src/components/wired-tools/WiredCreatorToolsView.tsx`

### 3.1 UI window

The `:wired` tool now has these main tabs:

- `Monitor`
- `Variables`
- `Inspection`
- `Chests`
- `Settings`

Current active work is mainly in:

- `Monitor`
- `Inspection`

`Chests` and `Settings` are now implemented too. `Chests` (`WiredChestsTabView.tsx`) shows the room-wide wired chest transaction log with a per-chest filter, lock/unlock controls for the room's chests, and a detail window for a single transaction. `Settings` (`WiredToolsSettingsTabView.tsx`) edits the room's wired inspect/modify permission masks and the room's wired timezone, and saves them together through the official `WiredMenuPermissionsSaveComposer` (1936; the older `WiredRoomSettingsSaveComposer` is no longer sent). It also has the reload and rollback buttons (3761, each behind a confirmation), the account preferences (toolbar button, inspect button, wired style) and, when the client configuration `wired.selfdonation.enabled` is on, the sandbox donation tool (`WiredSelfDonationView.tsx`, 2499).

### 3.2 Inspection

Implemented:

- element type switcher (`furni`, `user`, `global`)
- preview area
- variable table
- `Keep selected`
- inline editing

#### Furni

Added support for:

- detailed furni variables
- live preview
- wall/floor-specific handling
- teleport metadata
- inline edits for state/position/rotation/altitude/wall offset

#### User

Added support for:

- user/bot/pet identity
- rights / owner / group admin flags
- mute / trading / frozen flags
- team / sign / dance / idle / hand item / effect display
- room entry method and teleport entry id
- inline edits for position and direction

#### Global

Added support for:

- room counts
- wired timer
- team scores and sizes
- room/group ids
- server/client timezone
- current server time breakdown

The variable names follow the current client (`@position_x`, `@team_red_score`, `@effect_id`, ...);
the full list is in `docs/wired_tools_reference.md` section 5.

### 3.3 Monitor

Implemented:

- live stats table
- log summary list
- full log history
- info/documentation popup
- error information popup
- room log window (`WiredRoomLogsView.tsx`, `Room logs` button): the room's log 50 lines a page
  (meant newest first; the server currently pages oldest first, see `wired_bug_audit.md` 3.9),
  with a text filter, source and level menus, each line coloured by its level, and an
  auto refresh every 2.5 s

The auxiliary monitor windows now use proper card windows, so they are:

- draggable
- resizable
- closed through the normal card close button

### 3.4 Variables and the setup window

- `Manage` on the Variables tab opens the variable owners window (`WiredVariableOwnersView.tsx`):
  the holders of one variable, server-paged, with user type and sort filters
- `Contents` opens the array contents window (`WiredArrayInspectorView.tsx`) for array variables
- `Clear this variable` removes a user or furni variable from every holder (definition owner only)
- the box setup window (`components/wired/views/WiredBaseView.tsx`) has a quick menu: copy, paste,
  paste into (keep picks and delay), clear furni picks, reset to default and save without closing

---

## 4. Nitro_Render_V3 work (now Octane-Renderer)

Renderer-side work was focused on making the client receive enough metadata for the new UI.

Main areas:

- new wired monitor packet parsing
- room/session metadata extensions
- furni metadata extensions
- user metadata extensions

### 4.1 Monitor data

The renderer now parses and exposes:

- usage budget values
- delayed queue values
- execution timing values
- heavy/overload thresholds
- current logs
- history rows

It also parses the room log page (`WiredLogPageParser`), the variable holders page, the array
contents page, the variable fx packets, and the click settings, which it applies to the room engine
(`RoomEngine.setWiredClickSettings`).

### 4.2 Room metadata

The renderer/session flow was extended to expose values used by the client:

- room furni limit
- room group id
- hotel timezone / hotel time snapshot

### 4.3 Furni metadata

The furni info path now exposes values used by the inspector, including:

- dimensions
- `items_base`-driven flags such as sit/lay/stand/stack
- teleport target metadata

### 4.4 User metadata

The user/unit data path now exposes values used by the inspector, including:

- room entry method
- room entry teleport id
- identity data for user/bot/pet

---

## 5. Emulator work

Main areas:

- wired diagnostics engine
- monitor request/response packet
- room/user/furni metadata support
- configuration migration to `wired_emulator_settings`

### 5.1 Wired diagnostics

Added server-side room diagnostics with:

- usage budget tracking
- delayed event queue tracking
- average/peak execution timing
- overload detection
- heavy-room detection
- recursion protection logging
- killed-room protection logging
- `NO_TARGETS` and `UNREACHABLE` notes for setups that silently do nothing
- `WIRED_LOG`: lines written by the "write to logs" boxes (`wf_act_log`, `wf_act_neg_log`) at the
  level the box was set to

### 5.2 Diagnostics logs

Logs now carry:

- type
- severity
- count
- reason
- source label
- source id
- history entries with occurrence timestamps

Severities are Habbo's four room log levels, `0` debug, `1` info, `2` warning, `3` error
(`WiredRoomDiagnostics.Severity.getLogLevel()`; the enum ordinals are unchanged). The room keeps its
last 200 history entries; a box's log lines only push out older box lines, never the engine's
entries. The room log page (3882 in, 918 out, `WiredRoomLogsPageEvent`) filters that history by log
level, source (the entry type) and text.

### 5.3 Trigger/runtime fixes

Important behaviour fixes added during this work:

- empty repeater stacks no longer count as executable work
- monitor usage is consumed later in the execution path, closer to real execution
- timer/repeater behaviour is less noisy in diagnostics
- events that no stack in the room listens to no longer count toward the abuse rate limit, so
  visitors on plates or dice cannot get a room's wired banned (`WiredEventDispatcher`)
- every stack a call-stacks box runs is charged `10` to the room's execution budget before it runs,
  is refused while the room is banned, and one call reaches at most 20 tiles and 20 stacks
- array work is charged to the same execution budget
- the move-style hints of one firing go out as one packet per style instead of one per moved furni
  (`WiredMoveStyleHelper`)
- effects without an ordering add-on run in the stack's physical order (height, then id)

### 5.4 Monitor packet

A dedicated request/response path was added so the client can poll live room diagnostics. Octane
polls it every 250 ms while the Monitor tab is open; the server accepts one request per 50 ms and
clears the logs (action `1`) only for users with modify rights.

### 5.5 Configuration migration

All wired config is being moved out of `emulator_settings` and into:

- `wired_emulator_settings`

This now includes both:

- existing wired runtime settings
- the new `:wired` monitor threshold settings

Migration file:

- `Gameserver/Database/Database Updates/Own_Database_RunFirst/002_move_wired_settings_to_wired_emulator_settings.sql`

Fresh installs get the table from the Flyway base schema
(`Gameserver/Emulator/src/main/resources/db/migration/V20260518000000__base_database.sql`).

---

## 6. What the monitor currently measures

The monitor currently measures:

- execution budget consumed in the current server window
- delayed events currently pending
- average execution time inside the current window
- peak execution time inside the current window
- recursion depth
- remaining killed-room cooldown
- room heavy state
- room furni counts
- renderer custom variable counts on room items
- per-type log counts, including the write-to-logs boxes' own lines

---

## 7. What is configurable now

Current DB-configurable areas include:

- engine enable/debug/exclusive/max-steps
- custom wired compatibility mode
- furni selection limit
- max delay / max text length
- teleport delay
- tick interval/debug/priority
- abuse protection thresholds
- monitor usage/delayed/heavy/overload thresholds

All of these are documented in:

- `docs/wired_tools_reference.md`

---

## 8. Known limitations / future work

Current known limitations:

- `Permanent furni vars` uses a fixed UI denominator (`60`)
- `@wired_timer` is still client-side time since room entry
- `Chests` and `Settings` are implemented and served by the emulator (`WiredChestRoomLogsEvent` 9328, `WiredChestLockEvent` 9329, `WiredChestTransactionDetailsEvent` 9334, `WiredRoomSettingsRequestEvent` 10022, `WiredRoomSettingsSaveEvent` 10023, `WiredMenuPermissionsSaveEvent` 1936, `WiredRoomStateActionEvent` 3761, `SelfDonationEvent` 2499). The chest log is read-only and gated on room rights; locking every chest in the room, rather than only your own, is reserved for the room owner or `ACC_ANYROOMOWNER`. Self-donation needs `hotel.selfdonation.enabled` and the `acc_debug` permission on the server
- legacy wired configuration keys are still present for database compatibility, but runtime execution now goes only through the new engine (`WiredHandler` is a thin facade over it)
- the room log page comes back oldest first although it is meant to be newest first (`wired_bug_audit.md` 3.9)
- Octane does not send the official click-user (3122), holders request (2973) or variable cache sync (1735/1497) packets, although the emulator serves them

Good future tasks:

- make `Permanent furni vars` fully server-driven
- add export/copy actions for monitor history
- ~~add more detailed filtering/search in history~~ done: the room log window filters by text, source and level
- ~~document the chest transaction log filter codes and the wired permission mask bits in `docs/wired_tools_reference.md`~~ done: section 6.10
- optionally remove the compatibility keys entirely once old database defaults are no longer needed

---

## 9. Recommended rollout order

1. run the wired settings migration SQL (existing databases only; fresh installs get it from Flyway)
2. restart the emulator
3. refresh renderer/client
4. verify monitor values in a real room
5. tune `wired.monitor.*` thresholds using the new DB table
