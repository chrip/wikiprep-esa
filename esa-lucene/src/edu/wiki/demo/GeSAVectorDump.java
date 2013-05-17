package edu.wiki.demo;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;

import edu.wiki.api.concept.IConceptIterator;
import edu.wiki.api.concept.IConceptVector;
import edu.wiki.search.ESASearcher;
import edu.wiki.util.ScoredCentroid;

public class GeSAVectorDump {
	
	static Connection connection;
	static Statement stmtQuery;
	static Statement stmtQuery2;
	
	static String strTitles = "SELECT a.id,title,lat,lon FROM article a, geoarticle g WHERE a.id=g.id AND a.id IN ";
	
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
		stmtQuery2 = connection.createStatement();
		stmtQuery.setFetchSize(100);
		stmtQuery2.setFetchSize(100);
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
		
		ResultSet rTerms = stmtQuery2.executeQuery("select * from terms");
		
		while(rTerms.next()){
			String term = rTerms.getString(1);
			IConceptVector cvBase = searcher.getConceptVector(term);		
			//IConceptVector cv = searcher.getNormalVector(cvBase,10);
			
			if(cvBase == null){
				continue;
			}
			
			IConceptIterator it = cvBase.iterator();
			
			HashMap<Integer, Double> vals = new HashMap<Integer, Double>(10);
			String inPart = "(";
			
			int count = 0;
					
			while(it.next() /* && count < 10 */){
				inPart += it.getId() + ",";
				vals.put(it.getId(),it.getValue());
				count++;
			}
			
			inPart = inPart.substring(0,inPart.length()-1) + ")";
					
			ResultSet r = stmtQuery.executeQuery(strTitles + inPart);
			while(r.next()){
				System.out.println(term + "\t" + r.getInt(1) + "\t" + vals.get(r.getInt(1)) + "\t" + new String(r.getBytes(2),"UTF-8") + "\t" + r.getFloat(3) + "\t" + r.getFloat(4));
			}
			r.close();
		}
		rTerms.close();
	}

}
