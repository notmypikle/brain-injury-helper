"""Calm mode ("Too much") — grounding for overstimulation.

USER NEED (2026-11-10): too much noise and too much simultaneous
stimulation is a genuine daily barrier — this is a CORE accessibility
feature, not a nice-to-have. One tap starts a slow, one-step-at-a-time
grounding sequence. Every step is short, single-action, low cognitive
load (see docs/IMPLEMENTATION_PLAN.md).

Safety boundary: calm mode is NOT crisis support. Crisis detection runs
before the assistant (in main.py) and always wins — a test enforces it.
"""

# Ordered grounding steps. Keep each one SHORT and ONE action only.
# The Android app bundles a copy of these as an offline fallback — if
# you change them here, mirror the change in CalmScreen.kt.
STEPS = [
    "It's okay. Everything can wait for a moment.",
    "Breathe in slowly through your nose — 1, 2, 3, 4.",
    "And out gently through your mouth — 1, 2, 3, 4, 5, 6.",
    "Feel your feet on the floor. Press down gently.",
    "Look around. Name one thing you can see.",
    "Name one sound you can hear — or enjoy the quiet for a moment.",
    "Let your shoulders drop. Soften your jaw and hands.",
    "This feeling will pass. You're doing well.",
    "When you're ready: pick just ONE small thing to do next. Only one.",
]

# What the assistant says when the user says it's all too much. Points
# at the button and starts the first breathing step right away.
OVERLOAD_REPLY = (
    "That sounds like a lot right now. Everything can wait — nothing "
    "needs doing this minute. Try this with me: breathe in slowly — "
    "1, 2, 3, 4… and out gently. 💙\n"
    "Or tap the \"Too much\" button and I'll walk you through calming "
    "down, one tiny step at a time."
)
