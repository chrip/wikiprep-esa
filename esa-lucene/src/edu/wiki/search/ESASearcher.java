package edu.wiki.search;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.TermAttribute;

import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

import edu.wiki.api.concept.IConceptIterator;
import edu.wiki.api.concept.IConceptVector;
import edu.wiki.api.concept.scorer.CosineScorer;
import edu.wiki.api.concept.scorer.GeoMedianScorer;
import edu.wiki.concept.ConceptVectorSimilarity;
import edu.wiki.concept.TroveConceptVector;
import edu.wiki.index.WikipediaAnalyzer;
import edu.wiki.util.HeapSort;
import gnu.trove.TIntFloatHashMap;
import gnu.trove.TIntIntHashMap;

/**
 * Performs search on the index located in database.
 * 
 * @author Cagatay Calli <ccalli@gmail.com>
 */
public class ESASearcher {
	Connection connection;
	
	PreparedStatement pstmtQuery;
	PreparedStatement pstmtIdfQuery;
	PreparedStatement pstmtLinks;
	Statement stmtInlink;
	
	WikipediaAnalyzer analyzer;
	
	String strTermQuery = "SELECT t.vector FROM idx t WHERE t.term = ?";
	String strIdfQuery = "SELECT t.idf FROM terms t WHERE t.term = ?";
	
	String strMaxConcept = "SELECT MAX(id) FROM article";
	
	String strInlinks = "SELECT i.target_id, i.inlink FROM inlinks i WHERE i.target_id IN ";
	
	String strLinks = "SELECT target_id FROM pagelinks WHERE source_id = ?";

	int maxConceptId;
	
	int[] ids;
	double[] values;
	
	HashMap<String, Integer> freqMap = new HashMap<String, Integer>(30);
	HashMap<String, Double> tfidfMap = new HashMap<String, Double>(30);
	HashMap<String, Float> idfMap = new HashMap<String, Float>(30);
	
	ArrayList<String> termList = new ArrayList<String>(30);
	
	TIntIntHashMap inlinkMap;
	
	static float LINK_ALPHA = 0.5f;
	
	//ConceptVectorSimilarity sim = new ConceptVectorSimilarity(new CosineScorer());
	ConceptVectorSimilarity sim = new ConceptVectorSimilarity(new CosineScorer(), new GeoMedianScorer());
	
	class ScoredCoordinate {
		public int conceptId;
		public double score;
		public double lat;
		public double lon;
	}
	HashMap<String, ArrayList<ScoredCoordinate>> termGeoScoresMap;
		
	public void initDB() throws ClassNotFoundException, SQLException, IOException {
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
		
		pstmtQuery = connection.prepareStatement(strTermQuery);
		pstmtQuery.setFetchSize(1);
		
		pstmtIdfQuery = connection.prepareStatement(strIdfQuery);
		pstmtIdfQuery.setFetchSize(1);
		
		pstmtLinks = connection.prepareStatement(strLinks);
		pstmtLinks.setFetchSize(500);
		
		stmtInlink = connection.createStatement();
		stmtInlink.setFetchSize(50);
		
		ResultSet res = connection.createStatement().executeQuery(strMaxConcept);
		res.next();
		maxConceptId = res.getInt(1) + 1;

  }
	
	public void clean(){
		freqMap.clear();
		tfidfMap.clear();
		idfMap.clear();
		termList.clear();
		inlinkMap.clear();
		
		Arrays.fill(ids, 0);
		Arrays.fill(values, 0);
	}
	
	public ESASearcher() throws ClassNotFoundException, SQLException, IOException{
		initDB();
		analyzer = new WikipediaAnalyzer();
		
		ids = new int[maxConceptId];
		values = new double[maxConceptId];
		
		inlinkMap = new TIntIntHashMap(300);
		
		
		/// begin GeSA stuff
		termGeoScoresMap = new HashMap<String, ArrayList<ScoredCoordinate>>();
		FileReader is = new FileReader("/data/chrisschaefer/gesa/GeSA_dump_pruned3.txt");
		BufferedReader br = new BufferedReader(is);
		String line;		
		while((line = br.readLine()) != null){
			final String [] parts = line.split("\t");
			if(parts.length != 6) {				
				break;
			}
			ScoredCoordinate sc = new ScoredCoordinate();
			sc.conceptId = Integer.valueOf(parts[1]);
			sc.lat = Double.valueOf(parts[2]);
			sc.lon = Double.valueOf(parts[3]);
			// part[4] contains the ESA tfidf score
			// part[5] contains the GeSA term frequency inverse raster frequency
			sc.score = Double.valueOf("1.0");
			
			if(termGeoScoresMap.containsKey(parts[0])){
				termGeoScoresMap.get(parts[0]).add(sc);
            }
            else {
            	 ArrayList<ScoredCoordinate> a = new  ArrayList<ScoredCoordinate>();
            	 a.add(sc);
            	 termGeoScoresMap.put(parts[0], a);
            }  				
		}
		br.close();	
		is.close();
		// end GeSA stuff
	}
	
