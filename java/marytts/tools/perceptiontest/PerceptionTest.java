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
        
        if(args.length != 2){
            System.out.println("USAGE: PerceptionTest <Test sample set in xml> <output directory absolute path to store results>");
            System.out.println("Example: PerceptionTest testSampleSet.xml /home/user/output/");
            return;
        }
        
        //String fileName = "/home/sathish/phd/PerceptionTest/perception.xml";
        //String outPutDirectory = "/home/sathish/phd/PerceptionTest/";
        //(new PerceptionTestHttpServer(fileName, outPutDirectory)).run();
        (new PerceptionTestHttpServer(args[0], args[1])).run();
    }

}
