package marytts.tools.voiceimport;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.commons.io.FileUtils;

import marytts.exceptions.MaryConfigurationException;
import marytts.util.io.BasenameList;

public class MAUSLabeller extends VoiceImportComponent {
	private String locale;
	 
	
	public final String MAUSDIR = "MAUSLabeller.mdir";
	public final String PARDIR = "MAUSLabeller.pdir";
	public final String OUTLABDIR = "MAUSLabeller.outputLabDir";
	public final String LANG = "MAUSLabeller.lang";
	public final String TEXTGRIDDIR = "MAUSLabeller.textgrid";
	protected int percent = 0;
	
	@Override
	protected void setupHelp() {
		// TODO Auto-generated method stub
		props2Help = new TreeMap();
		props2Help.put(MAUSDIR,"directory containing the local installation of MAUS");
		props2Help.put(PARDIR,"directory containing the PAR files"); 		
		props2Help.put(OUTLABDIR, "Directory to store generated labels.");
		props2Help.put(LANG, "MAUS language to use for labelling.");

	}

	
	@Override
	public SortedMap<String, String> getDefaultProps(DatabaseLayout db) {
		// TODO Auto-generated method stub
		this.db = db;
		String phoneXml;
		locale = db.getProp(db.LOCALE);
		if (props == null){
			props = new TreeMap();
			
			
			String mausdir = System.getProperty("MAUSDIR");
			if ( mausdir == null ) {
				mausdir = db.getExternal(db.MAUSDIR);
				if ( mausdir == null ) {
					mausdir = "/";
				}       
			}
			
			props.put(MAUSDIR,mausdir);
			
			String pardir = System.getProperty("PARDIR");
			if ( pardir == null ) {
				pardir = db.getExternal(db.PARDIR);
				if ( pardir == null ) {
					pardir = "/";
				}       
			}
			props.put(PARDIR, pardir);
			

			props.put(OUTLABDIR, db.getProp(db.ROOTDIR)
					+"lab"
					+System.getProperty("file.separator"));
			
			props.put(LANG, "nze");
			
			props.put(TEXTGRIDDIR, db.getProp(db.ROOTDIR)
					+"textgrid"
					+System.getProperty("file.separator"));

		}
		return props;

	}

	@Override
	public String getName() {
		return "MAUSLabeller";
	}

	@Override
	public boolean compute() throws Exception {
		// TODO Auto-generated method stub

		CheckPraatScript();
		// Iterate through each file and get the maus output
        for (int i = 0; i < bnl.getLength(); i++) {
            percent = 100 * i / bnl.getLength();
            RunMAUS(bnl.getName(i));
            Convert2Lab(bnl.getName(i));
            
            //generateAllophonesFile(bnl.getName(i));
            System.out.println("    " + bnl.getName(i));
        }
		
		
		
		
		
		return true;
	}
	
	/**
	 * Run maus on the input file, with PAR file (with same base name as wave file) located in the pre-defined PAR directory
	 * @param signal wav file path
	 * @throws IOException 
	 * @throws InterruptedException 
	 * @throws MaryConfigurationException 
	 */
	private void RunMAUS(String signal) throws IOException, InterruptedException, MaryConfigurationException
	{
		String line = null;
		String task = "RunMAUS";
		
		File textgrid = new File(getProp(TEXTGRIDDIR), signal+".TextGrid");
		textgrid.getParentFile().mkdirs();
		
		//If the TextGrid file already exists, then don't redo this process
		if (textgrid.exists())
		{
			System.out.println(textgrid.getName() + "exists.");
			return;
		}
		
		
		File mausDir = new File(getProp(MAUSDIR));
		File maus = new File(mausDir, "maus");
		assert maus.exists();
		
		File wav = new File(db.getProp(db.WAVDIR), signal + db.getProp(db.WAVEXT));
		assert wav.exists();
		
		File parDir = new File(getProp(PARDIR));
		File par = new File (parDir, signal + ".par");
		assert par.exists();
		

		String cmd = maus.getAbsolutePath();
		cmd += " SIGNAL="+wav.getAbsolutePath();
		cmd += " BPF="+par.getAbsolutePath();
		cmd += " OUT="+textgrid.getAbsolutePath();
		cmd += " LANGUAGE="+getProp(LANG);
		cmd += " OUTFORMAT=TextGrid CANONLY=yes INSORTTEXTGRID=no INSKANTEXTGRID=no "
				+ "USETRN=no RULESET=rml-0.95.rul NOINITIALFINALSILENCE=no";
		
		
		ExecuteScript(cmd, mausDir.getAbsolutePath(), task);

	}
	
	private void CheckPraatScript() throws IOException
	{
        File TextGrid2Lab = new File(db.getVoiceDir(), "TextGrid2Lab.praat");
        if (!TextGrid2Lab.exists())
        {
        	FileUtils.copyInputStreamToFile(DatabaseImportMain.class.getResourceAsStream("TextGrid2Lab.praat"), TextGrid2Lab);
        }
        
        assert TextGrid2Lab.exists();
        
	}
	
	
	private void Convert2Lab(String signal) throws MaryConfigurationException, InterruptedException, IOException
	{
		String task = "ConvertTextGrid2Lab";
		
		File labDir = new File(getProp(OUTLABDIR));
		File lab = new File(labDir, signal + ".lab");
		if (lab.exists())
			return;
		
		
        lab.getParentFile().mkdirs();
            
        File textgrid = new File(getProp(TEXTGRIDDIR), signal+".TextGrid");
        
        File f = new File(labDir, signal);
        
        
        assert lab.getParentFile().exists();
        
        String cmd = "praat TextGrid2Lab.praat";
        cmd += " " + textgrid.getParentFile().getAbsolutePath();
        cmd += " " + lab.getParentFile().getAbsolutePath();
        cmd += " " + f.getName();
        cmd += " 1";
        
		ExecuteScript(cmd, db.getVoiceDir().getAbsolutePath(), task);

        ConvertPhones(lab);
	}
	
	private void ConvertPhones(File labelFile) throws IOException, MaryConfigurationException, InterruptedException
	{
		assert labelFile.exists();
        File mausScript = new File(db.getVoiceDir(), "maus_convert.sh");
        if (!mausScript.exists())
        {
        	FileUtils.copyInputStreamToFile(DatabaseImportMain.class.getResourceAsStream("maus_convert.sh"), mausScript);
        }
        
        String cmd = "sh maus_convert.sh " + labelFile.getAbsolutePath();
        
        ExecuteScript(cmd, db.getVoiceDir().getAbsolutePath(), "MAUSConvert");
		
	}
	
	private void ExecuteScript(String cmd, String startPath, String task) throws MaryConfigurationException, InterruptedException, IOException
	{
		String line = null;
		Runtime rtime = Runtime.getRuntime();
		//get a shell
		Process process = rtime.exec("/bin/bash");
		//get an output stream to write to the shell
		PrintWriter pw = new PrintWriter(
				new OutputStreamWriter(process.getOutputStream()));
	
		pw.print("( cd "+startPath
				+"; " + cmd
				+"; exit )\n");
		pw.flush();
		//shut down
		pw.close();
		process.waitFor();
		// check exit value
		if (process.exitValue() != 0) {
			BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()));
			while ((line = errorReader.readLine()) != null) {
				System.err.println("ERR> " + line);
			}
			errorReader.close();
			throw new MaryConfigurationException(task + " computation failed.\nError: " + line);
		}
	}

	@Override
	public int getProgress() {
		// TODO Auto-generated method stub
		return percent;
	}

}