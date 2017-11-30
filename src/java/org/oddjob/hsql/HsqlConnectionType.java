/*
 * (c) Rob Gordon 2005
 */
package org.oddjob.hsql;

import java.io.Serializable;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.oddjob.arooa.types.ValueFactory;

/**
 * @oddjob.description Definition for a Database connection.
 * 
 * @oddjob.example
 * 
 * See {@link org.oddjob.sql.SQLJob} for an example.
 * 
 * @author Rob Gordon.
 */
public class HsqlConnectionType implements ValueFactory<Connection>, Serializable {
	private final static long serialVersionUID = 20070315;
	
	private static final Logger logger = LoggerFactory.getLogger(HsqlConnectionType.class);
		
	public static final String DRIVER = "org.hsqldb.jdbcDriver";
	
	/** 
	 * @oddjob.property
	 * @oddjob.description The jdbc url.
	 * @oddjob.required Yes. 
	 */
	private String url;
	
	/** 
	 * @oddjob.property
	 * @oddjob.description The database username..
	 * @oddjob.required No. 
	 */
	private String username;

	/** 
	 * @oddjob.property
	 * @oddjob.description The users password.
	 * @oddjob.required No. 
	 */
	private String password;

	public Connection toValue() {
		if (url == null) {
			throw new NullPointerException("Url must be provided.");
		}
				
		try {
			Class.forName(DRIVER);
		} catch (ClassNotFoundException e) {
			throw new RuntimeException(e);					
		}
		
		Properties info = new Properties();
		if (username != null) {
		    info.put("user", username);
		}
		if (password != null) {
		    info.put("password", password);
		}

		try {
			return DriverManager.getConnection(url, info);
		} catch (SQLException e) {					
			logger.warn("Failed creating connection to: " + url, e);
			for (SQLException ce = e.getNextException(); ce != null; 
					ce = ce.getNextException()) {
				logger.warn("Next chained exception:", ce);						
			}
			throw new RuntimeException(e);
		}
	}
	
	/**
	 * Get the password.
	 * 
	 * @return The password.
	 */
	public String getPassword() {
		return password;
	}

	/**
	 * Set the password.
	 * 
	 * @param password The password.
	 */
	public void setPassword(String password) {
		this.password = password;
	}

	/**
	 * Get the url.
	 * 
	 * @return The url.
	 */
	public String getUrl() {
		return url;
	}

	/**
	 * Set the url.
	 * 
	 * @param url The url
	 */
	public void setUrl(String url) {
		this.url = url;
	}

	/**
	 * Get the username.
	 * 
	 * @return The username.
	 */
	public String getUsername() {
		return username;
	}

	/**
	 * Set the username.
	 * 
	 * @param username The username.
	 */
	public void setUsername(String username) {
		this.username = username;
	}
	
	/*
	 * (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		return "Connection to " + url;
	}
}
