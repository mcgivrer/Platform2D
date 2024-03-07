package com.snapgames.platform;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

import javax.swing.JFrame;

/**
 * The {@link Platform2D} class base of the platform framework.
 *
 * @author Frédéric Delorme
 * @since 1.0.0
 */
public class Platform2D implements KeyListener, ComponentListener {
    public static final int FPS = 60;

    /**
     * The {@link Vec2d} class is a 2 dimensional vector.
     *
     * @author Frédéric Delorme
     * @since 1.0.0
     */
    public static class Vec2d {
        public double x, y;

        public Vec2d(double x, double y) {
            this.x = x;
            this.y = y;
        }

        public Vec2d add(Vec2d v) {
            return new Vec2d(this.x + v.x, this.y + v.y);
        }

        public Vec2d multiply(double m) {
            return new Vec2d(this.x * m, this.y * m);
        }

        public Vec2d sub(Vec2d v) {
            return new Vec2d(this.x - v.x, this.y - v.y);
        }
    }

    /**
     * The {@link Material} class to be used by the physic computation.
     *
     * @author Frédéric Delorme
     * @since 1.0.0
     */
    public static class Material {

        public static Material DEFAULT = new Material("default", 1.0, 0.70, 0.98);

        public String name;
        public double density;
        public double elasticity;
        public double friction;

        public Material(String name, double d, double e, double f) {
            this.name = name;
            this.density = d;
            this.elasticity = e;
            this.friction = f;
        }
    }

    /**
     * The {@link GameObject} entity to be moved and animated.
     *
     * @author Frédéric Delorme
     * @since 1.0.0
     */
    public static class GameObject extends Rectangle2D.Double {
        private static long index = 0;
        private long id = index++;
        private String name = "gameobject_" + id;
        public double dx, dy;
        public double ax, ay;

        public List<Vec2d> forces = new ArrayList<>();

        public Material material = Material.DEFAULT;

        public Color borderColor = Color.WHITE;
        public Color fillColor = Color.RED;

        public Map<String, Object> attributes = new HashMap<>();
        public boolean contact;
        public boolean staticObject = false;
        public int priority = 1;

        public GameObject(String name) {
            super();
            this.name = name;
        }

        public GameObject(String name, double x, double y, double w, double h) {
            super(x, y, w, h);
            this.name = name;
        }

        public long getId() {
            return id;
        }

        public String getName() {
            return name;
        }
    }

    /**
     * A {@link World} entity to host gravity and play area and the world containing objects.
     *
     * @author Frédéric Delorme
     * @since 1.0.0
     */
    public static class World {
        private Vec2d gravity;
        private Rectangle2D playArea;
        private List<GameObject> constrains = new ArrayList<>();

        public World(Vec2d gravity, Rectangle2D playAreaSize) {
            this.gravity = gravity;
            this.playArea = playAreaSize;
        }

        public void addConstrain(GameObject constrain) {
            constrain.staticObject = true;
            constrains.add(constrain);
        }

        public Vec2d getGravity() {
            return gravity;
        }

        public Rectangle2D getPlayArea() {
            return playArea;
        }

        public List<GameObject> getConstrains() {
            return constrains;
        }

    }

    private static ResourceBundle messages = ResourceBundle.getBundle("i18n.messages");
    private static Properties config = new Properties();

    BufferedImage buffer;
    JFrame frame;

    private String name;
    private Dimension bufferSize;
    private Dimension screenSize;

    private boolean exit;

    private boolean[] keys = new boolean[1024];

    private List<GameObject> objects = new ArrayList<>();
    private Map<String, GameObject> objectMap = new HashMap<>();

    private World world;

    public Platform2D(String appName, Dimension bufferSize, Dimension screenSize) {
        this.name = appName;
        this.bufferSize = bufferSize;
        this.screenSize = screenSize;
    }

    private void initialize(String[] args) {
        loadConfiguration("/config.properties");

        buffer = new BufferedImage(bufferSize.width, bufferSize.height, BufferedImage.TYPE_INT_ARGB);
        frame = new JFrame("Platform2D");

        frame.setPreferredSize(screenSize);
        frame.setMaximumSize(screenSize);
        frame.setMinimumSize(screenSize);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.addComponentListener(this);
        frame.addKeyListener(this);
        frame.pack();
        frame.setVisible(true);
        frame.createBufferStrategy(3);
        create();
    }

