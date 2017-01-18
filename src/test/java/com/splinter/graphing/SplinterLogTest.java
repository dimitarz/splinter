package com.splinter.graphing;

import org.junit.Assert;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

public class SplinterLogTest {

    @Test
    public void testSunnyDay() {
        String expected = "$SPG$_T=file opened;_R=1;_O=open;rsr=/Users/dimitarz/filename.log;";
        Assert.assertEquals(expected, new SplinterLog("file opened", "1", "open")
                .withUserData("rsr", "/Users/dimitarz/filename.log").toString());

        Map<String, String> userData = new HashMap<String, String>();
        userData.put("rsr", "/Users/dimitarz/filename.log");
        Assert.assertEquals(expected, new SplinterLog("file opened", "1", "open")
                .withUserData(userData).build());
    }

    @Test
    public void testOptionalParams() {
        String expected = "$SPG$_T=display graph;_R=7;_I^=2001μs;";
        Assert.assertEquals(expected, new SplinterLog("display graph", "7")
                .withInstrumentationOverride(2001, SplinterLog.TimeNotation.MICROS)
                .build());

        expected = "$SPG$_T=display graph;_R=7;_C^=OtherComp;";
        Assert.assertEquals(expected, new SplinterLog("display graph", "7")
                .withComponentOverride("OtherComp")
                .build());
    }

    @Test
    public void testMissingParams() {
        String expected = "$SPG$_T=_MISSING_TASK_;_R=1;_O=open;rsr=/Users/dimitarz/filename.log;";
        Assert.assertEquals(expected, new SplinterLog(null, "1", "open")
                .withUserData("rsr", "/Users/dimitarz/filename.log")
                .build());

        expected = "$SPG$_T=_MISSING_TASK_;_R=_MISSING_REQUEST_;_O=;rsr=/Users/dimitarz/filename.log;";
        Assert.assertEquals(expected, new SplinterLog("", "", "")
                .withUserData("rsr", "/Users/dimitarz/filename.log")
                .build());

        expected = "$SPG$_T=_MISSING_TASK_;_R=_MISSING_REQUEST_;_O=;_MISSING_KEY_0=/Users/dimitarz/filename.log;";
        Assert.assertEquals(expected, new SplinterLog("", "", "")
                .withUserData(null, "/Users/dimitarz/filename.log")
                .build());
    }

    @Test
    public void testEscaping() {
        Assert.assertEquals("abcd", SplinterLog.escape("abcd"));
        Assert.assertEquals("ab\\ncd", SplinterLog.escape("ab\ncd"));
        Assert.assertNull(SplinterLog.escape(null));
        Assert.assertEquals("", SplinterLog.escape(""));
        Assert.assertEquals("ab\\=cd", SplinterLog.escape("ab=cd"));
        Assert.assertEquals("ab\\;cd", SplinterLog.escape("ab;cd"));
        Assert.assertEquals("ab\\\\cd", SplinterLog.escape("ab\\cd"));
    }

    @Test
    public void testEscapingLog() {
        String expected = "$SPG$_T=file\\; opened;_R=\\=1;_O=\\\\open;r\\=sr=/Users/dimitarz/\\;filename.log;";
        Assert.assertEquals(expected, new SplinterLog("file; opened")
                .withUserData("r=sr", "/Users/dimitarz/;filename.log")
                .withOperation("\\open")
                .withRequestId("=1")
                .withTask("file; opened")
                .build());

    }
}
