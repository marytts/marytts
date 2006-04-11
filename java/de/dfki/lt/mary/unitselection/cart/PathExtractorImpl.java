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
package de.dfki.lt.mary.unitselection.cart;

import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.StringTokenizer;

import org.apache.log4j.Logger;

import de.dfki.lt.mary.unitselection.featureprocessors.UtteranceFeatProcManager;

import com.sun.speech.freetts.util.Utilities;
import com.sun.speech.freetts.Item;
import com.sun.speech.freetts.FeatureProcessor;
import com.sun.speech.freetts.ProcessException;


/**
 * Interface that Manages a feature or item path. Allows navigation
 * to the corresponding feature or item.
 * This class in controlled by the following system properties:
 *
 * <pre>
 *   com.sun.speech.freetts.interpretCartPaths - default false
 *   com.sun.speech.freetts.lazyCartCompile - default true
 * </pre>
 *   com.sun.speech.freetts.interpretCartPaths
 *
 * Instances of this class will optionally pre-compile the paths.
 * Pre-compiling paths reduces the processing time and objects needed
 * to extract a feature or an item based upon a path.
 */
public class PathExtractorImpl implements PathExtractor {
    private UtteranceFeatProcManager featureProcessors;
    private Logger logger;
    /**
      * If this system property is set to true, paths will
      * not be compiled.
      */
    public final static String INTERPRET_PATHS_PROPERTY =
	"com.sun.speech.freetts.interpretCartPaths";

    /**
     * If this system property is set to true, CART feature/item
     * paths will only be compiled as needed.
     */
    public final static String LAZY_COMPILE_PROPERTY =
	"com.sun.speech.freetts.lazyCartCompile";

    private final static boolean INTERPRET_PATHS = 
	Utilities.getProperty(INTERPRET_PATHS_PROPERTY, "false").equals("true");
    private final static boolean LAZY_COMPILE  = 
	Utilities.getProperty(LAZY_COMPILE_PROPERTY, "true").equals("true");

    private String pathAndFeature;
    private String path;
    private String feature;
    private Object[] compiledPath;
    private boolean wantFeature = false;

    /**
     * Creates a path for the given feature.
     */
    public PathExtractorImpl(String pathAndFeature, boolean wantFeature) {
        this.pathAndFeature = pathAndFeature;
        logger = Logger.getLogger("PathExtractorImpl");
        if (INTERPRET_PATHS)  {
            path = pathAndFeature;
	    return;
	}
    
	if (wantFeature) {
	    int lastDot = pathAndFeature.lastIndexOf(".");
	    // string can be of the form "p.feature" or just "feature"

	    if (lastDot == -1) {
		feature = pathAndFeature;
		path = null;
	    } else {
		feature = pathAndFeature.substring(lastDot + 1);
		path = pathAndFeature.substring(0, lastDot);
	    }
	    this.wantFeature = wantFeature;
	} else {
	    this.path = pathAndFeature;
	}

	if (!LAZY_COMPILE) {
	    compiledPath = compile(path);
	}
    }

    public void setFeatureProcessors(UtteranceFeatProcManager fp){
        featureProcessors = fp;
    }
    
