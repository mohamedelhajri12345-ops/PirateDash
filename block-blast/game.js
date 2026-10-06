// ============================================================
//  BLOCK BLAST CLONE — Complete Game Logic
// ============================================================

// -------------------- CONSTANTS --------------------
const GRID_SIZE = 8;
const CELL_SIZE = 44;
const CELL_GAP = 2;
const GRID_PX = GRID_SIZE * CELL_SIZE;
const CANVAS_W = GRID_PX + 40;
const CANVAS_H = GRID_PX + 220;
const GRID_X = 20;
const GRID_Y = 10;
const BLOCK_AREA_Y = GRID_Y + GRID_PX + 30;
const PREVIEW_CELL = 20;

const COLORS = [
    '#FF6B6B', '#4ECDC4', '#45B7D1', '#96CEB4',
    '#FFEAA7', '#DDA0DD', '#FF8C42', '#74b9ff'
];

const SHAPES = [
    [[1]],
    [[1,1]],
    [[1,1,1]],
    [[1,1,1,1]],
    [[1,1,1,1,1]],
    [[1],[1]],
    [[1],[1],[1]],
    [[1],[1],[1],[1]],
    [[1],[1],[1],[1],[1]],
    [[1,1],[1,1]],
    [[1,1,1],[1,1,1],[1,1,1]],
    [[1,0],[1,0],[1,1]],
    [[0,1],[0,1],[1,1]],
    [[1,1,1],[0,1,0]],
    [[0,1,1],[1,1,0]],
    [[1,1,0],[0,1,1]],
    [[1,0],[1,1]],
    [[0,1],[1,1]],
    [[1,1],[1,0]],
    [[1,1],[0,1]],
    [[1,1,1],[1,0,0]],
    [[1,1,1],[0,0,1]],
    [[1,0,0],[1,1,1]],
    [[0,0,1],[1,1,1]],
    [[1,1],[1,1],[1,1]],
    [[1,1,1],[1,1,1]],
];

// -------------------- AUDIO MANAGER --------------------
class SoundPool {
    constructor(src, size) {
        this.pool = [];
        for (let i = 0; i < size; i++) {
            const a = new Audio(src);
            a.volume = 0.5;
            this.pool.push(a);
        }
        this.idx = 0;
    }
    play(vol) {
        const a = this.pool[this.idx];
        a.currentTime = 0;
        a.volume = vol !== undefined ? vol : 0.5;
        a.play().catch(function(){});
        this.idx = (this.idx + 1) % this.pool.length;
    }
}

const audio = {
    bgm: null,
    place: null,
    clear: null,
    combo: null,
    gameover: null,
    click: null,
    init: function() {
        this.bgm = new Audio('assets/audio/bgm.mp3');
        this.bgm.loop = true;
        this.bgm.volume = 0.25;
        this.place = new SoundPool('assets/audio/place.mp3', 5);
        this.clear = new SoundPool('assets/audio/clear.mp3', 5);
        this.combo = new SoundPool('assets/audio/combo.mp3', 3);
        this.gameover = new SoundPool('assets/audio/gameover.mp3', 1);
        this.click = new SoundPool('assets/audio/click.mp3', 3);
    },
    playBGM: function() {
        this.bgm.play().catch(function(){});
    },
    stopBGM: function() {
        this.bgm.pause();
        this.bgm.currentTime = 0;
    }
};

// -------------------- GAME STATE --------------------
var grid = [];
var score = 0;
var blocks = [];
var dragging = null;
var dragOffX = 0;
var dragOffY = 0;
var previewPos = null;
var isGameOver = false;
var canvas, ctx;

