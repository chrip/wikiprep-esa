package edu.wiki.demo;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.DecimalFormat;
import java.util.HashMap;

import edu.wiki.api.concept.IConceptIterator;
import edu.wiki.api.concept.IConceptVector;
import edu.wiki.modify.IndexModifier;
import edu.wiki.search.ESASearcher;
import edu.wiki.util.GeoUtil;
import edu.wiki.util.ScoredCentroid;

public class TwitterStreamTest {

	static Connection connection;
	static Statement stmtQuery;
	
	static String strTitles = "SELECT id,lat,lon FROM geoarticle WHERE id IN ";
	
	public static void initDB() throws ClassNotFoundException, SQLException, IOException {
		// Load the JDBC driver 
		String driverName = "com.mysql.jdbc.Driver"; // MySQL Connector 
		Class.forName(driverName); 
		
		// read DB config
		InputStream is = ESASearcher.class.getResourceAsStream("/config/db.conf");
		BufferedReader br = new BufferedReader(new InputStreamReader(is));
		String serverName = br.readLine();
		String mydatabase = br.readLine();
		String username = br.readLine(); 
		String password = br.readLine();
		br.close();

		// Create a connection to the database 
		String url = "jdbc:mysql://" + serverName + "/" + mydatabase; // a JDBC url 
		connection = DriverManager.getConnection(url, username, password);
		
		stmtQuery = connection.createStatement();
		stmtQuery.setFetchSize(100);

  }


	/**
	 * @param args
	 * @throws IOException 
	 * @throws SQLException 
	 * @throws ClassNotFoundException 
	 */
	public static void main(String[] args) throws ClassNotFoundException, SQLException, IOException {
		ESASearcher searcher = new ESASearcher();
		initDB();
		String line;
		
		InputStream is =  new FileInputStream("/data/chrisschaefer/gesa/twitter_full_text.txt");
		BufferedReader br = new BufferedReader(new InputStreamReader(is));
		//br.readLine(); //skip first line
		// System.out.println("Word 1\tWord 2\tHuman (mean)\tScore");
		// USER_79321756	2010-03-03T05:28:02	ÜT: 47.528139,-122.197916	47.528139	-122.197916	@USER_77a4822d yea ok..well answer that cheap as Sweden phone you came up on when I call.
		while((line = br.readLine()) != null){
			final String [] parts = line.split("\t");
			if(parts.length != 6)
				continue;
			
			

			IConceptVector cvBase = searcher.getConceptVector(parts[5]);		
			IConceptVector cv = searcher.getNormalVector(cvBase,10);
			
			if(cv == null){
				//System.exit(1);
				System.out.println(parts[0] + "\t" + 0 + "\t" + 0 + "\t" + 18000);
				continue;
			}
			
			IConceptIterator it = cv.orderedIterator();
			
			HashMap<Integer, Double> vals = new HashMap<Integer, Double>(10);
//			HashMap<Integer, String> titles = new HashMap<Integer, String>(10);
//			HashMap<Integer, Float> lat = new HashMap<Integer, Float>(10);
//			HashMap<Integer, Float> lon = new HashMap<Integer, Float>(10);
			
			String inPart = "(";
			
//			int count = 0;
					
			while(it.next() /* && count < 10 */){
				inPart += it.getId() + ",";
				vals.put(it.getId(),it.getValue());
//				count++;
			}
			
			inPart = inPart.substring(0,inPart.length()-1) + ")";
					
			ScoredCentroid centroid = new ScoredCentroid();
			ResultSet r = stmtQuery.executeQuery(strTitles + inPart);
			while(r.next()){
				int id = r.getInt(1);
//				titles.put(id, new String(r.getBytes(2),"UTF-8")); 			
//				lat.put(r.getInt(1), r.getFloat(3)); 
//				lon.put(r.getInt(1), r.getFloat(4)); 
				centroid.addScoredCoord(vals.get(id), r.getFloat(2), r.getFloat(3));
			}
			centroid.normalize();
			double km = GeoUtil.greatCircleDistanceInKm((float)centroid.getLatLon()[0], (float)centroid.getLatLon()[1], Float.parseFloat(parts[3]), Float.parseFloat(parts[4]));
			System.out.println(parts[0] + "\t" + (float)centroid.getLatLon()[0] + "\t" + (float)centroid.getLatLon()[1] + "\t" + (float)km);

		}
		br.close();
		
	}

}
