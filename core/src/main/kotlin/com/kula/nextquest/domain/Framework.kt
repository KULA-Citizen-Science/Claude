package com.kula.nextquest.domain

/**
 * How well-evidenced a framework is. Kept explicit because the taxonomy deliberately mixes
 * peer-reviewed pillars with useful-but-unvalidated clinical heuristics, and it's dishonest to
 * present them as the same thing. The card shows a tiny marker for this; the detail lives in
 * [Framework.source].
 */
enum class EvidenceTier(val marker: String, val label: String) {
    /** Peer-reviewed theory, meta-analysis, or RCT-supported clinical protocol. */
    PEER_REVIEWED("✓", "peer-reviewed"),

    /** Practitioner/coaching heuristic — practically useful, not a validated construct. */
    CLINICAL_HEURISTIC("~", "clinical heuristic"),
}

/**
 * The frameworks each friction point and strategy is grounded in.
 *
 * [tag] is the tiny label on the reading card. [tier] is shown as a one-character marker beside
 * it. [source] is the longer attribution with citations — not shown in the minimal UI, but it
 * keeps the taxonomy honest and is asserted on in tests so nothing ships ungrounded. See
 * `docs/RESEARCH.md` for the full evidence write-up.
 *
 * These describe established models of ADHD / executive function. Decision-support scaffolding,
 * not clinical advice.
 */
enum class Framework(val tag: String, val tier: EvidenceTier, val source: String) {
    TIME_BLINDNESS(
        tag = "Time blindness",
        tier = EvidenceTier.PEER_REVIEWED,
        source = "Barkley (1997; 2012) — behaviour falls under 'the temporal now', a.k.a. " +
            "temporal myopia. Timing deficits meta-analysed in Marx et al. (2022, JAACAP) and " +
            "Zheng et al. (2022, J. Attention Disorders): estimates run short, an accelerated " +
            "internal clock."
    ),
    ACTIVATION_ENERGY(
        tag = "Activation",
        tier = EvidenceTier.PEER_REVIEWED,
        source = "Brown's 'Activation' cluster (getting started/organising/prioritising) + " +
            "Sonuga-Barke's dual/triple-pathway model (2002–2010): executive dysfunction and " +
            "delay aversion make initiation a distinct, impaired step. 'Activation energy' framing " +
            "is coaching shorthand."
    ),
    WORKING_MEMORY(
        tag = "Working memory",
        tier = EvidenceTier.PEER_REVIEWED,
        source = "Barkley — weak nonverbal working memory; holding a multi-step sequence in mind " +
            "fails. Safren's CBT (2010, JAMA, RCT) 'Organising Multiple Tasks' module externalises " +
            "it onto lists."
    ),
    PRIORITISATION(
        tag = "Where to start",
        tier = EvidenceTier.PEER_REVIEWED,
        source = "Brown's Activation (prioritising) + Safren's A/B/C priority ratings and " +
            "'Managing Overwhelming Tasks' + GTD's single next physical action: collapse the " +
            "open task to one concrete start."
    ),
    DEFINE_DONE(
        tag = "Define 'done'",
        tier = EvidenceTier.PEER_REVIEWED,
        source = "Solanto's Meta-Cognitive Therapy (2010, Am. J. Psychiatry, RCT) + Safren — " +
            "open/ambiguous tasks stall; the evidence-based move is a mandatory 'define the " +
            "deliverable' pre-step before scheduling."
    ),
    SUSTAINED_EFFORT(
        tag = "Sustained effort",
        tier = EvidenceTier.PEER_REVIEWED,
        source = "Solanto lists 'avoidance of tasks requiring sustained mental effort' as a core " +
            "target; Safren's 'break tasks to fit your attention span'. Effort/focus regulation " +
            "(Brown; Barkley)."
    ),
    COGNITIVE_LOAD(
        tag = "Energy window",
        tier = EvidenceTier.CLINICAL_HEURISTIC,
        source = "Occupational-therapy energy/task-demand matching (UK Adult ADHD Network " +
            "consensus, BMC Psychiatry 2021) + Barkley's 'point of performance': schedule heavy " +
            "load in your peak/medication window."
    ),
    TRANSITION_COST(
        tag = "Transition cost",
        tier = EvidenceTier.CLINICAL_HEURISTIC,
        source = "Set-shifting/task-switching difficulty in ADHD (EF literature) + OT task-demand " +
            "analysis: leaving one context for another, especially the house, carries a large, " +
            "underestimated start cost."
    ),
    INTEREST_DRIVEN(
        tag = "Interest-driven",
        tier = EvidenceTier.CLINICAL_HEURISTIC,
        source = "Dodson's interest-based nervous system / INCUP (Interest, Novelty, Challenge, " +
            "Urgency, Passion) — a Psychiatric Times (2006) practitioner heuristic, not validated " +
            "science, but a real motivational lever."
    ),
    NO_FINISH_LINE(
        tag = "No finish line",
        tier = EvidenceTier.CLINICAL_HEURISTIC,
        source = "INCUP 'urgency' (Dodson, heuristic) resting on peer-reviewed steeper temporal " +
            "discounting in ADHD: with no endpoint/deadline there's no urgency signal, so the " +
            "task never gains traction."
    ),
    WALL_OF_AWFUL(
        tag = "Wall of Awful",
        tier = EvidenceTier.CLINICAL_HEURISTIC,
        source = "Brendan Mahan's coaching metaphor (2016): accumulated emotional charge becomes " +
            "the real barrier to starting. Overlaps peer-reviewed delay aversion (Sonuga-Barke)."
    ),
    OBJECT_PERMANENCE(
        tag = "Out of sight",
        tier = EvidenceTier.CLINICAL_HEURISTIC,
        source = "Popular ADHD 'time/object permanence' framing, grounded in Barkley's " +
            "externalisation principle: a plan not visibly present drops out of awareness during " +
            "gaps and waits."
    );
}
