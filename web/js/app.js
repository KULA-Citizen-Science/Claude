// NextQuest UI: Home / Picker / Reading, as a tiny hash-routed single page.
//   #/            home
//   #/pick        choose a quest
//   #/read/<id>/<trap>            archetype reading (trap = index into the ranked list)
//   #/read/custom/<7 bits>/<trap> "Other…" reading, e.g. #/read/custom/1000011/0
// Hash routes keep deep links and the Android back button working on any static host,
// including a subpath (GitHub Pages, Codeberg Pages) with no server rewrites.

import {
  Archetypes, CUSTOM_TOGGLES, EVIDENCE, Frameworks, FrictionEngine, customActivity,
} from "./engine.js";
import { TRAPS, EMBLEMS } from "./art.js";
import {
  COLORS, SKY, iconCanvas, mascotCanvas, paintStarfield, spriteCanvas,
} from "./render.js";

const app = document.getElementById("app");

/** Tiny hyperscript helper. */
function h(tag, attrs = {}, ...children) {
  const el = document.createElement(tag);
  for (const [k, v] of Object.entries(attrs)) {
    if (v === false || v == null) continue;
    if (k === "class") el.className = v;
    else if (k === "style") el.style.cssText = v;
    else if (k.startsWith("on")) el.addEventListener(k.slice(2), v);
    else el.setAttribute(k, v === true ? "" : v);
  }
  for (const c of children.flat()) {
    if (c == null || c === false) continue;
    el.append(c.nodeType ? c : document.createTextNode(c));
  }
  return el;
}

// ---------------------------------------------------------------------------- navigation

const depth = () => history.state?.d ?? 0;

function go(hash) {
  history.pushState({ d: depth() + 1 }, "", hash);
  render();
}

/** Back one step if we have in-app history; otherwise jump to the parent route. */
function back(fallbackHash) {
  if (depth() > 0) history.back();
  else {
    history.replaceState({ d: 0 }, "", fallbackHash);
    render();
  }
}

