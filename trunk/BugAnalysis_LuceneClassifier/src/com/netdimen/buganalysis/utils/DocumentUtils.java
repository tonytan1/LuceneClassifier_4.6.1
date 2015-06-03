package com.netdimen.buganalysis.utils;

import java.io.File;
import java.io.IOException;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.Fields;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.MultiFields;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.Terms;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.Version;

import com.netdimen.buganalysis.config.Config;

/**
 * Retrieve information to construct TF-IDF model
 * 
 * @author martin.wang
 *
 */
public class DocumentUtils {

	/**
	 * Get the number of documents in this index
	 * 
	 * @return
	 */
	public static int getTotalDocs() {

		int totalDocs = -1;

		try {
			final IndexReader reader = DirectoryReader.open(FSDirectory.open(new File(Config.directoryPath)));
			totalDocs = reader.numDocs();
		}
		catch (CorruptIndexException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return totalDocs;
	}

	public static int getDocID(final String field, final String value) {

		return getDocID(new Term(field, value));
	}

	public static int getDocID(final Term term) {

		int docID = -1;
		try {
			final IndexReader reader = DirectoryReader.open(FSDirectory.open(new File(Config.directoryPath)));
			final IndexSearcher searcher = new IndexSearcher(reader);
			final QueryParser parser = new QueryParser(Version.LUCENE_46, term.field(), new StandardAnalyzer(Version.LUCENE_46));
			final Query query = parser.parse(term.text());
			final TopDocs docs = searcher.search(query, 1);
			docID = docs.scoreDocs[0].doc;
		}
		catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		catch (org.apache.lucene.queryparser.classic.ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return docID;
	}

	public static void printDocument(final int docID) {

		try {
			final IndexReader reader = DirectoryReader.open(FSDirectory.open(new File(Config.directoryPath)));
			final Fields fields = MultiFields.getFields(reader);

			System.out.print("DocID=" + docID);
			for (final String field : fields) {
				if (field.equalsIgnoreCase("Summary")) {
					System.out.print(";field=" + field + ";term=");

					final TermsEnum termEnum = reader.getTermVector(docID, field).iterator(null);
					// TermsEnum termEnum = MultiFields.getTerms(reader, field).iterator(null); //what's difference
					// here?
					BytesRef bytesRef;
					while ((bytesRef = termEnum.next()) != null) {
						final String term = new String(bytesRef.bytes, bytesRef.offset, bytesRef.length);
						System.out.print(term + ",");
					}
				}
			}
			System.out.println("\n");
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

	/**
	 * Get term frequency for a specific term
	 * 
	 * @param docId
	 * @param field
	 * @param value
	 * @return
	 */
	public static int getTermFreq(final int docId, final String field, final String value) {

		int termFreq = -1;
		try {
			final IndexReader reader = DirectoryReader.open(FSDirectory.open(new File(Config.directoryPath)));
			final Terms vector = reader.getTermVector(docId, field);

			TermsEnum termsEnum = MultiFields.getTerms(reader, field).iterator(null);
			termsEnum = vector.iterator(termsEnum);
			BytesRef bytesRef; // how to locate term quickly
			while ((bytesRef = termsEnum.next()) != null) {
				final String termText = bytesRef.utf8ToString();
				if (termText.equals(value)) {
					termFreq = termsEnum.docFreq();
					break;
				}
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
		return termFreq;
	}

	public static int getDocFreq(final String field, final String value) {

		return getDocFreq(new Term(field, value));
	}

	/**
	 * Get document frequency for a specific term
	 * 
	 * @param term
	 * @return
	 */
	public static int getDocFreq(final Term term) {

		int docFreq = -1;
		try {
			final IndexReader reader = DirectoryReader.open(FSDirectory.open(new File(Config.directoryPath)));
			docFreq = reader.docFreq(term);
		}
		catch (CorruptIndexException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return docFreq;
	}

}
