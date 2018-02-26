package marytts.phonetic;

import java.util.HashMap;
import java.util.Set;

import marytts.MaryException;
import marytts.phonetic.converter.Alphabet;

// Reflection
import org.reflections.Reflections;
import java.lang.reflect.Modifier;
import java.lang.reflect.Method;
import java.lang.reflect.Constructor;


/**
 * The alphabet factory to produce the alphabet/ipa conversion objects
 *
 * @author <a href="mailto:slemaguer@coli.uni-saarland.de"></a>
 */
public class AlphabetFactory
{
    /** Alphabet map used by the factory to get the alphabet instance */
    protected static HashMap<String, Alphabet> alphabets = new HashMap<String, Alphabet>();

    // Initialisation bloc
    static {

	try {
	    // Instantiate available alphabets
	    Reflections reflections = new Reflections("marytts.phonetic.converter");
	    for (Class<? extends Alphabet> alphabetClass : reflections.getSubTypesOf(Alphabet.class)) {
		if (! Modifier.isAbstract(alphabetClass.getModifiers())) {

		    Class<?> clazz = Class.forName(alphabetClass.getName());
		    Constructor<?> ctor = clazz.getConstructor();
		    Alphabet a = (Alphabet) ctor.newInstance(new Object[] {});
		    addAlphabet(a.getClass().getSimpleName(), a);
		}
	    }
	}
	catch (Exception ex) {
	    System.err.println("Cannot initialize alphabet factory");
	    System.exit(-1);
	}
    }


    public static void addAlphabet(String name, Alphabet instance) {
	alphabets.put(name.toLowerCase(), instance);
    }

    public static Alphabet getAlphabet(String name) throws MaryException {
	name = name.toLowerCase();
	if (!alphabets.containsKey(name))
	    throw new MaryException(name + " is not a registered alphabet");

	return alphabets.get(name);
    }

    public static Set<String> listAvailableAlphabets() {
	return alphabets.keySet();
    }
}


/* AlphabetFactory.java ends here */
