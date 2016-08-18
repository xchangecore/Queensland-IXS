
package org.exist.xquery.modules.geom;

import org.exist.xquery.AbstractInternalModule;
import org.exist.xquery.FunctionDef;

/**
 * @author William Summers
 */
public class GeomModule extends AbstractInternalModule {

	public final static String NAMESPACE_URI = "http://exist-db.org/xquery/geom";

	public final static String PREFIX = "geom";
    public final static String INCLUSION_DATE = "2010-03-01";
    public final static String RELEASED_IN_VERSION = "eXist-1.4";

	private final static FunctionDef[] functions = {
		new FunctionDef(GeomFunctions.signature[0], GeomFunctions.class),
        new FunctionDef(GeomFunctions.signature[1], GeomFunctions.class),
        new FunctionDef(GeomFunctions.signature[2], GeomFunctions.class),
        new FunctionDef(GeomFunctions.signature[3], GeomFunctions.class),
        new FunctionDef(GeomFunctions.signature[4], GeomFunctions.class),
        new FunctionDef(GeomFunctions.signature[5], GeomFunctions.class)
    };

	public GeomModule() {
		super(functions);
	}

	public String getNamespaceURI() {
		return NAMESPACE_URI;
	}

	public String getDefaultPrefix() {
		return PREFIX;
	}

	public String getDescription() {
		return "A module for performing geometric functions on WKT formatted paramters.";
	}

    public String getReleaseVersion() {
        return RELEASED_IN_VERSION;
    }

}