// -------------------- INITIALIZATION --------------------
function init() {
    canvas = document.getElementById('gameCanvas');
    ctx = canvas.getContext('2d');
    canvas.width = CANVAS_W;
    canvas.height = CANVAS_H;

    audio.init();

    grid = makeEmptyGrid();
    score = 0;
    isGameOver = false;
    spawnThreeBlocks();

    canvas.addEventListener('mousedown', onPointerDown);
    canvas.addEventListener('mousemove', onPointerMove);
    canvas.addEventListener('mouseup', onPointerUp);
    canvas.addEventListener('touchstart', onTouchStart, {passive: false});
    canvas.addEventListener('touchmove', onTouchMove, {passive: false});
    canvas.addEventListener('touchend', onTouchEnd, {passive: false});

    document.getElementById('startBtn').addEventListener('click', function() {
        document.getElementById('menuOverlay').style.display = 'none';
        audio.click.play(0.3);
        audio.playBGM();
    });

    document.getElementById('restartBtn').addEventListener('click', function() {
        document.getElementById('gameOverOverlay').style.display = 'none';
        audio.click.play(0.3);
        grid = makeEmptyGrid();
        score = 0;
        isGameOver = false;
        spawnThreeBlocks();
        updateScoreDisplay();
        audio.playBGM();
        draw();
    });

    updateScoreDisplay();
    draw();
}

function makeEmptyGrid() {
    var g = [];
    for (var r = 0; r < GRID_SIZE; r++) {
        g.push([]);
        for (var c = 0; c < GRID_SIZE; c++) {
            g[r].push(null);
        }
    }
    return g;
}

function spawnThreeBlocks() {
    blocks = [];
    for (var i = 0; i < 3; i++) {
        blocks.push(makeBlock(i));
    }
}

function makeBlock(slot) {
    var shape = SHAPES[Math.floor(Math.random() * SHAPES.length)];
    var color = COLORS[Math.floor(Math.random() * COLORS.length)];
    var slotW = CANVAS_W / 3;
    var bx = slotW * slot + slotW / 2 - (shape[0].length * PREVIEW_CELL) / 2;
    var by = BLOCK_AREA_Y + 40;
    return {
        shape: shape,
        color: color,
        x: bx,
        y: by,
        origX: bx,
        origY: by,
        placed: false
    };
}

// -------------------- DRAWING --------------------
function draw() {
    ctx.clearRect(0, 0, CANVAS_W, CANVAS_H);
    drawGrid();
    drawPreview();
    drawBlocks();
}

function drawGrid() {
    for (var r = 0; r < GRID_SIZE; r++) {
        for (var c = 0; c < GRID_SIZE; c++) {
            var x = GRID_X + c * CELL_SIZE;
            var y = GRID_Y + r * CELL_SIZE;
            if (grid[r][c]) {
                ctx.fillStyle = grid[r][c];
                roundRect(ctx, x + CELL_GAP, y + CELL_GAP, CELL_SIZE - CELL_GAP * 2, CELL_SIZE - CELL_GAP * 2, 6, true, false);
                // highlight
                ctx.fillStyle = 'rgba(255,255,255,0.15)';
                roundRect(ctx, x + CELL_GAP, y + CELL_GAP, CELL_SIZE - CELL_GAP * 2, (CELL_SIZE - CELL_GAP * 2) / 2, 6, true, false);
            } else {
                ctx.fillStyle = '#2a2a4a';
                roundRect(ctx, x + CELL_GAP, y + CELL_GAP, CELL_SIZE - CELL_GAP * 2, CELL_SIZE - CELL_GAP * 2, 6, true, false);
            }
        }
    }
}

function drawBlocks() {
    for (var i = 0; i < 3; i++) {
        var b = blocks[i];
        if (b.placed) continue;
        var cs = (dragging === i) ? CELL_SIZE : PREVIEW_CELL;
        for (var r = 0; r < b.shape.length; r++) {
            for (var c = 0; c < b.shape[r].length; c++) {
                if (b.shape[r][c]) {
                    var x = b.x + c * cs;
                    var y = b.y + r * cs;
                    ctx.fillStyle = b.color;
                    roundRect(ctx, x + 1, y + 1, cs - 2, cs - 2, 4, true, false);
                    ctx.fillStyle = 'rgba(255,255,255,0.2)';
                    roundRect(ctx, x + 1, y + 1, cs - 2, (cs - 2) / 2, 4, true, false);
                }
            }
        }
    }
}

