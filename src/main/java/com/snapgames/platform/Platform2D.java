package com.snapgames.platform;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * The {@link Platform2D} class base of the platform framework.
 * <p>
 * This BIG class contains some subclasses to encapsulate
 * some very important objects and or concepts, to simplify platform 2d approach and help
 * readers and contributors to better understand code.
 * <ul>
 *     <li><code>{@link Vec2d}</code> to do vector math on 2 dimensions,</li>
 *     <li><code>{@link Material}</code> to describe physic attributes for materials into a {@link GameObject},</li>
 *     <li><code>{@link GameObject}</code> to process and animate visible or invisible objects,</li>
 *     <li><code>{@link TextObject}</code> to display some text on screen,</li>
 *     <li><code>{@link ConstraintObject}</code> to create invisible objects adding constraints to the {@link World}</li>
 *     <li><code>{@link World}</code> to define the context where all the {@link GameObject}s or its inheriting child evolve and move.</li>
 * </ul>
 *
 * <blockquote><em>NOTE</em> The 'mono-class' approach is to keep global project and code small.</blockquote>
 * <p>
 * This class has its own lifecycle:
 *
 * <ol>
 *     <li><code>initialize</code> to define and initialize (sic) everything,</li>
 *     <li><code>createScene</code> to create all the scene objects (visible and invisible),</li>
 *     <li><code>loop</code> on the game cycle until exit is requested:
 *      <ul>
 *          <li>manage user (player) <code>input</code>,</li>
 *          <li><code>update</code> scene and objects,</li>
 *          <li><code>draw</code> everything active onto multiple internal buffers and then on screen,</li>
 *      </ul>
 *     </li>
 *     <li><code>dispose</code> to release all reserved resources</li>
 * </ol>
 *
 * @author Frédéric Delorme
 * @since 1.0.0
 */
public class Platform2D extends JPanel implements KeyListener, ComponentListener {
    /**
     * internal default Frame Per Second target to draw Scene.
     */
    public static final int FPS = 60;
    /**
     * Physic engine computation time reduction factor to a slightly decelerated world.
     */
    public static final double PHYSIC_TIME_FACTOR = 0.005;
    private static final Map<String, Object> resources = new LinkedHashMap<>(20, 0.80f);
    /**
     * internal debugger flag (0=no debug, to 5 max debug and visual debug info)
     */
    int debug = 0;
    /**
     * filtering the visual debug information screen by listing (coma separated)
     * in the filter the targeted object's names.
     */
    String debugFilter = null;

    /**
     * Flag to activate/deactivate drawing operation in the game loop.
     */
    private boolean drawFlag = true;
    /**
     * Flag to activate/deactivate updating operation in the game loop.
     */
    private boolean updateFlag = true;
    private String configurationFilePath = "/config.properties";
    private boolean testMode = false;

    public void setWorld(World world) {
        this.world = world;
    }

    public World getWorld() {
        return this.world;
    }

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

        public static final Material DEFAULT = new Material("default", 1.0, 0.70, 0.98);
        public static final Material RUBBER = new Material("rubber", 0.7, 0.68, 0.63);
        public static final Material SUPER_BALL = new Material("superball", 0.7, 0.98, 0.23);
        public static final Material WOOD = new Material("wood", 0.65, 0.20, 0.50);
        public static final Material STEEL = new Material("steel", 1.2, 0.10, 0.12);
        public static final Material AIR = new Material("air", 0.05, 0.0, 0.99);
        public static final Material WATER = new Material("water", 0.95, 0.1, 0.40);


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
        public boolean stickToCamera = false;
        public int priority = 1;

        public double lifespan = -1;
        public double timer = 0;

        public double debugOffsetY = 0;

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

        public GameObject setStickToCamera(boolean stc) {
            this.stickToCamera = stc;
            return this;
        }

