package com.mycompany.myapp.utils;

import java.util.Comparator;
import java.util.Map;

/**Value comparator to help sort keywords and get Top-N terms
 * 
 * @author martin.wang
 *
 * @param <String>
 * @param <Integer>
 */
public class ValueComparator<String, Integer> implements Comparator<String> {
	Map<String, Integer> base;
	
	public ValueComparator(Map<String, Integer> base){
		this.base = base;
	}
	
	public int compare(String a, String b){
		if((int)base.get(a) > (int)base.get(b)){
			return 1;
		}else if((int)base.get(a) < (int)base.get(b)){
			return -1;
		}else{
			return 0;
		}
	}
}
