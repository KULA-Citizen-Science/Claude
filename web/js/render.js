// Canvas rendering for NextQuest: pixel sprites, the mascot, quest icons, and the scene starfield.
// Ported from the Compose code in app/.../ui/components/{PixelArt,SceneCard}.kt.

export const COLORS = {
  ink: "#120C24",
  cream: "#F4E9D0",
  danger: "#FF6B5E",
  gold: "#FFC247",
  cyan: "#49D6E0",
  magenta: "#E85AAD",
  lime: "#9BE24A",
  panelAlt: "#4A3A7A",
  panel: "#3B2E63",
  bevelDark: "#160F2C",
};

/** Sprite palette character → colour; anything else is transparent. */
const SPRITE_COLORS = {
  K: COLORS.ink,
  W: COLORS.cream,
  D: COLORS.danger,
  G: COLORS.gold,
  C: COLORS.cyan,
  M: COLORS.magenta,
  L: COLORS.lime,
  P: COLORS.panelAlt,
  S: COLORS.panel,
};

/** A <canvas> at the sprite's native pixel size; CSS scales it up crisp (see .sprite). */
export function spriteCanvas(rows, className = "sprite") {
  const canvas = document.createElement("canvas");
  canvas.className = className;
  canvas.width = rows[0].length;
  canvas.height = rows.length;
  const ctx = canvas.getContext("2d");
  rows.forEach((row, y) => {
    for (let x = 0; x < row.length; x++) {
      const color = SPRITE_COLORS[row[x]];
      if (color) {
        ctx.fillStyle = color;
        ctx.fillRect(x, y, 1, 1);
      }
    }
  });
  return canvas;
}

// A one-eyed blob "quest guide": cute + nerdy. '.' transparent.
// O = outline/dark, B = body, W = eye white, P = pupil, M = mouth.
const MASCOT = [
  "....OOOO....",
  "..OO....OO..",
  ".O..BBBB..O.",
  ".OBBBBBBBBO.",
  "OBBBBBBBBBBO",
  "OBBWWWWWWBBO",
  "OBBWPPPPWBBO",
  "OBBWWWWWWBBO",
  "OBBBBBBBBBBO",
  ".OBBMMMMBBO.",
  ".OOBBBBBBOO.",
  "...OO..OO...",
];

export function mascotCanvas(className = "mascot") {
  const canvas = document.createElement("canvas");
  canvas.className = className;
  canvas.width = 12;
  canvas.height = 12;
  const ctx = canvas.getContext("2d");
  MASCOT.forEach((row, y) => {
    for (let x = 0; x < row.length; x++) {
      const ch = row[x];
      const color = "OPM".includes(ch) ? COLORS.ink : ch === "B" ? COLORS.cyan : ch === "W" ? COLORS.cream : null;
      if (color) {
        ctx.fillStyle = color;
        ctx.fillRect(x, y, 1, 1);
      }
    }
  });
  return canvas;
}

/**
 * Hand-drawn glyph per archetype icon hint. Same geometry as PixelIcon in the Android app,
 * drawn at a 100×100 logical size and scaled by the canvas CSS size. `carve` is the surrounding
 * fill, used to notch shapes like the moon.
 */
