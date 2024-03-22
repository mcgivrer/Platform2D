package com.snapgames.platform.demo.behaviors;

import com.snapgames.platform.Platform2D;
import com.snapgames.platform.Platform2D.GameObject;
import com.snapgames.platform.Platform2D.Node;
import com.snapgames.platform.Platform2D.Scene;

import java.awt.event.KeyEvent;

public class PlayerBehavior extends Platform2D.AbstractBehavior<GameObject> {


    @Override
    public void input(Scene s, Node n) {
        Platform2D app = s.getPlatform();
        GameObject player = (GameObject) n;

        double speed = (double) player.attributes.getOrDefault("speed", 0.5);
        if (app.isKeyPressed(KeyEvent.VK_UP)) {
            player.forces.add(new Platform2D.Vec2d(0, -speed));
        }
        if (app.isKeyPressed(KeyEvent.VK_DOWN)) {
            player.forces.add(new Platform2D.Vec2d(0, speed));
        }
        if (app.isKeyPressed(KeyEvent.VK_LEFT)) {
            player.forces.add(new Platform2D.Vec2d(-speed, 0));
        }
        if (app.isKeyPressed(KeyEvent.VK_RIGHT)) {
            player.forces.add(new Platform2D.Vec2d(speed, 0));
        }

        if (app.isKeyPressed(KeyEvent.VK_X)) {

        }
    }
}
