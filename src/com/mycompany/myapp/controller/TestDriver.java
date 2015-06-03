package com.mycompany.myapp.controller;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import java.util.Map;

import org.apache.lucene.search.TopDocs;

import com.mycompany.myapp.poi.DataUtils;
import com.mycompany.myapp.utils.ClassifierUtils;
import com.mycompany.myapp.utils.DocumentUtils;
import com.mycompany.myapp.utils.SearchEngineUtils;
import com.mycompany.myapp.utils.SimilarityUtils;
import com.mycompany.myapp.utils.TermFrequencyObject;

/**This is a main driver to use bug analysis system. Bug analysis helps to improve auto-testing system in the following ways:
 * 1. Test case = test inputs + testing steps + test outputs;
 * 2. Test inputs: Bug analysis can find out interesting test inputs that are more likely to detect faults. 
 * E.g., over-lengthy inputs, special characters(",", "!", "'",...), invalid inputs (non-existing objects, duplicated inputs, empty inputs);
 * 3. Testing steps: QA && Dev. may assume "ABCD" is the normal steps for a function. However, in practice, clients use this function
 * in a different way (e.g., step sequence like "ACBD"). Bug analysis system finds such kind of mismatch with searching keywords "when";
 * 4. Testing outputs: QA && Dev. add check points after "ABCD". However, in practice, clients may find bugs between step "A" and "B". That is,
 * some check points are missed. Bug analysis system finds such kind of bugs with searching keywords like "error", "exception", "when"....  
 *  
 * 
 * @author martin.wang
 *
 */
public class TestDriver {
	
	/** Index bug file with Lucene 
	 * 
	 */
	public static void indexFile(){
		String srcFile = System.getProperty("user.dir")
				+ "/resource/BugReport_zendesk.xls";
		if (srcFile.contains("xls")) {
			SearchEngineUtils.indexExcelFile(srcFile);
		} else if (srcFile.contains("csv")) {
			SearchEngineUtils.indexCSVFile(srcFile);
		}
	}
	
	/**Search and return most-relevant (Top-100 by default) results for a specific keyword.
	 * 
	 * @param keyword: search keyword
	 */
	public static void searchKeyWord(String keyword){
		String srcFile = System.getProperty("user.dir")
				+ "/resource/BugReport_zendesk.xls";
		if(srcFile.contains("xls")){
			SearchEngineUtils.indexExcelFile(srcFile);	
		}else if(srcFile.contains("csv")){
			SearchEngineUtils.indexCSVFile(srcFile);	
		}
		String field = "Subject";
		int TopN = 100;
		
		StringBuilder sb = new StringBuilder();
		sb.append("Search keyword:").append(keyword).append("\n");
		TopDocs docs = SearchEngineUtils.search(field, keyword, TopN);
		ArrayList<String> fields = new ArrayList<String>();
		fields.add("Ticket Id");
		fields.add("Subject");
		SearchEngineUtils.showSearchResult(docs, fields, sb);
	}
	
	/**Get top-N keywords that are used by clients when reporting bugs.
	 * 
	 * @param TopN
	 */
	public static ArrayList<String> getTopNTerms(int TopN){
		String srcFile = System.getProperty("user.dir")
				+ "/resource/BugReport_zendesk.xls";
		//1. Index file
		if(srcFile.contains("xls")){
				SearchEngineUtils.indexExcelFile(srcFile);	
		}else if(srcFile.contains("csv")){
				SearchEngineUtils.indexCSVFile(srcFile);	
		}
		
		String field = "Subject";
		float topTermCutoff = 0.1f;
		return SearchEngineUtils.getTopTerms(topTermCutoff, field, TopN);
	}
	
	/**2015-01-06: Show term-coverage pair to help reduce term number
	 * 
	 */
	public static void getTermDistribution( ){
		String srcFile = System.getProperty("user.dir")
				+ "/resource/BugReport_zendesk.xls";
		//1. Index file
		if(srcFile.contains("xls")){
				SearchEngineUtils.indexExcelFile(srcFile);	
		}else if(srcFile.contains("csv")){
				SearchEngineUtils.indexCSVFile(srcFile);	
		}
		
		String field = "Subject";
		TermFrequencyObject obj = SearchEngineUtils.getAllTermsAndSort(field);
		List<String> termList = obj.getTermList();
		
		int totalDocs = DocumentUtils.getTotalDocs();
		StringBuilder sb = new StringBuilder();
		sb.append("Total terms:").append(termList.size()).
		append("\tTotal Docs:").append(totalDocs).
		append("\n");
		
		sb.append("-----------------------------------------------------------\n");
		sb.append("NO.\tTerm\tCoverage_New\tCoverage_Total\n");
		
		DecimalFormat df = new DecimalFormat("0.000");
		
		BitSet coveredDocs = new BitSet();
		int coverage = 0;
		for(int j = 0; j < termList.size(); j ++){
			String term = termList.get(j);
			TopDocs docs = SearchEngineUtils.search(field, term, totalDocs);
			int coverageContribution = 0;
			for(int i = 0; i < docs.scoreDocs.length; i ++){
				int docId = docs.scoreDocs[i].doc;
				if(coveredDocs.get(docId)==false){
					coverageContribution ++;
					coverage++;
					coveredDocs.set(docId, true);
				}
			}
			sb.append(j+1).append("\t").
			append(term).append("\t").
			append(coverageContribution).append("\t").
			append(df.format((double)coverage/(double)totalDocs)).append("\n");
		}
		
		System.out.println(sb.toString());
		
		String result = sb.toString();
		boolean overwrite = true;
		String filePath = "./resource/TermDistribution.txt";
		DataUtils.saveToFile(result, filePath, overwrite);
	}
	
