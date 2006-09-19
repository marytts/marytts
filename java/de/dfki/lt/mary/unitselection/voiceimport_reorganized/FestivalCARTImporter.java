
package de.dfki.lt.mary.unitselection.voiceimport_reorganized;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

import de.dfki.lt.mary.unitselection.cart.CARTWagonFormat;
import de.dfki.lt.mary.unitselection.cart.CART;

/**
 * Class for importing CARTs from Festival Text-Format to Mary Bin-Format
 * 
 * @author Anna Hunecke
 *
 */
public class FestivalCARTImporter {

    
    /**
     * Read in the CARTs from FreeTTS/trees.txt,
     * correct the instance numbers of the units
     * to the real indices, 
     * and dump the CARTs in destination/CARTS.bin
     * 
     * @param festvoxDirectory the festvox directory of a voice
     * @param destDir the destination directory
     * @param unitCatalog the unitCatalog for substituting the
     * 					  instance numbers for indices
     */
    public void importCARTS(String festvoxDirectory, 
        					String destDir, 
        					UnitCatalog unitCatalog){
        try{
        
            //open CART-File
            System.out.println("Reading CARTS");
            File treesDir = new File(festvoxDirectory + "/festival/trees");
     
            if (treesDir.isDirectory()){
                File[] entries = treesDir.listFiles();
                    Map cartMap = new HashMap();
                for (int i=0;i<entries.length;i++){
                    //get the name of the CART
                    
                    String name = entries[i].getName();
                    System.out.print(name);
                    name = name.substring(0,name.length()-3);
                    System.out.print(" "+name+"\n");
                    BufferedReader reader =
                        new BufferedReader(new 
                                InputStreamReader(new FileInputStream(entries[i])));
                    CART cart = new CARTWagonFormat(reader,name);
                    //store CART in map
                    cartMap.put(name, cart);
                    reader.close();
                    
                }
           
                //dump CARTS to binary file
                System.out.println("Dumping CARTS");
        
                //Open the destination file (CARTS.bin) and output the header
                DataOutputStream out = new DataOutputStream(new
                		BufferedOutputStream(new 
                        FileOutputStream(destDir+"CARTS.bin")));
                //create new CART-header and write it to output file
                MaryHeader hdr = new MaryHeader(MaryHeader.CARTS);
                hdr.write(out);
        
                //write number of CARTs
                out.writeInt(cartMap.size());
                Set carts = cartMap.keySet();
                //for each CART in the map,
                for (Iterator i = carts.iterator(); i.hasNext();) {
                    //get name and CART,
                    String name = (String) i.next();
                    CART cart =  (CART) cartMap.get(name);
                    //dump name and CART
                    out.writeUTF(name);
                    cart.dumpBinary(out);
                }
                //finish
                out.close();
                System.out.println("Done\n");
             } else {
                throw new Error(treesDir.getPath() + " is no directory!");
            }
        } catch (Exception e){
            e.printStackTrace();
            throw new Error("Error reading CARTS");
        }
    
    }     
}