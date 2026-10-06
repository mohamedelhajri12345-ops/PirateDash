# Star Puzzle — Block Blast-based game package

IMPORTANT (owner direction, Oct 6 2026): the player experience stays
EXACTLY as the owner's original file (block-blast-clone). The look now
matches the REAL Block Blast (colors and structure measured from its
official Google Play screenshots): flat navy #242C54 background,
board panel #2A3260, empty cells #1E264A, glossy 3D-bevel blocks,
big white score, wide gold PLAY button, coin chips on every screen
(+5 coins per cleared line). Only the brand (Star Puzzle) differs.

The owner-provided Block Blast reference package, split into clean
modules with the project structure preserved. Gameplay, game feel,
sounds and visuals are an exact copy of the reference; only the
colors and the brand (name/logo) are changed to Star Puzzle.

## Structure

```
block-blast/
  index.html          # shell: score bar, canvas, menu + game-over overlays
  style.css           # styling (Star Puzzle brand colors)
  js/
    config.js         # board geometry, 8-color palette, 26-shape catalog
    audio.js          # SoundPool + audio manager (reference sound set)
    board.js          # grid state, spawning, placement, line clears, game over
    render.js         # canvas drawing: grid, glossy blocks, drop preview
    input.js          # pointer + touch: drag, snap preview, drop
    main.js           # boot, canvas setup, overlay wiring
  assets/audio/       # reference sound set (see LICENSES.md)
    bgm.mp3  place.mp3  clear.mp3  combo.mp3  gameover.mp3  click.mp3
```

## Modules load order (plain scripts, no bundler)

config → audio → board → render → input → main

## Gameplay rules (identical to the reference)

- 8x8 grid, 3-piece tray, refill when all three are placed.
- Score: +1 per placed cell, +16 per cleared line (8 cells x 2).
- Combo sound when 2+ lines clear at once.
- Coins: +5 per cleared line, shown on menu, game and game-over screens
  (additive reward UI; core mechanics untouched).
- Game over when no tray piece fits anywhere.
- Drag grows the piece to full size and centers it above the finger,
  drop snaps with a preview ghost.
