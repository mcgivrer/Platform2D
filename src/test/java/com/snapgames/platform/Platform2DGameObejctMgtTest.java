package com.snapgames.platform;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

public class Platform2DGameObejctMgtTest {
    Platform2D app;

    @BeforeEach
    void setup() {
        app = new Platform2D("Test Platform2D GameObject Management");
    }

    @AfterEach
    void tearDown() {
        if (Optional.ofNullable(app).isPresent()) {
            app.dispose();
            app = null;
        }
    }

    @Test
    public void addGameObjectToSceneTest() {

        app.initialize(new String[]{"cf=/empty-scene-test.properties"});
        Platform2D.Scene scn = app.getSceneManager().getActive();
        scn.addGameObject(new Platform2D.GameObject("go00"));
        Assertions.assertEquals("go00", scn.getChild().get(0).getName(), "GameObject 'go00' has not been added to the internal objects list");
        Assertions.assertEquals("go00", scn.getObject("go00").getName(), "GameObject 'go00' has not been put in the internal object map.");
    }

    @Test
    public void addMultiplePrioritizedGameObjectToSceneTest() {

        app.initialize(new String[]{"cf=/empty-scene-test.properties"});
        Platform2D.Scene scn = app.getSceneManager().getActiveScene();
        scn.addGameObject(new Platform2D.GameObject("go01").setPriority(1));
        scn.addGameObject(new Platform2D.GameObject("go02").setPriority(2));

        Assertions.assertEquals("go01", scn.getObject(0).getName(), "GameObjects 'go01' has not been sorted into the internal objects list");
        Assertions.assertEquals("go02", scn.getObject(1).getName(), "GameObjects 'go02' has not been sorted into the internal objects list");
    }


}
