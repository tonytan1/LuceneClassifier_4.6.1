package com.netdimen.buganalysis.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.StringReader;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.commons.collections4.comparators.ReverseComparator;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.Field.TermVector;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.Fields;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.MultiFields;
import org.apache.lucene.index.Terms;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.LockObtainFailedException;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.Version;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;

import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.netdimen.buganalysis.config.Config;

/**
 * Build index, and search keyword with Lucene.
 * 
 * @author martin.wang
 *
 */
public class SearchEngineUtils {

	public static void indexFile(final String srcFile) {

		// 1. Delete previous indexing results
		try {
			final File tmp = new File(Config.directoryPath);
			if (tmp.exists()) {
				FileUtils.deleteDirectory(tmp);
			}
		}
		catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		// 2. index files
		if (srcFile.contains("xls")) {
			SearchEngineUtils.indexExcelFile(srcFile);
		} else if (srcFile.contains("csv")) {
			SearchEngineUtils.indexCSVFile(srcFile);
		}
	}

	/**
	 * Get all terms and sort them based on term frequency
	 * 
	 * @param indexField
	 * @return
	 */
	public static TermFrequencyObject getAllTermsAndSort(final String indexField) {

		final List<String> termList = Lists.newArrayList();
		final Map<String, Integer> termFreqMap = Maps.newHashMap();
		try {
			final IndexReader reader = DirectoryReader.open(FSDirectory.open(new File(Config.directoryPath)));

			final Fields fields = MultiFields.getFields(reader);
			final Terms terms = fields.terms(indexField);

			final TermsEnum termEnum = terms.iterator(null);
			BytesRef bytesRef = null;
			while ((bytesRef = termEnum.next()) != null) {
				final String termText = bytesRef.utf8ToString();
				final int frequency = termEnum.docFreq();
				termFreqMap.put(termText, frequency);
				termList.add(termText);
			}

			reader.close();
			// sort the term map by frequency descending
			Collections.sort(termList, new ReverseComparator<String>(new ValueComparator<String, Integer>(termFreqMap)));
		}
		catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return new TermFrequencyObject(termList, termFreqMap);
	}

	public static TermFrequencyObject getTermsAndSort(final String indexField, final Collection<String> includedList) {

		final TermFrequencyObject obj = getAllTermsAndSort(indexField);
		return new TermFrequencyObject(new DataUtils().filterList(obj.getTermList(), includedList, true), obj.getTermFreqMap());
	}

	/**
	 * Get Top-N terms
	 * 
	 * @param topTermCutoff
	 * @param field
	 * @param TopN
	 * @return
	 */
	public static ArrayList<String> getTopTerms(final float topTermCutoff, final String field, final int TopN) {

		final ArrayList<String> topTerms = Lists.newArrayList();
		final TermFrequencyObject obj = SearchEngineUtils.getAllTermsAndSort(field);
		final Map<String, Integer> termFreqMap = obj.getTermFreqMap();
		final List<String> termList = obj.getTermList();

		// retrieve the top terms based on topTermCutof
		float topFreq = -1.0f;
		for (final String term : termList) {
			if (topFreq < 0.0f) {
				// first term , capture the value
				topFreq = (float) termFreqMap.get(term);
				topTerms.add(term);
			} else {
				// not the first term, compute the ratio and discard if
				// below topTermCutoff score
				final float ratio = (float) ((float) termFreqMap.get(term) / topFreq);
				if (ratio > topTermCutoff && topTerms.size() < TopN) {
					topTerms.add(term);
				} else {
					break;
				}
			}
		}

		final StringBuilder sb = new StringBuilder();
		sb.append(">>> top ").append(TopN).append(" terms: ");
		int counter = 1;
		for (final String topTerm : topTerms) {
			if (counter % 1 == 0) {
				sb.append("\n");
			}
			sb.append(counter).append(":").append(topTerm).append("(").append(termFreqMap.get(topTerm)).append(");");
			counter++;
		}

		System.out.println(sb.toString());
		final String filePath = "./resource/TopTerms.txt";
		final boolean overwrite = true;
		DataUtils.saveToFile(sb.toString(), filePath, overwrite);
		return topTerms;
	}

