import java.io.IOException;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.IOException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.StringTokenizer;
import java.util.TreeSet;

/**
 * Reads in a festival clunits unit catalog.  The units are stored
 * as a HashMap of ArrayLists where the HashMap is indexed by unit
 * type (e.g., "ae_afternoon"), and the ArrayList is a list of the
 * actual units themselves (e.g., "ae_afternoon_23").
 *�   
 * Assumption: unit indeces are in reverse order.
 */
public class UnitCatalog extends HashMap {
    /* Helpers for handling the previous and next units for each
     * unit being read in.
     */
    private String currentFilename;
    private Unit previousUnit;
    private int currentIndex;
    
    /**
     * Creates a new UnitCatalog from the festival unit catalog file.
     *
     * @param filename the festival unit catalog file
     */
    public UnitCatalog(String filename) throws IOException {
        super();

        BufferedReader reader =
            new BufferedReader(
                new InputStreamReader(
                    new FileInputStream(filename)));

        /* Skip past the preamble.
         */
        String line = reader.readLine();
        while (!line.equals("EST_Header_End")) {
            line = reader.readLine();
        }

        /* Process each of the lines that have "real" content.
         */
        currentFilename = null;
        previousUnit = null;
        currentIndex = 0;
        line = reader.readLine();
        while (line != null) {
            processLine(line);
            line = reader.readLine();
        }

        reader.close();
    }

    /**
     * Processes a single line from the catalog.
     */
    void processLine(String line) {
        StringTokenizer tokenizer = new StringTokenizer(line);

        /* The whole unit, including its instance (e.g.,
         * "ae_afternoon_25")
         */
        String unitName = tokenizer.nextToken();

        /* Split the unit into type ("ae_afternoon") and instance
         * number (25).
         */
        String unitType = unitName.substring(0, unitName.lastIndexOf("_"));
        int unitNum = Integer.parseInt(
            unitName.substring(unitName.lastIndexOf("_") + 1));

        /* Get the remaining parameters.
         */
        String filename = tokenizer.nextToken();
        float start = Float.parseFloat(tokenizer.nextToken());
        float middle = Float.parseFloat(tokenizer.nextToken());
        float end = Float.parseFloat(tokenizer.nextToken());

        /* Create a new ArrayList for this unit type if it
         * is not in the catalog yet.
         */
        ArrayList units = (ArrayList) get(unitType);
        if (units == null) {
            units = new ArrayList();
            put(unitType, units);
        }

        /**
         * Make adjustments for previous and next units.
         */
        if (!filename.equals(currentFilename)) {
            previousUnit = null;
            currentFilename = filename;
        }
            
        /* The units come in reverse order, so always
         * add the unit to the begining of the entries.
         */
        Unit unit = new Unit(unitType,
                             unitNum,
                             filename,
                             start,
                             middle,
                             end,
                             previousUnit,
                             null,
                             currentIndex++);
        units.add(0, unit);

        /* Save away this unit as the next unit of the previous unit.
         */
        if (previousUnit != null) {
            previousUnit.next = unit;
        }
        previousUnit = unit;    
    }

    /**
     * Performs minimal checking on the catalog to make sure it was
     * read in correctly.
     */
    void checkCatalog() {
        /* Just make sure the unit numbers match the indexes in the
         * ArrayList.
         */
        Iterator keys = keySet().iterator();
        while (keys.hasNext()) {
            String key = (String) keys.next();
            ArrayList units = (ArrayList) get(key);
            for (int i = 0; i < units.size(); i++) {
                Unit unit = (Unit) units.get(i);
                if (unit.unitNum != i) {
                    System.out.println(key + " check failed: expected "
                                       + i + ", got " + unit.unitNum);
                }
            }
        }
    }    

    /**
     * Returns the given Unit or null if no such Unit exists.
     *
     * @param unitType the unit type (e.g., "ae_afternoon")
     * @param unitNum the unit number
     */
    public Unit getUnit(String unitType, int unitNum) {
        ArrayList units = (ArrayList) get(unitType);
        if ((units == null) || (units.size() < unitNum)){
            return null;
        } else {
            return (Unit) units.get(unitNum);
        }
    }     
     
    /**
     * For testing.  args[0] = festival/clunits/*.catalogue.
     */
    public static void main(String[] args) {        
        try {
            UnitCatalog catalog = new UnitCatalog(args[0]);
            catalog.checkCatalog();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
