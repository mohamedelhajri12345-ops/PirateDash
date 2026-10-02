/* ============================================================
   سباق القراصنة — Pirate Dash v1.0-rc (Major Overhaul #2)
   Original halal pirate crew, no crosses/skulls, gems + meat + crew.
   ============================================================ */
"use strict";

/* ================= Storage ================= */
const SAVE_KEY = "pd_save_v7";
let save = {
  best: 0, bank: 0, gems: 0, char: 0, unlocked: [0],
  missions: { coins100: { p: 0, done: false }, run1000: { p: 0, done: false }, pow3: { p: 0, done: false } },
  plays: 0, sound: true
};
function loadSave() {
  try {
    const s = JSON.parse(localStorage.getItem(SAVE_KEY) || "null");
    if (s) {
      save = Object.assign(save, s);
      save.missions = Object.assign({ coins100: { p: 0, done: false }, run1000: { p: 0, done: false }, pow3: { p: 0, done: false } }, s.missions || {});
    }
  } catch (e) {}
}
function persist() { try { localStorage.setItem(SAVE_KEY, JSON.stringify(save)); } catch (e) {} }
loadSave();

/* ================= Audio ================= */
const AudioSys = (() => {
  let ac = null, master = null;
  function ensure() {
    if (ac) { if (ac.state === "suspended") ac.resume(); return true; }
    const AC = window.AudioContext || window.webkitAudioContext;
    if (!AC) return false;
    ac = new AC();
    master = ac.createGain();
    master.gain.value = save.sound ? 0.5 : 0;
    master.connect(ac.destination);
    startWaves();
    return true;
  }
  function startWaves() {
    const len = ac.sampleRate * 2;
    const buf = ac.createBuffer(1, len, ac.sampleRate);
    const d = buf.getChannelData(0);
    for (let i = 0; i < len; i++) d[i] = Math.random() * 2 - 1;
    const src = ac.createBufferSource(); src.buffer = buf; src.loop = true;
    const lp = ac.createBiquadFilter(); lp.type = "lowpass"; lp.frequency.value = 420;
    const wg = ac.createGain(); wg.gain.value = 0.06;
    const lfo = ac.createOscillator(); lfo.frequency.value = 0.14;
    const lg = ac.createGain(); lg.gain.value = 0.035;
    lfo.connect(lg); lg.connect(wg.gain);
    src.connect(lp); lp.connect(wg); wg.connect(master);
    src.start(); lfo.start();
  }
  function tone(freq, dur, type, vol, slideTo) {
    if (!ac) return;
    const o = ac.createOscillator(), g = ac.createGain();
    o.type = type || "sine"; o.frequency.setValueAtTime(freq, ac.currentTime);
    if (slideTo) o.frequency.exponentialRampToValueAtTime(slideTo, ac.currentTime + dur);
    g.gain.setValueAtTime(vol || 0.2, ac.currentTime);
    g.gain.exponentialRampToValueAtTime(0.001, ac.currentTime + dur);
    o.connect(g); g.connect(master); o.start(); o.stop(ac.currentTime + dur + 0.02);
  }
  function noise(dur, vol, freq) {
    if (!ac) return;
    const n = Math.floor(ac.sampleRate * dur);
    const buf = ac.createBuffer(1, n, ac.sampleRate);
    const d = buf.getChannelData(0);
    for (let i = 0; i < n; i++) d[i] = (Math.random() * 2 - 1) * (1 - i / n);
    const src = ac.createBufferSource(); src.buffer = buf;
    const f = ac.createBiquadFilter(); f.type = "lowpass"; f.frequency.value = freq || 800;
    const g = ac.createGain(); g.gain.value = vol || 0.3;
    src.connect(f); f.connect(g); g.connect(master); src.start();
  }
  return {
    unlock: ensure,
    coin() { tone(988, 0.09, "square", 0.12); setTimeout(() => tone(1319, 0.14, "square", 0.12), 60); },
    jump() { tone(300, 0.22, "sine", 0.2, 640); },
    slide() { noise(0.18, 0.15, 600); },
    crash() { noise(0.5, 0.5, 300); tone(90, 0.4, "sawtooth", 0.3, 45); },
    power() { [523, 659, 784, 1047].forEach((f, i) => setTimeout(() => tone(f, 0.16, "triangle", 0.16), i * 70)); },
    gem() { tone(1568, 0.08, "sine", 0.16); setTimeout(() => tone(2093, 0.16, "sine", 0.14), 70); },
    meat() { tone(440, 0.1, "triangle", 0.18); setTimeout(() => tone(554, 0.14, "triangle", 0.16), 90); },
    dawn() { [392, 523, 659, 784, 1047, 1319].forEach((f, i) => setTimeout(() => tone(f, 0.2, "square", 0.12), i * 80)); },
    milestone() { tone(784, 0.12, "triangle", 0.18); setTimeout(() => tone(1047, 0.2, "triangle", 0.18), 110); },
    click() { tone(600, 0.05, "square", 0.08); },
    setSound(on) { save.sound = on; persist(); if (master) master.gain.value = on ? 0.5 : 0; },
    get on() { return save.sound; }
  };
})();

/* ================= Canvas ================= */
const canvas = document.getElementById("game");
const ctx = canvas.getContext("2d");
let W = 0, H = 0, DPR = 1;
function resize() {
  DPR = Math.min(window.devicePixelRatio || 1, 2);
  W = window.innerWidth; H = window.innerHeight;
  canvas.width = W * DPR; canvas.height = H * DPR;
  canvas.style.width = W + "px"; canvas.style.height = H + "px";
  ctx.setTransform(DPR, 0, 0, DPR, 0, 0);
}
window.addEventListener("resize", resize);
resize();
if (!ctx.roundRect) ctx.roundRect = function (x, y, w, h, r) {
  this.moveTo(x + r, y); this.arcTo(x + w, y, x + w, y + h, r); this.arcTo(x + w, y + h, x, y + h, r);
  this.arcTo(x, y + h, x, y, r); this.arcTo(x, y, x + w, y, r); this.closePath();
};

/* ================= Projection ================= */
const LANES = [-1, 0, 1];
const LANE_W = 1.18, CAM_H = 3.1, CAM_BACK = 5, DRAW_DIST = 64;
let horizon = 0, FOCAL = 0, camBob = 0;
function proj(wx, wy, wz) {
  const zz = wz + CAM_BACK;
  const s = FOCAL / zz;
  return { x: W / 2 + wx * FOCAL * s, y: horizon + camBob + (CAM_H - wy) * FOCAL * s, s };
}

/* ================= Crew (original, halal, modest) ================= */
const CHARS = [
  { name: "القبطان سليم", gems: 0,   vest: "#a4161a", pants: "#2b2d42", skin: "#d8a46f",
    hat: "straw", hair: "#231a14", scarf: "#f4d58d" },
  { name: "الملاح ليان",  gems: 15,  vest: "#e76f51", pants: "#264653", skin: "#e0aa7e",
    hat: "hijab", hair: "", scarf: "#2a9d8f", coat: "#264653", pendant: "#e9c46a" },
  { name: "الطباخ عادل",  gems: 30,  vest: "#f1faee", pants: "#2b2d42", skin: "#c68b59",
    hat: "chef", hair: "#4a2c1a", scarf: "#a4161a" },
  { name: "المبارز حاتم", gems: 50,  vest: "#1b4332", pants: "#14342f", skin: "#d8a46f",
    hat: "turban", hair: "#111", scarf: "#e0b84c", coat: "#2d6a4f", weapon: "sword" },
  { name: "القناص سراج",  gems: 75,  vest: "#457b9d", pants: "#1d3557", skin: "#c68b59",
    hat: "cap", hair: "#231a14", scarf: "#e0b84c", weapon: "sling" },
  { name: "الطبيبة ياسمين", gems: 100, vest: "#fff1e6", pants: "#2b2d42", skin: "#e8bd9a",
    hat: "hijab", hair: "", scarf: "#a8dadc", coat: "#f1faee" }
];

