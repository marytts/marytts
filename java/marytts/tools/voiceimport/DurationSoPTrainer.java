package marytts.tools.voiceimport;

import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Scanner;
import java.util.TreeMap;
import java.util.SortedMap;
import java.util.Vector;

import javax.swing.JButton;
import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import marytts.features.FeatureDefinition;
import marytts.features.FeatureVector;
import marytts.modules.phonemiser.AllophoneSet;
import marytts.unitselection.data.FeatureFileReader;
import marytts.unitselection.data.Unit;
import marytts.unitselection.data.UnitFileReader;
import marytts.util.math.MathUtils;
import marytts.util.math.PCA;
import marytts.util.math.Regression;
import marytts.util.MaryUtils;

/***
 * Modelling duration using Sum of products (SoP) 
 * @author marcela
 *
 */
public class DurationSoPTrainer extends VoiceImportComponent
{
  //protected String features;
  protected DatabaseLayout db = null;
  protected int percent = 0;
  protected boolean success = true;
  protected boolean intercepTerm = true;
  protected File unitlabelDir;
  protected File unitfeatureDir;
  
  private final String name = "DurationSoPTrainer";
  private final String LABELDIR = name+".labelDir";
  private final String FEATUREDIR = name+".featureDir";   
  private final String FEATUREFILE = name+".featureFile";
  private final String UNITFILE = name+".unitFile";
  private final String ALLOPHONESFILE = name+".allophonesFile";
  
  
  public String getName(){
    return name;
}

  public void initialiseComp()
  {
    this.unitlabelDir = new File(getProp(LABELDIR));
    this.unitfeatureDir = new File(getProp(FEATUREDIR));
    String rootDir = db.getProp(db.ROOTDIR);
  }

  public SortedMap<String, String> getDefaultProps(DatabaseLayout dbl){
    this.db = dbl;
    if (props == null){
      props = new TreeMap<String, String>();
      String fileSeparator = System.getProperty("file.separator");
      props.put(FEATUREDIR, db.getProp(db.ROOTDIR) + "phonefeatures" + fileSeparator);
      props.put(LABELDIR, db.getProp(db.ROOTDIR) + "phonelab" + fileSeparator);            
      props.put(FEATUREFILE, db.getProp(db.FILEDIR) + "phoneFeatures"+db.getProp(db.MARYEXT));
      props.put(UNITFILE, db.getProp(db.FILEDIR) + "phoneUnits"+db.getProp(db.MARYEXT));
      props.put(ALLOPHONESFILE, db.getProp(db.ALLOPHONESET));
    }
   return props; 
  }
  
  protected void setupHelp(){
    props2Help = new TreeMap<String, String>();
    props2Help.put(FEATUREDIR, "directory containing the phonefeatures");
    props2Help.put(LABELDIR, "directory containing the phone labels");                
    props2Help.put(FEATUREFILE, "file containing all phone units and their target cost features");
    props2Help.put(UNITFILE, "file containing all phone units");
    props2Help.put(ALLOPHONESFILE, "allophones set file (XML format) it will be taken from ../openmary/lib/modules/language/...)");
  }
  
  protected void setSuccess(boolean val)
  {
      success = val;
  }  
  
