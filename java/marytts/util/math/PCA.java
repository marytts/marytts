package marytts.util.math;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.Vector;

import Jama.EigenvalueDecomposition;
import Jama.SingularValueDecomposition;
import Jama.Matrix;

/***
 * Principal component analysis
 * solve PCA using eigenvectors decomposion and singular value decomposition (SVD).
 * 
 * Ref: Jonathon Shlens, "A tutorial on principal component analysis", (Dated: April 22, 2009; Version 3.01) 
 *      http://www.snl.salk.edu/~shlens/pca.pdf
 *      
 * @author marcela
 */
public class PCA {
  
   private Matrix covariance;
   private double[] V;  // eigenValues or diagonal of svd;
   private Matrix PC;
   private Matrix dataProjected;
   
   public double[][] getCovariance(){
     return covariance.getArray();
   }
  
   public double[] getV(){
     return V;
   }

   public double[][] getPC() {
     return PC.getArray();
   }

   public double[][] getDataProjected() {
     return dataProjected.getArray();
   }
   
   
   public void eigenPCA(Vector<Double> data, int rows, int cols){
     if (data == null) throw new NullPointerException("Null data");
     if (rows < 0 || cols < 0) throw new IllegalArgumentException("Number of rows and cols must be greater than 0");

     Matrix dataX = new Matrix(rows,cols);
     int numIndVar = cols;

     // Fill the data in the matrix X (independent variables) and vector y (dependet variable)
     int n = 0;  // number of data points
     for (int i=0; i<rows; i++) {
       for (int j=0; j< cols; j++) 
         //dataX[i][j] = data.elementAt(n++);
         dataX.set(i, j, data.elementAt(n++));
     }
   
     dataX.print(dataX.getRowDimension(), 3);
     
   }
   
  /***
   * Solving PCA using eigenvector decomposition
   * @param data Matrix with M rows corresponding to dimensions or factors 
   *             and N columns corresponding to trials or examples
   * @param scale if true : applying zscore normalisation
   *              if false: just removing the mean
   */
  public void eigenPCA(Matrix data, boolean scale, boolean debug){
    // M dimensions
    // N trials
    int M = data.getRowDimension();
    int N = data.getColumnDimension();
    
    //  substract the mean for each dimension
    // if applying zscore scaling then divide by the standard deviation
    double element[][] = data.getArrayCopy();
    double mn;
    double sd;
    for(int i=0; i<M; i++){
      mn = MathUtils.mean(element[i]);
      if(scale){
        sd = MathUtils.standardDeviation(element[i]);
        // divide by the standard deviation
        for(int j=0; j<N; j++)
          data.set(i, j, ( (data.get(i, j)-mn)/sd ));
      } else {
        // remove the mean
        for(int j=0; j<N; j++)
          data.set(i, j, (data.get(i, j)-mn));
      }
          
    }
    if(debug){
      System.out.println("Data:");
      data.print(data.getRowDimension(), 3);
    }
     
    // calculate the covariance matrix
    // covariance = 1/(N-1) * data * data'
    covariance = data.times(data.transpose());
    covariance = covariance.times(1.0/(N-1));
    System.out.println("Covariance");
    covariance.print(covariance.getRowDimension(), 3);
    
    // find the eigenvectors and eigenvalues
    // eig() returns the values not ordered
    EigenvalueDecomposition pc = covariance.eig();
    System.out.println("EigenValues (on the diagonal)");
    pc.getD().print(pc.getD().getRowDimension(), 3);
    System.out.println("EigenVectors");
    pc.getV().print(pc.getV().getRowDimension(), 3);
    
    // get the diagonal values and sort them
    double values[] = new double[pc.getD().getRowDimension()];
    for(int i=0; i<pc.getD().getRowDimension(); i++)
       values[i] = pc.getD().get(i, i);   
    // sort is from lowest to highest
    int indices[] = MathUtils.quickSort(values);
    V = new double[values.length];
    
    // sort the variances in decreasing order
    double d[][] = new double[pc.getV().getRowDimension()][pc.getV().getColumnDimension()];
    for(int j=0; j<values.length; j++){  
      int k = indices[values.length-1-j];
      V[j] = values[k];
      for(int i=0; i<pc.getV().getRowDimension(); i++)
        d[i][j] = pc.getV().get(i, k);
    }        
    PC = new Matrix(d);
    if(debug) {
      System.out.println("PC:");
      PC.print(PC.getRowDimension(), 3);
    }
    
    // Project the original data set
    dataProjected = PC.transpose().times(data);
    if(debug) {
      System.out.println("Data projected:");
      dataProjected.print(dataProjected.getRowDimension(), 3);
    }
    
  }
  
