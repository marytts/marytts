package marytts.modules;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Scanner;
import java.util.Vector;

import marytts.cart.CART;
import marytts.cart.DirectedGraph;
import marytts.cart.StringPredictionTree;
import marytts.cart.io.DirectedGraphReader;
import marytts.datatypes.MaryData;
import marytts.datatypes.MaryDataType;
import marytts.datatypes.MaryXML;
import marytts.exceptions.SynthesisException;
import marytts.features.FeatureDefinition;
import marytts.features.FeatureProcessorManager;
import marytts.features.FeatureRegistry;
import marytts.features.FeatureVector;
import marytts.features.TargetFeatureComputer;
import marytts.htsengine.CartTreeSet;
import marytts.htsengine.HMMData;
import marytts.htsengine.HMMVoice;
import marytts.htsengine.HTSModel;
import marytts.htsengine.HTSPStream;
import marytts.htsengine.HTSParameterGeneration;
import marytts.htsengine.HTSUttModel;
import marytts.modules.phonemiser.AllophoneSet;
import marytts.modules.synthesis.Voice;
import marytts.server.MaryProperties;
import marytts.unitselection.select.Target;
import marytts.unitselection.select.UnitSelector;
import marytts.util.MaryUtils;
import marytts.util.dom.MaryDomUtils;
import marytts.machinelearning.SoP;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.NodeList;
import org.w3c.dom.traversal.NodeIterator;
import org.w3c.dom.traversal.TreeWalker;

public class SoPDurationModeller extends InternalModule
{

  private String sopFileName;
  SoP sopVowel;
  SoP sopConsonant;
  SoP sopPause;

  protected TargetFeatureComputer featureComputer;
  private FeatureProcessorManager featureProcessorManager;
  private AllophoneSet allophoneSet;     
  FeatureDefinition voiceFeatDef;

  /**
   * Constructor which can be directly called from init info in the config file.
   * This constructor will use the registered feature processor manager for the given locale.
   * @param locale a locale string, e.g. "en"
   * @param sopFileName 
   * @throws Exception
   */
  public SoPDurationModeller(String locale, String sopFile)
  throws Exception {
      this(MaryUtils.string2locale(locale), sopFile,
              FeatureRegistry.getFeatureProcessorManager(MaryUtils.string2locale(locale)));
  }
  
  /**
   * Constructor which can be directly called from init info in the config file.
   * Different languages can call this code with different settings.
   * @param locale a locale string, e.g. "en"
   * @param propertyPrefix the prefix to be used when looking up entries in the config files, e.g. "english.duration"
   * @param featprocClassInfo a package name for an instance of FeatureProcessorManager, e.g. "marytts.language.en.FeatureProcessorManager"
   * @throws Exception
   */
  public SoPDurationModeller(String locale, String sopFile, String featprocClassInfo)
  throws Exception
  {
      this(MaryUtils.string2locale(locale), sopFile,
              (FeatureProcessorManager)MaryUtils.instantiateObject(featprocClassInfo));
  }
  
  /**
   * Constructor to be called with instantiated objects.
   * @param locale
   * @param propertyPrefix the prefix to be used when looking up entries in the config files, e.g. "english.duration"
   * @praam featureProcessorManager the manager to use when looking up feature processors.
   */
  protected SoPDurationModeller(Locale locale,
             String sopFile, FeatureProcessorManager featureProcessorManager)
  {
      super("SoPDurationModeller",
              MaryDataType.ALLOPHONES,
              MaryDataType.DURATIONS, locale);
      this.sopFileName = sopFile;
      this.featureProcessorManager = featureProcessorManager;
  }

