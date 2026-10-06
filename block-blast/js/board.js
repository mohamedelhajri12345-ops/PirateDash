// ============================================================
//  STAR PUZZLE — board.js
//  Grid state, piece spawning, placement validation, line
//  clears and the game-over rule. Split from the original
//  game.js without any behavior change.
// ============================================================

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

// -------------------- GRID & SPAWNING --------------------
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
        document.getElementById('bestScore').textContent = 'BEST ' + Math.max(score, getBest());
        document.getElementById('coinsGo').textContent = getCoins();
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
    addCoins(totalCleared * 5); // +5 coins per cleared line (reward UI)

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


// -------------------- COINS & BEST (reward UI) --------------------
// Additive on top of the original gameplay: coins are visible on
// every screen (owner requirement) and the best score persists.
var coinStore = (typeof localStorage !== 'undefined')
    ? localStorage
    : { getItem: function() { return null; }, setItem: function() {} };

function getBest() {
    return parseInt(coinStore.getItem('sp_best') || '0', 10) || 0;
}

function getCoins() {
    return parseInt(coinStore.getItem('sp_coins') || '100', 10) || 0;
}

function addCoins(n) {
    coinStore.setItem('sp_coins', getCoins() + n);
    refreshCoinsUI();
}

function refreshCoinsUI() {
    var ids = ['coinsGame', 'coinsMenu', 'coinsGo'];
    for (var i = 0; i < ids.length; i++) {
        var el = document.getElementById(ids[i]);
        if (el) el.textContent = getCoins();
    }
}
