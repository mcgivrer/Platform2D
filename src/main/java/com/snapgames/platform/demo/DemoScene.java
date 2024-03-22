package com.snapgames.platform.demo;

import com.snapgames.platform.Platform2D;
import com.snapgames.platform.Platform2D.GameObject;
import com.snapgames.platform.demo.behaviors.EnemyBehavior;
import com.snapgames.platform.demo.behaviors.PlayerBehavior;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;

import static com.snapgames.platform.Platform2D.getResource;

public class DemoScene extends Platform2D.AbstractScene {

    public DemoScene(Platform2D app) {
        super(app);
    }

    public String getName() {
        return "demo";
    }

    @Override
    public void initialize(Platform2D app) {
        BufferedImage backgroundImg = ((BufferedImage) getResource("/assets/images/backgrounds/forest.jpg"));
    }

    @Override
    public void create(Platform2D app) {
        // define the game World
        Platform2D.World world = new Platform2D.World(new Platform2D.Vec2d(0, 0.0981), new Rectangle2D.Double(0, 0, 320, 200));
        app.setWorld(world);

        BufferedImage backgroundImg = ((BufferedImage) getResource("/assets/images/backgrounds/forest.jpg"));

        Platform2D.ImageObject background = (Platform2D.ImageObject) new Platform2D.ImageObject("background")
                .setImage(backgroundImg)
                .setStaticObject(true)
                .setPriority(-10);
        add(background);

        // add a player object
        GameObject player = new GameObject(
                "player",
                app.getBufferSize().width >> 1, app.getBufferSize().height >> 1,
                16, 16)
                .setMaterial(new Platform2D.Material("player", 1.0, 0.30, 0.92))
                .addAttribute("energy", 100)
                .addAttribute("mana", 100)
                .addAttribute("lives", 3)
                .setMass(80.0)
                .addBehavior(new PlayerBehavior());
        add(player);

        createHUD(app, player);

        // Add some constraining object.
        Platform2D.ConstraintObject water = (Platform2D.ConstraintObject) new Platform2D.ConstraintObject("water",
                0,
                world.getPlayArea().getHeight() * 0.70,
                world.getPlayArea().getWidth(),
                world.getPlayArea().getHeight() * 0.30)
                .setPriority(2)
                .setFillColor(new Color(0.2f, 0.2f, 0.7f, 0.4f))
                .setBorderColor(new Color(0.0f, 0.0f, 0.0f, 0.0f))
                .addForce(new Platform2D.Vec2d(0, -0.3));
        add(water);

        world.addConstrain(water);

        // add some enemies
        addEnemies(app,  player, 10);

        setCamera(new Platform2D.Camera("cam01")
                .setTarget(player)
                .setTweenFactor(0.05)
                .setViewport(new Rectangle2D.Double(0, 0, app.getBufferSize().getWidth(), app.getBufferSize().getHeight())));

    }


    private void createHUD(Platform2D app, GameObject player) {
        BufferedImage buffer = app.getDrawBuffer();
        Platform2D.TextObject score = (Platform2D.TextObject) new Platform2D.TextObject("score")
                .setFont(buffer.createGraphics().getFont().deriveFont(18.0f))
                .setText("000000")
                .setShadowColor(new Color(0.2f, 0.2f, 0.2f, 0.8f))
                .setPosition(8, 16)
                .setFillColor(Color.WHITE)
                .setBorderColor(Color.BLACK)
                .setPriority(1)
                .setStaticObject(true)
                .setStickToCamera(true);
        add(score);

        BufferedImage heartImage = ((BufferedImage) getResource("/assets/images/tiles01.png|0,96,16,16"));
        Platform2D.ImageObject heart = (Platform2D.ImageObject) new Platform2D.ImageObject("heart")
                .setImage(heartImage)
                .setPosition(app.getBufferSize().width - 32, 16)
                .setPriority(1)
                .setStaticObject(true)
                .setStickToCamera(true);

        Platform2D.TextObject lifes = (Platform2D.TextObject) new Platform2D.TextObject("lives")
                .setFont(buffer.createGraphics().getFont().deriveFont(12.0f))
                .setText("" + (player.attributes.get("lives")))
                .setShadowColor(new Color(0.2f, 0.2f, 0.2f, 0.8f))
                .setPosition(app.getBufferSize().width - 24, 20)
                .setFillColor(Color.WHITE)
                .setBorderColor(Color.BLACK)
                .setPriority(2)
                .setStaticObject(true)
                .setStickToCamera(true);
        add(heart);
        add(lifes);
    }

    @Override
    public void input(Platform2D app) {
    }

    @Override
    public void keyReleased(KeyEvent e) {
        switch (e.getKeyCode()) {
            case KeyEvent.VK_PAGE_UP -> {
                int nbEnemeisToAdd = 10;
                if (e.isControlDown()) {
                    nbEnemeisToAdd *= 5;
                }
                if (e.isShiftDown()) {
                    nbEnemeisToAdd *= 10;
                }
            }
        }
    }

    private void addEnemies(Platform2D app, GameObject go, int nbEnemies) {
        Platform2D.Scene scn = app.getSceneManager().getActive();
        GameObject player = scn.getObject("player");

        for (int i = 0; i < nbEnemies; i++) {
            // add a player object
            GameObject enemy = new GameObject(
                    "enemy_" + i,
                    Math.random() * app.getBufferSize().width, Math.random() * app.getBufferSize().height,
                    8, 8)
                    .setMaterial(new Platform2D.Material("enemy", 0.7, 0.80, 0.99))
                    .setFillColor(Color.BLUE)
                    .setBorderColor(Color.DARK_GRAY)
                    .addAttribute("energy", 100)
                    .addAttribute("mana", 100)
                    .setPriority(10 + i)
                    .setMass(10.0)
                    .addBehavior(new EnemyBehavior(player));
            add(enemy);
        }
    }
}
