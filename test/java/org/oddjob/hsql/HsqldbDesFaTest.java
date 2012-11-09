package org.oddjob.hsql;

import junit.framework.TestCase;

import org.apache.commons.beanutils.DynaBean;
import org.oddjob.Helper;
import org.oddjob.OddjobDescriptorFactory;
import org.oddjob.arooa.ArooaDescriptor;
import org.oddjob.arooa.ArooaParseException;
import org.oddjob.arooa.ArooaType;
import org.oddjob.arooa.design.DesignInstance;
import org.oddjob.arooa.design.DesignParser;
import org.oddjob.arooa.design.view.ViewMainHelper;
import org.oddjob.arooa.standard.StandardArooaSession;
import org.oddjob.arooa.xml.XMLConfiguration;

public class HsqldbDesFaTest extends TestCase {

	DesignInstance design;
	
	public void testCreate() throws ArooaParseException {
		
		String xml =  
			"<hsql:hsqldb xmlns:hsql='http://rgordon.co.uk/oddjob/hsql' " +
			"            name='A Test'" +
			"		     id='this'" +
			"                  >" +
			"   <database>" +
			"    <value key='mydb' value='file:work/sql/hsql'/>" +
			"   </database>" +
			"   <properties>" +
			"    <properties>" +
			"     <values>" +
			"      <value key='server.port' value='11001'/>" +
			"      <value key='server.silent' value='false'/>" +
			"     </values>" +
			"    </properties>" +
			"   </properties>" +
			"</hsql:hsqldb>";
		
    	ArooaDescriptor descriptor = 
    		new OddjobDescriptorFactory().createDescriptor(null);
    	
		DesignParser parser = new DesignParser(
				new StandardArooaSession(descriptor));
		parser.setArooaType(ArooaType.COMPONENT);
		
		parser.parse(new XMLConfiguration("TEST", xml));
		
		design = parser.getDesign();
		
		assertEquals(HsqldbDesign.class, design.getClass());
		
		DynaBean test = (DynaBean) Helper.createComponentFromConfiguration(
				design.getArooaContext().getConfigurationNode());
		
		assertEquals("file:work/sql/hsql", test.get("database", "mydb"));
		
	}

	public static void main(String args[]) throws ArooaParseException {

		HsqldbDesFaTest test = new HsqldbDesFaTest();
		test.testCreate();
		
		ViewMainHelper view = new ViewMainHelper(test.design);
		view.run();
	}
}
