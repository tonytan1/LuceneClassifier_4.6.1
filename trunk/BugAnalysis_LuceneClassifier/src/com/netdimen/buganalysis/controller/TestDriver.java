package com.netdimen.buganalysis.controller;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import java.util.Map;

import org.apache.lucene.search.TopDocs;

import com.google.common.collect.Lists;
import com.netdimen.buganalysis.config.Config;
import com.netdimen.buganalysis.utils.ClassifierUtils;
import com.netdimen.buganalysis.utils.DataUtils;
import com.netdimen.buganalysis.utils.DocumentUtils;
import com.netdimen.buganalysis.utils.POIUtils;
import com.netdimen.buganalysis.utils.SearchEngineUtils;
import com.netdimen.buganalysis.utils.SimilarityUtils;
import com.netdimen.buganalysis.utils.TermFrequencyObject;

/**
 * This is a main driver to use bug analysis system. Bug analysis helps to improve auto-testing system in the following
 * ways: 1. Test case = test inputs + testing steps + test outputs; 2. Test inputs: Bug analysis can find out
 * interesting test inputs that are more likely to detect faults. E.g., over-lengthy inputs, special characters(",",
 * "!", "'",...), invalid inputs (non-existing objects, duplicated inputs, empty inputs); 3. Testing steps: QA && Dev.
 * may assume "ABCD" is the normal steps for a function. However, in practice, clients use this function in a different
 * way (e.g., step sequence like "ACBD"). Bug analysis system finds such kind of mismatch with searching keywords
 * "when"; 4. Testing outputs: QA && Dev. add check points after "ABCD". However, in practice, clients may find bugs
 * between step "A" and "B". That is, some check points are missed. Bug analysis system finds such kind of bugs with
 * searching keywords like "error", "exception", "when"....
 * 
 * 
 * @author martin.wang
 *
 */
public class TestDriver {

	/**
	 * Index bug file with Lucene
	 * 
	 */
	public static void indexFile() {

		SearchEngineUtils.indexFile(Config.getInstance().getProperty("bug.analysis.report.bug.file"));
	}

	/**
	 * Search and return most-relevant (Top-100 by default) results for a specific keyword.
	 * 
	 * @param keyword
	 *            : search keyword
	 */
	public static void searchKeyWord(final String keyword) {

		SearchEngineUtils.indexFile(Config.getInstance().getProperty("bug.analysis.report.bug.file"));
		final String field = "Subject";
		final int TopN = 100;

		final StringBuilder sb = new StringBuilder();
		sb.append("Search keyword:").append(keyword).append("\n");
		final TopDocs docs = SearchEngineUtils.search(field, keyword, TopN);
		final ArrayList<String> fields = new ArrayList<String>();
		fields.add("Ticket Id");
		fields.add("Subject");
		SearchEngineUtils.showSearchResult(docs, fields, sb);
	}

	/**
	 * Get top-N keywords that are used by clients when reporting bugs.
	 * 
	 * @param TopN
	 */
	public static ArrayList<String> getTopNTerms(final int TopN) {

		// 1. Index file
		SearchEngineUtils.indexFile(Config.getInstance().getProperty("bug.analysis.report.bug.file"));
		final String field = "Subject";
		final float topTermCutoff = 0.1f;
		return SearchEngineUtils.getTopTerms(topTermCutoff, field, TopN);
	}

	/**
	 * 2015-01-06: Show term-coverage pair to help reduce term number
	 * 
	 */
	public static void getTermDistribution() {

		// 1. Index file
		SearchEngineUtils.indexFile(Config.getInstance().getProperty("bug.analysis.report.bug.file"));
		final String field = "Subject";
		final TermFrequencyObject obj = SearchEngineUtils.getAllTermsAndSort(field);
		final List<String> termList = obj.getTermList();

		final int totalDocs = DocumentUtils.getTotalDocs();
		final StringBuilder sb = new StringBuilder();
		sb.append("Total terms:").append(termList.size()).append("\tTotal Docs:").append(totalDocs).append("\n");

		sb.append("-----------------------------------------------------------\n");
		sb.append("NO.\tTerm\tCoverage_New\tCoverage_Total\n");

		final DecimalFormat df = new DecimalFormat("0.000");

		final BitSet coveredDocs = new BitSet();
		int coverage = 0;
		for (int j = 0; j < termList.size(); j++) {
			final String term = termList.get(j);
			final TopDocs docs = SearchEngineUtils.search(field, term, totalDocs);
			int coverageContribution = 0;
			for (int i = 0; i < docs.scoreDocs.length; i++) {
				final int docId = docs.scoreDocs[i].doc;
				if (coveredDocs.get(docId) == false) {
					coverageContribution++;
					coverage++;
					coveredDocs.set(docId, true);
				}
			}
			sb.append(j + 1)
			  .append("\t")
			  .append(term)
			  .append("\t")
			  .append(coverageContribution)
			  .append("\t")
			  .append(df.format((double) coverage / (double) totalDocs))
			  .append("\n");
		}

		System.out.println(sb.toString());

		final String result = sb.toString();
		final boolean overwrite = true;
		final String filePath = "./resource/TermDistribution.txt";
		DataUtils.saveToFile(result, filePath, overwrite);
	}
	
