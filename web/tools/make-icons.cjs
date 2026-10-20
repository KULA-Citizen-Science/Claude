// Render the PNG icons from icons/icon.svg with headless Chromium (Playwright).
// Usage (repo root): NODE_PATH="$(npm root -g)" node web/tools/make-icons.cjs   (needs `playwright` installed globally)
const { chromium } = require("playwright");
const fs = require("fs");
const path = require("path");

const dir = path.resolve(__dirname, "../icons");
const gem = (inner) => `
  <path fill="#FFC247" d="M54,18 L90,54 L54,90 L18,54 Z"/>
  <path fill="#F4E9D0" d="M54,34 L74,54 L54,74 L34,54 Z"/>
  <path fill="#49D6E0" d="M54,43 L65,54 L54,65 L43,54 Z"/>`;

const variants = [
  // any-purpose: rounded tile
  { file: "icon-192.png", size: 192, svg: fs.readFileSync(path.join(dir, "icon.svg"), "utf8") },
  { file: "icon-512.png", size: 512, svg: fs.readFileSync(path.join(dir, "icon.svg"), "utf8") },
  // maskable: full-bleed background, gem shrunk into the central 80% safe zone
  {
    file: "icon-maskable-512.png", size: 512,
    svg: `<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 108 108"><rect width="108" height="108" fill="#2B2140"/>
      <g transform="translate(54 54) scale(0.78) translate(-54 -54)">${gem()}</g></svg>`,
  },
];

(async () => {
  const browser = await chromium.launch();
  for (const v of variants) {
    const page = await browser.newPage({ viewport: { width: v.size, height: v.size } });
    await page.setContent(`<body style="margin:0;background:transparent">${v.svg.replace("<svg ", `<svg width="${v.size}" height="${v.size}" `)}</body>`);
    await page.screenshot({ path: path.join(dir, v.file), omitBackground: true });
    await page.close();
    console.log("wrote", v.file);
  }
  await browser.close();
})();
