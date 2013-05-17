package edu.wiki.util;

import java.util.ArrayList;




public class ScoredMedian {
	
	class ScoredCoord {
		double[] nVec = {0.0,0.0,0.0};
		double score = 0;		
	}

	ArrayList<ScoredCoord> memberPoints = new ArrayList<ScoredCoord>();
	ScoredCoord median = new ScoredCoord();

	public void addScoredCoord(double conceptScore, float latitude, float longitude) {
		double[] nVector = GeoUtil.latLonToNVec(latitude, longitude);
		this.add(nVector, conceptScore);	
	}

	void add(double[] v, double score) {
		ScoredCoord c = new ScoredCoord();
		c.nVec = v;
		c.score =score;
		memberPoints.add(c);
	}

	public void normalize() {
		double currentMinDistance = Double.MAX_VALUE;
		if(memberPoints.size() != 0) {
			for(ScoredCoord c1 : memberPoints) {
				double temp = 0;
				for(ScoredCoord c2 : memberPoints) {
					temp += (GeoUtil.greatCircleDistance(c1.nVec, c2.nVec) / (c1.score * c2.score));
				}
				if(temp < currentMinDistance) {
					median = c1;
					currentMinDistance = temp;
				}
			}
		}
	}

	public void reset() {
		memberPoints.clear();
		median.nVec[0] = 0.0;
		median.nVec[1] = 0.0;
		median.nVec[2] = 0.0;
		median.score = 0.0;
	}

	public double distance(ScoredMedian median2) {
		return 1 - GeoUtil.greatCircleDistance(median.nVec, median2.getNVec()) / Math.PI;
	}

	public double[] getNVec() {
		return median.nVec;
	}

	public double[] getLatLon() {
		return GeoUtil.nVecToLatLon(median.nVec);
	}
}
