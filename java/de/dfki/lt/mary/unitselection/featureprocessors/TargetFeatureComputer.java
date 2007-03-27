package de.dfki.lt.mary.unitselection.featureprocessors;

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import de.dfki.lt.mary.unitselection.Target;

/**
 * Compute a given set of features for a Target.
 * @author schroed
 *
 */
public class TargetFeatureComputer
{
    protected ByteValuedFeatureProcessor[] byteValuedDiscreteFeatureProcessors;
    protected ShortValuedFeatureProcessor[] shortValuedDiscreteFeatureProcessors;
    protected ContinuousFeatureProcessor[] continuousFeatureProcessors;
    
    /**
     * Construct a TargetFeatureComputer that knows how to compute features
     * for a Target using the given set of feature processor names. These names
     * must be known to the given Feature processor manager.
     * @param manager 
     * @param featureProcessorNames a String containing the names of the
     * feature processors to use, separated by white space, and in the 
     * right order (byte-valued discrete feature processors first, then
     * short-valued, then continuous)
     */
    public TargetFeatureComputer(FeatureProcessorManager manager, String featureProcessorNames)
    {
        List byteValuedFeatureProcessors = new ArrayList();
        List shortValuedFeatureProcessors = new ArrayList();
        List continuousValuedFeatureProcessors = new ArrayList();

        StringTokenizer st = new StringTokenizer(featureProcessorNames);
        while (st.hasMoreTokens()) {
            String name = st.nextToken();
            MaryFeatureProcessor fp = manager.getFeatureProcessor(name);
            if (fp == null) {
                throw new IllegalArgumentException("Unknown feature processor: "+name);
            } else if (fp instanceof ByteValuedFeatureProcessor) {
                byteValuedFeatureProcessors.add(fp);
            } else if (fp instanceof ShortValuedFeatureProcessor) {
                shortValuedFeatureProcessors.add(fp);
            } else if (fp instanceof ContinuousFeatureProcessor) {
                continuousValuedFeatureProcessors.add(fp);
            } else {
                throw new IllegalArgumentException("Unknown feature processor type "+fp.getClass()+" for feature processor: "+name);
            }
        }
        this.byteValuedDiscreteFeatureProcessors = (ByteValuedFeatureProcessor[])byteValuedFeatureProcessors.toArray(new ByteValuedFeatureProcessor[0]);
        this.shortValuedDiscreteFeatureProcessors = (ShortValuedFeatureProcessor[])shortValuedFeatureProcessors.toArray(new ShortValuedFeatureProcessor[0]);
        this.continuousFeatureProcessors = (ContinuousFeatureProcessor[])continuousValuedFeatureProcessors.toArray(new ContinuousFeatureProcessor[0]);
    }
    
    /**
     * Using the set of feature processors defined when creating the target feature computer,
     * compute a feature vector for the target
     * @param target 
     * @return a feature vector for the target
     */
    public FeatureVector computeFeatureVector(Target target)
    {
        byte[] byteFeatures = new byte[byteValuedDiscreteFeatureProcessors.length];
        short[] shortFeatures = new short[shortValuedDiscreteFeatureProcessors.length];
        float[] floatFeatures = new float[continuousFeatureProcessors.length];
        for (int i=0; i<byteValuedDiscreteFeatureProcessors.length; i++) {
            byteFeatures[i] = byteValuedDiscreteFeatureProcessors[i].process(target);
        }
        for (int i=0; i<shortValuedDiscreteFeatureProcessors.length; i++) {
            shortFeatures[i] = shortValuedDiscreteFeatureProcessors[i].process(target);
        }
        for (int i=0; i<continuousFeatureProcessors.length; i++) {
            floatFeatures[i] = continuousFeatureProcessors[i].process(target);
        }
        return new FeatureVector(byteFeatures, shortFeatures, floatFeatures, 0);
    }

    /**
     * For the given feature vector, convert each encoded value into its string representation.
     * @param features a feature vector, which must match the feature processors known to this feature computer.
     * @return a string in which the string values of all features are separated by spaces.
     * @throws IllegalArgumentException if the number of byte-valued, short-valued or continuous elements in features 
     * do not match the set of feature processors in this feature computer.
     */
    public String toStringValues(FeatureVector features)
    {
        StringBuffer buf = new StringBuffer();
        byte[] bytes = features.getByteValuedDiscreteFeatures();
        short[] shorts = features.getShortValuedDiscreteFeatures();
        float[] floats = features.getContinuousFeatures();
        if (bytes.length != byteValuedDiscreteFeatureProcessors.length
                || shorts.length != shortValuedDiscreteFeatureProcessors.length
                || floats.length != continuousFeatureProcessors.length) {
            throw new IllegalArgumentException("Number of features in argument does not match number of feature processors");
        }
        for (int i=0; i<bytes.length; i++) {
            if (buf.length() > 0) buf.append(" ");
            buf.append(byteValuedDiscreteFeatureProcessors[i].getValues()[(int)bytes[i]]);
        }
        for (int i=0; i<shorts.length; i++) {
            if (buf.length() > 0) buf.append(" ");
            buf.append(shortValuedDiscreteFeatureProcessors[i].getValues()[(int)shorts[i]]);
        }
        for (int i=0; i<floats.length; i++) {
            if (buf.length() > 0) buf.append(" ");
            buf.append(floats[i]);
        }
        return buf.toString();
    }
    
    public ByteValuedFeatureProcessor[] getByteValuedFeatureProcessors()
    {
        return byteValuedDiscreteFeatureProcessors;
    }
    
    public ShortValuedFeatureProcessor[] getShortValuedFeatureProcessors()
    {
        return shortValuedDiscreteFeatureProcessors;
    }
    
