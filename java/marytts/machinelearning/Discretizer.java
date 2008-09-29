package marytts.machinelearning;

public interface Discretizer {
    
    public int discretize(int aValue);
    
    public int[] getPossibleValues();

}
