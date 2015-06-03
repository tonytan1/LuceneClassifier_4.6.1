/*
 * 
 * Copyright (c) 1999-2015 NetDimensions Ltd.
 * 
 * All rights reserved.
 * 
 * This software is the confidential and proprietary information of NetDimensions Ltd. ("Confidential Information"). You
 * shall not disclose such Confidential Information and shall use it only in accordance with the terms of the license
 * agreement you entered into with NetDimensions.
 */
package com.netdimen.buganalysis.utils;

import java.util.ArrayList;

public class BugReport {

	private String ID;

	private String title;

	private String commitNotes;
	
	private ArrayList<String> labels;

	public BugReport(final String ID, final String title, final String commitNotes) {

		this.ID = ID;
		this.title = title;
		this.commitNotes = commitNotes;
		labels = new ArrayList<>();
	}
	
    /**
     * @return the labels
     */
    public ArrayList<String> getLabels() {
    
    	return labels;
    }

	
    /**
     * @param labels the labels to set
     */
    public void setLabels(ArrayList<String> labels) {
    
    	this.labels = labels;
    }
    
    public void addUniqueLabel(String label){
    	
    	if(!labels.contains(label)){
    		labels.add(label);
    	}
    }

	/**
	 * @return the iD
	 */
	public String getID() {

		return ID;
	}

	/**
	 * @param iD
	 *            the iD to set
	 */
	public void setID(String iD) {

		ID = iD;
	}

	/**
	 * @return the title
	 */
	public String getTitle() {

		return title;
	}

	/**
	 * @param title
	 *            the title to set
	 */
	public void setTitle(String title) {

		this.title = title;
	}

	/**
	 * @return the commitNotes
	 */
	public String getCommitNotes() {

		return commitNotes;
	}

	/**
	 * @param commitNotes
	 *            the commitNotes to set
	 */
	public void setCommitNotes(String commitNotes) {

		this.commitNotes = commitNotes;
	}

}