function drawPreview() {
    if (dragging === null || !previewPos) return;
    var b = blocks[dragging];
    ctx.globalAlpha = 0.35;
    for (var r = 0; r < b.shape.length; r++) {
        for (var c = 0; c < b.shape[r].length; c++) {
            if (b.shape[r][c]) {
                var x = GRID_X + (previewPos.col + c) * CELL_SIZE;
                var y = GRID_Y + (previewPos.row + r) * CELL_SIZE;
                ctx.fillStyle = b.color;
                roundRect(ctx, x + CELL_GAP, y + CELL_GAP, CELL_SIZE - CELL_GAP * 2, CELL_SIZE - CELL_GAP * 2, 6, true, false);
            }
        }
    }
    ctx.globalAlpha = 1.0;
}

function roundRect(ctx, x, y, w, h, r, fill, stroke) {
    ctx.beginPath();
    ctx.moveTo(x + r, y);
    ctx.arcTo(x + w, y, x + w, y + h, r);
    ctx.arcTo(x + w, y + h, x, y + h, r);
    ctx.arcTo(x, y + h, x, y, r);
    ctx.arcTo(x, y, x + w, y, r);
    ctx.closePath();
    if (fill) ctx.fill();
    if (stroke) ctx.stroke();
}

// -------------------- GAME LOGIC --------------------
function canPlace(block, row, col) {
    for (var r = 0; r < block.shape.length; r++) {
        for (var c = 0; c < block.shape[r].length; c++) {
            if (block.shape[r][c]) {
                var gr = row + r;
                var gc = col + c;
                if (gr < 0 || gr >= GRID_SIZE || gc < 0 || gc >= GRID_SIZE) return false;
                if (grid[gr][gc] !== null) return false;
            }
        }
    }
    return true;
}

function placeBlock(block, row, col) {
    var cells = 0;
    for (var r = 0; r < block.shape.length; r++) {
        for (var c = 0; c < block.shape[r].length; c++) {
            if (block.shape[r][c]) {
                grid[row + r][col + c] = block.color;
                cells++;
            }
        }
    }
    score += cells;
    block.placed = true;
    audio.place.play(0.6);
    checkAndClearLines();
    updateScoreDisplay();
    if (checkGameOver()) {
        isGameOver = true;
        audio.stopBGM();
        audio.gameover.play(0.7);
        document.getElementById('finalScore').textContent = score;
        setTimeout(function() {
            document.getElementById('gameOverOverlay').style.display = 'flex';
        }, 500);
    }
    // If all 3 placed, spawn new 3
    if (blocks[0].placed && blocks[1].placed && blocks[2].placed) {
        spawnThreeBlocks();
    }
}

function checkAndClearLines() {
    var rowsToClear = [];
    var colsToClear = [];

    for (var r = 0; r < GRID_SIZE; r++) {
        var full = true;
        for (var c = 0; c < GRID_SIZE; c++) {
            if (grid[r][c] === null) { full = false; break; }
        }
        if (full) rowsToClear.push(r);
    }

    for (var c2 = 0; c2 < GRID_SIZE; c2++) {
        var full2 = true;
        for (var r2 = 0; r2 < GRID_SIZE; r2++) {
            if (grid[r2][c2] === null) { full2 = false; break; }
        }
        if (full2) colsToClear.push(c2);
    }

    var totalCleared = rowsToClear.length + colsToClear.length;
    if (totalCleared === 0) return;

    // Clear rows
    for (var i = 0; i < rowsToClear.length; i++) {
        for (var c3 = 0; c3 < GRID_SIZE; c3++) {
            grid[rowsToClear[i]][c3] = null;
        }
    }
    // Clear cols
    for (var j = 0; j < colsToClear.length; j++) {
        for (var r3 = 0; r3 < GRID_SIZE; r3++) {
            grid[r3][colsToClear[j]] = null;
        }
    }

    score += totalCleared * GRID_SIZE * 2;

    if (totalCleared >= 2) {
        audio.combo.play(0.7);
    } else {
        audio.clear.play(0.6);
    }
}

