/**   
 *           The HMM-Based Speech Synthesis System (HTS)             
 *                       HTS Working Group                           
 *                                                                   
 *                  Department of Computer Science                   
 *                  Nagoya Institute of Technology                   
 *                               and                                 
 *   Interdisciplinary Graduate School of Science and Engineering    
 *                  Tokyo Institute of Technology                    
 *                                                                   
 *                Portions Copyright (c) 2001-2006                       
 *                       All Rights Reserved.
 *                         
 *              Portions Copyright 2000-2007 DFKI GmbH.
 *                      All Rights Reserved.                  
 *                                                                   
 *  Permission is hereby granted, free of charge, to use and         
 *  distribute this software and its documentation without           
 *  restriction, including without limitation the rights to use,     
 *  copy, modify, merge, publish, distribute, sublicense, and/or     
 *  sell copies of this work, and to permit persons to whom this     
 *  work is furnished to do so, subject to the following conditions: 
 *                                                                   
 *    1. The source code must retain the above copyright notice,     
 *       this list of conditions and the following disclaimer.       
 *                                                                   
 *    2. Any modifications to the source code must be clearly        
 *       marked as such.                                             
 *                                                                   
 *    3. Redistributions in binary form must reproduce the above     
 *       copyright notice, this list of conditions and the           
 *       following disclaimer in the documentation and/or other      
 *       materials provided with the distribution.  Otherwise, one   
 *       must contact the HTS working group.                         
 *                                                                   
 *  NAGOYA INSTITUTE OF TECHNOLOGY, TOKYO INSTITUTE OF TECHNOLOGY,   
 *  HTS WORKING GROUP, AND THE CONTRIBUTORS TO THIS WORK DISCLAIM    
 *  ALL WARRANTIES WITH REGARD TO THIS SOFTWARE, INCLUDING ALL       
 *  IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS, IN NO EVENT   
 *  SHALL NAGOYA INSTITUTE OF TECHNOLOGY, TOKYO INSTITUTE OF         
 *  TECHNOLOGY, HTS WORKING GROUP, NOR THE CONTRIBUTORS BE LIABLE    
 *  FOR ANY SPECIAL, INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY        
 *  DAMAGES WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS,  
 *  WHETHER IN AN ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTUOUS   
 *  ACTION, ARISING OUT OF OR IN CONNECTION WITH THE USE OR          
 *  PERFORMANCE OF THIS SOFTWARE.                                    
 *                                                                   
 */
package marytts.cart.io;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.util.Scanner;
import java.util.StringTokenizer;

import marytts.cart.CART;
import marytts.cart.DecisionNode;
import marytts.cart.LeafNode;
import marytts.cart.Node;
import marytts.cart.DecisionNode.BinaryByteDecisionNode;
import marytts.cart.LeafNode.PdfLeafNode;
import marytts.exceptions.MaryConfigurationException;
import marytts.features.FeatureDefinition;
import marytts.htsengine.PhoneTranslator;
import marytts.htsengine.HMMData.PdfFileFormat;
import marytts.util.MaryUtils;

import org.apache.log4j.Logger;

/**
 * Reader functions for CARTs in HTS format
 * 
 * @author Marcela Charfuelan
 */
public class HTSCARTReader {

	private FeatureDefinition featDef;
	private PhoneTranslator phTrans;
	private Logger logger = MaryUtils.getLogger("HTSCARTReader");
	private int vectorSize; // the vector size of the mean and variance on the leaves of the tree.

	public int getVectorSize() {
		return vectorSize;
	}

