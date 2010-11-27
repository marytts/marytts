package marytts.vocalizations;

/**
 * Class represents a vocalization candidate
 * @author sathish
 */
public class VocalizationCandidate implements Comparable<VocalizationCandidate> {

    int unitIndex;
    double cost;
    
    public VocalizationCandidate(int unitIndex, double cost) {
        this.unitIndex = unitIndex;
        this.cost = cost;
    }
    
    public int compareTo(VocalizationCandidate other) {
        if (cost == other.cost) return 0;
        if (cost < other.cost) return -1;
        return 1;
    }
    
    public boolean equals(Object dc)
    {
        if (!(dc instanceof VocalizationCandidate)) return false;
        VocalizationCandidate other = (VocalizationCandidate) dc;
        if (cost == other.cost) return true;
        return false;
    }
    
    public String toString() {
        return unitIndex+" "+cost;
    }
    
}
