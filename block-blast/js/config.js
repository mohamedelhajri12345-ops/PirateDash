// ============================================================
//  STAR PUZZLE — config.js
//  Board geometry, block palette (Star Puzzle brand colors),
//  and the 26-shape Block Blast catalog. Split from the
//  original game.js without any behavior change.
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

// Block palette — restored EXACTLY to the original file the owner
// provided (block-blast-clone/game.js). Only the brand name differs.
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
