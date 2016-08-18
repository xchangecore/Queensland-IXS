package com.saic.uicds.core.infrastructure.dao.mssql;

import java.sql.SQLException;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;

import org.springframework.orm.hibernate3.support.HibernateDaoSupport;
import org.w3c.dom.Document;

import com.saic.uicds.core.infrastructure.model.WorkProduct;

abstract public class AbstractMSSQLDBDAO implements MSSQLDAOInterface {
		
	AbstractMSSQLDBDAO()
	{
		this("/unknown");
	}

	AbstractMSSQLDBDAO(String show) {
		System.out.println(show);
	}

	//sql way for now
	public abstract Document findDocsBySearchCriteria(String searchSQL) throws ParserConfigurationException, SQLException;
	public abstract WorkProduct findBySerachCritia(String searchSQL);	
	public abstract List<WorkProduct> findWPListBySearch(String searchSQL);
}
