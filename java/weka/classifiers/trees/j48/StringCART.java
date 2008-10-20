package weka.classifiers.trees.j48;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import marytts.cart.CART;
import marytts.cart.io.WagonCARTReader;
import marytts.cart.DecisionNode;
import marytts.cart.LeafNode;
import marytts.cart.Node;
import marytts.cart.DecisionNode.ByteDecisionNode;
import marytts.cart.LeafNode.IntAndFloatArrayLeafNode;
import marytts.cart.LeafNode.StringAndFloatLeafNode;
import marytts.features.FeatureDefinition;
import marytts.features.FeatureVector;
import marytts.unitselection.select.Target;

public class StringCART extends CART{
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
      // old: this.load(reader, aFeatDef);
      WagonCARTReader wagonReader = new WagonCARTReader(tf);
      this.setRootNode(wagonReader.load(reader, aFeatDef));
        
    }
    
    public FeatureDefinition getFeatureDefinition() {
        return this.featDef;
    }

    public int getTargetFeature() {
        return this.tf;
    }

 
    public String maxString(FeatureVector aFV){
        Target t = new Target(null,null,null);
        t.setFeatureVector(aFV);
        StringAndFloatLeafNode n = (StringAndFloatLeafNode) interpretToNode(t, 0);
        return n.maxString();
    }


}
