/**
 * Copyright 2007 DFKI GmbH.
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
package marytts.signalproc.adaptation.gmm.jointgmm;

import java.io.FileNotFoundException;
import java.io.IOException;

import marytts.machinelearning.ContextualGMMParams;
import marytts.signalproc.adaptation.VocalTractTransformationData;
import marytts.util.io.MaryRandomAccessFile;

/**
 * A collection of JointGMMs, i.e. joint source-target gmms each trained separately using groups of source-target feature vectors
 * 
 * @author Oytun T&uuml;rk
 */
public class JointGMMSet extends VocalTractTransformationData {
	public static final String DEFAULT_EXTENSION = ".jgs";
	public JointGMM[] gmms;
	public ContextualGMMParams cgParams;

	public JointGMMSet() {
		allocate(0, null);
	}

	public JointGMMSet(JointGMMSet existing) {
		gmms = null;
		if (existing != null) {
			allocate(existing.gmms.length, null);
			for (int i = 0; i < existing.gmms.length; i++)
				gmms[i] = new JointGMM(existing.gmms[i]);

			cgParams = new ContextualGMMParams(existing.cgParams);
		}
	}

	public JointGMMSet(int numGMMs) {
		this(numGMMs, null);
	}

	public JointGMMSet(int numGMMs, ContextualGMMParams cgParamsIn) {
		allocate(numGMMs, cgParamsIn);
	}

	public void allocate(int numGMMs, ContextualGMMParams cgParamsIn) {
		if (numGMMs > 0)
			gmms = new JointGMM[numGMMs];
		else
			gmms = null;

		if (cgParamsIn != null)
			cgParams = new ContextualGMMParams(cgParamsIn);
		else if (numGMMs > 0)
			cgParams = new ContextualGMMParams(numGMMs);
		else
			cgParams = null;
	}

	public JointGMMSet(String jointGMMFile) {
		read(jointGMMFile);
	}

	public void write(String jointGMMFile) {
		MaryRandomAccessFile stream = null;
		try {
			stream = new MaryRandomAccessFile(jointGMMFile, "rw");
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		if (stream != null) {
			if (gmms != null && gmms.length > 0) {
				try {
					stream.writeInt(gmms.length);
				} catch (IOException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}

				if (cgParams != null)
					cgParams.write(stream);

				for (int i = 0; i < gmms.length; i++) {
					if (gmms[i] != null) {
						try {
							stream.writeBoolean(true); // gmm is not null
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}

						gmms[i].write(stream);
					} else {
						try {
							stream.writeBoolean(false); // gmm is null
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
				}
			} else {
				try {
					stream.writeInt(0);
				} catch (IOException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
			}

			try {
				stream.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	public void read(String jointGMMFile) {
		MaryRandomAccessFile stream = null;
		try {
			stream = new MaryRandomAccessFile(jointGMMFile, "r");
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		if (stream != null) {
			int numGMMs = 0;
			try {
				numGMMs = stream.readInt();
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}

			if (numGMMs > 0) {
				ContextualGMMParams tmpCgParams = new ContextualGMMParams();
				tmpCgParams.read(stream);

				allocate(numGMMs, tmpCgParams);

				for (int i = 0; i < numGMMs; i++) {
					boolean isGmmExisting = false;

					try {
						isGmmExisting = stream.readBoolean();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}

					if (isGmmExisting)
						gmms[i] = new JointGMM(stream);
				}
			}

			try {
				stream.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
}
