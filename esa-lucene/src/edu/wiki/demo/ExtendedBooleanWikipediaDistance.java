package edu.wiki.demo;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.TermAttribute;

import edu.wiki.api.concept.IConceptIterator;
import edu.wiki.api.concept.IConceptVector;
import edu.wiki.index.WikipediaAnalyzer;
import edu.wiki.modify.IndexModifier;
import edu.wiki.search.ESASearcher;
import edu.wiki.util.GeoUtil;
import edu.wiki.util.ScoredCentroid;

public class ExtendedBooleanWikipediaDistance {
	static int levelOfDetail;
	static double threshold;
	static int maxPossibleFrequency = 7461;
	static int numWikiDocs = 200000;
	static HashMap<String, IdsAndScores> index;
	static WikipediaAnalyzer analyzer;
	static int totalTerms = 0;
	/**
	 * @param args
	 * @throws IOException 
	 * @throws SQLException 
	 * @throws ClassNotFoundException 
	 */
	public static void main(String[] args) throws ClassNotFoundException, SQLException, IOException {
		FileReader is = new FileReader("/home/chrisschaefer/Arbeitsfläche/GeSA_dump2.txt");
		//FileReader is = new FileReader(args[0]);
		BufferedReader br = new BufferedReader(is);
		String line;
		String currentTerm = "";
		ArrayList<Long> ids = new ArrayList<Long>();
		ArrayList<Double> tfidfScores = new ArrayList<Double>();
		index = new HashMap<String, IdsAndScores>();
		while((line = br.readLine()) != null){
			final String [] parts = line.split("\t");
			if(parts.length != 6) {				
				break;
			}
			if (currentTerm.equals(parts[0]) && !currentTerm.equals("")) {
				ids.add(Long.valueOf(parts[1]));
				tfidfScores.add(Double.valueOf(parts[2]));
			}
			else {
				addToIndex(currentTerm, ids, tfidfScores);
				ids.clear();
				tfidfScores.clear();

				totalTerms++;
				ids.add(Long.valueOf(parts[1]));
				tfidfScores.add(Double.valueOf(parts[2]));
				currentTerm = parts[0];
			}
		}
		addToIndex(currentTerm, ids, tfidfScores);
		br.close();	
		is.close();
		System.out.println("Finish building index " + totalTerms);
		
		
		is = new FileReader("config/wordsim353-combined.tab");
		br = new BufferedReader(is);
		br.readLine(); //skip first line
		System.out.println("Word 1\tWord 2\tHuman (mean)\tScore");
		while((line = br.readLine()) != null){
			final String [] parts = line.split("\t");
			if(parts.length != 3) {
				break;
			}

			//System.out.println(line + "\t" + computeExtendedBooleanWikipediaDistance(parts[0], parts[1]));
			System.out.println(line + "\t" + computeWikipediaDistance(parts[0], parts[1]));
		}
		br.close();
		
//		System.out.println("berlin <-> washington " + index.get("washington").getWikipediaDistance(index.get("berlin").ids));		
//		System.out.println("berlin <-> washington " + index.get("washington").getExtendedBooleanWikipediaDistance(index.get("berlin")));
//		
//		System.out.println("everest <-> washington " + index.get("washington").getWikipediaDistance(index.get("everest").ids));
//		System.out.println("everest <-> washington " + index.get("washington").getExtendedBooleanWikipediaDistance(index.get("everest")));
//		
//		System.out.println("havana <-> washington " + index.get("washington").getWikipediaDistance(index.get("havana").ids));
//		System.out.println("havana <-> washington " + index.get("washington").getExtendedBooleanWikipediaDistance(index.get("havana")));
//
//		System.out.println("obama <-> washington " + index.get("washington").getWikipediaDistance(index.get("obama").ids));
//		System.out.println("obama <-> washington " + index.get("washington").getExtendedBooleanWikipediaDistance(index.get("obama")));
//
//		System.out.println("merkel <-> washington " + index.get("washington").getWikipediaDistance(index.get("merkel").ids));
//		System.out.println("merkel <-> washington " + index.get("washington").getExtendedBooleanWikipediaDistance(index.get("merkel")));
//		
//		System.out.println("obama <-> berlin " + index.get("berlin").getWikipediaDistance(index.get("obama").ids));
//		System.out.println("berlin <-> obama " + index.get("obama").getExtendedBooleanWikipediaDistance(index.get("berlin")));
//		
//		System.out.println("merkel <-> berlin " + index.get("berlin").getWikipediaDistance(index.get("merkel").ids));
//		System.out.println("berlin <-> merkel " + index.get("merkel").getExtendedBooleanWikipediaDistance(index.get("berlin")));
	}
	
