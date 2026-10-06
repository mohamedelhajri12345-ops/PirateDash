// ============================================================
//  STAR PUZZLE — render.js
//  Canvas drawing matched to the REAL Block Blast look
//  (official Play Store screenshots): flat navy background
//  #242C54, board panel #2A3260, empty cells #1E264A, glossy
//  bevel blocks, tray slots. Gameplay code untouched.
// ============================================================

// Real Block Blast screen colors (measured from the official
// Google Play screenshots of Block Blast!)
const BB_BG      = '#242C54'; // screen background
const BB_PANEL   = '#2A3260'; // board container panel
const BB_CELL    = '#1E264A'; // empty cell
const BB_SLOT    = '#1E264A'; // tray slot

// -------------------- DRAWING --------------------
function draw() {
    ctx.clearRect(0, 0, CANVAS_W, CANVAS_H);
    drawBoardPanel();
    drawGrid();
    drawPreview();
    drawBlocks();
}

function darken(hex, f) {
    var n = parseInt(hex.slice(1), 16);
    var r = Math.floor(((n >> 16) & 255) * f);
    var g = Math.floor(((n >> 8) & 255) * f);
    var b = Math.floor((n & 255) * f);
    return 'rgb(' + r + ',' + g + ',' + b + ')';
}

function bbBlock(x, y, s, color, rad) {
    var r = rad !== undefined ? rad : Math.max(3, s * 0.16);
    // base with darker 3D bottom edge (Block Blast bevel)
    ctx.fillStyle = darken(color, 0.62);
    roundRect(ctx, x, y, s, s, r, true, false);
    // main face
    ctx.fillStyle = color;
    roundRect(ctx, x, y, s, s - Math.max(2, s * 0.10), r, true, false);
    // glossy top half
    ctx.fillStyle = 'rgba(255,255,255,0.18)';
    roundRect(ctx, x + s * 0.06, y + s * 0.05, s * 0.88, s * 0.42, r * 0.7, true, false);
}

function drawBoardPanel() {
    ctx.fillStyle = BB_PANEL;
    roundRect(ctx, GRID_X - 14, GRID_Y - 14, GRID_PX + 28, GRID_PX + 28, 16, true, false);
}

function drawGrid() {
    for (var r = 0; r < GRID_SIZE; r++) {
        for (var c = 0; c < GRID_SIZE; c++) {
            var x = GRID_X + c * CELL_SIZE;
            var y = GRID_Y + r * CELL_SIZE;
            if (grid[r][c]) {
                bbBlock(x + CELL_GAP, y + CELL_GAP, CELL_SIZE - CELL_GAP * 2, grid[r][c]);
            } else {
                ctx.fillStyle = BB_CELL;
                roundRect(ctx, x + CELL_GAP, y + CELL_GAP, CELL_SIZE - CELL_GAP * 2, CELL_SIZE - CELL_GAP * 2, 6, true, false);
            }
        }
    }
}

function drawBlocks() {
    // tray slots exactly like the real game: three dark rounded panels
    var slotW = CANVAS_W / 3;
    for (var i = 0; i < 3; i++) {
        ctx.fillStyle = BB_SLOT;
        roundRect(ctx, slotW * i + 8, BLOCK_AREA_Y + 16, slotW - 16, 116, 14, true, false);
    }
    for (var i = 0; i < 3; i++) {
        var b = blocks[i];
        if (b.placed) continue;
        var cs = (dragging === i) ? CELL_SIZE : PREVIEW_CELL;
        for (var r = 0; r < b.shape.length; r++) {
            for (var c = 0; c < b.shape[r].length; c++) {
                if (b.shape[r][c]) {
                    bbBlock(b.x + c * cs + 1, b.y + r * cs + 1, cs - 2, b.color, 4);
                }
            }
        }
    }
}

function drawPreview() {
    if (dragging === null || !previewPos) return;
    var b = blocks[dragging];
    ctx.globalAlpha = 0.4;
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
