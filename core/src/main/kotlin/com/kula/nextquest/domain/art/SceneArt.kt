package com.kula.nextquest.domain.art

/**
 * The symbolic art catalog for reading scenes, LucasArts-adventure style.
 *
 * Each friction point gets two sprites: [traps] — the big symbolic image of the difficulty (a
 * cracked wall, a melting clock, a road with no end) — and [emblems] — the small "inventory item"
 * representing the counter-move (an alarm bell, a checklist, a pomodoro). The app composes a
 * unique scene per (archetype, friction) pair by layering an archetype-tinted backdrop + prop,
 * the mascot, the trap sprite, and the emblem in an inventory slot.
 *
 * Keyed by `FrictionPoint.id`; tests assert full coverage of the catalog.
 */
object SceneArt {

    fun trapFor(frictionId: String): Sprite =
        traps[frictionId] ?: error("no trap sprite for friction '$frictionId'")

    fun emblemFor(frictionId: String): Sprite =
        emblems[frictionId] ?: error("no emblem sprite for friction '$frictionId'")

    /** The difficulty, drawn large: what the trap *looks like*. */
    val traps: Map<String, Sprite> = mapOf(
        // The Wall of Awful: a looming brick wall with a glowing crack.
        "dread" to Sprite(listOf(
            "KKKKKKKKKKKKKKKK",
            "KSSSKSSSSKSSSSSK",
            "KSSSKSSSSKSSSSSK",
            "KKKKKKKKDKKKKKKK",
            "KSSSSSKSDSKSSSSK",
            "KSSSSSKSDSKSSSSK",
            "KKKKKKKKDKKKKKKK",
            "KSSKSSSDSSSKSSSK",
            "KSSKSSSDSSSKSSSK",
            "KKKKKKKKKDKKKKKK",
            "KSSSSKSSSDSSKSSK",
            "KKKKKKKKKKKKKKKK",
        )),
        // An alarm clock sprinting off, motion lines trailing.
        "beingLate" to Sprite(listOf(
            "....GG....GG....",
            "...GGGG..GGGG...",
            "....KKKKKKKK....",
            "...KWWWWWWWWK...",
            "..KWWWWKWWWWWK..",
            "M.KWWWWKWWWWWK..",
            "MMKWWWWKKKKWWK..",
            "M.KWWWWWWWWWWK..",
            "..KWWWWWWWWWWK..",
            "...KKKKKKKKKK...",
            "....KK....KK....",
            "...KK......KK...",
        )),
        // An hourglass mid-pour; the plan fading into dots off to the side.
        "waitGap" to Sprite(listOf(
            "..KKKKKKKKKK....",
            "..KWWWWWWWWK....",
            "...KWGGGGWK.....",
            "....KWGGWK......",
            ".....KWWK.......",
            ".....KGWK...W...",
            "....KWWWWK....W.",
            "...KWGGGGWK.....",
            "..KWGGGGGGWK....",
            "..KKKKKKKKKK....",
        )),
        // A doorway with the way out arrowed — the threshold is the boss fight.
        "transition" to Sprite(listOf(
            ".KKKKKKKK.......",
            ".KPPPPPPK.......",
            ".KPPPPPPK...L...",
            ".KPPPGPPK...LL..",
            ".KPPPPPPKLLLLLL.",
            ".KPPPPPPKLLLLLLL",
            ".KPPPPPPKLLLLLL.",
            ".KPPPGPPK...LL..",
            ".KPPPPPPK...L...",
            ".KPPPPPPK.......",
            ".KKKKKKKK.......",
        )),
        // A battery on its last red bar, warning above.
        "cognitiveLoad" to Sprite(listOf(
            ".......DD.......",
            ".......DD.......",
            ".......DD.......",
            "................",
            ".......DD.......",
            "................",
            ".KKKKKKKKKKKK...",
            ".KPPPPPPPPPPKK..",
            ".KDDPPPPPPPPKK..",
            ".KDDPPPPPPPPKK..",
            ".KPPPPPPPPPPKK..",
            ".KKKKKKKKKKKK...",
        )),
        // Pages scattering out of your hands mid-sequence.
        "workingMemory" to Sprite(listOf(
            "..KKKKKK........",
            "..KWWWWK..KKKK..",
            "..KWKKWK..KWWK..",
            "..KWWWWK..KWKK..",
            "..KWKKWK..KWWK..",
            "..KWWWWK..KKKK..",
            "..KWKKWK....W...",
            "..KWWWWK........",
            "..KKKKKK..W.....",
            ".....W..........",
        )),
        // A goal flag flying a question mark: where even is "done"?
        "defineDone" to Sprite(listOf(
            "...KGGGGGGGGGG..",
            "...KGWWWWWGGGG..",
            "...KGGGGGWWGGG..",
            "...KGGGWWWGGGG..",
            "...KGGGWWGGGGG..",
            "...KGGGGGGGGGG..",
            "...KGGGWWGGGGG..",
            "...KGGGGGGGGGG..",
            "...K............",
            "...K............",
            "...K............",
            "...K............",
        )),
        // A signpost pointing three ways at once.
        "prioritisation" to Sprite(listOf(
            "......KK........",
            ".LLLLLKK........",
            ".LLLLLKK........",
            "......KKGGGGG...",
            "......KKGGGGG...",
            ".MMMMMKK........",
            ".MMMMMKK........",
            "......KK........",
            "......KK........",
            "......KK........",
        )),
        // A road running straight to the horizon — no finish line anywhere.
        "noFinishLine" to Sprite(listOf(
            "................",
            ".......KK.......",
            "......KWWK......",
            ".....KPWWPK.....",
            "....KPPWWPPK....",
            "...KPPPWWPPPK...",
            "..KPPPPWWPPPPK..",
            ".KPPPPPWWPPPPPK.",
            "KPPPPPPWWPPPPPPK",
        )),
        // A clock face gone spiral, melting off the shelf.
        "timeEstimation" to Sprite(listOf(
            "....KKKKKKKK....",
            "...KWWWWWWWWK...",
            "..KWWCCCCCWWWK..",
            "..KWCCWWWCCWWK..",
            "..KWCWWCWWCWWK..",
            "..KWCWWCCWCWWK..",
            "..KWWCWWWWCWWK..",
            "...KWWCCCCWWK...",
            "....KKKKKKKK....",
            "......KWWK......",
            ".......KWK......",
            "........K.......",
        )),
        // The big red START button, unpressed, gleaming.
        "initiation" to Sprite(listOf(
            "..W..........W..",
            "................",
            "....KKKKKKKK....",
            "...KDDDDDDDDK...",
            "..KDDDDDDDDDDK..",
            "..KDDDDDDDDDDK..",
            "..KKKKKKKKKKKK..",
            "..KSSSSSSSSSSK..",
            "..KSSSSSSSSSSK..",
            "...KKKKKKKKKK...",
        )),
        // A grey blob snoozing under a drifting trail of Z's.
        "boredom" to Sprite(listOf(
            "..........WWWW..",
            "............W...",
            "...........W....",
            "..........WWWW..",
            "......WWW.......",
            ".......W........",
            "......WWW.......",
            "..PPPPPP........",
            ".PPPPPPPP.......",
            ".PKKPPKKP.......",
            ".PPPPPPPP.......",
            "..PPPPPP........",
        )),
        // A butterfly, and the dotted wander-path your attention takes after it.
        "attentionDrift" to Sprite(listOf(
            ".MMM.MMM........",
            ".MMMKMMM........",
            ".MMMKMMM........",
            ".MMM.MMM........",
            ".....W..........",
            ".......W........",
            "..........W.....",
            ".............W..",
            "...........W....",
            "..............W.",
        )),
    )

