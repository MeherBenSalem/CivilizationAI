// Renders the listing artwork in marketing/images/ from real in-game captures (marketing/images/screenshots/) and the
// bundled OFL fonts. Usage (from the repository root): NODE_PATH="$(npm root -g)" node marketing/source/render.cjs [name...]
// Requires the `playwright` package with a Chromium build available.
const path = require('path');
const fs = require('fs');
const { chromium } = require('playwright');

// The release and the supported Minecraft versions. Adding a version is one line here
// (plus one row in the download table of curseforge/description.md).
const RELEASE = { version: '2.0', name: 'The Village Square Update' };
const VERSIONS = [
  { minecraft: '1.20.1', loaders: ['Fabric', 'Forge'] },
  { minecraft: '1.21.1', loaders: ['Fabric', 'Forge', 'NeoForge'] },
  { minecraft: '26.1.2', loaders: ['Fabric', 'NeoForge'] },
  { minecraft: '26.2', loaders: ['Fabric', 'NeoForge'] },
  { minecraft: '26.3', loaders: ['Fabric', 'NeoForge'] },
];
const LOADERS = ['Fabric', 'Forge', 'NeoForge'];

// The AI providers the config offers presets for (ai.provider), plus any OpenAI-compatible endpoint.
const PROVIDERS = ['DeepSeek', 'OpenAI', 'OpenRouter', 'NanoGPT', 'Ollama', 'LM Studio', 'Custom endpoint'];

const root = path.resolve(__dirname, '..', '..');
const out = path.join(root, 'marketing', 'images');
const shots = path.join(out, 'screenshots');
const fonts = path.join(__dirname, 'fonts');
const url = (file) => 'file://' + file;
const shot = (name) => url(path.join(shots, name));

// Palette: a village square at noon. Cream plaster, oak, emerald (the villagers' currency), terracotta roofs.
const C = { cream: '#fbf3e2', paper: '#f3e6c9', ink: '#3a2a1c', dim: '#7a6450', oak: '#8a5a33', emerald: '#22a35a',
  emeraldDark: '#15703c', mint: '#bdf0cf', terracotta: '#d0663c', sky: '#bfe3ff', gold: '#f0b429' };

const css = `
@font-face { font-family: Display; src: url('${url(path.join(fonts, 'fredoka.woff2'))}'); font-weight: 300 700; font-stretch: 75% 125%; }
@font-face { font-family: Body; src: url('${url(path.join(fonts, 'nunito.woff2'))}'); font-weight: 200 1000; }
* { box-sizing: border-box; }
html, body { margin: 0; background: ${C.cream}; color: ${C.ink}; font-family: Body, sans-serif; font-weight: 600; }
.stage { position: relative; overflow: hidden; background:
  radial-gradient(circle at 12% 18%, rgba(255,255,255,.9) 0, rgba(255,255,255,0) 38%),
  radial-gradient(circle at 88% 92%, rgba(34,163,90,.10) 0, rgba(34,163,90,0) 40%), ${C.cream}; }
.dots { position: absolute; inset: 0; opacity: .5;
  background-image: radial-gradient(rgba(138,90,51,.16) 1.6px, transparent 1.7px); background-size: 26px 26px; }
.display { font-family: Display, sans-serif; font-weight: 700; letter-spacing: -.01em; }
.kicker { display: inline-block; font-weight: 900; font-size: 20px; letter-spacing: .16em; text-transform: uppercase; color: #fff;
  background: ${C.terracotta}; padding: 7px 16px 6px; border-radius: 999px; }
/* A speech bubble: rounded box with a tail. */
.bubble { position: relative; background: #fff; border: 4px solid ${C.ink}; border-radius: 26px; box-shadow: 0 8px 0 rgba(58,42,28,.18); }
.bubble::after { content: ''; position: absolute; left: var(--tail, 60px); bottom: -26px; width: 34px; height: 34px; background: #fff;
  border-right: 4px solid ${C.ink}; border-bottom: 4px solid ${C.ink}; transform: skewX(-28deg) rotate(45deg); border-radius: 0 0 8px 0; }
.bubble.green { background: ${C.emerald}; color: #fff; } .bubble.green::after { background: ${C.emerald}; }
.bubble.dark { background: ${C.ink}; color: ${C.cream}; } .bubble.dark::after { background: ${C.ink}; }
.photo { position: relative; overflow: hidden; border: 5px solid ${C.ink}; border-radius: 22px; background: #000;
  box-shadow: 0 10px 0 rgba(58,42,28,.22), 0 30px 60px rgba(58,42,28,.25); }
.photo > div > img { display: block; }
.chip { display: inline-flex; align-items: center; gap: 8px; font-weight: 900; font-size: 18px; letter-spacing: .06em; text-transform: uppercase;
  padding: 7px 14px 6px; border-radius: 12px; background: #fff; border: 3px solid ${C.ink}; color: ${C.ink}; }
ul.points { list-style: none; padding: 0; margin: 0; }
ul.points li { position: relative; padding-left: 40px; margin: 0 0 16px; font-size: 27px; line-height: 1.3; font-weight: 700; }
ul.points li::before { content: ''; position: absolute; left: 2px; top: 8px; width: 22px; height: 22px; border-radius: 6px;
  background: ${C.emerald}; transform: rotate(45deg) scale(.78); box-shadow: inset -4px -4px 0 ${C.emeraldDark}; }
b.hi { color: ${C.emeraldDark}; font-weight: 900; }
.stats { display: flex; gap: 14px; margin-top: 24px; }
.stats > div { flex: 1; background: #fff; border: 3px solid ${C.ink}; border-radius: 16px; padding: 10px 18px 8px; box-shadow: 0 6px 0 rgba(58,42,28,.16); }
.stats .t { font-weight: 900; font-size: 15px; letter-spacing: .14em; color: ${C.dim}; text-transform: uppercase; }
.stats .v { font-family: Display, sans-serif; font-weight: 700; font-size: 32px; color: ${C.ink}; white-space: nowrap; }
.mc { font-family: 'DejaVu Sans Mono', monospace; }
`;