    /**
     * Finds the target item associated with this Path.
     * @param object the object(=item) to start at
     * @return the object(=item) associated with the path or null
     */
    public Object findTarget(Object object) {

        Item item = (Item) object;
        if (INTERPRET_PATHS) {
            return item.findItem(path);
        }

        if (compiledPath == null) {
            compiledPath = compile(path);
        }

        Item pitem = item;

        for (int i = 0; pitem != null && i < compiledPath.length; ) {
            OpEnum op = (OpEnum) compiledPath[i++];
            if (op == OpEnum.NEXT) {
                pitem = pitem.getNext();
            } else if (op == OpEnum.PREV) {
                pitem = pitem.getPrevious();
            } else if (op == OpEnum.NEXT_NEXT) {
                pitem = pitem.getNext();
                if (pitem != null) {
                    pitem = pitem.getNext();
                }
            } else if (op == OpEnum.PREV_PREV) {
                pitem = pitem.getPrevious();
                if (pitem != null) {
                    pitem = pitem.getPrevious();
                }
            } else if (op == OpEnum.PARENT) {
            	    pitem = pitem.getParent();
            	} else if (op == OpEnum.DAUGHTER) {
            	    	pitem = pitem.getDaughter();
            		} else if (op == OpEnum.LAST_DAUGHTER) {
            		    pitem = pitem.getLastDaughter();
            			} else if (op == OpEnum.RELATION) {
            			    String relationName = (String) compiledPath[i++];
            			    pitem = pitem.getSharedContents().getItemRelation(relationName);
            				} else {
            				    System.out.println("findItem: bad feature " + op +
            				            			" in " + path);
	    }
	}
	return pitem;
    }


    /**
     * Finds the feature associated with this Path.
     * @param object the object(=item) to start at
     * @return the feature associated or null  if the
     * feature was not found.
     */
    public Object findFeature(Object object) {
        Item item = (Item) object;

        if (INTERPRET_PATHS) {
            return item.findFeature(path);
        }

        Item pitem = (Item) findTarget(item);
        Object results = null;
        
        if (pitem != null) {

            FeatureProcessor fp = featureProcessors.getFeatureProcessor(feature);

	    if (fp != null) {
	        try {
	            results = fp.process(pitem);
			} catch (ProcessException pe) {
			    throw new Error("Trouble while processing in feature processor "
			            +fp);
			}
	    } else {
	   
	        results = pitem.getFeatures().getObject(feature);
	    }
	}
    if (results == null){
        logger.debug("findFeature: ...results = '" + results 
                + "' for feature "+feature+" and item "+item
                +" (pitem="+pitem+", path="+path+")");}
	results = (results == null) ? "0" : results;
      
	return results;
    }


    /**
     * Compiles the given path into the compiled form
     * @param path the path to compile
     * @return the compiled form which is in the form
     * of an array path traversal enums and associated strings
     */
    private Object[] compile(String path) {
	List list = new ArrayList();

	if (path == null) {
	    return list.toArray();
	}

	StringTokenizer tok = new StringTokenizer(path, ":.");

	while (tok.hasMoreTokens()) {
	    String token = tok.nextToken();
	    OpEnum op = OpEnum.getInstance(token);
	    if (op == null) {
		throw new Error("Bad path: " + path);
	    } 

	    list.add(op);

	    if (op == OpEnum.RELATION) {
		list.add(tok.nextToken());
	    }
	}
	return list.toArray();
    }

    // inherited for Object

    public String toString() {
	return pathAndFeature;
    }

}


/**
 * An enumerated type associated with path operations.
 */
class OpEnum {
    static private Map map = new HashMap();

    public final static OpEnum NEXT = new OpEnum("n");
    public final static OpEnum PREV = new OpEnum("p");
    public final static OpEnum NEXT_NEXT = new OpEnum("nn");
    public final static OpEnum PREV_PREV = new OpEnum("pp");
    public final static OpEnum PARENT = new OpEnum("parent");
    public final static OpEnum DAUGHTER = new OpEnum("daughter");
    public final static OpEnum LAST_DAUGHTER = new OpEnum("daughtern");
    public final static OpEnum RELATION = new OpEnum("R");

    private String name;

    /**
     * Creates a new OpEnum.. There is a limited
     * set of OpEnums
     * @param name the path name for this Enum
     */
    private OpEnum(String name) {
	this.name = name;
	map.put(name, this);
    }

    /**
     * gets an OpEnum thats associated with
     * the given name.
     * @param name the name of the OpEnum of interest
     */
    public static OpEnum getInstance(String name) {
	return (OpEnum) map.get(name);
    }

    // inherited from Object
    public String toString() {
	return name;
    }
}
