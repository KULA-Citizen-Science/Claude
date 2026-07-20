# The evidence behind NextQuest

NextQuest's whole value is that the *underlying* taxonomy is thorough and honestly graded, even
though the surface output is two short lines. This document maps the app's frictions and
strategies to their sources, and marks how well-evidenced each one is. It distils
[`adhd-task-taxonomy-report.md`](adhd-task-taxonomy-report.md) (the research brief this design was
built from).

**NextQuest is a nudge tool, not a diagnosis or treatment.** A system externalises executive
function; it doesn't replace medication, therapy, or coaching, which carry the strongest evidence.

## Two evidence tiers

The card shows a one-character marker next to each framework tag, because it would be dishonest to
present peer-reviewed models and coaching heuristics as the same thing:

- **`✓` peer-reviewed** — a peer-reviewed theory, meta-analysis, or RCT-supported clinical protocol.
- **`~` clinical heuristic** — a practitioner/coaching idea: practically useful, not a validated
  construct.

## The dimensions we describe an activity by

Following the report's synthesised, orthogonal set (kept short on purpose — decision fatigue is
itself an ADHD tax):

| Dimension (`Activity` field)        | Grounds |
|-------------------------------------|---------|
| interest (boring/neutral/engaging)  | Dodson's interest-based nervous system / INCUP (heuristic) |
| deadline (hard/soft/none)           | urgency lever (INCUP) resting on peer-reviewed temporal discounting |
| structure (defined/open)            | OT task-demand analysis; Solanto/Safren "define the deliverable" |
| duration (quick/medium/long)        | timing-deficit meta-analyses (Marx 2022; Zheng 2022) + planning fallacy |
| load (light/heavy)                  | OT energy/task-demand matching; Barkley "point of performance" |
| multiStep                           | Barkley working memory; Safren "organising multiple tasks" |
| requiresLeavingHome                 | set-shifting / transition cost |
| hasDread                            | Wall of Awful (heuristic) + delay aversion (peer-reviewed) |
| involvesWaiting                     | object/time permanence (externalisation) |
| externallyImposed                   | delay aversion; lowered intrinsic pull |

## Friction points → framework → evidence → counter-move

Each maps to `FrictionCatalog` in `core/.../FrictionPoint.kt`.

| Friction (trap) | Framework tag | Tier | Counter-move (strategy) |
|-----------------|---------------|------|-------------------------|
| Won't start | Activation | `✓` | if-then implementation intention + 5-min timer on step one |
| Dread is the blocker | Wall of Awful | `~` | name the feeling, do one tiny non-scary piece |
| Will run late getting ready | Time blindness | `✓` | alarm the "start getting ready", work backwards |
| Wait swallows the plan | Out of sight | `~` | alarm the far end of the wait now |
| Getting out the door | Transition cost | `~` | pre-stage everything by the door |
| Heavy thinking on low fuel | Energy window | `~` | book it for your peak/meds window |
| Lose the thread / drop a step | Working memory | `✓` | dump steps onto paper, work the list |
| Don't know what "done" is | Define 'done' | `✓` | write one sentence defining "done" first |
| Too many entry points | Where to start | `✓` | pick the single next physical action |
| No deadline, no traction | No finish line | `~` | set a real, witnessed deadline |
| Eats more time than it feels | Time blindness | `✓` | guess ×1.5, start a visible countdown |
| Too dull to hold you | Interest-driven | `~` | bundle with music/podcast, or race the clock |
| Drift off mid-way | Sustained effort | `✓` | one 25-min block, park it at the ring |

## Peer-reviewed pillars (the load-bearing citations)

- **Barkley (1997; 2012)** — self-regulation & time as the organising construct; "temporal myopia"
  / time blindness; externalise information, make time physical, arrange motivation at the point of
  performance.
- **Marx et al. (2022, *JAACAP*)** and **Zheng et al. (2022, *J. Attention Disorders*)** —
  meta-analytic evidence of broad timing deficits: estimates run short, an accelerated internal
  clock. Justifies distrusting internal estimates and buffering time.
- **Safren et al. (2010, *JAMA*)** and **Solanto et al. (2010, *Am. J. Psychiatry*)** — RCT-tested
  CBT for adult ADHD; the task operations here (breaking down tasks, defining deliverables,
  A/B/C priorities, organising multiple tasks, fitting work to attention span) come from their
  modules.
- **Gollwitzer implementation intentions** (if-then plans; meta-analysis Toli et al., 2016) —
  automate initiation; used for the "When I sit down, I start a 5-min timer" framing.
- **Sonuga-Barke dual/triple-pathway model** — executive dysfunction *and* delay aversion as
  dissociable deficits; underpins both the initiation and dread frictions.

## Useful-but-unvalidated heuristics (labelled `~`)

Dodson's INCUP / interest-based nervous system, Mahan's "Wall of Awful", and the popular
"object/time permanence" framing are coaching ideas, not validated science — but they capture real
motivational and emotional levers, so we use them and say so. The specific ×1.5 time buffer is
likewise a practitioner rule of thumb (the *deficit* it corrects for is peer-reviewed).