    /**
     * Load configuration file.
     *
     * @param configPathFile the properties file.
     */
    private void loadConfiguration(String configPathFile) {
        try {
            config.load(Platform2D.class.getResourceAsStream(configPathFile));
            parseArguments(config);
        } catch (IOException e) {
            error("Unable to load file %s", configPathFile);
        }
    }

    private void parseArguments(Properties props) {
        props.entrySet().forEach((e) -> {
            switch ((String) e.getKey()) {
                case "app.screen.size" -> {
                    String[] values = ((String) e.getValue()).split("x");
                    bufferSize = new Dimension(Integer.parseInt(values[0]), Integer.parseInt(values[1]));
                }
                case "app.window.size" -> {
                    String[] values = ((String) e.getValue()).split("x");
                    screenSize = new Dimension(Integer.parseInt(values[0]), Integer.parseInt(values[1]));
                }
                default -> {
                    warn("Value entry unknown %s=%s", e.getKey(), e.getValue());
                }
            }
        });
    }

    private void create() {
        // define the game World
        world = new World(new Vec2d(0, 0.0981), new Rectangle2D.Double(0, 0, 320, 200));

        // add a player object
        GameObject player = new GameObject(
                "player",
                bufferSize.width >> 1, bufferSize.height >> 1,
                16, 16);
        player.material = new Material("player", 1.0, 0.30, 0.92);
        player.attributes.put("energy", 100);
        player.attributes.put("mana", 100);
        player.attributes.put("live", 3);
        addGameObject(player);

        // Add a constrain
        GameObject water = new GameObject("water",
                0, world.getPlayArea().getHeight() * 0.70,
                world.getPlayArea().getWidth(),
                world.getPlayArea().getHeight() * 0.30);
        water.priority = 2;
        water.fillColor = new Color(0.2f, 0.2f, 0.7f, 0.4f);
        water.borderColor = new Color(0.0f, 0.0f, 0.0f, 0.0f);
        water.forces.add(new Vec2d(0, -0.5));
        world.constrains.add(water);
        addGameObject(water);

        // add some enemies
        for (int i = 0; i < 10; i++) {
            // add a player object
            GameObject enemy = new GameObject(
                    "enemy_" + i,
                    Math.random() * bufferSize.width, Math.random() * bufferSize.height,
                    8, 8);
            enemy.material = new Material("enemy", 0.7, 0.80, 0.99);
            enemy.fillColor = Color.BLUE;
            enemy.borderColor = Color.DARK_GRAY;
            enemy.attributes.put("energy", 100);
            enemy.attributes.put("mana", 100);
            enemy.priority = 10 + i;
            addGameObject(enemy);
        }

    }

    private void addGameObject(GameObject gameObject) {
        objects.add(gameObject);
        objectMap.put(gameObject.getName(), gameObject);
    }

    private void run(String[] args) {
        initialize(args);
        loop();
        dispose();
    }

    private void loop() {
        long currentTime = System.currentTimeMillis();
        long previous = currentTime;
        double elapse = 0;
        while (!exit) {
            currentTime = System.currentTimeMillis();
            elapse = currentTime - previous < (1000 / FPS) ? currentTime - previous : 16;
            input();
            update(elapse);
            draw();
            previous = currentTime;
            try {
                Thread.sleep(1000 / FPS);
            } catch (InterruptedException e) {
                error("Unable to sleep for 16ms%n");
            }
        }
    }

    private void input() {
        GameObject player = objectMap.get("player");
        double speed = (double) player.attributes.getOrDefault("speed", 0.5);
        if (isKeyPressed(KeyEvent.VK_UP)) {
            player.forces.add(new Vec2d(0, -speed));
        }
        if (isKeyPressed(KeyEvent.VK_DOWN)) {
            player.forces.add(new Vec2d(0, speed));
        }
        if (isKeyPressed(KeyEvent.VK_LEFT)) {
            player.forces.add(new Vec2d(-speed, 0));
        }
        if (isKeyPressed(KeyEvent.VK_RIGHT)) {
            player.forces.add(new Vec2d(speed, 0));
        }

        if (isKeyPressed(KeyEvent.VK_X)) {

        }

    }

