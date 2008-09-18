package marytts.cart;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import marytts.cart.LeafNode.IntAndFloatArrayLeafNode;
import marytts.cart.LeafNode.StringAndFloatLeafNode;
import marytts.features.FeatureDefinition;
import marytts.features.FeatureVector;
import marytts.unitselection.select.Target;

public class StringCART extends WagonCART{
    private int tf;
    
    public StringCART(List<StringCART> treeList, int combFeat){
        this.featDef = treeList.get(0).getFeatureDefinition();
        this.tf = treeList.get(0).getTargetFeature();
        this.rootNode = new DecisionNode.ByteDecisionNode(combFeat,treeList.size(),featDef);
        
        for (StringCART st : treeList){
            if (st.getFeatureDefinition() != this.featDef || st.tf != this.getTargetFeature())
                throw new IllegalArgumentException("can only combine trees based on same feature definition and with same target.");
            
            ((DecisionNode.ByteDecisionNode) this.rootNode).addDaughter(st.getRootNode());
        }
    }
    
    public StringCART( Node aRootNode, FeatureDefinition aFeatDef, int targetFeatId ){
        if (! aRootNode.isRoot())
            throw new IllegalArgumentException("Tried to set a non-root-node as root of the tree. ");
        
        this.rootNode = aRootNode;
        this.featDef = aFeatDef;
    }

    public StringCART(BufferedReader reader, FeatureDefinition aFeatDef, int targetFeatId)
            throws IOException {
        
        this.tf = targetFeatId;

        // read the rest of the tree
        this.load(reader, aFeatDef);
    }
    
    public FeatureDefinition getFeatureDefinition() {
        return this.featDef;
    }

    public int getTargetFeature() {
        return this.tf;
    }

    protected LeafNode createLeafNode(String line) {
        StringTokenizer tok = new StringTokenizer(line, " ");
        // read the indices from the tokenized String
        int numTokens = tok.countTokens();
        int index = 0;
        
        List<Integer> indexList = new ArrayList<Integer>();
        List<Float> probList = new ArrayList<Float>();
                
        //System.out.println("Line: "+line+", numTokens: "+numTokens);
        
        if (numTokens == 2) { // we do not have any indices
            // discard useless token
            tok.nextToken();
        } else {
            
            while (index * 2 < numTokens - 1){
                String token = tok.nextToken();
                if (index == 0){
                    token = token.substring(4);
                }else{
                    token = token.substring(1);
                }
                //System.out.println("int-token: "+token);
                indexList.add((int) this.featDef.getFeatureValueAsShort(tf, token));//getFeatureIndex(token));
                    
                token = tok.nextToken();
                int lastIndex = token.length() - 1;
                if ((index*2) == (numTokens - 3)){
                    token = token.substring(0,lastIndex-1);
                    if (token.equals("inf")){
                        probList.add(100000f);
                        index++;
                        continue;
                    }
                    if (token.equals("nan")){
                        probList.add(-1f);
                        index++;
                        continue;
                    }
                }else{
                    token = token.substring(0,lastIndex);
                    if (token.equals("inf")){
                        probList.add(100000f);
                        index++;
                        continue;
                    }
                    if (token.equals("nan")){
                        probList.add(-1f);
                        index++;
                        continue;
                    }
                }
                //System.out.println("float-token: "+token);
                probList.add(Float.parseFloat(token));
                index++;    
            } // end while
       
        } // end if
        
        assert(indexList.size() == probList.size());
        
        // The data to be saved in the leaf node:
        int[] indices = new int[indexList.size()];
        // The floats to be saved in the leaf node:
        float[] probs = new float[probList.size()];
        
        
        for (int i = 0 ; i < indexList.size(); i++){
            indices[i] = indexList.get(i);
            probs[i]   = probList.get(i);
        }
        
        return new LeafNode.StringAndFloatLeafNode(indices,probs,this.featDef,this.tf);
    }

    public String maxString(FeatureVector aFV){
        Target t = new Target(null,null,null);
        t.setFeatureVector(aFV);
        StringAndFloatLeafNode n = (StringAndFloatLeafNode) interpretToNode(t, 0);
        return n.maxString();
    }


}