const page = (width, height, body, transparent = false) => `<!doctype html><html><head><meta charset="utf-8"><style>${css}
body { width: ${width}px; height: ${height}px; } .stage { width: ${width}px; height: ${height}px; }
${transparent ? 'html, body, .stage { background: transparent; }' : ''}</style></head>
<body><div class="stage">${body}</div></body></html>`;

// An exact region (x, y, w, h in 1920x1080 pixels) of a capture, scaled to W pixels wide.
const region = (file, x, y, w, h, W) => `<div style="position:relative;width:${W}px;height:${Math.round(W * h / w)}px;overflow:hidden">
  <img src="${shot(file)}" style="position:absolute;width:${Math.round(1920 * W / w)}px;left:${-Math.round(x * W / w)}px;top:${-Math.round(y * W / w)}px"></div>`;
const photo = (file, crop, W, style = '') => `<div class="photo" style="${style}">${region(file, ...crop, W)}</div>`;
const stats = (items) => `<div class="stats">${items.map(([t, v]) => `<div><div class="t">${t}</div><div class="v">${v}</div></div>`).join('')}</div>`;

// The villager's face in pixels (front of the head, plus the nose hanging below it).
const FACE = [
  '.hhhhhhhh.', '.hhhhhhhh.', '.ssssssss.', '.bbbbbbbb.', '.swgnngws.',
  '.ssdnndss.', '.sssnnsss.', '.dssnnssd.', '.dddnnddd.', '....nn....',
];
const FACE_COLORS = { h: '#5b3a24', s: '#c4906f', d: '#a7725a', b: '#3d2a1e', w: '#f4f1ea', g: '#2c9a4b', n: '#9c5f47', '.': 'transparent' };
const villagerFace = (px) => `<div style="display:grid;grid-template-columns:repeat(10,${px}px);grid-auto-rows:${px}px">${FACE.map((row) =>
  row.padEnd(10, '.').split('').map((c) => `<div style="background:${FACE_COLORS[c]}"></div>`).join('')).join('')}</div>`;

const emerald = (size) => `<div style="width:${size}px;height:${size}px;transform:rotate(45deg);border-radius:${size / 5}px;
  background:linear-gradient(135deg,#7df0a8 0%,${C.emerald} 45%,${C.emeraldDark} 100%);box-shadow:inset -${size / 8}px -${size / 8}px 0 rgba(0,0,0,.18), 0 4px 0 rgba(58,42,28,.25)"></div>`;

// A feature card: copy on the left, artwork on the right.
const card = ({ kicker, title, points, art, width = 1600, height = 900, artWidth = 900 }) => page(width, height, `
  <div class="dots"></div>
  <div style="position:absolute;left:72px;top:78px;width:${width - artWidth - 190}px">
    <span class="kicker">${kicker}</span>
    <div class="display" style="font-size:78px;line-height:.98;margin:26px 0 30px">${title}</div>
    <ul class="points">${points.map((p) => `<li>${p}</li>`).join('')}</ul>
  </div>
  <div style="position:absolute;right:66px;top:70px;width:${artWidth}px">${art}</div>
  <div style="position:absolute;left:72px;bottom:36px;font-weight:900;font-size:17px;letter-spacing:.14em;color:${C.dim};text-transform:uppercase">
    Smart Villagers AI ${RELEASE.version} · ${VERSIONS.map((v) => v.minecraft).join(' · ')}</div>`);

