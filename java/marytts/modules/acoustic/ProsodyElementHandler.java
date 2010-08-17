package marytts.modules.acoustic;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.text.Document;

import marytts.cart.io.DirectedGraphReader;
import marytts.datatypes.MaryXML;
import marytts.exceptions.MaryConfigurationException;
import marytts.features.FeatureDefinition;
import marytts.features.FeatureProcessorManager;
import marytts.features.FeatureRegistry;
import marytts.features.TargetFeatureComputer;
import marytts.unitselection.select.Target;
import marytts.util.dom.MaryDomUtils;
import marytts.util.math.Polynomial;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.w3c.dom.traversal.NodeIterator;
import org.w3c.dom.traversal.TreeWalker;

/**
 * This module will aply prosody modifications to the already predicted values (dur and f0) in the acoustparams
 * @author Sathish
 */
public class ProsodyElementHandler {
    
    public ProsodyElementHandler(){}
    
    
    public void process(org.w3c.dom.Document doc) {
          applyProsodySpecifications(doc);
    }
    
    /**
     * A method to modify prosody modifications
     * @param doc
     */
    private void applyProsodySpecifications(org.w3c.dom.Document doc) {
        
        TreeWalker tw = MaryDomUtils.createTreeWalker(doc, doc, MaryXML.PHONE, MaryXML.BOUNDARY, MaryXML.PROSODY);
        Element e = null;
        
        // TODO: read prosody tags recursively 
        while ((e = (Element) tw.nextNode()) != null) {
            
            if ( "prosody".equals(e.getNodeName() ) ) {
                NodeList nl = e.getElementsByTagName("ph");
                applyNewContourSpecifications(nl, e);
                applySpeechRateSpecifications(nl, e);
            }
        }      
   }
    
    /**
     * Apply 'rate' requirements to ACOUSTPARAMS
     * @param nl
     * @param prosodyElement
     */
    private void applySpeechRateSpecifications(NodeList nl, Element prosodyElement) {
      
        String rateAttribute = null;
        if ( !prosodyElement.hasAttribute("rate") ) {
            return;
        }
        
        rateAttribute =  prosodyElement.getAttribute("rate");
        Pattern p = Pattern.compile("[+|-]\\d+%");
        
        // Split input with the pattern
        Matcher m = p.matcher(rateAttribute);
        if ( m.find() ) {
            double percentage = new Integer(rateAttribute.substring(1, rateAttribute.length()-1)).doubleValue();
            if ( rateAttribute.startsWith("+") ) {
                setSpeechRateSpecifications(nl, percentage, -1.0);
            }
            else {
                setSpeechRateSpecifications(nl, percentage, +1.0);
            }
        }
    }

    /**
     * set duration specifications according to 'rate' requirements
     * @param nl
     * @param percentage
     * @param incriment
     */
    private void setSpeechRateSpecifications(NodeList nl, double percentage, double incriment) {
        
        for ( int i=0; i < nl.getLength(); i++ ) {
            Element e = (Element) nl.item(i); 
            if ( !e.hasAttribute("d") ) {
                continue;
            }
            double durAttribute = new Double(e.getAttribute("d")).doubleValue();
            double newDurAttribute = durAttribute + ( incriment * percentage * durAttribute / 100);
            e.setAttribute("d", newDurAttribute+"");
            //System.out.println(durAttribute+" = " +newDurAttribute);
        }
        
        Element e = (Element) nl.item(0);
        
        Element rootElement = e.getOwnerDocument().getDocumentElement();
        NodeIterator nit = MaryDomUtils.createNodeIterator(rootElement, MaryXML.PHONE, MaryXML.BOUNDARY);
        Element nd; 
        double duration = 0.0;
        for ( int i=0; (nd = (Element) nit.nextNode()) != null; i++ ) {
            if ( "boundary".equals(nd.getNodeName()) ) {
                if ( nd.hasAttribute("duration") ) {
                    duration += new Double(nd.getAttribute("duration")).doubleValue();
                }
            }
            else {
                if ( nd.hasAttribute("d") ) {
                    duration += new Double(nd.getAttribute("d")).doubleValue();
                }
            }
            double endTime = 0.001 * duration;
            nd.setAttribute("end", endTime+"");
            //System.out.println(nd.getNodeName()+" = " +nd.getAttribute("end"));
        }
      
    }

