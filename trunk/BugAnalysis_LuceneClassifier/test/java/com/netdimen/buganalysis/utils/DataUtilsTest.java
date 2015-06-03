/*
 *
 * Copyright (c) 1999-2015 NetDimensions Ltd.
 *
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * NetDimensions Ltd. ("Confidential Information").  You shall not
 * disclose such Confidential Information and shall use it only in
 * accordance with the terms of the license agreement you entered into
 * with NetDimensions.
 */
package com.netdimen.buganalysis.utils;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Test;

import com.google.common.collect.Lists;


public class DataUtilsTest {

	@Test
	public void shouldFilterBasedOnExcludeList() {

		final List<String> from = Lists.newArrayList("1","2","3","4");
		final List<String> filterList = Lists.newArrayList("1","2");
		
		final List<String> includeResults = new DataUtils<String>().filterList(from, filterList, true);
		for(final String str: from){
			if(filterList.contains(str)){
				assertTrue(includeResults.contains(str));
			}else{
				assertFalse(includeResults.contains(str));
			}
		}
		
		final List<String> excludeResults = new DataUtils<String>().filterList(from, filterList, false);
		for(final String str: from){
			if(filterList.contains(str)){
				assertFalse(excludeResults.contains(str));
			}else{
				assertTrue(excludeResults.contains(str));
			}
		}
	}

}