	/**Get document similarity of documents (cosine distance by default)
	 * for a set of files. The result is reported with confusion matrix. 
	 * 
	 * @param docNum: the no. of documents (the first docNum documents) to be analyzed.
	 * 
	 */
	public static void getDocumentSimilarity(int docNum){
		String srcFile = System.getProperty("user.dir")
				+ "/resource/BugReport_zendesk.xls";
		if(srcFile.contains("xls")){
			SearchEngineUtils.indexExcelFile(srcFile);	
		}else if(srcFile.contains("csv")){
			SearchEngineUtils.indexCSVFile(srcFile);	
		}
		
		SimilarityUtils ins = new SimilarityUtils();
		String field = "Subject";
		ins.getDocumentSimilarityBasedOnTermVector(field, docNum);
		
	}
	
	/**Pairwise analyze Top-N terms to find out bug distributions.
	 * The result is reported with confusion matrix.
	 * 
	 * @param TopN: Top-N terms
	 */
	public static void getPairwiseAnalysis(int TopN){
		String srcFile = System.getProperty("user.dir")
				+ "/resource/BugReport_zendesk.xls";
		//1. Index file
		if(srcFile.contains("xls")){
				SearchEngineUtils.indexExcelFile(srcFile);	
		}else if(srcFile.contains("csv")){
				SearchEngineUtils.indexCSVFile(srcFile);	
		}
		SimilarityUtils ins = new SimilarityUtils();
		String field = "Subject";
		float topTermCutoff = 0.1f;
		ArrayList<String> topTerms = SearchEngineUtils.getTopTerms(topTermCutoff, field, TopN);
		ins.getPairwiseAnalysisBasedOnTerms(field, topTerms);
	}
	
	/**Use Naive Bayes model to auto-label bugs  
	 * 
	 */
	public static void classifyWithNaiveBayesClassifier(){
		String srcFile = System.getProperty("user.dir")
				+ "/resource/BugReport_trainset.xls";
		//1. Index file
		if(srcFile.contains("xls")){
				SearchEngineUtils.indexExcelFile(srcFile);	
		}else if(srcFile.contains("csv")){
				SearchEngineUtils.indexCSVFile(srcFile);	
		}
				
		ClassifierUtils.runNaiveBayesClassifier();
	}
	
	/**construct TF-IDF model based on Lucenen API. 
	 * 
	 */
	public static void generateIFIDFModel(){
		String srcFile = System.getProperty("user.dir")
				+ "/resource/BugReport_trainset.xls";
		if(srcFile.contains("xls")){
			SearchEngineUtils.indexExcelFile(srcFile);	
		}else if(srcFile.contains("csv")){
			SearchEngineUtils.indexCSVFile(srcFile);	
		}
		
		SimilarityUtils ins = new SimilarityUtils();
		String field = "Summary";
		int maxDoc = 4;
		ins.genTFIDFModel(field,maxDoc);
		ins.genTFIDFModelBasedOnLucene(field);		
		ins.genLuceneTFIDFModel(field);
	}
	
	/**Call diff. functions based on flags
	 * 
	 * @param flag
	 */
	public static void dispatcher(int flag){
		int TopN;
		switch(flag){
			case 1:
				//1. Index file with Lucene					
				indexFile();
				break;
				
			case 2:
				//2. Search keyword with Lucene					
				String keyword = "permissions";
				searchKeyWord(keyword);
				break;
				
			case 3:
				//3. Get document similarity based on term vector
				int docNum = 20;
				getDocumentSimilarity(docNum);					
				break;
				
			case 4:
				//4. Get top ranking term
				TopN = 100;
				getTopNTerms(TopN);
				break;
				
			case 5:
				//5. Pairwise analysis: term distribution
				TopN = 20;
				getPairwiseAnalysis(TopN);					
				break;
				
			case 6:
				//6. Run NaiveBayers classifier
				classifyWithNaiveBayesClassifier();
				break;
				
			case 7:
				//7. TF-IDF Model
				generateIFIDFModel();					
				break;
				
			case 8:
				getTermDistribution();
				break;
		}
	}		
	
	public static void main(String[] args) {
		int flag = 8;
		if(args.length == 1){
			flag =Integer.parseInt(args[0]);
		}
		dispatcher(flag);
	}
}
