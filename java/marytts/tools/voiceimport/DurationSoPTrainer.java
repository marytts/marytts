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

import Jama.Matrix;

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
 * SoP is modelled using multiple linear regression
 * Selection of features is performed with Sequential Floating Forward Search(SFFS):
 *   Ref: Pudil, P., J. Novovičová, and J. Kittler. 1994. Floating search methods in feature selection. Pattern Recogn. Lett. 15, no. 11: 1119-1125.
 *   (http://staff.utia.cas.cz/novovic/files/PudNovKitt_PRL94-Floating.pdf)
 *   
 * @author marcela
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
    String vowelsFile = durDir + "vowels.feats";
    String consonantsFile = durDir + "consonants.feats";
    
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
 
    int k = 0;
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
          //toConsonantsFile.println(Math.log(dur)); 
          toConsonantsFile.println(dur);
          numConsonants++;
        }
      }          
   }
   toVowelsFile.close();
   toConsonantsFile.close();
   percent = 10; 
   int cols, rows;
   
   intercepTerm=true;
   
   
   // -----------------------------------------------------------------------------------------
   // VOWELS results:
   int vd = 5;  // desired size of the solution
   int vD = 5; // maximum deviation allowed with respect to d
   cols = lingFactorsVowel.length;
   rows = numVowels;
   int rowIniVowTrain = 0;
   int percentVal = (int)(Math.floor((numVowels*0.7)));
   int rowEndVowTrain = percentVal-1;
   int rowIniVowTest = percentVal;
   int rowEndVowTest = percentVal + (numVowels-percentVal-1) - 1;
     
   System.out.println("\nProcessing Vowels:");
   System.out.println("Number of duration points: " + rows 
       + "\nNumber of points used for training from " + rowIniVowTrain + " to " + rowEndVowTrain + "(Total train=" + (rowEndVowTrain-rowIniVowTrain) 
       + ")\nNumber of points used for testing from " + rowIniVowTest + " to " + rowEndVowTest + "(Total test=" + (rowEndVowTest-rowIniVowTest) + ")");
   System.out.println("Number of linguistic factors: " + cols);  
   System.out.println("Max number of selected features in SFFS: " + (vd+vD));
   if(intercepTerm)
     System.out.println("Using intercept Term for regresion" + "\n");
   else
     System.out.println("No intercept Term for regresion" + "\n");
   // copy indexes of column features
   int vY[] = new int[lingFactorsVowel.length];
   int vX[] = {};
   for(int j=0; j < lingFactorsVowel.length; j++)
     vY[j] = j;
   
   // we need to remove from vY the column features that have mean 0.0
   vY = checkMeanColumns(vowelsFile, vY, lingFactorsConsonant);
   
   int vowelSelectFea[] = sequentialForwardFloatingSelection(vowelsFile, lingFactorsVowel, vX, vY, vd, vD, rowIniVowTrain, rowEndVowTrain);
   Regression regVowel = new Regression(); 
   regVowel.multipleLinearRegression(vowelsFile, cols, vowelSelectFea, lingFactorsVowel, intercepTerm, rowIniVowTrain, rowEndVowTrain);
   regVowel.printCoefficients(vowelSelectFea, lingFactorsVowel);
   System.out.println("Correlation vowels original duration / predicted duration = " + regVowel.getCorrelation());
   

   System.out.println("\nNumber points used for training=" + (rowEndVowTrain-rowIniVowTrain)); 
   regVowel.predictValues(vowelsFile, cols, vowelSelectFea, lingFactorsVowel, intercepTerm, rowIniVowTrain, rowEndVowTrain);

   
   System.out.println("\nNumber points used for testing=" + (rowEndVowTest-rowIniVowTest)); 
   regVowel.predictValues(vowelsFile, cols, vowelSelectFea, lingFactorsVowel, intercepTerm, rowIniVowTest, rowEndVowTest);
   

   //-----------------------------------------------------------------------------------------
   //CONSONANTS results:
   int cd = 5;  // desired size of the solution
   int cD = 5; // maximum deviation allowed with respect to d
   cols = lingFactorsConsonant.length;  // linguistic factors plus duration
   rows = numConsonants;  
   int rowIniConTrain = 0;
   percentVal = (int)(Math.floor((numConsonants*0.7)));
   int rowEndConTrain = percentVal-1;
   int rowIniConTest = percentVal;
   int rowEndConTest = percentVal + (numConsonants-percentVal-1) - 1;
   
   System.out.println("\nProcessing Consonants:");
   System.out.println("Number of duration points: " + rows 
     + "\nNumber of points used for training from " + rowIniConTrain + " to " + rowEndConTrain + "(Total train=" + (rowEndConTrain-rowIniConTrain) 
     + ")\nNumber of points used for testing from " + rowIniConTest + " to " + rowEndConTest + "(Total test=" + (rowEndConTest-rowIniConTest) + ")");
   System.out.println("Number of linguistic factors: " + cols);
   System.out.println("Max number of selected features in SFFS: " + (cd+cD));
   if(intercepTerm)
     System.out.println("Using intercept Term for regresion" + "\n");
   else
     System.out.println("No intercept Term for regresion" + "\n");
   // copy indexes of column features
   int cY[] = new int[lingFactorsConsonant.length];
   int cX[] = {};
   for(int j=0; j < lingFactorsConsonant.length; j++)
     cY[j] = j;
   
   // we need to remove from cY the column features that have mean 0.0
   cY = checkMeanColumns(consonantsFile, cY, lingFactorsConsonant);
   
   int consonantSelectFea[] = sequentialForwardFloatingSelection(consonantsFile, lingFactorsConsonant, cX, cY, cd, cD, rowIniConTrain, rowEndConTrain);

   Regression regConsonant = new Regression(); 
   regConsonant.multipleLinearRegression(consonantsFile, cols, consonantSelectFea, lingFactorsConsonant, intercepTerm, rowIniConTrain, rowEndConTrain);
   regConsonant.printCoefficients(consonantSelectFea, lingFactorsConsonant);
   System.out.println("Correlation consonants original duration / predicted duration = " + regConsonant.getCorrelation());
 

   System.out.println("\nNumber points used for training=" + (rowEndConTrain-rowIniConTrain)); 
   regConsonant.predictValues(consonantsFile, cols, consonantSelectFea, lingFactorsConsonant, intercepTerm, rowIniConTrain, rowEndConTrain);

   
   System.out.println("\nNumber points used for testing=" + (rowEndConTest-rowIniConTest)); 
   regConsonant.predictValues(consonantsFile, cols, consonantSelectFea, lingFactorsConsonant, intercepTerm, rowIniConTest, rowEndConTest);
   
   percent = 100;
   return true;
  }
  
  
  private int[] sequentialForwardFloatingSelection(String dataFile, String[] features, int X[], int Y[], int d, int D, int rowIni, int rowEnd){
    
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
        ms = sequentialForwardSelection(dataFile, features, indVarColNumber, X, Y, forwardJ, rowIni, rowEnd);
        System.out.format("corXplusy=%.4f  corX=%.4f\n", forwardJ[2], forwardJ[1]);
        corX = forwardJ[2];
        System.out.println("Most significant new feature to add: " + features[ms]);
        // add index to selected and remove it form Y
        X = addIndex(X, ms);      
        Y = removeIndex(Y, ms);
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
            ls = sequentialBackwardSelection(dataFile, features, indVarColNumber, X, backwardJ, rowIni, rowEnd);
            corX = backwardJ[1];
            System.out.format(" corXminusx=%.4f  corX=%.4f\n", backwardJ[0], backwardJ[1]);
            System.out.println(" Least significant feature to remove: " + features[ls]);
          
            // is this the best (k-1)-subset so far?
            // if corXminusx > corX
            if(backwardJ[0] >  backwardJ[1]) { // J(X_k - x_s) <= J(X_k-1)
              // exclude xs from X'_k and set k = k-1
              System.out.println(" better without least significant feature (removing feature)");
              X = removeIndex(X, ls);
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
    return X;
  }
  
   
  /**
   * Find the f feature in Y that maximise J(X+y) 
   * @param dataFile
   * @param features
   * @return the index of Y that maximises J(X+y)
   */
  private int sequentialForwardSelection(String dataFile, String[] features, int indVarColNumber, int X[], int Y[], double J[], int rowIni, int rowEnd){    
    double sig[] = new double[Y.length];
    int sigIndex[] = new int[Y.length]; // to keep track of the corresponding feature
    double corXplusy[] = new double[Y.length];
    
    // get J(X_k)
    double corX;
    if(X.length>0){
      Regression reg = new Regression();
      reg.multipleLinearRegression(dataFile, indVarColNumber, X, features, intercepTerm, rowIni, rowEnd);
      corX = reg.getCorrelation();
      //System.out.println("corX=" + corX);
    } else 
      corX=0.0;
    
    // Calculate the significance of a new feature y_j (y_j is not included in X)
    //S_k+1(y_j) = J(X_k + y_j) - J(X_k)     
    for(int i=0; i<Y.length; i++){
      // get J(X_k + y_j)
      corXplusy[i] = correlationOfNewFeature(dataFile, features, indVarColNumber, X, Y[i], rowIni, rowEnd);
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
  private int sequentialBackwardSelection(String dataFile, String[] features, int indVarColNumber, int X[], double J[], int rowIni, int rowEnd){    
    double sig[] = new double[X.length];
    double corXminusx[] = new double[X.length];
    int sigIndex[] = new int[X.length]; // to keep track of the corresponding feature
    
    // get J(X_k)
    double corX;
    if(X.length > 0){
      Regression reg = new Regression();
      reg.multipleLinearRegression(dataFile, indVarColNumber, X, features, intercepTerm, rowIni, rowEnd);
      //reg.printCoefficients(X, features);
      corX = reg.getCorrelation();
      //System.out.println("corX=" + corX);
    } else 
      corX = 0.0;
       
    // Calculate the significance a feature x_j (included in X)
    // S_k-1(x_j) = J(X_k) - J(X_k - x_i) 
    for(int i=0; i<X.length; i++){      
      // get J(X_k - x_i)
      corXminusx[i] = correlationOfFeature(dataFile, features, indVarColNumber, X, X[i], rowIni, rowEnd);
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
  private double correlationOfFeature(String dataFile, String[] features, int indVarColNumber, int[] X, int x, int rowIni, int rowEnd){

    double corXminusx;
    Regression reg = new Regression();

    // get J(X_k - x_i)
    // we need to remove the index x from X
    int j=0;
    int[] Xminusx = new int[X.length-1];
    for(int i=0; i<X.length; i++)
      if(X[i] != x)
        Xminusx[j++] = X[i];
    reg.multipleLinearRegression(dataFile, indVarColNumber, Xminusx, features, intercepTerm, rowIni, rowEnd);
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
  private double correlationOfNewFeature(String dataFile, String[] features, int indVarColNumber, int[] X, int y, int rowIni, int rowEnd){

    double corXplusy;
    Regression reg = new Regression();
   
    // get J(X_k + y_j)
    // we need to add the index y to X
    int j=0;
    int[] Xplusf = new int[X.length+1];
    for(int i=0; i<X.length; i++)
      Xplusf[i] = X[i];
    Xplusf[X.length] = y;
    
    reg.multipleLinearRegression(dataFile, indVarColNumber, Xplusf, features, intercepTerm, rowIni, rowEnd);
    //reg.printCoefficients(Xplusf, features);
    corXplusy = reg.getCorrelation();
    //System.out.println("corXplusf=" + corXplusy);      
    //System.out.println("significance of x[" + f + "]: " + features[f] + " = " + (corXplusf-corX));  
    
    return corXplusy;
    
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
  
  // remove the columns with mean = 0.0
  private int[] checkMeanColumns(String dataFile, int Y[], String[] features){
    try {
      BufferedReader reader = new BufferedReader(new FileReader(dataFile));        
      Matrix data = Matrix.read(reader);
      data = data.transpose();  // then I have easy access to the columns
      int rows = data.getRowDimension()-1;
      int cols = data.getColumnDimension()-1;
  
      data = data.getMatrix(0,rows,1,cols); // dataVowels(:,1:cols) -> dependent variables
      int M = data.getRowDimension();
      double mn;
      for(int i=0; i<M; i++){
        mn = MathUtils.mean(data.getArray()[i]);
        if(mn == 0.0){
          System.out.println("Removing feature: " + features[i] + " from list of features because it has mean=0.0");
          Y = removeIndex(Y, i);
        }
      }
    } catch ( Exception e ) {
      throw new RuntimeException( "Problem reading file " + dataFile, e );
    }    
    return Y;
  }
  
  
  private void printSelectedFeatures(int X[], String[] features){
    System.out.print("Features: ");
    for(int i=0; i<X.length; i++)
    System.out.print(features[X[i]] + "  ");
    System.out.println();
  }
  
  private void printSelectedFeatures(int X[], String[] features, PrintWriter file){
    file.print("Features: ");
    for(int i=0; i<X.length; i++)
      file.print(features[X[i]] + "  ");
    file.println();
  }
  
  private int[] addIndex(int[] X, int x){
    int newX[] = new int[X.length+1];
    for(int i=0; i<X.length; i++)
      newX[i] = X[i];
    newX[X.length] = x;
    return newX;
  }
  
  private int[] removeIndex(int[] X, int x){
    int newX[] = new int[X.length-1];
    int j=0;
    for(int i=0; i<X.length; i++)
      if( X[i] != x)
        newX[j++] = X[i];
    return newX;
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
