package marytts.tools.perceptiontest;

/**
 * 
 * Main class for perception test
 * @author sathish pammi
 *
 */
public class PerceptionTest {

    /**
     * @param args
     */
    public static void main(String[] args) {
        
        //String fileName = "/home/sathish/phd/PerceptionTest/perception.xml";
        //String outPutDirectory = "/home/sathish/phd/PerceptionTest/";
        //(new PerceptionTestHttpServer(fileName, outPutDirectory)).run();
        if(args.length == 2){
            (new PerceptionTestHttpServer(args[0], args[1])).run();
        }
        else if(args.length == 3){
            (new PerceptionTestHttpServer(args[0], args[1], new Integer(args[2]).intValue())).run();
        }
        else {
            System.out.println("USAGE: PerceptionTest <Test sample set in xml> <output directory absolute path to store results> [serverPort (default port=44547)]");
            System.out.println("Example: PerceptionTest testSampleSet.xml /home/user/output/ 44547");
            return;
        }
    }

}
