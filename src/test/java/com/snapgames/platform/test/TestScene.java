package com.snapgames.platform.test;

import com.snapgames.platform.Platform2D;

public class TestScene extends Platform2D.AbstractScene {
    public TestScene(Platform2D app) {
        super(app);
    }

    @Override
    public String getName() {
        return "test";
    }
}