    private void update(double elapse) {
        objects.stream()
                .filter(o -> !o.staticObject)
                .sorted((a, b) -> Integer.compare(b.priority, a.priority))
                .forEach(o -> {
                    // compute acceleration applied to the GameObject o
                    o.ax = 0;
                    o.ay = 0;

                    // apply gravity
                    o.forces.add(world.getGravity());

                    // compute resulting acceleration
                    for (Vec2d v : o.forces) {
                        o.ax += v.x;
                        o.ay += v.y;
                    }

                    // if o is under some world constrain
                    for (GameObject c : world.constrains) {
                        if (c.contains(o)) {
                            for (Vec2d v : c.forces) {
                                o.ax += v.x;
                                o.ay += v.y;
                            }
                        }
                    }
                    // compute resulting speed
                    o.dx = o.dx + o.ax * elapse * 0.005;
                    o.dy = o.dy + o.ay * elapse * 0.005;

                    // get the GameObject o position
                    o.x += o.dx * elapse;
                    o.y += o.dy * elapse;

                    // apply friction "force" to the velocity
                    o.dx *= o.material.friction;
                    o.dy *= o.material.friction;

                    // Constrains the Gameobject o into the play area.
                    Rectangle2D playArea = world.getPlayArea();
                    if (playArea.intersects(o) || !playArea.contains(o)) {
                        if (o.x < 0) {
                            o.x = 0;
                            o.dx *= -o.material.elasticity;
                            o.contact = true;
                        }
                        if (o.x > playArea.getWidth() - o.width) {
                            o.x = playArea.getWidth() - o.width;
                            o.dx *= -o.material.elasticity;
                            o.contact = true;
                        }
                        if (o.y < 0) {
                            o.y = 0;
                            o.dy *= -o.material.elasticity;
                            o.contact = true;
                        }
                        if (o.y > playArea.getHeight() - o.width) {
                            o.y = playArea.getHeight() - o.height;
                            o.dy *= -o.material.elasticity;
                            o.contact = true;
                        }
                    }
                    o.forces.clear();
                });
    }

    private void draw() {
        Graphics2D gb = buffer.createGraphics();
        // clear buffer
        gb.setBackground(Color.BLACK);
        gb.clearRect(0, 0, 640, 400);

        // draw all the platforme game scene.
        objects.forEach(o -> {
            gb.setColor(o.fillColor);
            gb.fill(o);
            gb.setColor(o.borderColor);
            gb.draw(o);
        });
        gb.dispose();

        // draw to screen.
        Graphics2D g = (Graphics2D) frame.getBufferStrategy().getDrawGraphics();
        g.drawImage(buffer,
                0, 0, screenSize.width, screenSize.height,
                0, 0, bufferSize.width, bufferSize.height,
                null);
        g.dispose();
        frame.getBufferStrategy().show();
    }

    private void dispose() {
        frame.dispose();
    }

    /**
     * ---- key management ----
     */

    @Override
    public void keyTyped(KeyEvent e) {
    }

    @Override
    public void keyPressed(KeyEvent e) {
        keys[e.getKeyCode()] = true;
    }

    @Override
    public void keyReleased(KeyEvent e) {
        keys[e.getKeyCode()] = false;

        switch (e.getKeyCode()) {
            case KeyEvent.VK_ESCAPE -> {
                exit = true;
            }
            default -> {
                // Nothing to do !
            }
        }

    }

    @Override
    public void componentResized(ComponentEvent e) {
        screenSize = e.getComponent().getSize();
    }

    @Override
    public void componentMoved(ComponentEvent e) {

    }

    @Override
    public void componentShown(ComponentEvent e) {

    }

    @Override
    public void componentHidden(ComponentEvent e) {

    }


    public static void error(String message, Object... args) {
        log("ERROR", message, args);
    }

    public static void info(String message, Object... args) {
        log("INFO", message, args);
    }

    public static void warn(String message, Object... args) {
        log("WARN", message, args);
    }

    public static void log(String level, String message, Object... args) {
        String date = DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(LocalDateTime.now());
        System.out.printf(date + "|" + level.toUpperCase() + "|" + message + "%n", args);
    }

    public boolean isKeyPressed(int keyCode) {
        return keys[keyCode];
    }

    /**
     * ---- Main entry ----
     */
    public static void main(String[] args) {
        Platform2D platform2d = new Platform2D(
                "Platform2D",
                new Dimension(320, 200),
                new Dimension(640, 400));
        platform2d.run(args);
    }
}
