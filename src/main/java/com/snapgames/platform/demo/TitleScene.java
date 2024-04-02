package com.snapgames.platform.demo;

import com.snapgames.platform.Platform2D;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;

import static com.snapgames.platform.Platform2D.getMessage;
import static com.snapgames.platform.Platform2D.getResource;

public class TitleScene extends Platform2D.AbstractScene {
    public TitleScene(Platform2D app) {
        super(app);
    }

    public void initialize(Platform2D app) {
        getResource("/assets/images/backgrounds/volcano.png");
    }

    @Override

    public void create(Platform2D app) {
        Graphics2D gb = app.getDrawBuffer().createGraphics();

        // add background Image
        BufferedImage bckImage = (BufferedImage) getResource("/assets/images/backgrounds/volcano.png");
        Platform2D.ImageObject backgroundIObj = (Platform2D.ImageObject) new Platform2D.ImageObject("background")
            .setImage(bckImage).setPriority(0);
        add(backgroundIObj);

        // add welcome message
        String welcomeTxt = getMessage("app.title.welcome");
        Font welcomeFont = gb.getFont().deriveFont(18.0f);
        gb.setFont(welcomeFont);
        int textWidth = gb.getFontMetrics().stringWidth(welcomeTxt);
        Platform2D.TextObject txtObject = (Platform2D.TextObject) new Platform2D.TextObject("welcome")
            .setText(welcomeTxt)
            .setShadowColor(Color.BLACK)
            .setFont(welcomeFont)
            .setPosition((app.getBufferSize().width - textWidth) * 0.5, app.getBufferSize().height * 0.5)
            .setBorderColor(Color.WHITE)
            .setPriority(1)
            .setStaticObject(true);
        add(txtObject);
    }


    @Override
    public void keyReleased(KeyEvent e) {
        super.keyReleased(e);
        switch (e.getKeyCode()) {
            case KeyEvent.VK_SPACE, KeyEvent.VK_ENTER -> {
                app.getSceneManager().activate("demo");
            }
            default -> {
                // Nothing to do there!
            }
        }
    }

    @Override
    public String getName() {
        return "title";
    }
}