	public static String preprocessQuery(String query) {
		try {
			analyzer = new WikipediaAnalyzer();
			TokenStream ts = analyzer.tokenStream("contents",new StringReader(query));
			ts.reset();
	        
	        if (ts.incrementToken()) { 
	        	TermAttribute t = ts.getAttribute(TermAttribute.class);	         
				return t.term();
	        }
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return "";        
	}
	
	// term frequency - inverse raster frequency
	private static void addToIndex (String term, ArrayList<Long> ids, 
			ArrayList<Double> tfidfScores) {
		    index.put(term, new IdsAndScores(ids, tfidfScores));

	}
	
	public static double computeExtendedBooleanWikipediaDistance(String term0, String term1) {
		term0 = preprocessQuery(term0);
		term1 = preprocessQuery(term1);
		if(index.containsKey(term0) && index.containsKey(term1)) {
			return index.get(term0).getExtendedBooleanWikipediaDistance(index.get(term1));
		}
		else {
			return 10000.0f;	// infinite distance
		}
	}
	
	public static double computeWikipediaDistance(String term0, String term1) {
		term0 = preprocessQuery(term0);
		term1 = preprocessQuery(term1);
		if(index.containsKey(term0) && index.containsKey(term1)) {
			return index.get(term0).getWikipediaDistance(index.get(term1).ids);
		}
		else {
			return 10000.0f;	// infinite distance
		}
	}
	
	public static class IdsAndScores {
		public ArrayList<Long> ids;
		public ArrayList<Double> tfidfScores;
		
		public IdsAndScores(ArrayList<Long> ids, ArrayList<Double> tfidfScores) {
			this.ids = new ArrayList<Long>(ids);
			this.tfidfScores = new ArrayList<Double>(tfidfScores);
		}
		
		int intersectionCount(ArrayList<Long> list2) {
			int count = 0;
	        for (long t : ids) {
	            if(list2.contains(t)) {
	                count++;
	            }
	        }
	        return count;
	    }
		
		public double getWikipediaDistance(ArrayList<Long> list2){
			int fCommon = intersectionCount(list2);
			if(fCommon == 0){
				return 10000.0f;	// infinite distance
			}
			
			
			double log1, log2 , logCommon, maxlog, minlog;
			log1 = Math.log(ids.size());	log2 = Math.log(list2.size());	logCommon = Math.log(fCommon);
			maxlog = Math.max(log1, log2);	minlog = Math.min(log1, log2);
			
			return (maxlog - logCommon) / (Math.log(numWikiDocs) - minlog); 
		}
		
		private double getExtendedBooleanWikipediaDistance(IdsAndScores list2){
			double fCommon = sumCommonTfidf(list2);
			if(fCommon == 0){
				return 10000.0f;	// infinite distance
			}
			
			
			double log1, log2 , logCommon, maxlog, minlog;
			log1 = Math.log(this.sumTfidf());	log2 = Math.log(list2.sumTfidf());	logCommon = Math.log(fCommon);
			maxlog = Math.max(log1, log2);	minlog = Math.min(log1, log2);
			
			return (maxlog - logCommon) / (Math.log(numWikiDocs) - minlog); 
		}
		
		public double sumTfidf() {
		     double sum= 0; 
		     for (double i:tfidfScores)
		         sum = sum + i;
		     return sum;
		}
		
		public double sumCommonTfidf(IdsAndScores list2){
			double sum = 0;
			for (int i = 0; i < ids.size(); i++) {
				for (int j = 0; j < list2.ids.size(); j++) {
		            if(ids.get(i).equals(list2.ids.get(j))) {
		            	double tfidf1 = tfidfScores.get(i);
		            	double tfidf2 = list2.tfidfScores.get(j);
		            	
		            	double sim1AND2 = 1 - Math.sqrt((Math.pow(1 - tfidf1, 2) + Math.pow(1 - tfidf2, 2)) / 2.0);
		            	
		                sum += sim1AND2;
		            }
				}
	        }
			return sum;
		}
	}
}
