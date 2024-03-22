package com.snapgames.platform;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.awt.geom.Rectangle2D;
import java.util.Optional;

public class Platform2DPhysicProcessingTest {
    Platform2D platform;

    @BeforeEach
    public void setup() {
        platform = new Platform2D("Test Platform2D");
    }

    @AfterEach
    public void tearDown() {
        if (Optional.ofNullable(platform).isPresent()) {
            platform.dispose();
            platform = null;
        }
    }

    @Test
    public void testGameObjectsUpdatedAndMovedCorrectly() {

        platform.setWorld(
                new Platform2D.World(
                        new Platform2D.Vec2d(0, 0.981),
                        new Rectangle2D.Double(0, 0, 200, 200)));

        Platform2D.GameObject gameObject =
                new Platform2D.GameObject("go00", 100, 100, 50, 50)
                        .setVelocity(1, 1)
                        .setStaticObject(false)
                        .addForce(new Platform2D.Vec2d(0.5, 0.5));
        platform.addGameObject(gameObject);
        platform.update(16);

        Assertions.assertEquals(116.64, gameObject.x, 0.01);
        Assertions.assertEquals(117.89, gameObject.y, 0.01);

    }

    @Test
    public void testGameObjectsUpdatedAndMovedCorrectlyWithConstraintObject() {

        platform.setWorld(
                new Platform2D.World(
                        new Platform2D.Vec2d(0, 0.981),
                        new Rectangle2D.Double(0, 0, 200, 200)));

        Platform2D.ConstraintObject co = (Platform2D.ConstraintObject) new Platform2D.ConstraintObject("constraint")
                .setPosition(0.0, 0.0)
                .setSize(200, 100)
                .setMaterial(Platform2D.Material.WOOD);
        platform.getWorld().addConstraint(co);

        Platform2D.GameObject gameObject =
                new Platform2D.GameObject("go00", 100, 100, 50, 50)
                        .setVelocity(1, 1)
                        .setStaticObject(false)
                        .addForce(new Platform2D.Vec2d(0.5, 0.5));
        platform.addGameObject(gameObject);


        platform.update(16);

        Assertions.assertEquals(116.64, gameObject.x, 0.01);
        Assertions.assertEquals(117.89, gameObject.y, 0.01);

    }

}
