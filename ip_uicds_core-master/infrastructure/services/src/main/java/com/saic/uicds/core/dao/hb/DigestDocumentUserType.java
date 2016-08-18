package com.saic.uicds.core.dao.hb;

import gov.ucore.ucore.x20.DigestDocument;

import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

import org.apache.xmlbeans.XmlException;
import org.hibernate.HibernateException;
import org.hibernate.usertype.UserType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DigestDocumentUserType implements UserType {

    Logger log = LoggerFactory.getLogger(DigestDocumentUserType.class);

     
 	@Override
	public int[] sqlTypes() {
		return new int[] { Types.VARCHAR };
	}
   
	@Override
	@SuppressWarnings("unchecked")
	public Class<DigestDocument> returnedClass() {		
		return DigestDocument.class;
	}
	
	@Override
	public Object assemble(Serializable cached, Object owner)
			throws HibernateException {
    	if (log.isDebugEnabled())
    		log.debug("assemble");
		//return deepCopy(cached);
		return (cached);
	}

	@Override
	public Object deepCopy(Object value) throws HibernateException {
    	if (log.isDebugEnabled())
    		log.debug("deepCopy");
		//log.debug("object value="+ ((value !=null) ? value.toString(): "null"));
		if (value == null)
			return null;
		DigestDocument doc=null;
		String s = value.toString();
        if ((s != null) && !(s.equals(""))) {
        	try {
				doc = DigestDocument.Factory.parse(s);
			} catch (XmlException e) {
				log.error("deepCopy. Error:"+e.getMessage());
				e.printStackTrace();
			}
        }
			
		return doc;
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
		//log.debug("nullSafeGet. String s="+  s );
		DigestDocument doc=null;
		
        if ((s != null) && !(s.equals(""))) {
        	try {
				doc = DigestDocument.Factory.parse(s);
			} catch (XmlException e) {
				log.error("nullSafeGet. Error:"+e.getMessage());
				e.printStackTrace();
			}
        }
        return doc;
	}

	@Override
	public void nullSafeSet(PreparedStatement statement, Object value, int index)
			throws HibernateException, SQLException {

		if (value == null) {
			if (log.isDebugEnabled())
				log.debug("nullSafeSet - setNull");
            statement.setNull(index, sqlTypes()[0]);
        } else {
	    	String str = (value != null)? ((DigestDocument) value).toString() : "";
	    	if (log.isDebugEnabled())
	    		//log.debug("nullSafeSet - setString="+str+"\n");
	    		log.debug("nullSafeSet");
	        statement.setString(index, str);
        }
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
