package com.snapgames.platform;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class Platform2DConfigurationTest {
    Platform2D app;

    @BeforeEach
    void setUp() {
        app = new Platform2D("Test Platform2D Configuration");
    }

    @AfterEach
    void tearDown() {
        app.dispose();
        app = null;
    }

    @Test
    public void defaultConfigTest() {
        app.initialize(new String[]{"cf=/no-config-test.properties", "app.test.mode=true"});
        Assertions.assertEquals(320, app.buffer.getWidth());
        Assertions.assertEquals(200, app.buffer.getHeight());
        Assertions.assertEquals(640, app.frame.getContentPane().getWidth());
        Assertions.assertEquals(400, app.frame.getContentPane().getHeight());
    }


    @Test
    public void loadSpecifiedConfigurationFileTest() {
        app.initialize(new String[]{"cf=/debug-config-test.properties", "app.test.mode=true"});

        Assertions.assertEquals(2, Platform2D.debug);
        Assertions.assertEquals("player,score", app.debugFilter);
        Assertions.assertEquals(320, app.buffer.getWidth());
        Assertions.assertEquals(200, app.buffer.getHeight());
        Assertions.assertEquals(640, app.frame.getContentPane().getWidth());
        Assertions.assertEquals(400, app.frame.getContentPane().getHeight());
    }

    @Test
    public void loadConfigurationFileTest() {
        app.initialize(new String[]{"cf=/debug-config-test.properties", "app.test.mode=true"});

        Assertions.assertEquals(2, Platform2D.debug);
        Assertions.assertEquals("player,score", app.debugFilter);
        Assertions.assertEquals(320, app.buffer.getWidth());
        Assertions.assertEquals(200, app.buffer.getHeight());
        Assertions.assertEquals(640, app.frame.getContentPane().getWidth());
        Assertions.assertEquals(400, app.frame.getContentPane().getHeight());
    }

    @Test
    public void loadDefaultConfigurationFileTest() {
        app.initialize(new String[]{"app.test.mode=true"});

        Assertions.assertEquals(0, Platform2D.debug);
        Assertions.assertEquals("", app.debugFilter);
        Assertions.assertEquals(320, app.buffer.getWidth());
        Assertions.assertEquals(200, app.buffer.getHeight());
        Assertions.assertEquals(640, app.frame.getContentPane().getWidth());
        Assertions.assertEquals(400, app.frame.getContentPane().getHeight());
    }

}