function parseRoute() {
  const parts = location.hash.replace(/^#\/?/, "").split("/").filter(Boolean);
  if (parts[0] === "pick") return { name: "pick" };
  if (parts[0] === "read") {
    const [, id, extra, trap] = parts;
    if (id === "custom" && /^[01]{7}$/.test(extra || "")) {
      return { name: "read", id, bits: extra, trap: parseInt(trap, 10) || 0 };
    }
    const arch = Archetypes.find((a) => a.id === id);
    if (arch) return { name: "read", id, trap: parseInt(extra, 10) || 0 };
  }
  return { name: "home" };
}

// ---------------------------------------------------------------------------- components

const btn = (text, onclick, cls = "") =>
  h("button", { class: `btn bevel ${cls}`, type: "button", onclick }, text);

const panel = (...children) => h("div", { class: "panel bevel" }, ...children);

function frameworkChip(frameworkKey) {
  const f = Frameworks[frameworkKey];
  const tier = EVIDENCE[f.tier];
  return h(
    "div",
    { class: "chip bevel", title: f.source },
    h("span", { class: f.tier === "PEER_REVIEWED" ? "mk-peer" : "mk-heur" }, tier.marker),
    h("span", {}, f.tag.toUpperCase()),
  );
}

function toggleRow(label, checked, onToggle) {
  const row = h(
    "button",
    { class: "toggle", type: "button", role: "checkbox", "aria-checked": String(checked) },
    h("span", { class: `box bevel${checked ? " on inset" : ""}` }, checked ? "X" : ""),
    h("span", {}, label),
  );
  row.addEventListener("click", onToggle);
  return row;
}

/** The point-and-click scene: quest-tinted room, mascot, trap sprite, escape item in an inventory slot. */
function sceneCard(icon, frictionId) {
  const sky = SKY[icon] || SKY.question;
  const stars = h("canvas", { class: "scene-bg" });
  const scene = h(
    "div",
    { class: "scene bevel inset", style: `--fill:${sky}`, role: "img", "aria-label": "Scene: the trap and its escape" },
    stars,
    h("div", { class: "scene-prop" }, iconCanvas(icon, { color: COLORS.cream, carve: sky })),
    h("div", { class: "scene-hero" }, mascotCanvas()),
    h("div", { class: "scene-trap" }, spriteCanvas(TRAPS[frictionId])),
    h(
      "div",
      { class: "scene-inv" },
      h("span", { class: "use" }, "USE:"),
      h("div", { class: "slot bevel inset" }, spriteCanvas(EMBLEMS[frictionId])),
    ),
  );
  // Starfield needs the element laid out to size itself.
  requestAnimationFrame(() => paintStarfield(stars, icon + frictionId));
  return scene;
}

// ---------------------------------------------------------------------------- screens

function homeScreen() {
  return h(
    "main",
    { class: "screen home" },
    h(
      "div",
      { class: "logo" },
      h("span", { class: "spark", "aria-hidden": "true" }, "***"),
      h("div", { class: "title" }, h("span", { class: "gold" }, "NEXT"), h("span", { class: "cyan" }, "QUEST")),
      h("span", { class: "spark", "aria-hidden": "true" }, "***"),
    ),
    h("div", { class: "mascot-lg" }, mascotCanvas()),
    h("p", { class: "dim lead" }, "Pick your next thing. I'll flag where it'll trip you up — and one move past it."),
    btn(">>  WHAT'S MY NEXT QUEST?", () => go("#/pick"), "gold-btn"),
    h("p", { class: "dim small" }, "A nudge tool, not medical advice."),
  );
}

function pickerScreen() {
  const bits = [false, false, false, false, false, false, false];
  let showCustom = false;
  const customHost = h("div");

  const renderCustom = () => {
    customHost.replaceChildren();
    if (!showCustom) return;
    customHost.append(
      panel(
        h("div", { class: "label gold" }, "DESCRIBE IT"),
        CUSTOM_TOGGLES.map((t, i) =>
          toggleRow(t.label, bits[i], () => {
            bits[i] = !bits[i];
            renderCustom();
          }),
        ),
        btn("READ MY QUEST", () => go(`#/read/custom/${bits.map((b) => (b ? 1 : 0)).join("")}/0`), "gold-btn block"),
      ),
    );
  };

  const otherBtn = btn("OTHER...  (describe it)", () => {
    showCustom = !showCustom;
    otherBtn.textContent = showCustom ? "OTHER... (below)" : "OTHER...  (describe it)";
    renderCustom();
  }, "magenta-btn block");

  return h(
    "main",
    { class: "screen" },
    btn("< BACK", () => back("#/"), "back"),
    h("h1", { class: "heading gold" }, "CHOOSE YOUR QUEST"),
    h(
      "div",
      { class: "grid" },
      Archetypes.map((arch) =>
        h(
          "button",
          { class: "card bevel", type: "button", onclick: () => go(`#/read/${arch.id}/0`) },
          iconCanvas(arch.icon, { color: COLORS.cyan, carve: COLORS.panel, className: "icon" }),
          h("span", { class: "small" }, arch.activity.name.toUpperCase()),
        ),
      ),
    ),
    otherBtn,
    customHost,
  );
}

function readingScreen(route) {
  const isCustom = route.id === "custom";
  const act = isCustom ? customActivity(route.bits) : Archetypes.find((a) => a.id === route.id).activity;
  const icon = isCustom ? "question" : Archetypes.find((a) => a.id === route.id).icon;
  const points = FrictionEngine.analyze(act);
  const index = ((route.trap % points.length) + points.length) % points.length;
  const fp = points[index];

  const base = isCustom ? `#/read/custom/${route.bits}` : `#/read/${route.id}`;
  const next = () => {
    // Cycling traps replaces the entry so Back still goes straight to the picker.
    history.replaceState(history.state, "", `${base}/${(index + 1) % points.length}`);
    render();
  };

  return h(
    "main",
    { class: "screen" },
    btn("< NEW QUEST", () => back("#/pick"), "back"),
    h("h1", { class: "heading gold" }, act.name.toUpperCase()),
    h("div", { class: "dim small" }, `Trap ${index + 1} of ${points.length}`),
    sceneCard(icon, fp.id),
    panel(
      h("div", { class: "label danger" }, "!  THE TRAP"),
      h("p", { class: "trap-text" }, fp.trap),
      frameworkChip(fp.framework),
      h("div", { class: "label lime", style: "margin-top:16px" }, ">  THE ESCAPE"),
      h("p", { class: "escape-text" }, fp.strategy),
    ),
    points.length > 1 ? btn("NEXT TRAP >", next, "cyan-btn block") : null,
    h(
      "div",
      { class: "legend small dim" },
      h("span", { class: "sw lime-bg" }),
      `${EVIDENCE.PEER_REVIEWED.marker} peer-reviewed`,
      h("span", { class: "sw gold-bg" }),
      `${EVIDENCE.CLINICAL_HEURISTIC.marker} heuristic`,
    ),
  );
}

// ---------------------------------------------------------------------------- render

function render() {
  const route = parseRoute();
  const screen = route.name === "pick" ? pickerScreen() : route.name === "read" ? readingScreen(route) : homeScreen();
  app.replaceChildren(screen);
  window.scrollTo(0, 0);
}

window.addEventListener("popstate", render);
if (!history.state) history.replaceState({ d: 0 }, "");
render();

// Offline support. Relative path so it works under a subpath host.
if ("serviceWorker" in navigator) {
  window.addEventListener("load", () => navigator.serviceWorker.register("./sw.js").catch(() => {}));
}
