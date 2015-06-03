package com.mycompany.myapp.utils;

import java.util.List;
import java.util.Map;

public class TermFrequencyObject {
	
		private List<String> termList; //sorted term list
		public List<String> getTermList() {
			return termList;
		}

		public Map<String, Integer> getTermFreqMap() {
			return termFreqMap;
		}

		public void setTermList(List<String> termList) {
			this.termList = termList;
		}

		public void setTermFreqMap(Map<String, Integer> termFreqMap) {
			this.termFreqMap = termFreqMap;
		}

		private Map<String, Integer> termFreqMap; //term-frequency pair
		
		TermFrequencyObject(List<String> termList, Map<String, Integer> termFreqMap){
			this.termFreqMap = termFreqMap;
			this.termList = termList;
		}
}
