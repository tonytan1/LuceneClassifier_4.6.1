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

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.ArrayList;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.google.common.collect.Lists;
import com.netdimen.buganalysis.config.Config;

public class POIUtilsTest {

	private final static String FILENAME = Config.getInstance().getProperty("bug.analysis.test.file");

	private final static String SHEETNAME = "sheet1";

	private final static ArrayList<String> row1 = Lists.newArrayList("1.1", "1.2", "1.3");

	private final static ArrayList<String> row2 = Lists.newArrayList("2.1", "2.2", "2.3");

	@BeforeClass
	public static void setupExcelFile() {

		final ArrayList<ArrayList<String>> data = new ArrayList<>();
		data.add(row1);
		data.add(row2);

		deleteFileIfExist(FILENAME);
		POIUtils.writeToExcel(FILENAME, SHEETNAME, data);
	}

	@Test
	public void shouldGetColumnFromExcel() {

		final ArrayList<String> result = POIUtils.getColumnFromExcel(FILENAME, SHEETNAME);
		assertTrue(result.size() == 2);
		assertTrue(result.get(0).equals(row1.get(0)));
		assertTrue(result.get(1).equals(row2.get(0)));
	}

	@Test
	public void shouldGetRowFromExcel() {

		final ArrayList<String> result = POIUtils.getRowFromExcel(FILENAME, SHEETNAME);
		assertTrue(result.size() == 3);
		assertTrue(result.get(0).equals(row1.get(0)));
		assertTrue(result.get(1).equals(row1.get(1)));
		assertTrue(result.get(2).equals(row1.get(2)));
	}

	@AfterClass
	public static void tearDown() {

		deleteFileIfExist(FILENAME);
	}

	private static void deleteFileIfExist(final String fileName) {

		final File temp = new File(FILENAME);
		if (temp.exists()) {
			temp.delete();
		}
	}
}
