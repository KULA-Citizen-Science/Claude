#!/usr/bin/env node
// Vocabulary scanner for building the EF classifier lexicon from YOUR real tasks.
//
//   node scan-vocab.mjs /path/to/YourVault [subfolder]
//
// Walks the vault (or an optional subfolder), finds TaskNotes tasks (frontmatter
// tag "task", or an inline #task), and extracts the words from each task title.
// Writes ef-vocab.txt with two frequency-ranked lists: the LEADING word of each
// title (usually the action verb) and ALL words. Paste ef-vocab.txt back to me
// and I'll turn it into a curated, categorised lexicon.
//
// Reads titles only (from frontmatter `title:` or the filename) — not task
// bodies. No dependencies, no network, nothing is modified in your vault.

import { readdir, readFile, writeFile, stat } from "node:fs/promises";
import { join, basename, extname } from "node:path";

const SKIP_DIRS = new Set([".obsidian", ".trash", ".git", "node_modules"]);
const TASK_TAG = "task"; // matches your taskTag setting

// Common German + English function words to drop (not action vocabulary).
const STOPWORDS = new Set([
  // de
  "der","die","das","den","dem","des","ein","eine","einen","einem","eines","und","oder","aber","mit","für","fuer",
  "von","vom","nach","bei","aus","auf","an","am","im","in","zu","zum","zur","um","als","wie","wenn","wann","dass",
  "ob","nicht","kein","keine","noch","schon","sich","ist","sind","war","werden","wird","hat","habe","haben","mein",
  "meine","gemeinsam","beide","beiden","wegen","über","ueber","the",
  // en
  "a","an","and","or","but","with","for","from","to","of","on","in","at","by","the","this","that","is","are","be",
  "as","if","when","about","my","our","your","no","new","get","do","done","make",
]);

/** Recursively collect all .md file paths under dir. */
async function walk(dir, out = []) {
  let entries;
  try {
    entries = await readdir(dir, { withFileTypes: true });
  } catch {
    return out;
  }
  for (const e of entries) {
    if (e.isDirectory()) {
      if (SKIP_DIRS.has(e.name)) continue;
      await walk(join(dir, e.name), out);
    } else if (extname(e.name) === ".md") {
      out.push(join(dir, e.name));
    }
  }
  return out;
}

/** Is this markdown file a TaskNotes task? Frontmatter tag "task" or inline #task. */
function isTask(text) {
  const fm = text.match(/^---\n([\s\S]*?)\n---/);
  if (fm) {
    const block = fm[1];
    // tags: [task, ...]  OR  tags:\n  - task
    const inline = block.match(/^tags:\s*\[([^\]]*)\]/m);
    if (inline && inline[1].split(",").some((t) => t.trim().replace(/["']/g, "") === TASK_TAG)) return true;
    const listMatch = block.match(/^tags:\s*\n((?:\s*-\s*.+\n?)+)/m);
    if (listMatch && listMatch[1].split("\n").some((l) => l.replace(/[-\s"']/g, "") === TASK_TAG)) return true;
  }
  return new RegExp(`(^|\\s)#${TASK_TAG}(\\s|$)`, "m").test(text);
}

/** Title from frontmatter `title:` if present, else the filename. */
function titleOf(text, path) {
  const m = text.match(/^---\n[\s\S]*?^title:\s*(.+?)\s*$/m);
  if (m) return m[1].replace(/^["']|["']$/g, "");
  return basename(path, ".md");
}

function tokenize(title) {
  return (title.toLowerCase().match(/[\p{L}][\p{L}0-9]{2,}/gu) ?? []).filter((w) => !STOPWORDS.has(w));
}

function ranked(counter) {
  return [...counter.entries()]
    .sort((a, b) => b[1] - a[1] || a[0].localeCompare(b[0]))
    .map(([w, n]) => `${String(n).padStart(4)}  ${w}`);
}

async function main() {
  const vault = process.argv[2];
  const sub = process.argv[3] ?? "";
  if (!vault) {
    console.error("Usage: node scan-vocab.mjs /path/to/YourVault [subfolder]");
    process.exit(1);
  }
  const root = sub ? join(vault, sub) : vault;
  try {
    await stat(root);
  } catch {
    console.error(`Path not found: ${root}`);
    process.exit(1);
  }

  const files = await walk(root);
  const allWords = new Map();
  const leadWords = new Map();
  let taskCount = 0;

  for (const file of files) {
    let text;
    try {
      text = await readFile(file, "utf8");
    } catch {
      continue;
    }
    if (!isTask(text)) continue;
    taskCount++;
    const tokens = tokenize(titleOf(text, file));
    if (tokens.length) leadWords.set(tokens[0], (leadWords.get(tokens[0]) ?? 0) + 1);
    for (const w of tokens) allWords.set(w, (allWords.get(w) ?? 0) + 1);
  }

  const report =
    `# EF vocabulary scan — ${taskCount} tasks scanned under ${root}\n\n` +
    `## LEADING WORDS (first word of each title — usually the action verb)\n` +
    ranked(leadWords).join("\n") +
    `\n\n## ALL WORDS (frequency-ranked)\n` +
    ranked(allWords).join("\n") +
    "\n";

  const out = "ef-vocab.txt";
  await writeFile(out, report);
  console.log(`Scanned ${taskCount} tasks, ${allWords.size} distinct words.`);
  console.log(`Wrote ${out} — paste its contents back to build the lexicon.`);
}

main().catch((e) => {
  console.error(e);
  process.exit(1);
});
