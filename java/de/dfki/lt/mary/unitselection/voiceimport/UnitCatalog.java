package de.dfki.lt.mary.unitselection.voiceimport;

import java.io.IOException;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;

import java.util.*;

/**
 * Reads in the units-catalog. The units are stored in a List according
 * to their order in the catalog. Null-Units are inserted into the List at
 * beginning and end of each file. Units are also stored according to
 * their type as described below.
 */
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
    private List unitsList;
    
    
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
        List nullUnits = new ArrayList();
        nullUnits.add(new Integer(0));
        put("null",nullUnits);
        Unit startUnit = new NullUnit("Start"); 
        currentFilename = null;
        previousUnit = startUnit;
        
        unitsList = new ArrayList(); 
        //store beginning null-unit
        unitsList.add(startUnit);
        currentIndex = 1;
        line = reader.readLine();
        while (line != null) {
            processLine(line);
            line = reader.readLine();
        }
        //store final null-unit
        nullUnits.add(new Integer(currentIndex));
        Unit finalUnit = new NullUnit("Final");
        previousUnit.next = finalUnit;
        unitsList.add(finalUnit);
        reader.close();
        //output info
        System.out.println("Number of units : "+unitsList.size()
                +",\nNumber of null-units : "+nullUnits.size());
    }

    /**
     * Processes a single line from the catalog.
     */
    void processLine(String line) {
        try{
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

        if (currentIndex == 1){
            currentFilename = filename;
        }        
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
        if (!(filename.equals(currentFilename)) && (currentIndex != 1)) {
            
            currentFilename = filename;
            //add two Null-Units
            Unit endUnit = new NullUnit("End");
            Unit beginUnit = new NullUnit("Begin");
            unitsList.add(endUnit);
            unitsList.add(beginUnit);
            previousUnit.next = endUnit;
            endUnit.next = beginUnit;
            previousUnit = beginUnit;
            List nullUnits = (List) get("null");
            nullUnits.add(new Integer(currentIndex));
            nullUnits.add(new Integer(currentIndex+1));
            currentIndex+= 2;
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
        //store unit in List, list position reflects index 
        unitsList.add(unit);
        //store index of unit under type specification
        units.add(0, new Integer(currentIndex));
        /* Save away this unit as the next unit of the previous unit.
         */
        previousUnit.next = unit;
        previousUnit = unit;    
        } catch (Exception e){
            e.printStackTrace();
            throw new Error("Error at index "+currentIndex
                    +", line "+line);
        }
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
            int index = ((Integer)units.get(unitNum)).intValue();
            return (Unit) unitsList.get(index);
        }
    }     
     
    
    /**
     * Returns the given Unit or null if no such Unit exists.
     *
     * @param unitType the unit type (e.g., "ae_afternoon")
     * @param unitNum the unit number
     */
    public int getUnitIndex(String unitType, int unitNum) {
        ArrayList units = (ArrayList) get(unitType);
        if ((units == null) || (units.size() < unitNum)){
            return -1;
        } else {
            return  ((Integer)units.get(unitNum)).intValue();
        }
    }    
    
    
    public List getUnits(){
        return unitsList;
    }
    
    public Unit getUnit(int index){
        return (Unit) unitsList.get(index);
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
