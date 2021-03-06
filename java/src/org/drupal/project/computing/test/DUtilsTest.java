package org.drupal.project.computing.test;

import org.apache.commons.exec.CommandLine;
import org.drupal.project.computing.DConfig;
import org.drupal.project.computing.DDrush;
import org.drupal.project.computing.DUtils;
import org.junit.Test;

import javax.script.Bindings;
import javax.script.SimpleBindings;
import java.io.File;
import java.util.Properties;
import java.util.logging.ConsoleHandler;
import java.util.logging.Logger;

import static junit.framework.Assert.*;

public class DUtilsTest {

    @Test
    public void testExecuteShell() throws Exception {
        String result = DUtils.getInstance().executeShell("echo Hello").trim();
        // System.out.println(result);
        assertEquals(result, "Hello");

        DUtils.getInstance().executeShell("touch /tmp/computing.test");
        assertTrue((new File("/tmp/computing.test")).exists());
        DUtils.getInstance().executeShell("rm /tmp/computing.test");
        assertTrue(!(new File("/tmp/computing.test")).exists());

        DUtils.getInstance().executeShell(CommandLine.parse("touch computing.test"), new File("/tmp"), null);
        assertTrue((new File("/tmp/computing.test")).exists());
        DUtils.getInstance().executeShell("rm /tmp/computing.test");
        assertTrue(!(new File("/tmp/computing.test")).exists());

        String msg = DUtils.getInstance().executeShell(CommandLine.parse("cat"), null, "hello\u0004");    // Ctrl+D, end of stream.
        assertEquals("hello", msg.trim());

        // this shows input stream automatically sends Ctrl+D.
        msg = DUtils.getInstance().executeShell(CommandLine.parse("cat"), null, "hello, world");
        assertEquals("hello, world", msg.trim());

            /*CommandLine commandLine = CommandLine.parse("drush @dev1");
           commandLine.addArgument("php-eval");
           commandLine.addArgument("echo 'Hello';", true);
           System.out.println(commandLine.toString());
           commandLine.addArgument("echo drupal_json_encode(eval('return node_load(1);'));", true);
           System.out.println(DUtils.getInstance().executeShell(commandLine, null));*/
    }


    @Test
    public void testDrush() throws Exception {
        // test drush version
        DDrush drush = DDrush.loadDefault();
        assertEquals("Expected drush version", "6.2.0", drush.getVersion());

        // test drupal version
        Bindings coreStatus = drush.getCoreStatus();
        System.out.println(coreStatus.toString());
        assertNotNull(coreStatus);
        String drupalVersion = (String) coreStatus.get("drupal-version");
        assertNotNull(drupalVersion);
        assertTrue("Expected drupal version", drupalVersion.startsWith("7"));

        // test computing-eval
        String jsonStr = drush.computingEval("return node_load(1);").trim();
        System.out.println(jsonStr);
        assertTrue(jsonStr.startsWith("{") && jsonStr.endsWith("}"));
        Bindings jsonObj = (Bindings) DUtils.Json.getInstance().fromJson(jsonStr);
        String nidStr = (String) jsonObj.get("nid");
        assertEquals("1", nidStr);
        assertEquals(new Integer(1), new Integer(nidStr));

        jsonStr = drush.computingEval("node_load(1);").trim();
        System.out.println(jsonStr);
        assertEquals("null", jsonStr);

        // test computing call
        String s2 = drush.computingCall(new String[]{"variable_get", DUtils.Json.getInstance().toJson("install_profile")});
        System.out.println(s2);
        assertEquals("standard", (String) DUtils.Json.getInstance().fromJson(s2));
        String s3 = drush.computingCall("variable_get", "install_profile");
        System.out.println(s3);
        assertEquals("standard", DUtils.Json.getInstance().fromJson(s3));

        // test exception
        try {
            drush.computingCall("hello");
            assertTrue(false);
        } catch (Exception e) {
            assertTrue("Expected exception caught.", true);
        }
        try {
            DDrush badDrush = new DDrush("drush", "@xxx");
            badDrush.computingCall("variable_get", "install_profile");
            assertTrue(false);
        } catch (Exception e) {
            assertTrue("Expected exception caught.", true);
        }
    }

//    @Test
//    public void testLocalDrush() throws Exception {
//        // test local environment
//        Drush drush = new Drush("drush @local");
//        Properties coreStatus = drush.getCoreStatus();
//        assertTrue(coreStatus.getProperty("drupal_version").startsWith("7"));
//        assertEquals("/Users/danithaca/Development/drupal7", coreStatus.getProperty("drupal_root"));
//        assertEquals("/Users/danithaca/Development/drupal7", drush.execute(new String[]{"drupal-directory", "--local"}).trim());
//    }

    @Test
    public void testLogger() {
        Logger l = DUtils.getInstance().getPackageLogger();
        assertTrue(l != null);
        assertTrue(l.getUseParentHandlers());
        assertEquals(0, l.getHandlers().length);

        while (true) {
            if (l.getParent() != null) {
                l = l.getParent();
            } else {
                break;
            }
        }

        // now l is the top level handler
        assertEquals(1, l.getHandlers().length);
        assertTrue(l.getHandlers()[0] instanceof ConsoleHandler);
    }


