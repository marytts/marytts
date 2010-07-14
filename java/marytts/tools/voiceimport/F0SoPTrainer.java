package marytts.tools.voiceimport;

import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.SortedMap;
import java.util.StringTokenizer;
import java.util.TreeMap;

import javax.swing.JButton;
import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import Jama.Matrix;

import marytts.cart.CART;
import marytts.cart.LeafNode.LeafType;
import marytts.cart.io.MaryCARTWriter;
import marytts.cart.io.WagonCARTReader;
import marytts.features.FeatureDefinition;
import marytts.features.FeatureVector;
import marytts.machinelearning.SFFS;
import marytts.machinelearning.SoP;
import marytts.unitselection.data.Datagram;
import marytts.unitselection.data.FeatureFileReader;
import marytts.unitselection.data.HnmTimelineReader;
import marytts.unitselection.data.TimelineReader;
import marytts.unitselection.data.UnitFileReader;
import marytts.util.math.MathUtils;
import marytts.util.string.PrintfFormat;

public class F0SoPTrainer extends VoiceImportComponent
{
  protected File f0Dir;
  protected String leftF0FeaturesFileName;
  protected String midF0FeaturesFileName;
  protected String rightF0FeaturesFileName;
  protected File leftSoPFile;
  protected File midSoPFile;
  protected File rightSoPFile;
  protected String featureExt = ".pfeats";  
  protected String labelExt = ".lab";
  protected DatabaseLayout db = null;
  protected int percent = 0;
  protected boolean success = true;
  protected boolean interceptTerm = true;
  protected boolean logF0 = false;
  protected int solutionSize;
  
  private final String name = "F0SoPTrainer";
  
  public final String FEATUREFILE = name+".featureFile";
  public final String UNITFILE = name+".unitFile";
  public final String WAVETIMELINE = name+".waveTimeline";
  public final String LABELDIR = name+".labelDir";
  public final String FEATUREDIR = name+".featureDir";
  public final String F0SoPFILE = name+".f0SoPFile";
  private final String SOLUTIONSIZE = name+".solutionSize";
  private final String INTERCEPTTERM = name+".interceptTerm";
  private final String LOGF0SOLUTION = name+".logF0";


  
  public F0SoPTrainer(){
      setupHelp();
  }
  
  public String getName(){
      return name;
  }

   public void initialiseComp()
  {       
      String rootDir = db.getProp(db.ROOTDIR);
      String f0DirName = db.getProp(db.TEMPDIR);
      this.f0Dir = new File(f0DirName);
      if (!f0Dir.exists()){
          System.out.print("temp dir " + f0DirName + " does not exist; ");
          if (!f0Dir.mkdir()){
              throw new Error("Could not create F0DIR");
          }
          System.out.print("Created successfully.\n");
      }  
      this.leftF0FeaturesFileName = f0Dir + "f0.sop.left.feats";
      this.midF0FeaturesFileName = f0Dir + "f0.sop.mid.feats";
      this.rightF0FeaturesFileName = f0Dir + "f0.sop.right.feats";
      this.interceptTerm =  Boolean.valueOf(getProp(INTERCEPTTERM)).booleanValue(); 
      this.logF0 =  Boolean.valueOf(getProp(LOGF0SOLUTION)).booleanValue();
      this.solutionSize = Integer.parseInt(getProp(SOLUTIONSIZE));
     
  }
   
   public SortedMap<String, String> getDefaultProps(DatabaseLayout dbl){
      this.db = dbl;
     if (props == null){
         props = new TreeMap<String, String>();
         String filedir = db.getProp(db.FILEDIR);
         String maryext = db.getProp(db.MARYEXT);
         props.put(FEATUREDIR, db.getProp(db.ROOTDIR) + "phonefeatures" + System.getProperty("file.separator"));
         props.put(LABELDIR, db.getProp(db.ROOTDIR) + "phonelab" + System.getProperty("file.separator"));
         props.put(FEATUREFILE, filedir + "phoneFeatures"+maryext);
         props.put(UNITFILE, filedir + "phoneUnits"+maryext);
         props.put(WAVETIMELINE, db.getProp(db.FILEDIR) + "timeline_waveforms"+db.getProp(db.MARYEXT));      
         props.put(F0SoPFILE,filedir + "f0.sop");
         props.put(INTERCEPTTERM, "true");
         props.put(LOGF0SOLUTION, "false");
         props.put(SOLUTIONSIZE, "5");

     }
     return props;
   }
   