/* ================= State ================= */
let state = "menu";
let score, coins, speed, distance, runTime, combo, comboT, milestones, gemsRun;
let obstacles, coinList, pickups, particles, floats, scenery, spawnZ;
let player, shake, slowmo, flash, grace;
let pow = { magnet: 0, dbl: 0, shield: false };
let meat, dawn, revived;
let nextPowZ = 40, nextMeatZ = 90, nextGemZ = 160;
const MS = save.missions;

function reset() {
  score = 0; coins = 0; distance = 0; speed = 15; runTime = 0;
  combo = 0; comboT = 0; milestones = 0; gemsRun = 0;
  obstacles = []; coinList = []; pickups = []; particles = []; floats = []; scenery = [];
  spawnZ = 26; nextPowZ = 55; nextMeatZ = 80; nextGemZ = 140;
  shake = 0; slowmo = 0; flash = 0; grace = 0;
  pow = { magnet: 0, dbl: 0, shield: false };
  meat = 0; dawn = 0; revived = false;
  player = { lane: 1, x: 0, jumpV: 0, jumpY: 0, sliding: false, slideT: 0, tilt: 0 };
  updateHUD();
}

/* ================= DOM ================= */
const $ = id => document.getElementById(id);
function updateHUD() {
  $("score").textContent = Math.floor(score || 0);
  $("coins").textContent = coins || 0;
  $("gems").textContent = (save.gems || 0) + (gemsRun || 0);
  $("meat").textContent = (meat || 0) + "/5";
  $("best").textContent = "🏆 " + save.best;
}
function renderMissions() {
  const defs = [
    ["coins100", "🪙 اجمع 100 عملة", 100],
    ["run1000", "🏃 اجرِ 1000 متر برحلة واحدة", 1000],
    ["pow3", "✨ التقط 3 قوى خاصة", 3]
  ];
  let html = "";
  for (const [k, label, goal] of defs) {
    const m = MS[k];
    html += '<div class="mission' + (m.done ? " done" : "") + '">' + label + " (" + Math.min(m.p, goal) + "/" + goal + ")" + "</div>";
  }
  $("missionList").innerHTML = html;
}
function missionCheck(k, inc) {
  const goals = { coins100: 100, run1000: 1000, pow3: 3 };
  const rewards = { coins100: 5, run1000: 10, pow3: 8 };
  const m = MS[k];
  if (m.done) return;
  if (k === "coins100") m.p = save.bank;
  else if (k === "run1000") m.p = Math.max(m.p, Math.floor(distance));
  else m.p += inc;
  if (m.p >= goals[k]) {
    m.done = true;
    save.gems += rewards[k];
    persist();
    floats.push({ text: "✅ مهمة! +" + rewards[k] + "💎", y: H * 0.3, life: 2.2, big: true });
  }
  renderMissions();
}

/* ================= Spawning ================= */
const R = (a, b) => a + Math.random() * (b - a);
const pick = arr => arr[(Math.random() * arr.length) | 0];

function coinRun(lane, z, n, air) {
  for (let i = 0; i < n; i++)
    coinList.push({ lane, laneF: LANES[lane], z: z + i * 1.5, y: air ? 1.7 : 0.85 });
}
function coinZig(z) {
  let l = (Math.random() * 3) | 0;
  for (let i = 0; i < 8; i++) {
    coinList.push({ lane: l % 3, laneF: LANES[l % 3], z: z + i * 1.4, y: 0.85 });
    l += (Math.random() < 0.5 ? 1 : 0);
  }
}
function spawnPattern() {
  const tiers = [
    () => { // single obstacle
      const l = (Math.random() * 3) | 0;
      obstacles.push({ lane: l, type: pick(["crate", "barrel"]), z: spawnZ });
      coinRun((l + 1 + ((Math.random() * 2) | 0)) % 3, spawnZ, 5);
    },
    () => { // slide gates
      for (let l = 0; l < 3; l++) obstacles.push({ lane: l, type: "gate", z: spawnZ });
      coinRun((Math.random() * 3) | 0, spawnZ, 6);
    },
    () => { // walls, one gap
      const gap = (Math.random() * 3) | 0;
      for (let l = 0; l < 3; l++) if (l !== gap) obstacles.push({ lane: l, type: "crate", z: spawnZ });
      coinRun(gap, spawnZ, 6);
    },
    () => { // jump barrels + air coins
      const l = (Math.random() * 3) | 0;
      obstacles.push({ lane: l, type: "barrel", z: spawnZ });
      coinRun(l, spawnZ - 1.2, 5, true);
      if (distance > 400) obstacles.push({ lane: (l + 2) % 3, type: "barrel", z: spawnZ + R(6, 10) });
    },
    () => { // rolling barrel
      const l = (Math.random() * 3) | 0;
      obstacles.push({ lane: l, type: "roll", z: spawnZ, vz: speed * 0.45 });
      coinRun((l + 1) % 3, spawnZ, 4);
    },
    () => coinZig(spawnZ),
    () => { // gates then crate
      for (let l = 0; l < 3; l++) obstacles.push({ lane: l, type: "gate", z: spawnZ });
      const l = (Math.random() * 3) | 0;
      obstacles.push({ lane: l, type: "crate", z: spawnZ + 7 });
      coinRun((l + 1) % 3, spawnZ + 5, 4);
    }
  ];
  let pool = [0, 1, 2, 3, 5];
  if (distance > 250) pool = [0, 1, 2, 3, 5, 6];
  if (distance > 600) pool = [0, 1, 2, 3, 4, 5, 6];
  tiers[pick(pool)]();

  if (Math.random() < 0.5)
    scenery.push({ side: Math.random() < 0.5 ? -1 : 1, type: pick(["sbarrel", "scrate"]), z: spawnZ + R(-4, 0) });

  const gap = Math.max(15 - distance * 0.008, 8.5) + R(0, 5);
  spawnZ += gap;

  if (spawnZ > nextPowZ) {
    pickups.push({ lane: (Math.random() * 3) | 0, z: spawnZ - gap / 2, type: pick(["magnet", "dbl", "shield"]) });
    nextPowZ = spawnZ + R(50, 90);
  }
  if (spawnZ > nextMeatZ) {
    pickups.push({ lane: (Math.random() * 3) | 0, z: spawnZ + 4, type: "meat" });
    nextMeatZ = spawnZ + R(130, 200);
  }
  if (spawnZ > nextGemZ) {
    pickups.push({ lane: (Math.random() * 3) | 0, z: spawnZ + 2, type: "gem" });
    nextGemZ = spawnZ + R(220, 320);
  }
}

/* ================= Input ================= */
function move(dir) {
  if (state !== "run") return;
  const cur = LANES.indexOf(player.lane);
  const nxt = Math.max(0, Math.min(2, cur + dir));
  if (nxt !== cur) { player.lane = LANES[nxt]; player.tilt = dir * 0.4; AudioSys.click(); }
}
function jump() {
  if (state !== "run") return;
  if (player.jumpY <= 0.01 && !player.sliding) { player.jumpV = 7.6; AudioSys.jump(); }
}
function slide() {
  if (state !== "run") return;
  if (player.jumpY > 0.5) { player.jumpV = -11; }
  else if (!player.sliding) { player.sliding = true; player.slideT = 0.55; AudioSys.slide(); }
}
document.addEventListener("keydown", e => {
  if (e.repeat) return;
  const k = e.key;
  if (k === "ArrowLeft" || k === "a") move(1);
  else if (k === "ArrowRight" || k === "d") move(-1);
  else if (k === "ArrowUp" || k === "w" || k === " ") { e.preventDefault(); jump(); }
  else if (k === "ArrowDown" || k === "s") slide();
  else if (k === "Enter") { if (state === "menu" || state === "over") startRun(); }
  else if (k === "p" || k === "Escape") togglePause();
});
let tsX = 0, tsY = 0;
canvas.addEventListener("touchstart", e => {
  AudioSys.unlock();
  const t = e.changedTouches[0]; tsX = t.clientX; tsY = t.clientY;
}, { passive: true });
canvas.addEventListener("touchend", e => {
  const t = e.changedTouches[0];
  const dx = t.clientX - tsX, dy = t.clientY - tsY;
  if (Math.abs(dx) < 22 && Math.abs(dy) < 22) { jump(); return; }
  if (Math.abs(dx) > Math.abs(dy)) move(dx < 0 ? 1 : -1);
  else if (dy < 0) jump(); else slide();
}, { passive: true });