	public static void generateBugReport(){
		
		final String indexField = "Subject";
		final String bugReportFile = Config.getInstance().getProperty("bug.analysis.report.bug.file");
		final String keywordFile = Config.getInstance().getProperty("bug.analysis.report.keyword.file");

		POIUtils.writeToExcel(Config.getInstance().getProperty("bug.analysis.report.bug.save.file"), 
		                      Config.getInstance().getProperty("bug.analysis.report.summary.sheet"), 
		                      SearchEngineUtils.generateSummarizedBugReport(indexField, bugReportFile, keywordFile));
		
		POIUtils.writeToExcel(Config.getInstance().getProperty("bug.analysis.report.bug.save.file"), 
		                      Config.getInstance().getProperty("bug.analysis.report.details.sheet"), 
		                      SearchEngineUtils.generateDetailedBugReport(bugReportFile, keywordFile));
		
	}

	/**
	 * Get document similarity of documents (cosine distance by default) for a set of files. The result is reported with
	 * confusion matrix.
	 * 
	 * @param docNum
	 *            : the no. of documents (the first docNum documents) to be analyzed.
	 * 
	 */
	public static void getDocumentSimilarity(final int docNum) {

		SearchEngineUtils.indexFile(Config.getInstance().getProperty("bug.analysis.report.bug.file"));
		final SimilarityUtils ins = new SimilarityUtils();
		final String field = "Subject";
		ins.getDocumentSimilarityBasedOnTermVector(field, docNum);
	}

	/**
	 * Pairwise analyze Top-N terms to find out bug distributions. The result is reported with confusion matrix.
	 * 
	 * @param TopN
	 *            : Top-N terms
	 */
	public static void getPairwiseAnalysis(final int TopN) {

		SearchEngineUtils.indexFile(Config.getInstance().getProperty("bug.analysis.report.bug.file"));
		final SimilarityUtils ins = new SimilarityUtils();
		final String field = "Subject";
		float topTermCutoff = 0.1f;
		final ArrayList<String> topTerms = SearchEngineUtils.getTopTerms(topTermCutoff, field, TopN);
		ins.getPairwiseAnalysisBasedOnTerms(field, topTerms);
	}

	/**
	 * Use Naive Bayes model to auto-label bugs
	 * 
	 */
	public static void classifyWithNaiveBayesClassifier() {

		SearchEngineUtils.indexFile(Config.getInstance().getProperty("bug.analysis.report.bug.file"));
		ClassifierUtils.runNaiveBayesClassifier();
	}

	/**
	 * construct TF-IDF model based on Lucenen API.
	 * 
	 */
	public static void generateIFIDFModel() {

		SearchEngineUtils.indexFile(Config.getInstance().getProperty("bug.analysis.report.bug.file"));
		final SimilarityUtils ins = new SimilarityUtils();
		final String field = "Summary";
		final int maxDoc = 4;
		ins.genTFIDFModel(field, maxDoc);
		ins.genTFIDFModelBasedOnLucene(field);
		ins.genLuceneTFIDFModel(field);
	}

	/**
	 * Call diff. functions based on flags
	 * 
	 * @param flag
	 */
	public static void dispatcher(final int flag) {

		final int TopN;
		switch (flag) {
			case 1:
				// 1. Index file with Lucene
				indexFile();
				break;
			case 2:
				// 2. Search keyword with Lucene
				String keyword = "permissions";
				searchKeyWord(keyword);
				break;
			case 3:
				// 3. Get document similarity based on term vector
				int docNum = 20;
				getDocumentSimilarity(docNum);
				break;
			case 4:
				// 4. Get top ranking term
				TopN = 100;
				getTopNTerms(TopN);
				break;
			case 5:
				// 5. Pairwise analysis: term distribution
				TopN = 20;
				getPairwiseAnalysis(TopN);
				break;
			case 6:
				// 6. Run NaiveBayers classifier
				classifyWithNaiveBayesClassifier();
				break;
			case 7:
				// 7. TF-IDF Model
				generateIFIDFModel();
				break;
			case 8:
				getTermDistribution();
				break;
			case 9:
				generateBugReport();
				break;
			default:
				break;
		}
	}

	public static void main(String[] args) {

		int flag = 9;
		if (args.length == 1) {
			flag = Integer.parseInt(args[0]);
		}
		dispatcher(flag);
	}
}
