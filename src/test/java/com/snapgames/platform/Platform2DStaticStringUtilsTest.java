package com.snapgames.platform;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

class Platform2DStaticStringUtilsTest {
    @Test
    void prepareStatsStringTest() {
        Map<String, Object> stringObjectMap = new HashMap<>(5);
        stringObjectMap.put("2:debug", 1);
        stringObjectMap.put("1:fps", 123.2);
        stringObjectMap.put("3:mode", "fix");

        String toDisplay = Platform2D.prepareStatsString(stringObjectMap, "[ ", " ]", " | ");

        Assertions.assertEquals("[ fps:123,20 | debug:    1 | mode:fix ]", toDisplay, "The Map has not been converted to an ordered string");
    }

    @Test
    void formatDurationWithoutMS() {
        long time = 1000 * 125;
        String strTime = Platform2D.formatDuration(time, false);
        Assertions.assertEquals("00:02:05", strTime, "Unable to format time in ms to a String without ms");
    }

    @Test
    void formatDurationWithMS() {
        long time = (1000 * 125) + 103;
        String strTime = Platform2D.formatDuration(time, true);
        Assertions.assertEquals("00:02:05.103", strTime, "Unable to format time in ms to a String with ms");
    }

    @Test
    void formatDurationDaysWithoutMS() {
        long time = (1000 * 3600 * 24 * 3) + (1000 * 125);
        String strTime = Platform2D.formatDuration(time, false);
        Assertions.assertEquals("3 d - 00:02:05", strTime, "Unable to format time in ms to a String without ms");
    }

    @Test
    void formatDurationDaysWithMS() {
        long time = (1000 * 3600 * 24 * 3) + (1000 * 3600 * 5) + (1000 * 125) + 103;
        String strTime = Platform2D.formatDuration(time, true);
        Assertions.assertEquals("3 d - 05:02:05.103", strTime, "Unable to format time in ms to a String with ms");
    }
}