  public void startup() throws Exception
  {
    super.startup();

    // Read dur.sop file to load linear equations
    // the first line corresponds to vowels and the second to consonants
    String sopFile = MaryProperties.getFilename(sopFileName);
    //System.out.println("sopFileName: " + sopFile);
    String nextLine;
    String strContext="";
    Scanner s = null;
    try {
      s = new Scanner(new BufferedReader(new FileReader(sopFile)));
      
      // The first part contains the feature definition
      while (s.hasNext()) {
        nextLine = s.nextLine(); 
        if (nextLine.trim().equals("")) break;
        else
          strContext += nextLine + "\n";
      }
      // the featureDefinition is the same for vowel, consonant and Pause
      voiceFeatDef = new FeatureDefinition(new BufferedReader(new StringReader(strContext)), false);
 
      // vowel line
      if (s.hasNext()){
        nextLine = s.nextLine();
        System.out.println("line vowel = " + nextLine);
        sopVowel = new SoP(nextLine, voiceFeatDef);
        //sopVowel.printCoefficients();
      }      
      // consonant line
      if (s.hasNext()){
        nextLine = s.nextLine();
        System.out.println("line consonants = " + nextLine);
        sopConsonant = new SoP(nextLine, voiceFeatDef);
        //sopConsonant.printCoefficients();
      }
      // pause line
      if (s.hasNext()){
        nextLine = s.nextLine();
        System.out.println("line pause = " + nextLine);
        sopPause = new SoP(nextLine, voiceFeatDef);
        //sopPause.printCoefficients();
      }               
    } finally {
        if (s != null)
          s.close();
    }   
      
    // get a feature computer
    featureComputer = FeatureRegistry.getTargetFeatureComputer(featureProcessorManager, voiceFeatDef.getFeatureNames());

  }

public MaryData process(MaryData d)
throws Exception
{
  Document doc = d.getDocument(); 
  NodeIterator sentenceIt = MaryDomUtils.createNodeIterator(doc, MaryXML.SENTENCE);
  Element sentence = null;
  while ((sentence = (Element) sentenceIt.nextNode()) != null) {
      // Make sure we have the correct voice:
      Element voice = (Element) MaryDomUtils.getAncestor(sentence, MaryXML.VOICE);
      Voice maryVoice = Voice.getVoice(voice);
      
      if (maryVoice == null) {                
          maryVoice = d.getDefaultVoice();
      }
      if (maryVoice == null) {
          // Determine Locale in order to use default voice
          Locale locale = MaryUtils.string2locale(doc.getDocumentElement().getAttribute("xml:lang"));
          maryVoice = Voice.getDefaultVoice(locale);
      }
    
      allophoneSet = maryVoice.getAllophoneSet();
      TargetFeatureComputer currentFeatureComputer = featureComputer;
     
      // cumulative duration from beginning of sentence, in seconds:
      float end = 0;
      float durInSeconds;
      TreeWalker tw = MaryDomUtils.createTreeWalker(sentence, MaryXML.PHONE, MaryXML.BOUNDARY);
      Element segmentOrBoundary;
      Element previous = null;
      while ((segmentOrBoundary = (Element)tw.nextNode()) != null) {          
          String phone = UnitSelector.getPhoneSymbol(segmentOrBoundary);
          
          Target t = new Target(phone, segmentOrBoundary);                
          t.setFeatureVector(currentFeatureComputer.computeFeatureVector(t));
                                       
          if (segmentOrBoundary.getTagName().equals(MaryXML.BOUNDARY)) { // a pause              
            System.out.print("Pause PHONE: " + phone);
            durInSeconds = (float)sopPause.solve(t, voiceFeatDef);
          } else {
            if (allophoneSet.getAllophone(phone).isVowel()){
              // calculate duration with sopVowel
              System.out.print("Vowel PHONE: " + phone);
              durInSeconds = (float)sopVowel.solve(t, voiceFeatDef);
            } else {
              // calculate duration with sopConsonant
              System.out.print("Cons. PHONE: " + phone);  
              durInSeconds = (float)sopConsonant.solve(t, voiceFeatDef);
            }
          }
          // TODO: where do we check that the solution is log(duration) or duration???
          System.out.format(" = %.3f\n", durInSeconds);
          end += durInSeconds;
          int durInMillis = (int) (1000 * durInSeconds);
          if (segmentOrBoundary.getTagName().equals(MaryXML.BOUNDARY)) {
              segmentOrBoundary.setAttribute("duration", String.valueOf(durInMillis));
          } else { // phone
              segmentOrBoundary.setAttribute("d", String.valueOf(durInMillis));
              segmentOrBoundary.setAttribute("end", String.valueOf(end));
          }
          previous = segmentOrBoundary;
      }
  }
  MaryData output = new MaryData(outputType(), d.getLocale());
  output.setDocument(doc);
  return output;

   }

 
}
