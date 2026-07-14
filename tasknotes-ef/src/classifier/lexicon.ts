// Data-only lexicon. Keyword cues matched (case-insensitively, as substrings so
// stems like "apolog" catch "apologise/apologize/apology") against the task's
// title + tags + contexts. Tuning the classifier means editing this file, not
// the scoring logic. Terms are intentionally conservative; the structural
// signals in classify.ts carry the rest.

import type { Category } from "./types";

export interface Cue {
  category: Category;
  weight: number;
  terms: string[];
}

/** Each cue-group contributes its `weight` at most once per task (first hit
 *  wins), so a title packed with synonyms doesn't runaway-score one category. */
export const TITLE_CUES: Cue[] = [
  {
    category: "planning",
    weight: 2,
    terms: ["plan", "outline", "roadmap", "strategy", "break down", "breakdown", "figure out", "scope", "sketch out", "design a"],
  },
  {
    category: "decision",
    weight: 2,
    terms: ["decide", "choose", "pick ", "select", "evaluate", "compare", "which ", "whether", "option", "vs "],
  },
  {
    category: "organization",
    weight: 2,
    terms: ["organize", "organise", "sort", "file ", "tidy", "clean up", "gather", "collect", "look up", "research", "compile", "catalog", "find "],
  },
  {
    category: "focus",
    weight: 2,
    terms: ["write", "draft", "read ", "review", "study", "analyze", "analyse", "implement", "edit ", "report", "chapter", "essay"],
  },
  {
    category: "initiation",
    weight: 2,
    terms: ["start", "begin", "set up", "kick off", "kickoff", "initiate", "get going", "create a"],
  },
  {
    category: "emotional",
    weight: 3,
    terms: ["taxes", "dentist", "doctor", "apolog", "confront", "difficult conversation", "cancel", "complaint", "insurance", "argument", "chase up", "overdue bill"],
  },
  {
    category: "routine",
    weight: 2,
    terms: ["water the", "take out", "laundry", "dishes", "standup", "check email", "backup", "chores", "refill", "restock"],
  },
];

// Social-demand cues. Live (synchronous) beats async when both are present.
export const SOCIAL_LIVE_TERMS = ["call", "meet", "meeting", "interview", "standup", "sync", "1:1", "one-on-one", "zoom", "present to", "catch up with"];
export const SOCIAL_ASYNC_TERMS = ["email", "reply", "message", "text ", "dm ", "slack", "respond", "write to", "send "];

export const SOCIAL_LIVE_CONTEXTS = ["phone", "call", "meeting", "zoom", "office"];
export const SOCIAL_ASYNC_CONTEXTS = ["email", "slack", "online"];
