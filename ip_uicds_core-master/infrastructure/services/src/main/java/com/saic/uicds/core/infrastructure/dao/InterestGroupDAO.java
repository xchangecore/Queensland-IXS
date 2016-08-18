package com.saic.uicds.core.infrastructure.dao;

import java.util.List;

import com.saic.uicds.core.dao.GenericDAO;
import com.saic.uicds.core.infrastructure.model.InterestGroup;

public interface InterestGroupDAO extends GenericDAO<InterestGroup, Integer> {
	
    public InterestGroup findByInterestGroup(String interestGroupID);

    public List<InterestGroup> findByOwningCore(String owningCore);
    
    public void delete(String interestGroupID, boolean isDelete);
}
