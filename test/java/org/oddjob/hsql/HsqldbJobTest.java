/*
 * (c) Rob Gordon 2005
 */
package org.oddjob.hsql;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Properties;

import junit.framework.TestCase;

import org.apache.log4j.Logger;
import org.oddjob.OurDirs;

public class HsqldbJobTest extends TestCase {
	private static final Logger logger = Logger.getLogger(HsqldbJobTest.class);
	
	String workDir;

	@Override
	protected void setUp() throws Exception {

		logger.info("-------------  " + getName() + "  ---------------------");
		logger.info(this.getClass().getClassLoader().toString());
		
		OurDirs dirs = new OurDirs();
		File dir = dirs.relative("work/sql");
		dir.mkdirs();
		
		workDir = dir.getPath();
	}
	
	public void testStartStop() throws Exception {
		HsqldbJob test = new HsqldbJob();
		
		test.setDatabase("mydb", "file:" + workDir + "/hsql");
		
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
		
}