  public boolean compute() throws Exception
  {
    String durDir = db.getProp(db.TEMPDIR);
    String vowelsFile = durDir+"vowels.feats";
    String consonantsFile = durDir+"consonants.feats";
    String[] lingFactorsVowel;
    String[] lingFactorsConsonant; 
      
    AllophoneSet allophoneSet;   
    String phoneXML = getProp(ALLOPHONESFILE);
    System.out.println("Reading allophones set from file: " + phoneXML);
    allophoneSet = AllophoneSet.getAllophoneSet(phoneXML);          
    
    FeatureFileReader featureFile = FeatureFileReader.getFeatureFileReader(getProp(FEATUREFILE));
    UnitFileReader unitFile = new UnitFileReader(getProp(UNITFILE));

    FeatureDefinition featureDefinition = featureFile.getFeatureDefinition();
    FeatureVector fv;
    int nUnitsVowel = 0;
    int nUnitsConsonant = 0;
    
    //System.out.println("Feature names: " + featureDefinition.getFeatureNames());
    // select features that will be used as linguistic factors on the regression
    lingFactorsVowel = selectLinguisticFactors(featureDefinition.getFeatureNames(), "Select linguistic factors for vowels:");
    lingFactorsConsonant = selectLinguisticFactors(featureDefinition.getFeatureNames(), "Select linguistic factors for consonants:");
    
    PrintWriter toVowelsFile = new PrintWriter(new FileOutputStream(vowelsFile));
    PrintWriter toConsonantsFile = new PrintWriter(new FileOutputStream(consonantsFile));
    /*
    Vector<Double> vowelDur = new Vector<Double>();
    Vector<Double> vowel = new Vector<Double>();
    Vector<Double> consonantDur = new Vector<Double>();
    Vector<Double> consonant = new Vector<Double>();
    */
    
    int k = 0;
    //int numData = 0;
    int numVowels=0;
    int numConsonants=0;
    // index of phone
    int phoneIndex = featureDefinition.getFeatureIndex("phone");
    for (int i=0, len=unitFile.getNumberOfUnits(); i<len; i++) {
      // We estimate that feature extraction takes 1/10 of the total time
      // (that's probably wrong, but never mind)
      percent = 10*i/len;
      
      Unit u = unitFile.getUnit(i);
      double dur = u.duration / (float) unitFile.getSampleRate();              
      
      fv = featureFile.getFeatureVector(i); 
      
      // first select vowell phones
      // when phone is 0 ??
      if(fv.getByteFeature(phoneIndex) > 0 && dur >= 0.01 ){  
        if (allophoneSet.getAllophone(fv.getFeatureAsString(phoneIndex, featureDefinition)).isVowel()){
          for(int j=0; j < lingFactorsVowel.length; j++)
            toVowelsFile.print(fv.getByteFeature(featureDefinition.getFeatureIndex(lingFactorsVowel[j])) + " ");
          //toVowelsFile.println(Math.log(dur)); // last column is the dependent variable, in this case duration
          toVowelsFile.println(dur);
          numVowels++;
        } else {
          for(int j=0; j < lingFactorsConsonant.length; j++)
            toConsonantsFile.print(fv.getByteFeature(featureDefinition.getFeatureIndex(lingFactorsConsonant[j])) + " ");
          //toConsonantsFile.println(Mat.log(dur)); 
          toConsonantsFile.println(dur);
          numConsonants++;
        }
      }          
   }
   toVowelsFile.close();
   toConsonantsFile.close();
   percent = 10; 
   int cols, rows;
   
   
   // -----------------------------------------------------------------------------------------
   // VOWELS results:
   cols = lingFactorsVowel.length;
   rows = numVowels;
   System.out.println("\nProcessing Vowels:");
   System.out.println("Number of duration points: " + rows);
   System.out.println("Number of linguistic factors: " + cols);  
   
   int Y[] = new int[lingFactorsVowel.length-2];
   int X[] = new int[2];
   X[0] = 0;
   X[1] = 1;
   for(int j=2; j < lingFactorsVowel.length; j++)
     Y[j-2] = j;
     
   sequentialForwardFloatingSelection(vowelsFile, lingFactorsVowel, X, Y);
   
   /*
   System.out.println("PCA analysis:");   
   PCA pcaVowel = new PCA();   
   pcaVowel.principalComponentAnalysis(vowelsFile, false, true);
   pcaVowel.printPricipalComponents(lingFactorsVowel, 1);   
   pcaVowel.printImportanceOfComponents();
   
   
   System.out.println("Linear regression analysis:");
   Regression regVowel = new Regression(); 
   //regVowel.multipleLinearRegression(vowelsFile, true);
   int c[] = {0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16};  // the last column is always the independent variable
   regVowel.multipleLinearRegression(vowelsFile, cols, c, lingFactorsVowel, true);
   
   regVowel.printCoefficients(c, lingFactorsVowel);
   System.out.println("Correlation vowels original duration / predicted duration = " + regVowel.getCorrelation());
   */
   //-----------------------------------------------------------------------------------------
   //CONSONANTS results:
/*   
   cols = lingFactorsConsonant.size();  // linguistic factors plus duration
   rows = numConsonants;
   System.out.println("\nResults for Consonants:");
   System.out.println("Number of duration points: " + rows);
   System.out.println("Number of linguistic factors: " + cols);   
   
   PCA pcaConsonant = new PCA();   
   pcaConsonant.principalComponentAnalysis(consonantsFile, false, true);
   pcaConsonant.printPricipalComponents(lingFactorsConsonant, 1); 
   pcaConsonant.printImportanceOfComponents();
   
   Regression regConsonant = new Regression(); 
   regConsonant.multipleLinearRegression(consonantsFile, true);
   regConsonant.printCoefficients(lingFactorsConsonant);
   System.out.println("Correlation vowels original duration / predicted duration = " + regConsonant.getCorrelation());

*/ 
    percent = 100;
    return true;
  }
  
