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
                    .toString());
        } finally {
            SLog.setEnabled(true);
        }
    }

    @Test
    public void testStaticUtilsVarArgs() {
        String expected = "$SPG$+T=Coffee Time;+O=selectCupSize;+M=S;";
        Assert.assertEquals(expected, SLogCall.log("Coffee Time", "selectCupSize", null));

        expected = "$SPG$+T=Coffee Time;+O=selectCupSize;+M=S;_MISSING_KEY_0=null;";
        Assert.assertEquals(expected, SLogCall.log("Coffee Time", "selectCupSize", null, null));

        expected = "$SPG$+T=Coffee Time;+O=selectCupSize;+M=S;";
        Assert.assertEquals(expected, SLogCall.log("Coffee Time", "selectCupSize", "size"));

        expected = "$SPG$+T=Coffee Time;+O=selectCupSize;+M=S;size=null;";
        Assert.assertEquals(expected, SLogCall.log("Coffee Time", "selectCupSize", "size", null));

        expected = "$SPG$+T=Coffee Time;+O=selectCupSize;+M=S;_MISSING_KEY_0=large;";
        Assert.assertEquals(expected, SLogCall.log("Coffee Time", "selectCupSize", null, "large"));

        expected = "$SPG$+T=Coffee Time;+O=selectCupSize;+M=S;_MISSING_KEY_0=large;";
        Assert.assertEquals(expected, SLogCall.log("Coffee Time", "selectCupSize", null, "large", "newkey"));

        expected = "$SPG$+T=Coffee Time;+O=selectCupSize;+M=S;size=large;";
        Assert.assertEquals(expected, SLogCall.log("Coffee Time", "selectCupSize", "size", "large"));

    }

    @Test
    public void testStaticUtils() {
        String expected = "$SPG$+T=Coffee Time;+O=selectCupSize;+M=S;size=large;";
        Assert.assertEquals(expected, SLogCall.log("Coffee Time", "selectCupSize", "size", "large"));

        expected = "$SPG$+T=Coffee Time;+O=selectCupSize;+M=S;";
        Assert.assertEquals(expected, SLogCall.log("Coffee Time", "selectCupSize"));

        expected = "$SPG$+T=Coffee Time;+O=selectCupSize;+M=A;size=large;";
        Assert.assertEquals(expected, SLogStart.log("Coffee Time", "selectCupSize", "size", "large"));

        expected = "$SPG$+T=Coffee Time;+O=selectCupSize;+M=A;";
        Assert.assertEquals(expected, SLogStart.log("Coffee Time", "selectCupSize"));

        expected = "$SPG$+T=Coffee Time;+O=selectCupSize;+M=F;size=large;";
        Assert.assertEquals(expected, SLogStop.log("Coffee Time", "selectCupSize", "size", "large"));

        expected = "$SPG$+T=Coffee Time;+O=selectCupSize;+M=F;";
        Assert.assertEquals(expected, SLogStop.log("Coffee Time", "selectCupSize"));

        expected = "$SPG$+T=Coffee Time;+O=selectCupSize;+M=S;+MC=1;size=large;";
        Assert.assertEquals(expected, SLogBroadcastSend.log("Coffee Time", "selectCupSize", "size", "large"));

        expected = "$SPG$+T=Coffee Time;+O=selectCupSize;+M=S;+MC=1;";
        Assert.assertEquals(expected, SLogBroadcastSend.log("Coffee Time", "selectCupSize"));

        expected = "$SPG$+T=Coffee Time;+O=bcastId;+M=A;+OA=selectCupSize;size=large;";
        Assert.assertEquals(expected, SLogBroadcastStart.log("Coffee Time", "bcastId", "selectCupSize","size", "large"));

        expected = "$SPG$+T=Coffee Time;+O=bcastId;+M=A;+OA=selectCupSize;";
        Assert.assertEquals(expected, SLogBroadcastStart.log("Coffee Time", "bcastId", "selectCupSize"));

        expected = "$SPG$+T=Coffee Time;+O=bcastId;+M=F;+OA=selectCupSize;size=large;";
        Assert.assertEquals(expected, SLogBroadcastStop.log("Coffee Time", "bcastId", "selectCupSize","size", "large"));

        expected = "$SPG$+T=Coffee Time;+O=bcastId;+M=F;+OA=selectCupSize;";
        Assert.assertEquals(expected, SLogBroadcastStop.log("Coffee Time", "bcastId", "selectCupSize"));
    }

    @Test
    public void testSunnyDay() {
        String expected = "$SPG$+T=Coffee Time;+O=selectCupSize;+M=S;size=large;";
        Assert.assertEquals(expected, new SLogCall("Coffee Time", "selectCupSize")
                .withUserData("size", "large").toString());

        Map<String, String> userData = new HashMap<String, String>();
        userData.put("size", "large");
        Assert.assertEquals(expected, new SLogCall("Coffee Time", "selectCupSize")
                .withUserData(userData).toString());
        
        expected = "$SPG$+T=Coffee Time;+O=selectCupSize;+M=S;size=large;size1=large;size2=large;size3=large;size4=large;size5=large;";
        Assert.assertEquals(expected, new SLogCall("Coffee Time", "selectCupSize")
                .withUserData("size", "large")
                .withUserData("size1", "large")
                .withUserData("size2", "large")
                .withUserData("size3", "large")
                .withUserData("size4", "large")
                .withUserData("size5", "large").toString());
    }

    @Test
    public void testOptionalParams() {
        String expected = "$SPG$+T=Coffee Time;+O=pumpWater;+M=A;+I^=100ms;";
        Assert.assertEquals(expected, new SLogStart("Coffee Time", "pumpWater")
                .withInstrumentationOverride(100, SLog.TimeNotation.MILLIS)
                .toString());

        expected = "$SPG$+T=Coffee Time;+O=coffeeComplete;+M=F;+OA=ensureCapacity;+C^=WaterReservoir;";
        Assert.assertEquals(expected, new SLogStop("Coffee Time", "coffeeComplete")
                .withOperationAlias("ensureCapacity")
                .withComponentOverride("WaterReservoir")
                .toString());
    }

    @Test
    public void testMissingParams() {
        String expected = "$SPG$+T=_MISSING_TASK_;+O=_MISSING_OPERATION_;+M=S;";
        Assert.assertEquals(expected, new SLog(null, null, null)
                .toString());

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
                .toString());

    }
}
