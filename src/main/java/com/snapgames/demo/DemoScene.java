package com.snapgames.demo;

import com.snapgames.demo.behaviors.EnemyBehavior;
import com.snapgames.demo.behaviors.PlayerBehavior;
import com.snapgames.platform.Platform2D;
import com.snapgames.platform.Platform2D.GameObject;
import com.snapgames.platform.Platform2D.ParticleSystem;
import com.snapgames.platform.Platform2D.Vec2d;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;

import static com.snapgames.platform.Platform2D.error;
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
    public void load(Platform2D app) {
        app.getSoundManager().loadSound("clic", "/assets/audio/clic.wav");
        app.getSoundManager().loadSound("tic", "/assets/audio/tic.wav");
        app.getSoundManager().loadSound("toc", "/assets/audio/toc.wav");
    }


    @Override
    public void create(Platform2D app) {
        Platform2D.Renderer renderer = app.getRenderer();
        // define the game World
        Platform2D.World world = new Platform2D.World(
                new Vec2d(0, 0.981),
                new Rectangle2D.Double(0, 0, 320, 200));
        app.setWorld(world);

        /*BufferedImage backgroundImg = ((BufferedImage) getResource("/assets/images/backgrounds/forest.jpg"));
        Platform2D.ImageObject background = (Platform2D.ImageObject) new Platform2D.ImageObject("background")
            .setImage(backgroundImg)
            .setStaticObject(true)
            .setPriority(-10);
        add(background);
        */

        // add a player object
        GameObject player = new GameObject(
                "player",
                world.getPlayArea().getWidth() * 0.5, world.getPlayArea().getHeight() * 0.5,
                16, 16)
                .setMaterial(new Platform2D.Material("player", 1.0, 0.30, 0.92))
                .addAttribute("energy", 100)
                .addAttribute("mana", 100)
                .addAttribute("lives", 3)
                .addAttribute("speed", 5.0)
                .setMass(80.0)
                .addBehavior(new PlayerBehavior());
        add(player);

        createHUD(app.getRenderer(), player);

        // Add some constraining object.
        Platform2D.ConstraintObject water = (Platform2D.ConstraintObject) new Platform2D.ConstraintObject(
                "water",
                0,
                world.getPlayArea().getHeight() * 0.70,
                world.getPlayArea().getWidth(),
                world.getPlayArea().getHeight() * 0.30)
                .setPriority(2)
                .setFillColor(new Color(0.2f, 0.2f, 0.7f, 0.4f))
                .setBorderColor(new Color(0.0f, 0.0f, 0.0f, 0.0f))
                .setMaterial(Platform2D.Material.WATER)
                .addForce(new Vec2d(0, -0.3));
        add(water);

        world.addConstrain(water);


        ParticleSystem ps = new ParticleSystem("ps01")
                .setMaxNbParticles(200)
                .setSize(getWorld().getPlayArea())
                .setFillColor(Color.BLACK)
                .setBorderColor(null)
                .setPriority(-10)
                .addBehavior(new Platform2D.ParticleBehavior<ParticleSystem>() {
                    @Override
                    public GameObject create(Platform2D.Scene s, GameObject parent) {
                        Color starColor = darkerColor(Color.WHITE, (int) (Math.random() * 15), 15);
                        return new GameObject(parent.getName() + "-" + parent.getNextIndex())
                                .setPosition(
                                        parent.width * Math.random(),
                                        parent.height * Math.random())
                                .setSize(0.5, 0.5)
                                .setBorderColor(starColor)
                                .setFillColor(starColor)
                                .setStaticObject(true);
                    }
                });
        add(ps);


        // add some enemies
        addEnemies(world, player, 10);

        setCamera(new Platform2D.Camera("cam01")
                .setTarget(player)
                .setTweenFactor(0.05)
                .setViewport(
                        new Rectangle2D.Double(0, 0,
                                renderer.getBufferSize().getWidth(),
                                renderer.getBufferSize().getHeight())))
        ;
        // play sound when ready
        app.getSoundManager().playSound("toc");

    }


    private Color darkerColor(Color fillColor, float intesity, float max) {
        float[] colors = new float[4];
        colors = fillColor.getRGBComponents(colors);

        return new Color(
                (colors[0] * (intesity / max)),
                (colors[1] * (intesity / max)),
                (colors[2] * (intesity / max)));
    }

    private void createHUD(Platform2D.Renderer renderer, GameObject player) {
        BufferedImage buffer = renderer.getDrawBuffer();
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
                .setPosition(renderer.getBufferSize().width - 32, 16)
                .setPriority(1)
                .setStaticObject(true)
                .setStickToCamera(true);

        Platform2D.TextObject lifes = (Platform2D.TextObject) new Platform2D.TextObject("lives")
                .setFont(buffer.createGraphics().getFont().deriveFont(12.0f))
                .setText("" + (player.attributes.get("lives")))
                .setShadowColor(new Color(0.2f, 0.2f, 0.2f, 0.8f))
                .setPosition(renderer.getBufferSize().width - 24, 20)
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
                GameObject player = getObject("player");
                addEnemies(getWorld(), player, nbEnemeisToAdd);
            }
            case KeyEvent.VK_G -> {
                // reverse Gravity
                Vec2d g = getWorld().getGravity();
                getWorld().setGravity(g.multiply(-1));
            }
            default -> {
            }

        }
    }

    private void addEnemies(Platform2D.World w, GameObject player, int nbEnemies) {

        for (int i = 0; i < nbEnemies; i++) {
            // add a player object
            GameObject enemy = new GameObject(
                    "enemy_" + i,
                    Math.random() * w.getPlayArea().getWidth(), Math.random() * w.getPlayArea().getHeight(),
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