    public ContinuousFeatureProcessor[] getContinuousFeatureProcessors()
    {
        return continuousFeatureProcessors;
    }
    
    /**
     * List the names of all feature processors.
     * The first line starts with "ByteValuedFeatureProcessors", followed by the list of names of the byte-valued feature processors;
     * the second line starts with "ShortValuedFeatureProcessors", followed by the list of names of the short-valued feature processors;
     * and the third line starts with "ContinuousFeatureProcessors", followed by the list of names of the continuous feature processors.
     * @return a string with the names.
     */
    public String getAllFeatureProcessorNames()
    {
        return "ByteValuedFeatureProcessors " + getByteValuedFeatureProcessorNames() + "\n"
            + "ShortValuedFeatureProcessors " + getShortValuedFeatureProcessorNames() + "\n"
            + "ContinuousFeatureProcessors " + getContinuousFeatureProcessorNames() + "\n";
    }
    
    /**
     * List the names of all byte-valued feature processors, separated by space characters.
     * @return a string with the names.
     */
    public String getByteValuedFeatureProcessorNames()
    {
        StringBuffer buf = new StringBuffer();
        for (int i=0; i < byteValuedDiscreteFeatureProcessors.length; i++) {
            if (i>0) buf.append(" ");
            buf.append(byteValuedDiscreteFeatureProcessors[i].getName());
        }
        return buf.toString();
    }

    /**
     * List the names of all short-valued feature processors, separated by space characters.
     * @return a string with the names.
     */
    public String getShortValuedFeatureProcessorNames()
    {
        StringBuffer buf = new StringBuffer();
        for (int i=0; i < shortValuedDiscreteFeatureProcessors.length; i++) {
            if (i>0) buf.append(" ");
            buf.append(shortValuedDiscreteFeatureProcessors[i].getName());
        }
        return buf.toString();
    }

    /**
     * List the names of all byte-valued feature processors, separated by space characters.
     * @return a string with the names.
     */
    public String getContinuousFeatureProcessorNames()
    {
        StringBuffer buf = new StringBuffer();
        for (int i=0; i < continuousFeatureProcessors.length; i++) {
            if (i>0) buf.append(" ");
            buf.append(continuousFeatureProcessors[i].getName());
        }
        return buf.toString();
    }
    
    /**
     * List the names and values of all feature processors.
     * The section describing the byte-valued feature processors starts with
     * the string "ByteValuedFeatureProcessors" in a line by itself, followed 
     * by the list of names and values of the byte-valued feature processors,
     * as described in getByteValuedFeatureProcessorNamesAndValues().
     * The section describing the short-valued feature processors starts with
     * the string "ShortValuedFeatureProcessors" in a line by itself, followed 
     * by the list of names and values of the short-valued feature processors,
     * as described in getShortValuedFeatureProcessorNamesAndValues().
     * The section describing the continuous feature processors starts with
     * the string "ContinuousFeatureProcessors" in a line by itself, followed 
     * by the list of names and values of the continuous feature processors,
     * as described in getContinuousFeatureProcessorNamesAndValues().
     * @return a string with the names and values.
     */
    public String getAllFeatureProcessorNamesAndValues()
    {
        return "ByteValuedFeatureProcessors\n" + getByteValuedFeatureProcessorNamesAndValues()
            + "ShortValuedFeatureProcessors\n" + getShortValuedFeatureProcessorNamesAndValues()
            + "ContinuousFeatureProcessors\n" + getContinuousFeatureProcessorNamesAndValues();
    }

    /**
     * List the names of all byte-valued feature processors and their possible values.
     * Each line starts with the name of a feature processor, followed by the 
     * full list of string values, separated by space characters. 
     * The values are ordered so that their position corresponds to the byte value.
     * @return a string with the names and values.
     */
    public String getByteValuedFeatureProcessorNamesAndValues()
    {
        StringBuffer buf = new StringBuffer();
        for (int i=0; i < byteValuedDiscreteFeatureProcessors.length; i++) {
            buf.append(byteValuedDiscreteFeatureProcessors[i].getName());
            String[] values = byteValuedDiscreteFeatureProcessors[i].getValues();
            for (int j=0; j<values.length; j++) {
                buf.append(" ");
                buf.append(values[j]);
            }
            buf.append("\n");
        }
        return buf.toString();
    }

    /**
     * List the names of all short-valued feature processors and their possible values.
     * Each line starts with the name of a feature processor, followed by the 
     * full list of string values, separated by space characters. 
     * The values are ordered so that their position corresponds to the short value.
     * @return a string with the names and values.
     */
    public String getShortValuedFeatureProcessorNamesAndValues()
    {
        StringBuffer buf = new StringBuffer();
        for (int i=0; i < shortValuedDiscreteFeatureProcessors.length; i++) {
            buf.append(shortValuedDiscreteFeatureProcessors[i].getName());
            String[] values = shortValuedDiscreteFeatureProcessors[i].getValues();
            for (int j=0; j<values.length; j++) {
                buf.append(" ");
                buf.append(values[j]);
            }
            buf.append("\n");
        }
        return buf.toString();
    }

    /**
     * List the names of all continuous feature processors and their possible values.
     * Each line starts with the name of a feature processor, followed by the value "float"
     * to indicate that the list of possible values is not limited.
     * @return a string with the names and values.
     */
    public String getContinuousFeatureProcessorNamesAndValues()
    {
        StringBuffer buf = new StringBuffer();
        for (int i=0; i < continuousFeatureProcessors.length; i++) {
            buf.append(continuousFeatureProcessors[i].getName());
            buf.append(" float\n");
        }
        return buf.toString();
    }

}