	/**
	 * Computes a term frequency map for the index at the specified location. Builds a Boolean OR query out of the
	 * "most frequent" terms in the index and returns it. "Most Frequent" is defined as the terms whose frequencies are
	 * greater than or equal to the topTermCutoff * the frequency of the top term, where the topTermCutoff is number
	 * between 0 and 1.
	 * 
	 * @param topTermCutoff
	 * @param field
	 * @return
	 */
	public static BooleanQuery computeTopTermQuery(final float topTermCutoff, final String field) {

		BooleanQuery query = null;

		try {
			final Map<String, Integer> termFreqMap = Maps.newHashMap();
			final List<String> termList = Lists.newArrayList();

			final IndexReader reader = DirectoryReader.open(FSDirectory.open(new File(Config.directoryPath)));

			final Fields fields = MultiFields.getFields(reader);
			final Terms terms = fields.terms(field);

			final TermsEnum termEnum = terms.iterator(null);
			BytesRef bytesRef;
			while ((bytesRef = termEnum.next()) != null) {
				final String termText = bytesRef.utf8ToString();
				final int frequency = termEnum.docFreq();
				termFreqMap.put(termText, frequency);
				termList.add(termText);
			}

			reader.close();
			// sort the term map by frequency descending
			Collections.sort(termList, new ReverseComparator<String>(new ValueComparator<String, Integer>(termFreqMap)));

			// retrieve the top terms based on topTermCutof
			final List<String> topTerms = Lists.newArrayList();
			float topFreq = -1.0f;
			for (final String term : termList) {
				if (topFreq < 0.0f) {
					// first term , capture the value
					topFreq = (float) termFreqMap.get(term);
					topTerms.add(term);
				} else {
					// not the first term, compute the ratio and discard if
					// below topTermCutoff score
					final float ratio = (float) ((float) termFreqMap.get(term) / topFreq);
					if (ratio > topTermCutoff) {
						topTerms.add(term);
					} else {
						break;
					}
				}
			}

			final StringBuilder termBuf = new StringBuilder();
			query = new BooleanQuery();

			final QueryParser parser = new QueryParser(Version.LUCENE_46, "Summary", new StandardAnalyzer(Version.LUCENE_46));
			for (final String topTerm : topTerms) {
				termBuf.append(topTerm).append("(").append(termFreqMap.get(topTerm)).append(");");
				query.add(parser.parse(topTerm), Occur.SHOULD); // "OR" operator
			}
			System.out.println(">>> top terms: " + termBuf.toString());
		}
		catch (CorruptIndexException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		catch (ParseException e) {
			e.printStackTrace();
		}
		return query;
	}

	public static String[] searchIndex(final Query query, final int TopN) {

		final SortedMap<Integer, String> ID_TextMap = new TreeMap<Integer, String>();
		try {
			final IndexSearcher searcher = new IndexSearcher(DirectoryReader.open(FSDirectory.open(new File(Config.directoryPath))));

			final Query q = new QueryParser(Version.LUCENE_46, "Summary", new StandardAnalyzer(Version.LUCENE_46)).parse(query.toString());
			System.out.println(q.toString());
			final TopDocs hits = SearchEngineUtils.search(q, TopN);
			for (final ScoreDoc scoreDoc : hits.scoreDocs) {
				final int docID = scoreDoc.doc;
				final Document doc = searcher.doc(docID);
				ID_TextMap.put(scoreDoc.doc, StringUtils.chomp(doc.get("text")));
			}
		}
		catch (CorruptIndexException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		catch (ParseException e) {
			e.printStackTrace();
		}
		return ID_TextMap.values().toArray(new String[0]);
	}

	/**
	 * Index a specific field value.
	 * 
	 * @param field
	 * @param value
	 */
	public static void index(final String field, final String value) {

		try {
			final StandardAnalyzer analyzer = new StandardAnalyzer(Version.LUCENE_46);
			final FSDirectory directory = FSDirectory.open(new File(Config.directoryPath));

			final IndexWriterConfig config = new IndexWriterConfig(Version.LUCENE_46, analyzer);
			final IndexWriter writer = new IndexWriter(directory, config);

			final Document doc = new Document();
			doc.add(new Field(field, value, Field.Store.YES, Field.Index.ANALYZED, Field.TermVector.WITH_POSITIONS_OFFSETS));
			writer.addDocument(doc);
			writer.close();
			directory.close();
		}
		catch (CorruptIndexException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		catch (LockObtainFailedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public static List<String> parseText(final Analyzer analyzer, final String text) {

		final List<String> results = Lists.newArrayList();
		try {
			final TokenStream stream = analyzer.tokenStream(null, new StringReader(text));
			final CharTermAttribute cta = stream.addAttribute(CharTermAttribute.class);
			stream.reset();
			results.add(cta.toString());
			while (stream.incrementToken()) {
				results.add(cta.toString());
			}
			stream.end();
			stream.close();
		}
		catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return results;
	}

	private static void indexExcelFile(final String srcFile) {

		try {
			final StandardAnalyzer analyzer = new StandardAnalyzer(Version.LUCENE_46);
			final FSDirectory directory = FSDirectory.open(new File(Config.directoryPath));
			final IndexWriterConfig config = new IndexWriterConfig(Version.LUCENE_46, analyzer);
			final IndexWriter writer = new IndexWriter(directory, config);
			final FileInputStream file = new FileInputStream(srcFile);
			final HSSFWorkbook wb = new HSSFWorkbook(file);
			final int sheetNo = wb.getNumberOfSheets();
			for (int i = 0; i < sheetNo; i++) {
				final HSSFSheet sheet = wb.getSheetAt(i);

				int rowIndex = 0;
				final ArrayList<String> fields = POIUtils.getRowFromExcel(srcFile, sheet.getSheetName());

				int columnIndex = 0;
				final int rowNo = POIUtils.NumRows(sheet, columnIndex);
				// iterate row by row
				for (rowIndex = 1; rowIndex < rowNo; rowIndex++) { // Row 0 =
					                                               // field,
					                                               // Row 1...N
					                                               // = values
					final ArrayList<String> values = POIUtils.getRowFromExcel(srcFile, sheet.getSheetName(), rowIndex);
					final Document doc = new Document();
					for (int j = 0; j < fields.size(); j++) {
						final String field = fields.get(j);
						if (values.size() > j) {
							final String value = values.get(j);
							if (value != null && !value.equals("")) {
								doc.add(new Field(field, value, Field.Store.YES, Field.Index.ANALYZED, TermVector.WITH_POSITIONS_OFFSETS));
							}
						}
					}

					if (rowIndex == 984) {
						System.out.println(rowIndex + ":" + values);
					}

					writer.addDocument(doc);
				}
				System.out.println(">>>Index statistics: sheet=" + sheet.getSheetName() + ";docNO=" + (rowNo - 1));
			}

			writer.close();
			directory.close();
			analyzer.close();
		}
		catch (CorruptIndexException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		catch (LockObtainFailedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/**
	 * Index all fields defined in a CSV file
	 * 
	 * @param srcFile
	 */
	private static void indexCSVFile(final String srcFile) {

		try {
			final StandardAnalyzer analyzer = new StandardAnalyzer(Version.LUCENE_46);
			final FSDirectory directory = FSDirectory.open(new File(Config.directoryPath));
			final IndexWriterConfig config = new IndexWriterConfig(Version.LUCENE_46, analyzer);
			final IndexWriter writer = new IndexWriter(directory, config);

			final BufferedReader br = new BufferedReader(new FileReader(srcFile));
			String line = br.readLine();
			final String[] fields = line.split(",");

			int counter = 0;
			while ((line = br.readLine()) != null) {
				String[] values = line.split(",");
				Document doc = new Document();
				for (int i = 0; i < fields.length; i++) {
					final String field = fields[i];
					final String value = values[i];
					doc.add(new Field(field, value, Field.Store.YES, Field.Index.ANALYZED, TermVector.WITH_POSITIONS_OFFSETS));
					if (counter == 220) {
						System.out.println(counter + ":" + field + "=" + value);
					}
				}
				writer.addDocument(doc);
				counter++;

			}

			writer.close();
			directory.close();
			br.close();
		}
		catch (CorruptIndexException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		catch (LockObtainFailedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public static TopDocs search(final Query query, final int TopN) {

		TopDocs docs = null;
		try {
			final IndexReader reader = DirectoryReader.open(FSDirectory.open(new File(Config.directoryPath)));
			final IndexSearcher searcher = new IndexSearcher(reader);

			docs = searcher.search(query, TopN);
		}
		catch (CorruptIndexException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return docs;
	}

	public static TopDocs search(final String field, final String keyword, final int TopN) {

		final QueryParser parser = new QueryParser(Version.LUCENE_46, field, new StandardAnalyzer(Version.LUCENE_46));
		Query query = null;

		try {
			query = parser.parse(keyword);
		}
		catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return search(query, TopN);
	}

	public static void showSearchResult(final TopDocs docs, final ArrayList<String> fields, final StringBuilder sb) {

		try {
			final IndexReader reader = DirectoryReader.open(FSDirectory.open(new File(Config.directoryPath)));
			final IndexSearcher searcher = new IndexSearcher(reader);
			final ScoreDoc[] hits = docs.scoreDocs;
			sb.append("Matched:" + hits.length).append("\n");

			// 1. Iterate through results:
			for (int i = 0; i < hits.length; i++) {
				final Document hitDoc = searcher.doc(hits[i].doc);
				sb.append(i + ":Score=" + hits[i].score + ";");
				for (final String field : fields) {
					final String value = hitDoc.get(field);
					sb.append(field).append("=").append(value).append(";");
				}
				sb.append("\n");
			}
			System.out.println(sb.toString());
			reader.close();
			final String filePath = "./resource/SearchResult.txt";
			final boolean overwrite = true;
			DataUtils.saveToFile(sb.toString(), filePath, overwrite);
		}
		catch (CorruptIndexException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public static String printConfusionMatrix(final String[] labels, final double[][] similarities) {

		final StringBuilder sb = new StringBuilder();
		if (labels.length != similarities[0].length) {
			sb.append("Label length does not match with data length").append("\n");
		} else {
			sb.append("----------------------------------------------------------").append("\n");
			sb.append("----------------------------------------------------------").append("\n");
			sb.append("Confusion Matrix:").append("\n");
			sb.append("----------------------------------------------------------").append("\n");

			for (int j = 0; j < labels.length; j++) {
				sb.append("   " + j + " ");
			}
			sb.append("\n\n");

			DecimalFormat df = new DecimalFormat("0.000");
			for (int i = 0; i < similarities.length; i++) {
				int sum = 0;
				for (int j = 0; j < similarities[i].length; j++) {
					sb.append(" " + df.format(similarities[i][j]) + " ");
				}

				if (labels.length > i) {
					sb.append("\t|\t").append(sum).append("\t").append(i).append("\t=").append(labels[i]).append("\n");
				} else {
					sb.append("\t|\t").append(sum).append("\t").append("\n");
				}
			}
		}

		final String result = sb.toString();
		System.out.print(result);
		return result;
	}

	public static String printConfusionMatrix(final String[] labels, final int[][] counts) {

		final StringBuilder sb = new StringBuilder();
		sb.append("----------------------------------------------------------").append("\n");
		sb.append("----------------------------------------------------------").append("\n");
		sb.append("Confusion Matrix:").append("\n");
		sb.append("----------------------------------------------------------").append("\n");

		int fc = 0, tc = 0;
		char letter = 'a';
		for (int j = 0; j < labels.length; j++) {
			sb.append(" ").append(letter).append(" ");
			letter++;
		}

		sb.append("\t<--Classified as").append("\n");
		letter = 'a';
		final DecimalFormat df = new DecimalFormat("0");
		for (int i = 0; i < labels.length; i++) {
			int sum = 0;
			for (int j = 0; j < labels.length; j++) {
				sb.append(" ").append(df.format(counts[i][j])).append(" ");
				sum += counts[i][j];
				if (i == j) {
					tc += counts[i][j];
				} else {
					fc += counts[i][j];
				}
			}
			sb.append("\t|\t").append(sum).append("\t").append(letter).append("\t=").append(labels[i]).append("\n");
			letter++;
		}

		final float accrate = (float) tc / (float) (tc + fc);
		final float errrate = (float) fc / (float) (tc + fc);
		sb.append("\n\n*** accuracy rate =").append(df.format(accrate)).append(";error rate =").append(df.format(errrate)).append("\n");
		final String result = sb.toString();
		System.out.println(result);
		return result;
	}
	
	public static ArrayList<ArrayList<String>> generateDetailedBugReport(
	                                                                     final String bugReportFile,
	                                                                     final String keywordFile){
		
		final ArrayList<String> keywords = new DataUtils().toLowerCase(POIUtils.getColumnFromExcel(keywordFile));
		final ArrayList<BugReport> bugReports = POIUtils.getBugReportFromExcel(bugReportFile);
		
		ArrayList<ArrayList<String>> results = Lists.newArrayList();
		results.add(Lists.newArrayList("ID","Bug Title","Keyword"));
		for(BugReport bugReport: bugReports){
			for(String label: keywords){
				if(bugReport.getTitle().toLowerCase().contains(label.toLowerCase())){
					bugReport.addUniqueLabel(label);
					results.add(Lists.newArrayList(bugReport.getID(), bugReport.getTitle(), label));
				}
			}
		}

		return results;
/*		return Lists.transform(bugReports, new Function<BugReport, ArrayList<ArrayList<String>>>(){
			@Override
			public ArrayList<ArrayList<String>> apply(BugReport bugReport){
				
				ArrayList<ArrayList<String>> results = Lists.newArrayList();
				for(String label: keywords){
					if(bugReport.getTitle().toLowerCase().contains(label)){
						bugReport.addUniqueLabel(label);
					}
				}
//				ArrayList<String> results = Lists.newArrayList(bugReport.getID(), bugReport.getTitle());
//				results.addAll(bugReport.getLabels());
				return results;
			}
		});*/
	}

	public static ArrayList<ArrayList<String>> generateSummarizedBugReport(final String indexField,
	                                                                       final String bugReportFile,
	                                                                       final String keywordFile) {

		SearchEngineUtils.indexFile(bugReportFile);
		final ArrayList<String> keywords = new DataUtils().toLowerCase(POIUtils.getColumnFromExcel(keywordFile));
		return SearchEngineUtils.generateSummariedBugReport(SearchEngineUtils.getTermsAndSort(indexField, keywords), keywords, indexField);
	}
	
	private static ArrayList<ArrayList<String>> generateSummariedBugReport(final TermFrequencyObject obj,
	                                                                      final ArrayList<String> keywords,
	                                                                      final String indexField) {

		final List<String> termList = obj.getTermList();
		final Map<String, Integer> termFreqMap = obj.getTermFreqMap();

		final ArrayList<ArrayList<String>> result = Lists.newArrayList();
		final int totalDocs = DocumentUtils.getTotalDocs();
		ArrayList<String> row = Lists.newArrayList();
		row.add("Total keywords:");
		row.add(termList.size() + "");
		row.add("Total bug reports:");
		row.add(totalDocs + "");
		result.add(row);

		row = Lists.newArrayList();
		row.add("Keyword");
		row.add("#BugReports");
		row.add("#NewCovered");
		row.add("%TotalCovered");
		result.add(row);

		final DecimalFormat df = new DecimalFormat("0.000");
		final BitSet coveredDocs = new BitSet();
		int coverage = 0;
		for (int j = 0; j < termList.size(); j++) {
			final String term = termList.get(j);
			final TopDocs docs = SearchEngineUtils.search(indexField, term, totalDocs);
			int coverageContribution = 0;
			for (int i = 0; i < docs.scoreDocs.length; i++) {
				final int docId = docs.scoreDocs[i].doc;
				if (coveredDocs.get(docId) == false) {
					coverageContribution++;
					coverage++;
					coveredDocs.set(docId, true);
				}
			}

			row = Lists.newArrayList();
			row.add(term);
			row.add(termFreqMap.get(term) + "");
			row.add(coverageContribution + "");
			row.add(df.format((double) coverage / (double) totalDocs));
			result.add(row);
		}

		return result;
	}
}
