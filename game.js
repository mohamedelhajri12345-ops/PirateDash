/* سباق القراصنة - Pirate Dash v0.1
   Endless pseudo-3D pirate runner, HTML5 Canvas.
   Original characters, halal content. MIT License. */

"use strict";

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

/* ---------- Game state ---------- */
const LANES = [-1, 0, 1];
const LANE_W = 2.2;          // world units
const CAM_H = 2.6;           // camera height
const PROJ_D = 6;            // projection depth constant
const DRAW_DIST = 60;        // how far we can see

let state = "menu";          // menu | run | over
let score, coins, best, speed, distance;
let obstacles, coinList, particles, spawnZ, runTime;
let player = { lane: 1, x: 0, jumpV: 0, jumpY: 0, sliding: false, slideT: 0, tilt: 0 };
let shake = 0;

best = parseInt(localStorage.getItem("piratedash_best") || "0", 10);
const $ = id => document.getElementById(id);
$("best").textContent = "🏆 " + best;

function reset() {
  score = 0; coins = 0; distance = 0; speed = 16; runTime = 0;
  obstacles = []; coinList = []; particles = [];
  spawnZ = 30; shake = 0;
  player = { lane: 1, x: 0, jumpV: 0, jumpY: 0, sliding: false, slideT: 0, tilt: 0 };
  $("score").textContent = "0";
  $("coins").textContent = "0";
}

/* ---------- Projection: world -> screen ---------- */
/* World: x = lane offset units, z = depth ahead of camera. */
function project(wx, wy, wz) {
  const zz = Math.max(wz, 0.001);
  const scale = PROJ_D / (zz + PROJ_D * 0.6);
  const horizon = H * 0.38;
  const groundY = horizon + (H - horizon) * scale;
  const sx = W / 2 + wx * (W / 7) * scale;
  const sy = groundY - wy * (H / 9) * scale - CAM_H * (H / 9) * scale * 0.55;
  return { x: sx, y: sy, s: scale };
}

/* ---------- Spawning ---------- */
function spawnPattern() {
  const patterns = [
    // one barrier wall in a lane, coins in another
    { obs: [{ lane: 0, type: "wall" }], coins: [{ lane: 2, z: spawnZ, n: 5 }] },
    { obs: [{ lane: 2, type: "wall" }], coins: [{ lane: 0, z: spawnZ, n: 4 }] },
    // jumpables (barrels) across, must jump
    { obs: [{ lane: 0, type: "barrel" }, { lane: 1, type: "barrel" }], coins: [{ lane: 1, z: spawnZ - 2, n: 5, air: true }] },
    // slide gate (hanging sign) across all lanes
    { obs: [{ lane: 0, type: "gate" }, { lane: 1, type: "gate" }, { lane: 2, type: "gate" }], coins: [] },
    // mixed wall + gate
    { obs: [{ lane: 2, type: "wall" }, { lane: 0, type: "barrel" }], coins: [{ lane: 1, z: spawnZ, n: 6 }] },
    // double wall, single gap
    { obs: [{ lane: 0, type: "wall" }, { lane: 2, type: "wall" }], coins: [{ lane: 1, z: spawnZ, n: 7 }] },
  ];
  const p = patterns[(Math.random() * patterns.length) | 0];
  for (const o of p.obs) obstacles.push({ lane: o.lane, type: o.type, z: spawnZ + Math.random() * 2 });
  for (const c of p.coins) {
    for (let i = 0; i < c.n; i++) coinList.push({ lane: c.lane, z: c.z + i * 1.6, y: c.air ? 1.6 : 0.9 });
  }
  spawnZ += 14 + Math.random() * 8 - Math.min(runTime * 0.02, 5);
}

/* ---------- Input ---------- */
function move(dir) {
  if (state !== "run") return;
  const cur = LANES.indexOf(player.lane);
  const nxt = Math.min(2, Math.max(0, cur + dir));
  if (nxt !== cur) { player.lane = LANES[nxt]; player.tilt = dir * 0.35; }
}
function jump() {
  if (state !== "run") return;
  if (player.jumpY <= 0.01 && !player.sliding) { player.jumpV = 7.5; }
}
function slide() {
  if (state !== "run") return;
  if (player.jumpY > 0.5) { player.jumpV = -10; }        // fast-drop from air
  else if (!player.sliding) { player.sliding = true; player.slideT = 0.55; }
}

