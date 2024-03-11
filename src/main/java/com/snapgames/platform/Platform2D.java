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
import java.util.List;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

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
    private String debugFilter = null;

    private boolean drawFlag = true;
    private boolean updateFlag = true;

    /**
     * The {@link Vec2d} class is a 2-dimension vector.
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
        private long id = ++index;
        private String name = "gameobject_" + id;
        public double dx, dy;
        public double ax, ay;

        public List<Vec2d> forces = new ArrayList<>();

        public double mass = 1.0;

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

        public GameObject setMaterial(Material mat) {
            this.material = mat;
            return this;
        }

        public GameObject setMass(double m) {
            this.mass = m;
            return this;
        }

        public GameObject addAttribute(String attrName, Object attrValue) {
            attributes.put(attrName, attrValue);
            return this;
        }

        public void update(double elapsed) {
            // Compute lifespan & Duration.
            if (lifespan != -1) {
                timer += elapsed;
                if (timer > lifespan) {
                    active = false;
                }
            }
        }

        public boolean isActive() {
            return active;
        }

        public boolean isObjectStatic() {
            return staticObject;
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

        public void update(double elapsed) {
            super.update(elapsed);
        }

        public void draw(Graphics2D g) {
            Rectangle2D bounds = g.getFontMetrics().getStringBounds(this.text, g);
            this.width = bounds.getWidth();
            this.height = bounds.getHeight();
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

    private final boolean[] keys = new boolean[1024];

    private final List<GameObject> objects = new CopyOnWriteArrayList<>();
    private final Map<String, GameObject> objectMap = new ConcurrentHashMap<>();

    Map<String, Object> stats = new HashMap<>();

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

        createScene();
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
        props.forEach((key, value) -> {
            switch ((String) key) {
                case "app.screen.size" -> {
                    String[] values = ((String) value).split("x");
                    bufferSize = new Dimension(Integer.parseInt(values[0]), Integer.parseInt(values[1]));
                }
                case "app.window.size" -> {
                    String[] values = ((String) value).split("x");
                    screenSize = new Dimension(Integer.parseInt(values[0]), Integer.parseInt(values[1]));
                }
                case "app.debug.level" -> {
                    debug = Integer.parseInt((String) value);
                }
                case "app.debug.filter" -> {
                    debugFilter = (String) value;
                }
                default -> {
                    warn("Value entry unknown %s=%s", key, value);
                }
            }
        });
    }

    private void createScene() {
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
            .addAttribute("lives", 3)
            .setMass(80.0);
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
            .addForce(new Vec2d(0, -0.3));
        addGameObject(water);

        world.addConstrain(water);

        // add some enemies
        addEnemies(10);

    }

    private void addEnemies(int nbEnemies) {
        for (int i = 0; i < nbEnemies; i++) {
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
                .setPriority(10 + i)
                .setMass(10.0);
            addGameObject(enemy);
        }
    }

    private void addGameObject(GameObject gameObject) {
        objects.add(gameObject);
        objectMap.put(gameObject.getName(), gameObject);
        // update statistics data
        long countActive = objects.stream()
            .filter(GameObject::isActive).count();
        long countStatic = objects.stream()
            .filter(GameObject::isObjectStatic).count();
        stats.put("3:active", countActive);
        stats.put("2:static", countStatic);
        stats.put("1:obj", objects.size());
        objects.sort((a, b) -> Integer.compare(a.priority, b.priority));
    }

    private void run(String[] args) {
        initialize(args);
        loop();
        dispose();
    }

    private void loop() {
        long frames = 0, framesPerSec = 0;
        long updates = 0, updatesPerSec = 0;
        long cumulatedTime = 0;
        long gameTime = 0;
        long currentTime = System.currentTimeMillis();
        long previous = currentTime;
        double elapsed = 0;
        while (!exit) {
            currentTime = System.currentTimeMillis();
            elapsed = currentTime - previous < (1000 / FPS) ? currentTime - previous : 16;
            input();
            if (updateFlag) {
                update(elapsed);
                updates += 1;
            }
            cumulatedTime += elapsed;
            if (cumulatedTime > 1000) {
                gameTime += cumulatedTime;
                framesPerSec = frames;
                updatesPerSec = updates;
                cumulatedTime = 0;
                frames = 0;
                updates = 0;
            }
            if (drawFlag) {
                frames += 1;
                draw(stats);

            }
            previous = currentTime;
            try {
                Thread.sleep(1000 / FPS);
            } catch (InterruptedException e) {
                error("Unable to sleep for 16ms%n");
            }
            stats.put("0:debug", debug);
            stats.put("4:fps", framesPerSec);
            stats.put("5:ups", updatesPerSec);
            stats.put("6:time", formatDuration(gameTime));

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
                // compute acceleration applied to the GameObject o
                o.ax = 0;
                o.ay = 0;

                // apply world constrains
                applyWorldConstrains(world, o, elapse);

                // add applied forces on acceleration
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

                o.update(elapse);
                keepGameObjectIntoPlayArea(world, o);
                o.forces.clear();

            });

    }

    private void keepGameObjectIntoPlayArea(World world, GameObject o) {
        // Constrains the GameObject o into the play area.
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

    private void applyWorldConstrains(World world, GameObject go, double elapsed) {
        // apply gravity
        go.forces.add(world.getGravity());
        // if o is under some world constrain
        for (GameObject c : world.constrains) {
            if (c.contains(go) || go.intersects(c)) {
                for (Vec2d v : c.forces) {
                    Rectangle2D penetration = c.createIntersection(go);
                    go.ax += v.x * (penetration.getWidth() / (go.width * go.mass)) * elapsed;
                    go.ay += v.y * ((penetration.getWidth() * 0.75) / (go.width * go.mass)) * elapsed;
                }
            }
        }
    }

    private void draw(Map<String, Object> stats) {
        Graphics2D gb = buffer.createGraphics();
        // set Antialiasing mode
        gb.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        gb.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        // clear buffer
        gb.setBackground(Color.BLACK);
        gb.clearRect(0, 0, 640, 400);

        // draw all the platform game's scene.
        objects.forEach(o -> {
            switch (o.getClass().getSimpleName()) {
                case "GameObject" -> {
                    gb.setColor(o.fillColor);
                    gb.fill(o);
                    gb.setColor(o.borderColor);
                    gb.draw(o);
                }
                case "ConstrainObject" -> {
                    gb.setColor(o.fillColor);
                    gb.fill(o);
                }
                case "TextObject" -> {
                    TextObject to = (TextObject) o;
                    int offFontY = gb.getFontMetrics(to.font).getAscent();
                    if (to.text != null) {
                        gb.setFont(to.font);
                        to.draw(gb);
                        for (int ix = -1; ix < 1; ix++) {
                            for (int iy = -1; iy < 1; iy++) {
                                gb.setColor(to.borderColor);
                                gb.drawString(to.text, (int) to.x + ix, (int) to.y + iy + offFontY);
                            }
                        }
                        gb.setColor(to.shadowColor);
                        gb.drawString(to.text, (int) to.x + 2, (int) to.y + 2 + offFontY);
                        gb.setColor(to.fillColor);
                        gb.drawString(to.text, (int) to.x, (int) to.y + offFontY);
                    }
                }
                default -> {
                    error("No rendering process defined for %s", o.getClass().getSimpleName());
                }
            }
            drawDebugInfo(o, gb);
        });

        gb.dispose();

        // draw to screen.
        displayToWindow(stats);
    }

    private void displayToWindow(Map<String, Object> stats) {
        Graphics2D g = (Graphics2D) frame.getBufferStrategy().getDrawGraphics();
        g.drawImage(buffer,
            0, 0, screenSize.width, screenSize.height,
            0, 0, bufferSize.width, bufferSize.height,
            null);
        g.setColor(Color.ORANGE);
        g.drawString(prepareStatsString(stats, "[ ", " ]", " | "),
            16, frame.getHeight() - 16);
        g.dispose();
        frame.getBufferStrategy().show();
    }

    /**
     * Create a String from all the {@link java.util.Map.Entry} of a {@link Map}.
     * <p>
     * the String is composed on the format "[ entry1:value1 | entry2:value2 ]"
     * where, in e the map :
     *
     * <pre>
     * Maps.of("1_entry1","value1","2_entry2","value2",...);
     * </pre>
     * <p>
     * this will sort the Entry on the `[9]` from the `[9]_[keyname]` key name.
     *
     * @param attributes the {@link Map} of value to be displayed.
     * @param start      the character to start the string with.
     * @param end        the character to end the string with.
     * @param delimiter  the character to seperate each entry.
     * @return a concatenated {@link String} based on the {@link Map}
     * {@link java.util.Map.Entry}.
     */
    public static String prepareStatsString(Map<String, Object> attributes, String start, String end,
                                            String delimiter) {
        return start + attributes.entrySet().stream().sorted(Map.Entry.comparingByKey()).map(entry -> {
            String value = "";
            switch (entry.getValue().getClass().getSimpleName()) {
                case "Double", "double", "Float", "float" -> {
                    value = String.format("%04.2f", entry.getValue());
                }
                case "Integer", "int" -> {
                    value = String.format("%5d", entry.getValue());
                }
                default -> {
                    value = entry.getValue().toString();
                }
            }
            return entry.getKey().substring(((String) entry.getKey().toString()).indexOf(':') + 1)
                + ":"
                + value;
        }).collect(Collectors.joining(delimiter)) + end;
    }

    /**
     * Convert a long duration value to a formatted String value "D hh:mm:ss.SSS".
     *
     * @param duration in ms
     * @return formatted String "%d d - %02d:%02d:%02d.%03d"
     */
    public static String formatDuration(long duration) {
        int ms, s, m, h, d;
        double dec;
        double time = duration * 1.0;

        time = (time / 1000.0);
        dec = time % 1;
        time = time - dec;
        ms = (int) (dec * 1000.0);

        time = (time / 60.0);
        dec = time % 1;
        time = time - dec;
        s = (int) (dec * 60.0);

        time = (time / 60.0);
        dec = time % 1;
        time = time - dec;
        m = (int) (dec * 60.0);

        time = (time / 24.0);
        dec = time % 1;
        time = time - dec;
        h = (int) (dec * 24.0);

        d = (int) time;
        if (d > 0) {
            return (String.format("%d d - %02d:%02d:%02d.%03d", d, h, m, s, ms));
        } else {
            return (String.format("%02d:%02d:%02d.%03d", h, m, s, ms));
        }

    }

    private void drawDebugInfo(GameObject o, Graphics2D gb) {
        if (Optional.ofNullable(debugFilter).isPresent() && debugFilter.contains(o.name)) {
            int offy = 0;
            if (debug > 0) {
                gb.setColor(Color.ORANGE);
                gb.setFont(gb.getFont().deriveFont(9.0f));
                if (o.getClass().getSimpleName().equals("TextObject")) {
                    offy = 8;
                }
                gb.drawString("#" + o.id, (int) (o.x + o.width + 4), (int) (o.y) + offy);
            }
            if (debug > 1) {
                // draw bounding box
                gb.setColor(Color.ORANGE);
                gb.draw(o);
                if (!o.staticObject) {
                    // show all applied forces
                    for (Vec2d f : o.forces) {
                        gb.setColor(Color.WHITE);
                        gb.drawLine(
                            (int) (o.x + (o.width * 0.5)),
                            (int) (o.y + (o.height * 0.5)),
                            (int) ((o.x + (o.width * 0.5)) + f.x * 100.0),
                            (int) ((o.y + (o.height * 0.5)) + f.y * 100.0));
                    }
                    // draw velocity
                    gb.setColor(Color.GREEN);
                    gb.drawLine(
                        (int) (o.x + (o.width * 0.5)),
                        (int) (o.y + (o.height * 0.5)),
                        (int) ((o.x + (o.width * 0.5)) + o.dx * 100.0),
                        (int) ((o.y + (o.height * 0.5)) + o.dy * 100.0));
                    // draw Acceleration
                    gb.setColor(Color.BLUE);
                    gb.drawLine((int) (o.x + (o.width * 0.5)),
                        (int) (o.y + (o.height * 0.5)),
                        (int) ((o.x + (o.width * 0.5)) + o.ax * 100.0),
                        (int) ((o.y + (o.height * 0.5)) + o.ay * 100.0));
                }
            }
            if (debug > 2) {
                gb.setColor(Color.ORANGE);
                if (o.getClass().getSimpleName().equals("TextObject")) {
                    offy = 8;
                }
                gb.drawString(o.name, (int) (o.x + o.width + 4), (int) (o.y) + offy + 8);
            }
        }
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
            case KeyEvent.VK_Z -> {
                if (e.isControlDown()) {
                    resetScene();
                    createScene();
                }
            }
            // switch debug mode level
            case KeyEvent.VK_D -> {
                if (e.isControlDown()) {
                    debug = 0;
                } else {
                    debug = (debug < 5 ? debug + 1 : 1);
                }
            }
            case KeyEvent.VK_PAGE_UP -> {
                int nb = 10;
                if (e.isControlDown()) {
                    nb *= 10;
                }
                if (e.isShiftDown()) {
                    nb *= 5;
                }
                addEnemies(nb);
            }
            default -> {
                // Nothing to do !
            }
        }

    }

    private void resetScene() {
        objects.clear();
        objectMap.clear();
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
        drawFlag = true;
    }

    @Override
    public void componentHidden(ComponentEvent e) {
        drawFlag = false;
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
