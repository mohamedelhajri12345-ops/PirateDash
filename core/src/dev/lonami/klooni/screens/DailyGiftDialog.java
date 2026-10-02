/*
    Star Puzzle — free customizable puzzle game for Android and Desktop
    Copyright (C) 2017-2019  Lonami Exo @ lonami.dev, GPL-3.0

    Daily gift dialog: a free coin reward every day.
    Coming back on consecutive days builds a streak that pays more.
*/
package dev.lonami.klooni.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Preferences;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.Dialog;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.utils.Align;

import java.text.SimpleDateFormat;
import java.util.Date;

import dev.lonami.klooni.Klooni;
import dev.lonami.klooni.SkinLoader;

// Daily gift dialog with streak tracking.
// Rewards: 10 coins on day 1, +10 per streak day, capped at 50.
public class DailyGiftDialog extends Dialog {

    //region Members

    private static final Preferences prefs = Gdx.app.getPreferences("dev.lonami.klooni.gift");

    private final Label messageLabel;
    private final TextButton claimButton;

    //endregion

    //region Constructor

    public DailyGiftDialog(final Skin skin) {
        super("DAILY GIFT", skin, "dialog");

        final Texture giftTexture = SkinLoader.loadPng("gift");
        final Image giftImage = new Image(giftTexture);
        getContentTable().add(giftImage).size(Gdx.graphics.getHeight() * 0.2f);

        messageLabel = new Label(currentStreakMessage(), skin);
        messageLabel.setAlignment(Align.center);
        getContentTable().row();
        getContentTable().add(messageLabel).padTop(12);

        claimButton = new TextButton("CLAIM", skin);
        claimButton.addListener(new com.badlogic.gdx.scenes.scene2d.utils.ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                final int reward = claim();
                messageLabel.setText("+" + reward + " coins!\nSee you tomorrow");
                claimButton.setText("DONE");
                claimButton.setDisabled(true);
            }
        });
        getButtonTable().add(claimButton);

        final TextButton closeButton = new TextButton("CLOSE", skin);
        closeButton.addListener(new com.badlogic.gdx.scenes.scene2d.utils.ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                hide();
            }
        });
        getButtonTable().add(closeButton).padLeft(16);

        if (!isGiftAvailable())
            claimButton.setDisabled(true);
    }

    //endregion

    //region Private methods

    private static String dayKey(final long offsetDays) {
        final Date d = new Date(System.currentTimeMillis() + offsetDays * 86400000L);
        return new SimpleDateFormat("yyyyMMdd").format(d);
    }

    private String currentStreakMessage() {
        if (!isGiftAvailable())
            return "Already claimed today";
        final int streak = nextStreak();
        final int reward = rewardForStreak(streak);
        if (streak > 1)
            return "Day " + streak + " in a row!\nClaim " + reward + " coins";
        return "Welcome back!\nClaim " + reward + " coins";
    }

    private static int nextStreak() {
        if (prefs.getString("giftDay", "").equals(dayKey(-1)))
            return prefs.getInteger("streak", 0) + 1;
        return 1;
    }

    private static int rewardForStreak(final int streak) {
        return Math.min(10 * streak, 50);
    }

    //endregion

    //region Static API

    // True if today's gift has not been claimed yet
    public static boolean isGiftAvailable() {
        return !prefs.getString("giftDay", "").equals(dayKey(0));
    }

    // Claims today's gift and pays the coins. Returns the reward paid.
    public static int claim() {
        if (!isGiftAvailable())
            return 0;

        final int streak = nextStreak();
        final int reward = rewardForStreak(streak);
        prefs.putInteger("streak", streak);
        prefs.putString("giftDay", dayKey(0));
        prefs.flush();

        Klooni.addMoney(reward);
        Klooni.playCoinSound();
        return reward;
    }

    //endregion
}