export function iconCanvas(icon, { color = COLORS.cream, carve = COLORS.panelAlt, className = "icon" } = {}) {
  const canvas = document.createElement("canvas");
  canvas.className = className;
  const dpr = Math.max(1, Math.round(window.devicePixelRatio || 1));
  canvas.width = 100 * dpr;
  canvas.height = 100 * dpr;
  const ctx = canvas.getContext("2d");
  ctx.scale(dpr, dpr);
  const s = 100;
  const stroke = s * 0.1;
  ctx.strokeStyle = color;
  ctx.fillStyle = color;
  ctx.lineWidth = stroke;
  ctx.lineCap = "butt";

  const line = (x1, y1, x2, y2, w = stroke) => {
    ctx.lineWidth = w;
    ctx.beginPath();
    ctx.moveTo(x1 * s, y1 * s);
    ctx.lineTo(x2 * s, y2 * s);
    ctx.stroke();
  };
  const poly = (pts) => {
    ctx.beginPath();
    pts.forEach(([x, y], i) => (i ? ctx.lineTo(x * s, y * s) : ctx.moveTo(x * s, y * s)));
    ctx.closePath();
    ctx.fill();
  };
  const circle = (cx, cy, r, fill = false, c = color) => {
    ctx.beginPath();
    ctx.arc(cx * s, cy * s, r * s, 0, Math.PI * 2);
    if (fill) {
      ctx.fillStyle = c;
      ctx.fill();
      ctx.fillStyle = color;
    } else {
      ctx.lineWidth = stroke;
      ctx.stroke();
    }
  };
  const rad = (deg) => (deg * Math.PI) / 180;

  switch (icon) {
    case "form":
      ctx.lineWidth = stroke;
      ctx.strokeRect(0.22 * s, 0.12 * s, 0.56 * s, 0.76 * s);
      for (let i = 0; i < 3; i++) {
        const y = 0.32 + i * 0.18;
        line(0.33, y, 0.67, y, stroke * 0.8);
      }
      break;
    case "envelope":
      ctx.lineWidth = stroke;
      ctx.strokeRect(0.14 * s, 0.26 * s, 0.72 * s, 0.48 * s);
      line(0.14, 0.26, 0.5, 0.54, stroke * 0.8);
      line(0.86, 0.26, 0.5, 0.54, stroke * 0.8);
      break;
    case "clock":
      circle(0.5, 0.5, 0.34);
      line(0.5, 0.5, 0.5, 0.26, stroke);
      line(0.5, 0.5, 0.66, 0.56, stroke);
      break;
    case "mountain":
      poly([[0.12, 0.8], [0.44, 0.24], [0.62, 0.52], [0.74, 0.36], [0.9, 0.8]]);
      break;
    case "broom":
      line(0.68, 0.16, 0.4, 0.6, stroke);
      poly([[0.28, 0.6], [0.52, 0.6], [0.6, 0.86], [0.2, 0.86]]);
      break;
    case "moon":
      circle(0.46, 0.5, 0.32, true);
      circle(0.6, 0.42, 0.28, true, carve);
      break;
    case "bag":
      poly([[0.28, 0.42], [0.72, 0.42], [0.8, 0.84], [0.2, 0.84]]);
      ctx.lineWidth = stroke;
      ctx.beginPath();
      ctx.ellipse(0.5 * s, 0.34 * s, 0.14 * s, 0.14 * s, 0, rad(180), rad(360), false);
      ctx.stroke();
      break;
    case "question":
      ctx.lineWidth = stroke;
      ctx.beginPath();
      ctx.ellipse(0.5 * s, 0.32 * s, 0.22 * s, 0.2 * s, 0, rad(-210), rad(45), false);
      ctx.stroke();
      line(0.5, 0.48, 0.5, 0.66, stroke);
      circle(0.5, 0.84, 0.07, true);
      break;
    default:
      circle(0.5, 0.5, 0.3);
  }
  return canvas;
}

/** Per-archetype sky tint, so each quest's scene reads as its own "room". */
export const SKY = {
  form: "#33285A",      // violet office dusk
  envelope: "#432B5E",  // plum
  clock: "#23305C",     // waiting-room blue
  mountain: "#2C3A63",  // steel-blue expedition
  broom: "#3D2E4C",     // mauve household
  moon: "#1B1638",      // deep night
  bag: "#234A4F",       // teal street
  question: "#3B2E63",  // custom quest
};

/** Deterministic 32-bit string hash → seed (stable across sessions and devices). */
export function hashSeed(str) {
  let h = 2166136261 >>> 0;
  for (let i = 0; i < str.length; i++) {
    h ^= str.charCodeAt(i);
    h = Math.imul(h, 16777619) >>> 0;
  }
  return h >>> 0;
}

/** mulberry32 PRNG: small, fast, seedable. */
export function mulberry32(seed) {
  let a = seed >>> 0;
  return () => {
    a = (a + 0x6d2b79f5) >>> 0;
    let t = a;
    t = Math.imul(t ^ (t >>> 15), t | 1);
    t ^= t + Math.imul(t ^ (t >>> 7), t | 61);
    return ((t ^ (t >>> 14)) >>> 0) / 4294967296;
  };
}

/**
 * Paint the scene's starfield and ground into `canvas`, sized to its CSS box and redrawn on resize.
 * Star placement is seeded per (quest, trap), so every combination is its own unique picture.
 */
export function paintStarfield(canvas, seedKey) {
  const draw = () => {
    const dpr = window.devicePixelRatio || 1;
    const w = canvas.clientWidth;
    const h = canvas.clientHeight;
    if (!w || !h) return;
    canvas.width = Math.round(w * dpr);
    canvas.height = Math.round(h * dpr);
    const ctx = canvas.getContext("2d");
    ctx.scale(dpr, dpr);
    const rng = mulberry32(hashSeed(seedKey));
    for (let i = 0; i < 22; i++) {
      const x = rng() * w;
      const y = rng() * h * 0.62;
      const big = rng() < 0.2;
      ctx.fillStyle = `rgba(244,233,208,${big ? 0.8 : 0.35})`;
      const sz = big ? 5 : 3;
      ctx.fillRect(x, y, sz, sz);
    }
    const groundTop = h * 0.78;
    ctx.fillStyle = COLORS.ink;
    ctx.fillRect(0, groundTop, w, 3);
    ctx.fillStyle = COLORS.bevelDark;
    ctx.fillRect(0, groundTop + 3, w, h - groundTop - 3);
  };
  new ResizeObserver(draw).observe(canvas);
  draw();
}
