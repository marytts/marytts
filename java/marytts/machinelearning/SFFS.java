package marytts.machinelearning;

import java.io.PrintWriter;
import marytts.util.MaryUtils;
import marytts.util.math.MathUtils;
import marytts.util.math.Regression;

/***
* Sequential Floating Forward Search(SFFS) for selection of features
*   Ref: Pudil, P., J. Novovičová, and J. Kittler. 1994. Floating search methods in feature selection. Pattern Recogn. Lett. 15, no. 11: 1119-1125.
*   (http://staff.utia.cas.cz/novovic/files/PudNovKitt_PRL94-Floating.pdf)
*   
* @author marcela
*/
public class SFFS {

  public void sequentialForwardFloatingSelection(String dataFile, int indVariable, String[] features, 
      int X[], int Y[], int d, int D, int rowIni, int rowEnd, boolean interceptTerm, SoP sop) throws Exception {
    
    int indVarColNumber = features.length;  // the last column is the independent variable
 
    int ms;  // most significant
    int ls;  // least significant
        
    double forwardJ[] = new double[3];
    forwardJ[0] = 0.0; // least significance X_k+1
    forwardJ[1] = 0.0; // significance of X_k
    forwardJ[2] = 0.0; // most significance of X_k+1
    
    double backwardJ[] = new double[3];
    backwardJ[0] = 0.0; // least significance X_k-1
    backwardJ[1] = 0.0; // significance X_k
    backwardJ[2] = 0.0; // most significance of X_k-1
    
    int k = X.length;    
    boolean condSFS = true;  // Forward condition to be able to select from Y a most new significant feature
    boolean condSBS = true;  // Backward condition: X has to have at least two elements to be able to select the least significant in X
    double corX = 0.0;
    while(k < d+D && condSFS )
    {          
      // we need at least 1 feature in Y to continue
      if( Y.length > 1) {
        // Step 1. (Inclusion)
        // given X_k create X_k+1 : add the most significant feature of Y to X
        System.out.println("ForwardSelection k=" + k);
        ms = sequentialForwardSelection(dataFile, features, indVarColNumber, X, Y, forwardJ, rowIni, rowEnd, interceptTerm);
        System.out.format("corXplusy=%.4f  corX=%.4f\n", forwardJ[2], forwardJ[1]);
        corX = forwardJ[2];
        System.out.println("Most significant new feature to add: " + features[ms]);
        // add index to selected and remove it form Y
        X = MathUtils.addIndex(X, ms);      
        Y = MathUtils.removeIndex(Y, ms);
        k = k+1; 
      
        // continue with a SBG step
        condSBS = true;
 
        // is this the best (k-1) subset so far
        while( condSBS && (k<=d+D) && k > 1) 
        {     
          if(X.length > 1){
            // Step 3. (Continuation of conditional exclusion)
            // Find the least significant feature x_s in the reduced X'
            System.out.println("\n BackwardSelection k=" + k);
            // get the least significant and check if removing it the correlation is better with or without this feature          
            ls = sequentialBackwardSelection(dataFile, features, indVarColNumber, X, backwardJ, rowIni, rowEnd, interceptTerm);
            corX = backwardJ[1];
            System.out.format(" corXminusx=%.4f  corX=%.4f\n", backwardJ[0], backwardJ[1]);
            System.out.println(" Least significant feature to remove: " + features[ls]);
          
            // is this the best (k-1)-subset so far?
            // if corXminusx > corX
            if(backwardJ[0] >  backwardJ[1]) { // J(X_k - x_s) <= J(X_k-1)
              // exclude xs from X'_k and set k = k-1
              System.out.println(" better without least significant feature (removing feature)");
              X = MathUtils.removeIndex(X, ls);
              k = k-1;  
              corX = backwardJ[0];
              condSBS = true;  
            } else {
              System.out.println(" better with least significant feature (keeping feature)\n");
              condSBS = false;
            }
          } else {
            System.out.println("X has one feature, can not execute a SBS step");
            condSBS = false;
          }
        }  // while SBG      
        System.out.format("k=%d corX=%.4f   ", k, corX);
        printSelectedFeatures(X, features);
        System.out.println("-------------------------\n");      
      } else {  // so X.length == 0
        System.out.println("No more elements in Y for selection");
        condSFS = false;
      }    
    } // while SFG
    // return the set of selected features

    // get the final equation coefficients
    Regression reg = new Regression(); 
    reg.multipleLinearRegression(dataFile, indVariable, X, features, interceptTerm, rowIni, rowEnd);

    // copy the coefficients and selected factors in SoP
    sop.setCoeffsAndFactors(reg.getCoeffs(), X, features, interceptTerm);
    sop.setCorrelation(reg.getCorrelation());
    sop.setRMSE(reg.getRMSE());  
  }
  
   
  /**
   * Find the f feature in Y that maximise J(X+y) 
   * @param dataFile
   * @param features
   * @return the index of Y that maximises J(X+y)
   */
  private int sequentialForwardSelection(String dataFile, String[] features, int indVarColNumber, int X[], int Y[], double J[], int rowIni, int rowEnd, boolean interceptTerm){    
    double sig[] = new double[Y.length];
    int sigIndex[] = new int[Y.length]; // to keep track of the corresponding feature
    double corXplusy[] = new double[Y.length];
    
    // get J(X_k)
    double corX;
    if(X.length>0){
      Regression reg = new Regression();
      reg.multipleLinearRegression(dataFile, indVarColNumber, X, features, interceptTerm, rowIni, rowEnd);
      corX = reg.getCorrelation();
      //System.out.println("corX=" + corX);
    } else 
      corX=0.0;
    
    // Calculate the significance of a new feature y_j (y_j is not included in X)
    //S_k+1(y_j) = J(X_k + y_j) - J(X_k)     
    for(int i=0; i<Y.length; i++){
      // get J(X_k + y_j)
      corXplusy[i] = correlationOfNewFeature(dataFile, features, indVarColNumber, X, Y[i], rowIni, rowEnd, interceptTerm);
      sig[i] = corXplusy[i] - corX;
      sigIndex[i] = Y[i];
      //System.out.println("Significance of new feature[" + sigIndex[i] + "]: " + features[sigIndex[i]] + " = " + sig[i]);  
    }
    // find min
    int minSig = MathUtils.getMinIndex(sig);
    J[0] = corXplusy[minSig];
    // J(X_k) = corX
    J[1] = corX;
    // find max
    int maxSig = MathUtils.getMaxIndex(sig);
    J[2] = corXplusy[maxSig];
    
    return sigIndex[maxSig];
      
  }
    