/* ================= Flow ================= */
function startRun() {
  AudioSys.unlock();
  reset();
  state = "run";
  save.plays++;
  persist();
  ["menu", "over", "pause"].forEach(id => $(id).classList.add("hidden"));
  $("hud").classList.remove("hidden");
  $("pauseBtn").classList.remove("hidden");
  $("muteBtn").classList.remove("hidden");
  $("reviveBtn").classList.add("hidden");
  $("touchHint").classList.toggle("hidden2", save.plays > 2);
}
function toMenu() {
  state = "menu";
  ["over", "pause"].forEach(id => $(id).classList.add("hidden"));
  $("hud").classList.add("hidden");
  $("pauseBtn").classList.add("hidden");
  $("muteBtn").classList.add("hidden");
  $("menu").classList.remove("hidden");
  $("menuBank").textContent = save.bank;
  $("menuGems").textContent = save.gems;
  $("menuBest").textContent = save.best;
  renderMissions();
  renderChar();
}
function togglePause() {
  if (state === "run") { state = "paused"; $("pause").classList.remove("hidden"); }
  else if (state === "paused") { state = "run"; $("pause").classList.add("hidden"); }
}
function gameOver() {
  state = "over";
  shake = 1; flash = 0.8; slowmo = 0.7;
  AudioSys.crash();
  save.bank += coins;
  save.gems += gemsRun;
  missionCheck("coins100");
  missionCheck("run1000");
  const isRecord = Math.floor(score) > save.best;
  if (isRecord) save.best = Math.floor(score);
  persist();
  $("finalScore").textContent = "🏃 المسافة: " + Math.floor(score) + " م";
  $("finalCoins").textContent = "🪙 " + coins + " · 💎 " + (save.gems) + " · الرصيد: " + save.bank;
  $("overTitle").textContent = isRecord ? "🎉 رقم قياسي جديد!" : "⚓ سقط البحّار!";
  const canRevive = !revived && save.gems >= 5;
  $("reviveBtn").classList.toggle("hidden", !canRevive);
  if (canRevive) $("reviveBtn").textContent = "⚓ انعش الطاقم بـ 5 💎";
  setTimeout(() => {
    $("over").classList.remove("hidden");
    $("hud").classList.add("hidden");
    $("pauseBtn").classList.add("hidden");
  }, 700);
}
function revive() {
  if (save.gems < 5) return;
  save.gems -= 5;
  revived = true;
  persist();
  state = "run";
  grace = 2.2; // invincible seconds
  slowmo = 0; shake = 0;
  obstacles = obstacles.filter(o => o.z > 12);
  player.jumpY = 0; player.jumpV = 0; player.sliding = false;
  $("over").classList.add("hidden");
  $("hud").classList.remove("hidden");
  $("pauseBtn").classList.remove("hidden");
  AudioSys.power();
  floats.push({ text: "⚓ انطلق مجدداً!", y: H * 0.3, life: 1.5, big: true });
}
$("startBtn").addEventListener("click", startRun);
$("retryBtn").addEventListener("click", startRun);
$("menuBtn").addEventListener("click", toMenu);
$("quitBtn").addEventListener("click", toMenu);
$("resumeBtn").addEventListener("click", togglePause);
$("reviveBtn").addEventListener("click", revive);
$("pauseBtn").addEventListener("click", togglePause);
$("muteBtn").addEventListener("click", () => {
  AudioSys.unlock();
  AudioSys.setSound(!AudioSys.on);
  $("muteBtn").textContent = AudioSys.on ? "🔊" : "🔇";
});

/* ================= Character select ================= */
let charIdx = save.char;
const cpc = $("charPreview").getContext("2d");
function renderChar() {
  const c = CHARS[charIdx];
  $("charName").textContent = c.name;
  const owned = save.unlocked.includes(charIdx);
  const st = $("charStatus");
  if (owned) { st.textContent = "مفتوح"; st.classList.remove("canBuy"); }
  else { st.textContent = "السعر: " + c.gems + " 💎 (اضغط للفتح)"; st.classList.add("canBuy"); }
}
function drawCharPreview() {
  cpc.clearRect(0, 0, 120, 150);
  cpc.save();
  cpc.translate(60, 145);
  drawCrew(cpc, CHARS[charIdx], 1.35, 0, 0, { face: true });
  cpc.restore();
}
function tryBuy() {
  const c = CHARS[charIdx];
  if (save.unlocked.includes(charIdx)) { save.char = charIdx; persist(); return; }
  if (save.gems >= c.gems) {
    save.gems -= c.gems;
    save.unlocked.push(charIdx);
    save.char = charIdx;
    persist();
    AudioSys.power();
    floats.push({ text: "🎉 تم فتح " + c.name + "!", y: H * 0.25, life: 2, big: true });
  } else {
    AudioSys.click();
    $("charStatus").textContent = "تحتاج " + (c.gems - save.gems) + " 💎 إضافية";
  }
  renderChar();
}
$("charPrev").addEventListener("click", () => { charIdx = (charIdx + CHARS.length - 1) % CHARS.length; AudioSys.click(); renderChar(); });
$("charNext").addEventListener("click", () => { charIdx = (charIdx + 1) % CHARS.length; AudioSys.click(); renderChar(); });
$("charBox").addEventListener("click", tryBuy);

