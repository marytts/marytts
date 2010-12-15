/**
 * Simple Unit entry for the Catalog.
 */
public class Unit {
    public String unitType;
    public int unitNum;
    public String filename;
    public float start;
    public float middle;
    public float end;
    public Unit previous;
    public Unit next;
    public int index;
    
    /**
     * Creates a new Unit entry for the catalog.
     *
     * @param unitType the type of this unit
     * @param unitNum the index of this unit
     * @param filename (without extension) where the audio and STS
     * data for this unit can be found
     * @param start the timing info (in seconds) for where the audio
     * and STS data for this unit starts in filename
     * @param middle the timing info (in seconds) for where the middle
     * of the audio and STS data for this unit is in filename
     * @param end the timing info (in seconds) for where the audio
     * and STS data for this unit ends in filename
     * @param previous the unit preceding this one in the recorded
     * utterance
     * @param next the unit following this one in the recorded
     * utterance
     * @param index the index of this unit in the overall catalog
     */
    public Unit(
        String unitType,
        int unitNum,
        String filename,
        float start,
        float middle,
        float end,
        Unit previous,
        Unit next,
        int index) {

        this.unitType = unitType;
        this.unitNum = unitNum;
        this.filename = filename;
        this.start = start;
        this.middle = middle;
        this.end = end;
        this.previous = previous;
        this.next = next;
        this.index = index;
    }

    public String toString() {
        StringBuffer buf = new StringBuffer(filename + " ");
        if (previous != null) {
            buf.append(previous.unitType + "_" + previous.unitNum + " ");
        } else {
            buf.append("CLUNIT_NONE ");
        }
        buf.append(unitType + "_" + unitNum);
        if (next != null) {
            buf.append(" " + next.unitType + "_" + next.unitNum);
        } else {
            buf.append(" CLUNIT_NONE");
        }
        buf.append(" (index=" + index + ")");
        
        return buf.toString();
    }
}
