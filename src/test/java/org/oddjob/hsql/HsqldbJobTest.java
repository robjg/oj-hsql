/*
 * (c) Rob Gordon 2005
 */
package org.oddjob.hsql;

import org.apache.commons.io.FileUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.oddjob.*;
import org.oddjob.arooa.convert.ArooaConversionException;
import org.oddjob.arooa.reflect.ArooaPropertyException;
import org.oddjob.arooa.xml.XMLConfiguration;
import org.oddjob.jobs.structural.SequentialJob;
import org.oddjob.state.JobState;
import org.oddjob.state.ParentState;
import org.oddjob.tools.ConsoleCapture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Properties;

public class HsqldbJobTest extends Assert {
    private static final Logger logger = LoggerFactory.getLogger(HsqldbJobTest.class);

    Path workDir;

    @Rule
    public TestName name = new TestName();

    @Before
    public void setUp() throws Exception {

        logger.info("-------------  " + name.getMethodName() + "  ---------------------");
        logger.info(this.getClass().getClassLoader().toString());

        workDir = OurDirs.workPathDir(getClass().getSimpleName() +
                "-" + name.getMethodName(), true);
    }

    @Test
    public void testStartStop() throws Exception {
        HsqldbJob test = new HsqldbJob();

        test.setDatabase("mydb", "file:" + workDir + "/test/hsql");

        Properties props = new Properties();
        props.setProperty("server.port", "11001");
        props.setProperty("server.silent", "false");

        test.setProperties(props);
        test.start();

        try {

            Class.forName("org.hsqldb.jdbcDriver");

            {
                Connection c = DriverManager.getConnection("jdbc:hsqldb:hsql://localhost:11001/mydb", "sa", "");

                Statement s = c.createStatement();
                s.executeUpdate("drop table test if exists\n" +
                        "create table test (greeting varchar(10))");
                s.executeUpdate("insert into test values ('hello')");
                s.close();
                c.close();
            }

            {
                Connection c = DriverManager.getConnection("jdbc:hsqldb:hsql://localhost:11001/mydb", "sa", "");

                Statement s = c.createStatement();
                ResultSet r = s.executeQuery("select * from test");
                r.next();
                assertEquals("hello", r.getString(1));
                s.close();
                c.close();
            }
        } finally {
            test.stop();
        }
    }

    @Test
    public void testExample() throws ArooaPropertyException, ArooaConversionException, FailedToStopException, IOException {

        File exampleDir = workDir.toFile();

        if (exampleDir.exists()) {
            FileUtils.forceDelete(exampleDir);
        }

        Properties properties = new Properties();
        properties.setProperty("work.dir", exampleDir.getPath());


        Oddjob oddjob = new Oddjob();
        oddjob.setConfiguration(new XMLConfiguration(
                "org/oddjob/hsql/HSQLExample.xml",
                getClass().getClassLoader()));
        oddjob.setProperties(properties);

        ConsoleCapture console = new ConsoleCapture();
        try (ConsoleCapture.Close ignored = console.captureConsole()) {

            oddjob.run();

            assertEquals(ParentState.STARTED, oddjob.lastStateEvent().getState());

            OddjobLookup lookup = new OddjobLookup(oddjob);

            // Setup

            SequentialJob setup = lookup.lookup("setup", SequentialJob.class);

            setup.run();

            assertEquals(ParentState.COMPLETE, setup.lastStateEvent().getState());

            // Single Query

            SequentialJob singleQuery = lookup.lookup("single-query", SequentialJob.class);

            singleQuery.run();

            assertEquals(ParentState.COMPLETE, singleQuery.lastStateEvent().getState());

            // Single Query

            SequentialJob allQuery = lookup.lookup("all-query", SequentialJob.class);

            allQuery.run();

            assertEquals(ParentState.COMPLETE, allQuery.lastStateEvent().getState());

            // Clean up

            Runnable cleanUp = lookup.lookup("clean-up", Runnable.class);

            cleanUp.run();

            assertEquals(JobState.COMPLETE, ((Stateful) cleanUp).lastStateEvent().getState());

            // done.
        }

        console.dump(logger);

        oddjob.stop();

        assertEquals(ParentState.COMPLETE, oddjob.lastStateEvent().getState());

        oddjob.destroy();
    }

    @Test
    public void testPersistExample() throws ArooaPropertyException, ArooaConversionException, FailedToStopException, IOException {

        File exampleDir = workDir.toFile();

        if (exampleDir.exists()) {
            FileUtils.forceDelete(exampleDir);
        }

        Properties properties = new Properties();
        properties.setProperty("work.dir", exampleDir.getPath());


        Oddjob oddjob = new Oddjob();
        oddjob.setConfiguration(new XMLConfiguration(
                "org/oddjob/hsql/OddjobPersisterService.xml",
                getClass().getClassLoader()));
        oddjob.setProperties(properties);

        ConsoleCapture console = new ConsoleCapture();
        try (ConsoleCapture.Close ignored = console.captureConsole()) {

            oddjob.run();

            //    	OddjobExplorer explorer = new OddjobExplorer();
            //    	explorer.setOddjob(oddjob);
            //    	explorer.run();
            //
            assertEquals(ParentState.STARTED, oddjob.lastStateEvent().getState());

            OddjobLookup lookup = new OddjobLookup(oddjob);

            Oddjob innerOddjob = lookup.lookup("database-persist-example",
                    Oddjob.class);

            assertEquals(ParentState.COMPLETE,
                    innerOddjob.lastStateEvent().getState());

            // Clean up

            Runnable cleanUp = lookup.lookup(
                    "clean-up", Runnable.class);

            cleanUp.run();

            assertEquals(JobState.COMPLETE, ((Stateful) cleanUp).lastStateEvent().getState());

            // done.
        }

        console.dump(logger);

        oddjob.stop();

        assertEquals(ParentState.COMPLETE, oddjob.lastStateEvent().getState());

        oddjob.destroy();
    }
}
