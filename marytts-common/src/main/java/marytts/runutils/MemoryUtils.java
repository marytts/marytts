package marytts.runutils;
/**
 *
 *
 * @author <a href="mailto:slemaguer@coli.uni-saarland.de"></a>
 */
public class MemoryUtils
{
    /**
     * Determine the amount of available memory. "Available" memory is calculated as <code>(max - total) + free</code>.
     *
     * @return the number of bytes of memory available according to the above algorithm.
     */
    public static long availableMemory() {
	Runtime rt = Runtime.getRuntime();
	return rt.maxMemory() - rt.totalMemory() + rt.freeMemory();
    }

    /**
     * Verify if the java virtual machine is in a low memory condition. The memory is considered low if less than a specified
     * value is still available for processing. "Available" memory is calculated using <code>availableMemory()</code>.The
     * threshold value can be specified as the Mary property mary.lowmemory (in bytes). It defaults to 20000000 bytes.
     *
     * @return a boolean indicating whether or not the system is in low memory condition.
     */
    public static boolean lowMemoryCondition() {
	return availableMemory() < lowMemoryThreshold();
    }

    /**
     * Verify if the java virtual machine is in a very low memory condition. The memory is considered very low if less than half a
     * specified value is still available for processing. "Available" memory is calculated using <code>availableMemory()</code>
     * .The threshold value can be specified as the Mary property mary.lowmemory (in bytes). It defaults to 20000000 bytes.
     *
     * @return a boolean indicating whether or not the system is in very low memory condition.
     */
    public static boolean veryLowMemoryCondition() {
	return availableMemory() < lowMemoryThreshold() / 2;
    }



    // FIXME: hardcoded
    private static long lowMemoryThreshold() {
	if (lowMemoryThreshold < 0) // not yet initialised
	    // lowMemoryThreshold = (long) MaryProperties.getInteger("mary.lowmemory", 10000000);
	    lowMemoryThreshold = 1000000000;
	return lowMemoryThreshold;
    }

    private static long lowMemoryThreshold = -1;
}


/* MemoryUtils.java ends here */
