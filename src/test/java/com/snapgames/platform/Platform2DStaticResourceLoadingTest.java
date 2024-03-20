package com.snapgames.platform;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.awt.image.BufferedImage;

public class Platform2DStaticResourceLoadingTest {
    @Test
    public void getImageResource() {
        BufferedImage logoTest = (BufferedImage) Platform2D.getResource("/assets/images/sg-logo-image.png");
        Assertions.assertNotNull(logoTest, "Logo image resource has not been loaded");
        Assertions.assertEquals(39, logoTest.getWidth());
        Assertions.assertEquals(39, logoTest.getHeight());
    }
}
