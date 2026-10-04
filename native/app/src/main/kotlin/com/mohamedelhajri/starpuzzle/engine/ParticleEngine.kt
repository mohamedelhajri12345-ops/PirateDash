package com.mohamedelhajri.starpuzzle.engine

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.PI
import kotlin.math.max
import kotlin.math.min

/**
 * Supported visual particle events in Star Puzzle.
 */
enum class ParticleEvent {
    LINE_CLEAR,   // Block fragment burst per cleared cell
    MULTI_CLEAR,  // Expanding shockwave ring + radial starburst
    COMBO,        // Rising sparkle column with horizontal sway
    PIECE_LAND,   // Lateral dust puff at placement coordinates
    PIECE_PICK,   // Touch pulse / pickup feedback
    STAR_SPECIAL, // Golden starburst dispersion with rotation
    BOMB_BOOST    // Explosive radial fragment burst + blast flash ring
}

/**
 * High-performance, zero-allocation particle engine for Jetpack Compose Canvas.
 *
 * Grounded in strict mobile game performance principles:
 *  - Fixed memory pool of 200 particles (zero runtime allocations in tick/draw).
 *  - Deterministic pseudo-random number generator (LCG) seeded per session.
 *  - Screen shake math (decaying sine wave, capped at 6px).
 *  - Squash & stretch pulse physics for piece placement impact.
 *
 * Usage Example (LINE_CLEAR):
 * ```
 * // 1. Instantiate engine in GameScreen with level seed
 * val particleEngine = remember(levelId, restartKey) { ParticleEngine(seed = level.seed) }
 *
 * // 2. Trigger event when lines are cleared
 * for ((cx, cy) in event.clearedCells) {
 *     val cellX = boardOrigin.x + (cx + 0.5f) * boardPx
 *     val cellY = boardOrigin.y + (cy + 0.5f) * boardPx
 *     particleEngine.spawn(
 *         event = ParticleEvent.LINE_CLEAR,
 *         x = cellX,
 *         y = cellY,
 *         cellPx = boardPx,
 *         colors = listOf(pieceColor(colorIndex))
 *     )
 * }
 *
 * // 3. Update in frame loop
 * particleEngine.tick(dtMs)
 *
 * // 4. Render inside BoardCanvas DrawScope
 * particleEngine.draw(this)
 * ```
 */