  /**
   * Find the x feature in X that minimise J(X-x), find the least significant feature in X.
   * @param dataFile
   * @param features
   * @return the x (index) that minimises J(X-x)
   */
  private int sequentialBackwardSelection(String dataFile, String[] features, int indVarColNumber, int X[], double J[], int rowIni, int rowEnd, boolean interceptTerm){    
    double sig[] = new double[X.length];
    double corXminusx[] = new double[X.length];
    int sigIndex[] = new int[X.length]; // to keep track of the corresponding feature
    
    // get J(X_k)
    double corX;
    if(X.length > 0){
      Regression reg = new Regression();
      reg.multipleLinearRegression(dataFile, indVarColNumber, X, features, interceptTerm, rowIni, rowEnd);
      //reg.printCoefficients(X, features);
      corX = reg.getCorrelation();
      //System.out.println("corX=" + corX);
    } else 
      corX = 0.0;
       
    // Calculate the significance a feature x_j (included in X)
    // S_k-1(x_j) = J(X_k) - J(X_k - x_i) 
    for(int i=0; i<X.length; i++){      
      // get J(X_k - x_i)
      corXminusx[i] = correlationOfFeature(dataFile, features, indVarColNumber, X, X[i], rowIni, rowEnd, interceptTerm);
      sig[i] = corX - corXminusx[i];
      sigIndex[i] = X[i];
      //System.out.println("Significance of current feature[" + sigIndex[i] + "]: " + features[sigIndex[i]] + " = " + sig[i]);  
    }    
    // find min
    int minSig = MathUtils.getMinIndex(sig);
    J[0] = corXminusx[minSig];
    // J(X_k) = corX
    J[1] = corX;
    // find max
    int maxSig = MathUtils.getMaxIndex(sig);
    J[2] = corXminusx[maxSig];
    
    return sigIndex[minSig];
  }
  
  
  
  /**
   * Correlation of X minus a feature x which is part of the set X: J(X_k - x_i)
   * @param dataFile one column per feature
   * @param features string array with the list of feature names
   * @param indVarColNumber number of the column that corresponds to the independent variable 
   * @param X set of current feature indexes
   * @param x one feature index in X
   * @return 
   */
  private double correlationOfFeature(String dataFile, String[] features, int indVarColNumber, int[] X, int x, int rowIni, int rowEnd, boolean interceptTerm){

    double corXminusx;
    Regression reg = new Regression();

    // get J(X_k - x_i)
    // we need to remove the index x from X
    int j=0;
    int[] Xminusx = new int[X.length-1];
    for(int i=0; i<X.length; i++)
      if(X[i] != x)
        Xminusx[j++] = X[i];
    reg.multipleLinearRegression(dataFile, indVarColNumber, Xminusx, features, interceptTerm, rowIni, rowEnd);
    //reg.printCoefficients(Xminusx, features);
    corXminusx = reg.getCorrelation();
    //System.out.println("corXminusx=" + corXminusx);      
    //System.out.println("significance of x[" + x + "]: " + features[x] + " = " + (corX-corXminusx));  
    
    return corXminusx;
    
  }
  
  /**
   * Correlation of X plus the new feature y (y is not included in X): J(X_k + y_j)
   * @param dataFile one column per feature
   * @param features string array with the list of feature names
   * @param indVarColNumber number of the column that corresponds to the independent variable
   * @param X set of current feature indexes
   * @param y a feature index that is not in X, new feature
   * @return
   */
  private double correlationOfNewFeature(String dataFile, String[] features, int indVarColNumber, int[] X, int y, int rowIni, int rowEnd, boolean interceptTerm){

    double corXplusy;
    Regression reg = new Regression();
   
    // get J(X_k + y_j)
    // we need to add the index y to X
    int j=0;
    int[] Xplusf = new int[X.length+1];
    for(int i=0; i<X.length; i++)
      Xplusf[i] = X[i];
    Xplusf[X.length] = y;
    
    reg.multipleLinearRegression(dataFile, indVarColNumber, Xplusf, features, interceptTerm, rowIni, rowEnd);
    //reg.printCoefficients(Xplusf, features);
    corXplusy = reg.getCorrelation();
    //System.out.println("corXplusf=" + corXplusy);      
    //System.out.println("significance of x[" + f + "]: " + features[f] + " = " + (corXplusf-corX));  
    
    return corXplusy;
    
  }


  static private void printSelectedFeatures(int X[], String[] features){
    System.out.print("Features: ");
    for(int i=0; i<X.length; i++)
    System.out.print(features[X[i]] + "  ");
    System.out.println();
  }
  
  static private void printSelectedFeatures(int X[], String[] features, PrintWriter file){
    file.print("Features: ");
    for(int i=0; i<X.length; i++)
      file.print(features[X[i]] + "  ");
    file.println();
  }

  
}
