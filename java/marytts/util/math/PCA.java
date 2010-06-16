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
   private Matrix PC;   // principal components 
   
   public double[][] getCovariance(){
     return covariance.getArray();
   }
  
   public double[] getV(){
     return V;
   }

   public double[][] getPC() {
     return PC.getArray();
   }

   public double[][] getDataProjected(Matrix data, boolean debug) {     
     // Project the original data set
     Matrix dataProjected;
     dataProjected = PC.transpose().times(data);
     if(debug) {
       System.out.println("Data projected:");
       dataProjected.print(dataProjected.getRowDimension(), 3);
     }
     return dataProjected.getArray();
   }
   
   public void printPricipalComponents() {
     System.out.println("PC");
     PC.print(PC.getRowDimension(), 4);  
   }
   
   /***
    * perform principal component analysis
    * @param data a vector of doubles
    * @param rows number of rows, trials or examples
    * @param cols number of cols, dimensions or factors
    * @param eigen if true use eigenvalues, if false use SVD (singular value decomposition)
    * @param scale if true use znormalisation, if false just remove the mean from each dimension
    */
   public void principalComponentAnalysis(Vector<Double> data, int rows, int cols, boolean eigen, boolean scale){
     if (data == null) throw new NullPointerException("Null data");
     if (rows < 0 || cols < 0) throw new IllegalArgumentException("Number of rows and cols must be greater than 0");

     Matrix dataX = new Matrix(rows,cols);

     // Fill the data in the matrix X (independent variables) and vector y (dependet variable)
     int n = 0;  // number of data points
     for (int i=0; i<rows; i++) {
       for (int j=0; j< cols; j++) 
         dataX.set(i, j, data.elementAt(n++));
     } 
     boolean debug = false;
     if(eigen)
       eigenPCA(dataX, scale, debug);
     else
       svdPCA(dataX, scale, debug);     
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
   // double element[][] = data.getArrayCopy();
    double mn;
    double sd;
    for(int i=0; i<M; i++){
      mn = MathUtils.mean(data.getArray()[i]);
      if(scale){
        sd = MathUtils.standardDeviation(data.getArray()[i]);
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
    if(debug) {
      System.out.println("Covariance");
      covariance.print(covariance.getRowDimension(), 3);
    }
    
    // find the eigenvectors and eigenvalues
    // eig() returns the values not ordered
    EigenvalueDecomposition pc = covariance.eig();
    if(debug) {
      System.out.println("EigenValues (on the diagonal)");
      pc.getD().print(pc.getD().getRowDimension(), 3);
      System.out.println("EigenVectors");
      pc.getV().print(pc.getV().getRowDimension(), 3);
    }
    
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
    double mn;
    double sd;
    for(int i=0; i<M; i++){
      mn = MathUtils.mean(data.getArray()[i]);
      if(scale){
        sd = MathUtils.standardDeviation(data.getArray()[i]);
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
    
  }
  
  public void principalComponentAnalysis(String fileName, boolean scale) {    
    try {
      BufferedReader reader = new BufferedReader(new FileReader(fileName));        
      Matrix data = Matrix.read(reader);
      int rows = data.getRowDimension()-1;
      int cols = data.getColumnDimension()-1;
  
      data = data.getMatrix(0,rows,1,cols); // dataVowels(:,1:cols) -> dependent variables
    
      svdPCA(data.transpose(), scale, false);

    } catch ( Exception e ) {
      throw new RuntimeException( "Problem reading file " + fileName, e );
    }

  }
  
  
  public static void main(String[] args) throws Exception
  {
    
    // get the data
    /*
    String dataFile="/project/mary/marcela/quality_parameters/pca/emo_mat.data";
    BufferedReader reader = new BufferedReader(new FileReader(dataFile));
    
    Matrix data = Matrix.read(reader);
    data = data.transpose();
    //data.print(data.getRowDimension(), 3);
    
    PCA pca = new PCA();
    pca.eigenPCA(data, true, true);
    pca.svdPCA(data, true, true);
    */
    
    /*
    String dataFile="/project/mary/marcela/UnitSel-voices/slt-arctic/temp/t1.vowels";
    BufferedReader reader = new BufferedReader(new FileReader(dataFile));        
    Matrix dataVowels = Matrix.read(reader);
    Matrix data = dataVowels.transpose();
    
    PCA pca = new PCA();
    //pca.eigenPCA(data, true, false);
    pca.svdPCA(data, true, false);
    pca.printPricipalComponents();
    
    String durFile="/project/mary/marcela/UnitSel-voices/slt-arctic/temp/dur.vowels";
    BufferedReader durReader = new BufferedReader(new FileReader(durFile));
    Matrix duration = Matrix.read(durReader);
       
    Regression regVowel = new Regression();
    regVowel.multipleLinearRegression(duration.getColumnPackedCopy(), dataVowels.getArray(), true);
    regVowel.printCoefficients();
    System.out.println("Correlation vowels original duration / predicted duration = " + regVowel.getCorrelation());   
    */
    
    String dataFile="/project/mary/marcela/UnitSel-voices/slt-arctic/temp/dur-vowels.data";
    BufferedReader reader = new BufferedReader(new FileReader(dataFile));        
    Matrix dataVowels = Matrix.read(reader);
    int rows = dataVowels.getRowDimension()-1;
    int cols = dataVowels.getColumnDimension()-1;
    
    Matrix indVar = dataVowels.getMatrix(0,rows,0,0); // dataVowels(:,0) -> col 0 is the independent variable
    dataVowels = dataVowels.getMatrix(0,rows,1,cols); // dataVowels(:,1:cols) -> dependent variables
    
    PCA pca = new PCA();
    //pca.eigenPCA(dataVowels.transpose(), true, false);
    pca.svdPCA(dataVowels.transpose(), true, false);
    pca.printPricipalComponents();

    Regression regVowel = new Regression();    
    regVowel.multipleLinearRegression(indVar, dataVowels, true);
    regVowel.printCoefficients();
    System.out.println("Correlation vowels original duration / predicted duration = " + regVowel.getCorrelation());
    
    
  }
    
    

}
