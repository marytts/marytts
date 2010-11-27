package marytts.vocalizations;

/**
 * Class represents Source unit, target unit and contour distance between these units.
 * Array of these pairs can sort with contour distance  
 * @author sathish
 *
 */
public class SourceTargetPair implements Comparable<SourceTargetPair> {

    private int targetUnitIndex;
    private int sourceUnitIndex;
    private double distance;
    
    public SourceTargetPair(int sourceUnitIndex, int targetUnitIndex, double distance) {
        this.sourceUnitIndex = sourceUnitIndex;
        this.targetUnitIndex = targetUnitIndex;
        this.distance = distance;
    }
    
    public int compareTo(SourceTargetPair other) {
        if (distance == other.distance) return 0;
        if (distance < other.distance) return -1;
        return 1;
    }
    
    public boolean equals(Object dc)
    {
        if (!(dc instanceof SourceTargetPair)) return false;
        SourceTargetPair other = (SourceTargetPair) dc;
        if (distance == other.distance) return true;
        return false;
    }
    
    public int getTargetUnitIndex() {
        return this.targetUnitIndex;
    }
    
    public int getSourceUnitIndex() {
        return this.sourceUnitIndex;
    }
}
