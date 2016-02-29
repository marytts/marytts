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
package marytts.signalproc.adaptation.codebook;

import java.io.IOException;

import marytts.signalproc.adaptation.Context;
import marytts.util.io.MaryRandomAccessFile;

/**
 * 
 * A collection of speaker specific acoustic features for a voice conversion unit, i.e. a speech frame or a phone, etc.
 * 
 * @author Oytun T&uuml;rk
 * 
 */
public class WeightedCodebookSpeakerItem {
	public double[] lsfs;
	public double[] mfccs;
	public double f0;
	public double duration;
	public double energy;
	public String phn;
	public Context context;

	public WeightedCodebookSpeakerItem() {
		this(0, 0);
	}

	public WeightedCodebookSpeakerItem(int lpOrder, int mffcDimension) {
		allocate(lpOrder, mffcDimension);
		phn = "";
	}

	public void allocate(int lpOrder, int mfccDimension) {
		allocateLsfs(lpOrder);
		allocateMfccs(mfccDimension);
	}

	public void allocateLsfs(int lpOrder) {
		if (lsfs == null || lpOrder != lsfs.length) {
			if (lpOrder > 0)
				lsfs = new double[lpOrder];
			else
				lsfs = null;
		}
	}

	public void allocateMfccs(int mffcDimension) {
		if (mfccs == null || mffcDimension != mfccs.length) {
			if (mffcDimension > 0)
				mfccs = new double[mffcDimension];
			else
				mfccs = null;
		}
	}

	public void setLsfs(double[] lsfsIn) {
		if (lsfsIn != null) {
			if (lsfs == null || lsfsIn.length != lsfs.length)
				allocateLsfs(lsfsIn.length);

			System.arraycopy(lsfsIn, 0, lsfs, 0, lsfsIn.length);
		} else
			lsfs = null;
	}

	public void setMfccs(double[] mfccsIn) {
		if (mfccsIn != null) {
			if (mfccs == null || mfccsIn.length != mfccs.length)
				allocateMfccs(mfccsIn.length);

			System.arraycopy(mfccsIn, 0, mfccs, 0, mfccsIn.length);
		} else
			mfccs = null;
	}

	public void write(MaryRandomAccessFile ler) {
		if (lsfs != null || mfccs != null) {
			if (lsfs != null) {
				try {
					ler.writeInt(lsfs.length);
				} catch (IOException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
				try {
					ler.writeDouble(lsfs);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			} else {
				try {
					ler.writeInt(0);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}

			if (mfccs != null) {
				try {
					ler.writeInt(mfccs.length);
				} catch (IOException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
				try {
					ler.writeDouble(mfccs);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			} else {
				try {
					ler.writeInt(0);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}

			try {
				ler.writeDouble(f0);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			try {
				ler.writeDouble(duration);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			try {
				ler.writeDouble(energy);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			int tmpLen = 0;

			if (phn != "")
				tmpLen = phn.length();

			try {
				ler.writeInt(tmpLen);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			if (tmpLen > 0) {
				try {
					ler.writeChar(phn.toCharArray());
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}

			tmpLen = 0;

			if (context != null) {
				if (context.allContext != "")
					tmpLen = context.allContext.length();
			}

			try {
				ler.writeInt(tmpLen);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			if (tmpLen > 0) {
				try {
					ler.writeChar(context.allContext.toCharArray());
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
	}

	public void read(MaryRandomAccessFile ler, int lpOrder, int mfccDimension) {
		allocate(lpOrder, mfccDimension);

		if ((lsfs != null && lpOrder > 0) || (mfccs != null && mfccDimension > 0)) {
			int lpOrderInFile = 0;

			try {
				lpOrderInFile = ler.readInt();
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			assert lpOrderInFile == lpOrder;

			if (lpOrderInFile > 0) {
				try {
					lsfs = ler.readDouble(lpOrderInFile);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			} else
				lsfs = null;

			int mfccDimensionInFile = 0;

			try {
				mfccDimensionInFile = ler.readInt();
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			assert mfccDimensionInFile == mfccDimension;

			if (mfccDimensionInFile > 0) {
				try {
					mfccs = ler.readDouble(mfccDimensionInFile);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			} else
				mfccs = null;

			try {
				f0 = ler.readDouble();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			try {
				duration = ler.readDouble();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			try {
				energy = ler.readDouble();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			int tmpLen = 0;
			try {
				tmpLen = ler.readInt();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			phn = "";
			if (tmpLen > 0) {
				try {
					phn = String.copyValueOf(ler.readChar(tmpLen));
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}

			tmpLen = 0;
			try {
				tmpLen = ler.readInt();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			context = null;
			if (tmpLen > 0) {
				try {
					context = new Context(String.copyValueOf(ler.readChar(tmpLen)));
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
	}
}