// Transparent section header: the title in a speech bubble with the villager's face.
const header = (title) => page(1400, 120, `
  <div style="position:absolute;left:0;top:10px;display:flex;align-items:center;gap:22px">
    <div style="width:84px;height:84px;border-radius:20px;background:${C.emerald};border:4px solid ${C.ink};display:flex;align-items:center;justify-content:center">
      ${villagerFace(6)}</div>
    <div class="bubble" style="--tail:-6px;padding:12px 34px 10px;border-radius:24px">
      <div class="display" style="font-size:46px;line-height:1.05;white-space:nowrap">${title}</div></div>
  </div>`, true);

const divider = () => page(1400, 44, `
  <div style="position:absolute;left:0;right:0;top:20px;height:4px;border-radius:4px;background:repeating-linear-gradient(90deg,${C.oak} 0 26px,transparent 26px 40px)"></div>
  <div style="position:absolute;left:50%;top:4px;transform:translateX(-50%);display:flex;gap:18px">${[0, 1, 2].map(() => emerald(26)).join('')}</div>`, true);

const providerChips = () => `<div style="display:flex;flex-wrap:wrap;gap:12px">${PROVIDERS.map((p) =>
  `<span class="chip">${p}</span>`).join('')}</div>`;

const standingLadder = () => {
  const steps = [['Hostile', '#b23a2a'], ['Wary', C.terracotta], ['Stranger', '#8d7a66'], ['Friend', C.emerald], ['Hero', C.gold]];
  return `<div style="display:flex;align-items:center;gap:10px">${steps.map(([s, c], i) =>
    `${i ? `<div style="flex:0 0 22px;height:4px;background:${C.ink};border-radius:2px"></div>` : ''}
     <span class="chip" style="background:${c};color:#fff;border-color:${C.ink}">${s}</span>`).join('')}</div>`;
};

