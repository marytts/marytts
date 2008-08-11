package marytts.htsengine;

public class MaryQuestion {
    
    
    private int questionFeaIndex;     /* question index name */
    private byte questionFeaValByte;  /* question index val */
    // Not used for the moment
    //private short questionFeaValShort;
    //private float questionFeaValFloat;
    
    public void setQuestionFeaIndex(int val){ questionFeaIndex = val; }
    public int getQuestionFeaIndex(){ return questionFeaIndex; }
    
    public void setQuestionFeaValByte(byte val){ questionFeaValByte = val; }
    public byte getQuestionFeaValByte(){ return questionFeaValByte; }
    
    // Not used for the moment.
    //public void setQuestionFeaValShort(short val){ questionFeaValShort = val; }
    //public short getQuestionFeaValShort(){ return questionFeaValShort; }
    //public void setQuestionFeaValFloat(byte val){ questionFeaValFloat = val; }
    //public float getQuestionFeaValFloat(){ return questionFeaValFloat; }
    
  

}