/* ================= Crew drawing ================= */
function drawCrew(g, c, sc, cx, groundY, opts) {
  opts = opts || {};
  const bob = opts.bob || 0, sliding = opts.sliding, running = opts.running, t = opts.t || 0;
  const bodyH = 60 * (sliding ? 0.5 : 1), bodyW = 30, headR = 12;
  g.save();
  g.translate(cx, groundY);
  g.scale(sc, sc);
  g.rotate(opts.tilt || 0);
  g.translate(0, bob);

  // sword on back (behind everything)
  if (c.weapon === "sword") {
    g.strokeStyle = "#c0c0c0"; g.lineWidth = 3.5;
    g.beginPath(); g.moveTo(-10, -14); g.lineTo(16, -88); g.stroke();
    g.strokeStyle = "#8b0000"; g.lineWidth = 4;
    g.beginPath(); g.moveTo(-6, -26); g.lineTo(2, -18); g.stroke();
  }

  // legs
  g.strokeStyle = c.pants; g.lineWidth = 6; g.lineCap = "round";
  if (sliding) {
    g.beginPath(); g.moveTo(0, -12); g.lineTo(22, -2); g.stroke();
    g.beginPath(); g.moveTo(0, -12); g.lineTo(16, 4); g.stroke();
  } else if (running) {
    const lt = t * 13;
    g.beginPath(); g.moveTo(-5, -18); g.lineTo(-5 + Math.sin(lt) * 7, 0); g.stroke();
    g.beginPath(); g.moveTo(5, -18); g.lineTo(5 - Math.sin(lt) * 7, 0); g.stroke();
  } else {
    g.beginPath(); g.moveTo(-5, -18); g.lineTo(-6, 0); g.stroke();
    g.beginPath(); g.moveTo(5, -18); g.lineTo(6, 0); g.stroke();
  }

  // coat (long, modest)
  if (c.coat) {
    g.fillStyle = c.coat;
    g.beginPath(); g.roundRect(-bodyW / 2 - 3, -bodyH - 8, bodyW + 6, bodyH - 2, 7); g.fill();
  }

  // vest
  const vestTop = -bodyH - 8;
  g.fillStyle = c.vest;
  g.beginPath(); g.roundRect(-bodyW / 2, vestTop, bodyW, bodyH, 8); g.fill();
  // scarf
  g.fillStyle = c.scarf;
  g.beginPath(); g.roundRect(-bodyW / 2 - 1, vestTop, bodyW + 2, 7, 3); g.fill();
  g.beginPath();
  g.moveTo(-bodyW / 2, vestTop + 2);
  g.quadraticCurveTo(-bodyW / 2 - 10 - Math.sin(t * 9) * 3, vestTop + 6, -bodyW / 2 - 14 - Math.sin(t * 7) * 4, vestTop + 3);
  g.lineTo(-bodyW / 2 - 13, vestTop + 7);
  g.closePath(); g.fill();
  // belt
  g.fillStyle = "#3d2314"; g.fillRect(-bodyW / 2, vestTop + bodyH - 9, bodyW, 6);
  g.fillStyle = "#e0b84c"; g.fillRect(-4, vestTop + bodyH - 9, 8, 6);
  // pendant (compass-style)
  if (c.pendant) {
    g.fillStyle = c.pendant;
    g.beginPath(); g.arc(0, vestTop + 16, 3.5, 0, 7); g.fill();
    g.strokeStyle = "#5b371b"; g.lineWidth = 1;
    g.beginPath(); g.arc(0, vestTop + 16, 3.5, 0, 7); g.stroke();
  }
  // arms
  g.strokeStyle = c.coat ? c.coat : c.skin; g.lineWidth = 5.5; g.lineCap = "round";
  if (running) {
    const at = t * 13;
    g.beginPath(); g.moveTo(-bodyW / 2 + 2, vestTop + 8); g.lineTo(-bodyW / 2 - 8 - Math.sin(at) * 5, vestTop + 20); g.stroke();
    g.beginPath(); g.moveTo(bodyW / 2 - 2, vestTop + 8); g.lineTo(bodyW / 2 + 8 + Math.sin(at) * 5, vestTop + 20); g.stroke();
  } else if (sliding) {
    g.beginPath(); g.moveTo(-bodyW / 2 + 2, vestTop + 8); g.lineTo(-bodyW / 2 - 6, vestTop + 2); g.stroke();
    g.beginPath(); g.moveTo(bodyW / 2 - 2, vestTop + 8); g.lineTo(bodyW / 2 + 12, vestTop + 4); g.stroke();
  } else {
    g.beginPath(); g.moveTo(-bodyW / 2 + 2, vestTop + 8); g.lineTo(-bodyW / 2 - 7, vestTop + 22); g.stroke();
    g.beginPath(); g.moveTo(bodyW / 2 - 2, vestTop + 8); g.lineTo(bodyW / 2 + 7, vestTop + 22); g.stroke();
  }
  // slingshot in hand
  if (c.weapon === "sling" && !sliding) {
    g.strokeStyle = "#5b371b"; g.lineWidth = 2.5;
    const hx = bodyW / 2 + 7, hy = vestTop + 22;
    g.beginPath(); g.moveTo(hx, hy); g.lineTo(hx + 3, hy - 9);
    g.moveTo(hx, hy); g.lineTo(hx + 8, hy - 7); g.stroke();
  }

  // head
  const headY = vestTop - headR + 2;
  // hijab behind head
  if (c.hat === "hijab") {
    g.fillStyle = c.scarf;
    g.beginPath(); g.arc(0, headY, headR * 1.25, 0, 7); g.fill();
    // drape down over shoulders
    g.beginPath(); g.roundRect(-headR * 1.15, headY - 2, headR * 2.3, 14, 5); g.fill();
  }
  g.fillStyle = c.skin;
  g.beginPath(); g.arc(0, headY, headR, 0, 7); g.fill();
  if (c.hat === "hijab") { // redo scarf over top, face open
    g.fillStyle = c.scarf;
    g.beginPath(); g.arc(0, headY, headR * 1.22, Math.PI * 1.05, Math.PI * 1.95); g.fill();
    g.beginPath(); g.arc(0, headY, headR * 1.22, Math.PI * 0.05, Math.PI * 0.95); g.fill();
  }
  if (opts.face) {
    g.fillStyle = "#fff";
    g.beginPath(); g.arc(-4, headY + 1, 2.4, 0, 7); g.arc(4, headY + 1, 2.4, 0, 7); g.fill();
    g.fillStyle = "#222";
    g.beginPath(); g.arc(-4, headY + 1.4, 1.1, 0, 7); g.arc(4, headY + 1.4, 1.1, 0, 7); g.fill();
    g.strokeStyle = "#a06a3f"; g.lineWidth = 1;
    g.beginPath(); g.arc(0, headY + 4, 3.4, 0.15, Math.PI - 0.15); g.stroke();
  }
  // hair
  if (c.hair && c.hat !== "hijab") {
    g.fillStyle = c.hair;
    g.beginPath(); g.arc(0, headY - 3, headR * 0.97, Math.PI * 0.95, Math.PI * 2.05); g.fill();
  }
  // hats
  const hatY = headY - headR * 0.72;
  if (c.hat === "straw") {
    g.fillStyle = "#e9c46a";
    g.beginPath(); g.ellipse(0, hatY, headR * 1.75, 5, 0, 0, 7); g.fill();
    g.fillStyle = "#f4d58d";
    g.beginPath(); g.ellipse(0, hatY - 1, headR * 0.9, headR * 0.62, 0, Math.PI, 0); g.fill();
    g.fillStyle = "#a4161a";
    g.fillRect(-headR * 0.9, hatY - 3, headR * 1.8, 3);
  } else if (c.hat === "bandana") {
    g.fillStyle = "#1b7f8c";
    g.beginPath(); g.arc(0, headY - 2, headR * 1.05, Math.PI, 0); g.fill();
    g.fillRect(-headR * 1.05, headY - 3, headR * 2.1, 4);
    g.beginPath(); g.moveTo(headR, headY - 1);
    g.quadraticCurveTo(headR + 12, headY + 2 + Math.sin(t * 8) * 3, headR + 16, headY - 4);
    g.lineTo(headR + 15, headY + 1); g.closePath(); g.fill();
  } else if (c.hat === "chef") {
    g.fillStyle = "#fff";
    g.fillRect(-7, hatY - 6, 14, 6);
    g.beginPath(); g.ellipse(0, hatY - 8, 10, 8, 0, Math.PI, 0); g.fill();
    g.strokeStyle = "#cdd7e0"; g.strokeRect(-7, hatY - 6, 14, 6);
  } else if (c.hat === "turban") {
    g.fillStyle = "#f1faee";
    g.beginPath(); g.arc(0, headY - 2, headR * 1.12, Math.PI, 0); g.fill();
    g.strokeStyle = "#cdd7e0"; g.lineWidth = 1.5;
    g.beginPath(); g.arc(0, headY - 4, headR * 0.8, Math.PI * 0.9, Math.PI * 2.1); g.stroke();
    g.fillStyle = "#e0b84c";
    g.beginPath(); g.arc(0, headY - headR * 0.9, 2.2, 0, 7); g.fill();
  } else if (c.hat === "cap") {
    g.fillStyle = "#1d3557";
    g.beginPath(); g.arc(0, headY - 1, headR * 1.05, Math.PI, 0); g.fill();
    g.fillRect(-headR * 1.2, headY - 2, headR * 2.4, 4);
    g.fillStyle = "#e0b84c"; g.fillRect(-2, hatY - 8, 4, 4);
  } else if (c.hat === "hijab") {
    // already drawn; small gold pin
    g.fillStyle = "#e0b84c";
    g.beginPath(); g.arc(headR * 0.8, headY - 4, 1.8, 0, 7); g.fill();
  }
  g.restore();
}

/* ================= World ================= */
const PAL = [
  ["#1d3557", "#e8a05c", "#14405c", "#0d1b2a", "#ffe08a"],
  ["#0b1026", "#1b2a4a", "#0a1f33", "#060d18", "#dfe8ff"],
  ["#274472", "#f2b26b", "#17506e", "#0d1b2a", "#ffd97a"],
  ["#4a9fd8", "#bfe3f2", "#1a6b8a", "#0d2a3f", "#fff3b0"]
];
function lerp(a, b, t) { return a + (b - a) * t; }
function lerpC(c1, c2, t) {
  const p = h => [parseInt(h.slice(1, 3), 16), parseInt(h.slice(3, 5), 16), parseInt(h.slice(5, 7), 16)];
  const A = p(c1), B = p(c2);
  return "rgb(" + Math.round(lerp(A[0], B[0], t)) + "," + Math.round(lerp(A[1], B[1], t)) + "," + Math.round(lerp(A[2], B[2], t)) + ")";
}
function pal() {
  const ph = ((distance || 0) / 900) % 4;
  const i = Math.floor(ph), t = ph - i;
  const a = PAL[i], b = PAL[(i + 1) % 4];
  return [lerpC(a[0], b[0], t), lerpC(a[1], b[1], t), lerpC(a[2], b[2], t), lerpC(a[3], b[3], t), lerpC(a[4], b[4], t), ph];
}
const stars = Array.from({ length: 70 }, () => ({ x: Math.random(), y: Math.random() * 0.3, r: Math.random() * 1.4 + 0.4, tw: Math.random() * 6 }));
const clouds = Array.from({ length: 6 }, () => ({ x: Math.random(), y: 0.05 + Math.random() * 0.2, s: 0.6 + Math.random() * 0.9, v: 0.004 + Math.random() * 0.008 }));
let birds = [], birdT = 4;

