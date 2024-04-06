package com.snapgames.platform;

import com.snapgames.platform.demo.DemoScene;

import javax.imageio.ImageIO;
import javax.sound.sampled.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

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
     * Internal map of KPI (statistics) to be maintained by the {@link Platform2D} instance,
     * and/or can be exposed/exported at anytime.
     */
    Map<String, Object> stats = new HashMap<>();

    /**
     * The game {@link World} context where all the {@link GameObject} will move in.
     */
    private World world;

    /**
     * internal debugger flag (0=no debug, to 5 max debug and visual debug info)
     */
    static int debug = 0;
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

    private Renderer renderer;
    private SceneManager scnManager;
    private SoundManager soundManager;
    private String defaultSceneName = "start";
    private String[] strSceneList = new String[]{"start:Platform2D"};


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

        public Vec2d substract(Vec2d v1) {
            return new Vec2d(x - v1.x, y - v1.y);
        }

        public double length() {
            return Math.sqrt(x * x + y * y);
        }

        public double distance(Vec2d v1) {
            return substract(v1).length();
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
     * {@link Node} class provide a tree structure to the platform architecture. all the internal objects are
     * Node with two links:  parent and child.
     *
     * @author Frederic Delorme
     * @since 1.0.1
     */
    public static class Node extends Rectangle2D.Double {
        private static long index = 0;

        private long id = ++index;

        public boolean active = true;
        public double lifespan = -1;
        public double timer = 0;
        protected String name = "node" + id;

        protected Node parent;

        protected List<Node> children = new ArrayList<>();

        protected List<Behavior<? extends Node>> behaviors = new ArrayList<>();

        public List<Behavior<? extends Node>> getBehaviors() {
            return behaviors;
        }

        /**
         * Create a new {@link Node} with some geometric data
         *
         * @param x    the horizontal position
         * @param y    the vertical position
         * @param w    the width if the node
         * @param h    the height of the node
         * @param name the name of this node.
         */
        public Node(double x, double y, double w, double h, String name) {
            super(x, y, w, h);
            this.name = name;
        }

        /**
         * Create a new {@link Node} with default parameter position (0,0) size (0,0) and
         * <code>name = "node_" + internal node unique id</code>.
         */
        public Node() {
            super();
        }

        public void add(Node child) {
            setParent(this);
            children.add(child);
        }

        public List<Node> getChild() {
            return children;
        }

        private void setParent(Node node) {
            this.parent = node;
        }

        public Node setLifespan(int d) {
            this.lifespan = d;
            return this;
        }

        public long getId() {
            return id;
        }

        public String getName() {
            return name;
        }

        /**
         * Update the lifespan regarding target duration.
         * <ul>
         *     <li>if duration=-1, no effect,</li>
         *     <li>if lifespan>duration, object is deactivated.</li>
         * </ul>
         * <p>
         * by default, a {@link Node} has no duration (=-1).
         *
         * @param elapsed the elapsed time since previous call.
         */
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

        public <T extends Node> T addBehavior(Behavior<T> b) {
            behaviors.add(b);
            return (T) this;
        }

        public long getNextIndex() {
            return index + 1;
        }
    }

    /**
     * The {@link GameObject} entity to be moved and animated.
     *
     * @author Frédéric Delorme
     * @since 1.0.0
     */
    public static class GameObject extends Node {
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

        public double debugOffsetY = 0;

        public GameObject() {
            super();
        }

        public GameObject(String name) {
            super();
            this.name = name;
        }

        public GameObject(String name, double x, double y, double w, double h) {
            super(x, y, w, h, name);
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

        public Node setStickToCamera(boolean stc) {
            this.stickToCamera = stc;
            return this;
        }

        public boolean getStickToCamera() {
            return this.stickToCamera;
        }

        public Node addForce(Vec2d f) {
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

        public boolean isStaticObject() {
            return staticObject;
        }

        public GameObject setSize(int w, int h) {
            setRect(x, y, w, h);
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

        public TextObject setText(String text) {
            this.text = text;
            return this;
        }

        public TextObject setFont(Font font) {
            this.font = font;
            return this;
        }

        public TextObject setShadowColor(Color shadow) {
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

    public static class ParticleSystem extends GameObject {
        int nbParticles = 0;
        int chunk = 10;

        public ParticleSystem(String name) {
            super(name);
        }

        @Override
        public void update(double elapsed) {
            if (nbParticles > children.size()) {
                for (int i = 0; i < chunk; i++) {
                    behaviors.stream().filter(b -> b instanceof ParticleBehavior<? extends Node>).forEach(b ->
                            ((ParticleBehavior<? extends Node>) b).create((Scene) this.parent, this));
                }
            }
        }

        public ParticleSystem setMaxNbParticles(int nbMaxParticles) {
            this.nbParticles = nbMaxParticles;
            this.children = new ArrayList<>();
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

        public World() {
            this(new Vec2d(0.0, 0.0), new Rectangle2D.Double(0, 0, 320, 200));
        }

        public World(Vec2d gravity, Rectangle2D playAreaSize) {
            this.gravity = gravity;
            this.playArea = playAreaSize;
        }

        public void addConstrain(ConstraintObject constrain) {
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
     * The {@link Camera} object intends to track a {@link GameObject} target and focus on it, providing a fixed viewport for rendering.
     * the {@link GameObject} and inheriting object can be stick to a {@link Camera}.
     * the tracking effect is delayed according to the {@link Camera}'s tweenFactor.
     *
     * @author Frederic Delorme
     * @since 1.0.0
     */
    public static class Camera extends GameObject {
        private Node target;
        private double tweenFactor;
        private Rectangle2D viewport;

        public Camera(String name) {
            super(name);
        }

        public Camera setTarget(Node target) {
            this.target = target;
            return this;
        }

        public Camera setTweenFactor(double tf) {
            this.tweenFactor = tf;
            return this;
        }

        public Camera setViewport(Rectangle2D vp) {
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

    /**
     * The new {@link Scene} interface provides the required mechanism, with the help of {@link AbstractScene}
     * to let the {@link SceneManager} switch between multiple scene in the game.
     *
     * @author Frederic Delorme
     * @since 1.0.0
     */
    public interface Scene {
        String getName();

        void initialize(Platform2D app);

        default void load(Platform2D app) {
        }

        void create(Platform2D app);

        void input(Platform2D app);

        default void update(Platform2D app, Map<String, Object> stats, double elapsed) {

        }

        default void draw(Platform2D app, Graphics2D g, Map<String, Object> stats) {

        }

        void close(Platform2D app);

        void dispose(Platform2D app);

        default void keyPressed(KeyEvent e) {
        }

        default void keyReleased(KeyEvent e) {
        }

        Camera getCamera();

        List<? extends Node> getChild();

        void add(GameObject go);

        GameObject getObject(String goName);

        void reset();

        GameObject getObject(int idx);

        List<Behavior<? extends Node>> getBehaviors();

        Platform2D getPlatform();

        World getWorld();
    }

    /**
     * The {@link AbstractScene} implements all the required object management for Scene and facilitate the {@link GameObject}
     * processing in the game loop through the input, update and draw methods.
     * <p>
     * It also helps on managing the switch between 2 {@link Scene} with initialize, create, close and dispose.
     *
     * @author Frederic Delorme
     * @since 1.0.1
     */
    public static abstract class AbstractScene extends Node implements Scene {
        protected final Platform2D app;
        protected Platform2D.Camera camera;

        /**
         * Internal Map of Scene {@link GameObject} for simplify of instance access
         */
        final Map<String, GameObject> objectMap = new ConcurrentHashMap<>();

        public AbstractScene(Platform2D app) {
            super();
            this.name = getName();
            this.app = app;
        }

        protected void setCamera(Platform2D.Camera cam) {
            this.camera = cam;
        }

        public void add(GameObject gameObject) {
            super.add(gameObject);
            objectMap.put(gameObject.getName(), gameObject);
            // update statistics data
            getChild().sort((a, b) -> Integer.compare(((GameObject) a).priority, ((GameObject) b).priority));
        }

        public GameObject getObject(String name) {
            return objectMap.get(name);
        }

        public GameObject getObject(int idx) {
            return (GameObject) children.get(idx);
        }

        public Platform2D.Camera getCamera() {
            return camera;
        }

        @Override
        public abstract String getName();

        @Override
        public void initialize(Platform2D app) {

        }

        @Override
        public void create(Platform2D app) {

        }

        @Override
        public void input(Platform2D app) {

        }

        public void close(Platform2D app) {

        }

        public void dispose(Platform2D app) {

        }

        public void reset() {
            objectMap.clear();
            children.clear();
        }

        public Platform2D getPlatform() {
            return app;
        }

        @Override
        public World getWorld() {
            return app.world;
        }
    }

    /**
     * the {@link StartScene} is the default one when none has been defined in the configuration file.
     *
     * @author Frederic Delorme
     * @since 1.0.1
     */
    public static class StartScene extends AbstractScene {

        public StartScene(Platform2D app) {
            super(app);
        }

        @Override
        public void initialize(Platform2D app) {
            getResource("/assets/images/backgrounds/volcano.png");
        }

        @Override
        public void create(Platform2D app) {
            Renderer renderer = app.getRenderer();
            Graphics2D gb = renderer.getDrawBuffer().createGraphics();

            // add background Image
            BufferedImage bckImage = (BufferedImage) getResource("/assets/images/backgrounds/volcano.png");
            ImageObject backgroundIObj = (ImageObject) new ImageObject("background")
                    .setImage(bckImage).setPriority(0);
            add(backgroundIObj);

            // add welcome message
            String welcomeTxt = getMessage("app.start.welcome");
            Font welcomeFont = gb.getFont().deriveFont(18.0f);
            gb.setFont(welcomeFont);
            int textWidth = gb.getFontMetrics().stringWidth(welcomeTxt);
            TextObject txtObject = (TextObject) new TextObject("welcome")
                    .setText(welcomeTxt)
                    .setShadowColor(Color.BLACK)
                    .setFont(welcomeFont)
                    .setPosition(
                            (renderer.getBufferSize().width - textWidth) * 0.5,
                            renderer.getBufferSize().height * 0.5)
                    .setBorderColor(Color.WHITE)
                    .setPriority(1)
                    .setStaticObject(true);
            add(txtObject);
        }

        @Override
        public String getName() {
            return "start";
        }
    }

    /**
     * The {@link SceneManager} is the internal Platform2D service dedicated to Scene management.
     * <p>
     * It takes care of initialization, creation of a Scene, its inpuit,update and draw phases, and finally
     * close and dispose ot if.
     *
     * @author Frederic Delorme
     * @since 1.0.1
     */
    public static class SceneManager {
        private Map<String, Scene> scenes = new HashMap<>();
        private Scene activeScene = null;

        private Platform2D app;

        SceneManager(Platform2D a) {
            this.app = a;
        }

        /**
         * Provide a list of scene implementation to be preloaded into the Scene manager tobe later activated.
         * <p>
         * The correct format for scene list string is "<code>scene1:my.implementation.of.Scene1,scene2:my.implementation.of.Scene2[,...]</code>".
         *
         * @param sceneItems a string array containing list of tuple "[scene_key]:[scene_implementation_class]"
         */
        public void load(String[] sceneItems) {
            Arrays.stream(sceneItems).forEach(item -> {
                String[] attrs = item.split(":");
                // Create Scene instance according to the defined class.
                try {
                    Class<?> sceneClass = Class.forName(attrs[1]);
                    Constructor<?> cstr = sceneClass.getConstructor(Platform2D.class);
                    Scene scene = (Scene) cstr.newInstance(this.app);
                    scenes.put(scene.getName(), scene);
                } catch (ClassNotFoundException | NoSuchMethodException | InstantiationException |
                         IllegalAccessException | InvocationTargetException e) {
                    error("Can not create the %s class: %s%n", attrs[1], e.getMessage());
                }
            });
        }

        public void add(Scene s) {
            scenes.put(s.getName(), s);
        }

        private void activate(String sName) {
            if (!scenes.isEmpty()) {
                if (Optional.ofNullable(activeScene).isPresent()) {
                    activeScene.close(app);
                }

                activeScene = scenes.get(sName);
                if (Optional.ofNullable(activeScene).isPresent()) {
                    activeScene.initialize(app);
                    activeScene.create(app);
                }
            }
        }

        public void create() {
            if (Optional.ofNullable(activeScene).isPresent()) {
                activeScene.create(app);
            }
        }

        public void update(Map<String, Object> stats, double elapsed) {
            if (Optional.ofNullable(activeScene).isPresent()) {
                activeScene.update(app, stats, elapsed);
            }
        }

        public void input() {
            if (Optional.ofNullable(activeScene).isPresent()) {

                activeScene.input(app);
                activeScene.getChild().forEach(c -> c.getBehaviors().forEach(b -> b.input(activeScene, c)));
            }
        }

        public void draw(Graphics2D g, Map<String, Object> stats) {
            if (Optional.ofNullable(activeScene).isPresent()) {

                activeScene.draw(app, g, stats);

            }
        }

        public void dispose() {
            if (Optional.ofNullable(activeScene).isPresent()) {
                activeScene.dispose(app);
            }
        }

        public void disposeAll() {
            scenes.values().stream().forEach(s -> s.dispose(app));
            activeScene = null;
        }

        public Collection<Scene> getScenes() {
            return scenes.values();
        }

        public Scene getActive() {
            return activeScene;
        }

        public void keyReleased(KeyEvent e) {
            activeScene.keyReleased(e);
        }

        public void keyPressed(KeyEvent e) {
            activeScene.keyPressed(e);
        }

        public Scene getActiveScene() {
            return activeScene;
        }
    }

    /**
     * The {@link Behavior} interface is a way to easily implement new processing for each of the <code>create</code>,
     * <code>input</code>, <code>update</code> and <code>draw</code> operation for the {@link Node} instance taking
     * advantage of this behavior.
     *
     * @param <T> The
     */
    public interface Behavior<T extends Node> {

        default void input(Scene s, Node n) {
        }

        default void update(Scene s, double elapsed, GameObject n) {
        }

        default void draw(Scene s, Graphics2D g, T n) {
        }
    }

    public static abstract class AbstractBehavior<T extends Node> implements Behavior<T> {

    }

    public interface ParticleBehavior<T extends Node> extends Behavior<T> {
        default GameObject create(Scene s, GameObject o) {
            return null;
        }
    }

    /**
     * The `SoundManager` will help play sound and music as WAV file.
     * <ul>
     *     <li>Add sound from file with {@link SoundManager#loadSound(String, String)},</li>
     *     <li>Play sound with {@link SoundManager#playSound(String)},</li>
     *     <li>Stop a sound with {@link SoundManager#stopSound(String)},</li>
     *     <li>Stop All soiund with {@link SoundManager#stopAllSounds()}.</li>
     * </ul>
     */
    public static class SoundManager {
        private Platform2D app;
        private Map<String, Clip> soundClips = new HashMap<>();

        public SoundManager(Platform2D app) {
            this.app = app;

        }

        public void loadSound(String name, String filePath) {
            try {
                AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(Objects.requireNonNull(SoundManager.class.getResourceAsStream(filePath)));
                Clip clip = AudioSystem.getClip();
                clip.open(audioInputStream);
                soundClips.put(name, clip);
            } catch (UnsupportedAudioFileException | IOException | LineUnavailableException e) {
                error("Unable to load sound %s from file %s: %s", name, filePath, e.getMessage());
            }
        }

        public void playSound(String name) {
            Clip clip = soundClips.get(name);
            if (clip != null) {
                if (!clip.isRunning())
                    clip.setFramePosition(0);
                clip.start();
            }
        }

        public void stopSound(String name) {
            Clip clip = soundClips.get(name);
            if (clip != null && clip.isRunning()) {
                clip.stop();
                clip.setFramePosition(0);
            }
        }

        public void stopAllSounds() {
            for (Clip clip : soundClips.values()) {
                if (clip.isRunning()) {
                    clip.stop();
                    clip.setFramePosition(0);
                }
            }
        }
    }

    public interface RenderPlugin<T extends GameObject> {
        void draw(Graphics2D g, Scene scn, Node go);

        Class<T> getObjectClass();
    }


    /**
     * Renderer service to draw all scene objects.
     *
     * @author Frederic Delorme
     * @since 1.0.0
     */
    public static class Renderer {

        public Map<Class<? extends GameObject>, RenderPlugin<? extends GameObject>> plugins = new HashMap<>();
        private Platform2D app;

        /**
         * the internal drawing image buffer
         */
        BufferedImage buffer;
        /**
         * the {@link JFrame} used as a window for out game.
         */
        JFrame frame;
        /**
         * configured screen buffer size
         */
        private Dimension bufferSize;
        /**
         * Configured game Window size.
         */
        private Dimension screenSize;

        public Renderer(Platform2D app) {
            this.app = app;
            addPlugin(new GameObjectRenderPlugin());
            addPlugin(new TextObjectRenderPlugin());
            addPlugin(new ImageObjectRenderPlugin());
            addPlugin(new ConstraintObjectRenderPlugin());
        }

        public void addPlugin(RenderPlugin<? extends GameObject> renderPlugin) {
            plugins.put(renderPlugin.getObjectClass(), renderPlugin);
        }

        public void initialize(Dimension bufferSize, Dimension screenSize) {
            this.bufferSize = bufferSize;
            this.screenSize = screenSize;

            // create internal drawing buffer
            buffer = new BufferedImage(bufferSize.width, bufferSize.height, BufferedImage.TYPE_INT_ARGB);

            // create window
            frame = new JFrame(getMessage("app.title"));

            frame.setContentPane(app);
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.addComponentListener(app);
            frame.addKeyListener(app);
            frame.pack();
            if (!app.testMode) {
                frame.setVisible(true);
            }
            frame.createBufferStrategy(3);
        }

        public void draw(Scene s, Map<String, Object> stats) {
            Camera camera = s.getCamera();
            Graphics2D gb = buffer.createGraphics();
            // set Antialiasing mode
            gb.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            gb.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            // clear buffer
            gb.setBackground(Color.BLACK);
            gb.clearRect(0, 0, 640, 400);

            if (Optional.ofNullable(camera).isPresent()) {
                gb.translate(-camera.x, -camera.y);
            }
            drawAllEntity(gb, s, false);

            if (Optional.ofNullable(s.getWorld()).isPresent() && debug > 1) {
                gb.setColor(Color.YELLOW);
                gb.draw(s.getWorld().getPlayArea());
            }

            if (Optional.ofNullable(camera).isPresent()) {
                gb.translate(camera.x, camera.y);
            }
            drawAllEntity(gb, s, true);
            s.draw(app, gb, stats);

            gb.dispose();

            // draw to screen.
            displayToWindow(stats);
        }

        private void draw(Map<String, Object> stats) {

        }

        /**
         * Draw all entities
         *
         * @param gb            the Graphics API
         * @param scn           Scene to be drawn.
         * @param stickToCamera define if entities must be moved with the {@link Camera} offset.
         */
        private void drawAllEntity(Graphics2D gb, Scene scn, boolean stickToCamera) {
            // draw all the platform game's scene.

            scn.getChild().stream()
                    .filter(Node::isActive)
                    .filter(o -> ((GameObject) o).getStickToCamera() == stickToCamera)
                    .forEach(o -> {
                        drawGameObject(gb, scn, (GameObject) o);
                        drawDebugInfo(gb, (GameObject) o);
                    });
        }

        /**
         * Draw the {@link GameObject} <code>o</code> from the scene <code>scn</code>
         * using the {@link Graphics2D} API <code>gb</code>.
         *
         * <p>The drawing method depends on the real nature  of the object (its class).</p>
         *
         * <blockquote><em><strong>TODO</strong> A possible evolution consists in replacing the specific internal implementation with an
         * extensible plugin pattern. See <a href="https://github.com/mcgivrer/Platform2D/issues/4">issue #4</a>.</em></blockquote>
         *
         * @param gb  the Graphics API
         * @param scn the {@link Scene} parent of the {@link GameObject}.
         * @param o   the {@link GameObject} to be drawn.
         */
        private <T extends GameObject> void drawGameObject(Graphics2D gb, Scene scn, GameObject o) {
            if (plugins.containsKey(o.getClass())) {
                plugins.get(o.getClass()).draw(gb, scn, o);
            }

            o.getBehaviors().forEach(b -> b.input(scn, o));
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
            if (debug > 0) {
                g.drawString(prepareStatsString(stats, "[ ", " ]", " | "),
                        16, frame.getHeight() - 16);
            }
            g.dispose();
            frame.getBufferStrategy().show();
        }

        /**
         * Draw visual debug information relative to a specific {@link GameObject} instance.
         *
         * @param gb the {@link Graphics2D} API instance to be used.
         * @param o  the {@link GameObject} to display visual debug information for.
         */
        private void drawDebugInfo(Graphics2D gb, GameObject o) {
            if (Optional.ofNullable(app.debugFilter).isPresent()
                    && (app.debugFilter.contains(o.name) || app.debugFilter.equals("all"))) {
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


        public BufferedImage getDrawBuffer() {
            return buffer;
        }

        public Dimension getBufferSize() {
            return bufferSize;
        }

        public void dispose() {
            frame.dispose();
        }
    }


    public static class GameObjectRenderPlugin implements RenderPlugin<GameObject> {

        @Override
        public void draw(Graphics2D gb, Scene scn, Node o) {
            GameObject go = (GameObject) o;
            gb.setColor(go.fillColor);
            gb.fill(go);
            gb.setColor(go.borderColor);
            gb.draw(go);
        }

        @Override
        public Class<GameObject> getObjectClass() {
            return GameObject.class;
        }
    }

    public static class TextObjectRenderPlugin implements RenderPlugin<TextObject> {
        @Override
        public void draw(Graphics2D gb, Scene scn, Node o) {
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

        @Override
        public Class<TextObject> getObjectClass() {
            return TextObject.class;
        }
    }

    public static class ImageObjectRenderPlugin implements RenderPlugin<ImageObject> {

        @Override
        public void draw(Graphics2D gb, Scene scn, Node o) {
            ImageObject io = (ImageObject) o;
            if (Optional.ofNullable(scn).isPresent()) {
                io.width = Math.min(scn.getWorld().playArea.getWidth() - io.x, io.getImage().getWidth());
                io.height = Math.min(scn.getWorld().playArea.getHeight() - io.y, io.getImage().getHeight());
            }
            gb.drawImage(io.getImage(), (int) io.x, (int) io.y, (int) io.width, (int) io.height, null);
        }

        @Override
        public Class<ImageObject> getObjectClass() {
            return ImageObject.class;
        }
    }

    public static class ConstraintObjectRenderPlugin implements RenderPlugin<ConstraintObject> {
        @Override
        public void draw(Graphics2D gb, Scene scn, Node o) {
            ConstraintObject co = (ConstraintObject) o;
            if (Optional.ofNullable(co.fillColor).isPresent()) {
                gb.setColor(co.fillColor);
                gb.fill(co);
            }
        }

        @Override
        public Class<ConstraintObject> getObjectClass() {
            return ConstraintObject.class;
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
        // load configuration
        initializeDefaultConfiguration();
        parseArguments(args);
        config = loadConfiguration(configurationFilePath);
        parseArguments(config);
        parseArguments(args);

        // set Content size
        setPreferredSize(screenSize);
        setMaximumSize(screenSize);
        setMinimumSize(screenSize);

        // create internal drawing buffer
        renderer = new Renderer(this);
        renderer.initialize(bufferSize, screenSize);

        // initialize Sound Management
        soundManager = new SoundManager(this);

        // prepare Scenes
        scnManager = new SceneManager(this);
        scnManager.load(strSceneList);
        if (!testMode && scnManager.getScenes().isEmpty()) {
            Scene start = new StartScene(this);
            setWorld(new World());
            scnManager.add(start);
        }
        // Activate the default scene
        scnManager.activate(defaultSceneName);
        logDebugSceneTreeNode((Node) scnManager.getActive(), 0);
    }

    /**
     * trace content of the Scene tree structure.
     *
     * @param node  the Starting point of the tree parsing
     * @param level the depth of parsing.
     */
    private void logDebugSceneTreeNode(Node node, int level) {
        if (node != null) {
            debug("%s|_ node:%s", "   ".repeat(level), node.getName());
            level += 1;
            for (Node n : node.getChild()) {
                debug("%s|_ node:%s", "   ".repeat(level), n.getName());
                if (!n.getChild().isEmpty()) {
                    logDebugSceneTreeNode(n, level);
                }
            }
        }
    }

    public static String getMessage(String key) {
        return messages.getString(key);
    }

    private void initializeDefaultConfiguration() {
        config.put("app.debug.level", "0");
        config.put("app.debug.filter", "");
        config.put("app.screen.size", "320x200");
        config.put("app.window.size", "640x400");
        config.put("app.scenes.default", "start");
        if (!testMode) {
            config.put("app.scenes.list", "start:com.snapgames.platform.PlatForm2D.StartScene,");
            config.put("app.test.mode", "false");
        }
    }

    protected void loadScenes() {
        scnManager.add(new DemoScene(this));
    }

    /**
     * Load configuration file.
     *
     * @param configPathFile the properties file.
     */
    private Properties loadConfiguration(String configPathFile) {
        try {
            config.load(Platform2D.class.getResourceAsStream(configPathFile));
            parseArguments(config);
        } catch (IOException e) {
            error("Unable to load file %s", configPathFile);
        }
        return config;
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
        if (!attributes.isEmpty()) {
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
                    case "app.test.mode", "test" -> this.testMode = Boolean.parseBoolean((String) value);
                    case "app.scenes.default", "default" -> this.defaultSceneName = (String) value;
                    case "app.scenes.list", "scenes" -> this.strSceneList = ((String) value).split(",");
                    default -> warn("Value entry unknown %s=%s", key, value);
                }
            }
        } else {
            error("Configuration is missing");
        }
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
    public static Object getResource(String path) {
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
     * Load and slice image according to the path and the following parameters (pattern [\x,y,width,height]).
     *
     * @param path the path to the resource with optional attribute.
     *             For image, it can be the slicing operation at [\x,y,width,height].
     */
    private static void loadAndOrSliceImageFrom(String path) {
        if (Optional.ofNullable(path).isPresent()) {
            try {
                if (path.contains("|")) {
                    //required to extract path or the image file.
                    String[] parts = path.substring(path.lastIndexOf("|") + 1).split(",");
                    int px = Integer.parseInt(parts[0]);
                    int py = Integer.parseInt(parts[1]);
                    int pw = Integer.parseInt(parts[2]);
                    int ph = Integer.parseInt(parts[3]);
                    String realPath = path.substring(0, path.lastIndexOf("|"));
                    BufferedImage img = ImageIO.read(Objects.requireNonNull(Platform2D.class.getResourceAsStream(realPath)));
                    resources.put(path, img.getSubimage(px, py, pw, ph));
                } else {
                    resources.put(path, ImageIO.read(Objects.requireNonNull(Platform2D.class.getResourceAsStream(path))));
                }
            } catch (IOException e) {
                error("Unable to read Image resource: %s", e.getMessage());
            }
        } else {
            error("resource path is null !");
        }
    }


    private void run(String[] args) {
        initialize(args);
        loop();
        dispose();
    }

    /**
     * The main game loop.
     */
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
            }
            if (cumulatedTime % 333 == 0) {
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
        Scene scn = getSceneManager().getActiveScene();
        List<GameObject> objects = (List<GameObject>) scn.getChild();
        long countActive = objects.stream()
                .filter(Node::isActive).count();
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
     * Retrieve the current {@link SceneManager} instance.
     *
     * @return the current {@link SceneManager} instance.
     */
    public SceneManager getSceneManager() {
        return scnManager;
    }

    /**
     * Process the inputs (keyboard)
     */
    private void input() {
        scnManager.input();
    }

    /**
     * Process all the scene's {@link GameObject} list update according to elapsed time.
     *
     * @param elapsed
     */
    void update(double elapsed) {
        Scene scn = getSceneManager().getActiveScene();

        scn.getChild().stream()
                // process only active and non-static objects
                .filter(o -> !((GameObject) o).isStaticObject() && o.isActive())
                .forEach(n -> {
                    GameObject o = (GameObject) n;
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

                    o.getBehaviors().forEach(b -> b.update(scn, elapsed, o));
                    o.update(elapsed);
                    keepGameObjectIntoPlayArea(world, o);
                    o.forces.clear();

                });

        if (Optional.ofNullable(scnManager.getActiveScene()).isPresent()) {
            Camera camera = scnManager.getActiveScene().getCamera();
            if (Optional.ofNullable(camera).isPresent()) {
                camera.update(elapsed);
            }
        }
        scnManager.update(stats, elapsed);
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
        renderer.draw(scnManager.getActiveScene(), stats);
    }

    /**
     * free all created/loaded resources.
     */
    void dispose() {
        if (Optional.ofNullable(scnManager).isPresent()) {
            scnManager.disposeAll();
        }
        if (Optional.ofNullable(renderer).isPresent()) {
            renderer.dispose();
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

        scnManager.keyPressed(e);
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
                    scnManager.activate(defaultSceneName);
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

            default -> {
                // Nothing to do !
            }
        }
        scnManager.keyReleased(e);
    }

    /**
     * Reset all objects from the current scene
     */
    private void resetScene() {
        scnManager.getActive().reset();
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
        if (Platform2D.debug > 0) {
            log("DEBUG", message, args);
        }
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

    public void setWorld(World world) {
        this.world = world;
    }

    public Renderer getRenderer() {
        return renderer;
    }

    /**
     * Return te current instance of the {@link SoundManager}
     *
     * @return the instance of {@link SoundManager}
     */
    public SoundManager getSoundManager() {
        return soundManager;
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
