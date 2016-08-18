package com.saic.uicds.core.infrastructure.util;

import javax.xml.namespace.QName;

import org.apache.xmlbeans.SchemaType;
import org.apache.xmlbeans.XmlCursor;
import org.apache.xmlbeans.XmlObject;
import org.apache.xmlbeans.XmlOptions;

public class XmlUtil {

    public static final XmlOptions normal = new XmlOptions().setSavePrettyPrint()
        .setLoadStripWhitespace();

    public static final XmlOptions innerOnly = new XmlOptions().setSavePrettyPrint().setSaveInner();

    public static final void substitute(XmlObject parentObject, String subNamespace,
        String subTypeName, SchemaType subSchemaType, XmlObject theObject) {

        XmlObject subObject = parentObject.substitute(new QName(subNamespace, subTypeName),
            subSchemaType);
        if (subObject != parentObject) {
            subObject.set(theObject);
        }
    }

	public static String getTextFromAny(XmlObject object) {
	    XmlCursor c = object.newCursor();
	    String text = c.getTextValue();
	    c.dispose();
	    return text;
	}

}
