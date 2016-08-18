package com.saic.uicds.core.em.adminconsole.client.model;

import java.io.Serializable;

public class IGInstanceGWT implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1754761565765166125L;
	
	private String interestGroupID;
	private String interestGroupSubType;
	private String summary;
	private String workProductID;

	private String instanceName;

	public IGInstanceGWT() {
	}

	public String getInstanceName() {
		return instanceName;
	}

	public String getInterestGroupID() {
		return interestGroupID;
	}

	public String getInterestGroupSubType() {
		return interestGroupSubType;
	}

	public String getSummary() {

		return summary;
	}

	public String getWorkProductID() {
		return workProductID;
	}

	public void setInstanceName(String instanceName) {
		this.instanceName = instanceName;
	}

	public void setInterestGroupID(String interestGroupID) {
		this.interestGroupID = interestGroupID;
	}

	public void setInterestGroupSubType(String interestGroupSubType) {
		this.interestGroupSubType = interestGroupSubType;
	}

	public void setSummary(String summary) {

		this.summary = summary;
	}

	public void setWorkProductID(String workProductID) {
		this.workProductID = workProductID;
	}

}