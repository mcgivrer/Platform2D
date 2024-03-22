package com.snapgames.platform;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

public class Platform2DGameObjectManagementTest {

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

        app.addGameObject(new Platform2D.GameObject("go00"));

        Assertions.assertEquals("go00",
                app.objects.get(0).getName(),
                "GameObject 'go00' has not been added to the internal objects list");

        Assertions.assertEquals("go00",
                app.objectMap.get("go00").getName(),
                "GameObject 'go00' has not been put in the internal object map.");
    }

    @Test
    public void addMultiplePrioritizedGameObjectToSceneTest() {

        app.addGameObject(new Platform2D.GameObject("go01").setPriority(1));
        app.addGameObject(new Platform2D.GameObject("go02").setPriority(2));

        Assertions.assertEquals(2,
                app.objects.size(),
                "GameObjects 'go01','go02' have not been added to the internal objects list");

        Assertions.assertEquals("go01",
                app.objects.get(0).getName(),
                "GameObjects 'go01' has not been sorted into the internal objects list");

        Assertions.assertEquals("go02",
                app.objects.get(1).getName(),
                "GameObjects 'go02' has not been sorted into the internal objects list");
    }

}