  public String[] selectLinguisticFactors(String featureNames, String label) throws IOException
  {
      String[] lingFactors=null;
      String features = checkFeatureList(featureNames);
 
      final JFrame frame = new JFrame(label);
      GridBagLayout gridBagLayout = new GridBagLayout();
      GridBagConstraints gridC = new GridBagConstraints();
      frame.getContentPane().setLayout( gridBagLayout );
      
      final JEditorPane editPane = new JEditorPane();
      editPane.setPreferredSize(new Dimension(500, 500));
      editPane.setText(features);        
      
      JButton saveButton = new JButton("Save");
      saveButton.addActionListener(new ActionListener() {
         public void actionPerformed(ActionEvent e) {                
             setSuccess(true);
             frame.setVisible(false);
          }
      });
      JButton cancelButton = new JButton("Cancel");
      cancelButton.addActionListener(new ActionListener() {
          public void actionPerformed(ActionEvent e) {
              setSuccess(false);
              frame.setVisible(false);
          }
      });
      
      gridC.gridx = 0;
      gridC.gridy = 0;
      // resize scroll pane:
      gridC.weightx = 1;
      gridC.weighty = 1;
      gridC.fill = GridBagConstraints.HORIZONTAL;
      JScrollPane scrollPane = new JScrollPane(editPane);
      scrollPane.setPreferredSize(editPane.getPreferredSize());
      gridBagLayout.setConstraints( scrollPane, gridC );
      frame.getContentPane().add(scrollPane);
      gridC.gridy = 1;
      // do not resize buttons:
      gridC.weightx = 0;
      gridC.weighty = 0;
      JPanel buttonPanel = new JPanel();
      buttonPanel.setLayout(new FlowLayout());
      buttonPanel.add(saveButton);
      buttonPanel.add(cancelButton);
      gridBagLayout.setConstraints( buttonPanel, gridC );
      frame.getContentPane().add(buttonPanel);
      frame.pack();
      frame.setVisible(true);
      
      do {
          try {
              Thread.sleep(10); 
          } catch (InterruptedException e) {}
      } while (frame.isVisible());
      frame.dispose();

      if (success) {
          try{
              lingFactors = saveFeatures(editPane.getText());                
          } catch (Exception ex){
              ex.printStackTrace();
              throw new Error("Error defining replacements");
          }
      }
     //return true;
      return lingFactors;
  }

  private String checkFeatureList(String featureNames) throws IOException
  {
      String featureList = "";
      String recommendedFeatureList = "";
      String feaList[] = featureNames.split(" ");
      String line;
     
      for(int i=0; i<feaList.length; i++){
          line = feaList[i];
          
          // CHECK: Maybe we need to exclude some features from the selection list???
          // The following have variance 0
          if( !(line.contains("style") ||
                line.contains("sentence_punc") ||
                line.contains("next_punctuation") ||
                line.contains("prev_punctuation") ||
                line.contains("ph_cplace") ||
                line.contains("ph_cvox") ||
                line.contains("ph_vc") ||
                line.contains("onsetcoda") ||
                line.contains("edge") )) {
            
               // CHECK: here i am including arbitrarily some....
              // put in front the recomended ones: "ph_vfront","ph_vrnd","position_type","pos_in_syl"                
              if( line.contentEquals("ph_vfront") ||
                  line.contentEquals("ph_height") ||
                  line.contentEquals("ph_vlng") ||
                  line.contentEquals("ph_vrnd") ||
                  line.contentEquals("ph_cplace") ||
                  line.contentEquals("ph_ctype") ||
                  line.contentEquals("ph_cvox") ||
                  line.contentEquals("phone") ||
                  line.contentEquals("position_type") )
                    recommendedFeatureList += line + "\n";
              else
                 featureList += line + "\n";
          }
              
        }
      return recommendedFeatureList + "\n" + featureList;
      //return "";

  }
 
  
  private String[] saveFeatures(String newFeatures)
  {     
     String fea[] = newFeatures.split("\n");
     String[] lingFactors = new String[fea.length];
     System.out.print("Selected linguistic factors (" + fea.length + "):");
     for(int i=0; i<fea.length; i++){
       System.out.print(fea[i] + " ");
       lingFactors[i] = fea[i];
     }     
     System.out.println();
     return lingFactors;
  }  
  
  
  
