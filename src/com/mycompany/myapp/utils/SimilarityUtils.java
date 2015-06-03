package com.mycompany.myapp.utils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.commons.math.linear.OpenMapRealVector;
import org.apache.commons.math.linear.RealVectorFormat;
import org.apache.commons.math.linear.SparseRealVector;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.DocsEnum;
import org.apache.lucene.index.Fields;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.MultiFields;
import org.apache.lucene.index.Terms;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.search.DocIdSetIterator;
import org.apache.lucene.search.similarities.DefaultSimilarity;
import org.apache.lucene.search.similarities.TFIDFSimilarity;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.BytesRef;

import com.mycompany.myapp.config.Config;
import com.mycompany.myapp.poi.DataUtils;

/**
 * Once you have the index, find the list of all the terms in the "content"
 * field across the entire index. These terms would represent the entries in the
 * document vector for a document. Then for the documents which you want to
 * consider for your similarity computation, extract its term vector. The term
 * vector gives you two arrays, an array of terms within this document, and the
 * corresponding frequency of that term in this document. Using these three data
 * structures, it is easy to construct a (sparse) document vector representing
 * the document(s).Once you have the two document vectors, the similarity
 * between the two can be computed using Cosine Similarity (or other measure -
 * most of them work with document vectors).
 * 
 * @author martin.wang
 *
 */
public class SimilarityUtils {
	
	private class DocVector {
		public Map<String, Integer> term_pos;
		public Map<Integer, String> pos_terms;
		public SparseRealVector vector;

		public DocVector(Map<String, Integer> terms) {
			this.term_pos = terms;
			this.vector = new OpenMapRealVector(terms.size());

			pos_terms = new HashMap<Integer, String>();
			Iterator<String> termSet = term_pos.keySet().iterator();
			while (termSet.hasNext()) {
				String term = termSet.next();
				int pos = term_pos.get(term);
				pos_terms.put(pos, term);
			}
		}

		public void setEntry(String term, int freq) {
			if (term_pos.containsKey(term)) {
				int pos = term_pos.get(term);
				vector.setEntry(pos, (double) freq);
			}
		}

		public void setEntry(int pos, int freq) {
			if (pos_terms.containsKey(pos)) {
				vector.setEntry(pos, (double) freq);
			}

		}

		public void normalize() {
			double sum = vector.getL1Norm();
			vector = (SparseRealVector) vector.mapDivide(sum);
		}

		public double getEntry(int pos) {

			if (vector.getDimension() >= pos) {
				return vector.getEntry(pos);
			} else {
				return -1;
			}
		}

		public String toString() {
			RealVectorFormat formatter = new RealVectorFormat();
			return formatter.format(vector);
		}

		public double[] toVector() {
			return vector.getData();
		}
	}

	private double getCosineSimilarity(DocVector d1, DocVector d2) {
		return (d1.vector.dotProduct(d2.vector))
				/ (d1.vector.getNorm() * d2.vector.getNorm());
		
	}

