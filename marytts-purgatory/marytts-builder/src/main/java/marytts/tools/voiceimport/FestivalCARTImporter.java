/**
 * Portions Copyright 2006 DFKI GmbH.
 * Portions Copyright 2001 Sun Microsystems, Inc.
 * Portions Copyright 1999-2001 Language Technologies Institute, 
 * Carnegie Mellon University.
 * All Rights Reserved.  Use is subject to license terms.
 * 
 * Permission is hereby granted, free of charge, to use and distribute
 * this software and its documentation without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of this work, and to
 * permit persons to whom this work is furnished to do so, subject to
 * the following conditions:
 * 
 * 1. The code must retain the above copyright notice, this list of
 *    conditions and the following disclaimer.
 * 2. Any modifications must be clearly marked as such.
 * 3. Original authors' names are not deleted.
 * 4. The authors' names are not used to endorse or promote products
 *    derived from this software without specific prior written
 *    permission.
 *
 * DFKI GMBH AND THE CONTRIBUTORS TO THIS WORK DISCLAIM ALL WARRANTIES WITH
 * REGARD TO THIS SOFTWARE, INCLUDING ALL IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS, IN NO EVENT SHALL DFKI GMBH NOR THE
 * CONTRIBUTORS BE LIABLE FOR ANY SPECIAL, INDIRECT OR CONSEQUENTIAL
 * DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR
 * PROFITS, WHETHER IN AN ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS
 * ACTION, ARISING OUT OF OR IN CONNECTION WITH THE USE OR PERFORMANCE OF
 * THIS SOFTWARE.
 */
package marytts.tools.voiceimport;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import marytts.cart.CART;
import marytts.cart.LeafNode.LeafType;
import marytts.cart.io.WagonCARTReader;
import marytts.cart.io.WagonCARTWriter;
import marytts.features.FeatureDefinition;
import marytts.util.data.MaryHeader;

/**
 * Class for importing CARTs from Festival Text-Format to Mary Bin-Format
 * 
 * @author Anna Hunecke
 * 
 */
public class FestivalCARTImporter {

	private Map cartMap;

	/**
	 * Read in the CARTs from festival/trees/ directory, and store them in a CARTMap
	 * 
	 * @param festvoxDirectory
	 *            the festvox directory of a voice
	 * @param destDir
	 *            destDir
	 * @param featDef
	 *            featDef
	 */
	public void importCARTS(String festvoxDirectory, String destDir, FeatureDefinition featDef) {
		try {

			// open CART-File
			System.out.println("Reading CARTS from " + festvoxDirectory);
			File treesDir = new File(festvoxDirectory + "/festival/trees/");

			if (treesDir.isDirectory()) {
				File[] entries = treesDir.listFiles();
				cartMap = new HashMap();
				for (int i = 0; i < entries.length; i++) {
					// get the name of the CART

					String name = entries[i].getName();
					System.out.print(name);
					name = name.substring(0, name.length() - 5);
					System.out.print(" " + name + "\n");
					BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(entries[i])));

					// old: CART cart = new ClassificationTree(reader, featDef);
					CART cart = new CART();
					WagonCARTReader wagonReader = new WagonCARTReader(LeafType.IntArrayLeafNode);
					cart.setRootNode(wagonReader.load(reader, featDef));

					// store CART in map
					cartMap.put(name, cart);
					reader.close();

				}
			} else {
				throw new Error(treesDir.getPath() + " is no directory!");
			}
		} catch (Exception e) {
			e.printStackTrace();
			throw new Error("Error reading CARTS");
		}
	}

	/**
	 * Dump the CARTs in the cart map to destinationDir/CARTS.bin
	 * 
	 * @param destDir
	 *            the destination directory
	 * @param featDef
	 *            featDef
	 */
	public void dumpCARTS(String destDir, FeatureDefinition featDef) {
		try {
			// dump CARTS to binary file
			System.out.println("Dumping CARTS to " + destDir + "/CARTs.bin");
			WagonCARTWriter ww = new WagonCARTWriter();

			// Open the destination file (CARTS.bin) and output the header
			DataOutputStream out = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(destDir + "/CARTS.bin")));
			// create new CART-header and write it to output file
			MaryHeader hdr = new MaryHeader(MaryHeader.CARTS);
			hdr.writeTo(out);

			// write number of CARTs
			out.writeInt(cartMap.size());
			Set carts = cartMap.keySet();
			// for each CART in the map,
			for (Iterator i = carts.iterator(); i.hasNext();) {
				// get name and CART,
				String name = (String) i.next();
				CART cart = (CART) cartMap.get(name);
				// dump name and CART
				out.writeUTF(name);
				// cart.dumpBinary(out); //old version
				ww.dumpBinary(cart, out);
				// cart.toTextOut(new PrintWriter(System.out)); //old version
				ww.toTextOut(cart, new PrintWriter(System.out));
			}
			// finish
			out.close();
			System.out.println("Done\n");
		} catch (IOException e) {
			e.printStackTrace();
			throw new Error("Error dumping CARTS");
		}
	}
}