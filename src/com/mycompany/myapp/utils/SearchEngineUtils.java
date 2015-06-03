package com.mycompany.myapp.utils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringReader;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
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

import com.mycompany.myapp.config.Config;
import com.mycompany.myapp.poi.DataUtils;
import com.mycompany.myapp.poi.POIUtils;

/**Build index, and search keyword with Lucene.
 * 
 * @author martin.wang
 *
 */
public class SearchEngineUtils {
	
	
	
	/**Get all terms and sort them based on term frequency
	 * 
	 * @param field
	 * @return
	 */
	public static TermFrequencyObject getAllTermsAndSort(String field){
		List<String> termList = new ArrayList<String>();
		Map<String, Integer> termFreqMap = new HashMap<String, Integer>();
			try {
				IndexReader reader = DirectoryReader.open(FSDirectory
						.open(new File(Config.directoryPath)));

				Fields fields = MultiFields.getFields(reader);
				Terms terms = fields.terms(field);

				TermsEnum termEnum = terms.iterator(null);
				BytesRef bytesRef;
				while ((bytesRef = termEnum.next()) != null) {
					String termText = bytesRef.utf8ToString();
					int frequency = termEnum.docFreq();
					termFreqMap.put(termText, frequency);
					termList.add(termText);
				}

				reader.close();
				// sort the term map by frequency descending
				Collections.sort(termList, new ReverseComparator<String>(
						new ValueComparator<String, Integer>(termFreqMap)));
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
		TermFrequencyObject obj = new TermFrequencyObject(termList, termFreqMap);
		
		
		
		return obj;
	}
	
	/**Get Top-N terms
	 * 
	 * @param topTermCutoff
	 * @param field
	 * @param TopN
	 * @return
	 */
	public static ArrayList<String> getTopTerms(float topTermCutoff, String field, int TopN){
		ArrayList<String> topTerms = new ArrayList<String>();
		TermFrequencyObject obj = SearchEngineUtils.getAllTermsAndSort(field);
		Map<String, Integer> termFreqMap = obj.getTermFreqMap();
		List<String> termList = obj.getTermList();
		
		// retrieve the top terms based on topTermCutof
		float topFreq = -1.0f;
		for (String term : termList) {
			if (topFreq < 0.0f) {
				// first term , capture the value
				topFreq = (float) termFreqMap.get(term);
				topTerms.add(term);
			} else {
				// not the first term, compute the ratio and discard if
				// below topTermCutoff score
				float ratio = (float) ((float) termFreqMap.get(term) / topFreq);
				if (ratio > topTermCutoff && topTerms.size() < TopN) {
					topTerms.add(term);
				} else {
					break;
				}
			}
		}

		StringBuilder sb = new StringBuilder();
		sb.append(">>> top ").append(TopN).append(" terms: ");
		int counter = 1;			
		for (String topTerm : topTerms) {
			if(counter%1 == 0){
				sb.append("\n");
			}
			sb.append(counter).append(":").append(topTerm).append("(")
			.append(termFreqMap.get(topTerm)).append(");");
			counter ++;
		}
		
		System.out.println(sb.toString());
		String filePath = "./resource/TopTerms.txt";
		boolean overwrite = true;
		DataUtils.saveToFile(sb.toString(), filePath, overwrite);

		return topTerms;
	}
	
	/**
	 * Computes a term frequency map for the index at the specified location.
	 * Builds a Boolean OR query out of the "most frequent" terms in the index
	 * and returns it. "Most Frequent" is defined as the terms whose frequencies
	 * are greater than or equal to the topTermCutoff * the frequency of the top
	 * term, where the topTermCutoff is number between 0 and 1.
	 * 
	 * @param topTermCutoff
	 * @param field
	 * @return
	 */
	public static BooleanQuery computeTopTermQuery(float topTermCutoff,
			String field) {
		BooleanQuery query = null;

		try {
			Map<String, Integer> termFreqMap = new HashMap<String, Integer>();
			List<String> termList = new ArrayList<String>();

			IndexReader reader = DirectoryReader.open(FSDirectory
					.open(new File(Config.directoryPath)));

			Fields fields = MultiFields.getFields(reader);
			Terms terms = fields.terms(field);

			TermsEnum termEnum = terms.iterator(null);
			BytesRef bytesRef;
			while ((bytesRef = termEnum.next()) != null) {
				String termText = bytesRef.utf8ToString();
				int frequency = termEnum.docFreq();
				termFreqMap.put(termText, frequency);
				termList.add(termText);
			}

			reader.close();
			// sort the term map by frequency descending
			Collections.sort(termList, new ReverseComparator<String>(
					new ValueComparator<String, Integer>(termFreqMap)));

			// retrieve the top terms based on topTermCutof
			List<String> topTerms = new ArrayList<String>();
			float topFreq = -1.0f;
			for (String term : termList) {
				if (topFreq < 0.0f) {
					// first term , capture the value
					topFreq = (float) termFreqMap.get(term);
					topTerms.add(term);
				} else {
					// not the first term, compute the ratio and discard if
					// below topTermCutoff score
					float ratio = (float) ((float) termFreqMap.get(term) / topFreq);
					if (ratio > topTermCutoff) {
						topTerms.add(term);
					} else {
						break;
					}
				}
			}

			StringBuilder termBuf = new StringBuilder();
			query = new BooleanQuery();

			QueryParser parser = new QueryParser(Version.LUCENE_46, "Summary",
					new StandardAnalyzer(Version.LUCENE_46));
			for (String topTerm : topTerms) {
				termBuf.append(topTerm).append("(")
						.append(termFreqMap.get(topTerm)).append(");");
				query.add(parser.parse(topTerm), Occur.SHOULD); // "OR" operator
			}

			System.out.println(">>> top terms: " + termBuf.toString());
//			System.out.println(">>> query: " + query.toString());

		} catch (CorruptIndexException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ParseException e) {
			e.printStackTrace();
		}

		return query;
	}

	public static String[] searchIndex(Query query, int TopN) {
		SortedMap<Integer, String> ID_TextMap = new TreeMap<Integer, String>();
		try {

			IndexSearcher searcher = new IndexSearcher(
					DirectoryReader.open(FSDirectory.open(new File(
							Config.directoryPath))));

			Query q = new QueryParser(Version.LUCENE_46, "Summary",
					new StandardAnalyzer(Version.LUCENE_46)).parse(query
					.toString());
			System.out.println(q.toString());
			TopDocs hits = SearchEngineUtils.search(q, TopN);
			for (ScoreDoc scoreDoc : hits.scoreDocs) {
				int docID = scoreDoc.doc;
				Document doc = searcher.doc(docID);
				ID_TextMap
						.put(scoreDoc.doc, StringUtils.chomp(doc.get("text")));
			}

		} catch (CorruptIndexException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ParseException e) {
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
	public static void index(String field, String value) {
		try {
			StandardAnalyzer analyzer = new StandardAnalyzer(Version.LUCENE_46);
			FSDirectory directory = FSDirectory.open(new File(
					Config.directoryPath));

			IndexWriterConfig config = new IndexWriterConfig(Version.LUCENE_46,
					analyzer);
			IndexWriter writer = new IndexWriter(directory, config);

			Document doc = new Document();
			doc.add(new Field(field, value, Field.Store.YES,
					Field.Index.ANALYZED,
					Field.TermVector.WITH_POSITIONS_OFFSETS));
			writer.addDocument(doc);

			writer.close();
			directory.close();
		} catch (CorruptIndexException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (LockObtainFailedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public static List<String> parseText(Analyzer analyzer, String text){
		List<String> results= new ArrayList<String>();
		
		try {
			
			TokenStream stream = analyzer.tokenStream(null, new StringReader(text));
			CharTermAttribute cta = stream.addAttribute(CharTermAttribute.class);
			stream.reset();
			results.add(cta.toString());
			while(stream.incrementToken()){
				results.add(cta.toString());
			}
			stream.end();
			stream.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return results;
	}
	
	public static void indexExcelFile(String srcFile) {
		try {
			StandardAnalyzer analyzer = new StandardAnalyzer(Version.LUCENE_46);
			File tmp = new File(Config.directoryPath);
			if(tmp.exists()){
				FileUtils.deleteDirectory(tmp);
			}
			FSDirectory directory = FSDirectory.open(tmp);
			IndexWriterConfig config = new IndexWriterConfig(Version.LUCENE_46,
					analyzer);
			IndexWriter writer = new IndexWriter(directory, config);

			FileInputStream file = new FileInputStream(srcFile);
			HSSFWorkbook wb = new HSSFWorkbook(file);

			int sheetNo = wb.getNumberOfSheets();
			for (int i = 0; i < sheetNo; i++) {
				HSSFSheet sheet = wb.getSheetAt(i);

				int rowIndex = 0;
				ArrayList<String> fields = POIUtils.getRowFromExcel(sheet,
						rowIndex);

				int columnIndex = 0;
				int rowNo = POIUtils.NumRows(sheet, columnIndex);

				// iterate row by row
				for (rowIndex = 1; rowIndex < rowNo; rowIndex++) { // Row 0 =
																	// field,
																	// Row 1...N
																	// = values
					ArrayList<String> values = POIUtils.getRowFromExcel(sheet,
							rowIndex);
					Document doc = new Document();
					for (int j = 0; j < fields.size(); j++) {
						String field = fields.get(j);
						if(values.size() > j){
							String value = values.get(j);
							if (value != null && !value.equals("")) {
								doc.add(new Field(field, value, Field.Store.YES,
										Field.Index.ANALYZED,
										TermVector.WITH_POSITIONS_OFFSETS));
								
							/*	List<String> results = parseText(analyzer,value);
								for(String result: results){
									if(!result.equals("")){
										doc.add(new Field(field, result, Field.Store.YES,
												Field.Index.ANALYZED,
												TermVector.WITH_POSITIONS_OFFSETS));	
									}
								}*/
							}
						}
					}

					if(rowIndex == 984){
						System.out.println(rowIndex + ":" + values);	
					}

					writer.addDocument(doc);
				}
				System.out.println(">>>Index statistics: sheet=" + sheet.getSheetName() + ";docNO=" + (rowNo-1));
			}

			writer.close();
			directory.close();
			analyzer.close();
		} catch (CorruptIndexException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (LockObtainFailedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/**
	 * Index all fields defined in a CSV file
	 * 
	 * @param srcFile
	 */
	public static void indexCSVFile(String srcFile) {
		try {
			StandardAnalyzer analyzer = new StandardAnalyzer(Version.LUCENE_46);
			FSDirectory directory = FSDirectory.open(new File(
					Config.directoryPath));
			IndexWriterConfig config = new IndexWriterConfig(Version.LUCENE_46,
					analyzer);
			IndexWriter writer = new IndexWriter(directory, config);

			BufferedReader br = new BufferedReader(new FileReader(srcFile));
			String line = br.readLine();
			String[] fields = line.split(",");

			int counter = 0;
			while ((line = br.readLine()) != null) {
				String[] values = line.split(",");
				Document doc = new Document();
				for (int i = 0; i < fields.length; i++) {
					String field = fields[i];
					String value = values[i];
					doc.add(new Field(field, value, Field.Store.YES,
							Field.Index.ANALYZED,
							TermVector.WITH_POSITIONS_OFFSETS));
					if(counter == 220){
						System.out.println(counter + ":" + field + "=" + value);	
					}
				}
				writer.addDocument(doc);
				counter++;

			}

			writer.close();
			directory.close();
			br.close();
		} catch (CorruptIndexException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (LockObtainFailedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public static TopDocs search(Query query, int TopN) {
		TopDocs docs = null;
		try {
			IndexReader reader = DirectoryReader.open(FSDirectory
					.open(new File(Config.directoryPath)));
			IndexSearcher searcher = new IndexSearcher(reader);

			docs = searcher.search(query, TopN);			
		} catch (CorruptIndexException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return docs;
	}

	public static TopDocs search(String field, String keyword, int TopN) {
//		System.out.println("Search keyword:" +  keyword);
		
		QueryParser parser = new QueryParser(Version.LUCENE_46, field,
				new StandardAnalyzer(Version.LUCENE_46));
		Query query = null;

		try {
			query = parser.parse(keyword);
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return search(query, TopN);
	}
	
	
	
	public static void showSearchResult(TopDocs docs, ArrayList<String> fields, StringBuilder sb) {

		try {
			IndexReader reader = DirectoryReader.open(FSDirectory
					.open(new File(Config.directoryPath)));
			IndexSearcher searcher = new IndexSearcher(reader);
			ScoreDoc[] hits = docs.scoreDocs;
			sb.append("Matched:" + hits.length).append("\n");
//			System.out.println("Matched:" + hits.length);

			// 1. Iterate through results:
			for (int i = 0; i < hits.length; i++) {
				Document hitDoc = searcher.doc(hits[i].doc);
				
				
				sb.append(i + ":Score=" + hits[i].score + ";");
				for(String field: fields){
					String value = hitDoc.get(field);
					sb.append(field).append("=").append(value).append(";");
				}
				sb.append("\n");
//				System.out.print(sb.toString());
			}
			System.out.println(sb.toString());
			reader.close();
			
			String filePath = "./resource/SearchResult.txt";
			boolean overwrite = true;
			DataUtils.saveToFile(sb.toString(), filePath, overwrite);
		} catch (CorruptIndexException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public static String printConfusionMatrix(String[] labels, double[][] similarities){
		
		StringBuilder sb = new StringBuilder();
		if(labels.length != similarities[0].length){
			sb.append("Label length does not match with data length").append("\n");
//			System.out.println();
		}else{
			sb.append("----------------------------------------------------------").append("\n");
			sb.append("----------------------------------------------------------").append("\n");
			sb.append("Confusion Matrix:").append("\n");
			sb.append("----------------------------------------------------------").append("\n");
//			System.out.println("----------------------------------------------------------");
//			System.out.println("----------------------------------------------------------");
//			System.out.println("Confusion Matrix:");
//			System.out.println("----------------------------------------------------------");
			
			for(int j = 0; j < labels.length; j++){
				sb.append("   "+j+" ");
//				System.out.print("   "+j+" ");
			}
			sb.append("\n\n");
//			System.out.println("\n");
			
			DecimalFormat df = new DecimalFormat("0.000");
			for (int i = 0; i < similarities.length; i++) {
				int sum = 0;
				for (int j = 0; j < similarities[i].length; j++) {
					sb.append(" "+df.format(similarities[i][j]) + " ");
//					System.out.printf(" %.3f ", similarities[i][j]);
				}
				
				if(labels.length > i){
					sb.append("\t|\t").append(sum).append("\t").append(i).append("\t=").append(labels[i]).append("\n");
//					System.out.println("\t|\t" + sum + "\t" + i +"\t=" + labels[i]);	
				}else{
					sb.append("\t|\t").append(sum).append("\t").append("\n");
//					System.out.println("\t|\t" + sum + "\t");
				}
			}
		}
		
		String result = sb.toString();
		System.out.print(result);
		
		return result;
	}
	
	public static String printConfusionMatrix(String[] labels, int[][] counts){
		StringBuilder sb = new StringBuilder();
		sb.append("----------------------------------------------------------").append("\n");
		sb.append("----------------------------------------------------------").append("\n");
		sb.append("Confusion Matrix:").append("\n");
		sb.append("----------------------------------------------------------").append("\n");
		
		/*System.out.println("----------------------------------------------------------");
		System.out.println("----------------------------------------------------------");
		System.out.println("Confusion Matrix:");
		System.out.println("----------------------------------------------------------");*/
		
		int fc = 0, tc = 0;
		
		char letter = 'a';
		for(int j = 0; j < labels.length; j++){
			sb.append(" ").append(letter).append(" ");
//			System.out.print("   "+letter+" ");
			letter++;
		}
		
		sb.append("\t<--Classified as").append("\n");
//		System.out.println("\t<--Classified as");
		letter = 'a';
		DecimalFormat df = new DecimalFormat("0");
		for (int i = 0; i < labels.length; i++) {
			int sum = 0;
			for (int j = 0; j < labels.length; j++) {
				sb.append(" ").append(df.format(counts[i][j])).append(" ");
//				System.out.printf(" %3d ", counts[i][j]);
				sum += counts[i][j];
				if (i == j) {
					tc += counts[i][j];
				} else {
					fc += counts[i][j];
				}
			}
			sb.append("\t|\t").append(sum).append("\t").append(letter).append("\t=").append(labels[i]).append("\n");
//			System.out.println("\t|\t" + sum + "\t" + letter +"\t=" + labels[i]);
			letter++;
		}
		
		float accrate = (float) tc / (float) (tc + fc);
		
		float errrate = (float) fc / (float) (tc + fc);
		sb.append("\n\n*** accuracy rate =").append(df.format(accrate)).append(";error rate =").append(df.format(errrate)).append("\n");
		
		String result = sb.toString();
		System.out.println(result);
		
		return result;
	}
}
