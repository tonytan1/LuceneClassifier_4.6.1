package com.netdimen.buganalysis.utils;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.classification.ClassificationResult;
import org.apache.lucene.classification.SimpleNaiveBayesClassifier;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.AtomicReader;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.SlowCompositeReaderWrapper;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.Version;

import com.netdimen.buganalysis.config.Config;

/**
 * Supervised machine learning algorithm (Naive Bayes Model) to auto-classify bug titles.
 * 
 * @author martin.wang
 *
 */
public class ClassifierUtils {

	public static final String INDEX = Config.directoryPath;

	public static final String[] CATEGORIES = { "error", "user", "module", "report", "editor", "exam", "learning", "catalog", "session" };

	private static int[][] counts;

	private static Map<String, Integer> catindex;

	/**
	 * Apply Naive Bayes Classifier to auto-assign labels to a specific bug title
	 * 
	 */
	public static void runNaiveBayesClassifier() {

		init();

		try {
			final long startTime = System.currentTimeMillis();
			final SimpleNaiveBayesClassifier classifier = new SimpleNaiveBayesClassifier();
			final IndexReader reader = DirectoryReader.open(dir());
			final AtomicReader ar = SlowCompositeReaderWrapper.wrap(reader);
			// Note that the cat field is a classification class while body field is the target learning field.
			/*
			 * classifier.train(ar, "Category", "Summary", new StandardAnalyzer( Version.LUCENE_46));
			 */
			classifier.train(ar, "Summary", "Category", new StandardAnalyzer(Version.LUCENE_46)); // training
			final int maxdoc = reader.maxDoc();
			for (int i = 0; i < maxdoc; i++) {
				final Document doc = ar.document(i);
				final String correctAnswer = doc.get("Category");
				final int cai = idx(correctAnswer);
				final ClassificationResult<BytesRef> result = classifier.assignClass(doc.get("Summary")); // label the result
				final String classified = result.getAssignedClass().utf8ToString();

				if (correctAnswer == null) {
					System.out.println("\"" + doc.get("Summary") + "\"" + " is classified as:" + classified);
				}

				final int cli = idx(classified);
				if (cli > -1 && cai > -1) {
					counts[cai][cli]++;
				} else {
					/*
					 * if(cli == -1){ System.out.println("Unknown category:" + classified); }
					 * 
					 * if(cai == -1){ System.out.println("Unknown summary:" + correctAnswer); }
					 */
				}
			}
			final long endTime = System.currentTimeMillis();
			final int elapse = (int) (endTime - startTime) / 1000;

			// print results
			final String result = SearchEngineUtils.printConfusionMatrix(CATEGORIES, counts);
			final boolean overwrite = true;
			final String filePath = "./resource/NaiveBayesClassification.txt";
			DataUtils.saveToFile(result, filePath, overwrite);
			reader.close();
		}
		catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	public static Directory dir() throws IOException {

		return FSDirectory.open(new File(INDEX));
	}

	public static void init() {

		counts = new int[CATEGORIES.length][CATEGORIES.length];
		catindex = new HashMap<String, Integer>();
		for (int i = 0; i < CATEGORIES.length; i++) {
			catindex.put(CATEGORIES[i], i);
		}
	}

	static int idx(final String cat) {

		if (!catindex.containsKey(cat)) {
			return -1;
		} else {
			return catindex.get(cat);
		}
	}
}