  private void sequentialForwardSelection1(String dataFile, String[] features){
    
    int indVarColNumber = features.length;  // the last column is always the independent variable
    System.out.println("Linear regression analysis:");
    Regression reg = new Regression(); 
    //regVowel.multipleLinearRegression(vowelsFile, true);
    //int c[] = {0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16};  
    
    double cor[] = new double[features.length];
    // for i=0
    int c0[] = {0,1,2,3};
    reg.multipleLinearRegression(dataFile, indVarColNumber, c0, features, false);
    reg.printCoefficients(c0, features);
    cor[0] = reg.getCorrelation();
    System.out.println("i=0 Correlation original / predicted ind. feature = " + cor[0]);
    
    for(int i=4; i<features.length; i++){
      int c[] = {0, 1, 2, 3, i};
      reg.multipleLinearRegression(dataFile, indVarColNumber, c, features, false);
      reg.printCoefficients(c, features);
      cor[i] = reg.getCorrelation();
      System.out.println("i=" + i + " Correlation original / predicted ind. feature = " + cor[i]);  
    }
    System.out.println();
    
    // sort the correlations, this function returns the cor array already ordered from lower to highest
    int sortCorInd[] = MathUtils.quickSort(cor);
    System.out.println();
    
    for(int i=sortCorInd.length-1; i>=0; i--)
      System.out.println("Correlation = " + cor[i] + " --> " + features[sortCorInd[i]]);
    System.out.println();
  }
  
  
  private void sequentialForwardFloatingSelection1(String dataFile, String[] features){
    
    int indVarColNumber = features.length;  // the last column is the independent variable
    
    //int X[] = {0,1,2,3,4,5,6,7,8,9};
    //int Y[] = {10,11,12,13,14,15,16};
    int X[] = {0,1,2,3,4};
    int Y[] = {5,6,7,8,9,10,11,12,13,14,15,16}; 

    int ms;  // most significant
    int ls;  // least significant
    
    int d = 5;  // desired size of the solution
    int D = 10; // maximum deviation allowed with respect to d
    
    // The idea of the forward selection is add to the set the feature with the most cor
    double forwardJ[] = new double[3];
    forwardJ[0] = 0.0; // least significance X_k+1
    forwardJ[1] = 0.0; // significance of X_k
    forwardJ[2] = 0.0; // most significance of X_k+1
    
    double backwardJ[] = new double[3];
    backwardJ[0] = 0.0; // least significance X_k-1
    backwardJ[1] = 0.0; // significance X_k
    backwardJ[2] = 0.0; // most significance of X_k-1
    
    int k = 5;
    while(k < d+D)
    {
      System.out.println("k=" + k); 
      // Step 1. (Inclusion)
      // given X_k create X_k+1 : add the most significant feature of Y to X
      System.out.println("ForwardSelection");
      ms = sequentialForwardSelection(dataFile, features, indVarColNumber, X, Y, forwardJ);   
      System.out.println("Most significant new feature[" + ms + "]: " + features[ms]);
      X = addIndex(X, ms);    
      System.out.println();
      
      
      // Step 2. (Conditional exclusion)
      // find the least significant feature of X_k+1
      System.out.println("BackwardSelection I");
      ls = sequentialBackwardSelection(dataFile, features, indVarColNumber, X, backwardJ);
      System.out.println("Least significant feature[" + ls + "]: " + features[ls]);
      
      // is this the best (k-1) subset so far
      if( ls != ms ) // X' = X_k+1 - x_r
      {
        // if ls is not ms then exclude it from X
        X = removeIndex(X, ls);  // here J(X'_k) > J(X_k) because untik here it was added a most significant feature
                                 // and removed a least significant feature
 
        while(k > 2) {
          // Step 3. (Continuation of conditional exclusion)
          // Find the least significant feature x_s in the reduced X'
          System.out.println("BackwardSelection II");
          ls = sequentialBackwardSelection(dataFile, features, indVarColNumber, X, backwardJ);
          System.out.println("Least significant feature[" + ls + "]: " + features[ls]);
          
          if(backwardJ[0] >  backwardJ[1]) { // J(X_k - x_s) <= J(X_k-1)
            // exclude xs from X'_k and set k = k-1
            X = removeIndex(X, ls);
            k = k-1;
             
            // if(k==2)
            //   return to step 1.
            // else
            //   repeat step 3             
          } // else
            // return to setep 1.
        }
        
      } else {
        // this means that ls == ms, so the least signicant feature is the one just added
        // set k=k+1 and go to 1
        k = k+1;
       
      }
    }     
  }

