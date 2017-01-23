package com.splinter.graphing;

import org.junit.Assert;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

public class SplinterLogTest {
    @Test
    public void testDisableLogs() {
        try {
            SLog.setEnabled(false);
            String expected = "";
            Assert.assertEquals(expected, new SLogStop("Coffee Time", "coffeeComplete")
                    .withOperationAlias("ensureCapacity")
                    .withComponentOverride("WaterReservoir")
                    .withUserData("size", "large")
                    .withInstrumentationOverride(0, null)
                    .build());
        } finally {
            SLog.setEnabled(true);
        }
    }
    @Test
    public void testSunnyDay() {
        String expected = "$SPG$+T=Coffee Time;+O=selectCupSize;+M=S;size=large;";
        Assert.assertEquals(expected, new SLogCall("Coffee Time", "selectCupSize")
                .withUserData("size", "large").toString());

        Map<String, String> userData = new HashMap<String, String>();
        userData.put("size", "large");
        Assert.assertEquals(expected, new SLogCall("Coffee Time", "selectCupSize")
                .withUserData(userData).build());
    }

    @Test
    public void testOptionalParams() {
        String expected = "$SPG$+T=Coffee Time;+O=pumpWater;+M=A;+I^=100ms;";
        Assert.assertEquals(expected, new SLogStart("Coffee Time", "pumpWater")
                .withInstrumentationOverride(100, SLog.TimeNotation.MILLIS)
                .build());

        expected = "$SPG$+T=Coffee Time;+O=coffeeComplete;+M=F;+OA=ensureCapacity;+C^=WaterReservoir;";
        Assert.assertEquals(expected, new SLogStop("Coffee Time", "coffeeComplete")
                .withOperationAlias("ensureCapacity")
                .withComponentOverride("WaterReservoir")
                .build());
    }

    @Test
    public void testMissingParams() {
        String expected = "$SPG$+T=_MISSING_TASK_;+O=_MISSING_OPERATION_;+M=S;";
        Assert.assertEquals(expected, new SLog(null, null, null)
                .build());

        expected = "$SPG$+T=Coffee Time;+O=selectCupSize;+M=S;_MISSING_KEY_0=large;";
        Assert.assertEquals(expected, new SLogCall("Coffee Time", "selectCupSize")
                .withUserData(null, "large").toString());
    }

    @Test
    public void testEscaping() {
        Assert.assertEquals("abcd", SLog.escape("abcd"));
        Assert.assertEquals("ab\\ncd", SLog.escape("ab\ncd"));
        Assert.assertNull(SLog.escape(null));
        Assert.assertEquals("", SLog.escape(""));
        Assert.assertEquals("ab\\=cd", SLog.escape("ab=cd"));
        Assert.assertEquals("ab\\;cd", SLog.escape("ab;cd"));
        Assert.assertEquals("ab\\\\cd", SLog.escape("ab\\cd"));
    }

    @Test
    public void testEscapingLog() {
        String expected = "$SPG$+T=file\\; opened;+O=\\\\open;+M=S;+OA=\\=1;r\\=sr=/Users/dimitarz/\\;filename.log;";
        Assert.assertEquals(expected, new SLog(null, null, null)
                .withUserData("r=sr", "/Users/dimitarz/;filename.log")
                .withOperation("\\open")
                .withOperationAlias("=1")
                .withTask("file; opened")
                .build());

    }
}
