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
import static org.junit.Assert.*;

import org.junit.BeforeClass;
import org.junit.Test;

import com.netdimen.buganalysis.config.Config;

public class SearchEngineUtilsTest {

	private final static String BUGREPORT_FILE = Config.getInstance().getProperty("bug.analysis.test.bug.file");

	private final static String KEYWORD_FILE = Config.getInstance().getProperty("bug.analysis.test.keyword.file");

	private final static String INDEX_FIELD = "Subject";

	private static ArrayList<String> keywords = null;

	private static TermFrequencyObject obj = null;


	@Test
	public void shouldGenerateSummarizedBugReport() {

		final ArrayList<ArrayList<String>> results = SearchEngineUtils.generateSummarizedBugReport(INDEX_FIELD, BUGREPORT_FILE, KEYWORD_FILE);
		assertTrue(results.size()==8);
		for(int i = 2; i < results.size(); i ++){
			assertTrue(results.get(i).get(1).equalsIgnoreCase("1"));
		}
	}
	
	@Test
	public void shouldGenerateDetailedReport(){
		
		final ArrayList<ArrayList<String>> results = SearchEngineUtils.generateDetailedBugReport(BUGREPORT_FILE, KEYWORD_FILE);
		assertTrue(results.size() == 6);
		for(ArrayList<String> result: results){
			assertFalse(result.get(2).equals(""));
		}
	}

}
