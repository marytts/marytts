
package de.dfki.lt.mary.unitselection.voiceimport_reorganized;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.DataOutputStream;
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

import de.dfki.lt.mary.unitselection.cart.CART;
import de.dfki.lt.mary.unitselection.cart.CARTImpl;

/**
 * Class for importing CARTs from FreeTTS Text-Format to Mary Bin-Format
 * 
 * @author Anna Hunecke
 *
 */
public class CARTImporter {

    
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
            URL fileURL = new URL("file:" + festvoxDirectory + "/FreeTTS/trees.txt");
            BufferedReader reader =
                new BufferedReader(new 
                        InputStreamReader(new FileInputStream(fileURL.getFile())));
            //read CARTs into a map (CART-name -> CART)
            Map cartMap = new HashMap();
            String line = reader.readLine();
            //for each line, 
            while (line != null){
                //tokenize line, discard first token
                StringTokenizer tokenizer = new StringTokenizer(line);
                tokenizer.nextToken();
                //next tokens are CART name and number of nodes, 
                String name = tokenizer.nextToken();
                int nodes = Integer.parseInt(tokenizer.nextToken());
                //construct new CART,
                CART cart = new CARTImpl(reader, nodes);
                //store CART in map
                cartMap.put(name, cart);
                line = reader.readLine();
            }
            reader.close();
        
            //correct instance numbers to index numbers
            System.out.println("Correcting units indices in CARTS");
            Set carts = cartMap.keySet();
            //for each cart in the map,
            for (Iterator it = carts.iterator(); it.hasNext();){
                String nextCart = (String) it.next();
                //get the units
                List units = (List)unitCatalog.get(nextCart);
                //correct the indices
                ((CART)cartMap.get(nextCart)).correctNumbers(units);
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
        
        } catch (Exception e){
            e.printStackTrace();
            throw new Error("Error reading CARTS");
        }
    
    }     
}