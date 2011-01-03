/**
 * Copyright 2009 DFKI GmbH.
 * All Rights Reserved.  Use is subject to license terms.
 *
 * This file is part of MARY TTS.
 *
 * MARY TTS is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
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
