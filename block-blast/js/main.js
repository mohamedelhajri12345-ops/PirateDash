// ============================================================
//  STAR PUZZLE — main.js
//  Boot, canvas setup and overlay wiring. Split from the
//  original game.js without any behavior change.
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

// -------------------- BOOT --------------------
window.onload = init;
