package com.snapgames.platform;

import javax.swing.*;
import java.awt.*;
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
import java.util.List;

/**
 * The {@link Platform2D} class base of the platform framework.
 *
 * @author Frédéric Delorme
 * @since 1.0.0
 */
public class Platform2D extends JPanel implements KeyListener, ComponentListener {
    public static final int FPS = 60;
    public static final double PHYSIC_TIME_FACTOR = 0.005;
    private int debug = 0;

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
        public boolean active = true;
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

        public double lifespan = -1;
        public double timer = 0;

        public GameObject() {
            super();
        }

        public GameObject(String name) {
            super();
            this.name = name;
        }

        public GameObject(String name, double x, double y, double w, double h) {
            super(x, y, w, h);
            this.name = name;
        }

        public GameObject setLifespan(int d) {
            this.lifespan = d;
            return this;
        }

        public GameObject setPosition(double x, double y) {
            this.x = x;
            this.y = y;
            return this;
        }

        public GameObject setVelocity(double dx, double dy) {
            this.dx = dx;
            this.dy = dy;
            return this;
        }

        public GameObject setAcceleration(double ax, double ay) {
            this.ax = ax;
            this.ay = ay;
            return this;
        }

        public long getId() {
            return id;
        }

        public String getName() {
            return name;
        }

        public GameObject setFillColor(Color fill) {
            this.fillColor = fill;
            return this;
        }

        public GameObject setBorderColor(Color border) {
            this.borderColor = border;
            return this;
        }

        public GameObject setPriority(int p) {
            this.priority = p;
            return this;
        }

        public GameObject setStaticObject(boolean staticObject) {
            this.staticObject = staticObject;
            return this;
        }

        public GameObject addForce(Vec2d f) {
            forces.add(f);
            return this;
        }

        public GameObject setMaterial(Material m) {
            this.material = m;
            return this;
        }

