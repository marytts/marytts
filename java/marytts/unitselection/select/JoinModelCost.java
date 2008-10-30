/**
 * Copyright 2006 DFKI GmbH.
 * All Rights Reserved.  Use is subject to license terms.
 * 
 * Permission is hereby granted, free of charge, to use and distribute
 * this software and its documentation without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of this work, and to
 * permit persons to whom this work is furnished to do so, subject to
 * the following conditions:
 * 
 * 1. The code must retain the above copyright notice, this list of
 *    conditions and the following disclaimer.
 * 2. Any modifications must be clearly marked as such.
 * 3. Original authors' names are not deleted.
 * 4. The authors' names are not used to endorse or promote products
 *    derived from this software without specific prior written
 *    permission.
 *
 * DFKI GMBH AND THE CONTRIBUTORS TO THIS WORK DISCLAIM ALL WARRANTIES WITH
 * REGARD TO THIS SOFTWARE, INCLUDING ALL IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS, IN NO EVENT SHALL DFKI GMBH NOR THE
 * CONTRIBUTORS BE LIABLE FOR ANY SPECIAL, INDIRECT OR CONSEQUENTIAL
 * DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR
 * PROFITS, WHETHER IN AN ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS
 * ACTION, ARISING OUT OF OR IN CONNECTION WITH THE USE OR PERFORMANCE OF
 * THIS SOFTWARE.
 */
package marytts.unitselection.select;

import java.io.IOException;

import marytts.features.FeatureDefinition;
import marytts.features.FeatureVector;
import marytts.cart.CART;
import marytts.cart.Node;
import marytts.cart.LeafNode.PdfLeafNode;
import marytts.cart.io.HTSCARTReader;
import marytts.server.MaryProperties;
import marytts.signalproc.analysis.distance.DistanceComputer;
import marytts.unitselection.data.DiphoneUnit;
import marytts.unitselection.data.Unit;


public class JoinModelCost implements JoinCostFunction
{
    protected int nCostComputations = 0;
    
    /****************/
    /* DATA FIELDS  */
    /****************/
    private JoinCostFeatures jcf = null;
    
    CART[] joinTree = null;  // an array of carts, one per HMM state.
    
    private float f0Weight;
    
    private FeatureDefinition featureDef = null;
    
    private boolean debugShowCostGraph = false;
   
    
    /****************/
    /* CONSTRUCTORS */
    /****************/

    /**
     * Empty constructor; when using this, call load() separately to 
     * initialise this class.
     * @see #load(String)
     */
    public JoinModelCost()
    {
    }
    
    /**
     * Initialise this join cost function by reading the appropriate settings
     * from the MaryProperties using the given configPrefix.
     * @param configPrefix the prefix for the (voice-specific) config entries
     * to use when looking up files to load.
     */
    public void init(String configPrefix) throws IOException
    {
        String joinFileName = MaryProperties.needFilename(configPrefix+".joinCostFile");
        String joinPdfFileName = MaryProperties.needFilename(configPrefix + ".joinPdfFile");
        String joinTreeFileName = MaryProperties.needFilename(configPrefix + ".joinTreeFile");
        load(joinFileName, joinPdfFileName, joinTreeFileName);
    }
    
    @Deprecated
    public void load(String a, String b, String c, float d)
    {
        throw new RuntimeException("Do not use load() -- use init()");
    }
    
    
   /**
     * Load weights and values from the given file
     * @param joinFileName the file from which to read join cost features
     * @param joinPdfFileName the file from which to read the Gaussian models in the leaves of the tree
     * @param joinTreeFileName the file from which to read the Tree, in HTS format.
     */
    public void load(String joinFileName, String joinPdfFileName, String joinTreeFileName)
    throws IOException
    {
        jcf = new JoinCostFeatures(joinFileName);

        assert featureDef != null : "Expected to have a feature definition, but it is null!";
      
        /* Load Trees */
        HTSCARTReader htsReader = new HTSCARTReader();
        int numStates = 1;  // just one state in the joinModeller
        
        try {
            //joinTree.loadTreeSetGeneral(joinTreeFileName, 0, featureDef);
            joinTree = htsReader.load(numStates, joinTreeFileName, joinPdfFileName, featureDef);
            
        } catch (Exception e) {
            IOException ioe = new IOException("Cannot load join model trees from "+joinTreeFileName);
            ioe.initCause(e);
            throw ioe;
        }
                

    }
    
    /**
     * Set the feature definition to use for interpreting target feature vectors.
     * @param def the feature definition to use.
     */
    public void setFeatureDefinition(FeatureDefinition def)
    {
        this.featureDef = def;
    }
    
    /*****************/
    /* MISC METHODS  */
    /*****************/

    /**
     * A combined cost computation, as a weighted sum
     * of the signal-based cost (computed from the units)
     * and the phonetics-based cost (computed from the targets).
     * 
     * @param t1 The left target.
     * @param u1 The left unit.
     * @param t2 The right target.
     * @param u2 The right unit.
     * 
     * @return the cost of joining the left unit with the right unit, as a non-negative value.
     */
    public double cost(Target t1, Unit u1, Target t2, Unit u2 ) {
        // Units of length 0 cannot be joined:
        if (u1.getDuration() == 0 || u2.getDuration() == 0) return Double.POSITIVE_INFINITY;
        // In the case of diphones, replace them with the relevant part:
        if (u1 instanceof DiphoneUnit) {
            u1 = ((DiphoneUnit)u1).getRight();
        }
        if (u2 instanceof DiphoneUnit) {
            u2 = ((DiphoneUnit)u2).getLeft();
        }
        
        if (u1.getIndex()+1 == u2.getIndex()) return 0;
        double cost = 1; // basic penalty for joins of non-contiguous units. 
        
        float[] v1 = jcf.getRightJCF(u1.getIndex());
        float[] v2 = jcf.getLeftJCF(u2.getIndex());
        //double[] diff = new double[v1.length];
        //for ( int i = 0; i < v1.length; i++ ) {
        //    diff[i] = (double)v1[i] - v2[i];
        //}
        double[] diff = new double[v1.length];
        for ( int i = 0; i < v1.length; i++ ) {
            diff[i] = (double)v1[i] - v2[i];
        }
                
        // Now evaluate likelihood of the diff under the join model
        // Compute the model name:
        assert featureDef != null : "Feature Definition was not set";
        FeatureVector fv1 = null;
        if (t1 instanceof DiphoneTarget) {
            HalfPhoneTarget hpt1 = ((DiphoneTarget)t1).getRight();
            assert hpt1 != null;
            fv1 = hpt1.getFeatureVector();
        } else {
            fv1 = t1.getFeatureVector();
        }
        assert fv1 != null : "Target has no feature vector";
        //String modelName = contextTranslator.features2context(featureDef, fv1, featureList);
       
        int state = 0;  // just one state in the joinModeller
        double[] mean;
        double[] variance;
        
        Node node = joinTree[state].interpretToNode(fv1, 1);
        
        assert node instanceof PdfLeafNode : "The node must be a PdfLeafNode.";
        
        mean = ((PdfLeafNode)node).getMean();
        variance = ((PdfLeafNode)node).getVariance();
        
        double distance = DistanceComputer.getNormalizedEuclideanDistance(diff, mean, variance);
        
        cost += distance;

        return cost;
    }
    
    
    
    
    
    

}