document.addEventListener("keydown", e => {
  if (e.repeat) return;
  if (e.key === "ArrowLeft" || e.key === "a") move(1);   // RTL canvas: left key = left on screen
  else if (e.key === "ArrowRight" || e.key === "d") move(-1);
  else if (e.key === "ArrowUp" || e.key === "w" || e.key === " ") jump();
  else if (e.key === "ArrowDown" || e.key === "s") slide();
  else if (e.key === "Enter" && state !== "run") startRun();
});

/* touch swipes */
let tsX = 0, tsY = 0, tsT = 0;
canvas.addEventListener("touchstart", e => {
  const t = e.changedTouches[0]; tsX = t.clientX; tsY = t.clientY; tsT = performance.now();
}, { passive: true });
canvas.addEventListener("touchend", e => {
  const t = e.changedTouches[0];
  const dx = t.clientX - tsX, dy = t.clientY - tsY;
  if (Math.abs(dx) < 24 && Math.abs(dy) < 24) { if (state === "run") jump(); return; }
  if (Math.abs(dx) > Math.abs(dy)) move(dx < 0 ? 1 : -1);
  else if (dy < 0) jump(); else slide();
}, { passive: true });

$("startBtn").addEventListener("click", startRun);
$("retryBtn").addEventListener("click", startRun);

function startRun() {
  reset();
  state = "run";
  $("menu").classList.add("hidden");
  $("over").classList.add("hidden");
  $("touchHint").classList.remove("hidden2");
}

function gameOver() {
  state = "over";
  shake = 1;
  $("finalScore").textContent = "المسافة: " + Math.floor(score) + " م";
  $("finalCoins").textContent = "🪙 الذهب: " + coins;
  if (score > best) {
    best = Math.floor(score);
    localStorage.setItem("piratedash_best", String(best));
    $("best").textContent = "🏆 " + best;
    $("overTitle").textContent = "🎉 رقم قياسي جديد!";
  } else {
    $("overTitle").textContent = "💰 سقط القبطان!";
  }
  setTimeout(() => $("over").classList.remove("hidden"), 550);
}

/* ---------- Drawing helpers ---------- */
function drawSea() {
  const horizon = H * 0.38;
  // sky
  const sky = ctx.createLinearGradient(0, 0, 0, horizon);
  sky.addColorStop(0, "#1d3557"); sky.addColorStop(1, "#e8a05c");
  ctx.fillStyle = sky; ctx.fillRect(0, 0, W, horizon);
  // sun
  ctx.fillStyle = "rgba(255,224,138,.9)";
  ctx.beginPath(); ctx.arc(W * 0.72, horizon * 0.62, 34, 0, 7); ctx.fill();
  // sea
  const sea = ctx.createLinearGradient(0, horizon, 0, H);
  sea.addColorStop(0, "#14405c"); sea.addColorStop(1, "#0d1b2a");
  ctx.fillStyle = sea; ctx.fillRect(0, horizon, W, H - horizon);
  // sparkles
  ctx.fillStyle = "rgba(255,255,255,.18)";
  for (let i = 0; i < 24; i++) {
    const t = (performance.now() * 0.0006 + i * 0.62) % 1;
    const y = horizon + (H - horizon) * t * 0.35;
    const x = (i * 137.5) % W;
    ctx.fillRect(x, y, 14 * t + 3, 2);
  }
}

function drawDeck() {
  const horizon = H * 0.38;
  // wooden deck surface (trapezoid)
  const l0 = project(-LANE_W * 1.5 - 0.6, 0, 0), r0 = project(LANE_W * 1.5 + 0.6, 0, 0);
  const lF = project(-LANE_W * 1.5 - 0.6, 0, DRAW_DIST), rF = project(LANE_W * 1.5 + 0.6, 0, DRAW_DIST);
  const g = ctx.createLinearGradient(0, horizon, 0, H);
  g.addColorStop(0, "#5c4033"); g.addColorStop(1, "#8a5a3b");
  ctx.fillStyle = g;
  ctx.beginPath();
  ctx.moveTo(lF.x, lF.y); ctx.lineTo(rF.x, rF.y);
  ctx.lineTo(r0.x, r0.y); ctx.lineTo(l0.x, l0.y);
  ctx.closePath(); ctx.fill();
  // lane lines
  ctx.strokeStyle = "rgba(224,184,76,.5)"; ctx.lineWidth = 2;
  for (const lx of [-LANE_W, 0, LANE_W]) {
    const a = project(lx, 0, 0.01), b = project(lx, 0, DRAW_DIST);
    ctx.beginPath(); ctx.moveTo(a.x, a.y); ctx.lineTo(b.x, b.y); ctx.stroke();
  }
  // moving planks for speed feel
  ctx.fillStyle = "rgba(0,0,0,.16)";
  const off = (distance * 2) % 4;
  for (let z = 0; z < DRAW_DIST; z += 4) {
    const zz = z + (4 - off);
    const a = project(-LANE_W * 1.5 - 0.6, 0, zz), b = project(LANE_W * 1.5 + 0.6, 0, zz);
    const c = project(-LANE_W * 1.5 - 0.6, 0, zz + 0.35), d = project(LANE_W * 1.5 + 0.6, 0, zz + 0.35);
    ctx.beginPath(); ctx.moveTo(a.x, a.y); ctx.lineTo(b.x, b.y);
    ctx.lineTo(d.x, d.y); ctx.lineTo(c.x, c.y); ctx.closePath(); ctx.fill();
  }
}