	/**
	 * Load the cart from the given file
	 * 
	 * @param numStates
	 *            number of states in the HTS model, it will create one cart tree per state.
	 * @param treeStream
	 *            the HTS tree text file, example tree-mgc.inf.
	 * @param pdfStream
	 *            the corresponding HTS pdf binary file, example mgc.pdf.
	 * @param fileFormat
	 *            fileFormat
	 * @param featDefinition
	 *            the feature definition
	 * @param phTranslator
	 *            a phone translator
	 * @return the size of the mean and variance vectors on the leaves.
	 * @throws IOException
	 *             if a problem occurs while loading
	 * @throws MaryConfigurationException
	 *             MaryConfigurationException
	 */
	public CART[] load(int numStates, InputStream treeStream, InputStream pdfStream, PdfFileFormat fileFormat,
			FeatureDefinition featDefinition, PhoneTranslator phTranslator) throws IOException, MaryConfigurationException {

		featDef = featDefinition;
		// phTrans = phoneTranslator;
		int i, j, length, state;
		BufferedReader s = null;
		String line, aux;

		phTrans = phTranslator;

		// create the number of carts it is going to read
		CART treeSet[] = new CART[numStates];
		for (i = 0; i < numStates; i++)
			treeSet[i] = new CART();

		// First load pdfs, so when creates the tree fill the leaf nodes with
		// the corresponding mean and variances.
		/**
		 * load pdf's, mean and variance pdfs format : pdf[numStates][numPdfs][numStreams][2*vectorSize]
		 * ------------------------------------------------------------------- for dur : pdf[ 1 ][numPdfs][ 1 ][2*numStates ] for
		 * mgc,str,mag: pdf[numStates][numPdfs][ 1 ][2*vectorSize]; for joinModel : pdf[ 1 ][numPdfs][ 1 ][2*vectorSize]; for lf0
		 * : pdf[numStates][numPdfs][numStreams][ 4 ] for gv-switch : pdf[ 1 ][ 1 ][ 1 ][ 1 ]
		 * ------------------------------------------------------------------ - numPdf : corresponds to the unique leaf node id. -
		 * 2*vectorSize : means that mean and variance are in the same vector. - 4 in lf0 : means 0: mean, 1: variance, 2: voiced
		 * weight and 3: unvoiced weight ------------------------------------------------------------------
		 */
		double pdf[][][][];
		pdf = loadPdfs(numStates, pdfStream, fileFormat);

		assert featDefinition != null : "Feature Definition was not set";

		/* read lines of tree-*.inf fileName */
		s = new BufferedReader(new InputStreamReader(treeStream, "UTF-8"));

		// skip questions section
		while ((line = s.readLine()) != null) {
			if (line.indexOf("QS") < 0)
				break; /* a new state is indicated by {*}[2], {*}[3], ... */
		}

		while ((line = s.readLine()) != null) {
			if (line.indexOf("{*}") >= 0) { /* this is the indicator of a new state-tree */
				aux = line.substring(line.indexOf("[") + 1, line.indexOf("]"));
				state = Integer.parseInt(aux);
				// loads one cart tree per state
				treeSet[state - 2].setRootNode(loadStateTree(s, pdf[state - 2]));

				// Now count all data once, so that getNumberOfData()
				// will return the correct figure.
				if (treeSet[state - 2].getRootNode() instanceof DecisionNode)
					((DecisionNode) treeSet[state - 2].getRootNode()).countData();

				logger.debug("load: CART[" + (state - 2) + "], total number of nodes in this CART: "
						+ treeSet[state - 2].getNumNodes());
			}
		} /* while */
		if (s != null)
			s.close();

		/* check that the tree was correctly loaded */
		if (treeSet.length == 0) {
			throw new IOException("LoadTreeSet: error no trees loaded");
		}

		return treeSet;

	}

