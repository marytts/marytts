package marytts.machinelearning;

import java.io.PrintWriter;


public class SoP {
  
  private double coeffs[];  
  private String factors[];
  private int factorsIndex[];  // original indices in factors list
  boolean interceptTerm;
  double correlation;
  double rmse;
  
  public void setCorrelation(double val){ correlation = val; }
  public void setRMSE(double val){ rmse = val; }
  
  public double[] getCoeffs(){ return coeffs; }
  public double getCorrelation(){ return correlation; }
  public double getRMSE(){ return rmse; }
  public int[] getFactorsIndex(){ return factorsIndex; }
  
  /***
   * if b0=true then the number of selected factors 0 numCoeffs-1 (there is one coeff more)
   * if b0=false then the number of selected factor is the same as the number of coeffs
   * @param coeffsVal
   * @param selectedFactorsIndex
   * @param allFactorsList
   * @param b0
   */
  public SoP(double coeffsVal[], int selectedFactorsIndex[], String allFactorsList[], boolean b0)
  {    
    interceptTerm = b0;
    int numFactors = selectedFactorsIndex.length;      
    if(interceptTerm){
      // there is one coefficient more than factors
      coeffs = new double[numFactors+1];
      factors = new String[numFactors+1];
      factorsIndex = new int[numFactors];
      coeffs[0] = coeffsVal[0];
      factors[0] = "_";   // if there is intercept term then the first factor is empty here indicated with _
      for(int i=1; i<(numFactors+1); i++){
        coeffs[i] = coeffsVal[i];
        factorsIndex[i-1] = selectedFactorsIndex[i-1];
        factors[i] = allFactorsList[selectedFactorsIndex[i-1]];
      }
    } else {
      coeffs = new double[numFactors];
      factors = new String[numFactors];
      factorsIndex = new int[numFactors];
      for(int i=0; i<numFactors; i++){
        coeffs[i] = coeffsVal[i];
        factorsIndex[i] = selectedFactorsIndex[i];
        factors[i] = allFactorsList[selectedFactorsIndex[i]];
      }

    }
  }
  
  public SoP(String line){
    String word[] = line.split(" ");
    int j = 0;
    coeffs = new double[word.length/2];
    factors = new String[word.length/2];
    interceptTerm = false;
    for(int i=0; i<word.length; i++){
      //System.out.println("w=" + word[i]);
      coeffs[j] = Double.parseDouble(word[i]);
      factors[j] = word[i+1];
      if(word[i+1].contentEquals("_"))
        interceptTerm=true;
      i++;
      j++;
    }  
  }
  
  
  
  /**
   * First line vowel coefficients plus factors, second line consonant coefficients plus factors
   */
  public void saveSelectedFeatures(PrintWriter toSopFile)
  {      
     for (int j=0; j<coeffs.length; j++) 
       toSopFile.print(coeffs[j] + " " + factors[j] + " ");
     toSopFile.println();
  }
  
  public void printCoefficients(){
    if(coeffs != null){
      System.out.println("SoP coefficients:");
        for (int j=0; j<coeffs.length; j++) 
          System.out.format(" %.5f (%s)\n", coeffs[j], factors[j]);
    } else 
      System.out.println("There is no coefficients to print (coeffs=null).");
  }
  
  

}