function drawObstacle(o) {
  const wx = LANES[o.lane] * LANE_W;
  const base = project(wx, 0, o.z);
  const s = base.s;
  if (base.y > H + 50) return;
  if (o.type === "wall") {
    // tall crate wall
    const w = 70 * s * 2, h = 130 * s * 2;
    ctx.fillStyle = "#7f4f24";
    ctx.fillRect(base.x - w / 2, base.y - h, w, h);
    ctx.strokeStyle = "#5b371b"; ctx.lineWidth = 2;
    ctx.strokeRect(base.x - w / 2, base.y - h, w, h);
    ctx.beginPath(); ctx.moveTo(base.x - w / 2, base.y - h); ctx.lineTo(base.x + w / 2, base.y);
    ctx.moveTo(base.x + w / 2, base.y - h); ctx.lineTo(base.x - w / 2, base.y); ctx.stroke();
  } else if (o.type === "barrel") {
    // low barrel: jump over
    const rw = 56 * s * 2, rh = 62 * s * 2;
    ctx.fillStyle = "#a47148";
    ctx.beginPath();
    ctx.ellipse(base.x, base.y - rh / 2, rw / 2, rh / 2, 0, 0, 7);
    ctx.fill();
    ctx.strokeStyle = "#3d2314"; ctx.lineWidth = 3;
    ctx.beginPath(); ctx.ellipse(base.x, base.y - rh / 2, rw / 2 - 2, rh / 2 - 2, 0, 0, 7); ctx.stroke();
    ctx.beginPath(); ctx.moveTo(base.x - rw / 2 + 2, base.y - rh / 2 - 8 * s);
    ctx.lineTo(base.x + rw / 2 - 2, base.y - rh / 2 - 8 * s); ctx.stroke();
    ctx.beginPath(); ctx.moveTo(base.x - rw / 2 + 2, base.y - rh / 2 + 8 * s);
    ctx.lineTo(base.x + rw / 2 - 2, base.y - rh / 2 + 8 * s); ctx.stroke();
  } else if (o.type === "gate") {
    // hanging banner: slide under
    const w = 90 * s * 2, h = 95 * s * 2;
    const top = project(wx, 2.4, o.z);
    ctx.fillStyle = "#8b0000";
    ctx.fillRect(base.x - w / 2, top.y, w, h);
    ctx.strokeStyle = "#e0b84c"; ctx.lineWidth = 2;
    ctx.strokeRect(base.x - w / 2, top.y, w, h);
    // skull-free ornament (star, not skull)
    ctx.fillStyle = "#e0b84c";
    ctx.save(); ctx.translate(base.x, top.y + h * 0.32);
    ctx.beginPath();
    for (let i = 0; i < 10; i++) {
      const r = i % 2 ? 6 * s * 2 : 13 * s * 2, a = i * Math.PI / 5 - Math.PI / 2;
      ctx.lineTo(Math.cos(a) * r, Math.sin(a) * r);
    }
    ctx.closePath(); ctx.fill(); ctx.restore();
    // posts
    ctx.fillStyle = "#5b371b";
    ctx.fillRect(base.x - w / 2 - 4, top.y - 20 * s, 4, h + 40 * s * 2);
    ctx.fillRect(base.x + w / 2, top.y - 20 * s, 4, h + 40 * s * 2);
  }
}

