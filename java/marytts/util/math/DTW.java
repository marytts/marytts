package marytts.util.math;

import java.io.File;
import java.io.BufferedReader;
import java.io.FileReader;

/**
 * Dynamic programming to compute similarity measure
 * @author sathish
 *
 */

public class DTW {

    double[][] signal;
    double[][] reference;
    // the slope constraint value
    double slope = 0.0;
    String distanceFunction;
    
    double costValue;

    /**
     * Dynamic time warping (DTW) cost signal and reference  
     * Default 'Euclidean' distance function
     * @param signal
     * @param reference
     */
    public DTW(double[][] signal,double[][] reference){
        this.signal = signal;
        this.reference = reference;
        this.distanceFunction = "Euclidean";
        setCost(dpDistance());
    }
    
    /**
     * Dynamic time warping (DTW) cost signal and reference
     * distanceFunction = {"Euclidean" or "Absolute"} 
     * @param signal
     * @param reference
     */
    public DTW(double[][] signal,double[][] reference, String distanceFunction){
        this.signal = signal;
        this.reference = reference;
        this.distanceFunction = distanceFunction;
        setCost(dpDistance());
    }
    
    /**
     * Set cost of best path
     * @param cost
     */
    public void setCost(double cost){
        this.costValue = cost;
    }
    
    
    /**
     * Get cost of best path 
     * @return cost
     */
    public double getCost(){
        return this.costValue;
    }
    
    /**
     * the major method to compute the matching score between selected test 
     * signal and reference. 
     *
     * @return DP cost  
     */
  public double dpDistance() {
    if ((signal == null ) || (reference == null)) {
        return -1.0;
    }

    double cost = 0;
    int s;
    int t;
    int p;
    int ns;
    int nt;
    double maxV = 1.0e+32;
    int idx;
    int dc = 13; 
    ns = reference.length; // ns = rr;
    nt = signal.length; // nt = dr;
    
    double[][] trellis = (double[][]) new double[ns][nt];
    //Initializing first column.......
    t = 0;
    for (s = 0; s < ns; s++) {
       if (s == 0) {
           trellis[s][t] = frameDistance(reference[s], signal[t], distanceFunction);
           //pbuf[s][t] = 0;
       } else {
           trellis[s][t] = maxV;
       }
    }

    for (t = 1; t < nt; t++) {
        for (s = 0; s < ns; s++) {
       //connections s t-1, s-1, t-1, s-2, t-1           
       trellis[s][t] = minConnect(trellis, s, t) + frameDistance(reference[s], signal[t], distanceFunction);
       // pbuf[s][t] = idx;
        }
     }
     cost = trellis[ns-1][nt-1];
     return cost;    
  }
    
  /**
   * Euclidean distance 
   */  
  public double EuclideanDistance(double[] x, double[] y) {

      double sum = 0;
      if(x.length != y.length){
          throw new RuntimeException("Given array lengths were not equal."); 
      }
      int d = x.length;
      for (int i = 0; i < d; i++) {
       sum = sum + (x[i] - y[i]) * (x[i] - y[i]);
      }
      sum = Math.sqrt(sum);
      return sum;
     }
       
  /**
   * Absolute distance
   * @param x
   * @param y
   * @return
   */
  public double AbsDistance(double[] x, double[] y) {

      double sum = 0;
      if(x.length != y.length){
          throw new RuntimeException("Given array lengths were not equal."); 
      }
      int d = x.length;
      for (int i = 0; i < d; i++) {
       sum = sum + Math.abs(x[i] - y[i]);
      }
      return sum;
     }
  
  /**
   * Index of minimum connection  
   * @param trel
   * @param s
   * @param t
   * @return
   */
  private double minConnect(double[][] trel, int s, int t) {

      double minV;
      int t1 = t - 1;

      if (s - 2 >= 0) {
        minV = trel[s-2][t1];           
        //idx = s-2;
        if (minV > trel[s-1][t1]) {
           minV = trel[s-1][t1]; 
           //idx = s-1;
        }
        if (minV > trel[s][t1]) {
           minV = trel[s][t1];
           //idx = s;
        }
      } else if (s - 1 >= 0) {
        minV = trel[s-1][t1];           
        //idx = s-1;
        if (minV > trel[s][t1]) {
          minV = trel[s][t1];
          //idx = s;
        }
      } else {
        minV = trel[s][t1];
        //idx = s;
    }
      return(minV);
     }
  
  // methods to compute distance between two frames    
  private double frameDistance(double f1[], double f2[], String distanceType) {
  double dis = 0.0;
  if (distanceType == "Euclidean") 
      dis = EuclideanDistance(f1, f2);
  else dis = AbsDistance(f1, f2);
  return dis;
  }
    
  
}
