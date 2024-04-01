package com.snapgames.demo.behaviors;

import com.snapgames.platform.Platform2D;

public class EnemyBehavior extends Platform2D.AbstractBehavior<Platform2D.GameObject> {

    private final Platform2D.GameObject target;

    public EnemyBehavior(Platform2D.GameObject object) {
        this.target = object;
    }

    @Override
    public void update(Platform2D.Scene s, double elapsed, Platform2D.GameObject n) {
        Platform2D.Vec2d ePos = new Platform2D.Vec2d(n.x, n.y);
        Platform2D.Vec2d pPos = new Platform2D.Vec2d(target.x, target.y);

        double dist = pPos.distance(ePos);
        if (dist < 40.00) {
            n.addForce(new Platform2D.Vec2d(pPos.x, pPos.y).multiply(-0.05));
        }
    }

}
