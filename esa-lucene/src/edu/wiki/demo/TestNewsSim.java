package edu.wiki.demo;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.util.Arrays;

import edu.wiki.modify.IndexModifier;
import edu.wiki.search.ESASearcher;

public class TestNewsSim {

	/**
	 * @param args
	 * @throws IOException 
	 * @throws SQLException 
	 * @throws ClassNotFoundException 
	 */
	public static void main(String[] args) throws ClassNotFoundException, SQLException, IOException {
		ESASearcher searcher = new ESASearcher();
		String line;
		double val;
		
		// read Lee's newspaper articles
		InputStream is = IndexModifier.class.getResourceAsStream("/config/lee.cor");
		BufferedReader br = new BufferedReader(new InputStreamReader(is));
		String[] leesCorpus = new String[50];
		int i = 0;
		while((line = br.readLine()) != null){
			leesCorpus[i] = line;
			//System.out.println(leesCorpus[i]);
			i++;
		}
		br.close();
		double[][] result = new double[50][50];
		for(i=0; i < 50; i++) {
			for(int j = 0; j< 50; j++) {
				if(i > j) {
					result[i][j] = 0.0;
				}
				else if (i == j) {
					result[i][j] = 1.0;
				}
				else {
					val = searcher.getRelatedness(leesCorpus[i], leesCorpus[j]);
					if (val == -1) {
						val = 0;
					}
					result[i][j] = val;
				}
				System.out.print(result[i][j] + "\t");
			}
			System.out.print("\n");
		}		
	}

}
