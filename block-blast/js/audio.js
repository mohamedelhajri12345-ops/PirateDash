// ============================================================
//  STAR PUZZLE — audio.js
//  Sound pool + audio manager using the reference sound set
//  (assets/audio). Split from the original game.js without
//  any behavior change.
// ============================================================

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
