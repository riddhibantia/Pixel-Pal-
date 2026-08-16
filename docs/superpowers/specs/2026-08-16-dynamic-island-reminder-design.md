# Dynamic Island Reminder — Design

Date: 2026-08-16
Status: Approved (voice-reminder feature deferred to a separate spec)

## Problem

When a reminder fires, `ReminderPillView` appears as a static card at the top of the
screen that behaves like a notification: it sits there, requires a tap to expand, and
offers Accept/Deny buttons. The desired experience is an iPhone Dynamic Island–style
live activity: the island blooms open with a spring animation when the alarm fires
(alongside the existing ringtone + system notification), stays alive while ringing,
compacts to a small island after a few seconds, and is dismissed by sliding it right.

## Decisions (defaults confirmed by user's go-ahead)

- Swipe **right** = complete: mark the reminder done (one-shot) or record the
  completion (recurring — the schedule was already re-armed by `advanceSchedule`),
  close the island, cancel the system notification, pet reacts happy.
- Swipe **left** = snooze 10 minutes (reuses `SnoozeReminderUseCase`).
- Tap toggles compact/expanded. No Accept/Deny buttons — gestures + hint text only.
- **No system notification** (updated 2026-08-16 per user): the island is the sole
  reminder UI. `AlarmReceiver` keeps only the ringtone; the notification code was
  removed. Tradeoff: if the screen is locked or the overlay is off, only the ringtone
  plays — nothing is visible until the screen is unlocked.
- Island is pinned to the **physical top of the screen** (`FLAG_LAYOUT_IN_SCREEN`,
  ~6dp offset) so it overlaps the status bar / camera cutout like the iOS island,
  instead of sitting below the status bar inset.

## Components

### `overlay/DynamicIslandView.kt` (new, replaces `ReminderPillView`)

A `FrameLayout` holding a capsule `container` (existing app palette: #26263C→#17172A
gradient, #00D4AA border, rounded 26dp).

States:
- **Expanded**: pulsing accent dot + title + time, message line, muted swipe hint
  ("Swipe → done · ← snooze"). Width = screen − 32dp.
- **Compact**: dot + title only. Width ≈ 200dp. Auto-compact after 8s.

Animations: bloom-in (scale 0.6→1 + fade, OvershootInterpolator), width animation
between states (ValueAnimator + requestLayout; the window is WRAP_CONTENT so it
resizes with the root view), pulsing dot (repeating alpha animator).

Gestures (touch events with VelocityTracker + touch slop):
- Horizontal drag → reports deltas to the manager, which moves the window
  (`updateViewLayout` on `WindowManager.LayoutParams.x`, same technique as pet drag).
- Release past 35% of width or fast fling (≈2000dp/s) in a direction → commit.
- Below threshold → springs back to center.
- Tap (no slop crossing, <250ms) → toggle expanded/compact.

The view never touches the window; the manager owns position and lifecycle.

### `OverlayManager` changes

`showReminderPill` → `showDynamicIsland(title, timeLabel, note, onComplete, onSnooze)`:

- Removes any bubble/island first (same replace semantics as today).
- Window: TYPE_APPLICATION_OVERLAY, WRAP_CONTENT, FLAG_NOT_FOCUSABLE |
  FLAG_NOT_TOUCH_MODAL | FLAG_LAYOUT_IN_SCREEN, gravity TOP | CENTER_HORIZONTAL,
  y = small top offset from the physical screen edge (draws over the status bar /
  camera cutout). x offset = 0 while at rest; drag updates it.
- Commit animations slide the window off-screen (±screen width) then remove the view
  and invoke the callback; spring-back animates x → 0.
- `hideCompanion()` hides the island too.

### `CompanionEngine.onReminderTriggered` changes

- Injects `SnoozeReminderUseCase`.
- onComplete: if recurring (`recurrence != "ONCE"` or a positive
  `recurrenceInterval`) just record the bond completion — `AlarmReceiver.advanceSchedule`
  already re-armed the next occurrence; else `reminderRepository.complete(id)` +
  bond completion. Trigger HAPPY emotion either way.
- onSnooze: `snoozeReminderUseCase(id, 10)`; THINKING emotion briefly.

### Removals

- `overlay/ReminderPillView.kt` deleted.
- Dead `SpeechBubbleOverlayView.showReminderPill` + `revealReminderActions` removed
  (uncalled legacy of the old pill-as-notification behavior).
- `Constants.OVERLAY_PILL_TOP_DP` renamed to `OVERLAY_ISLAND_TOP_DP`.

## Error handling

- Window add failures are caught (existing pattern); system notification still covers
  the reminder when no overlay is showing (`isShowing()` guard retained).
- All animator/handler cleanup in `destroy()` to avoid leaks on service teardown.

## Testing

- No new unit-testable logic (all view/window code); verified by `assembleDebug` +
  existing unit tests, then manual on-device checks: bloom, auto-compact, tap toggle,
  spring-back, swipe commit both directions, recurring reminder survives a swipe-right.