function drawCoin(c) {
  const wx = LANES[c.lane] * LANE_W;
  const p = project(wx, c.y, c.z);
  const r = 14 * p.s * 2;
  const t = performance.now() * 0.005 + c.z;
  const wob = Math.abs(Math.cos(t));
  ctx.fillStyle = "#e0b84c";
  ctx.beginPath(); ctx.ellipse(p.x, p.y, r * wob + 1, r, 0, 0, 7); ctx.fill();
  ctx.strokeStyle = "#9c7422"; ctx.lineWidth = 1.5;
  ctx.beginPath(); ctx.ellipse(p.x, p.y, r * wob + 1, r, 0, 0, 7); ctx.stroke();
  if (wob > 0.55) {
    ctx.fillStyle = "#f6d788";
    ctx.beginPath(); ctx.ellipse(p.x, p.y, (r * 0.5) * wob, r * 0.55, 0, 0, 7); ctx.fill();
  }
}

function drawPlayer() {
  const wx = player.x;
  const p = project(wx, 0, 0);
  const s = p.s; // near scale ~ big
  const bob = Math.sin(performance.now() * 0.012) * 3 * (speed / 16);
  const jump = player.jumpY * (H / 9) * 0.55;
  const slideSquash = player.sliding ? 0.55 : 1;
  const cx = p.x, groundY = p.y - jump;

  // shadow
  ctx.fillStyle = "rgba(0,0,0,.3)";
  const shScale = 1 - player.jumpY * 0.15;
  ctx.beginPath(); ctx.ellipse(cx, p.y + 2, 26 * shScale, 7 * shScale, 0, 0, 7); ctx.fill();

  // tilt while switching lanes
  ctx.save();
  ctx.translate(cx, groundY);
  ctx.rotate(player.tilt * 0.5);
  ctx.translate(-cx, -groundY);

  const bodyH = 92 * slideSquash, bodyW = 44;
  const headR = 17;
  const top = groundY - bodyH - headR * 1.6 + bob;

  // legs (animated)
  if (!player.sliding) {
    const legT = performance.now() * 0.02;
    ctx.strokeStyle = "#2b2d42"; ctx.lineWidth = 7; ctx.lineCap = "round";
    ctx.beginPath(); ctx.moveTo(cx - 8, groundY - 26 + bob);
    ctx.lineTo(cx - 8 + Math.sin(legT) * 9, groundY - bob % 2); ctx.stroke();
    ctx.beginPath(); ctx.moveTo(cx + 8, groundY - 26 + bob);
    ctx.lineTo(cx + 8 - Math.sin(legT) * 9, groundY - bob % 2); ctx.stroke();
  } else {
    ctx.strokeStyle = "#2b2d42"; ctx.lineWidth = 7; ctx.lineCap = "round";
    ctx.beginPath(); ctx.moveTo(cx, groundY - 16); ctx.lineTo(cx + 30, groundY - 4); ctx.stroke();
  }

  // body: red vest
  ctx.fillStyle = "#a4161a";
  const vestTop = groundY - bodyH - 10 + bob;
  ctx.beginPath();
  ctx.roundRect(cx - bodyW / 2, vestTop, bodyW, bodyH * 0.62, 10);
  ctx.fill();
  // belt
  ctx.fillStyle = "#3d2314";
  ctx.fillRect(cx - bodyW / 2, vestTop + bodyH * 0.6 - 6, bodyW, 8);
  ctx.fillStyle = "#e0b84c";
  ctx.fillRect(cx - 6, vestTop + bodyH * 0.6 - 6, 12, 8);

  // arms
  ctx.strokeStyle = "#c68b59"; ctx.lineWidth = 8; ctx.lineCap = "round";
  const armT = performance.now() * 0.02;
  ctx.beginPath(); ctx.moveTo(cx - bodyW / 2 + 2, vestTop + 10);
  ctx.lineTo(cx - bodyW / 2 - 12 - Math.sin(armT) * 6, vestTop + 26); ctx.stroke();
  ctx.beginPath(); ctx.moveTo(cx + bodyW / 2 - 2, vestTop + 10);
  ctx.lineTo(cx + bodyW / 2 + 12 + Math.sin(armT) * 6, vestTop + 26); ctx.stroke();

  // head
  const headY = vestTop - headR + 2;
  ctx.fillStyle = "#d8a46f";
  ctx.beginPath(); ctx.arc(cx, headY, headR, 0, 7); ctx.fill();
  // hair
  ctx.fillStyle = "#231a14";
  ctx.beginPath(); ctx.arc(cx, headY - 3, headR * 0.95, Math.PI, 0); ctx.fill();
  // straw hat (captain's hat)
  const hatY = headY - headR * 0.75;
  ctx.fillStyle = "#e9c46a";
  ctx.beginPath(); ctx.ellipse(cx, hatY, headR * 1.7, 6, 0, 0, 7); ctx.fill();
  ctx.fillStyle = "#f4d58d";
  ctx.beginPath(); ctx.ellipse(cx, hatY - 1, headR * 0.85, headR * 0.55, 0, Math.PI, 0); ctx.fill();
  ctx.fillStyle = "#a4161a";
  ctx.fillRect(cx - headR * 0.85, hatY - 3, headR * 1.7, 3);

  ctx.restore();
}

