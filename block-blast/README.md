# Star Puzzle — Block Blast-based game package

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
- Game over when no tray piece fits anywhere.
- Drag grows the piece to full size and centers it above the finger,
  drop snaps with a preview ghost.