  /***
   * Solving PCA using singular value decomposition (SVD) (more general solution)
   * @param data Matrix with M rows corresponding to dimensions or factors 
   *             and N columns corresponding to trials or examples
   * * @param scale if true : applying zscore normalisation
   *                if false: just removing the mean
   */
  public void svdPCA(Matrix data, boolean scale, boolean debug){
    // M dimensions
    // N trials
    int M = data.getRowDimension();
    int N = data.getColumnDimension();
    
    // substract the mean for each dimension
    // if applying zscore scaling then divide by the standard deviation
    double element[][] = data.getArrayCopy();
    double mn;
    double sd;
    for(int i=0; i<M; i++){
      mn = MathUtils.mean(element[i]);
      if(scale){
        sd = MathUtils.standardDeviation(element[i]);
        // divide by the standard deviation
        for(int j=0; j<N; j++)
          data.set(i, j, ( (data.get(i, j)-mn)/sd ));
      } else {
        // remove the mean
        for(int j=0; j<N; j++)
          data.set(i, j, (data.get(i, j)-mn));
      }
          
    }
    if(debug){
      System.out.println("Data:");
      data.print(data.getRowDimension(), 3);
    }
     
    // construct the matrix Y
    // Y =  data' / sqrt(N-1);
    Matrix Y = data.transpose();
    Y = Y.times(1.0/Math.sqrt(N-1));
    
    //SVD does it all
    //[u, S, PC] = svd(Y);
    SingularValueDecomposition svd = Y.svd();
    
    // calculate the variances
    if(debug)
      System.out.println("Values:");
    //svd.getS().print(svd.getS().getRowDimension(), 3);
    // get the diagonal values and sort them
    V = new double[svd.getS().getRowDimension()];
    for(int i=0; i<svd.getS().getRowDimension(); i++){      
       V[i] = svd.getS().get(i, i);
       if(debug)
         System.out.println(V[i]);
    }
    
    
    
    //System.out.println("V:");
    //svd.getV().print(svd.getV().getRowDimension(), 3);
    
    
    //System.out.println("U:");
    //svd.getU().print(svd.getU().getRowDimension(), 3);
    
    // project the original data
    //signals = PC' * data
    PC = svd.getV();
    if(debug) {
      System.out.println("PC:");
      PC.print(PC.getRowDimension(), 3);
    }
    
    // Project the original data set
    dataProjected = PC.transpose().times(data);
    if(debug) {
      System.out.println("Data projected:");
      dataProjected.print(dataProjected.getRowDimension(), 3);
    }
    
  }
  
  public static void main(String[] args) throws Exception
  {
    
    // get the data
    String dataFile="/project/mary/marcela/quality_parameters/pca/emo_mat.data";
    BufferedReader reader = new BufferedReader(new FileReader(dataFile));
    
    Matrix data = Matrix.read(reader);
    data = data.transpose();
    //data.print(data.getRowDimension(), 3);
    
    PCA pca = new PCA();
    pca.eigenPCA(data, true, true);
    pca.svdPCA(data, true, true);
    
  }
    
    

}
