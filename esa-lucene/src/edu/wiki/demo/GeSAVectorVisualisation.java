package edu.wiki.demo;

import java.awt.Dimension;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
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

import org.tc33.jheatchart.HeatChart;

import edu.wiki.api.concept.IConceptIterator;
import edu.wiki.api.concept.IConceptVector;
import edu.wiki.modify.IndexModifier;
import edu.wiki.search.ESASearcher;
import edu.wiki.util.GeoUtil;
import edu.wiki.util.ScoredCentroid;

public class GeSAVectorVisualisation {
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

		levelOfDetail = Integer.parseInt(args[1]);
		threshold = Double.parseDouble(args[2]);
		double[][] data = new double[GeoUtil.MapSize(levelOfDetail)][GeoUtil.MapSize(levelOfDetail)];
		System.out.println(GeoUtil.MapSize(levelOfDetail) + " " +GeoUtil.MapSize(levelOfDetail));
		int total = 0;
		double max = 0;
		while((line = br.readLine()) != null){
			final String [] parts = line.split("\t");
			if(parts.length != 6) {				
				break;
			}
			
			int[] xy = GeoUtil.LatLongToXY(Double.valueOf(parts[4]), Double.valueOf(parts[5]), levelOfDetail);
//			if(data[xy[1]][xy[0]] != 1.0) {
//				data[xy[1]][xy[0]] = 1.0;
//			}
			if(data[xy[1]][xy[0]] > max) {
				max = data[xy[1]][xy[0]];
			}
			
			if(data[xy[1]][xy[0]] < 1.0) {
				data[xy[1]][xy[0]] += Double.valueOf(parts[2]);
			}
			total++;
		}
		System.out.println(max + " max");
//		for (int i = 0; i < GeoUtil.MapSize(levelOfDetail); i++){
//			for (int j = 0; j < GeoUtil.MapSize(levelOfDetail); i++){
//				data[i][j] = data[i][j]/max;
//			}	
//		}
		System.out.println(total + " samples");
		HeatChart map = new HeatChart(data);
//		map.setCellHeight(1);
//		map.setCellWidth(1);
		map.setCellSize(new Dimension(1,1));
		map.setAxisThickness(0);
		map.setShowXAxisValues(false);
		map.setShowYAxisValues(false);
		map.saveToFile(new File("/home/chrisschaefer/Arbeitsfläche/java-heat-chart.png"));
		br.close();	
		is.close();
	}
	
	
}
