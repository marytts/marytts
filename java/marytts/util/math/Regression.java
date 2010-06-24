package marytts.util.math;

import Jama.Matrix;
import Jama.EigenvalueDecomposition;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.Vector;
import java.io.PrintWriter;

import marytts.tools.voiceimport.DurationSoPTrainer;
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
 *      Prentice Hall, chapter 12, pg. 400, 2002. 
 *   
 * @author marcela 
 *  */
public class Regression {

  
  private double[] coeffs;          // keep coefficient in a Matrix
  private double[] residuals;       // duration(i) - predicted_duration(i)
  private double[] predictedValues; // predicted_duration(i) --> y(i)
  private double correlation;       // correlation between predicted_duration and duration
  private Matrix b;
  private boolean b0Term;
  
  public Regression(){
    predictedValues = null;
    b = null;
  }
  
  public double[] getResiduals(){
    return residuals;
  }
  
  public double[] getPredictedValues(){
      return predictedValues;
  }

  /***
   * Correlation between original values and predicted ones.
   * @return
   */
  public double getCorrelation(){
      return correlation;
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
  public double[] multipleLinearRegression(double[] data, int rows, int cols, boolean intercepTerm){   
    if (data == null) throw new NullPointerException("Null data");
    if (rows < 0 || cols < 0) throw new IllegalArgumentException("Number of rows and cols must be greater than 0");

    b0Term = intercepTerm;
    Matrix dataX; 
    if(intercepTerm){ // first column of X is filled with 1s if b_0 != 0
      dataX = new Matrix(rows,cols);
      coeffs = new double[cols];
    }
    else{
      dataX = new Matrix(rows,cols-1);
      coeffs = new double[cols-1];
    }     
    double[] datay = new double[rows];
    
    // Fill the data in the matrix X (independent variables) and vector y (dependet variable)
    int n = 0;  // number of data points
    for (int i=0; i<rows; i++) {
      if(intercepTerm) {
        dataX.set(i, 0, 1.0);
        datay[i] = data[n++];  // first column is the dependent variable
        for (int j=1; j< cols; j++)
          dataX.set(i, j, data[n++]);
      } else { // No intercepTerm so no need to fill the first column with 1s
        datay[i] = data[n++]; // first column is the dependent variable
        for (int j=0; j< cols-1; j++)
          dataX.set(i, j, data[n++]);
      }      
    }
    multipleLinearRegression(datay, dataX);
    return coeffs;    
   }
  
  
  public double[] multipleLinearRegression(double[] datay, double[][] datax, boolean intercepTerm){
    if (datay == null || datax==null) throw new NullPointerException("Null data");
    b0Term = intercepTerm;
    int rows = datay.length;
    int cols = datax[0].length;
    Matrix dataX; 
    if(intercepTerm){ // first column of X is filled with 1s if b_0 != 0
      dataX = new Matrix(rows,cols+1);
      coeffs = new double[cols+1];
    }
    else{
      dataX = new Matrix(datax);
      coeffs = new double[cols];
    }
     
    // If intercept, we need to add a ones column to dataX
    if(intercepTerm) {
      for (int i=0; i<rows; i++) {
        dataX.set(i, 0, 1.0);
        for (int j=1; j< cols+1; j++)
          dataX.set(i, j, datax[i][j-1]);
      }      
    }    
    multipleLinearRegression(datay, dataX);    
    return coeffs;      
  }


  public double[] multipleLinearRegression(Vector<Double> vectory, Vector<Double> vectorx, int rows, int cols, boolean intercepTerm){
    if (vectory == null || vectorx==null) throw new NullPointerException("Null data");
    b0Term = intercepTerm;
    Matrix dataX; 
    if(intercepTerm){ // first column of X is filled with 1s if b_0 != 0
      dataX = new Matrix(rows,cols+1);
      coeffs = new double[cols+1];
    }
    else{
      dataX = new Matrix(rows,cols);
      coeffs = new double[cols];
    }     
    double[] datay = new double[rows];
    
    // Fill the data in the matrix X (independent variables) and vector y (dependet variable)
    int n = 0;  // number of data points
    for (int i=0; i<rows; i++) {
      if(intercepTerm) {
        datay[i] = vectory.elementAt(i);  // first column is the dependent variable        
        dataX.set(i, 0, 1.0);
        for (int j=1; j< cols+1; j++) {
          dataX.set(i, j, vectorx.elementAt(n++));
        } 
      } else { // No intercepTerm so no need to fill the first column with 1s
        datay[i] = vectory.elementAt(i); // first column is the dependent variable
        for (int j=0; j< cols; j++) {
          dataX.set(i, j, vectorx.elementAt(n++));
        }         
      }      
    }
    multipleLinearRegression(datay, dataX);    
    return coeffs;      
  }
  
  
  
  /***
   * 
   * @param data Vector contains dependent variable first and then independent variables
   * @param rows 
   * @param cols
   * @param intercepTerm
   * @return
   */
  public double[] multipleLinearRegression(Vector<Double> data, int rows, int cols, boolean intercepTerm){
    
    if (data == null) throw new NullPointerException("Null data");
    if (rows < 0 || cols < 0) throw new IllegalArgumentException("Number of rows and cols must be greater than 0");
    b0Term = intercepTerm;
    Matrix dataX; 
    if(intercepTerm){ // first column of X is filled with 1s if b_0 != 0
      dataX = new Matrix(rows,cols);
      coeffs = new double[cols];
    }
    else{
      dataX = new Matrix(rows,cols-1);
      coeffs = new double[cols-1];
    }
     
    double[] datay = new double[rows];
    
    // Fill the data in the matrix X (independent variables) and vector y (dependet variable)
    int n = 0;  // number of data points
    for (int i=0; i<rows; i++) {
      if(intercepTerm) {
        dataX.set(i, 0, 1.0);
        datay[i] = data.elementAt(n++);  // first column is the dependent variable
        for (int j=1; j< cols; j++)
          dataX.set(i, j, data.elementAt(n++));
      } else { // No intercepTerm so no need to fill the first column with 1s
        datay[i] = data.elementAt(n++); // first column is the dependent variable
        for (int j=0; j< cols-1; j++)
          dataX.set(i, j, data.elementAt(n++));
      }      
    }   
    multipleLinearRegression(datay, dataX);
    return coeffs;  
   }  


  
  public void multipleLinearRegression(Matrix datay, Matrix dataX, boolean intercepTerm){  
    b0Term = intercepTerm;
    if(intercepTerm){ // first column of X is filled with 1s if b_0 != 0      
      int row = dataX.getRowDimension();
      int col = dataX.getColumnDimension();
          
      Matrix B = new Matrix(row, col+1);
      Matrix ones = new Matrix(row, 1);
      for(int i=0; i<row; i++)
        ones.set(i, 0, 1.0);
      B.setMatrix(0, row-1, 0, 0, ones);
      B.setMatrix(0, row-1, 1, col, dataX);
      multipleLinearRegression(datay, B);
    }
    else{
      multipleLinearRegression(datay, dataX);    
      }  
  }
  
  private void multipleLinearRegression(double[] datay, Matrix dataX){
    Matrix y = new Matrix(datay,datay.length);
    multipleLinearRegression(y, dataX);  
  }

  /***
   *  Least-square solution y = X * b where:
   *  y_i = b_0 + b_1*x_1i + b_2*x_2i + ... + b_k*x_ki  including intercep term
   *  y_i =       b_1*x_1i + b_2*x_2i + ... + b_k*x_ki  without intercep term
   * 
   * @param datay
   * @param dataX 
   */
  private void multipleLinearRegression(Matrix datay, Matrix dataX){
    Matrix X, y; 
    try {
      X = dataX;
      y = datay;
      b = X.solve(y);
      coeffs = new double[b.getRowDimension()];
      for (int j=0; j<b.getRowDimension(); j++) {
          coeffs[j] = b.get(j, 0);
          //System.out.println("coeff[" + j + "]=" + coeffs[j]);
      } 
      
      // Residuals:
      Matrix r = X.times(b).minus(y);
      residuals = r.getColumnPackedCopy();
      
      // Predicted values
      Matrix p = X.times(b);
      predictedValues = p.getColumnPackedCopy();
      
      // Correlation between original values and predicted ones       
      correlation = MathUtils.correlation(predictedValues, y.getColumnPackedCopy());
      
    } catch (RuntimeException re) {
        throw new Error("Error solving Least-square solution: y = X * b");
    }  
  }
  
  public void printCoefficients(int[] indices, String[] factors){
    if(coeffs != null){
      System.out.println("Linear regression:");
      if(b0Term){  
        System.out.format(" %.5f\n", coeffs[0]);
        for (int j=1; j<coeffs.length; j++) 
          System.out.format(" %.5f (%s)\n", coeffs[j], factors[indices[j-1]]);
      } else {
        for (int j=0; j<coeffs.length; j++) 
          System.out.format(" %.5f (%s)\n", coeffs[j], factors[indices[j]]);
      }
    } else 
      System.out.println("There is no coefficients to print.");
  }
  
  public void printCoefficients(){
    if(coeffs != null){
        for (int j=0; j<coeffs.length; j++) 
          System.out.format("coeff[%d]=%.5f\n",j,coeffs[j]);
    } else 
      System.out.println("There is no coefficients to print.");
  }
  
  public void multipleLinearRegression(String fileName, boolean intercepTerm) {    
    try {
      BufferedReader reader = new BufferedReader(new FileReader(fileName));        
      Matrix data = Matrix.read(reader);
      int rows = data.getRowDimension()-1;
      int cols = data.getColumnDimension()-1;
    
      Matrix indVar = data.getMatrix(0,rows,0,0); // dataVowels(:,0) -> col 0 is the independent variable
      data = data.getMatrix(0,rows,1,cols); // dataVowels(:,1:cols) -> dependent variables
    
      multipleLinearRegression(indVar, data, intercepTerm);
    
    } catch ( Exception e ) {
      throw new RuntimeException( "Problem reading file " + fileName, e );
    }

  }
  
  /***
   * multipleLinearRegression providing index numbers for the columns in fileName, index 0 correspond to column 1
   * @param fileName
   * @param indVariable column number (index) of the independent variable 
   * @param c int[] column numbers array (indices) of dependent variables
   * @param rowIni and rowEnd should be given from 0 - maxData-1 
   * @param intercepTerm
   */
  public void multipleLinearRegression(String fileName, int indVariable, int[] c, String[] factors, boolean intercepTerm, int rowIni, int rowEnd) {    
    try {
      BufferedReader reader = new BufferedReader(new FileReader(fileName));        
      Matrix data = Matrix.read(reader);
      int rows = data.getRowDimension()-1;
      int cols = data.getColumnDimension()-1;
      
      if(rowIni<0 || rowIni>rows)
        throw new RuntimeException( "Problem reading file, rowIni=" + rowIni + "  and number of rows in file=" + rows);
      if(rowEnd<0 || rowEnd>rows)
        throw new RuntimeException( "Problem reading file, rowIni=" + rowIni + "  and number of rows in file=" + rows);
      if(rowIni > rowEnd)
        throw new RuntimeException( "Problem reading file, rowIni < rowend" + rowIni + " < " + rowEnd);
    
      Matrix indVar = data.getMatrix(rowIni,rowEnd,indVariable,indVariable); // dataVowels(:,0) -> last col is the independent variable
      
      data = data.getMatrix(rowIni, rowEnd, c);  // the dependent variables correspond to the column indices in c
    
      multipleLinearRegression(indVar, data, intercepTerm);
       
    } catch ( Exception e ) {
      throw new RuntimeException( "Problem reading file " + fileName, e );
    }

  }
  
  // Given a set of coefficients and data predic values applying linear equation
  // This function can be used to test with data that was not used in training
  public void predictValues(String fileName, int indVariable, int[] c, String[] factors, boolean intercepTerm, int rowIni, int rowEnd) {    
    try {
      BufferedReader reader = new BufferedReader(new FileReader(fileName));        
      Matrix data = Matrix.read(reader);
      
      int rows = data.getRowDimension()-1;
      int cols = data.getColumnDimension()-1;

      if(rowIni<0 || rowIni>rows)
        throw new RuntimeException( "Problem reading file, rowIni=" + rowIni + "  and number of rows in file=" + rows);
      if(rowEnd<0 || rowEnd>rows)
        throw new RuntimeException( "Problem reading file, rowIni=" + rowIni + "  and number of rows in file=" + rows);
      if(rowIni > rowEnd)
        throw new RuntimeException( "Problem reading file, rowIni < rowend" + rowIni + " < " + rowEnd);
    
      Matrix indVar = data.getMatrix(rowIni,rowEnd,indVariable,indVariable); // dataVowels(:,0) -> last col is the independent variable      
      data = data.getMatrix(rowIni, rowEnd, c);  // the dependent variables correspond to the column indices in c
        
      int numCoeff;
      if(intercepTerm)
        numCoeff = c.length + 1;
      else
        numCoeff = c.length;

      if(b != null) {
        if(b.getRowDimension() == numCoeff) {
          
          if(intercepTerm){ // first column of X is filled with 1s if b_0 != 0      
            int row = data.getRowDimension();
            int col = data.getColumnDimension();
                
            Matrix B = new Matrix(row, col+1);
            Matrix ones = new Matrix(row, 1);
            for(int i=0; i<row; i++)
              ones.set(i, 0, 1.0);
            B.setMatrix(0, row-1, 0, 0, ones);
            B.setMatrix(0, row-1, 1, col, data);
            data = B;
          }
          
          // Residuals:
          Matrix r = data.times(b).minus(indVar);
          residuals = r.getColumnPackedCopy();
      
          // Predicted values
          Matrix p = data.times(b);
          predictedValues = p.getColumnPackedCopy();
      
          // Correlation between original values and predicted ones       
          correlation = MathUtils.correlation(predictedValues, indVar.getColumnPackedCopy());
      
          System.out.println("Correlation predicted values and real: " + correlation);
        } else {
          throw new RuntimeException("Number of columns of data is not the same as number of coeficients");
        }
      } else {
        throw new RuntimeException("Regression coefficients are not loaded");
      }
       
    } catch ( Exception e ) {
      throw new RuntimeException( "Problem reading file " + fileName, e );
    }

  }
  
  
  
  public static void main(String[] args) throws Exception
  {
    Regression reg = new Regression();
    double[] yvals = {25.5, 31.2, 25.9, 38.4, 18.4, 26.7, 26.4, 25.9, 32.0, 25.2, 39.7, 35.7, 26.5};      
    double[][] xvals = {{1.74, 5.30, 10.8},
        {6.32, 5.42, 9.4},
        {6.22, 8.41, 7.2},
        {10.52, 4.63, 8.5},
        {1.19,11.60, 9.4},
        {1.22, 5.85, 9.9},
        {4.10, 6.62, 8.0},
        {6.32, 8.72, 9.1},
        {4.08, 4.42, 8.7},
        {4.15, 7.60, 9.2},
        {10.15, 4.83, 9.4},
        {1.72, 3.12, 7.6},
        {1.70, 5.30, 8.2}};   
    
    
    Matrix A = new Matrix(xvals);
    int row = A.getRowDimension();
    int col = A.getColumnDimension();
    A.print(row, 3);
        
    Matrix B = new Matrix(row, col+1);
    Matrix ones = new Matrix(row, 1);
    for(int i=0; i<row; i++)
      ones.set(i, 0, 1.0);
    B.setMatrix(0, row-1, 0, 0, ones);
    B.setMatrix(0, row-1, 1, col, A);
    B.print(row, 3);
    
    
    
    boolean intercepTerm = true;

    double coeffs[] = reg.multipleLinearRegression(yvals, xvals, intercepTerm);
    reg.printCoefficients();
    
    Vector<Double> y = new Vector<Double>();
    Vector<Double> x = new Vector<Double>();
    Vector<Double> data = new Vector<Double>();
    double array[] = new double[13*4];
    
    int cols = 3;
    int rows = yvals.length;
    
    int n = 0;
    for(int i=0; i<rows; i++){
      y.add(yvals[i]);
      data.add(yvals[i]);
      array[n++] = yvals[i];
      for(int j=0; j<cols; j++){
        x.add(xvals[i][j]);
        data.add(xvals[i][j]);
        array[n++] = xvals[i][j];
      }
    }    
    System.out.println("Vectors y and x:");
    coeffs = reg.multipleLinearRegression(y, x, rows, cols, intercepTerm);  
    reg.printCoefficients();
     
    // All the data in only one Vector<Double>
    cols = 4; // because includes the dependent variable
    rows = yvals.length;
    System.out.println("Vector<> data:");
    coeffs = reg.multipleLinearRegression(data, rows, cols, intercepTerm);
    reg.printCoefficients();
    
    // array
    System.out.println("Vector array [] data:");
    coeffs = reg.multipleLinearRegression(array, rows, cols, intercepTerm);  
    reg.printCoefficients();
    
    
  }

  
  
   
  
}