function drawSky(P, now) {
  const hz = horizon + camBob;
  const g = ctx.createLinearGradient(0, 0, 0, hz);
  g.addColorStop(0, P[0]); g.addColorStop(1, P[1]);
  ctx.fillStyle = g; ctx.fillRect(0, 0, W, hz + 2);
  const night = Math.max(0, 1 - Math.abs((((P[5] + 2) % 4) - 3)));
  if (night > 0.05) {
    for (const s of stars) {
      const a = night * (0.5 + 0.5 * Math.sin(now * 0.002 + s.tw));
      if (a <= 0) continue;
      ctx.globalAlpha = a;
      ctx.fillStyle = "#fff";
      ctx.beginPath(); ctx.arc(s.x * W, s.y * hz, s.r, 0, 7); ctx.fill();
    }
    ctx.globalAlpha = 1;
  }
  const ph = P[5];
  const isNight = ph >= 0.75 && ph < 1.75;
  const bodyT = isNight ? (ph - 0.75) : (ph < 0.75 ? ph / 0.75 : (ph - 1.75) / 2.25);
  const bx = bodyT * W * 0.8 + W * 0.1;
  const by = hz * (0.65 - Math.sin(bodyT * Math.PI) * 0.45);
  ctx.fillStyle = isNight ? "#e8eef7" : P[4];
  ctx.beginPath(); ctx.arc(bx, by, isNight ? 22 : 30, 0, 7); ctx.fill();
  if (isNight) {
    ctx.fillStyle = P[0];
    ctx.beginPath(); ctx.arc(bx - 9, by - 4, 19, 0, 7); ctx.fill();
  }
  for (const c of clouds) {
    c.x = (c.x + c.v * 0.016) % 1;
    const cx = c.x * (W + 300) - 150, cy = c.y * hz;
    ctx.fillStyle = "rgba(255,255,255," + (isNight ? 0.1 : 0.22) + ")";
    ctx.beginPath();
    ctx.ellipse(cx, cy, 55 * c.s, 16 * c.s, 0, 0, 7);
    ctx.ellipse(cx + 30 * c.s, cy + 4, 35 * c.s, 12 * c.s, 0, 0, 7);
    ctx.ellipse(cx - 32 * c.s, cy + 5, 30 * c.s, 11 * c.s, 0, 0, 7);
    ctx.fill();
  }
  birdT -= 0.016;
  if (birdT <= 0) { birds.push({ x: -40, y: R(0.1, 0.28) * hz, v: R(30, 55) }); birdT = R(3, 9); }
  birds = birds.filter(b => b.x < W + 60);
  ctx.strokeStyle = "rgba(20,30,45,.75)"; ctx.lineWidth = 2;
  for (const b of birds) {
    b.x += b.v * 0.016;
    const fl = Math.sin(now * 0.012 + b.x) * 5;
    ctx.beginPath();
    ctx.moveTo(b.x - 8, b.y + fl); ctx.lineTo(b.x, b.y - 3); ctx.lineTo(b.x + 8, b.y + fl);
    ctx.stroke();
  }
  const iso = (distance || 0) * 0.06;
  ctx.fillStyle = "rgba(10,22,35,.85)";
  for (let i = 0; i < 3; i++) {
    const bx2 = ((i * 500 + 200 - iso) % (W + 600)) - 300;
    const bw = 140 + i * 60, bh = 26 + i * 8;
    ctx.beginPath();
    ctx.moveTo(bx2 - bw / 2, hz);
    ctx.quadraticCurveTo(bx2 - bw / 4, hz - bh, bx2, hz - bh);
    ctx.quadraticCurveTo(bx2 + bw / 4, hz - bh + 4, bx2 + bw / 2, hz);
    ctx.closePath(); ctx.fill();
    if (i === 1) {
      ctx.strokeStyle = "rgba(10,22,35,.9)"; ctx.lineWidth = 3;
      const px2 = bx2 + 20, py = hz - bh;
      ctx.beginPath(); ctx.moveTo(px2, py); ctx.quadraticCurveTo(px2 + 4, py - 14, px2 + 9, py - 20); ctx.stroke();
      ctx.beginPath(); ctx.moveTo(px2 + 9, py - 20); ctx.lineTo(px2 + 1, py - 24);
      ctx.moveTo(px2 + 9, py - 20); ctx.lineTo(px2 + 17, py - 25);
      ctx.moveTo(px2 + 9, py - 20); ctx.lineTo(px2 + 12, py - 27); ctx.stroke();
    }
  }
}
function drawSea(P, now) {
  const hz = horizon + camBob;
  const g = ctx.createLinearGradient(0, hz, 0, H);
  g.addColorStop(0, P[2]); g.addColorStop(1, P[3]);
  ctx.fillStyle = g; ctx.fillRect(0, hz, W, H - hz);
  for (let i = 0; i < 8; i++) {
    const t = ((now * 0.00004 * (i + 1)) + i * 0.13) % 1;
    const y = hz + t * (H - hz) * 0.35;
    ctx.fillStyle = "rgba(255,255,255," + (0.05 + 0.05 * (1 - t)) + ")";
    ctx.fillRect(0, y, W, 2);
  }
}
function drawDeck() {
  const hz = horizon + camBob;
  const xl = -LANE_W * 1.5 - 0.55, xr = LANE_W * 1.5 + 0.55;
  const a0 = proj(xl, 0, 0), b0 = proj(xr, 0, 0);
  const aF = proj(xl, 0, DRAW_DIST), bF = proj(xr, 0, DRAW_DIST);
  const g = ctx.createLinearGradient(0, hz, 0, H);
  g.addColorStop(0, "#6b4b35"); g.addColorStop(1, "#96613f");
  ctx.fillStyle = g;
  ctx.beginPath();
  ctx.moveTo(aF.x, aF.y); ctx.lineTo(bF.x, bF.y); ctx.lineTo(b0.x, b0.y + 2); ctx.lineTo(a0.x, a0.y + 2);
  ctx.closePath(); ctx.fill();
  const off = (distance * 2) % 4;
  for (let z = 0; z < DRAW_DIST; z += 4) {
    const zz = z + (4 - off);
    const c1 = proj(xl, 0, zz), c2 = proj(xr, 0, zz), c3 = proj(xl, 0, zz + 0.4), c4 = proj(xr, 0, zz + 0.4);
    ctx.fillStyle = "rgba(0,0,0,.14)";
    ctx.beginPath(); ctx.moveTo(c1.x, c1.y); ctx.lineTo(c2.x, c2.y); ctx.lineTo(c4.x, c4.y); ctx.lineTo(c3.x, c3.y);
    ctx.closePath(); ctx.fill();
  }
  ctx.strokeStyle = "rgba(224,184,76,.45)"; ctx.lineWidth = 2;
  for (const lx of [-LANE_W, 0, LANE_W]) {
    const p1 = proj(lx, 0, 0.01), p2 = proj(lx, 0, DRAW_DIST);
    ctx.beginPath(); ctx.moveTo(p1.x, p1.y); ctx.lineTo(p2.x, p2.y); ctx.stroke();
  }
  for (const side of [xl, xr]) {
    for (let z = 0; z < DRAW_DIST; z += 3) {
      const zz = z + (3 - (distance * 2) % 3);
      const p = proj(side, 0, zz), pt = proj(side, 1.05, zz);
      ctx.strokeStyle = "#4a2c17"; ctx.lineWidth = 3;
      ctx.beginPath(); ctx.moveTo(p.x, p.y); ctx.lineTo(pt.x, pt.y); ctx.stroke();
    }
    for (const hy of [0.95, 0.55]) {
      const r1 = proj(side, hy, 0.01), r2 = proj(side, hy, DRAW_DIST);
      ctx.strokeStyle = "#8a5a3b"; ctx.lineWidth = 2;
      ctx.beginPath(); ctx.moveTo(r1.x, r1.y); ctx.lineTo(r2.x, r2.y); ctx.stroke();
    }
  }
}
function drawScenery(s) {
  const wx = s.side * (LANE_W * 1.5 + 0.2);
  const p = proj(wx, 0, s.z);
  const sc = p.s * 2;
  if (s.type === "sbarrel") {
    ctx.fillStyle = "#8a5a3b";
    ctx.beginPath(); ctx.ellipse(p.x, p.y - 12 * sc, 12 * sc, 14 * sc, 0, 0, 7); ctx.fill();
    ctx.strokeStyle = "#3d2314"; ctx.lineWidth = 1.5;
    ctx.beginPath(); ctx.ellipse(p.x, p.y - 12 * sc, 12 * sc, 14 * sc, 0, 0, 7); ctx.stroke();
  } else {
    ctx.fillStyle = "#7f4f24";
    ctx.fillRect(p.x - 12 * sc, p.y - 22 * sc, 24 * sc, 22 * sc);
    ctx.strokeStyle = "#5b371b"; ctx.strokeRect(p.x - 12 * sc, p.y - 22 * sc, 24 * sc, 22 * sc);
  }
}
function drawObstacle(o, now) {
  const wx = LANES[o.lane] * LANE_W;
  const base = proj(wx, 0, o.z);
  const s = base.s * 2.1;
  if (base.y > H + 60) return;
  if (o.type === "crate") {
    const w = 68 * s, h = 135 * s;
    ctx.fillStyle = "#7f4f24";
    ctx.fillRect(base.x - w / 2, base.y - h, w, h);
    ctx.strokeStyle = "#5b371b"; ctx.lineWidth = 2;
    ctx.strokeRect(base.x - w / 2, base.y - h, w, h);
    ctx.beginPath();
    ctx.moveTo(base.x - w / 2, base.y - h); ctx.lineTo(base.x + w / 2, base.y);
    ctx.moveTo(base.x + w / 2, base.y - h); ctx.lineTo(base.x - w / 2, base.y);
    ctx.stroke();
    ctx.fillStyle = "rgba(224,184,76,.7)";
    ctx.fillRect(base.x - w / 2, base.y - h * 0.53, w, 4 * s);
  } else if (o.type === "barrel") {
    const rw = 52 * s, rh = 58 * s;
    ctx.fillStyle = "#a47148";
    ctx.beginPath(); ctx.ellipse(base.x, base.y - rh / 2, rw / 2, rh / 2, 0, 0, 7); ctx.fill();
    ctx.strokeStyle = "#3d2314"; ctx.lineWidth = 2.5;
    ctx.beginPath(); ctx.ellipse(base.x, base.y - rh / 2, rw / 2 - 1, rh / 2 - 1, 0, 0, 7); ctx.stroke();
    ctx.beginPath(); ctx.moveTo(base.x - rw / 2 + 2, base.y - rh / 2 - 7 * s); ctx.lineTo(base.x + rw / 2 - 2, base.y - rh / 2 - 7 * s); ctx.stroke();
    ctx.beginPath(); ctx.moveTo(base.x - rw / 2 + 2, base.y - rh / 2 + 7 * s); ctx.lineTo(base.x + rw / 2 - 2, base.y - rh / 2 + 7 * s); ctx.stroke();
  } else if (o.type === "roll") {
    const rot = now * 0.012 * (o.vz || 10);
    const rw = 54 * s, rh = 60 * s;
    ctx.save();
    ctx.translate(base.x, base.y - rh / 2);
    ctx.rotate(rot);
    ctx.fillStyle = "#b5762f";
    ctx.beginPath(); ctx.ellipse(0, 0, rw / 2, rh / 2, 0, 0, 7); ctx.fill();
    ctx.strokeStyle = "#6b4b1f"; ctx.lineWidth = 3;
    ctx.beginPath(); ctx.ellipse(0, 0, rw / 2 - 2, rh / 2 - 2, 0, 0, 7); ctx.stroke();
    ctx.beginPath(); ctx.moveTo(-rw / 2 + 3, 0); ctx.lineTo(rw / 2 - 3, 0); ctx.stroke();
    ctx.beginPath(); ctx.moveTo(0, -rh / 2 + 3); ctx.lineTo(0, rh / 2 - 3); ctx.stroke();
    ctx.restore();
  } else if (o.type === "gate") {
    const w = 92 * s, h = 100 * s;
    const top = proj(wx, 2.45, o.z);
    ctx.fillStyle = "#8b1c1c";
    ctx.fillRect(base.x - w / 2, top.y, w, h);
    ctx.strokeStyle = "#e0b84c"; ctx.lineWidth = 2;
    ctx.strokeRect(base.x - w / 2, top.y, w, h);
    ctx.fillStyle = "#e0b84c";
    ctx.save(); ctx.translate(base.x, top.y + h * 0.33);
    ctx.beginPath();
    for (let i = 0; i < 10; i++) {
      const r = i % 2 ? 6 * s : 14 * s, a = i * Math.PI / 5 - Math.PI / 2;
      ctx.lineTo(Math.cos(a) * r, Math.sin(a) * r);
    }
    ctx.closePath(); ctx.fill(); ctx.restore();
    ctx.fillStyle = "#5b371b";
    ctx.fillRect(base.x - w / 2 - 5 * s, top.y - 18 * s, 5 * s, h + 36 * s);
    ctx.fillRect(base.x + w / 2, top.y - 18 * s, 5 * s, h + 36 * s);
  }
}
function drawCoin(c, now) {
  const p = proj(c.laneF * LANE_W, c.y, c.z);
  const r = 15 * p.s * 2;
  const wob = Math.abs(Math.cos(now * 0.005 + c.z));
  ctx.fillStyle = "#e0b84c";
  ctx.beginPath(); ctx.ellipse(p.x, p.y, r * wob + 1, r, 0, 0, 7); ctx.fill();
  ctx.strokeStyle = "#9c7422"; ctx.lineWidth = 1.5;
  ctx.beginPath(); ctx.ellipse(p.x, p.y, r * wob + 1, r, 0, 0, 7); ctx.stroke();
  if (wob > 0.55) {
    ctx.fillStyle = "#f6d788";
    ctx.beginPath(); ctx.ellipse(p.x, p.y, r * 0.5 * wob, r * 0.55, 0, 0, 7); ctx.fill();
  }
}
function drawPickup(u, now) {
  const p = proj(LANES[u.lane] * LANE_W, 1.1, u.z);
  const r = 17 * p.s * 2;
  const bob2 = Math.sin(now * 0.004 + u.z) * 5 * p.s * 2;
  ctx.save();
  ctx.translate(p.x, p.y - bob2);
  const gl = ctx.createRadialGradient(0, 0, 2, 0, 0, r * 1.8);
  gl.addColorStop(0, "rgba(246,215,136,.45)"); gl.addColorStop(1, "rgba(246,215,136,0)");
  ctx.fillStyle = gl;
  ctx.beginPath(); ctx.arc(0, 0, r * 1.8, 0, 7); ctx.fill();
  if (u.type === "magnet") {
    ctx.strokeStyle = "#c1121f"; ctx.lineWidth = 4;
    ctx.beginPath(); ctx.arc(0, 0, r * 0.62, Math.PI * 0.05, Math.PI * 0.95, true); ctx.stroke();
    ctx.fillStyle = "#c1121f";
    ctx.fillRect(-r * 0.62 - 2, -3, 4, 8); ctx.fillRect(r * 0.62 - 2, -3, 4, 8);
  } else if (u.type === "shield") {
    ctx.fillStyle = "#3a86ff";
    ctx.beginPath();
    ctx.moveTo(0, -r * 0.8); ctx.lineTo(r * 0.65, -r * 0.4);
    ctx.lineTo(r * 0.55, r * 0.5); ctx.lineTo(0, r * 0.85);
    ctx.lineTo(-r * 0.55, r * 0.5); ctx.lineTo(-r * 0.65, -r * 0.4);
    ctx.closePath(); ctx.fill();
    ctx.strokeStyle = "#fff"; ctx.lineWidth = 2; ctx.stroke();
  } else if (u.type === "dbl") {
    ctx.fillStyle = "#e0b84c";
    ctx.beginPath(); ctx.arc(0, 0, r * 0.62, 0, 7); ctx.fill();
    ctx.fillStyle = "#5b371b";
    ctx.font = "bold " + (r * 0.7) + "px Arial";
    ctx.textAlign = "center"; ctx.textBaseline = "middle";
    ctx.fillText("×2", 0, 1);
  } else if (u.type === "gem") {
    ctx.fillStyle = "#48cae4";
    ctx.beginPath();
    ctx.moveTo(0, -r * 0.75); ctx.lineTo(r * 0.6, -r * 0.1);
    ctx.lineTo(0, r * 0.8); ctx.lineTo(-r * 0.6, -r * 0.1);
    ctx.closePath(); ctx.fill();
    ctx.strokeStyle = "#90e0ef"; ctx.lineWidth = 2; ctx.stroke();
    ctx.fillStyle = "rgba(255,255,255,.5)";
    ctx.beginPath(); ctx.moveTo(0, -r * 0.75); ctx.lineTo(r * 0.25, -r * 0.1); ctx.lineTo(0, r * 0.2); ctx.closePath(); ctx.fill();
  } else if (u.type === "meat") {
    // halal meal: juicy drumstick shape
    ctx.fillStyle = "#b45309";
    ctx.beginPath(); ctx.ellipse(-r * 0.1, 0, r * 0.55, r * 0.42, -0.5, 0, 7); ctx.fill();
    ctx.strokeStyle = "#78350f"; ctx.lineWidth = 1.5; ctx.stroke();
    ctx.fillStyle = "#f59e0b";
    ctx.beginPath(); ctx.ellipse(-r * 0.2, -r * 0.08, r * 0.3, r * 0.2, -0.5, 0, 7); ctx.fill();
    ctx.strokeStyle = "#e7dfd5"; ctx.lineWidth = 4; ctx.lineCap = "round";
    ctx.beginPath(); ctx.moveTo(r * 0.35, -r * 0.3); ctx.lineTo(r * 0.68, -r * 0.62); ctx.stroke();
    ctx.fillStyle = "#e7dfd5";
    ctx.beginPath(); ctx.arc(r * 0.74, -r * 0.68, r * 0.16, 0, 7); ctx.fill();
    ctx.beginPath(); ctx.arc(r * 0.64, -r * 0.76, r * 0.14, 0, 7); ctx.fill();
  }
  ctx.restore();
}

