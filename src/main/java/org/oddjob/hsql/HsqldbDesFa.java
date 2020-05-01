/*
 * (c) Rob Gordon 2005.
 */
package org.oddjob.hsql;

import org.oddjob.arooa.design.*;
import org.oddjob.arooa.design.screem.BorderedGroup;
import org.oddjob.arooa.design.screem.Form;
import org.oddjob.arooa.design.screem.StandardForm;
import org.oddjob.arooa.parsing.ArooaContext;
import org.oddjob.arooa.parsing.ArooaElement;
import org.oddjob.designer.components.BaseDC;

/**
 *
 */
public class HsqldbDesFa implements DesignFactory {
	
	public DesignInstance createDesign(ArooaElement element,
			ArooaContext parentContext) {

		return new HsqldbDesign(element, parentContext);
	}
		
}

class HsqldbDesign extends BaseDC {

	private final MappedDesignProperty database;
	private final SimpleDesignProperty properties;

	public HsqldbDesign(ArooaElement element, ArooaContext parentContext) {
		super(element, parentContext);
		
		database = new MappedDesignProperty(
				"database", this);		
		
		properties  = new SimpleDesignProperty(
				"properties", this);
	}

	public DesignProperty[] children() {
		return new DesignProperty[] { name, database, properties };
	}
	
	public Form detail() {
		return new StandardForm(this)
			.addFormItem(basePanel())
			.addFormItem(new BorderedGroup("Configuration")
				.add(database.view().setTitle("Databases"))
				.add(properties.view().setTitle("Server Properties")));
	}
		
}
