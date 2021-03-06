package edu.wiki.util;

public class GeoUtil {
	  /**
	   * Return the length of a vector.
	   * 
	   * @param v  Vector to compute length of [x,y,z].
	   * @return   Length of vector.
	   */
	  public static double length (double[] v)
	  {
	    return Math.sqrt (v[0]*v[0] + v[1]*v[1] + v[2]*v[2]);
	  }
	 
	  public static double[] normalize (double[] v)
	  {
	    double length = length(v);
	    if (length != 0.0) {
	    	return multiply(v, 1.0/length);
	    }
	    else {
	    	return v;
	    }
	  }
	  
	  public static double[] multiply (double[] v, double score)
	  {
		  double product[] = new double[3];
		  product[0] = v[0] * score; 
		  product[1] = v[1] * score; 
		  product[2] = v[2] * score; 		  
	    return product;
	  }

	  /**
	   * Compute the dot product (a scalar) between two vectors.
	   * 
	   * @param v0, v1  Vectors to compute dot product between [x,y,z].
	   * @return        Dot product of given vectors.
	   */
	  public static double dot (double[] v0, double[] v1)
	  {
	    return v0[0] * v1[0] + v0[1] * v1[1] + v0[2] * v1[2];
	  }
	  
	  /**
	   * Compute the cross product (a vector) of two vectors.
	   * 
	   * @param v0, v1        Vectors to compute cross product between [x,y,z].
	   * @param crossProduct  Cross product of specified vectors [x,y,z].
	   */
	  public static double[] cross (double[] v0, double[] v1)
	  {
	    double crossProduct[] = new double[3];
	    
	    crossProduct[0] = v0[1] * v1[2] - v0[2] * v1[1];
	    crossProduct[1] = v0[2] * v1[0] - v0[0] * v1[2];
	    crossProduct[2] = v0[0] * v1[1] - v0[1] * v1[0];

	    return crossProduct;
	  }
	  
	  public static double greatCircleDistance(double[] nVec1, double[] nVec2) {
		return Math.atan2(length(cross(nVec1, nVec2)), dot(nVec1, nVec2));
	}

	  public static double greatCircleDistance2(double[] nVec1, double[] nVec2) {
		  return Math.acos(dot(nVec1, nVec2));
	}
	  
	  public static double greatCircleDistanceInKm(float latitude1, float longitude1, float latitude2, float longitude2) {
		  double[] nVec1 = latLonToNVec(latitude1, longitude1);
		  double[] nVec2 = latLonToNVec(latitude2, longitude2);
		  return greatCircleDistance(nVec1, nVec2) * 6367;
	}


	public static double[] latLonToNVec(float latitude, float longitude) {

		// convert decimal degrees to radians 
		double radianLat = Math.toRadians(latitude);
		double radianLon = Math.toRadians(longitude);  
		double[] nVec = new double[3];
		nVec[0] =  Math.sin(radianLat);
		nVec[1] = Math.sin(radianLon) * Math.cos(radianLat);
		nVec[2] = -Math.cos(radianLon) * Math.cos(radianLat);
		return nVec;
	}
	  
	public static double[] nVecToLatLon(double[] nVec) {
		double latLon[] = new double[2];
		latLon[0] = Math.toDegrees(Math.atan2(nVec[0], Math.sqrt(nVec[1]*nVec[1] + nVec[2]* nVec[2])));
		latLon[1] = Math.toDegrees(Math.atan2(nVec[1], -nVec[2]));
	  return latLon;
	}

	public static double[]  nVecCentroid(double[][] nVecs) {
		double centroid[] = new double[3];
	  for(double[] v : nVecs) {
		  centroid[0] += v[0];
		  centroid[1] += v[1];
		  centroid[2] += v[2];
	  }
	  centroid[0] /= (double) nVecs.length;
	  centroid[1] /= (double) nVecs.length;
	  centroid[2] /= (double) nVecs.length;
	  return centroid;
	}
	
	// see http://msdn.microsoft.com/en-us/library/bb259689.aspx
    private final static double EarthRadius = 6378137;
    private final static double MinLatitude = -85.05112878;
    private final static double MaxLatitude = 85.05112878;
    private final static double MinLongitude = -180;
    private final static double MaxLongitude = 180;
    
    /// Clips a number to the specified minimum and maximum values.
    /// <param name="n">The number to clip.</param>
    /// <param name="minValue">Minimum allowable value.</param>
    /// <param name="maxValue">Maximum allowable value.</param>
    /// <returns>The clipped value.</returns>
    private static double Clip(double n, double minValue, double maxValue)
    {
        return Math.min(Math.max(n, minValue), maxValue);
    }
    

    /// Determines the map width and height (in pixels) at a specified level
    /// of detail.
    /// <param name="levelOfDetail">Level of detail, from 1 (lowest detail)
    /// to 23 (highest detail).</param>
    /// <returns>The map width and height in pixels.</returns>
    public static int MapSize(int levelOfDetail)
    {
        return 256 << levelOfDetail;
    }
    
	/// Converts a point from latitude/longitude WGS-84 coordinates (in degrees)
    /// into pixel XY coordinates at a specified level of detail.
    /// <param name="latitude">Latitude of the point, in degrees.</param>
    /// <param name="longitude">Longitude of the point, in degrees.</param>
    /// <param name="levelOfDetail">Level of detail, from 1 (lowest detail)
    /// to 23 (highest detail).</param>
    public static long LatLongToPixel(double latitude, double longitude, int levelOfDetail)
    {
        latitude = Clip(latitude, MinLatitude, MaxLatitude);
        longitude = Clip(longitude, MinLongitude, MaxLongitude);

        double x = (longitude + 180) / 360; 
        double sinLatitude = Math.sin(latitude * Math.PI / 180);
        double y = 0.5 - Math.log((1 + sinLatitude) / (1 - sinLatitude)) / (4 * Math.PI);

        int mapSize = MapSize(levelOfDetail);
        int pixelX, pixelY;
        pixelX = (int) Clip(x * mapSize + 0.5, 0, mapSize - 1);
        pixelY = (int) Clip(y * mapSize + 0.5, 0, mapSize - 1);
        return pixelX + (mapSize * pixelY);
    }
    public static int[] LatLongToXY(double latitude, double longitude, int levelOfDetail)
    {
        latitude = Clip(latitude, MinLatitude, MaxLatitude);
        longitude = Clip(longitude, MinLongitude, MaxLongitude);

        double x = (longitude + 180) / 360; 
        double sinLatitude = Math.sin(latitude * Math.PI / 180);
        double y = 0.5 - Math.log((1 + sinLatitude) / (1 - sinLatitude)) / (4 * Math.PI);

        int mapSize = MapSize(levelOfDetail);
        int pixelX, pixelY;
        pixelX = (int) Clip(x * mapSize + 0.5, 0, mapSize - 1);
        pixelY = (int) Clip(y * mapSize + 0.5, 0, mapSize - 1);
        return new int [] {pixelX, pixelY};
    }
}