/* ================= Player ================= */
function drawPlayer(now) {
  const ch = CHARS[save.char] || CHARS[0];
  const p = proj(player.x, 0, 0);
  const jumpPix = player.jumpY * (proj(player.x, 0, 0).y - proj(player.x, 1, 0).y);
  const gy = p.y - jumpPix;
  const sc = p.s * 2.4;
  const shS = 1 - player.jumpY * 0.18;
  ctx.fillStyle = "rgba(0,0,0,.32)";
  ctx.beginPath(); ctx.ellipse(p.x, p.y + 3, 30 * sc * shS, 8 * sc * shS, 0, 0, 7); ctx.fill();
  if (dawn > 0) { // dawn mode golden aura
    const a = 0.35 + 0.2 * Math.sin(now * 0.01);
    ctx.strokeStyle = "rgba(246,215,136," + a + ")"; ctx.lineWidth = 5;
    ctx.beginPath(); ctx.ellipse(p.x, gy - 55 * sc * 0.55, 46 * sc * 0.85, 82 * sc * 0.5, 0, 0, 7); ctx.stroke();
  } else if (pow.shield) {
    ctx.strokeStyle = "rgba(58,134,255,.65)"; ctx.lineWidth = 3;
    ctx.beginPath(); ctx.ellipse(p.x, gy - 55 * sc * 0.55, 42 * sc * 0.8, 78 * sc * 0.5, 0, 0, 7); ctx.stroke();
  }
  drawCrew(ctx, ch, sc, p.x, gy, {
    running: state === "run", t: now * 0.001, bob: player.sliding ? 0 : Math.sin(now * 0.014) * 2,
    sliding: player.sliding, tilt: player.tilt * 0.5
  });
  if (speed > 30 && state === "run") {
    ctx.strokeStyle = dawn > 0 ? "rgba(246,215,136,.5)" : "rgba(246,215,136,.25)";
    ctx.lineWidth = 3;
    for (let i = 1; i <= 3; i++) {
      ctx.beginPath(); ctx.moveTo(p.x - 26 * sc - i * 6, gy - 10);
      ctx.lineTo(p.x - 26 * sc - i * 14, gy - 10 + i * 8); ctx.stroke();
      ctx.beginPath(); ctx.moveTo(p.x + 26 * sc + i * 6, gy - 10);
      ctx.lineTo(p.x + 26 * sc + i * 14, gy - 10 + i * 8); ctx.stroke();
    }
  }
}