        public boolean getStickToCamera() {
            return this.stickToCamera;
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

        public List<String> toDebugString() {
            List<String> dbgInfo = new ArrayList<>();
            dbgInfo.add(String.format("0_id:%d", getId()));
            dbgInfo.add("1_name:" + getName());
            dbgInfo.add(String.format("2_pos:(%.01f,%.01f)", x, y));
            dbgInfo.add(String.format("2_vel:(%.01f,%.01f)", dx, dy));
            dbgInfo.add(String.format("2_acc:(%.01f,%.01f)", dx, dy));
            return dbgInfo;
        }

        protected GameObject setDebugOffsetY(int offY) {
            this.debugOffsetY = offY;
            return this;
        }

        public GameObject setSize(double w, double h) {
            this.width = w;
            this.height = h;
            return this;
        }

    }

    /**
     * Add a new {@link ImageObject}, supporting a new attribute image.
     *
     * @author Frédéric Delorme
     * @since 1.0.0
     */
    public static class ImageObject extends GameObject {
        /**
         * The internal image for this {@link ImageObject}.
         */
        public BufferedImage image;

        public ImageObject(String name) {
            super(name);
        }

        public ImageObject setImage(BufferedImage img) {
            this.image = img;
            this.setRect(x, y, image.getWidth(), image.getHeight());
            return this;
        }