/* ---------- Update ---------- */
let lastT = performance.now();
function loop(now) {
  const dt = Math.min((now - lastT) / 1000, 0.05);
  lastT = now;

  if (state === "run") {
    runTime += dt;
    speed = Math.min(16 + runTime * 0.55, 42);
    distance += speed * dt;
    score = distance + coins * 10;
    $("score").textContent = Math.floor(score);
    $("coins").textContent = coins;

    // player lane interpolation
    const targetX = LANES[player.lane] * LANE_W;
    player.x += (targetX - player.x) * Math.min(dt * 12, 1);
    player.tilt *= Math.max(1 - dt * 6, 0);

    // jump physics
    if (player.jumpY > 0 || player.jumpV > 0) {
      player.jumpY += player.jumpV * dt;
      player.jumpV -= 22 * dt;
      if (player.jumpY <= 0) { player.jumpY = 0; player.jumpV = 0; }
    }
    // slide timer
    if (player.sliding) {
      player.slideT -= dt;
      if (player.slideT <= 0) player.sliding = false;
    }

    // spawn
    if (distance + DRAW_DIST > spawnZ) spawnPattern();

    // move obstacles & coins toward player
    for (const o of obstacles) o.z -= speed * dt;
    for (const c of coinList) c.z -= speed * dt;
    obstacles = obstacles.filter(o => o.z > -3);
    coinList = coinList.filter(c => c.z > -3);

    // collisions
    const pz = 0.0;
    for (const o of obstacles) {
      if (o.z > pz - 0.8 && o.z < pz + 0.9) {
        const dx = Math.abs(LANES[o.lane] * LANE_W - player.x);
        if (dx < LANE_W * 0.55) {
          const jumpOK = o.type === "barrel" && player.jumpY > 0.9;
          const slideOK = o.type === "gate" && player.sliding;
          if (!jumpOK && !slideOK && o.type !== "none") { gameOver(); break; }
        }
      }
    }
    // coins pickup
    for (const c of coinList) {
      if (c.z > -0.5 && c.z < 1 && Math.abs(LANES[c.lane] * LANE_W - player.x) < LANE_W * 0.6) {
        if (!c.got) {
          c.got = true; coins++;
          for (let i = 0; i < 6; i++) particles.push({
            x: 0, y: 0, lane: c.lane, z: c.z, vy: (Math.random() - 0.5) * 4,
            vx: (Math.random() - 0.5) * 4, life: 0.5, yw: c.y
          });
        }
      }
    }
    coinList = coinList.filter(c => !c.got);
  }

  // ---------- render ----------
  ctx.save();
  if (shake > 0) {
    shake = Math.max(shake - dt * 2, 0);
    ctx.translate((Math.random() - 0.5) * 14 * shake, (Math.random() - 0.5) * 14 * shake);
  }
  drawSea();
  drawDeck();
  // depth sort: draw far to near
  const all = [];
  for (const o of obstacles) all.push({ z: o.z, draw: () => drawObstacle(o) });
  for (const c of coinList) all.push({ z: c.z, draw: () => drawCoin(c) });
  all.sort((a, b) => b.z - a.z);
  for (const it of all) it.draw();
  if (state !== "menu") drawPlayer();

  // particles
  ctx.fillStyle = "#f6d788";
  for (const p of particles) {
    p.life -= dt; p.yw += p.vy * dt * 0.3; p.z += p.vx * dt;
    if (p.life > 0) {
      const pr = project(LANES[p.lane] * LANE_W, p.yw, p.z);
      ctx.globalAlpha = Math.min(p.life * 2, 1);
      ctx.beginPath(); ctx.arc(pr.x, pr.y, 4 * pr.s * 2, 0, 7); ctx.fill();
    }
  }
  ctx.globalAlpha = 1;
  particles = particles.filter(p => p.life > 0);
  ctx.restore();

  requestAnimationFrame(loop);
}
requestAnimationFrame(loop);

// menu idle: gentle demo movement
setInterval(() => { if (state === "menu") { distance = (distance || 0) + 1; } }, 50);