	/**
	 * Load a tree per state
	 * 
	 * @param s
	 *            : text scanner of the whole tree-*.inf file
	 * @param pdf
	 *            : the pdfs for this state, pdf[numPdfs][numStreams][2*vectorSize]
	 */
	private Node loadStateTree(BufferedReader s, double pdf[][][]) throws IOException, MaryConfigurationException {

		Node rootNode = null;
		Node lastNode = null;

		StringTokenizer sline;
		String aux, buf;

		// create an empty binary decision node with unique id=0, this will be the rootNode
		Node nextNode = new DecisionNode.BinaryByteDecisionNode(0, featDef);

		// this is the rootNode
		rootNode = nextNode;
		nextNode.setIsRoot(true);

		int iaux, feaIndex, ndec, nleaf;
		ndec = 0;
		nleaf = 0;
		Node node = null;
		aux = s.readLine(); /* next line for this state tree must be { */
		int id;

		if (aux.indexOf("{") >= 0) {
			while ((aux = s.readLine()) != null && aux.indexOf("}") < 0) { /* last line for this state tree must be } */
				/* then parse this line, it contains 4 fields */
				/* 1: node index # 2: Question name 3: NO # node 4: YES # node */
				sline = new StringTokenizer(aux);

				/* 1: gets index node and looks for the node whose idx = buf */
				buf = sline.nextToken();
				if (buf.startsWith("-")) {
					id = Integer.parseInt(buf.substring(1));
					ndec++;
				} else if (buf.contentEquals("0"))
					id = 0;
				else
					throw new MaryConfigurationException("LoadStateTree: line does not start with a decision node (-id), line="
							+ aux);
				// 1. find the node in the tree, it has to be already created.
				node = findDecisionNode(rootNode, id);

				if (node == null)
					throw new MaryConfigurationException("LoadStateTree: Node not found, index = " + buf);
				else {
					/* 2: gets question name and question name val */
					buf = sline.nextToken();
					String[] fea_val = buf.split("="); /* splits featureName=featureValue */
					feaIndex = featDef.getFeatureIndex(fea_val[0]);

					/* Replace back punctuation values */
					/* what about tricky phones, if using halfphones it would not be necessary */
					if (fea_val[0].contentEquals("sentence_punc") || fea_val[0].contentEquals("prev_punctuation")
							|| fea_val[0].contentEquals("next_punctuation")) {
						// System.out.print("CART replace punc: " + fea_val[0] + " = " + fea_val[1]);
						fea_val[1] = phTrans.replaceBackPunc(fea_val[1]);
						// System.out.println(" --> " + fea_val[0] + " = " + fea_val[1]);
					} else if (fea_val[0].contains("tobi_")) {
						// System.out.print("CART replace tobi: " + fea_val[0] + " = " + fea_val[1]);
						fea_val[1] = phTrans.replaceBackToBI(fea_val[1]);
						// System.out.println(" --> " + fea_val[0] + " = " + fea_val[1]);
					} else if (fea_val[0].contains("phone")) {
						// System.out.print("CART replace phone: " + fea_val[0] + " = " + fea_val[1]);
						fea_val[1] = phTrans.replaceBackTrickyPhones(fea_val[1]);
						// System.out.println(" --> " + fea_val[0] + " = " + fea_val[1]);
					}

					// add featureName and featureValue to the decision nod
					((BinaryByteDecisionNode) node).setFeatureAndFeatureValue(fea_val[0], fea_val[1]);

					// add NO and YES indexes to the daughther nodes
					/* NO index */
					buf = sline.nextToken();
					if (buf.startsWith("-")) { // Decision node
						iaux = Integer.parseInt(buf.substring(1));
						// create an empty binary decision node with unique id
						BinaryByteDecisionNode auxnode = new DecisionNode.BinaryByteDecisionNode(iaux, featDef);
						((DecisionNode) node).replaceDaughter(auxnode, 1);
					} else { // LeafNode
						iaux = Integer.parseInt(buf.substring(buf.lastIndexOf("_") + 1, buf.length() - 1));
						// create an empty PdfLeafNode
						PdfLeafNode auxnode = new LeafNode.PdfLeafNode(iaux, pdf[iaux - 1]);
						((DecisionNode) node).replaceDaughter(auxnode, 1);
						nleaf++;
					}

					/* YES index */
					buf = sline.nextToken();
					if (buf.startsWith("-")) { // Decision node
						iaux = Integer.parseInt(buf.substring(1));
						// create an empty binary decision node with unique id=0
						BinaryByteDecisionNode auxnode = new DecisionNode.BinaryByteDecisionNode(iaux, featDef);
						((DecisionNode) node).replaceDaughter(auxnode, 0);
					} else { // LeafNode
						iaux = Integer.parseInt(buf.substring(buf.lastIndexOf("_") + 1, buf.length() - 1));
						// create an empty PdfLeafNode
						PdfLeafNode auxnode = new LeafNode.PdfLeafNode(iaux, pdf[iaux - 1]);
						((DecisionNode) node).replaceDaughter(auxnode, 0);
						nleaf++;
					}
				} /* if node not null */
				sline = null;
			} /* while there is another line and the line does not contain } */
		} /* if not "{" */

		logger.debug("loadStateTree: loaded CART contains " + (ndec + 1) + " Decision nodes and " + nleaf + " Leaf nodes.");
		return rootNode;

	} /* method loadTree() */