  private void sequentialForwardFloatingSelection(String dataFile, String[] features, int X[], int Y[]){
    
    int indVarColNumber = features.length;  // the last column is the independent variable
    
    //int X[] = {0,1,2,3,4,5,6,7,8,9};
    //int Y[] = {10,11,12,13,14,15,16};
    //int X[] = {0,1};
    //int Y[] = {2,3,4,5,6,7,8,9,10,11,12,13,14,15,16}; 

    int ms;  // most significant
    int ls;  // least significant
    
    int d = 2;  // desired size of the solution
    int D = 5; // maximum deviation allowed with respect to d
    
    // The idea of the forward selection is add to the set the feature with the most cor
    double forwardJ[] = new double[3];
    forwardJ[0] = 0.0; // least significance X_k+1
    forwardJ[1] = 0.0; // significance of X_k
    forwardJ[2] = 0.0; // most significance of X_k+1
    
    double backwardJ[] = new double[3];
    backwardJ[0] = 0.0; // least significance X_k-1
    backwardJ[1] = 0.0; // significance X_k
    backwardJ[2] = 0.0; // most significance of X_k-1
    
    int k = X.length;
    boolean continueSBS = true;
    double corX = 0.0;
    while(k < d+D)
    {     
      // Step 1. (Inclusion)
      // given X_k create X_k+1 : add the most significant feature of Y to X
      System.out.println("ForwardSelection");
      ms = sequentialForwardSelection(dataFile, features, indVarColNumber, X, Y, forwardJ);
      System.out.println("corXplusy=" + forwardJ[2] + "  corX=" + forwardJ[1]);
      corX = forwardJ[2];
      System.out.println("Most significant new feature[" + ms + "]: " + features[ms]);
      X = addIndex(X, ms);
      
      System.out.println("k=" + k + "  Forward corX=" + corX + "\n"); 
      continueSBS = true;
      k = k+1;
      
      // is this the best (k-1) subset so far
      while( continueSBS && (k<d+D)) // X' = X_k+1 - x_r
      {        
          // Step 3. (Continuation of conditional exclusion)
          // Find the least significant feature x_s in the reduced X'
          System.out.println("BackwardSelection");
          ls = sequentialBackwardSelection(dataFile, features, indVarColNumber, X, backwardJ);
          corX = backwardJ[1];
          System.out.println("corXminusx=" + backwardJ[0] + "  corX=" + backwardJ[1]);
          
          // is this the best (k-1)-subset so far?
          // if corXminusx > corX
          if(backwardJ[0] >  backwardJ[1]) { // J(X_k - x_s) <= J(X_k-1)
            // exclude xs from X'_k and set k = k-1
            X = removeIndex(X, ls);
            k = k-1;         
            System.out.println("better without xs");
            System.out.println("Least significant feature[" + ls + "]: " + features[ls]);
            System.out.println("k=" + k + "  Backward corX=" + corX + "\n");
            continueSBS = true;  
          } else {
            // return to step 1.
            System.out.println("better with xs\n");
            continueSBS = false;
          }
      }
      System.out.print("k=" + k + "  corX=" + corX + "  ");
      printSelectedFeatures(X, features);
      System.out.println("-------------------------\n");
      }  
  }
  
