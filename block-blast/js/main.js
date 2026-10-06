// ============================================================
//  STAR PUZZLE — main.js
//  Boot, canvas setup and overlay wiring (Block Blast home
//  structure: glossy letter logo, gold PLAY, coin chips).
//  Split from the original game.js without any gameplay
//  change.
// ============================================================

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

    // glossy letter-tile logo (STAR), Block Blast letter style
    buildLogo();

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
    refreshCoinsUI();
    draw();
}

// Build the STAR logo tiles with the game's block palette
function buildLogo() {
    var wrap = document.getElementById('logoTiles');
    var tiles = [
        ['S', COLORS[0]],
        ['T', COLORS[2]],
        ['A', COLORS[4]],
        ['R', COLORS[1]]
    ];
    for (var i = 0; i < tiles.length; i++) {
        var d = document.createElement('div');
        d.className = 'tile';
        d.textContent = tiles[i][0];
        d.style.background = 'linear-gradient(180deg,' + tiles[i][1] + ' 0%,' + tiles[i][1] + ' 55%, ' + tiles[i][1] + ' 100%)';
        wrap.appendChild(d);
    }
}

// -------------------- BOOT --------------------
window.onload = init;