class ParticleEngine(
    seed: Long = 1337L
) {
    companion object {
        const val MAX_PARTICLES = 200
        const val MAX_SHAKE_CAP_PX = 6.0f
    }

    /**
     * Pre-allocated particle object pool.
     */
    private class Particle {
        var active: Boolean = false
        var x: Float = 0f
        var y: Float = 0f
        var vx: Float = 0f
        var vy: Float = 0f
        var ax: Float = 0f
        var ay: Float = 0f
        var size: Float = 0f
        var endSize: Float = 0f
        var lifeMs: Float = 0f
        var maxLifeMs: Float = 0f
        var colorR: Float = 0f
        var colorG: Float = 0f
        var colorB: Float = 0f
        var startAlpha: Float = 1f
        var endAlpha: Float = 0f
        var rotationRad: Float = 0f
        var vRot: Float = 0f
        var shapeType: Int = SHAPE_CIRCLE
        var extra1: Float = 0f // Shockwave start radius / extra param
        var extra2: Float = 0f // Shockwave end radius / extra param

        fun reset() {
            active = false
            x = 0f; y = 0f
            vx = 0f; vy = 0f
            ax = 0f; ay = 0f
            size = 0f; endSize = 0f
            lifeMs = 0f; maxLifeMs = 0f
            colorR = 0f; colorG = 0f; colorB = 0f
            startAlpha = 1f; endAlpha = 0f
            rotationRad = 0f; vRot = 0f
            shapeType = SHAPE_CIRCLE
            extra1 = 0f; extra2 = 0f
        }

        companion object {
            const val SHAPE_CIRCLE = 0
            const val SHAPE_SQUARE = 1
            const val SHAPE_RING = 2
            const val SHAPE_STAR = 3
            const val SHAPE_SPARKLE = 4
        }
    }

    // Pool & allocation ring buffer
    private val pool = Array(MAX_PARTICLES) { Particle() }
    private var spawnIndex = 0
    private var activeParticleCount = 0

    // Deterministic LCG Pseudo-RNG (zero-allocation)
    private var rngSeed: Long = seed

    // Screen Shake state
    private var shakeDurationMs: Float = 0f
    private var shakeTimeMs: Float = 0f
    private var maxShakeIntensityPx: Float = 0f
    private var currentShakeX: Float = 0f
    private var currentShakeY: Float = 0f

    // Squash & Stretch state
    private var pulseDurationMs: Float = 0f
    private var pulseTimeMs: Float = 0f
    private var pulseAmplitude: Float = 0f
    private var currentScaleX: Float = 1f
    private var currentScaleY: Float = 1f

    // Pre-allocated Stroke cache for zero-allocation rendering in DrawScope
    private val strokeCache = Array(64) { i -> Stroke(width = (i + 1) * 0.5f) }

    private fun strokeForWidth(w: Float): Stroke {
        val idx = (w * 2f).toInt().coerceIn(0, 63)
        return strokeCache[idx]
    }

    /** Returns true if any particles are active or juice effects are running. */
    val isActive: Boolean
        get() = activeParticleCount > 0 ||
                shakeTimeMs < shakeDurationMs ||
                pulseTimeMs < pulseDurationMs

    /** Current screen shake horizontal offset in pixels (clamped to [-6, +6]). */
    val shakeOffsetX: Float get() = currentShakeX

    /** Current screen shake vertical offset in pixels (clamped to [-6, +6]). */
    val shakeOffsetY: Float get() = currentShakeY

    /** Current horizontal scale factor for squash & stretch pulse. */
    val scalePulseX: Float get() = currentScaleX

    /** Current vertical scale factor for squash & stretch pulse. */
    val scalePulseY: Float get() = currentScaleY

    /**
     * Resets the particle system, clearing all live particles and resetting juice states.
     */
    fun reset() {
        for (i in 0 until MAX_PARTICLES) {
            pool[i].reset()
        }
        spawnIndex = 0
        activeParticleCount = 0
        shakeTimeMs = 0f
        shakeDurationMs = 0f
        currentShakeX = 0f
        currentShakeY = 0f
        pulseTimeMs = 0f
        pulseDurationMs = 0f
        currentScaleX = 1f
        currentScaleY = 1f
    }

    /**
     * Sets the RNG seed for deterministic particle generation.
     */
    fun setSeed(seed: Long) {
        this.rngSeed = seed
    }

    // Deterministic random float [0.0, 1.0)
    private fun nextFloat(): Float {
        rngSeed = (rngSeed * 0x5DEECE66DL + 0xBL) and 0xFFFFFFFFFFFFL
        return ((rngSeed ushr 16) and 0xFFFFFFFFL).toFloat() / 4294967296f
    }

    // Deterministic random float [min, max)
    private fun nextFloat(minVal: Float, maxVal: Float): Float {
        return minVal + nextFloat() * (maxVal - minVal)
    }

    // Acquires next available particle slot from pool using ring-buffer recycling.
    private fun obtainParticle(): Particle {
        val p = pool[spawnIndex]
        spawnIndex = (spawnIndex + 1) % MAX_PARTICLES
        p.reset()
        return p
    }

    /**
     * Spawns particles for a given event at (x, y) with specified cell dimensions and colors.
     *
     * @param event The gameplay event triggering the particle burst.
     * @param x Canvas X coordinate (center of burst).
     * @param y Canvas Y coordinate (center of burst).
     * @param cellPx Pixel dimension of a single board cell (used for proportional sizing/speed).
     * @param colors Color palette provided for the event (e.g., color of cleared piece).
     */
    fun spawn(
        event: ParticleEvent,
        x: Float,
        y: Float,
        cellPx: Float = 60f,
        colors: List<Color> = emptyList()
    ) {
        val primaryColor = colors.getOrNull(0) ?: Color(0xFFFFD54F)
        val secondaryColor = colors.getOrNull(1) ?: Color.White

        when (event) {
            ParticleEvent.LINE_CLEAR -> spawnLineClear(x, y, cellPx, primaryColor)
            ParticleEvent.MULTI_CLEAR -> spawnMultiClear(x, y, cellPx, primaryColor)
            ParticleEvent.COMBO -> spawnCombo(x, y, cellPx, primaryColor)
            ParticleEvent.PIECE_LAND -> spawnPieceLand(x, y, cellPx, primaryColor)
            ParticleEvent.PIECE_PICK -> spawnPiecePick(x, y, cellPx, primaryColor)
            ParticleEvent.STAR_SPECIAL -> spawnStarSpecial(x, y, cellPx, primaryColor)
            ParticleEvent.BOMB_BOOST -> spawnBombBoost(x, y, cellPx, primaryColor)
        }
    }

    private fun spawnLineClear(x: Float, y: Float, cellPx: Float, color: Color) {
        val count = 14
        for (i in 0 until count) {
            val p = obtainParticle()
            p.active = true
            p.x = x + nextFloat(-cellPx * 0.3f, cellPx * 0.3f)
            p.y = y + nextFloat(-cellPx * 0.3f, cellPx * 0.3f)

            val angle = nextFloat(0f, 2f * PI.toFloat())
            val speed = nextFloat(cellPx * 2.5f, cellPx * 6.0f)
            p.vx = cos(angle) * speed
            p.vy = sin(angle) * speed
            p.ax = 0f
            p.ay = cellPx * 5.0f // gravity decay

            p.size = nextFloat(cellPx * 0.18f, cellPx * 0.32f)
            p.endSize = p.size * 0.2f
            p.lifeMs = 0f
            p.maxLifeMs = nextFloat(280f, 450f)

            p.colorR = color.red
            p.colorG = color.green
            p.colorB = color.blue
            p.startAlpha = 1.0f
            p.endAlpha = 0.0f

            p.rotationRad = nextFloat(0f, 2f * PI.toFloat())
            p.vRot = nextFloat(-8f, 8f)
            p.shapeType = if (i % 2 == 0) Particle.SHAPE_SQUARE else Particle.SHAPE_CIRCLE
        }
    }

    private fun spawnMultiClear(x: Float, y: Float, cellPx: Float, color: Color) {
        // Shockwave ring
        val ring = obtainParticle()
        ring.active = true
        ring.x = x
        ring.y = y
        ring.size = cellPx * 0.3f // stroke width
        ring.endSize = cellPx * 0.05f
        ring.lifeMs = 0f
        ring.maxLifeMs = 320f
        ring.colorR = 1.0f
        ring.colorG = 0.9f
        ring.colorB = 0.4f
        ring.startAlpha = 0.95f
        ring.endAlpha = 0.0f
        ring.shapeType = Particle.SHAPE_RING
        ring.extra1 = cellPx * 0.2f // start radius
        ring.extra2 = cellPx * 3.5f // end radius

        // Radial burst sparkles
        val count = 18
        for (i in 0 until count) {
            val p = obtainParticle()
            p.active = true
            p.x = x
            p.y = y
            val angle = (i.toFloat() / count) * 2f * PI.toFloat() + nextFloat(-0.1f, 0.1f)
            val speed = nextFloat(cellPx * 4f, cellPx * 8.5f)
            p.vx = cos(angle) * speed
            p.vy = sin(angle) * speed
            p.ax = -p.vx * 1.5f // high drag
            p.ay = -p.vy * 1.5f

            p.size = nextFloat(cellPx * 0.22f, cellPx * 0.4f)
            p.endSize = 0f
            p.lifeMs = 0f
            p.maxLifeMs = nextFloat(300f, 500f)

            p.colorR = 1.0f
            p.colorG = nextFloat(0.7f, 1.0f)
            p.colorB = nextFloat(0.2f, 0.5f)
            p.startAlpha = 1.0f
            p.endAlpha = 0.0f
            p.shapeType = Particle.SHAPE_STAR
            p.rotationRad = angle
            p.vRot = nextFloat(-5f, 5f)
        }

        triggerShake(intensityPx = 5.0f, durationMs = 260f)
    }

    private fun spawnCombo(x: Float, y: Float, cellPx: Float, color: Color) {
        val count = 16
        for (i in 0 until count) {
            val p = obtainParticle()
            p.active = true
            p.x = x + nextFloat(-cellPx * 1.5f, cellPx * 1.5f)
            p.y = y + nextFloat(-cellPx * 0.5f, cellPx * 0.5f)

            p.vx = nextFloat(-cellPx * 0.6f, cellPx * 0.6f)
            p.vy = -nextFloat(cellPx * 3.0f, cellPx * 6.0f) // rising column
            p.ax = 0f
            p.ay = -cellPx * 1.0f // upward acceleration

            p.size = nextFloat(cellPx * 0.2f, cellPx * 0.38f)
            p.endSize = 0f
            p.lifeMs = 0f
            p.maxLifeMs = nextFloat(450f, 750f)

            p.colorR = 1.0f
            p.colorG = nextFloat(0.75f, 0.95f)
            p.colorB = nextFloat(0.1f, 0.4f)
            p.startAlpha = 1.0f
            p.endAlpha = 0.0f
            p.shapeType = Particle.SHAPE_SPARKLE
            p.rotationRad = nextFloat(0f, 2f * PI.toFloat())
            p.vRot = nextFloat(-4f, 4f)
        }

        triggerShake(intensityPx = 3.0f, durationMs = 180f)
    }

    private fun spawnPieceLand(x: Float, y: Float, cellPx: Float, color: Color) {
        val count = 8
        for (i in 0 until count) {
            val p = obtainParticle()
            p.active = true
            p.x = x + nextFloat(-cellPx * 0.4f, cellPx * 0.4f)
            p.y = y + cellPx * 0.3f // bottom of piece

            val direction = if (i % 2 == 0) -1f else 1f
            p.vx = direction * nextFloat(cellPx * 0.8f, cellPx * 2.2f)
            p.vy = -nextFloat(cellPx * 0.2f, cellPx * 0.8f) // slight lift
            p.ax = -p.vx * 2f // quick stop
            p.ay = cellPx * 1.5f

            p.size = nextFloat(cellPx * 0.12f, cellPx * 0.22f)
            p.endSize = p.size * 1.4f // expand puff
            p.lifeMs = 0f
            p.maxLifeMs = nextFloat(160f, 260f)

            p.colorR = 0.9f
            p.colorG = 0.92f
            p.colorB = 0.98f
            p.startAlpha = 0.6f
            p.endAlpha = 0.0f
            p.shapeType = Particle.SHAPE_CIRCLE
        }

        triggerSquashStretch(amplitude = 0.12f, durationMs = 200f)
    }

    private fun spawnPiecePick(x: Float, y: Float, cellPx: Float, color: Color) {
        val count = 6
        for (i in 0 until count) {
            val p = obtainParticle()
            p.active = true
            val angle = (i.toFloat() / count) * 2f * PI.toFloat()
            val radius = cellPx * 0.5f
            p.x = x + cos(angle) * radius
            p.y = y + sin(angle) * radius

            p.vx = cos(angle) * cellPx * 1.2f
            p.vy = sin(angle) * cellPx * 1.2f
            p.ax = 0f
            p.ay = 0f

            p.size = cellPx * 0.18f
            p.endSize = 0f
            p.lifeMs = 0f
            p.maxLifeMs = 150f

            p.colorR = 1.0f
            p.colorG = 0.85f
            p.colorB = 0.3f
            p.startAlpha = 0.8f
            p.endAlpha = 0.0f
            p.shapeType = Particle.SHAPE_CIRCLE
        }
    }

    private fun spawnStarSpecial(x: Float, y: Float, cellPx: Float, color: Color) {
        val count = 18
        for (i in 0 until count) {
            val p = obtainParticle()
            p.active = true
            p.x = x
            p.y = y

            val angle = nextFloat(0f, 2f * PI.toFloat())
            val speed = nextFloat(cellPx * 2.0f, cellPx * 6.5f)
            p.vx = cos(angle) * speed
            p.vy = sin(angle) * speed
            p.ax = 0f
            p.ay = cellPx * 2.5f // gentle gravity drop

            p.size = nextFloat(cellPx * 0.25f, cellPx * 0.45f)
            p.endSize = p.size * 0.1f
            p.lifeMs = 0f
            p.maxLifeMs = nextFloat(400f, 700f)

            p.colorR = 1.0f
            p.colorG = nextFloat(0.8f, 1.0f)
            p.colorB = nextFloat(0.2f, 0.5f)
            p.startAlpha = 1.0f
            p.endAlpha = 0.0f
            p.rotationRad = nextFloat(0f, 2f * PI.toFloat())
            p.vRot = nextFloat(-10f, 10f)
            p.shapeType = Particle.SHAPE_STAR
        }
    }

    private fun spawnBombBoost(x: Float, y: Float, cellPx: Float, color: Color) {
        // Explosive shockwave ring
        val ring = obtainParticle()
        ring.active = true
        ring.x = x
        ring.y = y
        ring.size = cellPx * 0.4f
        ring.endSize = cellPx * 0.05f
        ring.lifeMs = 0f
        ring.maxLifeMs = 300f
        ring.colorR = 1.0f
        ring.colorG = 0.4f
        ring.colorB = 0.2f
        ring.startAlpha = 1.0f
        ring.endAlpha = 0.0f
        ring.shapeType = Particle.SHAPE_RING
        ring.extra1 = cellPx * 0.2f
        ring.extra2 = cellPx * 4.5f

        // Debris fragment burst
        val count = 22
        for (i in 0 until count) {
            val p = obtainParticle()
            p.active = true
            p.x = x + nextFloat(-cellPx * 0.2f, cellPx * 0.2f)
            p.y = y + nextFloat(-cellPx * 0.2f, cellPx * 0.2f)

            val angle = nextFloat(0f, 2f * PI.toFloat())
            val speed = nextFloat(cellPx * 5.0f, cellPx * 11.0f)
            p.vx = cos(angle) * speed
            p.vy = sin(angle) * speed
            p.ax = -p.vx * 1.8f // strong atmospheric drag
            p.ay = cellPx * 4.0f

            p.size = nextFloat(cellPx * 0.2f, cellPx * 0.42f)
            p.endSize = 0f
            p.lifeMs = 0f
            p.maxLifeMs = nextFloat(320f, 580f)

            p.colorR = nextFloat(0.85f, 1.0f)
            p.colorG = nextFloat(0.2f, 0.6f)
            p.colorB = nextFloat(0.0f, 0.2f)
            p.startAlpha = 1.0f
            p.endAlpha = 0.0f
            p.rotationRad = nextFloat(0f, 2f * PI.toFloat())
            p.vRot = nextFloat(-12f, 12f)
            p.shapeType = if (i % 2 == 0) Particle.SHAPE_SQUARE else Particle.SHAPE_CIRCLE
        }

        triggerShake(intensityPx = 6.0f, durationMs = 320f)
    }

    /**
     * Triggers screen shake with [intensityPx] (clamped to max 6px) decaying over [durationMs].
     */
    fun triggerShake(intensityPx: Float = 6.0f, durationMs: Float = 250f) {
        this.maxShakeIntensityPx = intensityPx.coerceAtMost(MAX_SHAKE_CAP_PX)
        this.shakeDurationMs = max(10f, durationMs)
        this.shakeTimeMs = 0f
    }

    /**
     * Triggers squash-stretch scale pulse over [durationMs].
     */
    fun triggerSquashStretch(amplitude: Float = 0.12f, durationMs: Float = 200f) {
        this.pulseAmplitude = amplitude.coerceIn(0.02f, 0.35f)
        this.pulseDurationMs = max(10f, durationMs)
        this.pulseTimeMs = 0f
    }

    /**
     * Advances the simulation by [dtMs] milliseconds. Zero allocations.
     */
    fun tick(dtMs: Float) {
        if (dtMs <= 0f) return
        val dtSec = dtMs / 1000f

        // 1. Update particles
        var activeCount = 0
        for (i in 0 until MAX_PARTICLES) {
            val p = pool[i]
            if (!p.active) continue

            p.lifeMs += dtMs
            if (p.lifeMs >= p.maxLifeMs) {
                p.active = false
                continue
            }

            p.vx += p.ax * dtSec
            p.vy += p.ay * dtSec
            p.x += p.vx * dtSec
            p.y += p.vy * dtSec
            p.rotationRad += p.vRot * dtSec

            activeCount++
        }
        this.activeParticleCount = activeCount

        // 2. Update screen shake
        if (shakeTimeMs < shakeDurationMs) {
            shakeTimeMs += dtMs
            if (shakeTimeMs >= shakeDurationMs) {
                shakeTimeMs = shakeDurationMs
                currentShakeX = 0f
                currentShakeY = 0f
            } else {
                val progress = shakeTimeMs / shakeDurationMs
                val envelope = (1f - progress) * (1f - progress) // quadratic decay
                val intensity = min(MAX_SHAKE_CAP_PX, maxShakeIntensityPx * envelope)

                val angleX = (shakeTimeMs / 1000f) * 2f * PI.toFloat() * 24f
                val angleY = (shakeTimeMs / 1000f) * 2f * PI.toFloat() * 19f
                currentShakeX = (sin(angleX) * intensity).coerceIn(-MAX_SHAKE_CAP_PX, MAX_SHAKE_CAP_PX)
                currentShakeY = (cos(angleY) * intensity).coerceIn(-MAX_SHAKE_CAP_PX, MAX_SHAKE_CAP_PX)
            }
        } else {
            currentShakeX = 0f
            currentShakeY = 0f
        }

        // 3. Update squash & stretch
        if (pulseTimeMs < pulseDurationMs) {
            pulseTimeMs += dtMs
            if (pulseTimeMs >= pulseDurationMs) {
                pulseTimeMs = pulseDurationMs
                currentScaleX = 1f
                currentScaleY = 1f
            } else {
                val progress = pulseTimeMs / pulseDurationMs
                val envelope = 1f - progress
                val wave = sin(progress * PI.toFloat() * 3f) * envelope * pulseAmplitude
                currentScaleX = 1f + wave
                currentScaleY = 1f - wave * 0.8f
            }
        } else {
            currentScaleX = 1f
            currentScaleY = 1f
        }
    }

    /**
     * Renders all live particles onto the provided Compose [DrawScope]. Zero allocations.
     */
    fun draw(drawScope: DrawScope) {
        if (activeParticleCount == 0) return

        for (i in 0 until MAX_PARTICLES) {
            val p = pool[i]
            if (!p.active) continue

            val progress = (p.lifeMs / p.maxLifeMs).coerceIn(0f, 1f)
            val currentSize = p.size + (p.endSize - p.size) * progress
            val currentAlpha = (p.startAlpha + (p.endAlpha - p.startAlpha) * progress).coerceIn(0f, 1f)

            if (currentAlpha <= 0f || currentSize <= 0f) continue

            val particleColor = Color(
                red = p.colorR,
                green = p.colorG,
                blue = p.colorB,
                alpha = currentAlpha
            )

            when (p.shapeType) {
                Particle.SHAPE_CIRCLE -> {
                    drawScope.drawCircle(
                        color = particleColor,
                        radius = currentSize * 0.5f,
                        center = Offset(p.x, p.y)
                    )
                }
                Particle.SHAPE_SQUARE -> {
                    val halfSize = currentSize * 0.5f
                    val corner = CornerRadius(currentSize * 0.2f)
                    if (p.rotationRad == 0f) {
                        drawScope.drawRoundRect(
                            color = particleColor,
                            topLeft = Offset(p.x - halfSize, p.y - halfSize),
                            size = Size(currentSize, currentSize),
                            cornerRadius = corner
                        )
                    } else {
                        drawScope.withTransform({
                            rotate(
                                degrees = (p.rotationRad * 180f / PI.toFloat()),
                                pivot = Offset(p.x, p.y)
                            )
                        }) {
                            drawRoundRect(
                                color = particleColor,
                                topLeft = Offset(p.x - halfSize, p.y - halfSize),
                                size = Size(currentSize, currentSize),
                                cornerRadius = corner
                            )
                        }
                    }
                }
                Particle.SHAPE_RING -> {
                    val ringRadius = p.extra1 + (p.extra2 - p.extra1) * progress
                    val strokeW = (currentSize * (1f - progress)).coerceAtLeast(1f)
                    if (ringRadius > 0f) {
                        drawScope.drawCircle(
                            color = particleColor,
                            radius = ringRadius,
                            center = Offset(p.x, p.y),
                            style = strokeForWidth(strokeW)
                        )
                    }
                }
                Particle.SHAPE_STAR -> {
                    val hSize = currentSize * 0.5f
                    val strokeW = (currentSize * 0.25f).coerceAtLeast(1.5f)
                    val strokeObj = strokeForWidth(strokeW)
                    drawScope.withTransform({
                        if (p.rotationRad != 0f) {
                            rotate(
                                degrees = (p.rotationRad * 180f / PI.toFloat()),
                                pivot = Offset(p.x, p.y)
                            )
                        }
                    }) {
                        drawLine(
                            color = particleColor,
                            start = Offset(p.x, p.y - hSize),
                            end = Offset(p.x, p.y + hSize),
                            strokeWidth = strokeObj.width
                        )
                        drawLine(
                            color = particleColor,
                            start = Offset(p.x - hSize, p.y),
                            end = Offset(p.x + hSize, p.y),
                            strokeWidth = strokeObj.width
                        )
                    }
                }
                Particle.SHAPE_SPARKLE -> {
                    val hSize = currentSize * 0.5f
                    drawScope.drawCircle(
                        color = particleColor,
                        radius = hSize * 0.4f,
                        center = Offset(p.x, p.y)
                    )
                    drawScope.drawLine(
                        color = particleColor,
                        start = Offset(p.x - hSize, p.y),
                        end = Offset(p.x + hSize, p.y),
                        strokeWidth = 2.0f
                    )
                    drawScope.drawLine(
                        color = particleColor,
                        start = Offset(p.x, p.y - hSize),
                        end = Offset(p.x, p.y + hSize),
                        strokeWidth = 2.0f
                    )
                }
            }
        }
    }
}
