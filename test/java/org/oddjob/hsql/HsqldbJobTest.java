/*
 * (c) Rob Gordon 2005
 */
package org.oddjob.hsql;

import java.beans.PropertyVetoException;
import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Properties;

import junit.framework.TestCase;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.oddjob.ConsoleCapture;
import org.oddjob.FailedToStopException;
import org.oddjob.Oddjob;
import org.oddjob.OddjobLookup;
import org.oddjob.OurDirs;
import org.oddjob.Stateful;
import org.oddjob.arooa.convert.ArooaConversionException;
import org.oddjob.arooa.reflect.ArooaPropertyException;
import org.oddjob.arooa.xml.XMLConfiguration;
import org.oddjob.jobs.structural.SequentialJob;
import org.oddjob.state.JobState;
import org.oddjob.state.ParentState;

public class HsqldbJobTest extends TestCase {
	private static final Logger logger = Logger.getLogger(HsqldbJobTest.class);
	
	String workDir;

	@Override
	protected void setUp() throws Exception {

		logger.info("-------------  " + getName() + "  ---------------------");
		logger.info(this.getClass().getClassLoader().toString());
		
		OurDirs dirs = new OurDirs();
		File dir = dirs.relative("work");
		
		dir.mkdirs();
		
		workDir = dir.getPath();
	}
	
	public void testStartStop() throws Exception {
		HsqldbJob test = new HsqldbJob();
		
		test.setDatabase("mydb", "file:" + workDir + "/test/hsql");
		
		Properties props = new Properties();
		props.setProperty("server.port", "11001");
		props.setProperty("server.silent", "false");
		
		test.setProperties(props);
		test.start();

		try {

			Class.forName("org.hsqldb.jdbcDriver" );

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
		}
		finally { 	
			test.stop();
		}
	}
		
	public void testExample() throws ArooaPropertyException, ArooaConversionException, FailedToStopException, IOException {
		
		File exampleDir = new File(workDir, "example");
		
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
    	console.capture(Oddjob.CONSOLE);
    	
    	oddjob.run();

    	assertEquals(ParentState.ACTIVE, oddjob.lastStateEvent().getState());
    	
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
    	
    	console.close();
    		
    	console.dump(logger);
    
    	oddjob.stop();
    	
    	assertEquals(ParentState.COMPLETE, oddjob.lastStateEvent().getState());
    	
    	oddjob.destroy();
	}
	
	public void testPersistExample() throws ArooaPropertyException, ArooaConversionException, FailedToStopException, IOException, PropertyVetoException {
		
		File exampleDir = new File(workDir, "persist");
		
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
    	console.capture(Oddjob.CONSOLE);
    	
    	oddjob.run();

//    	OddjobExplorer explorer = new OddjobExplorer();
//    	explorer.setOddjob(oddjob);
//    	explorer.run();
//    	
    	assertEquals(ParentState.ACTIVE, oddjob.lastStateEvent().getState());
    	
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
    	
    	console.close();
    		
    	console.dump(logger);
    
    	oddjob.stop();
    	
    	assertEquals(ParentState.COMPLETE, oddjob.lastStateEvent().getState());
    	
    	oddjob.destroy();
	}
}