	/**
	 * @param node
	 *            , decision node
	 * @param numId
	 *            , index to look for.
	 * @return node if node is instance of decision node, aux if aux != null, return aux otherwise
	 */
	private Node findDecisionNode(Node node, int numId) {

		Node aux = null;

		if (node instanceof DecisionNode) {
			// System.out.print(" id=" + ((DecisionNode)node).getUniqueDecisionNodeId());
			if (((DecisionNode) node).getUniqueDecisionNodeId() == numId)
				return node;
			else {
				for (int i = 0; i < ((DecisionNode) node).getNumberOfDaugthers(); i++) {
					aux = findDecisionNode(((DecisionNode) node).getDaughter(i), numId);
					if (aux != null)
						return aux;
				}
			}
		}
		return aux;

	} /* method findDecisionNode */

	/**
	 * Load pdf's, mean and variance the #leaves corresponds to the unique leaf node id pdf -->
	 * [#states][#leaves][#streams][vectorsize] The format of pdf files for mgc, str or mag is: header: 4 byte int: dimension
	 * feature vector 4 byte int: # of leaf nodes for state 1 4 byte int: # of leaf nodes for state 2 ... 4 byte int: # of leaf
	 * nodes for state N probability distributions: 4 byte float means and variances (2*pdfVsize): all leaves for state 1 4 byte
	 * float means and variances (2*pdfVsize): all leaves for state 2 ... 4 byte float means and variances (2*pdfVsize): all
	 * leaves for state N --------------------------------------------------------------------- The format of pdf files for dur
	 * and JoinModeller is: header: 4 byte int: # of HMM states <-- this is the dimension of vector in duration 4 byte int: # of
	 * leaf nodes for state 1 <-- dur has just one state probability distributions: 4 byte float means and variances (2*HMMsize):
	 * all leaves for state 1 --------------------------------------------------------------------- The format of pdf files for
	 * lf0 is: header: 4 byte int: dimension feature vector 4 byte int: # of leaf nodes for state 1 4 byte int: # of leaf nodes
	 * for state 2 ... 4 byte int: # of leaf nodes for state N probability distributions: 4 byte float mean, variance, voiced,
	 * unvoiced (4 floats): stream 1..S, leaf 1..L, state 1 4 byte float mean, variance, voiced, unvoiced (4 floats): stream 1..S,
	 * leaf 1..L, state 2 ... 4 byte float mean, variance, voiced, unvoiced (4 floats): stream 1..S, leaf 1..L, state N
	 */
	private double[][][][] loadPdfs(int numState, InputStream pdfStream, PdfFileFormat fileFormat) throws IOException,
			MaryConfigurationException {

		DataInputStream data_in;
		int i, j, k, l, numDurPdf, lf0Stream;
		double vw, uvw;
		int vsize;
		int numPdf[];
		int numStream;
		int numMSDFlag; /* MSD: Multi stream dimensions: in case of lf0 for example */
		double pdf[][][][] = null; // pdf[numState][numPdf][stream][vsize];

		// TODO: how to make this loading more general, different files have different formats. Right now the way
		// of loading depends on the name of the file, I need to change that!
		// pdfFileName.contains("dur.pdf") || pdfFileName.contains("joinModeller.pdf")
		if (fileFormat == PdfFileFormat.dur || fileFormat == PdfFileFormat.join) {
			/* ________________________________________________________________ */
			/*-------------------- load pdfs for duration --------------------*/
			data_in = new DataInputStream(new BufferedInputStream(pdfStream));
			logger.debug("loadPdfs reading model of type " + fileFormat);

			/* read the number of states & the number of pdfs (leaf nodes) */
			/* read the number of HMM states, this number is the same for all pdf's. */

			numMSDFlag = data_in.readInt();
			numStream = data_in.readInt();
			vectorSize = data_in.readInt();
			// ---vectorSize = numState;
			// System.out.println("loadPdfs: nstate = " + nstate);

			numState = numStream;

			/* check number of states */
			if (numState < 0)
				throw new MaryConfigurationException("loadPdfs: #HMM states must be positive value.");

			/* read the number of duration pdfs */
			numDurPdf = data_in.readInt();
			logger.debug("loadPdfs: numPdf[state:0]=" + numDurPdf);

			/* Now we know the number of duration pdfs and the vector size which is */
			/* the number of states in each HMM. Here the vector size is 2*nstate because */
			/* the first nstate correspond to the mean and the second nstate correspond */
			/* to the diagonal variance vector, the mean and variance are copied here in */
			/* only one vector. */
			/* 2*nstate because the vector size for duration is the number of states */
			pdf = new double[1][numDurPdf][1][2 * numState]; // just one state and one stream
			vsize = (2 * numState);
			/* read pdfs (mean & variance) */
			// NOTE: Here (hts_engine v1.04) the order is different as before, here mean and variance are saved consecutively
			for (i = 0; i < numDurPdf; i++) {
				for (j = 0; j < numState; j++) {
					pdf[0][i][0][j] = data_in.readFloat(); // read mean
					pdf[0][i][0][j + numState] = data_in.readFloat(); // read variance
					// System.out.println("durpdf[" + i + "]" + "[" + j + "]:" + pdf[0][i][0][j]);
				}
			}
			data_in.close();
			data_in = null;

		} else if (fileFormat == PdfFileFormat.lf0) { // pdfFileName.contains("lf0.pdf")
			/* ____________________________________________________________________ */
			/*-------------------- load pdfs for Log F0 --------------*/
			data_in = new DataInputStream(new BufferedInputStream(pdfStream));
			logger.debug("loadPdfs reading model of type " + fileFormat);
			/* read the number of streams for f0 modeling */
			// lf0Stream = data_in.readInt();
			// vectorSize = lf0Stream;
			numMSDFlag = data_in.readInt();
			numStream = data_in.readInt();
			vectorSize = data_in.readInt();

			lf0Stream = numStream;
			// System.out.println("loadPdfs: lf0stream = " + lf0stream);

			if (lf0Stream < 0)
				throw new MaryConfigurationException("loadPdfs:  #stream for log f0 part must be positive value.");

			/* read the number of pdfs for each state position */
			pdf = new double[numState][][][];
			numPdf = new int[numState];
			for (i = 0; i < numState; i++) {
				numPdf[i] = data_in.readInt();
				logger.debug("loadPdfs: numPdf[state:" + i + "]=" + numPdf[i]);
				if (numPdf[i] < 0)
					throw new MaryConfigurationException("loadPdfs: #lf0 pdf at state " + i + " must be positive value.");
				// System.out.println("nlf0pdf[" + i + "] = " + numPdf[i]);
				/* Now i know the size of pdfs for lf0 [#states][#leaves][#streams][lf0_vectorsize] */
				/* lf0_vectorsize = 4: mean, variance, voiced weight, and unvoiced weight */
				/* so i can allocate memory for lf0pdf[][][] */
				pdf[i] = new double[numPdf[i]][lf0Stream][4];
			}

			/* read lf0 pdfs (mean, variance and weight). */
			for (i = 0; i < numState; i++) {
				for (j = 0; j < numPdf[i]; j++) {
					for (k = 0; k < lf0Stream; k++) {
						for (l = 0; l < 4; l++) {
							pdf[i][j][k][l] = data_in.readFloat();
							// System.out.format("pdf[%d][%d][%d][%d]=%f\n", i,j,k,l,pdf[i][j][k][l]);
						}
						// System.out.format("\n");
						// NOTE: Here (hts_engine v1.04) the order seem to be the same as before
						/* pdf[i][j][k][0]; mean */
						/* pdf[i][j][k][1]; vari */
						vw = pdf[i][j][k][2]; /* voiced weight */
						uvw = pdf[i][j][k][3]; /* unvoiced weight */
						if (vw < 0.0 || uvw < 0.0 || vw + uvw < 0.99 || vw + uvw > 1.01)
							throw new MaryConfigurationException("loadPdfs: voiced/unvoiced weights must be within 0.99 to 1.01.");
					}
				}
			}

			data_in.close();
			data_in = null;

		} else if (fileFormat == PdfFileFormat.mgc || fileFormat == PdfFileFormat.str || fileFormat == PdfFileFormat.mag) {
			// pdfFileName.contains("mgc.pdf") ||
			// pdfFileName.contains("str.pdf") ||
			// pdfFileName.contains("mag.pdf")
			/* ___________________________________________________________________________ */
			/*-------------------- load pdfs for mgc, str or mag ------------------------*/
			data_in = new DataInputStream(new BufferedInputStream(pdfStream));
			logger.debug("loadPdfs reading model of type " + fileFormat);
			/* read vector size for spectrum */

			// numStream = 1; // just one stream for mgc, str, mag. This is just to have only one
			// type of pdf vector for all posible pdf's
			// vsize = data_in.readInt();
			// vectorSize = vsize;
			numMSDFlag = data_in.readInt();
			numStream = data_in.readInt();
			vectorSize = data_in.readInt();

			vsize = vectorSize;
			// System.out.println("loadPdfs: vsize = " + vsize);

			if (vsize < 0)
				throw new MaryConfigurationException("loadPdfs: vector size of pdf must be positive.");

			/* Now we need the number of pdf's for each state */
			pdf = new double[numState][][][];
			numPdf = new int[numState];
			for (i = 0; i < numState; i++) {
				numPdf[i] = data_in.readInt();
				logger.debug("loadPdfs: numPdf[state:" + i + "]=" + numPdf[i]);
				if (numPdf[i] < 0)
					throw new MaryConfigurationException("loadPdfs: #pdf at state " + i + " must be positive value.");
				// System.out.println("nmceppdf[" + i + "] = " + nmceppdf[i]);
				/* Now i know the size of mceppdf[#states][#leaves][vectorsize] */
				/* so i can allocate memory for mceppdf[][][] */
				pdf[i] = new double[numPdf[i]][numStream][2 * vsize];
			}

			/* read pdfs (mean, variance). (2*vsize because mean and diag variance */
			/* are allocated in only one vector. */
			for (i = 0; i < numState; i++) {
				for (j = 0; j < numPdf[i]; j++) {
					/*
					 * for( k=0; k<(2*vsize); k++ ){ pdf[i][j][0][k] = data_in.readFloat(); // [0] corresponds to stream, in this
					 * case just one. //System.out.println("pdf["+ i + "][" + j + "][0][" + k + "] =" + pdf[i][j][0][k]); }
					 */
					// NOTE: Here (hts_engine v1.04) the order is different as before, here mean and variance are saved
					// consecutively
					// so now the pdf contains: mean[0], vari[0], mean[1], vari[1], etc...
					for (k = 0; k < vsize; k++) {
						pdf[i][j][0][k] = data_in.readFloat(); // [0] corresponds to stream, in this case just one.
						// System.out.println("pdf["+ i + "][" + j + "][0][" + k + "] =" + pdf[i][j][0][k]);
						pdf[i][j][0][k + vsize] = data_in.readFloat();
					}
				}
			}
			data_in.close();
			data_in = null;

		}

		return pdf;

	} /* method loadPdfs */

