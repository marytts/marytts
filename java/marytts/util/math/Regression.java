package marytts.util.math;

import Jama.Matrix;
import java.util.Vector;
import marytts.util.math.MathUtils;

/***
 * Multiple linear regresion
 * For the case of k independent variables 
 *   x_1, x_2, ... x_k
 * the sample regression equation is given by
 *   y_i = b_0 + b_1*x_1i + b_2*x_2i + ... + b_k*x_ki + e_i
 *     y = X*b
 *   
 * Ref: Walpole, Myers, Myers and Ye, "Probability and statistics for engineers and scientists", 
 *      Prentice Hall, chapter 12, 2002. 
 *   
 * @author marcela 
 *  */
public class Regression {

  
  private double[] coeffs;          // keep coefficient in a Matrix
  private double[] residuals;       // duration(i) - predicted_duration(i)
  private double[] predictedValues; // predicted_duration(i) --> y(i)
  private double correlation;       // correlation between predicted_duration and duration
  private Matrix X;
  private Matrix y;
  private Matrix b;
  
  public Regression(){
    predictedValues = null;
    X = null;
    y = null;
    b = null;
  }
 
  
  /***
   * 
   * @param data         dependent and independent variables
   *                     data={{y1, x11, x12, ... x1k},
   *                           {y2, x21, x22, ... x2k},
   *                           ...
   *                           {yn, xn1, xn2, ... xnk}}
   * @param dependentVar number of the column that will be used as dependent variable --> vector y
   *                     by default the first column is y
   * @param rows number of rows 
   * @param cols number of cols including the dependent variable  
   * @return coefficients or null if problems found
   */
  public static double[] multipleLinearRegression(double[] data, int rows, int cols, boolean intercepTerm){   
    double[] coeffs;
    if (data == null) throw new NullPointerException("Null data");
    if (rows < 0 || cols < 0) throw new IllegalArgumentException("Number of rows and cols must be greater than 0");

    double[][] X;
    int numIndVar;
    if(intercepTerm){ // first column of X is filled with 1s if b_0 != 0
      X = new double[rows][cols];
      coeffs = new double[cols];
      numIndVar = cols;
    }
    else{
      X = new double[rows][cols-1];  
      coeffs = new double[cols-1];
      numIndVar = cols-1;
    }
     
    double[] y = new double[rows];
    
    // Fill the data in the matrix X (independent variables) and vector y (dependet variable)
    int n = 0;  // number of data points
    for (int i=0; i<rows; i++) {
      if(intercepTerm) {
        X[i][0] = 1.0;
        y[i] = data[n++];  // first column is the dependent variable
        for (int j=1; j< cols; j++) {
            X[i][j] = data[n++];
        } 
      } else { // No intercepTerm so no need to fill the first column with 1s
        y[i] = data[n++]; // first column is the dependent variable
        for (int j=0; j< cols-1; j++) {
          X[i][j] = data[n++];
        } 
        
      }      
    }
    
    // Least-square solution y = X * b where:
    // y_i = b_0 + b_1*x_1i + b_2*x_2i + ... + b_k*x_ki  including intercep term
    // y_i =       b_1*x_1i + b_2*x_2i + ... + b_k*x_ki  without intercep term
    try {
      Matrix b = new Matrix(X).solve(new Matrix(y,y.length));
      coeffs = new double[numIndVar];
      for (int j=0; j<=b.getRowDimension(); j++) {
          coeffs[j] = b.get(j, 0);
          System.out.println("coeff[" + j + "]=" + coeffs[j]);
      }      
    } catch (RuntimeException re) {
      return coeffs;
    }
     
    /*
    if (Double.isNaN(data[i]))
      X[i][j] = 0.0;
    else
      X[i][j] = data[i];
      */
  
    return coeffs;  
  
   }

  public double[] multipleLinearRegression(Vector<Double> data, int rows, int cols, boolean intercepTerm){
    
    if (data == null) throw new NullPointerException("Null data");
    if (rows < 0 || cols < 0) throw new IllegalArgumentException("Number of rows and cols must be greater than 0");

    double[][] dataX;
    int numIndVar;
    if(intercepTerm){ // first column of X is filled with 1s if b_0 != 0
      dataX = new double[rows][cols];
      coeffs = new double[cols];
      numIndVar = cols;
    }
    else{
      dataX = new double[rows][cols-1];  
      coeffs = new double[cols-1];
      numIndVar = cols-1;
    }
     
    double[] datay = new double[rows];
    
    // Fill the data in the matrix X (independent variables) and vector y (dependet variable)
    int n = 0;  // number of data points
    for (int i=0; i<rows; i++) {
      if(intercepTerm) {
        dataX[i][0] = 1.0;
        datay[i] = data.elementAt(n++);  // first column is the dependent variable
        for (int j=1; j< cols; j++) {
            dataX[i][j] = data.elementAt(n++);
        } 
      } else { // No intercepTerm so no need to fill the first column with 1s
        datay[i] = data.elementAt(n++); // first column is the dependent variable
        for (int j=0; j< cols-1; j++) {
          dataX[i][j] = data.elementAt(n++);
        } 
        
      }      
    }
    
    // Least-square solution y = X * b where:
    // y_i = b_0 + b_1*x_1i + b_2*x_2i + ... + b_k*x_ki  including intercep term
    // y_i =       b_1*x_1i + b_2*x_2i + ... + b_k*x_ki  without intercep term
    try {
      X = new Matrix(dataX);
      y = new Matrix(datay,datay.length);
      b = X.solve(y);
      coeffs = new double[numIndVar];
      for (int j=0; j<=b.getRowDimension(); j++) {
          coeffs[j] = b.get(j, 0);
          //System.out.println("coeff[" + j + "]=" + coeffs[j]);
      } 
      
    } catch (RuntimeException re) {
      return coeffs;
    }

    return coeffs;  
  
   }  
  
  public double[] getResiduals(){
    // Residuals
    if( X != null && y != null){
      Matrix r = X.times(b).minus(y);
      residuals = r.getColumnPackedCopy();
      return residuals;
    } else {
       System.out.println("No values set for matrix X and y"); 
       return null;
    }
      
    
  }
  
  public double[] getPredictedValues(){
    // Residuals
    if( X != null && y != null && b != null){
      Matrix p = X.times(b);
      predictedValues = p.getColumnPackedCopy();
      return predictedValues;
    } else {
      System.out.println("No values set for matrix X and y"); 
      return null;      
    }
  }

  /***
   * Correlation between original values and predicted ones.
   * @return
   */
  public double getCorrelation(){
    double r;
    double oriValues[];
    if( X != null && y != null && b != null){
      Matrix p = X.times(b);
      predictedValues = p.getColumnPackedCopy();
      oriValues = y.getColumnPackedCopy();      
      r = MathUtils.correlation(predictedValues, oriValues);
      return r;
    } else {
      System.out.println("No values set for matrix X and y"); 
      return 0.0;            
    }    
  }
  
}