	@Override
	protected void finalize() throws Throwable {
        connection.close();
		super.finalize();
	}
	
	/**
	 * Retrieves full vector for regular features
	 * @param query
	 * @return Returns concept vector results exist, otherwise null 
	 * @throws IOException
	 * @throws SQLException
	 */
	public IConceptVector getConceptVector(String query) throws IOException, SQLException{
		String strTerm;
		int numTerms = 0;
		ResultSet rs;
		int doc;
		double score;
		int vint;
		double vdouble;
		double tf;
		double vsum;
		int plen;
        TokenStream ts = analyzer.tokenStream("contents",new StringReader(query));
        ByteArrayInputStream bais;
        DataInputStream dis;

        this.clean();

		for( int i=0; i<ids.length; i++ ) {
			ids[i] = i;
		}
        
        ts.reset();
        
        while (ts.incrementToken()) { 
        	
            TermAttribute t = ts.getAttribute(TermAttribute.class);
            strTerm = t.term();
            
            // record term IDF
            if(!idfMap.containsKey(strTerm)){
	            pstmtIdfQuery.setBytes(1, strTerm.getBytes("UTF-8"));
	            pstmtIdfQuery.execute();
	            
	            rs = pstmtIdfQuery.getResultSet();
	            if(rs.next()){
	            	idfMap.put(strTerm, rs.getFloat(1));	          	  
	            }
            }
            
            // records term counts for TF
            if(freqMap.containsKey(strTerm)){
            	vint = freqMap.get(strTerm);
            	freqMap.put(strTerm, vint+1);
            }
            else {
            	freqMap.put(strTerm, 1);
            }
            
            termList.add(strTerm);
	            
            numTerms++;	

        }
                
        ts.end();
        ts.close();
                
        if(numTerms == 0){
        	return null;
        }
        
        // calculate TF-IDF vector (normalized)
        vsum = 0;
        for(String tk : idfMap.keySet()){
        	tf = 1.0 + Math.log(freqMap.get(tk));
        	vdouble = (idfMap.get(tk) * tf);
        	tfidfMap.put(tk, vdouble);
        	vsum += vdouble * vdouble;
        }
        vsum = Math.sqrt(vsum);
        
        
        // comment this out for canceling query normalization
        for(String tk : idfMap.keySet()){
        	vdouble = tfidfMap.get(tk);
        	tfidfMap.put(tk, vdouble / vsum);
        }
        
        score = 0;
        for (String tk : termList) { 
        	            
            pstmtQuery.setBytes(1, tk.getBytes("UTF-8"));
            pstmtQuery.execute();
            
            rs = pstmtQuery.getResultSet();
            
            if(rs.next()){
          	  bais = new ByteArrayInputStream(rs.getBytes(1));
          	  dis = new DataInputStream(bais);
          	  
          	  /**
          	   * 4 bytes: int - length of array
          	   * 4 byte (doc) - 8 byte (tfidf) pairs
          	   */
          	  
          	  plen = dis.readInt();
          	  // System.out.println("vector len: " + plen);
          	  for(int k = 0;k<plen;k++){
          		  doc = dis.readInt();
          		  score = dis.readFloat();
          		  values[doc] += score * tfidfMap.get(tk);
          	  }
          	  
          	  bais.close();
          	  dis.close();
            }

        }
        
        // no result
        if(score == 0){
        	return null;
        }
        
        HeapSort.heapSort( values, ids );
        
        IConceptVector newCv = new TroveConceptVector(ids.length);
		for( int i=ids.length-1; i>=0 && values[i] > 0; i-- ) {
			newCv.set( ids[i], values[i] / numTerms );
		}
		
		return newCv;
	}
	
