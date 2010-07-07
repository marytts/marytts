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
import marytts.machinelearning.SFFS;
import marytts.machinelearning.SoP;
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
 *    
 * @author marcela
 */
public class DurationSoPTrainer extends VoiceImportComponent
{
  //protected String features;
  protected DatabaseLayout db = null;
  protected int percent = 0;
  protected boolean success = true;
  protected boolean interceptTerm;
  protected boolean logDuration;
  protected int solutionSize;
  protected File unitlabelDir;
  protected File unitfeatureDir;
  protected File sopDurFile;
  
  private final String name = "DurationSoPTrainer";
  private final String LABELDIR = name+".labelDir";
  private final String FEATUREDIR = name+".featureDir";   
  private final String FEATUREFILE = name+".featureFile";
  private final String UNITFILE = name+".unitFile";
  private final String ALLOPHONESFILE = name+".allophonesFile"; 
  private final String SOLUTIONSIZE = name+".solutionSize";
  private final String INTERCEPTTERM = name+".interceptTerm";
  private final String LOGDURATION = name+".logDuration";
 
  
  public String getName(){
    return name;
  }

  public void initialiseComp()
  {
    this.unitlabelDir = new File(getProp(LABELDIR));
    this.unitfeatureDir = new File(getProp(FEATUREDIR));
    String rootDir = db.getProp(db.ROOTDIR);
    this.interceptTerm =  Boolean.valueOf(getProp(INTERCEPTTERM)).booleanValue(); 
    this.logDuration =  Boolean.valueOf(getProp(LOGDURATION)).booleanValue();
    this.solutionSize = Integer.parseInt(getProp(SOLUTIONSIZE));
    
    String durDir = db.getProp(db.TEMPDIR);
    this.sopDurFile = new File(durDir, "dur.sop");
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
      props.put(INTERCEPTTERM, "true");
      props.put(LOGDURATION, "true");
      props.put(SOLUTIONSIZE, "10");
      
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
    
    // the final regression will be saved in this file, one line for vowels and another for consonants
    PrintWriter toSopFile = new PrintWriter(new FileOutputStream(sopDurFile));
    
    // the following files contain all the feature files in columns
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
          //---if (allophoneSet.getAllophone(fv.getFeatureAsString(phoneIndex, featureDefinition)).name().contentEquals("I")){
          for(int j=0; j < lingFactorsVowel.length; j++)
            toVowelsFile.print(fv.getByteFeature(featureDefinition.getFeatureIndex(lingFactorsVowel[j])) + " ");
          if(logDuration)
            toVowelsFile.println(Math.log(dur)); // last column is the dependent variable, in this case duration
          else
            toVowelsFile.println(dur);
          numVowels++;
          //---}
        } else {
          for(int j=0; j < lingFactorsConsonant.length; j++)
            toConsonantsFile.print(fv.getByteFeature(featureDefinition.getFeatureIndex(lingFactorsConsonant[j])) + " ");
          if(logDuration)
            toConsonantsFile.println(Math.log(dur));
          else
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
   int vd = solutionSize;  // desired size of the solution
   int vD = 0; // maximum deviation allowed with respect to d
   cols = lingFactorsVowel.length;  
   int indVariable = cols; // the last column is the independent variable, in this case duration
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
   if(interceptTerm)
     System.out.println("Using intercept Term for regression");
   else
     System.out.println("No intercept Term for regression");
   if(logDuration)
     System.out.println("Using log(duration) as independent variable" + "\n");
   else
     System.out.println("Using duration as independent variable" + "\n");
   // copy indexes of column features
   int vY[] = new int[lingFactorsVowel.length];
   int vX[] = {};
   for(int j=0; j < lingFactorsVowel.length; j++)
     vY[j] = j;
   
   // we need to remove from vY the column features that have mean 0.0
   vY = checkMeanColumns(vowelsFile, vY, lingFactorsConsonant);
   
   SFFS sffsVowel = new SFFS();
   SoP sopVowel = sffsVowel.sequentialForwardFloatingSelection(vowelsFile, indVariable, lingFactorsVowel, vX, vY, vd, vD, rowIniVowTrain, rowEndVowTrain, interceptTerm);
   
   // Save selected features and coefficients 
   // First line vowel coefficients plus factors, second line consonant coefficients plus factors
   sopVowel.saveSelectedFeatures(toSopFile);
   sopVowel.printCoefficients();
   System.out.println("Correlation vowels original duration / predicted duration = " + sopVowel.getCorrelation() +  
                      "\nRMSE (root mean square error) = " + sopVowel.getRMSE() );   
   Regression regVowel = new Regression();
   regVowel.setCoeffs(sopVowel.getCoeffs());
   System.out.println("\nNumber points used for training=" + (rowEndVowTrain-rowIniVowTrain)); 
   regVowel.predictValues(vowelsFile, cols, sopVowel.getFactorsIndex(), interceptTerm, rowIniVowTrain, rowEndVowTrain); 
   System.out.println("\nNumber points used for testing=" + (rowEndVowTest-rowIniVowTest)); 
   regVowel.predictValues(vowelsFile, cols, sopVowel.getFactorsIndex(), interceptTerm, rowIniVowTest, rowEndVowTest);
   

   //-----------------------------------------------------------------------------------------
   //CONSONANTS results:
   int cd = solutionSize;  // desired size of the solution
   int cD = 0; // maximum deviation allowed with respect to d
   cols = lingFactorsConsonant.length;  // linguistic factors plus duration
   indVariable = cols; // the last column is the independent variable, in this case duration
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
   if(interceptTerm)
     System.out.println("Using intercept Term for regression");
   else
     System.out.println("No intercept Term for regression");
   if(logDuration)
     System.out.println("Using log(duration) as independent variable" + "\n");
   else
     System.out.println("Using duration as independent variable" + "\n");

   // copy indexes of column features
   int cY[] = new int[lingFactorsConsonant.length];
   int cX[] = {};
   for(int j=0; j < lingFactorsConsonant.length; j++)
     cY[j] = j;
   
   // we need to remove from cY the column features that have mean 0.0
   cY = checkMeanColumns(consonantsFile, cY, lingFactorsConsonant);
   SFFS sffsConsonant = new SFFS();
   SoP sopConsonant = sffsConsonant.sequentialForwardFloatingSelection(consonantsFile, indVariable, lingFactorsConsonant, cX, cY, cd, cD, rowIniConTrain, rowEndConTrain, interceptTerm);
   // Save selected features and coefficients 
   // First line vowel coefficients plus factors, second line consonant coefficients plus factors
   sopConsonant.saveSelectedFeatures(toSopFile);
   sopConsonant.printCoefficients();
   System.out.println("Correlation consonants original duration / predicted duration = " + sopConsonant.getCorrelation()  +  
                      "\nRMSE (root mean square error) = " + sopConsonant.getRMSE() );
   
   Regression regConsonant = new Regression();
   regConsonant.setCoeffs(sopConsonant.getCoeffs());
   System.out.println("\nNumber points used for training=" + (rowEndConTrain-rowIniConTrain)); 
   regConsonant.predictValues(consonantsFile, cols, sopConsonant.getFactorsIndex(), interceptTerm, rowIniConTrain, rowEndConTrain);
   System.out.println("\nNumber points used for testing=" + (rowEndConTest-rowIniConTest)); 
   regConsonant.predictValues(consonantsFile, cols, sopConsonant.getFactorsIndex(), interceptTerm, rowIniConTest, rowEndConTest);

   percent = 100;
   
   toSopFile.close();
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
          
          /*
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
          */
          featureList += line + "\n";    
        }
      //return recommendedFeatureList + "\n" + featureList;
      return featureList;
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
      reader.close(); 
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
          Y = MathUtils.removeIndex(Y, i);
        }
      }
    } catch ( Exception e ) {
      throw new RuntimeException( "Problem reading file " + dataFile, e );
    }  
    System.out.println();
    return Y;
  }
  
  
  public int getProgress()
  {
      return percent;
  }


  
  public static void main(String[] args) throws Exception
  {
      /*DurationSoPTrainer sop = new DurationSoPTrainer(); 
      DatabaseLayout db = new DatabaseLayout(sop);
      sop.compute();
      */
      String sopFileName = "/project/mary/marcela/UnitSel-voices/slt-arctic/temp/dur.sop";
      File sopFile = new File(sopFileName);
      
      // Read dur.sop file 
      // the first line corresponds to vowels and the second to consonants
      String nextLine;
      Scanner s = null;
      try {
        s = new Scanner(new BufferedReader(new FileReader(sopFileName)));
        
        // vowel line
        if (s.hasNext()){
          nextLine = s.nextLine();
          System.out.println("line vowel = " + nextLine);
          SoP sopVowel = new SoP(nextLine);
          sopVowel.printCoefficients();
        }
        
        // consonant line
        if (s.hasNext()){
          nextLine = s.nextLine();
          System.out.println("line consonants = " + nextLine);
          SoP sopConsonants = new SoP(nextLine);
          sopConsonants.printCoefficients();
        }
                   
      } finally {
          if (s != null)
            s.close();
      }   
      
  }

  
  
}
