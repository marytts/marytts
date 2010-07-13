package marytts.machinelearning;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.PrintWriter;
import java.io.StringReader;
import java.util.Scanner;

import marytts.features.FeatureDefinition;
import marytts.unitselection.select.Target;


/**
 * Contains the coefficients and factors of an equation of the form:
 * if interceptTterm = TRUE
 *   solution = coeffs[0] + coeffs[1]*factors[0] + coeffs[2]*factors[1] + ... + coeffs[n]*factors[n-1]
 * if interceptterm = FALSE
 *   solution = coeffs[0]*factors[0] + coeffs[1]*factors[1] + ... + coeffs[n]*factors[n]
 *   
 * @author marcela
 *
 */
public class SoP {
  
  private double coeffs[];     // coefficients of the multiple linear equation
  private String factors[];    // variables in the multiple linear 
  private int factorsIndex[];  // indices in featureDefinition
  boolean interceptTerm;
  double correlation;
  double rmse;
  double solution;
  FeatureDefinition featureDefinition = null;
  
  public void setCorrelation(double val){ correlation = val; }
  public void setRMSE(double val){ rmse = val; }
  
  public double[] getCoeffs(){ return coeffs; }
  public double getCorrelation(){ return correlation; }
  public double getRMSE(){ return rmse; }
  public int[] getFactorsIndex(){ return factorsIndex; }
  
  
  /**
   * Build a new empty sop with the given feature definition.
   * @param featDef
   */
  public SoP(FeatureDefinition featDef)
  {
      this.featureDefinition = featDef;
  }
  
 
  /***
   * if b0=true then the number of selected factors 0 numCoeffs-1 (there is one coeff more)
   * if b0=false then the number of selected factor is the same as the number of coeffs
   * When setting the factors, it checks to which indexes correspond according to the featureDefinition.
   *                  
   * @param coeffsVal
   * @param selectedFactorsIndex
   * @param allFactorsList
   * @param b0
   */
  public void setCoeffsAndFactors(double coeffsVal[], int selectedFactorsIndex[], String allFactorsList[], boolean b0) throws Exception
  {  
    if(featureDefinition == null){
      throw new Exception("FeatureDefinition not defined in SoP");
    } else {
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
          factors[i] = allFactorsList[selectedFactorsIndex[i-1]];
          factorsIndex[i-1] = featureDefinition.getFeatureIndex(factors[i]);
        }
      } else {
        coeffs = new double[numFactors];
        factors = new String[numFactors];
        factorsIndex = new int[numFactors];
        for(int i=0; i<numFactors; i++){
          coeffs[i] = coeffsVal[i];          
          factors[i] = allFactorsList[selectedFactorsIndex[i]];
          factorsIndex[i] = featureDefinition.getFeatureIndex(factors[i]);
        }
      }
    }
  }
  
  public SoP(String line, FeatureDefinition feaDef){
    
    this.featureDefinition = feaDef;
    
    String word[] = line.split(" ");
    int j = 0;
    coeffs = new double[word.length/2];
    factors = new String[word.length/2];
    factorsIndex = new int[word.length/2];
    interceptTerm = false;
    for(int i=0; i<word.length; i++){
      //System.out.println("w=" + word[i]);
      coeffs[j] = Double.parseDouble(word[i]);
      factors[j] = word[i+1];      
      if(word[i+1].contentEquals("_")){
        interceptTerm=true;
        factorsIndex[j] = -1; 
      }
      else
        factorsIndex[j] = featureDefinition.getFeatureIndex(factors[j]);
      i++;
      j++;
    }     
  }
  
  public FeatureDefinition getFeatureDefinition(){ return featureDefinition; }
  
  /**
   * Solve the linear equation given the features (factors) in t and coeffs and factors in the SoP object
   *  * if interceptTterm = TRUE
   *   solution = coeffs[0] + coeffs[1]*factors[0] + coeffs[2]*factors[1] + ... + coeffs[n]*factors[n-1]
   * if interceptterm = FALSE
   *   solution = coeffs[0]*factors[0] + coeffs[1]*factors[1] + ... + coeffs[n]*factors[n]
 */
  public double solve(Target t, FeatureDefinition feaDef){
    solution = 0.0f;
    if(interceptTerm){
      // the first factor is empty filled with "_" so it should not be used
      solution = coeffs[0];
      for(int i=1; i<coeffs.length; i++){        
        solution = solution + ( coeffs[i] * t.getFeatureVector().getByteFeature(factorsIndex[i]) );       
        //System.out.format("   solution:  %.3f = %.3f + %.3f * %d (%s)  featureIndex=%d\n", solution, solution, coeffs[i], 
        //    t.getFeatureVector().getByteFeature(factorsIndex[i]), factors[i], factorsIndex[i]);        
      }      
    } else {
      for(int i=0; i<coeffs.length; i++)        
        solution = solution + ( coeffs[i] * t.getFeatureVector().getByteFeature(factorsIndex[i]) );
    }
    
    return solution;
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
      System.out.println("SoP coefficients (factor : factorIndex in FeatureDefinition)");
        for (int j=0; j<coeffs.length; j++) 
          System.out.format(" %.5f (%s : %d)\n", coeffs[j], factors[j], factorsIndex[j]);
    } else 
      System.out.println("There is no coefficients to print (coeffs=null).");
  }
  
  

}