  /**
     * 
     * @param nl
     * @param prosodyElement
     */
    private void applyNewContourSpecifications(NodeList nl, Element prosodyElement) {
        
        
        String contourAttribute = null;
        if ( prosodyElement.hasAttribute("contour") ) {
            contourAttribute =  prosodyElement.getAttribute("contour");
        }
        
        String pitchAttribute = null;
        if ( prosodyElement.hasAttribute("pitch") ) {
            pitchAttribute =  prosodyElement.getAttribute("pitch");
        }
        
        if ( contourAttribute == null && pitchAttribute == null ) {
            return;
        }
        
        double[] contour = getContinuousContour(nl);
        contour = interpolateNonZeroValues(contour);
        double[] coeffs     = Polynomial.fitPolynomial(contour, 1);
        double[] polyValues = Polynomial.generatePolynomialValues(coeffs, 100, 0, 1);
        double[] diffValues = new double[100];
        
        // Extract base contour from original contour
        for ( int i=0; i < contour.length ; i++ ) {
            diffValues[i] =  contour[i] - polyValues[i];
        }
              
        polyValues = setBaseContourModifications(polyValues, contourAttribute, pitchAttribute);
        
        // Now, imposing back the diff. contour
        for ( int i=0; i < contour.length ; i++ ) {
            contour[i] =  diffValues[i] + polyValues[i];
        }
        
        setModifiedContour(nl, contour);
     
        return;         
    }
    
    
    /**
     * To set new modified contour into XML
     * @param nl
     * @param contour
     */
    private void setModifiedContour(NodeList nl, double[] contour) {
        
        Element firstElement =  (Element) nl.item(0);
        Element lastElement =  (Element) nl.item(nl.getLength()-1);
        
        double fEnd = (new Double(firstElement.getAttribute("end"))).doubleValue();
        double fDuration = 0.001 * (new Double(firstElement.getAttribute("d"))).doubleValue();
        double lEnd = (new Double(lastElement.getAttribute("end"))).doubleValue();
        double fStart = fEnd - fDuration; // 'prosody' tag starting point
        double duration = lEnd - fStart;  // duaration of 'prosody' modification request
        
        Map<Integer, Integer> f0Map;
        
        for ( int i=0; i < nl.getLength(); i++ ) {
            
            Element e = (Element) nl.item(i);
            String f0Attribute = e.getAttribute("f0");
            
            if( f0Attribute == null || "".equals(f0Attribute) ) {
                continue;
            }
            
            double phoneEndTime       = (new Double(e.getAttribute("end"))).doubleValue();
            double phoneDuration = 0.001 * (new Double(e.getAttribute("d"))).doubleValue();
            
            Pattern p = Pattern.compile("(\\d+,\\d+)");
            
            // Split input with the pattern
            Matcher m = p.matcher(e.getAttribute("f0"));
            String setF0String = "";
            while ( m.find() ) {
                String[] f0Values = (m.group().trim()).split(",");
                Integer percent = new Integer(f0Values[0]);
                Integer f0Value = new Integer(f0Values[1]);
                double partPhone = phoneDuration * (percent.doubleValue()/100.0);
                
                int placeIndex  = (int) Math.floor((( ((phoneEndTime - phoneDuration) - fStart ) +  partPhone ) * 100 ) / (double) duration );
                if ( placeIndex >= 100 ) { 
                    placeIndex = 99;
                }
                setF0String = setF0String + "(" + percent + "," +(int) contour[placeIndex] + ")" ;          
                
            }
          
            e.setAttribute("f0", setF0String);
        }    
  }

    
    
