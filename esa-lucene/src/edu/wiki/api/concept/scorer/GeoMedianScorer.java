package edu.wiki.api.concept.scorer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import edu.wiki.api.concept.IConceptVectorData;
import edu.wiki.api.concept.search.IScorer;
import edu.wiki.modify.IndexModifier;
import edu.wiki.util.ScoredMedian;


public class GeoMedianScorer implements IScorer {	
	
	static Connection connection = null;
	static PreparedStatement pstmtLatLon;	
	static String strLatLonQuery = "SELECT lat,lon FROM geoarticle WHERE id = ?";		
		
	public static void initDB() throws ClassNotFoundException, SQLException, IOException {
		// Load the JDBC driver 
		String driverName = "com.mysql.jdbc.Driver"; // MySQL Connector 
		Class.forName(driverName); 
		
		// read DB config
		InputStream is = IndexModifier.class.getResourceAsStream("/config/db.conf");
		BufferedReader br = new BufferedReader(new InputStreamReader(is));
		String serverName = br.readLine();
		String mydatabase = br.readLine();
		String username = br.readLine(); 
		String password = br.readLine();
		br.close();

		// Create a connection to the database 
		String url = "jdbc:mysql://" + serverName + "/" + mydatabase + "?useUnicode=yes&characterEncoding=UTF-8"; // a JDBC url 
		connection = DriverManager.getConnection(url, username, password);
		
		pstmtLatLon = connection.prepareStatement(strLatLonQuery);		
	}
	// Constructor
	public GeoMedianScorer() {
		try {
			initDB();
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	//	Destructor
	protected void finalize () {
		try {
			connection.close();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }
    int median1Count = 0;
    int median2Count = 0;
    int medianLimit = 100;
	ScoredMedian median1 = new ScoredMedian();
	ScoredMedian median2 = new ScoredMedian();
	
	public double getScore() {
		return median1.distance(median2);
	}

	public void reset( IConceptVectorData queryData, IConceptVectorData docData, int numberOfDocuments ) {
	    median1Count = 0;
	    median2Count = 0;
		median1.reset();
		median2.reset();
	}

	public void addConcept(
			int queryConceptId, double queryConceptScore,
			int docConceptId, double docConceptScore,
			int conceptFrequency ) {
		if(docConceptId !=0 && median1Count < medianLimit) {
			median1Count++;
			try {
				pstmtLatLon.setInt(1, docConceptId);
				ResultSet r = pstmtLatLon.executeQuery();    	
				while(r.next()){
					float latitude = r.getFloat(1);
					float longitude = r.getFloat(2);
					median1.addScoredCoord(docConceptScore, latitude, longitude);
				}
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} 
		}		
		if(queryConceptId !=0 && median2Count < medianLimit) {
			median2Count++;
			try {
				pstmtLatLon.setInt(1, queryConceptId);
				ResultSet r = pstmtLatLon.executeQuery();    	
				while(r.next()){
					float latitude = r.getFloat(1);
					float longitude = r.getFloat(2);
					median2.addScoredCoord(queryConceptScore, latitude, longitude);
				}
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} 
		}		
	}

	public void finalizeScore( IConceptVectorData queryData, IConceptVectorData docData ) {
		median1.normalize();
		median2.normalize();
	}

	public boolean hasScore() {
		return true;
	}

}
