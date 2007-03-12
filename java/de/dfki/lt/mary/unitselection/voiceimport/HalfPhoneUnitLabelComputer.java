package de.dfki.lt.mary.unitselection.voiceimport;

import java.io.File;
import java.io.IOException;
import java.util.StringTokenizer;

/**
 * Compute unit labels from phone labels.
 * @author schroed
 *
 */
public class HalfPhoneUnitLabelComputer extends UnitLabelComputer
{
    
    /**/
    public HalfPhoneUnitLabelComputer( DatabaseLayout setdb, BasenameList setbnl ) throws IOException
    {
        super(setdb, setbnl);
    }
    
    /**
     * Set some global variables that subclasses may want to override.
     *
     */
    protected void init()
    {
        unitlabelDir = new File( db.halfphoneUnitLabDirName() );
        if (!unitlabelDir.exists()) unitlabelDir.mkdir();
        unitlabelExt = db.halfphoneUnitLabExt();
    }
    
    
    protected String[] toUnitLabels(String[] phoneLabels)
    {
        // We will create exactly two half phones for every phone:
        String[] halfPhoneLabels = new String[2*phoneLabels.length];
        float startTime = 0;
        int unitIndex = 0;
        for (int i=0; i<phoneLabels.length; i++) {
            unitIndex++;
            StringTokenizer st = new StringTokenizer(phoneLabels[i]);
            String endTimeString = st.nextToken();
            String dummyNumber = st.nextToken();
            String phone = st.nextToken();
            assert !st.hasMoreTokens();
            float endTime = Float.parseFloat(endTimeString);
            float duration = endTime - startTime;
            assert duration > 0 : "Duration is not > 0 for phone "+i+" ("+phone+")";
            float midTime = startTime + duration/2;
            String leftUnitLine = midTime + " " + unitIndex + " " + phone + "_L";
            unitIndex++;
            String rightUnitLine = endTime + " " + unitIndex + " " + phone + "_R";
            halfPhoneLabels[2*i] = leftUnitLine;
            halfPhoneLabels[2*i+1] = rightUnitLine;
            startTime = endTime;
        }
        return halfPhoneLabels;
    }
}