        public GameObject addAttribute(String attrName, Object attrValue) {
            attributes.put(attrName, attrValue);
            return this;
        }
    }

    /**
     * {@link TextObject} bring a new text capability to the {@link GameObject} to be displayed and managed.
     * A bunch of new attributes :
     * <ul>
     *     <li>`text` supports {@link String} text,</li>
     *     <li>`font` is defining the text font to used on rendering time,</li>
     *     <li>`shadowColor` if exists will draw a slightly offset shadow behind the text.</li>
     * </ul>
     * <p>
     * The existing `fillColor` is used to draw the text itself,
     * and the `borderColor` is used to draw a border around the text.
     */
    public static class TextObject extends GameObject {
        public Color shadowColor = Color.BLACK;
        String text;
        private Font font;

        public TextObject(String name) {
            super(name);
        }

        TextObject setText(String text) {
            this.text = text;
            return this;
        }

        public TextObject setFont(Font font) {
            this.font = font;
            return this;
        }

        public GameObject setShadowColor(Color shadow) {
            this.shadowColor = shadow;
            return this;
        }
    }

    /**
     * The `{@link ConstrainObject}` is a specific object bringing physical contains into the {@link World},
     * to let influence existing other {@link GameObject} and inheritance with constrained forces.
     *
     * @author Frédéric Delorme
     * @since 1.0.0
     */
    public static class ConstrainObject extends GameObject {

        public ConstrainObject(String name) {
            super(name);
            this.staticObject = true;
        }

        public ConstrainObject(String name, double x, double y, double w, double h) {
            super(name);
            setRect(x, y, w, h);
        }

        public ConstrainObject setForce(Vec2d f) {
            forces.clear();
            forces.add(f);
            return this;
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
        private List<ConstrainObject> constrains = new ArrayList<>();

        public World(Vec2d gravity, Rectangle2D playAreaSize) {
            this.gravity = gravity;
            this.playArea = playAreaSize;
        }

        public void addConstrain(ConstrainObject constrain) {
            constrain.staticObject = true;
            constrains.add(constrain);
        }

        public Vec2d getGravity() {
            return gravity;
        }

        public Rectangle2D getPlayArea() {
            return playArea;
        }

        public List<ConstrainObject> getConstrains() {
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
        frame = new JFrame(messages.getString("app.title"));
        setPreferredSize(screenSize);
        setMaximumSize(screenSize);
        setMinimumSize(screenSize);
        frame.setContentPane(this);
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
            16, 16)
            .setMaterial(new Material("player", 1.0, 0.30, 0.92))
            .addAttribute("energy", 100)
            .addAttribute("mana", 100)
            .addAttribute("lives", 3);
        addGameObject(player);

        TextObject score = (TextObject) new TextObject("score")
            .setFont(buffer.createGraphics().getFont().deriveFont(18.0f))
            .setText("000000")
            .setShadowColor(new Color(0.2f, 0.2f, 0.2f, 0.8f))
            .setPosition(10, 32)
            .setFillColor(Color.WHITE)
            .setBorderColor(Color.BLACK)
            .setPriority(1)
            .setStaticObject(true);
        addGameObject(score);

        TextObject heart = (TextObject) new TextObject("heart")
            .setFont(buffer.createGraphics().getFont().deriveFont(14.0f))
            .setText("❤")
            .setShadowColor(new Color(0.2f, 0.2f, 0.2f, 0.8f))
            .setPosition(bufferSize.width - 40, 32)
            .setFillColor(Color.RED)
            .setBorderColor(Color.BLACK)
            .setPriority(1)
            .setStaticObject(true);

        TextObject lifes = (TextObject) new TextObject("lives")
            .setFont(buffer.createGraphics().getFont().deriveFont(18.0f))
            .setText("" + (player.attributes.get("lives")))
            .setShadowColor(new Color(0.2f, 0.2f, 0.2f, 0.8f))
            .setPosition(bufferSize.width - 30, 32)
            .setFillColor(Color.WHITE)
            .setBorderColor(Color.BLACK)
            .setPriority(2)
            .setStaticObject(true);
        addGameObject(heart);
        addGameObject(lifes);

        // Add some constraining object.
        ConstrainObject water = (ConstrainObject) new ConstrainObject("water",
            0,
            world.getPlayArea().getHeight() * 0.70,
            world.getPlayArea().getWidth(),
            world.getPlayArea().getHeight() * 0.30)
            .setPriority(2)
            .setFillColor(new Color(0.2f, 0.2f, 0.7f, 0.4f))
            .setBorderColor(new Color(0.0f, 0.0f, 0.0f, 0.0f))
            .addForce(new Vec2d(0, -0.5));
        addGameObject(water);

        world.addConstrain(water);

        // add some enemies
        for (int i = 0; i < 10; i++) {
            // add a player object
            GameObject enemy = new GameObject(
                "enemy_" + i,
                Math.random() * bufferSize.width, Math.random() * bufferSize.height,
                8, 8)
                .setMaterial(new Material("enemy", 0.7, 0.80, 0.99))
                .setFillColor(Color.BLUE)
                .setBorderColor(Color.DARK_GRAY)
                .addAttribute("energy", 100)
                .addAttribute("mana", 100)
                .setPriority(10 + i);
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
            .filter(o -> !o.staticObject && o.active)
            .sorted((a, b) -> Integer.compare(b.priority, a.priority))
            .forEach(o -> {
                applyWorldConstrains(world, o);
                // compute acceleration applied to the GameObject o
                o.ax = 0;
                o.ay = 0;
                // compute resulting acceleration
                o.forces.forEach(v -> {
                    o.ax += v.x;
                    o.ay += v.y;
                });

                // compute resulting speed
                o.dx += (o.ax * elapse * PHYSIC_TIME_FACTOR);
                o.dy += (o.ay * elapse * PHYSIC_TIME_FACTOR);

                // get the GameObject o position
                o.x += o.dx * elapse;
                o.y += o.dy * elapse;

                // apply friction "force" to the velocity
                o.dx *= o.material.friction;
                o.dy *= o.material.friction;

                // Compute lifespan & Duration.
                if (o.lifespan != -1) {
                    o.timer += elapse;
                    if (o.timer > o.lifespan) {
                        o.active = false;
                    }
                }
                keepGameObjectIntoPlayArea(world, o);
                o.forces.clear();

            });

    }

    private void keepGameObjectIntoPlayArea(World world, GameObject o) {
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
    }

    private void applyWorldConstrains(World world, GameObject go) {
        // apply gravity
        go.forces.add(world.getGravity());
        // if o is under some world constrain
        for (GameObject c : world.constrains) {
            if (c.contains(go)) {
                for (Vec2d v : c.forces) {
                    go.ax += v.x;
                    go.ay += v.y;
                }
            }
        }
    }

    private void draw() {
        Graphics2D gb = buffer.createGraphics();
        // set Antialiasing mode
        gb.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        gb.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        // clear buffer
        gb.setBackground(Color.BLACK);
        gb.clearRect(0, 0, 640, 400);

        // draw all the platforme game scene.
        objects.forEach(o -> {
            switch (o.getClass().getSimpleName()) {
                case "GameObject" -> {
                    gb.setColor(o.fillColor);
                    gb.fill(o);
                    gb.setColor(o.borderColor);
                    gb.draw(o);
                }
                case "TextObject" -> {
                    TextObject to = (TextObject) o;
                    if (to.text != null) {
                        gb.setFont(to.font);
                        for (int ix = -1; ix < 1; ix++) {
                            for (int iy = -1; iy < 1; iy++) {
                                gb.setColor(to.borderColor);
                                gb.drawString(to.text, (int) to.x + ix, (int) to.y + iy);
                            }
                        }
                        gb.setColor(to.shadowColor);
                        gb.drawString(to.text, (int) to.x + 2, (int) to.y + 2);
                        gb.setColor(to.fillColor);
                        gb.drawString(to.text, (int) to.x, (int) to.y);
                    }
                }
                default -> {
                    error("No renderering for %s", o.getClass().getSimpleName());
                }
            }
            if (debug > 3) {
                // show all applied forces
                for (Vec2d f : o.forces) {
                    gb.setColor(Color.BLUE);
                    gb.drawLine((int) (o.x + (o.width * 0.5)), (int) (o.y + (o.height * 0.5)),
                        (int) ((o.x + (o.width * 0.5)) + f.x * 100.0), (int) ((o.y + (o.height * 0.5)) + f.y * 100.0));
                }
                gb.setColor(Color.ORANGE);
                gb.drawLine((int) (o.x + (o.width * 0.5)), (int) (o.y + (o.height * 0.5)),
                    (int) ((o.x + (o.width * 0.5)) + o.dx * 100.0), (int) ((o.y + (o.height * 0.5)) + o.dy * 100.0));
                gb.setColor(Color.YELLOW);
                gb.drawLine((int) (o.x + (o.width * 0.5)), (int) (o.y + (o.height * 0.5)),
                    (int) ((o.x + (o.width * 0.5)) + o.ax * 100.0), (int) ((o.y + (o.height * 0.5)) + o.ay * 100.0));

            }
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
