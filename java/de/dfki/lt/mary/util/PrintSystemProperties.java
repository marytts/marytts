package de.dfki.lt.mary.util;

import java.util.Iterator;
import java.util.Properties;
import java.util.SortedSet;
import java.util.TreeSet;

public class PrintSystemProperties {

    /**
     * @param args
     */
    public static void main(String[] args) {
        // TODO Auto-generated method stub
        Properties p = System.getProperties();
        SortedSet keys = new TreeSet(p.keySet());
        for (Iterator it=keys.iterator(); it.hasNext(); ) {
            String key = (String) it.next();
            System.out.println(key + " = " + p.getProperty(key));
        }
    }

}