    @Test
    public void testJson() {
        Bindings jsonObj = new SimpleBindings();
        jsonObj.put("abc", 1);
        jsonObj.put("hello", "world");
        String jsonString = DUtils.Json.getInstance().toJson(jsonObj);
        Bindings json1 = (Bindings) DUtils.Json.getInstance().fromJson(jsonString);
        assertEquals(1, ((Number) json1.get("abc")).intValue());
        assertEquals("world", (String) json1.get("hello"));

        Bindings json2 = DUtils.Json.getInstance().fromJsonObject(jsonString);
        assertEquals("world", (String) json2.get("hello"));

        // produce error
        //Gson gson = new Gson();
        //Integer jsonObj = 1;
        //String jsonStr = gson.toJson(jsonObj);
        //System.out.println(jsonStr);
        // the library doesn't have fromJson(obj), so the library has no problem
        // we have a problem because we want to use fromJson(obj).
        //System.out.println(gson.toJson(gson.fromJson(jsonStr)));


            /*Bindings oldJson = new HashBindings();
            oldJson.put("hello", 1);
            String oldJsonString = DUtils.getInstance().toJson(oldJson);
            Bindings newJson = new SimpleBindings();
            newJson.put("hello", 1);
            newJson.put("abc", "def");
            String newJsonString = DUtils.getInstance().toJson(newJson);
            assertEquals(newJsonString, DUtils.getInstance().appendJsonString(oldJsonString, "abc", "def"));*/
    }


    @Test
    public void testMisc() {
        assertEquals(new Long(12L), DUtils.getInstance().getLong(new Long(12L)));
        assertEquals(new Long(5L), DUtils.getInstance().getLong("5"));
        assertEquals(new Long(100L), DUtils.getInstance().getLong(new Float(100.53)));

        // test tostring
        System.out.println(DUtils.getInstance().objectToString(new DConfig()));
        System.out.println(DUtils.getInstance().objectToString(1));

        Properties params = new Properties();
        params.put("abc", "hello world");
        params.put("def", "haha,haha");
        String encoded = DUtils.getInstance().encodeURLQueryParameters(params);
        System.out.println(encoded);
        assertEquals("abc=hello+world&def=haha%2Chaha", encoded);

        Bindings bi = new SimpleBindings();
        bi.put("k1", 1);
        bi.put("k2", "2");
        bi.put("k3", true);
        Properties p = DUtils.getInstance().bindingsToProperties(bi);
        assertEquals("1", p.getProperty("k1"));
        assertEquals("2", p.getProperty("k2"));
        assertEquals("true", p.getProperty("k3"));

        DConfig config = DConfig.loadDefault();
        System.out.println(config.getAgentName());

        assertEquals(true, DUtils.getInstance().getBoolean("true"));
        assertEquals(true, DUtils.getInstance().getBoolean("1"));
        assertEquals(false, DUtils.getInstance().getBoolean(null));
        assertEquals(false, DUtils.getInstance().getBoolean(""));
        assertEquals(false, DUtils.getInstance().getBoolean("0"));
        assertEquals(false, DUtils.getInstance().getBoolean("false"));
    }


    //@Test
//    public void testPhp() throws Exception {
//        String results;
//        DConfig config = new DConfig();
//        assertTrue(StringUtils.isNotBlank(config.getPhpExec()));
//
//        DUtils.Php php = new DUtils.Php();
//
//        // System.out.println(DUtils.getInstance().executeShell("php -v"));
//
//        results = php.evaluate("<?php echo 'hello, world';");
//        assertEquals("hello, world", results.trim());
//        results = php.evaluate("<?php echo json_encode(100);");
//        assertEquals(new Integer(100), new Gson().fromJson(results, Integer.class));
//
//        // try to get $databases from settings.php.
//        config.setProperty("drupal.drush", "drush @local");
//        File settingsFile = config.locateFile("settings.php");
//        String databasesCode = php.extractVariable(settingsFile, "$databases");
//        assertTrue(databasesCode.startsWith("$databases"));
//
//        // test serialization
//        byte[] serialized;
//        // serialized = php.serialize(new Integer[] {1, 3}))
//        serialized = php.serialize("hello, world");
//        //System.out.println(new String(serialized));
//        assertEquals("hello, world", php.unserialize(serialized, String.class));
//        assertEquals(new Integer(1), php.unserialize(php.serialize(1), Integer.class));
//    }

    /**
     * This method is not supposed to test anything. It's simply running to print out some stuff.
     */
    //@Test
//    public void simplePrint() throws Exception {
//        DConfig config = new DConfig();
//        config.setProperty("drupal.drush", "drush @local");
//        File settingsFile = config.locateFile("settings.php");
//        //String settingsCode = DUtils.getInstance().readContent(new FileReader(settingsFile));
//        System.out.println("Settings.php: " + settingsFile.getAbsolutePath());
//
//        DUtils.Php php = new DUtils.Php(config.getPhpExec());
//        System.out.println(php.extractVariable(settingsFile, "$databases"));
//        //System.out.println(DUtils.getInstance().stripPhpComments(settingsCode));
//    }

    //@Test
//    public void testXmlrpc() throws Exception {
//        DUtils.Xmlrpc xmlrpc = new DUtils.Xmlrpc("http://rgb.knowsun.com/x");
//        //Xmlrpc xmlrpc = new Xmlrpc("http://d7dev1.localhost/xmlrpc");
//        Map result;
//        result = xmlrpc.connect();
//        assertTrue(((String)result.get("sessid")).length() > 0);
//
//        result = xmlrpc.login("test", "test");
//        assertTrue(result.size() > 0);
//        System.out.println(result);
//
//        result = (Map) xmlrpc.execute("node.retrieve", 1);
//        assertTrue(result.size() > 0);
//        System.out.println(result);
//
//        assertTrue(xmlrpc.logout());
//    }

}


