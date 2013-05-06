package edu.wiki.util;

public class ScoredCentroid {
	
	double sumScores = 0.0;
	double[] nVecCentroid = {0.0, 0.0, 0.0};
	
	public void addScoredCoord(double conceptScore, float latitude, float longitude) {
		double[] nVector = GeoUtil.latLonToNVec(latitude, longitude);
		this.add(GeoUtil.multiply(nVector, conceptScore));
		sumScores += conceptScore;		
	}
		  
	  void add(double[] v)
	  {
	    nVecCentroid[0] += v[0];
	    nVecCentroid[1] += v[1];
	    nVecCentroid[2] += v[2];
	  }
	  
	  public void normalize() {
		  if(sumScores != 0.0) {
		    nVecCentroid[0] /= sumScores;
		    nVecCentroid[1] /= sumScores;
		    nVecCentroid[2] /= sumScores;
		  }
	  }
	  
	  public void reset() {
			sumScores = 0.0;
			nVecCentroid[0] = 0.0;
			nVecCentroid[1] = 0.0;
			nVecCentroid[2] = 0.0;
	  }

	public double distance(ScoredCentroid centroid2) {
		return 1 - GeoUtil.greatCircleDistance(nVecCentroid, centroid2.getNVec()) / Math.PI;
	}

	public double[] getNVec() {
		return nVecCentroid;
	}

	public double[] getLatLon() {
		return GeoUtil.nVecToLatLon(nVecCentroid);
	}
}
