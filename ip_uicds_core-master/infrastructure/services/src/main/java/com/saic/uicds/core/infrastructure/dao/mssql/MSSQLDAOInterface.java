package com.saic.uicds.core.infrastructure.dao.mssql;

import java.sql.SQLException;

import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;


public interface MSSQLDAOInterface {

    public abstract Document findDocsBySearchCriteria(String searchSQL) throws ParserConfigurationException, SQLException;
}