	/**
	 * Retrieves full vector for regular features
	 * @param query
	 * @return Returns concept vector results exist, otherwise null 
	 * @throws IOException
	 * @throws SQLException
	 */
	public IConceptVector getGeoConceptVector(String query) throws IOException, SQLException{
		String strTerm;
		int numTerms = 0;
		ResultSet rs;
		int doc;
		double score;
		int vint;
		double vdouble;
		double tf;
		double vsum;
		int plen;
        TokenStream ts = analyzer.tokenStream("contents",new StringReader(query));
        ByteArrayInputStream bais;
        DataInputStream dis;

        this.clean();

		for( int i=0; i<ids.length; i++ ) {
			ids[i] = i;
		}
        
        ts.reset();
        
        while (ts.incrementToken()) { 
        	
            TermAttribute t = ts.getAttribute(TermAttribute.class);
            strTerm = t.term();
            
            // record term IDF
            if(!idfMap.containsKey(strTerm)){
	            pstmtIdfQuery.setBytes(1, strTerm.getBytes("UTF-8"));
	            pstmtIdfQuery.execute();
	            
	            rs = pstmtIdfQuery.getResultSet();
	            if(rs.next()){
	            	idfMap.put(strTerm, rs.getFloat(1));	          	  
	            }
            }
            
            // records term counts for TF
            if(freqMap.containsKey(strTerm)){
            	vint = freqMap.get(strTerm);
            	freqMap.put(strTerm, vint+1);
            }
            else {
            	freqMap.put(strTerm, 1);
            }
            
            termList.add(strTerm);
	            
            numTerms++;	

        }
                
        ts.end();
        ts.close();
                
        if(numTerms == 0){
        	return null;
        }
        
        // calculate TF-IDF vector (normalized)
        vsum = 0;
        for(String tk : idfMap.keySet()){
        	tf = 1.0 + Math.log(freqMap.get(tk));
        	vdouble = (idfMap.get(tk) * tf);
        	tfidfMap.put(tk, vdouble);
        	vsum += vdouble * vdouble;
        }
        vsum = Math.sqrt(vsum);
        
        
        // comment this out for canceling query normalization
        for(String tk : idfMap.keySet()){
        	vdouble = tfidfMap.get(tk);
        	tfidfMap.put(tk, vdouble / vsum);
        }
        
        score = 0;
        for (String tk : termList) {   
		/// begin GeSA stuff
		    if(termGeoScoresMap.containsKey(tk)){          	 
		  	  for(ScoredCoordinate sc : termGeoScoresMap.get(tk)){
		  		  doc = sc.conceptId;
		  		  score = sc.score;
		  		  values[doc] += score * tfidfMap.get(tk);
		  	  }          	  
		    }
		  /// end GeSA stuff
        }
        
        // no result
        if(score == 0){
        	return null;
        }
        
        HeapSort.heapSort( values, ids );
        
        IConceptVector newCv = new TroveConceptVector(ids.length);
		for( int i=ids.length-1; i>=0 && values[i] > 0; i-- ) {
			newCv.set( ids[i], values[i] / numTerms );
		}
		
		return newCv;
	}
	
	/**
	 * Returns trimmed form of concept vector
	 * @param cv
	 * @return
	 */
	public IConceptVector getNormalVector(IConceptVector cv, int LIMIT){
		IConceptVector cv_normal = new TroveConceptVector( LIMIT);
		IConceptIterator it;
		
		if(cv == null)
			return null;
		
		it = cv.orderedIterator();
		
		int count = 0;
		while(it.next()){
			if(count >= LIMIT) break;
			cv_normal.set(it.getId(), it.getValue());
			count++;
		}
		
		return cv_normal;
	}
	
	private TIntIntHashMap setInlinkCounts(Collection<Integer> ids) throws SQLException{
		inlinkMap.clear();
		
		String inPart = "(";
		
		for(int id: ids){
			inPart += id + ",";
		}
		
		inPart = inPart.substring(0,inPart.length()-1) + ")";

		// collect inlink counts
		ResultSet r = stmtInlink.executeQuery(strInlinks + inPart);
		while(r.next()){
			inlinkMap.put(r.getInt(1), r.getInt(2)); 
		}
		
		return inlinkMap;
	}
	
	private Collection<Integer> getLinks(int id) throws SQLException{
		ArrayList<Integer> links = new ArrayList<Integer>(100); 
		
		pstmtLinks.setInt(1, id);
		
		ResultSet r = pstmtLinks.executeQuery();
		while(r.next()){
			links.add(r.getInt(1)); 
		}
		
		return links;
	}
	
	
	public IConceptVector getLinkVector(IConceptVector cv, int limit) throws SQLException {
		if(cv == null)
			return null;
		return getLinkVector(cv, true, LINK_ALPHA, limit);
	}
	
