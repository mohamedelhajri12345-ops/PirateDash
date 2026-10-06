// ============================================================
//  STAR PUZZLE — input.js
//  Pointer + touch handling with drag, snap preview and
//  drop. Split from the original game.js without any
//  behavior change.
// ============================================================

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
