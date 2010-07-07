package marytts.modules;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
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

  protected TargetFeatureComputer featureComputer;
  private String propertyPrefix;
  private FeatureProcessorManager featureProcessorManager;
  private AllophoneSet allophoneSet;   

  /**
   * Constructor which can be directly called from init info in the config file.
   * This constructor will use the registered feature processor manager for the given locale.
   * @param locale a locale string, e.g. "en"
   * @param propertyPrefix the prefix to be used when looking up entries in the config files, e.g. "english.duration"
   * @throws Exception
   */
  public SoPDurationModeller(String locale, String propertyPrefix)
  throws Exception {
      this(MaryUtils.string2locale(locale), propertyPrefix,
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
  public SoPDurationModeller(String locale, String propertyPrefix, String featprocClassInfo)
  throws Exception
  {
      this(MaryUtils.string2locale(locale), propertyPrefix,
              (FeatureProcessorManager)MaryUtils.instantiateObject(featprocClassInfo));
  }
  
  /**
   * Constructor to be called with instantiated objects.
   * @param locale
   * @param propertyPrefix the prefix to be used when looking up entries in the config files, e.g. "english.duration"
   * @praam featureProcessorManager the manager to use when looking up feature processors.
   */
  protected SoPDurationModeller(Locale locale,
             String propertyPrefix, FeatureProcessorManager featureProcessorManager)
  {
      super("SoPDurationModeller",
              MaryDataType.ALLOPHONES,
              MaryDataType.DURATIONS, locale);
      if (propertyPrefix.endsWith(".")) this.propertyPrefix = propertyPrefix;
      else this.propertyPrefix = propertyPrefix + ".";
      this.featureProcessorManager = featureProcessorManager;
  }

  public void startup() throws Exception
  {
    super.startup();
    
    // load equations
    sopFileName = "/project/mary/marcela/UnitSel-voices/slt-arctic/temp/dur.sop";    
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
        sopVowel = new SoP(nextLine);
        sopVowel.printCoefficients();
      }
      
      // consonant line
      if (s.hasNext()){
        nextLine = s.nextLine();
        System.out.println("line consonants = " + nextLine);
        sopConsonant = new SoP(nextLine);
        sopConsonant.printCoefficients();
      }
                 
    } finally {
        if (s != null)
          s.close();
    }   

    // get allophoneset
 
    String phoneXML = "/project/mary/marcela/openmary//lib/modules/en/us/lexicon/allophones.en_US.xml";
    System.out.println("Reading allophones set from file: " + phoneXML);
    allophoneSet = AllophoneSet.getAllophoneSet(phoneXML);          

 
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

      String features = d.getOutputParams();
      if (maryVoice != null) {
          featureComputer = FeatureRegistry.getTargetFeatureComputer(maryVoice, features);
          
      } 
      assert featureComputer != null : "Cannot get a feature computer!";
      TargetFeatureComputer currentFeatureComputer = featureComputer;
      
      System.out.println("\n\n****FILE=" + propertyPrefix+"featuredefinition" + "\nmary properties:" + MaryProperties.needFilename(propertyPrefix+"featuredefinition"));
      // TODO: Check this will not be the case for hmm voices....
      //       need to find another way of getting a featureDefinition
      File fdFile = new File(MaryProperties.needFilename(propertyPrefix+"featuredefinition"));
      FeatureDefinition voiceFeatDef = new FeatureDefinition(new BufferedReader(new FileReader(fdFile)), true);
      
      
      // cumulative duration from beginning of sentence, in seconds:
      float end = 0;

      TreeWalker tw = MaryDomUtils.createTreeWalker(sentence, MaryXML.PHONE, MaryXML.BOUNDARY);
      Element segmentOrBoundary;
      Element previous = null;
      while ((segmentOrBoundary = (Element)tw.nextNode()) != null) {          
          String phone = UnitSelector.getPhoneSymbol(segmentOrBoundary);
          System.out.println("PHONE:" + phone);
          Target t = new Target(phone, segmentOrBoundary);                
          t.setFeatureVector(currentFeatureComputer.computeFeatureVector(t));
                    
          if (allophoneSet.getAllophone(phone).isVowel()){
            // calculate duration with sopVowel
            // here i need a durInSeconds = sopVowel.calDuration(Target t)
            
          } else {
            // calculate duration with sopConsonant
            
          }
          byte[] byteValues = t.getFeatureVector().byteValuedDiscreteFeatures;
          
          float durInSeconds;
          if (segmentOrBoundary.getTagName().equals(MaryXML.BOUNDARY)) { // a pause
              //---durInSeconds = enterPauseDuration(segmentOrBoundary, previous, pausetree, pauseFeatureComputer);
            durInSeconds = 0.5f;
          } else {
              //float[] dur = (float[])currentCart.interpret(t);
              float[] dur = {0.02f, 0.03f};
              assert dur != null : "Null duration";
              assert dur.length == 2 : "Unexpected duration length: "+dur.length;
              durInSeconds = dur[1];
              float stddevInSeconds = dur[0];
          }
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
