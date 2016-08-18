package com.saic.uicds.core.infrastructure.service.impl;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;

import com.saic.uicds.core.infrastructure.dao.WorkProductDAO;
//import com.saic.uicds.core.infrastructure.dao.mssql.MSSQLDAOInterface;
import com.saic.uicds.core.infrastructure.dao.mssql.WorkProductMSSQLDB2DAO;
import com.saic.uicds.core.infrastructure.model.WorkProduct;
import com.saic.uicds.core.infrastructure.service.ISearchService;
import com.saic.uicds.core.infrastructure.util.WorkProductSqlBuilder;

public class SQLSearchServiceImpl implements ISearchService {

	Logger log = LoggerFactory.getLogger(getClass());

	//private MSSQLDAOInterface workProductDAO;
	private WorkProductDAO workProductDAO;

	public SQLSearchServiceImpl() {
	}

	public WorkProductDAO getWorkProductDAO() {
		return workProductDAO;
	}

	public void setWorkProductDAO(WorkProductDAO workProductDAO) {
		this.workProductDAO = workProductDAO;
	}

	@Override
	public Document searchAsDocument(HashMap<String, String[]> params) {
		if (log.isDebugEnabled())
			log.debug("searchAsDocument");
		WorkProductSqlBuilder builder = new WorkProductSqlBuilder();
		String query = builder.buildQuery(params);

		Document result = null;
		try {
			result = ((WorkProductMSSQLDB2DAO) workProductDAO).findDocsBySearchCriteria(query);
		} catch (ParserConfigurationException e) {
			log.error("ParseConfiguration error " + e.getMessage());
		} catch (SQLException e) {
			log.error("SQLException error " + e.getMessage());
		}
		return result;
	}

	@Override
	public List<Object> search(HashMap<String, String[]> params) {
		WorkProductSqlBuilder builder = new WorkProductSqlBuilder();
		String query = builder.buildQuery(params);
		WorkProduct wps = ((WorkProductMSSQLDB2DAO) workProductDAO).findWPBySerachCriteria(query);
		List<Object> result = new ArrayList<Object>();
		result.add(wps);
		return result;
	}
}