    /**
     * Set modifications to base contour (first order polynomial fit contour) 
     * @param polyValues
     * @param contourAttribute
     * @param pitchAttribute
     * @return
     */
    private double[] setBaseContourModifications(double[] polyValues, String contourAttribute, String pitchAttribute) {
        
        if(pitchAttribute != null && !"".equals(pitchAttribute) ) {
            polyValues = setPitchSpecifications( polyValues, pitchAttribute );
        }
        
        if(contourAttribute != null && !"".equals(contourAttribute) ) {
            polyValues = setContourSpecifications( polyValues, contourAttribute );
        }
        
        return polyValues;
    }
    
    
    /**
     * Set all specifications to original contour
     * @param polyValues
     * @param contourAttribute
     * @return
     */
    private double[] setContourSpecifications(double[] polyValues, String contourAttribute) {
     
        Map<String, String> f0Specifications = getContourSpecifications(contourAttribute);
        Iterator<String> it =  f0Specifications.keySet().iterator();
        double[] modifiedF0Values = new double[100];
        Arrays.fill(modifiedF0Values, 0.0);
        
        if ( polyValues.length != modifiedF0Values.length ) {
            throw new RuntimeException("The lengths of two arrays are not same!");
        }
        
        modifiedF0Values[0] = polyValues[0];
        modifiedF0Values[ modifiedF0Values.length - 1 ] = polyValues[ modifiedF0Values.length - 1 ];
        
        while ( it.hasNext() ) {
        
            String percent = it.next();
            String f0Value = f0Specifications.get( percent );
            
            int percentDuration  =  (new Integer(percent.substring(0, percent.length()-1))).intValue();
            
            //System.out.println( percent  + " " + f0Value );
            
            if ( f0Value.startsWith("+") ) {
                if ( f0Value.endsWith("%") ) {
                    double f0Mod = (new Double(f0Value.substring( 1, f0Value.length()-1 ))).doubleValue();
                    modifiedF0Values[percentDuration] = polyValues[percentDuration] + (polyValues[percentDuration] * (f0Mod / 100.0));
                }
                else if ( f0Value.endsWith("Hz") ) {
                    int f0Mod = (new Integer(f0Value.substring( 1, f0Value.length()-2 ))).intValue();
                    modifiedF0Values[percentDuration] = polyValues[percentDuration] + f0Mod ;
                }
            }
            else if ( f0Value.startsWith("-") ) {
                if ( f0Value.endsWith("%") ) {
                    double f0Mod = (new Double(f0Value.substring( 1, f0Value.length()-1 ))).doubleValue();
                    modifiedF0Values[percentDuration] = polyValues[percentDuration] - (polyValues[percentDuration] * (f0Mod / 100.0));
                
                }
                else if ( f0Value.endsWith("Hz") ) {
                    int f0Mod = (new Integer(f0Value.substring( 1, f0Value.length()-2 ))).intValue();
                    modifiedF0Values[percentDuration] = polyValues[percentDuration] - f0Mod ;
                }
            }        
        }
        
      modifiedF0Values = interpolateNonZeroValues(modifiedF0Values);
        
      return modifiedF0Values;
    
    }

    
    /**
     * set pitch specifications: 
     * Ex: pitch="+20%" or pitch="+50Hz"
     * @param polyValues
     * @param pitchAttribute
     * @return
     */
    private double[] setPitchSpecifications(double[] polyValues, String pitchAttribute) {
      
        boolean positivePitch = pitchAttribute.startsWith("+");
        double modificationPitch = (new Integer(pitchAttribute.substring(1, pitchAttribute.length()-1))).doubleValue();
        
        if ( pitchAttribute.startsWith("+") ) {
            if ( pitchAttribute.endsWith("%") ) {
                for ( int i=0; i < polyValues.length; i++ ) {
                    polyValues[i] = polyValues[i] + (polyValues[i] * (modificationPitch / 100.0));
                }
            }
            else if ( pitchAttribute.endsWith("Hz") ) {
                for ( int i=0; i < polyValues.length; i++ ) {
                    polyValues[i] = polyValues[i] + modificationPitch;
                }
            }
        }
        else if ( pitchAttribute.startsWith("-") ) {
            if ( pitchAttribute.endsWith("%") ) {
                for ( int i=0; i < polyValues.length; i++ ) {
                    polyValues[i] = polyValues[i] - (polyValues[i] * (modificationPitch / 100.0));
                }
            }
            else if ( pitchAttribute.endsWith("Hz") ) {
                for ( int i=0; i < polyValues.length; i++ ) {
                    polyValues[i] = polyValues[i] - modificationPitch;
                }
            }
        }
        
      return polyValues;
      
    }

    
    /**
     * to get contour specifications into MAP
     * @param attribute
     * @return
     */
    private Map<String, String> getContourSpecifications(String attribute) {
        
        Map<String, String> f0Map = new HashMap<String, String>();
        Pattern p = Pattern.compile("(\\d+%,[+|-]\\d+[%|Hz])");
        
        // Split input with the pattern
        Matcher m = p.matcher(attribute);
        while ( m.find() ) {
            //System.out.println(m.group());
            String[] f0Values = (m.group().trim()).split(",");
            f0Map.put(f0Values[0], f0Values[1]);
        }
        return f0Map;
    }

