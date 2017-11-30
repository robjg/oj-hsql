/*
 * (c) Rob Gordon 2005
 */
package org.oddjob.hsql;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.hsqldb.Server;
import org.hsqldb.persist.HsqlProperties;
import org.hsqldb.server.ServerAcl.AclFormatException;

/**
 * @oddjob.description Start an instance of the HSQL 
 * Database engine. For more information on the properties please see
 * <a href="http://hsqldb.org">http://hsqldb.org</a>
 * 
 * @oddjob.example 
 * 
 * Using an HSQL Server and an HSQL connection to run
 * lots of SQL.
 * 
 * {@oddjob.xml.resource org/oddjob/hsql/HSQLExample.xml}
 * 
 * @oddjob.example 
 * 
 * Using an HSQL Server to provide a 
 * {@link org.oddjob.sql.SQLPersisterService}.
 * 
 * {@oddjob.xml.resource org/oddjob/hsql/OddjobPersisterService.xml}
 * 
 * @author Rob Gordon.
 */
public class HsqldbJob {

	private static final Logger logger = LoggerFactory.getLogger(HsqldbJob.class);
	
	class ServerWithStateNotify extends Server {
		synchronized protected void setState(int state) {
			super.setState(state);
				notifyAll();
		}
		
		@Override
		public String toString() {
			return "HsqldbServer";
		}
	}
	
	/** 
	 * @oddjob.property
	 * @oddjob.description A name, can be any text.
	 * @oddjob.required No. 
	 */
	private String name;
	
	/** 
	 * @oddjob.property database
	 * @oddjob.description A list of key/value pairs as a  
	 * which are the database name and the database path. 
	 * Hsqldb supports up to 10 databases per server instance.
	 * @oddjob.required No.
	 */
	private Map<String, String> databases = 
		new HashMap<String, String>();
	
	
	/** 
	 * @oddjob.property
	 * @oddjob.description The server hsql properties. For more information please
	 * see the hsqldb documentation.
	 * @oddjob.required No. 
	 */
	private Properties properties;
	
	/** The server */
	private Server server;
	
	/**
	 * Get the name.
	 * 
	 * @return The name.
	 */
	public String getName() {
		return name;
	}
	
	/**
	 * Set the name.
	 * 
	 * @param name The name.
	 */
	public void setName(String name) {
		this.name = name;
	}
	
	/**
	 * Get the database path for a give name.
	 * 
	 * @param name The database path.
	 * @return The database path.
	 */
	public String getDatabase(String name) {
		return databases.get(name);
	}
	
	/**
	 * Set the database name.
	 * 
	 * @param name The database name.
	 */
	public void setDatabase(String name, String path) {
		if (name == null) {
			return;
		}
		
		if (path == null) {
			this.databases.remove(name);
		}
		else {
			this.databases.put(name, path);
		}
	}
	
	/**
	 * Setter for server properties.
	 * 
	 * @param properties The property name.
	 * @param value The property value.
	 */
	public void setProperties(Properties properties) {
		this.properties = properties;
	}
	
	/**
	 * Getter for properties.
	 * 
	 * @param name The property name.
	 */
	public Properties getProperties() {
		return properties;
	}
	
	/**
	 * Start the server.
	 * @throws AclFormatException 
	 * @throws IOException 
	 *
	 */
	public void start() throws IOException, AclFormatException {

		// use a local reference, because stop() could in theory set
		// the member to null.			
		Server server = new ServerWithStateNotify();
		
		int dbCount = 0;
		for (Iterator<Map.Entry<String, String>> it = databases.entrySet().iterator(); it.hasNext(); ++dbCount) {
			Map.Entry<String, String> entry = it.next();
			server.setDatabaseName(dbCount, entry.getKey());
			server.setDatabasePath(dbCount, entry.getValue());
		}
		
		if (properties != null) {
			HsqlProperties hsqlProps = new HsqlProperties(properties);
			server.setProperties(hsqlProps);
		}
		
		// make the reference available for stop.
		this.server = server;
		
		logger.info("Starting " + server.getProductName() + 
				" Server, version " + server.getProductVersion());
		
		server.start();
		
		synchronized (server) {
			while (server.getState() == 4) { 
				try {
					logger.debug("Waiting for server to start...");
					server.wait();
				} catch (InterruptedException e) {
					break;
				}
			}
		}
		
		logger.debug("Started server, state=" + server.getState());
	}
	
	/**
	 * Stop the server.
	 *
	 */
	public void stop() {
		Server server = this.server;
		if (server == null) {
			logger.debug("Server not available. " +
					"(Not full started/Already Stopped)");
			return;
		}
		
		server.stop();
		
		synchronized (server) {
			while (server.getState() != 16) { 
				try {
					logger.debug("Waiting for server to shutdown...");
					server.wait(1000);
				} catch (InterruptedException e) {
					break;
				}
			}
			logger.debug("Shutdown server, state=" + server.getState());
		}
		this.server= null;
		this.properties = null;
		
		logger.debug("Server shutdown.");
	}
	
	/**
	 * @oddjob.property serverState
	 * @oddjob.description The current state of this server in numerically coded form.
	 * <p>
	 * Typically, this will be one of:
     * <ol>
     *  <li>ServerProperties.SERVER_STATE_ONLINE (1).</li>
     *  <li>ServerProperties.SERVER_STATE_OPENING (4).</li>
     *  <li>ServerProperties.SERVER_STATE_CLOSING (8).</li>
     *  <li>ServerProperties.SERVER_STATE_SHUTDOWN (16).</li>
     * </ol>
	 * @oddjob.required Read Only.
	 * @return
	 */
	public Integer getServerState() {
		Server server = this.server;
		if (server != null) {
			return new Integer(server.getState());
		}
		return null;
	}
	
	/*
	 *  (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		if (name == null) {
			return "Hsqldb server";
		}
		return name;
	}
}
