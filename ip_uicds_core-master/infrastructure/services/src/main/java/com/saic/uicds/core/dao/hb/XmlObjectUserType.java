package com.saic.uicds.core.dao.hb;

import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

import org.apache.xmlbeans.XmlException;
import org.apache.xmlbeans.XmlObject;
import org.hibernate.HibernateException;
import org.hibernate.usertype.UserType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class XmlObjectUserType implements UserType {

    Logger log = LoggerFactory.getLogger(XmlObjectUserType.class);

     
 	@Override
	public int[] sqlTypes() {
		return new int[] { Types.VARCHAR };
	}
   
	@Override
	@SuppressWarnings("unchecked")
	public Class<XmlObject> returnedClass() {		
		return XmlObject.class;
	}
	
	@Override
	public Object assemble(Serializable cached, Object owner)
			throws HibernateException {
		if (log.isDebugEnabled())
			log.debug("assemble");
		//return deepCopy(cached);
		return cached;
	}

	@Override
	public Object deepCopy(Object value) throws HibernateException {
		if (log.isDebugEnabled())
			log.debug("deepCopy");		
		//log.debug("object value="+value);
		if (value == null)
			return null;
		XmlObject xml=null;
		String s = value.toString();
        if ((s != null) && !(s.equals(""))) {
        	try {
				xml = XmlObject.Factory.parse(s);
			} catch (XmlException e) {
				log.error("deepCopy. Error:"+e.getMessage());
				e.printStackTrace();
			}
        }
        
		return xml;

	}

	@Override
	public Serializable disassemble(Object value) throws HibernateException {
		if (log.isDebugEnabled())
			log.debug("disassemble");
		//return (Serializable) deepCopy(value);
		return (Serializable) value;
	}

	@Override
	public boolean equals(Object x, Object y) throws HibernateException {
		if (log.isDebugEnabled())
			log.debug("equal");
		String xmlStr1 = (x != null)? x.toString(): "";
		String xmlStr2 = (y != null)? y.toString(): "";

		//log.debug("object x ="+xmlStr1);
		//log.debug("object y ="+xmlStr2);

    	boolean result = xmlStr1.equals(xmlStr2);
    	if (log.isDebugEnabled())
    		log.debug("equal="+result);
        return result;
	}

	@Override
	public int hashCode(Object x) throws HibernateException {
		return x.hashCode();
	}

	@Override
	public boolean isMutable() {		
		boolean checkUpdate = true;
    	if (log.isDebugEnabled())
    		log.debug("isMutable - "+ checkUpdate);
		return checkUpdate;
	}

	@Override
	public Object nullSafeGet(ResultSet rs, String[] names, Object owner)
			throws HibernateException, SQLException {
		String s = rs.getString(names[0]);
		if (log.isDebugEnabled())
			log.debug("nullSafeGet");
			//log.debug("nullSafeGet. String s="+s);
		XmlObject xml =null;
		
        if ((s != null) && !(s.equals(""))) {
        	try {
				xml = XmlObject.Factory.parse(s);
			} catch (XmlException e) {
				log.error("nullSafeGet. Error:"+e.getMessage());
				e.printStackTrace();
			}
        }
        return xml;
	}

	@Override
	public void nullSafeSet(PreparedStatement statement, Object value, int index)
			throws HibernateException, SQLException {
		/*
        if (value == null) {
        	log.debug("nullSafeSet - setNull");
            statement.setNull(index, sqlTypes()[0]);
        } else {
        */
		// do not set null, because ms sql will treat it differently.
        
    	String str = (value != null)? ((XmlObject) value).toString() : "";
    	if (log.isDebugEnabled())
    		log.debug("nullSafeSet");
    		//log.debug("nullSafeSet - setString="+str+"\n");
        statement.setString(index, str);
	}

	@Override
	public Object replace(Object original, Object target, Object owner)
			throws HibernateException {
		
		if (log.isDebugEnabled())
			log.debug("replace");
		
		//return deepCopy(original);
		return original;
	}
		    
}
