package com.saic.uicds.core.em.adminconsole.client.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class InterestGroupGWT implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -1048990991378419550L;

	private boolean root = false;
	private boolean leaf = false;
	private boolean closed = false;

	private String interestGroupID;
	private String interestGroupSubType;
	private String interestGroupType;
	private String summary;
	private List<IGInstanceGWT> igInstances = new ArrayList<IGInstanceGWT>();

	private String name;

	private String title;

	public InterestGroupGWT() {

	}

	public InterestGroupGWT(String title) {
		setTitle(title);
	}

	public List<IGInstanceGWT> getIgInstances() {
		return igInstances;
	}

	public String getInterestGroupID() {
		return interestGroupID;
	}

	public String getInterestGroupSubType() {
		return interestGroupSubType;
	}

	public String getInterestGroupType() {
		return interestGroupType;
	}

	public String getName() {

		return name;
	}

	/**
	 * Return the summary of the incident.
	 * 
	 * @return
	 */
	public String getSummary() {

		return summary;
	}

	public String getTitle() {

		return title;
	}

	public boolean isClosed() {

		return closed;
	}

	public boolean isLeaf() {

		return leaf;
	}

	public boolean isRoot() {

		return root;
	}

	public void setClosed(boolean closed) {

		this.closed = closed;
	}

	public void setIgInstances(List<IGInstanceGWT> igInstances) {
		this.igInstances = igInstances;
	}

	public void setInterestGroupID(String interestGroupID) {
		this.interestGroupID = interestGroupID;
	}

	public void setInterestGroupSubType(String interestGroupSubType) {
		this.interestGroupSubType = interestGroupSubType;
	}

	public void setInterestGroupType(String interestGroupType) {
		this.interestGroupType = interestGroupType;
	}

	public void setLeaf(boolean leaf) {

		this.leaf = leaf;
	}

	public void setName(String name) {

		this.name = name;
	}

	public void setRoot(boolean root) {

		this.root = root;
	}

	/**
	 * Set the summary of the incident.
	 * 
	 * @param summary
	 */
	public void setSummary(String summary) {

		this.summary = summary;
	}

	/**
	 * Set the title of the incident.
	 * 
	 * @param title
	 */
	public void setTitle(String title) {

		this.title = title;
		setName(title);
	}

	@Override
	public String toString() {

		return getName();
	}
}