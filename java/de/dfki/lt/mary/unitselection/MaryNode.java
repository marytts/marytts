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
package de.dfki.lt.mary.unitselection;

import de.dfki.lt.mary.unitselection.featureprocessors.FeatureVector;

/**
 * A generic node class for the tree structures.
 * 
 * @author sacha
 *
 */
public class MaryNode {
    
    protected int featureIndex = -1;
    protected int from = 0;
    protected int to = 0;
    private MaryNode[] kids = null;
    
    /***************************/
    /* Constructor             */
    public MaryNode( int setFrom, int setTo ) {
        from = setFrom;
        to = setTo;
        kids = null;
    }
    
    /******************************/
    /* Getters for various fields */
    public int getFrom() {
        return( from );
    }
    
    public int getTo() {
        return( to );
    }
    
    public int getNumChildren() {
        return( kids.length );
    }
    
    public MaryNode[] getChildren() {
        return( kids );
    }
    
    /******************************/
    /* Feature index management   */
    public void setFeatureIndex( int i ) {
        featureIndex = i;
    }
    
    public int getFeatureIndex() {
        return( featureIndex );
    }
    
    /***************************/
    /* Node splitting          */
    public void split( int numKids ) {
        kids = new MaryNode[numKids];
    }
    
    public void setChild( int i, MaryNode n ) {
        kids[i] = n;
    }
    
    public MaryNode getChild( int i ) {
        return( kids[i] );
    }
    
    /*************************************/
    /* Check if this is a node or a leaf */
    public boolean isNode() {
        return( kids != null );
    }
    public boolean isLeaf() {
        return( kids == null );
    }
    
    //debug output
    public void toStandardOut(FeatureFileIndexer ffi, int level ){
        
        String blanks = "";
        for ( int i = 0; i < level; i++ ) blanks += "   ";
        
        if (kids != null){
            String featureName = ffi.getFeatureDefinition().getFeatureName(featureIndex);
            System.out.println( "Node "+ featureName + " has " + (to-from) + " units divided into " + kids.length + " branches." );
            for (int i=0;i<kids.length;i++){
                if ( kids[i] != null ) {
                    System.out.print( blanks + "Branch " + i + "/" + kids.length + " ( " + ffi.getFeatureDefinition().getFeatureName(featureIndex)
                            + " is " + ffi.getFeatureDefinition().getFeatureValueAsString(featureIndex,i) + " )" + " -> " );
                    kids[i].toStandardOut(ffi, level+1 );
                }
                else {
                    System.out.println( blanks + "Branch " + i + "/" + kids.length + " ( " + ffi.getFeatureDefinition().getFeatureName(featureIndex)
                            + " is " + ffi.getFeatureDefinition().getFeatureValueAsString(featureIndex,i) + " )" + " -> DEAD BRANCH (0 units)" );
                }
            }         
        } else {
            //get the unit indices
            FeatureVector[] fv = ffi.getFeatureVectors(from,to);
            System.out.print("LEAF has " + (to-from) + " units : " );
            for (int i=0;i<fv.length;i++){
                System.out.print( fv[i].getUnitIndex() + " " );
            }
            System.out.print("\n");
        }
        
    }
    
}
