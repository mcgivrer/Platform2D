package com.snapgames.platform;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JFrame;

public class Platform2D implements KeyListener {

  public static class GameObject extends Rectangle {
    private static long index = 0;
    private long id = index++;
    private String name = "gameobject_" + id;

    public GameObject(String name) {
      super();
      this.name = name;
    }

    public GameObject(String name, int x, int y, int w, int h) {
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

  BufferedImage buffer;
  JFrame frame;

  private String name;
  private Dimension bufferSize;
  private Dimension screenSize;

  private boolean exit;

  private boolean[] keys = new boolean[1024];

  private List<GameObject> objects = new ArrayList<>();
  private Map<String, GameObject> objectMap = new HashMap<>();

  private Rectangle playArea;

  Platform2D(String appName, Dimension bufferSize, Dimension screenSize) {
    this.name = appName;
    this.bufferSize = bufferSize;
    this.screenSize = screenSize;
    this.playArea = new Rectangle(0, 0, bufferSize.width, bufferSize.height);
  }

  private void initialize(String[] args) {
    buffer = new BufferedImage(bufferSize.width, bufferSize.height, BufferedImage.TYPE_INT_ARGB);
    frame = new JFrame("Platform2D");
    frame.setPreferredSize(screenSize);
    frame.setMaximumSize(screenSize);
    frame.setMinimumSize(screenSize);
    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    frame.pack();
    frame.setVisible(true);
    frame.addKeyListener(this);
    frame.createBufferStrategy(3);
    create();
  }

  private void create() {
    GameObject player = new GameObject(
        "player",
        bufferSize.width >> 1, bufferSize.height >> 1,
        16, 16);
    addGameObject(player);
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
    while (!exit) {
      input();
      update();
      draw();
    }
  }

  private void input() {
    GameObject player = objectMap.get("player");
    if (isKeyPressed(KeyEvent.VK_UP)) {
      player.y -= 2;
    }
    if (isKeyPressed(KeyEvent.VK_DOWN)) {
      player.y += 2;

    }
    if (isKeyPressed(KeyEvent.VK_LEFT)) {
      player.x -= 2;

    }
    if (isKeyPressed(KeyEvent.VK_RIGHT)) {
      player.x += 2;
    }

    if (isKeyPressed(KeyEvent.VK_X)) {

    }

  }

  private void update() {
    objects.forEach(o -> {
      if (playArea.intersects(o)) {
        if (o.x < 0) {
          o.x = 0;
        }
        if (o.x > playArea.width - o.width) {
          o.x = playArea.width - o.width;
        }
        if (o.y < 0) {
          o.y = 0;
        }
        if (o.y > playArea.height - o.width) {
          o.y = playArea.height - o.height;
        }
      }
    });
  }

  private void draw() {
    Graphics2D gb = buffer.createGraphics();
    // clear buffer
    gb.setBackground(Color.BLACK);
    gb.clearRect(0, 0, 640, 400);

    // draw all the platforme game scene.
    gb.setColor(Color.RED);
    objects.forEach(o -> {
      gb.fill(o);
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

  /** ---- key management ---- */

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

  public boolean isKeyPressed(int keyCode) {
    return keys[keyCode];
  }

  /** ---- Main entry ---- */
  public static void main(String[] args) {
    Platform2D platform2d = new Platform2D(
        "Platform2D",
        new Dimension(320, 200),
        new Dimension(640, 400));
    platform2d.run(args);
  }
}