	public static void main(String[] args) throws IOException, InterruptedException {
		/* configure log info */
		org.apache.log4j.BasicConfigurator.configure();

		String contextFile = "/project/mary/marcela/openmary/lib/voices/hsmm-slt/cmu_us_arctic_slt_a0001.pfeats";
		Scanner context = new Scanner(new BufferedReader(new FileReader(contextFile)));
		String strContext = "";
		while (context.hasNext()) {
			strContext += context.nextLine();
			strContext += "\n";
		}
		context.close();
		// System.out.println(strContext);
		FeatureDefinition feaDef = new FeatureDefinition(new BufferedReader(new StringReader(strContext)), false);

		CART[] mgcTree = null;
		int numStates = 5;
		String trickyPhones = "/project/mary/marcela/openmary/lib/voices/hsmm-slt/trickyPhones.txt";
		String treefile = "/project/mary/marcela/openmary/lib/voices/hsmm-slt/tree-dur.inf";
		String pdffile = "/project/mary/marcela/openmary/lib/voices/hsmm-slt/dur.pdf";
		int vSize;

		// Check if there are tricky phones, and create a PhoneTranslator object
		PhoneTranslator phTranslator = new PhoneTranslator(new FileInputStream(trickyPhones));

		HTSCARTReader htsReader = new HTSCARTReader();
		try {
			mgcTree = htsReader.load(numStates, new FileInputStream(treefile), new FileInputStream(pdffile), PdfFileFormat.dur,
					feaDef, phTranslator);
			vSize = htsReader.getVectorSize();
			System.out.println("loaded " + pdffile + "  vector size=" + vSize);
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

	}

}
