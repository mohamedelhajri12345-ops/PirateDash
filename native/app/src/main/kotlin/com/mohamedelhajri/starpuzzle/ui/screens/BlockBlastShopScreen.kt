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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.key
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.mohamedelhajri.starpuzzle.R
import com.mohamedelhajri.starpuzzle.audio.SoundManager
import com.mohamedelhajri.starpuzzle.core.BlockBlastMissions
import com.mohamedelhajri.starpuzzle.core.BlockBlastSpec

/**
 * SHOP — Block Blast-style store. Everything is FREE during the
 * testing phase (owner standing instruction). Block skins apply live
 * to the board palette; boosters are free helpers.
 */
@Composable
fun BlockBlastShopScreen(
    soundManager: SoundManager,
    onSkinSelected: (Int) -> Unit,
    onCellSkinSelected: (Int) -> Unit,
    onClearEffectSelected: (Int) -> Unit,
    onBack: () -> Unit
) {
    var selectedSkin by remember { mutableIntStateOf(BlockBlastSpec.activeSkinId) }
    var selectedCellSkin by remember { mutableIntStateOf(BlockBlastSpec.activeCellSkinId) }
    var selectedEffect by remember { mutableIntStateOf(BlockBlastSpec.activeClearEffectId) }
    val cellTextureRes = mapOf(
        1 to R.drawable.bb_cell_basic, 2 to R.drawable.bb_cell_bubble, 3 to R.drawable.bb_cell_bulb,
        4 to R.drawable.bb_cell_circle, 5 to R.drawable.bb_cell_drop, 6 to R.drawable.bb_cell_ghost,
        7 to R.drawable.bb_cell_grass, 8 to R.drawable.bb_cell_leaf, 9 to R.drawable.bb_cell_snowflake,
        10 to R.drawable.bb_cell_sun
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BbBackground)
            .padding(top = 14.dp, start = 18.dp, end = 18.dp, bottom = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "\u2190",
                color = BbTextSoft,
                fontSize = 26.sp,
                fontWeight = FontWeight.Black,
                modifier = Modifier.clickable {
                    soundManager.play(SoundManager.Sfx.BACK)
                    onBack()
                }
            )
            Spacer(Modifier.weight(1f))
            Text("SHOP", color = Color.White, fontWeight = FontWeight.Black, fontSize = 24.sp)
            Spacer(Modifier.weight(1f))
            Spacer(Modifier.width(26.dp))
        }

        Spacer(Modifier.height(16.dp))
        Text(
            "BLOCK SKINS",
            color = BbGold,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 15.sp,
            letterSpacing = 2.sp,
            modifier = Modifier.align(Alignment.Start)
        )
        Spacer(Modifier.height(10.dp))

        // skin cards — all FREE during testing
        BlockBlastSpec.SKINS.forEach { skin ->
            val isSelected = skin.id == selectedSkin
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 5.dp)
                    .background(BbPanel, RoundedCornerShape(14.dp))
                    .border(
                        2.dp,
                        if (isSelected) BbGold else Color.Transparent,
                        RoundedCornerShape(14.dp)
                    )
                    .clickable {
                        selectedSkin = skin.id
                        BlockBlastSpec.activeSkinId = skin.id
                        soundManager.play(SoundManager.Sfx.CONFIRM)
                        onSkinSelected(skin.id)
                    }
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // mini preview: 4 color chips
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    skin.colors.take(4).forEach { c ->
                        Box(
                            Modifier
                                .size(22.dp)
                                .background(Color(c.toLong() or 0xFF000000L), RoundedCornerShape(6.dp))
                        )
                    }
                }
                Spacer(Modifier.width(12.dp))
                Text(
                    skin.name,
                    color = Color.White,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 16.sp
                )
                Spacer(Modifier.weight(1f))
                Text(
                    if (isSelected) "SELECTED" else "FREE",
                    color = if (isSelected) BbGold else BbTextSoft,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 13.sp
                )
            }
        }

        // ---- CELL STYLE (reference package textures) ----
        Spacer(Modifier.height(20.dp))
        Text(
            "CELL STYLE",
            color = BbGold,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 15.sp,
            letterSpacing = 2.sp,
            modifier = Modifier.align(Alignment.Start)
        )
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            BlockBlastSpec.CELL_SKINS.take(6).forEach { skin ->
                val isSelected = skin.id == selectedCellSkin
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .background(
                            BbPanel,
                            RoundedCornerShape(12.dp)
                        )
                        .border(
                            2.dp,
                            if (isSelected) BbGold else Color.Transparent,
                            RoundedCornerShape(12.dp)
                        )
                        .clickable {
                            selectedCellSkin = skin.id
                            BlockBlastSpec.activeCellSkinId = skin.id
                            soundManager.play(SoundManager.Sfx.CONFIRM)
                            onCellSkinSelected(skin.id)
                        }
                        .padding(6.dp)
                ) {
                    if (skin.id == 0) {
                        Box(
                            Modifier
                                .size(34.dp)
                                .background(Color(0xFF4D7CFE), RoundedCornerShape(8.dp))
                        )
                    } else {
                        Image(
                            painter = painterResource(cellTextureRes[skin.id]!!),
                            contentDescription = skin.name,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.size(34.dp),
                            colorFilter = ColorFilter.tint(Color(0xFF4D7CFE), BlendMode.Multiply)
                        )
                    }
                    Text(skin.name, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
            BlockBlastSpec.CELL_SKINS.drop(6).forEach { skin ->
                val isSelected = skin.id == selectedCellSkin
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .background(BbPanel, RoundedCornerShape(12.dp))
                        .border(
                            2.dp,
                            if (isSelected) BbGold else Color.Transparent,
                            RoundedCornerShape(12.dp)
                        )
                        .clickable {
                            selectedCellSkin = skin.id
                            BlockBlastSpec.activeCellSkinId = skin.id
                            soundManager.play(SoundManager.Sfx.CONFIRM)
                            onCellSkinSelected(skin.id)
                        }
                        .padding(6.dp)
                ) {
                    Image(
                        painter = painterResource(cellTextureRes[skin.id]!!),
                        contentDescription = skin.name,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.size(34.dp),
                        colorFilter = ColorFilter.tint(Color(0xFF4D7CFE), BlendMode.Multiply)
                    )
                    Text(skin.name, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // ---- CLEAR EFFECT (reference package effect factories) ----
        Spacer(Modifier.height(20.dp))
        Text(
            "CLEAR EFFECT",
            color = BbGold,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 15.sp,
            letterSpacing = 2.sp,
            modifier = Modifier.align(Alignment.Start)
        )
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            BlockBlastSpec.CLEAR_EFFECTS.forEach { effect ->
                val isSelected = effect.id == selectedEffect
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .background(BbPanel, RoundedCornerShape(12.dp))
                        .border(
                            2.dp,
                            if (isSelected) BbGold else Color.Transparent,
                            RoundedCornerShape(12.dp)
                        )
                        .clickable {
                            selectedEffect = effect.id
                            BlockBlastSpec.activeClearEffectId = effect.id
                            soundManager.play(SoundManager.Sfx.CONFIRM)
                            onClearEffectSelected(effect.id)
                        }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        when (effect.id) {
                            0 -> "\uD83D\uDCA5"
                            1 -> "\uD83C\uDF00"
                            2 -> "\uD83D\uDCA7"
                            3 -> "\u2601\uFE0F"
                            else -> "\u2728"
                        },
                        fontSize = 18.sp
                    )
                    Text(effect.name, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(Modifier.height(20.dp))
        Text(
            "BOOSTERS",
            color = BbGold,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 15.sp,
            letterSpacing = 2.sp,
            modifier = Modifier.align(Alignment.Start)
        )
        Spacer(Modifier.height(10.dp))
        Text(
            "NEW PIECES — reroll the tray. Free & unlimited during the testing phase (button under the board).",
            color = BbTextSoft,
            fontSize = 13.sp
        )
    }
}

/**
 * MISSIONS — the three daily missions with live progress bars and
 * coin rewards (claim button on completion).
 */
@Composable
fun BlockBlastMissionsScreen(
    soundManager: SoundManager,
    onCoinsEarned: (Int) -> Unit,
    onBack: () -> Unit
) {
    val day = remember { BlockBlastMissions.today() }
    val missions = remember(day) { BlockBlastMissions.forDay(day) }
    var refresh by remember { mutableIntStateOf(0) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BbBackground)
            .padding(top = 14.dp, start = 18.dp, end = 18.dp, bottom = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "\u2190",
                color = BbTextSoft,
                fontSize = 26.sp,
                fontWeight = FontWeight.Black,
                modifier = Modifier.clickable {
                    soundManager.play(SoundManager.Sfx.BACK)
                    onBack()
                }
            )
            Spacer(Modifier.weight(1f))
            Text("MISSIONS", color = Color.White, fontWeight = FontWeight.Black, fontSize = 24.sp)
            Spacer(Modifier.weight(1f))
            Spacer(Modifier.width(26.dp))
        }

        Spacer(Modifier.height(16.dp))
        key(refresh) {
        missions.forEach { mission ->
            val progress = BlockBlastMissions.progress(mission, day).coerceAtMost(mission.target)
            val complete = progress >= mission.target
            val claimed = BlockBlastMissions.isClaimed(mission, day)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp)
                    .background(BbPanel, RoundedCornerShape(14.dp))
                    .padding(12.dp)
            ) {
                Text(mission.text, color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(10.dp)
                            .background(Color.Black.copy(alpha = 0.3f), RoundedCornerShape(5.dp))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(if (mission.target == 0) 0f else progress.toFloat() / mission.target)
                                .height(10.dp)
                                .background(BbGold, RoundedCornerShape(5.dp))
                        )
                    }
                    Spacer(Modifier.width(10.dp))
                    Text(
                        "$progress/${mission.target}",
                        color = BbTextSoft,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 13.sp
                    )
                }
                if (complete && !claimed) {
                    Spacer(Modifier.height(8.dp))
                    BbGoldPill("CLAIM +${mission.reward} COINS") {
                        val reward = BlockBlastMissions.claim(mission)
                        if (reward > 0) {
                            soundManager.play(SoundManager.Sfx.COIN)
                            onCoinsEarned(reward)
                        }
                        refresh++
                    }
                } else if (claimed) {
                    Spacer(Modifier.height(6.dp))
                    Text("\u2714 CLAIMED", color = BbGold, fontWeight = FontWeight.ExtraBold, fontSize = 13.sp)
                }
            }
        }
        }
    }
}