	/**
	 * Computes secondary interpretation vector of regular features
	 * @param cv
	 * @param moreGeneral
	 * @param ALPHA
	 * @param LIMIT
	 * @return
	 * @throws SQLException
	 */
	public IConceptVector getLinkVector(IConceptVector cv, boolean moreGeneral, double ALPHA, int LIMIT) throws SQLException {
		IConceptIterator it;
		
		if(cv == null)
			return null;
		
		it = cv.orderedIterator();
		
		int count = 0;
		ArrayList<Integer> pages = new ArrayList<Integer>();
						
		TIntFloatHashMap valueMap2 = new TIntFloatHashMap(1000);
		TIntFloatHashMap valueMap3 = new TIntFloatHashMap();
		
		ArrayList<Integer> npages = new ArrayList<Integer>();
		
		HashMap<Integer, Float> secondMap = new HashMap<Integer, Float>(1000);
		
		
		this.clean();
				
		// collect article objects
		while(it.next()){
			pages.add(it.getId());
			valueMap2.put(it.getId(),(float) it.getValue());
			count++;
		}
		
		// prepare inlink counts
		setInlinkCounts(pages);
				
		for(int pid : pages){			
			Collection<Integer> raw_links = getLinks(pid);
			if(raw_links.isEmpty()){
				continue;
			}
			ArrayList<Integer> links = new ArrayList<Integer>(raw_links.size());
			
			final double inlink_factor_p = Math.log(inlinkMap.get(pid));
										
			float origValue = valueMap2.get(pid);
			
			setInlinkCounts(raw_links);
						
			for(int lid : raw_links){
				final double inlink_factor_link = Math.log(inlinkMap.get(lid));
				
				// check concept generality..
				if(inlink_factor_link - inlink_factor_p > 1){
					links.add(lid);
				}
			}
						
			for(int lid : links){				
				if(!valueMap2.containsKey(lid)){
					valueMap2.put(lid, 0.0f);
					npages.add(lid);
				}
			}
						
			
			
			float linkedValue = 0.0f;
									
			for(int lid : links){
				if(valueMap3.containsKey(lid)){
					linkedValue = valueMap3.get(lid); 
					linkedValue += origValue;
					valueMap3.put(lid, linkedValue);
				}
				else {
					valueMap3.put(lid, origValue);
				}
			}
			
		}
		
		
//		for(int pid : pages){			
//			if(valueMap3.containsKey(pid)){
//				secondMap.put(pid, (float) (valueMap2.get(pid) + ALPHA * valueMap3.get(pid)));
//			}
//			else {
//				secondMap.put(pid, (float) (valueMap2.get(pid) ));
//			}
//		}
		
		for(int pid : npages){			
			secondMap.put(pid, (float) (ALPHA * valueMap3.get(pid)));

		}
		
		
		//System.out.println("read links..");
		
		
		ArrayList<Integer> keys = new ArrayList(secondMap.keySet());
		
		//Sort keys by values.
		final Map langForComp = secondMap;
		Collections.sort(keys, 
			new Comparator(){
				public int compare(Object left, Object right){
					Integer leftKey = (Integer)left;
					Integer rightKey = (Integer)right;
					
					Float leftValue = (Float)langForComp.get(leftKey);
					Float rightValue = (Float)langForComp.get(rightKey);
					return leftValue.compareTo(rightValue);
				}
			});
		Collections.reverse(keys);
		
		

		IConceptVector cv_link = new TroveConceptVector(maxConceptId);
		
		int c = 0;
		for(int p : keys){
			cv_link.set(p, secondMap.get(p));
			c++;
			if(c >= LIMIT){
				break;
			}
		}
		
		
		return cv_link;
	}
	
	public IConceptVector getCombinedVector(String query) throws IOException, SQLException{
		IConceptVector cvBase = getConceptVector(query);
		IConceptVector cvNormal, cvLink;
		
		if(cvBase == null){
			return null;
		}
		
		cvNormal = getNormalVector(cvBase,10);
		cvLink = getLinkVector(cvNormal,5);
		
		cvNormal.add(cvLink);
		
		return cvNormal;
	}
	
	/**
	 * Calculate semantic relatedness between documents
	 * @param doc1
	 * @param doc2
	 * @return returns relatedness if successful, -1 otherwise
	 */
	public double getRelatedness(String doc1, String doc2){
		try {

			
//			IConceptVector c1 = getConceptVector(doc1);
//			IConceptVector c2 = getConceptVector(doc2);
			IConceptVector c1 = getGeoConceptVector(doc1);
			IConceptVector c2 = getGeoConceptVector(doc2);			
			
			if(c1 == null || c2 == null){
				// return 0;
				return -1;	// undefined
			}
			
			final double rel = sim.calcSimilarity(c1, c2);
			
			// mark for dealloc
			c1 = null;
			c2 = null;
			
			return rel;

		}
		catch(Exception e){
			e.printStackTrace();
			return 0;
		}

	}

}
