package edu.wiki.demo;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import edu.wiki.api.concept.IConceptIterator;
import edu.wiki.api.concept.IConceptVector;
import edu.wiki.modify.IndexModifier;
import edu.wiki.search.ESASearcher;
import edu.wiki.util.GeoUtil;
import edu.wiki.util.ScoredCentroid;

public class GeSAVectorPruner {
	static int levelOfDetail;
	static double threshold;
	static int maxPossibleFrequency = 7461;
	/**
	 * @param args
	 * @throws IOException 
	 * @throws SQLException 
	 * @throws ClassNotFoundException 
	 */
	public static void main(String[] args) throws ClassNotFoundException, SQLException, IOException {
		//FileReader is = new FileReader("/home/chrisschaefer/Arbeitsfläche/GeSA_dump2.txt");
		FileReader is = new FileReader(args[0]);
		BufferedReader br = new BufferedReader(is);
		String line;
		String currentTerm = "";
		levelOfDetail = Integer.parseInt(args[1]);
		threshold = Double.parseDouble(args[2]);
		ArrayList<String> ids = new ArrayList<String>();
		ArrayList<Double> tfidfScores = new ArrayList<Double>();
		ArrayList<Double> coordinates = new ArrayList<Double>();
		while((line = br.readLine()) != null){
			final String [] parts = line.split("\t");
			if(parts.length != 6) {				
				break;
			}
			if (currentTerm.equals(parts[0]) && !currentTerm.equals("")) {
				ids.add(parts[1]);
				tfidfScores.add(Double.valueOf(parts[2]));
				coordinates.add(Double.valueOf(parts[4]));
				coordinates.add(Double.valueOf(parts[5]));
			}
			else {
				tfirf(currentTerm, ids, tfidfScores, coordinates);
				ids.clear();
				tfidfScores.clear();
				coordinates.clear();
				ids.add(parts[1]);
				tfidfScores.add(Double.valueOf(parts[2]));
				coordinates.add(Double.valueOf(parts[4]));
				coordinates.add(Double.valueOf(parts[5]));
				currentTerm = parts[0];
			}
		}
		tfirf(currentTerm, ids, tfidfScores, coordinates);
		br.close();	
		is.close();
	}
	
	// term frequency - inverse raster frequency
	private static void tfirf (String term, ArrayList<String> ids, 
			ArrayList<Double> tfidfScores, ArrayList<Double> coordinates) {
		HashMap<Long, Integer> rasterMap = new HashMap<Long, Integer>(30);

		for(int i = 0; i < coordinates.size(); i+=2) {
            
			long raster = GeoUtil.LatLongToPixel(coordinates.get(i), coordinates.get(i+1), levelOfDetail);
            if(rasterMap.containsKey(raster)){
            	int vint = rasterMap.get(raster);
            	rasterMap.put(raster, vint+1);
            }
            else {
            	rasterMap.put(raster, 1);
            }            
		}
		double totalNumberOfRasters = GeoUtil.MapSize(levelOfDetail);
		double irf = Math.log(totalNumberOfRasters/ rasterMap.size());
		double maxPossibleIrf = Math.log(totalNumberOfRasters/ 1);
		for(int i = 0; i < coordinates.size(); i+=2) {
            
			long raster = GeoUtil.LatLongToPixel(coordinates.get(i), coordinates.get(i+1), levelOfDetail);
			double tf = Math.log(rasterMap.get(raster) + 1);
			double tfirfNormalized = tf*irf / (maxPossibleFrequency * maxPossibleIrf);
			if(tfirfNormalized > threshold) {
				System.out.print(term + "\t"); 
				System.out.print(ids.get(i/2) + "\t"); 
				System.out.print(String.valueOf(coordinates.get(i)) + "\t"); 
				System.out.print(String.valueOf(coordinates.get(i+1)) + "\t"); 
				System.out.print(String.valueOf(tfidfScores.get(i/2)) + "\t"); 
				System.out.print(String.valueOf(tfirfNormalized) + "\n");
			}
		}
	}
	
}