/* ================= HUD extras ================= */
function updatePowHUD() {
  let html = "";
  if (dawn > 0) html += '<div class="pow">⚓ وضع الفجر ' + Math.ceil(dawn) + '<div class="bar" style="width:' + (dawn / 6 * 100) + '%"></div></div>';
  if (pow.magnet > 0) html += '<div class="pow">🧲 ' + Math.ceil(pow.magnet) + '<div class="bar" style="width:' + (pow.magnet / 8 * 100) + '%"></div></div>';
  if (pow.dbl > 0) html += '<div class="pow">×2 ' + Math.ceil(pow.dbl) + '<div class="bar" style="width:' + (pow.dbl / 10 * 100) + '%"></div></div>';
  if (pow.shield) html += '<div class="pow">🛡️</div>';
  $("pows").innerHTML = html;
}

/* ================= Main loop ================= */
let lastT = performance.now();
function loop(now) {
  const rawDt = Math.min((now - lastT) / 1000, 0.05);
  lastT = now;
  const ts = slowmo > 0 ? 0.25 : 1;
  if (slowmo > 0) slowmo -= rawDt;
  const dt = rawDt * ts;

  horizon = H * 0.36;
  FOCAL = H * 0.95;
  camBob = state === "run" ? Math.sin(now * 0.006) * 4 : 0;

  if (state === "run") {
    runTime += dt;
    speed = Math.min(15 + runTime * 0.5 + distance * 0.008, 44);
    distance += speed * dt;
    score = distance + coins * 10;
    updateHUD();

    if (Math.floor(distance / 500) > milestones) {
      milestones = Math.floor(distance / 500);
      AudioSys.milestone();
      floats.push({ text: milestones * 500 + " م! 🔥", y: H * 0.3, life: 1.6, big: true });
    }
    if (Math.floor(distance / 1000) > (loop.lastKm || 0)) {
      loop.lastKm = Math.floor(distance / 1000);
      gemsRun++;
      AudioSys.gem();
      floats.push({ text: "💎 +1 جوهرة!", y: H * 0.34, life: 1.5, big: true });
    }

    if (pow.magnet > 0) pow.magnet = Math.max(0, pow.magnet - dt);
    if (pow.dbl > 0) pow.dbl = Math.max(0, pow.dbl - dt);
    if (grace > 0) grace -= dt;
    if (dawn > 0) {
      dawn = Math.max(0, dawn - dt);
      pow.magnet = Math.max(pow.magnet, 0.01);
      if (dawn === 0) pow.dbl = Math.max(pow.dbl, 0.01);
    }
    if (comboT > 0) { comboT -= dt; if (comboT <= 0) combo = 0; }
    updatePowHUD();

    const targetX = LANES[player.lane] * LANE_W;
    player.x += (targetX - player.x) * Math.min(dt * 11, 1);
    player.tilt *= Math.max(1 - dt * 6, 0);
    if (player.jumpY > 0 || player.jumpV > 0) {
      player.jumpY += player.jumpV * dt;
      player.jumpV -= 21 * dt;
      if (player.jumpY <= 0) {
        player.jumpY = 0; player.jumpV = 0;
        for (let i = 0; i < 4; i++) particles.push({ x: player.x, z: 0, y: 0, vx: R(-1.5, 1.5), vy: R(0.5, 2), life: 0.35, col: "#c9a227" });
      }
    }
    if (player.sliding) { player.slideT -= dt; if (player.slideT <= 0) player.sliding = false; }

    if (distance + DRAW_DIST > spawnZ) spawnPattern();

    for (const o of obstacles) { o.z -= speed * dt; if (o.type === "roll") o.z -= (o.vz || 8) * dt; }
    for (const c of coinList) c.z -= speed * dt;
    for (const u of pickups) u.z -= speed * dt;
    for (const s of scenery) s.z -= speed * dt;
    scenery = scenery.filter(s => s.z > -2);

    if (pow.magnet > 0) {
      for (const c of coinList) {
        if (c.z < 14 && c.z > -1) {
          const target = LANES[player.lane];
          c.laneF += (target - c.laneF) * Math.min(dt * 4, 1);
          c.y += ((player.jumpY > 0.5 ? player.jumpY + 0.6 : 0.85) - c.y) * Math.min(dt * 3, 1);
        }
      }
    }

    // collisions
    for (let i = obstacles.length - 1; i >= 0; i--) {
      const o = obstacles[i];
      if (o.z > -0.9 && o.z < 0.9) {
        const dx = Math.abs(LANES[o.lane] * LANE_W - player.x);
        if (dx < LANE_W * 0.55) {
          const jumpOK = (o.type === "barrel" || o.type === "roll") && player.jumpY > 0.8;
          const slideOK = o.type === "gate" && player.sliding;
          if (!jumpOK && !slideOK) {
            if (dawn > 0) { // smash through!
              obstacles.splice(i, 1);
              coins += 5;
              shake = Math.max(shake, 0.35);
              AudioSys.meat();
              floats.push({ text: "💥 +5", y: H * 0.4, life: 0.8 });
              for (let j = 0; j < 8; j++) particles.push({ x: LANES[o.lane], z: o.z, y: 0.8, vx: R(-3, 3), vy: R(1, 4), life: 0.5, col: "#b5762f" });
            } else if (pow.shield) {
              pow.shield = false;
              o.z = -5;
              shake = 0.5; flash = 0.35;
              AudioSys.power();
              floats.push({ text: "🛡️ حماية!", y: H * 0.32, life: 1.2, big: true });
            } else if (grace > 0) {
              // pass through
            } else { gameOver(); break; }
          }
        }
      }
    }
    // coin pickup
    for (const c of coinList) {
      if (c.z > -0.6 && c.z < 1 && !c.got) {
        const dx = Math.abs(c.laneF * LANE_W - player.x);
        const dy = Math.abs(c.y - (player.jumpY > 0.3 ? player.jumpY : 0.85));
        if (dx < LANE_W * 0.6 && dy < 1.2) {
          c.got = true;
          coins += pow.dbl > 0 || dawn > 0 ? 2 : 1;
          combo++; comboT = 2.2;
          AudioSys.coin();
          for (let j = 0; j < 5; j++) particles.push({ x: c.laneF, z: c.z, y: c.y, vx: R(-2, 2), vy: R(1, 3.5), life: 0.4, col: "#f6d788" });
          if (combo > 0 && combo % 15 === 0)
            floats.push({ text: "🔥 سلسلة ×" + combo + "!", y: H * 0.28, life: 1.2, big: true });
        }
      }
    }
    coinList = coinList.filter(c => !c.got && c.z > -3);
    obstacles = obstacles.filter(o => o.z > -3);
    // pickups
    for (let i = pickups.length - 1; i >= 0; i--) {
      const u = pickups[i];
      if (u.z > -0.7 && u.z < 1) {
        const dx = Math.abs(LANES[u.lane] * LANE_W - player.x);
        if (dx < LANE_W * 0.7) {
          pickups.splice(i, 1);
          if (u.type === "magnet") { pow.magnet = 8; AudioSys.power(); floats.push({ text: "🧲 مغناطيس!", y: H * 0.3, life: 1.4, big: true }); missionCheck("pow3", 1); }
          else if (u.type === "dbl") { pow.dbl = 10; AudioSys.power(); floats.push({ text: "✨ ذهب مضاعف!", y: H * 0.3, life: 1.4, big: true }); missionCheck("pow3", 1); }
          else if (u.type === "shield") { pow.shield = true; AudioSys.power(); floats.push({ text: "🛡️ درع!", y: H * 0.3, life: 1.4, big: true }); missionCheck("pow3", 1); }
          else if (u.type === "gem") { gemsRun++; AudioSys.gem(); floats.push({ text: "💎 جوهرة!", y: H * 0.3, life: 1.2, big: true }); updateHUD(); }
          else if (u.type === "meat") {
            AudioSys.meat();
            meat++;
            if (meat >= 5) {
              meat = 0; dawn = 6;
              AudioSys.dawn();
              floats.push({ text: "⚓ وضع الفجر! لا يوقفك شيء!", y: H * 0.3, life: 2, big: true });
              flash = 0.4;
            } else {
              floats.push({ text: "🍖 وجبة! " + meat + "/5", y: H * 0.36, life: 1 });
            }
            updateHUD();
          }
        }
      }
    }
    pickups = pickups.filter(u => u.z > -3);
  }

  for (const p of particles) { p.life -= dt; p.z -= (p.vx || 0) * dt * 2; p.y += (p.vy || 0) * dt * 0.5; }
  particles = particles.filter(p => p.life > 0);
  for (const f of floats) { f.life -= rawDt; f.y -= rawDt * 30; }
  floats = floats.filter(f => f.life > 0);

  /* render */
  ctx.save();
  if (shake > 0) {
    shake = Math.max(shake - rawDt * 1.8, 0);
    ctx.translate((Math.random() - 0.5) * 16 * shake, (Math.random() - 0.5) * 16 * shake);
  }
  const P = pal();
  drawSky(P, now);
  drawSea(P, now);
  drawDeck();
  const items = [];
  for (const o of obstacles) items.push({ z: o.z, d: () => drawObstacle(o, now) });
  for (const c of coinList) items.push({ z: c.z, d: () => drawCoin(c, now) });
  for (const u of pickups) items.push({ z: u.z, d: () => drawPickup(u, now) });
  for (const s of scenery) items.push({ z: s.z, d: () => drawScenery(s) });
  items.sort((a, b) => b.z - a.z);
  for (const it of items) it.d();
  if (state !== "menu") drawPlayer(now);
  for (const p of particles) {
    const pr = proj(p.x * LANE_W, p.y, p.z);
    ctx.globalAlpha = Math.max(p.life * 2.4, 0);
    ctx.fillStyle = p.col || "#f6d788";
    ctx.beginPath(); ctx.arc(pr.x, pr.y, 3.5 * pr.s * 2 + 1, 0, 7); ctx.fill();
  }
  ctx.globalAlpha = 1;
  for (const f of floats) {
    ctx.globalAlpha = Math.min(f.life, 1);
    ctx.fillStyle = "#e0b84c";
    ctx.font = (f.big ? "bold 26px" : "18px") + " Tahoma";
    ctx.textAlign = "center";
    ctx.strokeStyle = "rgba(13,27,42,.8)"; ctx.lineWidth = 4;
    ctx.strokeText(f.text, W / 2, f.y);
    ctx.fillText(f.text, W / 2, f.y);
  }
  ctx.globalAlpha = 1;
  ctx.restore();
  if (flash > 0) {
    flash = Math.max(flash - rawDt * 1.6, 0);
    ctx.fillStyle = "rgba(255,255,255," + flash * 0.5 + ")";
    ctx.fillRect(0, 0, W, H);
  }
  requestAnimationFrame(loop);
}

/* ================= Boot ================= */
reset();
toMenu();
$("muteBtn").textContent = save.sound ? "🔊" : "🔇";
renderMissions();
setInterval(drawCharPreview, 80);
requestAnimationFrame(loop);
