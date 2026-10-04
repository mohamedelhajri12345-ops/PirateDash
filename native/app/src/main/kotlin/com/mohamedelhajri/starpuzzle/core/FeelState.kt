package com.mohamedelhajri.starpuzzle.core

/**
 * Global feel switches. Set at boot from persisted settings.
 * motionOn=false → particles, shake and encouragement popups stay quiet
 * (accessibility / reduced motion), gameplay logic untouched.
 */
object FeelState {
    @Volatile var motionOn: Boolean = true
}
