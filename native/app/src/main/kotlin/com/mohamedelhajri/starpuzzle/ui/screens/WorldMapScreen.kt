package com.mohamedelhajri.starpuzzle.ui.screens

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.mohamedelhajri.starpuzzle.core.GameProgress
import com.mohamedelhajri.starpuzzle.core.LevelCatalog
import com.mohamedelhajri.starpuzzle.ui.screens.CosmosBackdrop

/**
 * World map: an adventure overview. Pick a world, see its 100 level nodes
 * with their states (locked / available / current / done with stars).
 */
@Composable
fun WorldMapScreen(
    progress: GameProgress,
    onBack: () -> Unit,
    onPickLevel: (Int) -> Unit
) {
    val firstUnfinished = progress.firstUnfinished()
    var selectedWorld by remember { mutableIntStateOf((firstUnfinished - 1) / 100 + 1) }

    Box(modifier = Modifier.fillMaxSize()) {
        // Phase B: the selected world's identity behind its map
        CosmosBackdrop(
            modifier = Modifier.fillMaxSize()
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.35f))
        ) {
        // ── Header ──
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back",
                    tint = MaterialTheme.colorScheme.onBackground)
            }
            Column {
                Text(
                    "WORLDS",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    "★ ${progress.totalStars()} stars",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        // ── World selector ──
        ScrollableTabRow(
            selectedTabIndex = selectedWorld - 1,
            edgePadding = 8.dp,
            containerColor = Color.Transparent,
            divider = {}
        ) {
            for (w in 1..LevelCatalog.TOTAL_WORLDS) {
                val unlocked = progress.worldUnlocked(w)
                Tab(
                    selected = selectedWorld == w,
                    onClick = { if (unlocked) selectedWorld = w },
                    enabled = unlocked,
                    text = {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                "W$w",
                                style = MaterialTheme.typography.titleMedium,
                                color = if (!unlocked) MaterialTheme.colorScheme.onSurfaceVariant
                                else if (selectedWorld == w) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurface
                            )
                            if (!unlocked) {
                                Text(
                                    "★${LevelCatalog.worldUnlockStars(w)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                )
            }
        }

        // ── World banner ──
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    LevelCatalog.worldName(selectedWorld),
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    "WORLD $selectedWorld",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // ── Level nodes ──
        LazyVerticalGrid(
            columns = GridCells.Fixed(5),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            val worldFirst = (selectedWorld - 1) * 100 + 1
            items((0 until 100).toList()) { idx ->
                val levelId = worldFirst + idx
                val unlocked = levelId <= firstUnfinished ||
                        progress.starsForLevel(levelId) > 0
                LevelNode(
                    levelId = levelId,
                    stars = progress.starsForLevel(levelId),
                    isCurrent = levelId == firstUnfinished,
                    onClick = { if (unlocked) onPickLevel(levelId) }
                )
            }
        }
    }
    }
}

@Composable
private fun LevelNode(
    levelId: Int,
    stars: Int,
    isCurrent: Boolean,
    onClick: () -> Unit
) {
    val done = stars > 0
    // Only the single current node runs a pulse — 100 idle infinite
    // transitions in the grid would saturate the animation clock.
    val scale = if (isCurrent) {
        val pulse = rememberInfiniteTransition(label = "nodePulse")
        val pulseScale by pulse.animateFloat(
            initialValue = 1f, targetValue = 1.08f,
            animationSpec = infiniteRepeatable(tween(800), RepeatMode.Reverse),
            label = "nodeScale"
        )
        pulseScale
    } else 1f

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .background(
                when {
                    done -> MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
                    isCurrent -> MaterialTheme.colorScheme.primary.copy(alpha = 0.28f)
                    else -> MaterialTheme.colorScheme.surface
                }
            )
            .border(
                width = if (isCurrent || done) 1.dp else 0.dp,
                color = MaterialTheme.colorScheme.primary.copy(
                    alpha = if (isCurrent) 0.8f else 0.4f),
                shape = RoundedCornerShape(14.dp)
            )
            .clickable { onClick() }
            .padding(vertical = 10.dp, horizontal = 4.dp)
    ) {
        Box(
            modifier = Modifier.size(34.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "$levelId",
                style = MaterialTheme.typography.titleMedium,
                color = if (done || isCurrent) MaterialTheme.colorScheme.onSurface
                else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.scale(scale)
            )
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(1.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.height(10.dp)
        ) {
            repeat(3) { i ->
                Icon(
                    Icons.Filled.Star,
                    contentDescription = null,
                    tint = if (i < stars) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier
                        .size(9.dp)
                        .alpha(0.4f.coerceAtLeast(if (i < stars) 1f else 0.4f))
                )
            }
        }
        }
}