	/**
	 * Get the confusion matrix for a set of interested terms
	 * 
	 * @param field
	 * @param interestedFieldList
	 *            :a set of interested terms
	 */
	public void getPairwiseAnalysisBasedOnTerms(String field,
			ArrayList<String> interestedTermList) {
		try {
			IndexReader reader = DirectoryReader.open(FSDirectory
					.open(new File(Config.directoryPath)));

			// 1. List all terms across all documents;
			Fields fields = MultiFields.getFields(reader);
			Terms terms_temp = fields.terms(field);
			TermsEnum termsEnum = terms_temp.iterator(null);

			Map<String, Integer> terms = new HashMap<String, Integer>();
			Map<Integer, String> pos_terms = new HashMap<Integer, String>();
			int pos = 0;

			BytesRef byteRef = null;
			if (interestedTermList == null) {
				while ((byteRef = termsEnum.next()) != null) {
					String termText = new String(byteRef.bytes, byteRef.offset,
							byteRef.length);
					terms.put(termText, pos);
					pos_terms.put(pos, termText);
					pos++;
				}
			} else {
				for (int i = 0; i < interestedTermList.size(); i++) {
					String termText = interestedTermList.get(i);
					terms.put(termText, pos);
					pos_terms.put(pos, termText);
					pos++;
				}
			}

			// 2. Construct term vector for each document
			DocVector[] docs = new DocVector[reader.maxDoc()];
			for (int i = 0; i < reader.maxDoc(); i++) {
				int docID = i;
				terms_temp = reader.getTermVector(docID, field);

				if (terms_temp != null) {
					DocVector doc = new DocVector(terms);
					termsEnum = terms_temp.iterator(null);
					while ((byteRef = termsEnum.next()) != null) {
						String termText = byteRef.utf8ToString();
						int termFreq = termsEnum.docFreq();
						doc.setEntry(termText, termFreq);
					}
					docs[i] = doc;
				}
			}

			// 3. Pairwise analysis: for each pair of terms
			int total = terms.values().size();
			int[][] pairwise_matrix = new int[total][total];

			// for i-th term
			for (int i = 0; i < total; i++) {
				// for (i, i) element
				// System.out.println(i);
				pairwise_matrix[i] = new int[total];
				int counter = 0;
				for (int k = 0; k < docs.length; k++) {
					DocVector doc = docs[k];
					// if(k==984){
					// System.out.println(i+":"+k);
					// }

					if (doc != null && doc.getEntry(i) > 0) {
						counter++; // add counter if contains i-th element
					}
				}
				pairwise_matrix[i][i] = counter;

				// for (i, j) element (j > i)
				for (int j = i + 1; j < terms.values().size(); j++) {
					counter = 0;
					// for each document
					for (int k = 0; k < docs.length; k++) {
						DocVector doc = docs[k];
						if (doc != null && doc.getEntry(i) > 0
								&& doc.getEntry(j) > 0) {
							// add counter if contains both i- and j-the element
							counter++;
						}
					}
					pairwise_matrix[i][j] = counter;
				}
			}

			String[] labels = new String[total];
			for (int i = 0; i < labels.length; i++) {
				labels[i] = pos_terms.get(i);
			}

			String result = SearchEngineUtils.printConfusionMatrix(labels, pairwise_matrix);
			boolean overwrite = true;
			String filePath = "./resource/PairwiseAnalysis.txt";
			DataUtils.saveToFile(result, filePath, overwrite);

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void getPairwiseAnalysisBasedOnTerms(String field) {
		this.getPairwiseAnalysisBasedOnTerms(field, null);
	}

	
	public DocVector[] genTFIDFModel(String field) {
		IndexReader reader = null;
		try {
			reader = DirectoryReader.open(FSDirectory.open(new File(
					Config.directoryPath)));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return this.genTFIDFModel(field, reader.maxDoc());
	}
	
	/**Lucene API:use TFIDFSimilarity class to get TF-IDF model 
	 * 
	 * @param field
	 */
	public void genLuceneTFIDFModel(String field){
		IndexReader reader = null;
		try {
			reader = DirectoryReader.open(FSDirectory.open(new File(
					Config.directoryPath)));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		this.getIdfs(reader, field);
		this.getTF(reader, field);
	}

	/**
	 * Lucene API: gen TF-IDF model across all documents
	 * 
	 * @param field
	 */
	public void genTFIDFModelBasedOnLucene(String field) {
		try {
			IndexReader reader = DirectoryReader.open(FSDirectory
					.open(new File(Config.directoryPath)));

			// 1. list all terms across all documents
			Fields fields = MultiFields.getFields(reader);
			Terms terms_temp = fields.terms(field);
			TermsEnum termsEnum = terms_temp.iterator(null);

			// 2. list doc frequency and term frequency
			// API 1: DF: termsEnum.docFreq(); TF: docsEnum.freq()
			if (termsEnum != null) {
				BytesRef byteRef = null;
				while ((byteRef = termsEnum.next()) != null) {
					String termText = new String(byteRef.bytes, byteRef.offset,
							byteRef.length);
					System.out.println("DF:" + termText + "="
							+ termsEnum.docFreq());

					DocsEnum docsEnum = termsEnum.docs(null, null,
							DocsEnum.FLAG_FREQS);
					while (docsEnum.nextDoc() != DocIdSetIterator.NO_MORE_DOCS) {
						int tf = docsEnum.freq();
						int docID = docsEnum.docID();
						System.out.println("TF:docID=" + docID + ";" + termText
								+ "=" + tf);
					}
				}
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	TFIDFSimilarity tfidfSIM = new DefaultSimilarity();
	Map<String, Float> docFrequencies = new HashMap<>();
	Map<String, Float> termFrequencies = new HashMap<>();
	Map<String, Float> tf_Idf_Weights = new HashMap<>();
	Map<Integer, Map<String, Float>> doc_term_tf_Idf_Weights = new HashMap<>();
	
	Map<String, Float> getIdfs(IndexReader reader, String fieldStr){
		try {
			Fields fields = MultiFields.getFields(reader);

			for (String field : fields) {
				TermsEnum termEnum = MultiFields.getTerms(reader, field).iterator(
						null);
				BytesRef bytesRef;
				while ((bytesRef = termEnum.next()) != null) {

					String term = bytesRef.utf8ToString();
					float idf = tfidfSIM.idf(termEnum.docFreq(), reader.numDocs());
					docFrequencies.put(term, idf);
				}
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return docFrequencies;
	}

	public Map<String, Float> getTF(IndexReader reader, String field) {
		try {
			for (int docID = 0; docID < reader.maxDoc(); docID++) {
				TermsEnum termsEnum = MultiFields.getTerms(reader, field).iterator(
						null);
				DocsEnum docsEnum = null;
				
				Terms vector = reader.getTermVector(docID, field);
				if(vector != null){
					termsEnum = vector.iterator(null);
					if(termsEnum != null){
						BytesRef bytesRef = null;
						
						Map<String, Float> term_tf_Idf_Weights = new HashMap<>();
						
						while ((bytesRef = termsEnum.next()) != null) {
							if (termsEnum.seekExact(bytesRef)) {
								String term = bytesRef.utf8ToString();
								float tf = 0;

								docsEnum = termsEnum.docs(null, null, DocsEnum.FLAG_FREQS);
								while (docsEnum.nextDoc() != DocIdSetIterator.NO_MORE_DOCS) {
									tf = tfidfSIM.tf(docsEnum.freq());
									termFrequencies.put(term, tf);
								}
								
								if(docFrequencies.containsKey(term)){
									float idf = docFrequencies.get(term);
									float w = tf * idf;
									term_tf_Idf_Weights.put(term, w);	
								}else{
									System.out.println("Term does not Exist in TF:" + term);
								}
							}
						}
						
						doc_term_tf_Idf_Weights.put(docID, term_tf_Idf_Weights);
					}	
				}
				
				
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return termFrequencies;
	}

	/**
	 * Naive methods: Get the TF-IDF model for a specific set of documents
	 * 
	 * @param field
	 * @param maxDoc
	 */
	public DocVector[] genTFIDFModel(String field, int maxDoc) {
		DocVector[] results = new DocVector[maxDoc];

		// 1.Generate TF-Matrix
		DocVector[] TFMatrix = this.getTermVector(field, maxDoc);

		// 2.Generate IDF-Matrix
		DocVector IDFMatrix = new DocVector(TFMatrix[0].term_pos);
		for (int i = 0; i < IDFMatrix.term_pos.size(); i++) {
			// for i-th term t, idf = N/(1+x); where x is the num of documents
			// that contain t
			int df = 0;
			for (DocVector doc : TFMatrix) {
				double d = doc.getEntry(i);
				if (d > 0) {
					df++;
				}
			}
			IDFMatrix.setEntry(i, df);
		}

		// 3. Get labels
		Map<Integer, String> pos_terms = IDFMatrix.pos_terms;
		String[] terms = new String[pos_terms.size()];
		for (int i = 0; i < terms.length; i++) {
			String term = pos_terms.get(i);
			terms[i] = term;
		}

		// 4. Print TF matrix
		DocVector doc = TFMatrix[0];
		double[][] tfMatrix = new double[maxDoc][doc.pos_terms.size()];
		for (int i = 0; i < tfMatrix.length; i++) {
			doc = TFMatrix[i];
			tfMatrix[i] = doc.toVector();
		}
		System.out.println("Term Frequency:");
		SearchEngineUtils.printConfusionMatrix(terms, tfMatrix);

		// Print IDF matrix
		double[][] idfMatrix = new double[1][];
		idfMatrix[0] = new double[doc.pos_terms.size()];
		for (int i = 0; i < idfMatrix.length; i++) {
			idfMatrix[0] = IDFMatrix.toVector();
		}
		System.out.println("Doc Frequency:");
		SearchEngineUtils.printConfusionMatrix(terms, idfMatrix);

		return results;
	}

	/**
	 * Each document is represented as a term vector (terms across all
	 * documents)
	 * 
	 * @param field
	 * @param maxDoc
	 * @return
	 */
	public DocVector[] getTermVector(String field, int maxDoc) {
		DocVector[] docs = new DocVector[maxDoc];

		try {
			IndexReader reader = DirectoryReader.open(FSDirectory
					.open(new File(Config.directoryPath)));

			// 1. list all terms across all documents
			Fields fields = MultiFields.getFields(reader);
			Terms terms_temp = fields.terms(field);
			TermsEnum termsEnum = terms_temp.iterator(null);

			if (termsEnum != null) {
				Map<String, Integer> terms = new HashMap<String, Integer>();
				int pos = 0;

				BytesRef byteRef = null;
				while ((byteRef = termsEnum.next()) != null) {
					String termText = new String(byteRef.bytes, byteRef.offset,
							byteRef.length);
					terms.put(termText, pos++);
				}

				// 2. initialize term frequency for a specific doc
				for (int i = 0; i < maxDoc; i++) {
					int docID = i;

					terms_temp = reader.getTermVector(docID, field);

					if (terms_temp != null) {
						DocVector doc = new DocVector(terms);
						termsEnum = terms_temp.iterator(null);
						while ((byteRef = termsEnum.next()) != null) {
							String termText = byteRef.utf8ToString();
							int termFreq = termsEnum.docFreq();
							doc.setEntry(termText, termFreq);
						}
						docs[i] = doc;
					}
				}
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return docs;
	}

	/**
	 * Pairwise document similarity
	 * 
	 * @param field
	 * @param maxDoc
	 *            : the number of docs to analyze
	 */
	public void getDocumentSimilarityBasedOnTermVector(String field, int maxDoc) {
		DocVector[] docs = this.getTermVector(field, maxDoc);

		double[][] similarities = new double[maxDoc][maxDoc];
		for (int i = 0; i < maxDoc; i++) {
			similarities[i] = new double[maxDoc];
			DocVector doc1 = docs[i];
			for (int j = i + 1; j < maxDoc; j++) {
				DocVector doc2 = docs[j];
				double cosim = getCosineSimilarity(doc1, doc2);
				similarities[i][j] = cosim;
			}
		}

		String[] labels = new String[maxDoc];
		for (int i = 0; i < maxDoc; i++) {
			labels[i] = i + "";
		}

		String result = SearchEngineUtils.printConfusionMatrix(labels, similarities);
		boolean overwrite = true;
		String filePath = "./resource/DocSimilarity.txt";
		DataUtils.saveToFile(result, filePath, overwrite);
	}

	/**
	 * Get document similarity based on term vector
	 * 
	 * @param field
	 * @param docIds
	 */
	public void getDocumentSimilarityBasedOnTermVector(String field,
			int[] docIds) {
		DocVector[] docs = new DocVector[docIds.length];
		double cosim01 = getCosineSimilarity(docs[docIds[0]], docs[docIds[1]]);
		System.out.println("Cosine Similarity between document = " + cosim01);
	}

}