    /**
     * To interpolate Zero values with respect to NonZero values
     * @param contour
     * @return
     */
    private double[] interpolateNonZeroValues( double[] contour ) {
        
        for ( int i=0; i < contour.length ; i++ ) {
            if ( contour[i] == 0 ) {
                int index = findNextIndexNonZero( contour, i );
                //System.out.println("i: "+i+"index: "+index);
                if( index == -1 ) {
                    for ( int j=i; j < contour.length; j++ ) {
                        contour[j] = contour[j-1];
                    }
                    break;
                }
                else {
                    for ( int j=i; j < index; j++ ) {
                        //contour[j] = contour[i-1] * (index - j) + contour[index] * (j - (i-1)) / ( index - i );
                        if ( i == 0 ) {
                            contour[j] = contour[index];
                        }
                        else {
                            contour[j] = contour[j-1] + ((contour[index] - contour[i-1]) / (index - i)) ;
                        }
                    }
                    i = index-1; 
                }
            }
        }
        
        return contour;
    }
    
    /**
     * To find next NonZero index
     * @param contour
     * @param current
     * @return
     */
    private int findNextIndexNonZero(double[] contour, int current) {
        for ( int i=current+1; i < contour.length ; i++ ) {
            if ( contour[i] != 0 ) {
                return i;
            }
        }
       return -1;
    }

    
    /**
     * get Continuous contour from "ph" nodelist
     * @param nl
     * @return
     */
    private double[] getContinuousContour(NodeList nl) {
      

      Element firstElement =  (Element) nl.item(0);
      Element lastElement =  (Element) nl.item(nl.getLength()-1);
      
      double[] contour = new double[100]; // Assume contour has 100 frames
      Arrays.fill(contour, 0.0);
      
      double fEnd = (new Double(firstElement.getAttribute("end"))).doubleValue();
      double fDuration = 0.001 * (new Double(firstElement.getAttribute("d"))).doubleValue();
      double lEnd = (new Double(lastElement.getAttribute("end"))).doubleValue();
      double fStart = fEnd - fDuration; // 'prosody' tag starting point
      double duration = lEnd - fStart;  // duaration of 'prosody' modification request
      
      Map<Integer, Integer> f0Map;
      
      for ( int i=0; i < nl.getLength(); i++ ) {
          Element e = (Element) nl.item(i);
          String f0Attribute = e.getAttribute("f0");
          
          if( f0Attribute == null || "".equals(f0Attribute) ) {
              continue;
          }
          
          double phoneEndTime       = (new Double(e.getAttribute("end"))).doubleValue();
          double phoneDuration = 0.001 * (new Double(e.getAttribute("d"))).doubleValue();
          //double localStartTime = endTime - phoneDuration;
          
          f0Map = getPhoneF0Data(e.getAttribute("f0"));
          
          Iterator<Integer> it =  f0Map.keySet().iterator();
          while(it.hasNext()){
              Integer percent = it.next();
              Integer f0Value = f0Map.get(percent);
              double partPhone = phoneDuration * (percent.doubleValue()/100.0);
              int placeIndex  = (int) Math.floor((( ((phoneEndTime - phoneDuration) - fStart ) +  partPhone ) * 100 ) / (double) duration );
              if ( placeIndex >= 100 ) {
                  placeIndex = 99;
              }
              contour[placeIndex] = f0Value.doubleValue();
          }
      }
      
      return contour;
    }

    /**
     * Get f0 specifications in HashMap
     * @param attribute
     * @return
     */
    private Map<Integer, Integer> getPhoneF0Data(String attribute) {
      
      Map<Integer, Integer> f0Map = new HashMap<Integer, Integer>();
      Pattern p = Pattern.compile("(\\d+,\\d+)");
      
      // Split input with the pattern
      Matcher m = p.matcher(attribute);
      while ( m.find() ) {
          String[] f0Values = (m.group().trim()).split(",");
          f0Map.put(new Integer(f0Values[0]), new Integer(f0Values[1]));
      }

      //attribute.split(regex)
      return f0Map;
      
    }


}
