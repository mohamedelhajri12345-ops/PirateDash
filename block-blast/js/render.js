// ============================================================
//  STAR PUZZLE — render.js
//  Canvas drawing: grid, glossy blocks, drop preview.
//  Split from the original game.js without any behavior
//  change.
// ============================================================

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
