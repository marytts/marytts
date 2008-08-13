/*
 * POSWords.java
 *
 * Created on 13 August, 2008, 6:50 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package marytts.tools.voiceimport.firstvoice;

/**
 * 
 * @author sathish
 */
public class POSWords {
    
    private String word;
    private String type;
    private boolean inspection;
    
    /** Creates a new instance of POSWords */
    public POSWords() {
    }
    
    String getWord(){
        return this.word;
    }
    
    void setWord(String word){
        this.word=word;
    }
    
    String getType(){
        return this.type;
    }
    
    void setType(String type){
        this.type=type;
    }
    
    boolean getInspection(){
        return this.inspection;
    }
    
    void setInspection(boolean inspection){
        this.inspection=inspection;
    }
}