        public BufferedImage getImage() {
            return image;
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
            super.setDebugOffsetY(8);
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
     * The `{@link ConstraintObject}` is a specific object bringing physical contains into the {@link World},
     * to let influence existing other {@link GameObject} and inheritance with constrained forces.
     *
     * @author Frédéric Delorme
     * @since 1.0.0
     */
    public static class ConstraintObject extends GameObject {

        public ConstraintObject(String name) {
            super(name);
            this.staticObject = true;
        }

        public ConstraintObject(String name, double x, double y, double w, double h) {
            super(name);
            setRect(x, y, w, h);
        }

        public ConstraintObject setForce(Vec2d f) {
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
        private List<ConstraintObject> constrains = new ArrayList<>();

        public World(Vec2d gravity, Rectangle2D playAreaSize) {
            this.gravity = gravity;
            this.playArea = playAreaSize;
        }

        public void addConstraint(ConstraintObject constrain) {
            constrain.staticObject = true;
            constrains.add(constrain);
        }

        public Vec2d getGravity() {
            return gravity;
        }

        public Rectangle2D getPlayArea() {
            return playArea;
        }

        public List<ConstraintObject> getConstrains() {
            return constrains;
        }

    }

    /**
     * The {@link Camera} class provides a way to follow a {@link GameObject} target to move
     * a view according to the target moves. The velocity of the following movement is defined by a tween factor,
     * acting as a spring linking the {@link Camera} and the focus {@link GameObject}.
     *
     * @author Frederic Delorme
     * @since 1.0.0
     */
    public static class Camera extends GameObject {
        private GameObject target;
        private double tweenFactor;
        private Rectangle2D viewport;

        public Camera(String name) {
            super(name);
        }

        Camera setTarget(GameObject target) {
            this.target = target;
            return this;
        }

        Camera setTweenFactor(double tf) {
            this.tweenFactor = tf;
            return this;
        }

        Camera setViewport(Rectangle2D vp) {
            this.viewport = vp;
            return this;
        }

        public void update(double dt) {
            super.update(dt);

            this.x += Math
                    .ceil((target.x + (target.getWidth() * 0.5) - ((viewport.getWidth()) * 0.5) - this.x)
                            * tweenFactor * Math.min(dt, 10));
            this.y += Math
                    .ceil((target.y + (target.getHeight() * 0.5) - ((viewport.getHeight()) * 0.5) - this.y)
                            * tweenFactor * Math.min(dt, 10));

            this.viewport.setRect(this.x, this.y, this.viewport.getWidth(),
                    this.viewport.getHeight());

        }
    }

    public static class Tile {
        private final int width;
        private final int height;
        private BufferedImage image;

        public Tile(int w, int h) {
            this.width = w;
            this.height = h;
        }

        Tile setImage(BufferedImage img) {
            this.image = img;
            return this;
        }
    }

    public static class TileMap extends GameObject {
        Tile[] tiles;
        int[][] map;

        public TileMap(String name, int w, int h) {
            super(name);
            map = new int[w][h];
        }

        public int getTile(int x, int y) {
            return map[x][y];
        }

        public TileMap load(String path){
            try {
                Stream<String> tileMapData = Files.lines(Paths.get(path));
                tileMapData.forEach(s->{
                    
                });
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            return this;
        }
    }

    /**
     * The translated messages to be displayed in log or on screen.
     */
    private static ResourceBundle messages = ResourceBundle.getBundle("i18n.messages");
    /**
     * The required configuration key/values to define internal attributes default values at start.
     */
    private static Properties config = new Properties();

    /**
     * the internal drawing image buffer
     */
    BufferedImage buffer;
    /**
     * the {@link JFrame} used as a window for out game.
     */
    JFrame frame;

    /**
     * Internal {@link Platform2D} instance name.
     */
    private String name;
    /**
     * configured screen buffer size
     */
    private Dimension bufferSize;
    /**
     * Configured game Window size.
     */
    private Dimension screenSize;

    /**
     * Flag to request game loop exit and Platform2D instance ending.
     */
    private boolean exit;

    /**
     * Internal keyboard status array to track each Key state (true=pressed).
     */
    private final boolean[] keys = new boolean[1024];

    /**
     * Internal list of  Scene's {@link GameObject}.
     */
    final List<GameObject> objects = new CopyOnWriteArrayList<>();
    /**
     * Internal Map of Scene {@link GameObject} for simplify of instance access
     */
    final Map<String, GameObject> objectMap = new ConcurrentHashMap<>();

    /**
     * Internal map of KPI (statistics) to be maintained by the {@link Platform2D} instance,
     * and/or can be exposed/exported at anytime.
     */
    Map<String, Object> stats = new HashMap<>();

    /**
     * The game {@link World} context where all the {@link GameObject} will move in.
     */
    private World world;

    private Camera camera;

    /**
     * Create a new {@link Platform2D} instance, having an <code>appName</code>,
     * a specific rendering buffer dimension and a defined window size.
     *
     * @param appName    the Platform2D internal instance name (mainly used for logging)
     * @param bufferSize the internal rendering image buffer size
     * @param screenSize the window size
     */
    public Platform2D(String appName, Dimension bufferSize, Dimension screenSize) {
        this.name = appName;
        this.bufferSize = bufferSize;
        this.screenSize = screenSize;
    }

    public Platform2D(String name) {
        this(name, null, null);
    }

    /**
     * Initialization of everything based on the configuration file (here is <code>config.properties</code>)
     * and the java CLI arguments.
     *
     * @param args the java CLI arguments.
     */
    void initialize(String[] args) {
        intializeDefaultConfiguration();
        parseArguments(args);

        loadConfiguration(configurationFilePath);

        buffer = new BufferedImage(bufferSize.width, bufferSize.height, BufferedImage.TYPE_INT_ARGB);

        frame = new JFrame(getMessage("app.title"));
        setPreferredSize(screenSize);
        setMaximumSize(screenSize);
        setMinimumSize(screenSize);

        frame.setContentPane(this);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.addComponentListener(this);
        frame.addKeyListener(this);

        frame.pack();

        if (!testMode) {
            frame.setVisible(true);
        }
        frame.createBufferStrategy(3);

        createScene();
    }

    private void intializeDefaultConfiguration() {
        config.put("app.debug.level", "0");
        config.put("app.debug.filter", "");
        config.put("app.screen.size", "320x200");
        config.put("app.window.size", "640x400");
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

    /**
     * Parse the String list of argument, each noted <code>key=value</code>.
     *
     * @param args the list of String to be processed.
     */
    private void parseArguments(String[] args) {
        Map<String, Object> attributes = Arrays.stream(args)
                .map(s -> s.split("="))
                .collect(Collectors.toMap(split -> split[0], split -> split[1]));

        parseAttributes(attributes);
    }

    /**
     * Parse the list of entry from this {@link Properties} instance.
     *
     * @param props the Properties instance to feed with all configuration attributes.
     */
    private void parseArguments(Properties props) {
        Map<String, Object> attributes = props.entrySet().stream()
                .collect(Collectors.toMap(split -> (String) split.getKey(), Map.Entry::getValue));
        parseAttributes(attributes);
    }

    /**
     * Parse all the entries from the map and assign accordingly internal
     * variables with corresponding values.
     *
     * @param attributes the map of (key,value) to feed the configuration.
     */
    private void parseAttributes(Map<String, Object> attributes) {
        if (attributes.size() > 0) {
            for (Map.Entry<String, Object> entry : attributes.entrySet()) {
                String key = entry.getKey();
                Object value = entry.getValue();
// Each entry is cased as ("configuration key", "long CLI argument", "short key argument")
                switch (key) {
                    case "configuration.file", "config", "cf" -> configurationFilePath = (String) value;
                    case "app.screen.size", "buffer", "b" -> {
                        String[] values = ((String) value).split("x");
                        bufferSize = new Dimension(Integer.parseInt(values[0]), Integer.parseInt(values[1]));
                    }
                    case "app.window.size", "window", "w" -> {
                        String[] values = ((String) value).split("x");
                        screenSize = new Dimension(Integer.parseInt(values[0]), Integer.parseInt(values[1]));
                    }
                    case "app.debug.level", "debug", "d" -> debug = Integer.parseInt((String) value);
                    case "app.debug.filter", "filter", "df" -> debugFilter = (String) value;
                    case "app.test.mode" -> this.testMode = Boolean.parseBoolean((String) value);
                    default -> warn("Value entry unknown %s=%s", key, value);
                }
            }
        } else {
            error("Configuration file %s is empty", configurationFilePath);
        }
    }

    private void createScene() {
        // define the game World
        world = new World(
                new Vec2d(0, 0.0981),
                new Rectangle2D.Double(0, 0, 320, 200));

        BufferedImage backgroundImg = ((BufferedImage) getResource("/assets/images/backgrounds/forest.jpg"));
        ImageObject background = (ImageObject) new ImageObject("background")
                .setImage(backgroundImg)
                .setStaticObject(true)
                .setPriority(-10);
        addGameObject(background);

        // add a player object
        GameObject player = new GameObject(
                "player")
                .setPosition(bufferSize.width >> 1, bufferSize.height >> 1)
                .setSize(16, 16)
                .setMaterial(new Material("player", 1.0, 0.30, 0.92))
                .addAttribute("energy", 100)
                .addAttribute("mana", 100)
                .addAttribute("lives", 3)
                .setMass(80.0);
        addGameObject(player);

        createHUD(player);

        // Add some constraining object.
        ConstraintObject waterConstraint =
                (ConstraintObject) new ConstraintObject("water",
                        0,
                        world.getPlayArea().getHeight() * 0.70,
                        world.getPlayArea().getWidth(),
                        world.getPlayArea().getHeight() * 0.30)
                        .setPriority(2)
                        .setFillColor(new Color(0.2f, 0.2f, 0.7f, 0.4f))
                        .setBorderColor(new Color(0.0f, 0.0f, 0.0f, 0.0f))
                        .setMaterial(Material.WATER)
                        .addForce(new Vec2d(0, -0.37));

        addGameObject(waterConstraint);

        world.addConstraint(waterConstraint);

        // add some enemies
        addEnemies(10, Material.WOOD);

        setCamera(new Camera("cam01")
                .setTarget(player)
                .setTweenFactor(0.05)
                .setViewport(
                        new Rectangle2D.Double(
                                0, 0,
                                bufferSize.getWidth(), bufferSize.getHeight())));
    }

    private void setCamera(Camera cam) {
        this.camera = cam;
    }

    private void createHUD(GameObject player) {
        TextObject score = (TextObject) new TextObject("score")
                .setFont(buffer.createGraphics().getFont().deriveFont(18.0f))
                .setText("000000")
                .setShadowColor(new Color(0.2f, 0.2f, 0.2f, 0.8f))
                .setPosition(8, 16)
                .setFillColor(Color.WHITE)
                .setBorderColor(Color.BLACK)
                .setPriority(1)
                .setStaticObject(true)
                .setStickToCamera(true);
        addGameObject(score);

        BufferedImage heartImage = ((BufferedImage) getResource("/assets/images/tiles01.png|0,96,16,16"));
        ImageObject heart = (ImageObject) new ImageObject("heart")
                .setImage(heartImage)
                .setPosition(bufferSize.width - 32, 16)
                .setPriority(1)
                .setStaticObject(true)
                .setStickToCamera(true);

        TextObject lifes = (TextObject) new TextObject("lives")
                .setFont(buffer.createGraphics().getFont().deriveFont(12.0f))
                .setText("" + (player.attributes.get("lives")))
                .setShadowColor(new Color(0.2f, 0.2f, 0.2f, 0.8f))
                .setPosition(bufferSize.width - 24, 20)
                .setFillColor(Color.WHITE)
                .setBorderColor(Color.BLACK)
                .setPriority(2)
                .setStaticObject(true)
                .setStickToCamera(true);
        addGameObject(heart);
        addGameObject(lifes);
    }

    /**
     * Retrieve from cache or Load a resource from the storage.
     * Cache is 'path' oriented, each loaded resource from a path is stored in the cache with its path as a key.
     *
     * <blockquote>The default processing of the resource loading is based on the path String, that also can contain some processing operations:
     * <pre>
     *     path/to/my/resource[|optional command]
     * </pre>
     * <p>
     * Where optional command depends on the resource nature.
     * </blockquote>
     * <p>For image resource:
     *
     * <ul>
     *     <li><code>path</code>You can request for any image from PNG or JPG file,</li>
     *     <li><code>path|x,y,w,h</code> you can extract part of the image from path according to rectangle at (x,y) with size (w,h).</li>
     * </ul>
     *
     * @param path file path to the resources to be retrieved/loaded.
     * @return the corresponding object resource.
     */
    static Object getResource(String path) {
        Object resource = null;
        if (!resources.containsKey(path)) {
            String ext = "";
            if (path.contains("|")) {
                ext = path.substring(path.lastIndexOf("."), path.lastIndexOf("|"));
            } else {
                ext = path.substring(path.lastIndexOf("."));
            }
            switch (ext.toLowerCase()) {
                case ".png", ".jpg" -> {
                    loadAndOrSliceImageFrom(path);
                }
                default -> {
                    error("Unknown file extension %s", ext);
                }
            }
        }
        return resources.get(path);
    }

    /**
     * Retrieve a specific message from the translation according to its message key.
     *
     * @param keyMsg the key of the message in the translation files
     * @return the corresponding translated message.
     */
    public String getMessage(String keyMsg) {
        return replaceTemplate(messages.getString(keyMsg), messages);
    }

    /**
     * Process template text to replace "${specific.key}" by their translated values.
     *
     * @param template the template text to be parsed and where keys must be translated.
     * @param values   the list of translated keys/values.
     * @return the key translated resulting message.
     */
    public static String replaceTemplate(String template, ResourceBundle values) {
        StringTokenizer tokenizer = new StringTokenizer(template, "${}", true);
        StringJoiner joiner = new StringJoiner("");
        while (tokenizer.hasMoreTokens()) {
            String token = tokenizer.nextToken();
            if (token.equals("$")) {
                if (tokenizer.hasMoreTokens()) {
                    token = tokenizer.nextToken();

                    if (token.equals("{") && tokenizer.hasMoreTokens()) {
                        String key = tokenizer.nextToken();

                        if (tokenizer.hasMoreTokens() && tokenizer.nextToken().equals("}")) {
                            String value = values.containsKey(key) ? values.getString(key) : "${" + key + "}";
                            joiner.add(value);
                        }
                    }
                }
            } else {
                joiner.add(token);
            }
        }
        return joiner.toString();
    }


    private static void loadAndOrSliceImageFrom(String path) {
        try {
            if (path.contains("|")) {
                //required to extract path or the image file.
                String parts[] = path.substring(path.lastIndexOf("|") + 1).split(",");
                int px = Integer.parseInt(parts[0]);
                int py = Integer.parseInt(parts[1]);
                int pw = Integer.parseInt(parts[2]);
                int ph = Integer.parseInt(parts[3]);
                String realPath = path.substring(0, path.lastIndexOf("|"));
                BufferedImage img = ImageIO.read(Platform2D.class.getResourceAsStream(realPath));
                resources.put(path, img.getSubimage(px, py, pw, ph));
            } else {
                resources.put(path, ImageIO.read(Objects.requireNonNull(Platform2D.class.getResourceAsStream(path))));
            }
        } catch (IOException e) {
            error("Unable to read Image resource " + e.getMessage());
        }
    }

    private void addEnemies(int nbEnemies, Material material) {
        for (int i = 0; i < nbEnemies; i++) {
            // add a player object
            GameObject enemy = new GameObject(
                    "enemy_" + i,
                    Math.random() * bufferSize.width,
                    Math.random() * bufferSize.height,
                    8, 8)
                    .setMaterial(new Material("enemy", 0.7, 0.80, 0.99))
                    .setFillColor(Color.BLUE)
                    .setBorderColor(Color.DARK_GRAY)
                    .addAttribute("energy", 100)
                    .addAttribute("mana", 100)
                    .setMaterial(material)
                    .setPriority(10 + i)
                    .setMass(10.0);
            addGameObject(enemy);
        }
    }

    void addGameObject(GameObject gameObject) {
        objects.add(gameObject);
        objectMap.put(gameObject.getName(), gameObject);
        // update statistics data
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
            cumulatedTime += (long) elapsed;
            if (cumulatedTime > 1000) {
                gameTime += cumulatedTime;
                framesPerSec = frames;
                updatesPerSec = updates;
                cumulatedTime = 0;
                frames = 0;
                updates = 0;
                updateStats(stats, framesPerSec, updatesPerSec, gameTime, elapsed);
            }
            if (drawFlag) {
                frames += 1;
                draw(stats);
            }

            // compute time to wait regarding elapsed minimal frame time
            int wait = Math.max((int) ((1000 / FPS) - elapsed), 1);
            try {
                Thread.sleep(wait);
            } catch (InterruptedException e) {
                error("Unable to sleep for %dms%n", wait);
            }
            previous = currentTime;
        }
    }

    /**
     * Update internal statistics to be exposed/exported later.
     *
     * @param stats         the bunch of internal KPI
     * @param framesPerSec  the famous real FPS
     * @param updatesPerSec the almost famous UPS (no, not the worldwide size delivery company...)
     * @param gameTime      the internal real game time (without pause, focus lost, etc...)
     * @param elapsed       the elapsed time between two loop iterations.
     */
    private void updateStats(
            Map<String, Object> stats,
            long framesPerSec,
            long updatesPerSec,
            long gameTime,
            double elapsed) {

        long countActive = objects.stream()
                .filter(GameObject::isActive).count();
        long countStatic = objects.stream()
                .filter(GameObject::isObjectStatic).count();
        stats.put("0:debug", debug);
        stats.put("1:obj", objects.size());
        stats.put("2:static", countStatic);
        stats.put("3:active", countActive);
        stats.put("4:fps", framesPerSec);
        stats.put("5:ups", updatesPerSec);
        stats.put("6:time", formatDuration(gameTime, false));
        stats.put("6:elps", elapsed);

    }

    /**
     * Process the inputs (keyboard)
     */
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

    /**
     * Process all the scene's {@link GameObject} list update according to elapsed time.
     *
     * @param elapsed
     */
    void update(double elapsed) {
        objects.stream()
                // process only active and non-static objects
                .filter(o -> !o.staticObject && o.active)
                .forEach(o -> {
                    // reset current GameObject acceleration
                    o.ax = 0;
                    o.ay = 0;

                    // apply all concerned World constraints
                    applyWorldConstraints(world, o, elapsed);

                    // add applied forces on acceleration
                    o.forces.forEach(v -> {
                        o.ax += v.x;
                        o.ay += v.y;
                    });

                    // compute resulting speed
                    o.dx += (o.ax * elapsed * PHYSIC_TIME_FACTOR);
                    o.dy += (o.ay * elapsed * PHYSIC_TIME_FACTOR);

                    // get the GameObject o position
                    o.x += o.dx * elapsed;
                    o.y += o.dy * elapsed;

                    // apply friction "force" to the velocity
                    o.dx *= o.material.friction;
                    o.dy *= o.material.friction;

                    o.update(elapsed);
                    keepGameObjectIntoPlayArea(world, o);
                    o.forces.clear();

                });
        if (Optional.ofNullable(camera).isPresent()) {
            camera.update(elapsed);
        }
    }

    /**
     * the sheep keeper for all scene's {@link GameObject} to keep in the {@link World} defined play area.
     *
     * @param world the parent {@link World} instance
     * @param go    the {@link GameObject} instance to be kept.
     */
    private void keepGameObjectIntoPlayArea(World world, GameObject go) {
        // Constrains the GameObject o into the play area.
        Rectangle2D playArea = world.getPlayArea();
        if (playArea.intersects(go) || !playArea.contains(go)) {
            if (go.x < 0) {
                go.x = 0;
                go.dx *= -go.material.elasticity;
                go.contact = true;
            }
            if (go.x > playArea.getWidth() - go.width) {
                go.x = playArea.getWidth() - go.width;
                go.dx *= -go.material.elasticity;
                go.contact = true;
            }
            if (go.y < 0) {
                go.y = 0;
                go.dy *= -go.material.elasticity;
                go.contact = true;
            }
            if (go.y > playArea.getHeight() - go.width) {
                go.y = playArea.getHeight() - go.height;
                go.dy *= -go.material.elasticity;
                go.contact = true;
            }
        }
    }

    /**
     * Apply all World constraints on colliding {@link GameObject} instance.
     *
     * @param world   the game {@link World} instance.
     * @param go      the {@link GameObject} to be compared
     * @param elapsed the elapsed time since previous call.
     */
    private void applyWorldConstraints(World world, GameObject go, double elapsed) {
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

    /**
     * Draw all active {@link GameObject} in the current scene.
     *
     * @param stats the internal statistics and KPI to be displayed on screen (only if debug>0)
     */
    private void draw(Map<String, Object> stats) {
        Graphics2D gb = buffer.createGraphics();
        // set Antialiasing mode
        gb.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        gb.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        // clear buffer
        gb.setBackground(Color.BLACK);
        gb.clearRect(0, 0, 640, 400);

        gb.translate(-camera.x, -camera.y);
        drawAllEntity(gb, false);

        if (debug > 1) {
            gb.setColor(Color.YELLOW);
            gb.draw(world.getPlayArea());
        }

        gb.translate(camera.x, camera.y);
        drawAllEntity(gb, true);

        gb.dispose();

        // draw to screen.
        displayToWindow(stats);
    }

    private void drawAllEntity(Graphics2D gb, boolean stickToCamera) {
        // draw all the platform game's scene.
        objects.stream()
                .filter(GameObject::isActive)
                .filter(o -> o.getStickToCamera() == stickToCamera)
                .forEach(o -> {
                    drawGameObject(gb, o);
                    drawDebugInfo(o, gb);
                });
    }

    private void drawGameObject(Graphics2D gb, GameObject o) {
        switch (o.getClass().getSimpleName()) {
            case "GameObject" -> {
                gb.setColor(o.fillColor);
                gb.fill(o);
                gb.setColor(o.borderColor);
                gb.draw(o);
            }
            case "ImageObject" -> {
                ImageObject io = (ImageObject) o;
                double w = Math.min(world.playArea.getWidth() - io.x, io.getImage().getWidth());
                double h = Math.min(world.playArea.getHeight() - io.y, io.getImage().getHeight());
                gb.drawImage(io.getImage(), (int) io.x, (int) io.y, (int) w, (int) h, null);
            }
            case "ConstraintObject" -> {
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
    }

    /**
     * Copy rendering buffer to game window and show visual stats on screen bottom.
     *
     * @param stats a Map of statistics and KPI to be displayed in debug mode (debug>0).
     */
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
     *
     * <p>
     * This will sort the Entry on the `[9]` from the `[9]_[keyname]` key name.
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
     * @param milliseconds in ms
     * @return formatted String "%d d - %02d:%02d:%02d.%03d"
     */
    public static String formatDuration(long milliseconds, boolean withMS) {
        Duration duration = Duration.ofMillis(milliseconds);
        if (duration.toDays() > 0) {
            return String.format(withMS ? "%d d - %02d:%02d:%02d.%03d" : "%d d - %02d:%02d:%02d",
                    duration.toDays(),
                    duration.toHours() - (24 * duration.toDays()),
                    duration.toMinutesPart(),
                    duration.toSecondsPart(),
                    duration.toMillisPart());
        } else {
            return String.format(withMS ? "%02d:%02d:%02d.%03d" : "%02d:%02d:%02d",
                    duration.toHours(),
                    duration.toMinutesPart(),
                    duration.toSecondsPart(),
                    duration.toMillisPart());

        }
    }

    /**
     * Draw visual debug information relative to a specific {@link GameObject} instance.
     *
     * @param o  the {@link GameObject} to display visual debug information for.
     * @param gb the {@link Graphics2D} API instance to be used.
     */
    private void drawDebugInfo(GameObject o, Graphics2D gb) {
        if (Optional.ofNullable(debugFilter).isPresent()
                && (debugFilter.contains(o.name) || debugFilter.equals("all"))) {
            if (debug > 0) {
                gb.setColor(Color.ORANGE);
                gb.setFont(gb.getFont().deriveFont(9.0f));
                int line = 0;
                for (String item : o.toDebugString()) {
                    int level = Integer.parseInt(item.contains("_") ? item.substring(0, item.indexOf("_")) : "0");
                    if (debug > level) {
                        String info = item.substring(item.indexOf("_") + 1);
                        gb.drawString(info, (int) (o.x + o.width + 4), (int) (o.y + o.debugOffsetY + (line++ * 9)));
                    }
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
            }

        }
    }

    /**
     * free all created/loaded resources.
     */
    void dispose() {
        if (Optional.ofNullable(frame).isPresent()) {
            frame.dispose();
        }
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
                addEnemies(nb, Material.WOOD);
            }
            default -> {
                // Nothing to do !
            }
        }

    }

    /**
     * Reset all objects from the current scene
     */
    private void resetScene() {
        objects.clear();
        objectMap.clear();
    }

    /*---- Component Event processing ---------------------------*/
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

    /*---- write to output log according to risk level (INFO, DEBUG, WARN, ERROR) ----*/
    public static void debug(String message, Object... args) {
        log("DEBUG", message, args);
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