    /** The counter-move, drawn small: the item you USE on the trap. */
    val emblems: Map<String, Sprite> = mapOf(
        // One tiny step of the staircase over the wall.
        "dread" to Sprite(listOf(
            "..........",
            "........LL",
            "......LLLL",
            "....LLLLLL",
            "..LLLLLLLL",
            "LLLLLLLLLL",
        )),
        // The get-ready alarm clock.
        "beingLate" to Sprite(listOf(
            ".GG....GG.",
            "..KKKKKK..",
            ".KWWWWWWK.",
            ".KWWKWWWK.",
            ".KWWKKWWK.",
            ".KWWWWWWK.",
            "..KKKKKK..",
            ".KK....KK.",
        )),
        // The far-end-of-the-wait bell.
        "waitGap" to Sprite(listOf(
            "....GG....",
            "...GGGG...",
            "..GGGGGG..",
            "..GGGGGG..",
            ".GGGGGGGG.",
            "KKKKKKKKKK",
            "....KK....",
        )),
        // Boots pre-staged by the door.
        "transition" to Sprite(listOf(
            "..KKK.....",
            "..KGGK....",
            "..KGGK....",
            "..KGGKK...",
            "..KGGGGK..",
            ".KKKKKKKK.",
        )),
        // Your peak-window sun.
        "cognitiveLoad" to Sprite(listOf(
            "....G.....",
            ".G..G..G..",
            "..GGGGG...",
            "G.GGGGG.G.",
            "..GGGGG...",
            ".G..G..G..",
            "....G.....",
        )),
        // The checklist that remembers for you.
        "workingMemory" to Sprite(listOf(
            "KKKKKKKK..",
            "KWWWWWWK..",
            "KWLWKKKWK.",
            "KWWWWWWK..",
            "KWLWKKKWK.",
            "KWWWWWWK..",
            "KKKKKKKK..",
        )),
        // A pencil writing the one-sentence definition of done.
        "defineDone" to Sprite(listOf(
            ".......MM.",
            "......MMM.",
            ".....GGG..",
            "....GGG...",
            "...GGG....",
            "..KK......",
            "WWWW......",
            "..WWWWW...",
        )),
        // The single #1 next action.
        "prioritisation" to Sprite(listOf(
            "CCCCCCCCC.",
            "CCCCWCCCC.",
            "CCCWWCCCC.",
            "CCCCWCCCC.",
            "CCCCWCCCC.",
            "CCCWWWCCC.",
            "CCCCCCCCC.",
        )),
        // A finish flag you plant yourself.
        "noFinishLine" to Sprite(listOf(
            "KKWWKKWWK.",
            "KWWKKWWKK.",
            "KKWWKKWWK.",
            "KWWKKWWKK.",
            "K.........",
            "K.........",
            "K.........",
        )),
        // The buffered hourglass (guess, then add half again).
        "timeEstimation" to Sprite(listOf(
            "KKKKKKKK..",
            "KGGGGGGK..",
            ".KGGGGK...",
            "..KGGK....",
            "..KWWK....",
            ".KWWWWK...",
            "KWGGGGWK..",
            "KKKKKKKK..",
        )),
        // The 5-minute stopwatch.
        "initiation" to Sprite(listOf(
            "....CC....",
            "...CCCC...",
            ".KKKKKK...",
            "KWWWWWWK..",
            "KWWCWWWK..",
            "KWWCCWWK..",
            "KWWWWWWK..",
            ".KKKKKK...",
        )),
        // Headphones: bundle the dull thing with something alive.
        "boredom" to Sprite(listOf(
            "..MMMMM...",
            ".M.....M..",
            ".M.....M..",
            "MM.....MM.",
            "MM.....MM.",
        )),
        // The pomodoro: one tomato-timer block.
        "attentionDrift" to Sprite(listOf(
            "....LL....",
            "...LL.....",
            "..DDDDDD..",
            ".DDDDDDDD.",
            ".DDDDDDDD.",
            ".DDDDDDDD.",
            "..DDDDDD..",
        )),
    )
}