const assets = {
  icon: () => page(512, 512, `
    <div style="position:absolute;inset:0;border-radius:96px;background:linear-gradient(160deg,#3cc574 0%,${C.emerald} 45%,${C.emeraldDark} 100%);
      box-shadow:inset 0 -18px 0 rgba(0,0,0,.18)"></div>
    <div class="bubble" style="position:absolute;left:66px;top:62px;width:380px;height:318px;border-width:10px;border-radius:70px;--tail:70px;
      box-shadow:0 14px 0 rgba(0,0,0,.2);display:flex;align-items:center;justify-content:center">
      ${villagerFace(25)}</div>
    <div style="position:absolute;right:52px;bottom:46px">${emerald(92)}</div>`, true),

  banner: () => page(1800, 600, `
    <div class="dots"></div>
    <div style="position:absolute;right:-30px;top:0;bottom:0;width:1020px">
      <img src="${shot('square.jpg')}" style="position:absolute;inset:0;width:100%;height:100%;object-fit:cover;object-position:30% 50%">
      <div style="position:absolute;inset:0;background:linear-gradient(90deg,${C.cream} 0%,rgba(251,243,226,.6) 18%,rgba(251,243,226,0) 42%)"></div>
    </div>
    <div style="position:absolute;left:84px;top:70px;display:flex;align-items:center;gap:26px">
      <div style="width:150px;height:150px">${'<img src="' + url(path.join(out, 'icon.png')) + '" style="width:150px;height:150px">'}</div>
      <div>
        <div class="display" style="font-size:104px;line-height:.92">Smart Villagers</div>
        <div class="display" style="font-size:104px;line-height:.92;color:${C.emerald}">AI</div>
      </div>
    </div>
    <div class="bubble" style="position:absolute;left:84px;top:330px;width:840px;padding:22px 30px 20px;--tail:70px">
      <div style="font-size:33px;line-height:1.22;font-weight:800">Villagers who talk back, remember you and ask for your help. With any AI, or none at all.</div>
    </div>
    <div style="position:absolute;left:84px;bottom:46px;display:flex;gap:12px">
      <span class="kicker" style="background:${C.emerald}">${RELEASE.version} · ${RELEASE.name}</span>
      <span class="kicker" style="background:${C.ink}">${LOADERS.join(' · ')}</span>
      <span class="kicker" style="background:${C.oak}">${VERSIONS.map((v) => v.minecraft).join(' · ')}</span>
    </div>`),

  'card-requests': () => card({
    kicker: 'New in 2.0', title: 'Villagers<br>who need you',
    points: ['Ask <b class="hi">“any work for me?”</b> in your own words', 'Each trade wants its own things; a third of the jobs are monster hunts',
      'Accept by saying yes or clicking <b class="hi">[Accept]</b>', 'Finish it: emeralds, XP and better standing'],
    art: `${photo('offer.jpg', [0, 170, 1360, 880], 900)}
      <div style="position:relative;margin-top:-6px">${stats([['Professions', '13'], ['Kinds', 'Fetch · Hunt'], ['AI needed', 'No']])}</div>` }),

  'card-bubbles': () => card({
    kicker: 'Speech bubbles', title: 'See who’s<br>talking',
    points: ['Replies float above the villager who said them', 'A “…” bubble while the AI is thinking',
      'Every player standing nearby sees the conversation', 'Gone after a few seconds, never left behind'],
    art: photo('crowd.jpg', [300, 150, 1300, 820], 900) }),

  'card-reputation': () => card({
    kicker: 'Reputation & gossip', title: 'Word gets<br>around',
    points: ['Finished requests add vanilla positive gossip: villagers trade it and it lowers prices',
      'Neighbours hear what you did and bring it up', 'Hurt a villager and the village won’t give you work',
      'Ten favours make you a hero to every villager in the world'],
    art: `${photo('gossip.jpg', [0, 120, 1500, 960], 900)}<div style="margin-top:28px">${standingLadder()}</div>` }),

  'card-ai': () => card({
    kicker: 'Your choice of brain', title: 'Any AI,<br>or none',
    points: ['Presets for cloud providers, local models and any OpenAI-compatible endpoint',
      'No key? Requests, gossip and bubbles still work with built-in dialogue',
      'The AI only words the reply: it can’t hand out free emeralds', 'Players opt in before chat goes to an AI provider'],
    art: `${photo('thinking.jpg', [400, 140, 1120, 700], 900)}<div style="margin-top:26px">${providerChips()}</div>` }),

  'card-advancements': () => card({
    kicker: 'Advancements', title: 'Small talk<br>to hero',
    points: ['<b class="hi">Smart Villagers</b>: say something to a villager', '<b class="hi">A Favor for a Friend</b>: finish a request',
      '<b class="hi">Hero of the Village Square</b>: finish ten', 'Favours are counted per world, across every village'],
    art: photo('advancements.jpg', [570, 300, 780, 470], 900) }),

  'card-everywhere': () => card({
    kicker: 'Everywhere', title: 'Every loader,<br>five versions',
    points: ['Fabric, Forge and NeoForge', 'Bubbles and request cards are built from vanilla text displays and chat',
      'Optional spoken replies through Simple Voice Chat', '1.x worlds and configs carry over'],
    art: `<div style="display:grid;grid-template-columns:repeat(${VERSIONS.length},1fr);gap:14px">${VERSIONS.map((v) => `
      <div class="bubble" style="--tail:30px;padding:18px 10px 16px;text-align:center;margin-bottom:30px">
        <div class="display" style="font-size:40px">${v.minecraft}</div>
        <div style="margin-top:10px;display:flex;flex-direction:column;gap:8px;align-items:center">${v.loaders.map((l) =>
          `<span class="chip" style="font-size:15px;background:${l === 'Fabric' ? '#efe3c8' : l === 'Forge' ? '#e3e7ee' : '#f6dccd'}">${l}</span>`).join('')}</div>
      </div>`).join('')}</div>
      ${photo('complete.jpg', [0, 200, 1920, 830], 900, 'margin-top:10px')}` }),
};

const HEADERS = { 'whats-new': 'What’s new in 2.0', requests: 'Village requests', bubbles: 'Speech bubbles',
  reputation: 'Reputation and gossip', ai: 'AI providers', commands: 'Commands', config: 'Configuration',
  compatibility: 'Downloads and compatibility', faq: 'FAQ', gallery: 'Gallery' };
for (const [key, title] of Object.entries(HEADERS)) assets[`header-${key}`] = () => header(title);
assets.divider = divider;

const SIZES = { icon: [512, 512], banner: [1800, 600], divider: [1400, 44] };
const sizeOf = (name) => SIZES[name] || (name.startsWith('header-') ? [1400, 120] : [1600, 900]);

(async () => {
  const wanted = process.argv.slice(2);
  // The banner shows the icon, so the icon renders first.
  const names = Object.keys(assets).filter((n) => !wanted.length || wanted.includes(n))
    .sort((a, b) => (a === 'icon' ? -1 : b === 'icon' ? 1 : 0));
  const browser = await chromium.launch();
  for (const name of names) {
    const [width, height] = sizeOf(name);
    const pg = await browser.newPage({ viewport: { width, height } });
    const file = path.join(out, `.render-${name}.html`);
    fs.writeFileSync(file, assets[name]());
    await pg.goto(url(file));
    await pg.evaluate(() => document.fonts.ready);
    await pg.waitForTimeout(150);
    await pg.screenshot({ path: path.join(out, `${name}.png`), omitBackground: name === 'icon' || name === 'divider' || name.startsWith('header-') });
    fs.unlinkSync(file);
    await pg.close();
    console.log('rendered', name);
  }
  await browser.close();
})().catch((e) => { console.error(e); process.exit(1); });
