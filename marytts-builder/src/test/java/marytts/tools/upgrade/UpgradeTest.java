package marytts.tools.upgrade;

import static org.junit.Assert.assertEquals;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.List;

import marytts.tools.install.InstallFileParser;
import marytts.tools.install.VoiceComponentDescription;
import marytts.util.io.FileUtils;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.text.StrBuilder;
import org.incava.util.diff.Diff;
import org.incava.util.diff.Difference;
import org.junit.Test;
import static org.incava.util.diff.Difference.NONE;

public class UpgradeTest {

	private int maxLength(String[] lines) {
		int max = 0;
		for (int i=0; i<lines.length; i++) {
			if (lines[i] != null && lines[i].length() > max) {
				max = lines[i].length();
			}
		}
		return max;
	}
	
	private String diffSideBySide(String fromStr, String toStr){
		// from http://stackoverflow.com/questions/319479/generate-formatted-diff-output-in-java
        // this is equivalent of running unix diff -y command
        // not pretty, but it works. Feel free to refactor against unit test.
        String[] fromLines = fromStr.split("\n");
        String[] toLines = toStr.split("\n");
        List<Difference> diffs = new Diff(fromLines, toLines).diff();

        int padding = 3;
        int maxStrWidth = Math.max(maxLength(fromLines), maxLength(toLines)) + padding;

        StrBuilder diffOut = new StrBuilder();
        diffOut.setNewLineText("\n");
        int fromLineNum = 0;
        int toLineNum = 0;
        for(Difference diff : diffs) {
                int delStart = diff.getDeletedStart();
                int delEnd = diff.getDeletedEnd();
                int addStart = diff.getAddedStart();
                int addEnd = diff.getAddedEnd();

                boolean isAdd = (delEnd == NONE && addEnd != NONE);
                boolean isDel = (addEnd == NONE && delEnd != NONE);
                boolean isMod = (delEnd != NONE && addEnd != NONE);

                //write out unchanged lines between diffs
                while(true) {
                        String left = "";
                        String right = "";
                        if (fromLineNum < (delStart)){
                                left = fromLines[fromLineNum];
                                fromLineNum++;
                        }
                        if (toLineNum < (addStart)) {
                                right = toLines[toLineNum];
                                toLineNum++;
                        }
                        diffOut.append(StringUtils.rightPad(left, maxStrWidth));
                        diffOut.append("  "); // no operator to display
                        diffOut.appendln(right);

                        if( (fromLineNum == (delStart)) && (toLineNum == (addStart))) {
                                break;
                        }
                }

                if (isDel) {
                        //write out a deletion
                        for(int i=delStart; i <= delEnd; i++) {
                                diffOut.append(StringUtils.rightPad(fromLines[i], maxStrWidth));
                                diffOut.appendln("<");
                        }
                        fromLineNum = delEnd + 1;
                } else if (isAdd) {
                        //write out an addition
                        for(int i=addStart; i <= addEnd; i++) {
                                diffOut.append(StringUtils.rightPad("", maxStrWidth));
                                diffOut.append("> ");
                                diffOut.appendln(toLines[i]);
                        }
                        toLineNum = addEnd + 1; 
                } else if (isMod) {
                        // write out a modification
                        while(true){
                                String left = "";
                                String right = "";
                                if (fromLineNum <= (delEnd)){
                                        left = fromLines[fromLineNum];
                                        fromLineNum++;
                                }
                                if (toLineNum <= (addEnd)) {
                                        right = toLines[toLineNum];
                                        toLineNum++;
                                }
                                diffOut.append(StringUtils.rightPad(left, maxStrWidth));
                                diffOut.append("| ");
                                diffOut.appendln(right);

                                if( (fromLineNum > (delEnd)) && (toLineNum > (addEnd))) {
                                        break;
                                }
                        }
                }

        }

        //we've finished displaying the diffs, now we just need to run out all the remaining unchanged lines
        while(true) {
                String left = "";
                String right = "";
                if (fromLineNum < (fromLines.length)){
                        left = fromLines[fromLineNum];
                        fromLineNum++;
                }
                if (toLineNum < (toLines.length)) {
                        right = toLines[toLineNum];
                        toLineNum++;
                }
                diffOut.append(StringUtils.rightPad(left, maxStrWidth));
                diffOut.append("  "); // no operator to display
                diffOut.appendln(right);

                if( (fromLineNum == (fromLines.length)) && (toLineNum == (toLines.length))) {
                        break;
                }
        }

        return diffOut.toString();
}
	
	@Test
	public void convertHmmConfig() throws Exception {
		// setup SUT
		InstallFileParser parser = new InstallFileParser(UpgradeTest.class.getResource("/marytts/tools/upgrade/cmu-slt-4-component.xml"));
		List<VoiceComponentDescription> voiceDescriptions = parser.getVoiceDescriptions();
		String packageFilename = voiceDescriptions.get(0).getPackageFilename();
		Mary4To5VoiceConverter converter = new Mary4To5VoiceConverter(voiceDescriptions, new File(packageFilename));
		
		// exercise
		converter.loadConfigFromStream(UpgradeTest.class.getResourceAsStream("en_US-cmu-slt-hsmm-4.x.config"));
		converter.updateConfig("CmuSltHsmm");
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		converter.saveConfigToStream(baos);
		baos.close();
		String convertedConfig = baos.toString("UTF-8");
		// verify
		String expectedConfig = FileUtils.getStreamAsString(UpgradeTest.class.getResourceAsStream("en_US-cmu-slt-hsmm-5.config"), "UTF-8");
		assertEquals("Config differs from expectation as follows:\n" + diffSideBySide(expectedConfig, convertedConfig), expectedConfig, convertedConfig);
	}
	
}
