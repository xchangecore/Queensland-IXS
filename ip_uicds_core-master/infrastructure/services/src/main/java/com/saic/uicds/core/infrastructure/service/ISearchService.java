package com.saic.uicds.core.infrastructure.service;

import java.util.HashMap;
import java.util.List;

import org.w3c.dom.Document;

public interface ISearchService {

    List<Object> search(HashMap<String,String[]> params);

    Document searchAsDocument(HashMap<String,String[]> params);

}