  void printSelectedFeatures(int X[], String[] features){
    System.out.print("Features:");
    for(int i=0; i<X.length; i++)
    System.out.print(features[X[i]] + "  ");
    System.out.println();
  }
  
  int[] addIndex(int[] X, int x){
    int newX[] = new int[X.length+1];
    for(int i=0; i<X.length; i++)
      newX[i] = X[i];
    newX[X.length] = x;
    return newX;
  }
  
  int[] removeIndex(int[] X, int x){
    int newX[] = new int[X.length-1];
    int j=0;
    for(int i=0; i<X.length; i++)
      if( X[i] != x)
        newX[j++] = X[i];
    return newX;
  }  
  
  /**
   * Find the f feature in Y that maximise J(X+y) 
   * @param dataFile
   * @param features
   * @return the index of Y that maximises J(X+y)
   */
  private int sequentialForwardSelection(String dataFile, String[] features, int indVarColNumber, int X[], int Y[], double J[]){    
    double sig[] = new double[Y.length];
    int sigIndex[] = new int[Y.length]; // to keep track of the corresponding feature
    double corXplusy[] = new double[Y.length];
    
    // get J(X_k)
    Regression reg = new Regression();
    reg.multipleLinearRegression(dataFile, indVarColNumber, X, features, true);
    double corX = reg.getCorrelation();
    //System.out.println("corX=" + corX);
    
    // Calculate the significance of a new feature y_j (y_j is not included in X)
    //S_k+1(y_j) = J(X_k + y_j) - J(X_k)     
    for(int i=0; i<Y.length; i++){
      // get J(X_k + y_j)
      corXplusy[i] = correlationOfNewFeature(dataFile, features, indVarColNumber, X, Y[i]);
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
  private int sequentialBackwardSelection(String dataFile, String[] features, int indVarColNumber, int X[], double J[]){    
    double sig[] = new double[X.length];
    double corXminusx[] = new double[X.length];
    int sigIndex[] = new int[X.length]; // to keep track of the corresponding feature
    
    // get J(X_k)
    Regression reg = new Regression();
    reg.multipleLinearRegression(dataFile, indVarColNumber, X, features, true);
    //reg.printCoefficients(X, features);
    double corX = reg.getCorrelation();
    //System.out.println("corX=" + corX);
       
    // Calculate the significance a feature x_j (included in X)
    // S_k-1(x_j) = J(X_k) - J(X_k - x_i) 
    for(int i=0; i<X.length; i++){      
      // get J(X_k - x_i)
      corXminusx[i] = correlationOfFeature(dataFile, features, indVarColNumber, X, X[i]);
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
  private double correlationOfFeature(String dataFile, String[] features, int indVarColNumber, int[] X, int x){

    double corXminusx;
    Regression reg = new Regression();
    
    // J(X_k) is corX
    
    // get J(X_k - x_i)
    // we need to remove the index x from X
    int j=0;
    int[] Xminusx = new int[X.length-1];
    for(int i=0; i<X.length; i++)
      if(X[i] != x)
        Xminusx[j++] = X[i];
    reg.multipleLinearRegression(dataFile, indVarColNumber, Xminusx, features, true);
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
  private double correlationOfNewFeature(String dataFile, String[] features, int indVarColNumber, int[] X, int y){

    double corXplusy;
    Regression reg = new Regression();
   
    // get J(X_k + y_j)
    // we need to add the index y to X
    int j=0;
    int[] Xplusf = new int[X.length+1];
    for(int i=0; i<X.length; i++)
      Xplusf[i] = X[i];
    Xplusf[X.length] = y;
    
    reg.multipleLinearRegression(dataFile, indVarColNumber, Xplusf, features, true);
    //reg.printCoefficients(Xplusf, features);
    corXplusy = reg.getCorrelation();
    //System.out.println("corXplusf=" + corXplusy);      
    //System.out.println("significance of x[" + f + "]: " + features[f] + " = " + (corXplusf-corX));  
    
    return corXplusy;
    
  }
  
  
  public int getProgress()
  {
      return percent;
  }


  
  public static void main(String[] args) throws Exception
  {
      DurationSoPTrainer sop = new DurationSoPTrainer(); 
      DatabaseLayout db = new DatabaseLayout(sop);
      sop.compute();
      
  }

  
  
}
