package com.saic.uicds.core.em.adminconsole.server.impl;

import org.apache.xmlbeans.XmlObject;
import org.apache.xmlbeans.XmlOptions;

public class Util {

    public final static int IdentSize = 4;

    public final static String getPrettyXmlFromBytes(byte[] bytes) {

        XmlObject xmlObject = toXmlObject(bytes);
        if (xmlObject == null)
            return null;

        return xmlObject.xmlText(new XmlOptions().setSavePrettyPrint().setSavePrettyPrintIndent(
            IdentSize));
    }

    public final static String getPrettyXmlFromString(String xmlString) {

        String content = null;
        try {
            XmlObject xmlObject = XmlObject.Factory.parse(xmlString);
            content = xmlObject.xmlText(new XmlOptions().setSavePrettyPrint().setSavePrettyPrintIndent(
                IdentSize));
        } catch (Exception e) {
            // TODO: handle exception
        }
        return content;
    }

    public final static XmlObject toXmlObject(byte[] bytes) {

        XmlObject xmlObject = null;
        try {
            xmlObject = XmlObject.Factory.parse(new String(bytes));
        } catch (Exception e) {
            e.printStackTrace();
        }

        return xmlObject;
    }
}