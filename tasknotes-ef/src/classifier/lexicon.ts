// Data-only lexicon. Keyword cues matched (case-insensitively, as substrings so
// stems like "apolog" catch "apologise/apologize/apology") against the task's
// title + tags + contexts. Tuning the classifier means editing this file, not
// the scoring logic. Terms are intentionally conservative; the structural
// signals in classify.ts carry the rest.
//
// Bilingual EN + DE. German nouns are capitalised in prose but matched
// lower-cased here (the blob is lower-cased before matching). German umlauts
// appear both as ü/ä/ö and as ue/ae/oe transliterations in the wild, so both
// forms are listed where a term is likely to show up either way.

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
    terms: [
      // en
      "plan", "outline", "roadmap", "strategy", "break down", "breakdown", "figure out", "scope", "sketch out", "design a",
      // de
      "planen", "überleg", "ueberleg", "konzept", "entwurf", "strukturier", "vorbereiten", "ausarbeiten", "erarbeiten",
    ],
  },
  {
    category: "decision",
    weight: 2,
    terms: [
      // en
      "decide", "choose", "pick ", "select", "evaluate", "compare", "which ", "whether", "option", "vs ",
      // de
      "entscheiden", "auswählen", "auswaehlen", "wählen", "waehlen", "vergleichen", " ob ",
    ],
  },
  {
    category: "organization",
    weight: 2,
    terms: [
      // en
      "organize", "organise", "sort", "file ", "tidy", "clean up", "gather", "collect", "look up", "research", "compile", "catalog", "find ",
      // de
      "organisier", "sortier", "einsammeln", "sammeln", "suchen", "nachsehen", "nachschauen", "nachschlagen", "prüfen", "pruefen",
      "liste", "raussuchen", "mitnehmen", "einpacken", "packen",
    ],
  },
  {
    category: "focus",
    weight: 2,
    terms: [
      // en
      "write", "draft", "read ", "review", "study", "analyze", "analyse", "implement", "edit ", "report", "chapter", "essay",
      // de
      "schreiben", "lesen", "bericht", "nachbereiten", "posten", "blogpost", "notizen",
    ],
  },
  {
    category: "initiation",
    weight: 2,
    terms: [
      // en
      "start", "begin", "set up", "kick off", "kickoff", "initiate", "get going", "create a", "reserve", "booking",
      // de
      "anfangen", "beginnen", "starten", "loslegen", "einrichten", "erstellen", "besorgen", "buchen",
    ],
  },
  {
    category: "emotional",
    weight: 3,
    terms: [
      // en
      "taxes", "dentist", "doctor", "apolog", "confront", "difficult conversation", "cancel", "complaint", "insurance", "argument",
      "chase up", "overdue bill", "reimburse", "invoice", "refund",
      // de
      "steuer", "arzt", "rechnung", "abrechnung", "versicherung", "kündigen", "kuendigen", "mahnung", "ausgleichen", "erstattung",
    ],
  },
  {
    category: "routine",
    weight: 2,
    terms: [
      // en
      "water the", "take out", "laundry", "dishes", "standup", "check email", "backup", "chores", "refill", "restock",
      // de
      "einkaufen", "putzen", "wäsche", "waesche", "müll", "muell", "nachfüllen", "nachfuellen", "gießen", "giessen",
    ],
  },
];

// Social-demand cues. Live (synchronous) beats async when both are present.
export const SOCIAL_LIVE_TERMS = [
  // en
  "call", "meet", "meeting", "interview", "standup", "sync", "1:1", "one-on-one", "zoom", "present to", "catch up with",
  // de
  "anrufen", "anruf", "telefon", "treffen", "zusammensetzen", "besprechen", "gespräch", "gespraech",
];
export const SOCIAL_ASYNC_TERMS = [
  // en
  "email", "reply", "message", "text ", "dm ", "slack", "respond", "write to", "send ",
  // de
  "e-mail", "mailen", "senden", "schicken", "antworten", "nachricht", "posten",
];

export const SOCIAL_LIVE_CONTEXTS = ["phone", "call", "meeting", "zoom", "office", "telefon"];
export const SOCIAL_ASYNC_CONTEXTS = ["email", "slack", "online", "mail"];
