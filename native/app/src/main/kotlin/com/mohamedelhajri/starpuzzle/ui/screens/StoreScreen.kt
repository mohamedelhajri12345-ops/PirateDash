package com.mohamedelhajri.starpuzzle.ui.screens

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
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.PaddingValues
import com.mohamedelhajri.starpuzzle.audio.SoundManager
import com.mohamedelhajri.starpuzzle.core.GameProgress
import com.mohamedelhajri.starpuzzle.ui.theme.MaterialSkinCatalog
import com.mohamedelhajri.starpuzzle.ui.theme.PieceSkins
import com.mohamedelhajri.starpuzzle.ui.theme.drawMaterial
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import com.mohamedelhajri.starpuzzle.ui.theme.SkinState
import com.mohamedelhajri.starpuzzle.ui.theme.skinPrice

// ── The approved professional store: in-game coins ONLY ──────────────
// No real money, no stars — everything priced in the coins players earn.

private val StoreGold = Color(0xFFFFD54F)
private val CardNavy = Color(0xFF16204A)

@Composable
fun StoreScreen(
    progress: GameProgress,
    sound: SoundManager,
    onBack: () -> Unit
) {
    var refresh by remember { mutableIntStateOf(0) }
    val coins = androidx.compose.runtime.remember(refresh) { progress.coins }
    val inv = androidx.compose.runtime.remember(refresh) { progress.boosters() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF0B1026), Color(0xFF1B2C5C), Color(0xFF0B1026))
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(8.dp))

            // top bar: coin balance + close
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .border(1.dp, StoreGold.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
                        .background(StoreGold.copy(alpha = 0.08f), RoundedCornerShape(20.dp))
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Text(
                        "● $coins",
                        style = MaterialTheme.typography.titleMedium,
                        color = StoreGold
                    )
                }
                Spacer(Modifier.weight(1f))
                OutlinedButton(
                    onClick = { sound.play(SoundManager.Sfx.BACK); onBack() },
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.onSurface
                    ),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 4.dp)
                ) { Text("CLOSE") }
            }

            Spacer(Modifier.height(20.dp))
            Text(
                "SHOP",
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(Modifier.height(18.dp))

            // ── boosters: 2×2 grid of cards ──
            val cards = listOf(
                "BOMB" to GameProgress.BoosterKind.BOMB,
                "ZAP" to GameProgress.BoosterKind.LIGHTNING,
                "STAR" to GameProgress.BoosterKind.STAR,
                "MOVE" to GameProgress.BoosterKind.MOVE
            )
            for (row in 0 until 2) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    for (col in 0 until 2) {
                        val (label, kind) = cards[row * 2 + col]
                        val owned = when (kind) {
                            GameProgress.BoosterKind.BOMB -> inv.bomb
                            GameProgress.BoosterKind.LIGHTNING -> inv.lightning
                            GameProgress.BoosterKind.STAR -> inv.star
                            GameProgress.BoosterKind.MOVE -> inv.move
                        }
                        val price = progress.boosterPrice(kind)
                        BoosterCard(
                            label, owned, price,
                            affordable = coins >= price,
                            onBuy = {
                                if (progress.buyBooster(kind)) {
                                    sound.play(SoundManager.Sfx.COIN)
                                    refresh++
                                } else {
                                    sound.play(SoundManager.Sfx.INVALID)
                                }
                            }
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
            }

            Spacer(Modifier.height(16.dp))
            Text(
                "THEMES",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(10.dp))

            // ── skins: owned = selectable, otherwise buy with coins ──
            val owned = remember(refresh) { progress.ownedSkins() }
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                items(PieceSkins.size) { i ->
                    val skin = PieceSkins[i]
                    val isOwned = i in owned
                    val isSelected = SkinState.active == i
                    val price = skinPrice(i)
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.clickable {
                            if (isOwned) {
                                SkinState.active = i
                                progress.saveSkin(i)
                                sound.play(SoundManager.Sfx.CONFIRM)
                                refresh++
                            } else if (progress.buySkin(i)) {
                                SkinState.active = i
                                progress.saveSkin(i)
                                sound.play(SoundManager.Sfx.COIN)
                                refresh++
                            } else {
                                sound.play(SoundManager.Sfx.INVALID)
                            }
                        }.padding(vertical = 4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .border(
                                    if (isSelected) 2.dp else 1.dp,
                                    if (isSelected) StoreGold
                                    else Color.White.copy(alpha = 0.25f),
                                    CircleShape
                                )
                                .padding(4.dp)
                                .background(skin.colors[2], CircleShape)
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            if (isOwned) skin.name else "$price ●",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isSelected) StoreGold
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(Modifier.height(20.dp))
            Text(
                "MATERIAL SKINS",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(10.dp))

            // ── v3.1: REAL material skins — ice, fire, gems, wood, candy,
            // chrome. Every cell is drawn with gloss, bevel and shadow. ──
            LazyRow(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                items(MaterialSkinCatalog.premiumSkins) { skin ->
                    val isOwned = skin.id in owned
                    val isSelected = SkinState.active == skin.id
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.clickable {
                            if (isOwned) {
                                SkinState.active = skin.id
                                progress.saveSkin(skin.id)
                                sound.play(SoundManager.Sfx.CONFIRM)
                                refresh++
                            } else if (progress.buySkinAt(skin.id, skin.price)) {
                                SkinState.active = skin.id
                                progress.saveSkin(skin.id)
                                sound.play(SoundManager.Sfx.COIN)
                                refresh++
                            } else {
                                sound.play(SoundManager.Sfx.INVALID)
                            }
                        }.padding(vertical = 4.dp)
                    ) {
                        Canvas(Modifier.size(66.dp)) {
                            val cs = size.width / 3.4f
                            for (k in 0 until 3) {
                                drawMaterial(
                                    topLeft = Offset(
                                        k * (cs + size.width / 26f) + size.width / 14f,
                                        size.height / 2f - cs / 2f
                                    ),
                                    size = Size(cs, cs),
                                    spec = skin.materials[k * 3 + 1]
                                )
                            }
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "★ EPIC",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFFB388FF)
                        )
                        Text(
                            skin.name,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isSelected) StoreGold
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            if (isOwned) (if (isSelected) "ACTIVE" else "OWNED")
                            else "${skin.price} ●",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isSelected) StoreGold
                            else if (isOwned) Color(0xFF66BB6A)
                            else Color(0xFF9FA8CC)
                        )
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun BoosterCard(
    label: String,
    owned: Int,
    price: Int,
    affordable: Boolean,
    onBuy: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .size(150.dp, 132.dp)
            .border(1.dp, StoreGold.copy(alpha = 0.35f), RoundedCornerShape(18.dp))
            .background(CardNavy, RoundedCornerShape(18.dp))
            .clickable { onBuy() }
            .padding(10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(54.dp)
                .background(StoreGold.copy(alpha = 0.12f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                when (label) {
                    "BOMB" -> "◎"
                    "ZAP" -> "⚡"
                    "STAR" -> "★"
                    else -> "↺"
                },
                style = MaterialTheme.typography.headlineMedium,
                color = StoreGold
            )
        }
        Spacer(Modifier.height(8.dp))
        Text(
            when (label) {
                "BOMB" -> "Bomb 3×3"
                "ZAP" -> "Line Zap"
                "STAR" -> "Star Swap"
                else -> "Move Back"
            },
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            "owned ×$owned",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(6.dp))
        Box(
            modifier = Modifier
                .border(
                    1.dp,
                    if (affordable) StoreGold else Color(0xFF9FA8CC).copy(alpha = 0.4f),
                    RoundedCornerShape(14.dp)
                )
                .padding(horizontal = 12.dp, vertical = 3.dp)
        ) {
            Text(
                "$price ●",
                style = MaterialTheme.typography.labelMedium,
                color = if (affordable) StoreGold else Color(0xFF9FA8CC).copy(alpha = 0.6f)
            )
        }
    }
}