function checkGameOver() {
    for (var i = 0; i < 3; i++) {
        if (blocks[i].placed) continue;
        for (var r = 0; r < GRID_SIZE; r++) {
            for (var c = 0; c < GRID_SIZE; c++) {
                if (canPlace(blocks[i], r, c)) return false;
            }
        }
    }
    return true;
}

function updateScoreDisplay() {
    document.getElementById('score-value').textContent = score;
}

// -------------------- INPUT HANDLING --------------------
function getCanvasPos(e) {
    var rect = canvas.getBoundingClientRect();
    var scaleX = canvas.width / rect.width;
    var scaleY = canvas.height / rect.height;
    return {
        x: (e.clientX - rect.left) * scaleX,
        y: (e.clientY - rect.top) * scaleY
    };
}

function hitTestBlock(px, py) {
    for (var i = 0; i < 3; i++) {
        var b = blocks[i];
        if (b.placed) continue;
        var bw = b.shape[0].length * PREVIEW_CELL;
        var bh = b.shape.length * PREVIEW_CELL;
        if (px >= b.x && px <= b.x + bw && py >= b.y && py <= b.y + bh) {
            return i;
        }
    }
    return -1;
}

function getGridCell(px, py, block) {
    var centerX = px + (block.shape[0].length * CELL_SIZE) / 2;
    var centerY = py + (block.shape.length * CELL_SIZE) / 2;
    var col = Math.round((centerX - GRID_X - (block.shape[0].length * CELL_SIZE) / 2) / CELL_SIZE);
    var row = Math.round((centerY - GRID_Y - (block.shape.length * CELL_SIZE) / 2) / CELL_SIZE);
    return {row: row, col: col};
}

function handleDown(px, py) {
    if (isGameOver) return;
    var idx = hitTestBlock(px, py);
    if (idx >= 0) {
        dragging = idx;
        var b = blocks[idx];
        // grow to full size and center the piece on the finger
        b.x = px - (b.shape[0].length * CELL_SIZE) / 2;
        b.y = py - (b.shape.length * CELL_SIZE) / 2 - 40;
        dragOffX = 0;
        dragOffY = -40;
        previewPos = null;
        audio.click.play(0.25);
        draw();
    }
}

function handleMove(px, py) {
    if (dragging === null) return;
    var b = blocks[dragging];
    b.x = px - (b.shape[0].length * CELL_SIZE) / 2;
    b.y = py - (b.shape.length * CELL_SIZE) / 2 - 40;
    var cell = getGridCell(b.x, b.y, b);
    previewPos = canPlace(b, cell.row, cell.col) ? cell : null;
    draw();
}

function handleUp() {
    if (dragging === null) return;
    var b = blocks[dragging];
    var cell = getGridCell(b.x, b.y, b);
    if (canPlace(b, cell.row, cell.col)) {
        placeBlock(b, cell.row, cell.col);
    } else {
        b.x = b.origX;
        b.y = b.origY;
    }
    dragging = null;
    previewPos = null;
    draw();
}

function onPointerDown(e) { handleDown(getCanvasPos(e).x, getCanvasPos(e).y); }
function onPointerMove(e) { handleMove(getCanvasPos(e).x, getCanvasPos(e).y); }
function onPointerUp(e) { handleUp(); }

function onTouchStart(e) {
    e.preventDefault();
    var t = e.touches[0];
    handleDown(getCanvasPos(t).x, getCanvasPos(t).y);
}

function onTouchMove(e) {
    e.preventDefault();
    var t = e.touches[0];
    handleMove(getCanvasPos(t).x, getCanvasPos(t).y);
}

function onTouchEnd(e) {
    e.preventDefault();
    handleUp();
}

// -------------------- BOOT --------------------
window.onload = init;
