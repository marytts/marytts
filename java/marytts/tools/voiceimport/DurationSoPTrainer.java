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
import marytts.util.math.Regression;
import marytts.util.MaryUtils;

/***
 * Modelling duration using Sum of products (SoP) 
 * @author marcela
 *
 */
public class DurationSoPTrainer extends VoiceImportComponent
{
  protected String features;
  protected DatabaseLayout db = null;
  protected int percent = 0;
  protected boolean success = true;  
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

    Vector<String> lingFactorsVowel = new Vector<String>();
    Vector<String> lingFactorsConsonant = new Vector<String>(); 
      
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
    selectLinguisticFactors(lingFactorsVowel, featureDefinition.getFeatureNames(), "Select linguistic factors for vowels:");
    selectLinguisticFactors(lingFactorsConsonant, featureDefinition.getFeatureNames(), "Select linguistic factors for consonants:");
  
    // data for "duration", "ph_vfront","ph_vrnd","position_type","pos_in_syl"
    Vector<Double> vowel = new Vector<Double>();
    Vector<Double> consonant = new Vector<Double>();
    
    int k = 0;
    int numData = 0;
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
        if (allophoneSet.getAllophone(fv.getFeatureAsString(phoneIndex, featureDefinition)).isVowel()) { // enforce a minimum duration for training data                    
          vowel.add(dur);  // first column is the dependent variable, in this case duration          
          for(int j=0; j<lingFactorsVowel.size(); j++)
            vowel.add((double)fv.getByteFeature(featureDefinition.getFeatureIndex(lingFactorsVowel.elementAt(j))));
          nUnitsVowel++;
          //System.out.println("phone (VOWEL)" + fv.getFeatureAsString(phoneIndex, featureDefinition));
        } else {
            consonant.add(dur);  // first column is the dependent variable, in this case duration          
            for(int j=0; j<lingFactorsConsonant.size(); j++)
              consonant.add((double)fv.getByteFeature(featureDefinition.getFeatureIndex(lingFactorsConsonant.elementAt(j))));
            nUnitsConsonant++; 
          //System.out.println("phone (consonant)" + fv.getFeatureAsString(phoneIndex, featureDefinition));
        }
      } 
         
   }
    
   percent = 10; 
   int cols, rows;
   
   
   // -----------------------------------------------------------------------------------------
   // VOWELS results:
   cols = lingFactorsVowel.size()+1;  // linguistic factors plus duration
   rows = vowel.size()/cols;
   System.out.println("\nResults for Vowels:");
   System.out.println("Number of duration points: " + rows);
   System.out.println("Number of linguistic factors: " + (cols-1));   
   Regression regVowel = new Regression();   
   double coeffsVowel[] = regVowel.multipleLinearRegression(vowel, rows, cols, true);
   
   System.out.println("Regression:");
     System.out.format(" %.5f (intercept)\n", coeffsVowel[0]);
   for(int j=1; j<lingFactorsVowel.size(); j++)
     System.out.format(" %.5f (%s)\n", coeffsVowel[j], lingFactorsVowel.elementAt(j-1));
   
   double[] durVowel = regVowel.getPredictedValues();
   double[] resVowel = regVowel.getResiduals();
   double corVowel = regVowel.getCorrelation();
   
   //MaryUtils.plot(resVowel, "Residuals");   
   //MaryUtils.plot(durVowel, "Predicted");
   System.out.println("Correlation vowels original duration / predicted duration = " + corVowel);
   

   
   //-----------------------------------------------------------------------------------------
   //CONSONANTS results:
   cols = lingFactorsConsonant.size()+1;  // linguistic factors plus duration
   rows = consonant.size()/cols;
   System.out.println("\nResults for Consonants:");
   System.out.println("Number of duration points: " + rows);
   System.out.println("Number of linguistic factors: " + (cols-1));   
   Regression regConsonant = new Regression();   
   double coeffsConsonant[] = regConsonant.multipleLinearRegression(consonant, rows, cols, true);
   
   System.out.println("Regression:");
     System.out.format(" %.5f (intercept)\n", coeffsConsonant[0]);
   for(int j=1; j<lingFactorsConsonant.size(); j++)
     System.out.format(" %.5f (%s)\n", coeffsConsonant[j], lingFactorsConsonant.elementAt(j-1));
   
   double[] durConsonant = regConsonant.getPredictedValues();
   double[] resConsonant = regConsonant.getResiduals();
   double corConsonant = regConsonant.getCorrelation();
   
   //MaryUtils.plot(resConsonant, "Residuals");   
   //MaryUtils.plot(durConsonant, "Predicted");
   System.out.println("Correlation vowels original duration / predicted duration = " + corConsonant);
 
    percent = 100;
    return true;
  }
  
  public boolean selectLinguisticFactors(Vector<String> lingFactors, String featureNames, String label) throws IOException
  {
      features = checkFeatureList(featureNames);
 
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
              saveFeatures(lingFactors, editPane.getText());                
          } catch (Exception ex){
              ex.printStackTrace();
              throw new Error("Error defining replacements");
          }
      }
     return true;
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
          // The following have mean value 0
          if( !(line.contains("style") ||
                line.contains("sentence_punc") ||
                line.contains("next_punctuation") ||
                line.contains("prev_punctuation") ||
                line.contains("edge") )) {
            
               // CHECK: here i am including arbitrarily some....
              // put in front the recomended ones: "ph_vfront","ph_vrnd","position_type","pos_in_syl"              
              if( line.contentEquals("ph_vfront") ||
                  line.contentEquals("ph_vrnd") ||
                  line.contentEquals("position_type") ||
                  line.contentEquals("pos_in_syl") )
                    recommendedFeatureList += line + "\n";
              else
                 featureList += line + "\n";
          }
              
        }
      return recommendedFeatureList + "\n" + featureList;

  }

  private void saveFeatures(Vector<String> lingFactors, String newFeatures)
  {
     System.out.print("Selected linguistic factors: ");
     String fea[] = newFeatures.split("\n");
     for(int i=0; i<fea.length; i++){
       System.out.print(fea[i] + " ");
       lingFactors.add(fea[i]);    
     }
     System.out.println();
  }  
  
  
  public int getProgress()
  {
      return percent;
  }


  public static void main(String[] args) throws Exception
  {
      DurationSoPTrainer sop = new DurationSoPTrainer();   
      sop.compute();
      
  }

  
  
}