   protected void setupHelp(){         
       props2Help = new TreeMap<String, String>();
       props2Help.put(FEATUREDIR, "directory containing the phonefeatures");
       props2Help.put(LABELDIR, "directory containing the phone label files");
       props2Help.put(FEATUREFILE, "file containing all phone units and their target cost features");
       props2Help.put(UNITFILE, "file containing all phone units");
       props2Help.put(WAVETIMELINE, "file containing all waveforms or models that can genarate them");   
       props2Help.put(F0SoPFILE,"file containing the f0 SoP model. Will be created by this module");
       props2Help.put(INTERCEPTTERM, "whether to include interceptTerm (b0) on the solution equation : b0 + b1X1 + .. bnXn");
       props2Help.put(LOGF0SOLUTION, "whether to use log(independent variable)");
       props2Help.put(SOLUTIONSIZE, "size of the solution, number of dependend variables");

   }
    
  /**/
  public boolean compute() throws Exception
  {
      FeatureFileReader featureFile = FeatureFileReader.getFeatureFileReader(getProp(FEATUREFILE));
      UnitFileReader unitFile = new UnitFileReader(getProp(UNITFILE));
      TimelineReader waveTimeline = null;
      waveTimeline = new TimelineReader(getProp(WAVETIMELINE));
      
      PrintWriter toLeftFeaturesFile = new PrintWriter(new FileOutputStream(leftF0FeaturesFileName));
      PrintWriter toMidFeaturesFile = new PrintWriter(new FileOutputStream(midF0FeaturesFileName));
      PrintWriter toRightFeaturesFile = new PrintWriter(new FileOutputStream(rightF0FeaturesFileName));
      
      FeatureDefinition featureDefinition = featureFile.getFeatureDefinition();
      
      // Select the features that can be used for SFFS selection
      String[] lingFactorsToSelect;
      lingFactorsToSelect = selectLinguisticFactors(featureDefinition.getFeatureNames(), "Select linguistic factors for vowels:");

      System.out.println("F0 CART trainer: exporting f0 features");      
      byte isVowel = featureDefinition.getFeatureValueAsByte("ph_vc", "+");
      int iVC = featureDefinition.getFeatureIndex("ph_vc");
      int iSegsFromSylStart = featureDefinition.getFeatureIndex("segs_from_syl_start");
      int iSegsFromSylEnd = featureDefinition.getFeatureIndex("segs_from_syl_end");
      int iCVoiced = featureDefinition.getFeatureIndex("ph_cvox");
      byte isCVoiced = featureDefinition.getFeatureValueAsByte("ph_cvox", "+");

      int nSyllables = 0;
      for (int i=0, len=unitFile.getNumberOfUnits(); i<len; i++) {
          // We estimate that feature extraction takes 1/10 of the total time
          // (that's probably wrong, but never mind)
          percent = 10*i/len;
          FeatureVector fv = featureFile.getFeatureVector(i);
          if (fv.getByteFeature(iVC) == isVowel) {
              // Found a vowel, i.e. found a syllable.
              int mid = i;
              FeatureVector fvMid = fv;
              // Now find first/last voiced unit in the syllable:
              int first = i;
              for(int j=1, lookLeft = (int)fvMid.getByteFeature(iSegsFromSylStart); j<lookLeft; j++) {
                  fv = featureFile.getFeatureVector(mid-j); // units are in sequential order
                  // No need to check if there are any vowels to the left of this one,
                  // because of the way we do the search in the top-level loop.
                  if (fv.getByteFeature(iCVoiced) != isCVoiced) {
                      break; // mid-j is not voiced
                  }
                  first = mid-j; // OK, the unit we are currently looking at is part of the voiced syllable section
              }
              int last = i;
              for(int j=1, lookRight = (int)fvMid.getByteFeature(iSegsFromSylEnd); j<lookRight; j++) {
                  fv = featureFile.getFeatureVector(mid+j); // units are in sequential order

                  if (fv.getByteFeature(iVC) != isVowel && fv.getByteFeature(iCVoiced) != isCVoiced) {
                      break; // mid+j is not voiced
                  }
                  last = mid+j; // OK, the unit we are currently looking at is part of the voiced syllable section
              }
              // TODO: make this more robust, e.g. by fitting two straight lines to the data:
              Datagram[] midDatagrams = waveTimeline.getDatagrams(unitFile.getUnit(mid), unitFile.getSampleRate());
              Datagram[] leftDatagrams = waveTimeline.getDatagrams(unitFile.getUnit(first), unitFile.getSampleRate());
              Datagram[] rightDatagrams = waveTimeline.getDatagrams(unitFile.getUnit(last), unitFile.getSampleRate());
              if (midDatagrams != null && midDatagrams.length > 0
                      && leftDatagrams != null && leftDatagrams.length > 0
                      && rightDatagrams != null && rightDatagrams.length > 0) {
                  double midF0 = waveTimeline.getSampleRate() / (float) midDatagrams[midDatagrams.length/2].getDuration();
                  double leftF0 = waveTimeline.getSampleRate() / (float) leftDatagrams[0].getDuration();
                  double rightF0 = waveTimeline.getSampleRate() / (float) rightDatagrams[rightDatagrams.length-1].getDuration();
                  //System.out.format("Syllable at %d (length %d ): left = %.3f, mid = %.3f, right = %.3f\n", mid, (last-first+1), leftF0, midF0, rightF0);

                  for(int j=0; j < lingFactorsToSelect.length; j++){
                    byte feaVal = fvMid.getByteFeature(featureDefinition.getFeatureIndex(lingFactorsToSelect[j]));
                    toLeftFeaturesFile.print(feaVal + " ");                    
                    toMidFeaturesFile.print(feaVal + " ");                    
                    toRightFeaturesFile.print(feaVal + " ");                    
                  }
                  // last column is the F0 value
                  if(midF0 == Double.NEGATIVE_INFINITY || midF0 == Double.POSITIVE_INFINITY){
                    //System.out.println("midDatagrams.length/2 = " + midDatagrams.length/2);
                    //System.out.println("midDatagrams[midDatagrams.length/2].getDuration() = " + midDatagrams[midDatagrams.length/2].getDuration());
                    System.out.format("Syllable at %d (length %d ): left = %.3f, mid = %.3f, right = %.3f\n", mid, (last-first+1), leftF0, midF0, rightF0);                    
                    midF0 = (leftF0 + rightF0) / 2.0;
                    System.out.format("  mindF0 is Nan --> changed to (leftF0 + rightF0) / 2.0 = %.3f", midF0);
                  }
     
                  if(logF0){
                    toLeftFeaturesFile.println(Math.log(leftF0));
                    toMidFeaturesFile.println(Math.log(midF0));
                    toRightFeaturesFile.println(Math.log(rightF0));
                  } else {
                    toLeftFeaturesFile.println(leftF0);
                    toMidFeaturesFile.println(midF0);                    
                    toRightFeaturesFile.println(rightF0);
                  }
                  nSyllables++;
                  
              }                 
              // Skip the part we just covered:
              i = last;               
          }
      }
      toLeftFeaturesFile.close();
      toMidFeaturesFile.close();
      toRightFeaturesFile.close();
      System.out.println("F0 features extracted for "+nSyllables+" syllables");
      
      
      int cols, rows;

      double percentToTrain = 0.9;
      
      // the final regression will be saved in this file, one line for vowels, one for consonants and another for pause
      PrintWriter toSopFile = new PrintWriter(new FileOutputStream(getProp(F0SoPFILE)));
      
      // Save first the features definition on the output file
      featureDefinition.writeTo(toSopFile, false);
      toSopFile.println();
      
      SFFS sffs = new SFFS(solutionSize, interceptTerm, logF0);
      
      System.out.println("\n==================================\nProcessing Left F0:");
      SoP sopLeft = new SoP(featureDefinition);
      sffs.trainModel(lingFactorsToSelect, leftF0FeaturesFileName, nSyllables, percentToTrain, sopLeft);
      sopLeft.saveSelectedFeatures(toSopFile);
      
      System.out.println("\n==================================\nProcessing Mid F0:");
      SoP sopMid = new SoP(featureDefinition);
      sffs.trainModel(lingFactorsToSelect, midF0FeaturesFileName, nSyllables, percentToTrain, sopMid);
      sopMid.saveSelectedFeatures(toSopFile);
      
      System.out.println("\n==================================\nProcessing Right F0:");
      SoP sopRight = new SoP(featureDefinition);
      sffs.trainModel(lingFactorsToSelect, rightF0FeaturesFileName, nSyllables, percentToTrain, sopRight);
      sopRight.saveSelectedFeatures(toSopFile);
      
      toSopFile.close();
      
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
 
  protected void setSuccess(boolean val)
  {
      success = val;
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
   
  /**
   * Provide the progress of computation, in percent, or -1 if
   * that feature is not implemented.
   * @return -1 if not implemented, or an integer between 0 and 100.
   */
  public int getProgress()
  {
      return percent;
  }

  public static void main(String[] args) throws Exception
  {
      F0CARTTrainer f0ct = new F0CARTTrainer();
      DatabaseLayout db = new DatabaseLayout(f0ct);
      f0ct.compute();
  }

}

