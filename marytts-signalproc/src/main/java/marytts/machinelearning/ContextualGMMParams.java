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
package marytts.machinelearning;

import java.io.IOException;
import java.util.Iterator;
import java.util.Set;

import marytts.modules.phonemiser.Allophone;
import marytts.modules.phonemiser.AllophoneSet;
import marytts.util.io.MaryRandomAccessFile;
import marytts.util.math.MathUtils;
import marytts.util.signal.SignalProcUtils;
import marytts.util.string.StringUtils;

/**
 * 
 * Wrapper for contextual parameters for GMM training - includes various phone identity or class based groups
 * 
 * @author Oytun T&uuml;rk
 */
public class ContextualGMMParams {
	public static final int FRICATIVE = Integer.parseInt("000000000001", 2);
	public static final int GLIDE = Integer.parseInt("000000000010", 2);
	public static final int LIQUID = Integer.parseInt("000000000100", 2);
	public static final int NASAL = Integer.parseInt("000000001000", 2);
	public static final int PAUSE = Integer.parseInt("000000010000", 2);
	public static final int PLOSIVE = Integer.parseInt("000000100000", 2);
	public static final int SONORANT = Integer.parseInt("000001000000", 2);
	public static final int SYLLABIC = Integer.parseInt("000010000000", 2);
	public static final int VOICED = Integer.parseInt("000100000000", 2);
	public static final int VOWEL = Integer.parseInt("001000000000", 2);

	public int contextClassificationType;
	public static final int NO_PHONEME_CLASS = -1;
	public static final int SILENCE_SPEECH = 1;
	public static final int VOWEL_SILENCE_CONSONANT = 2;
	public static final int PHONOLOGY_CLASS = 3;
	public static final int FRICATIVE_GLIDELIQUID_NASAL_PLOSIVE_VOWEL_OTHER = 4;
	public static final int PHONEME_IDENTITY = 5;

	public static final int FRICATIVE_MULTIPLIER = 1;
	public static final int GLIDELIQUID_MULTIPLIER = 1;
	public static final int NASAL_MULTIPLIER = 1;
	public static final int PLOSIVE_MULTIPLIER = 1;
	public static final int VOWEL_MULTIPLIER = 8;
	public static final int OTHER_MULTIPLIER = 1;
	public static final int CONSONANT_MULTIPLIER = 4;
	public static final int SILENCE_MULTIPLIER = 1;
	public static final int SPEECH_MULTIPLIER = 8;

	public String[][] phoneClasses; // Each row corresponds to a String array of phones that are grouped in the same class
	public GMMTrainerParams[] classTrainerParams; // Training parameters for each context class

	public ContextualGMMParams() {
		this(null, null);
	}

	public ContextualGMMParams(AllophoneSet allophoneSet, GMMTrainerParams commonParams) {
		this(allophoneSet, commonParams, NO_PHONEME_CLASS);
	}

	public ContextualGMMParams(AllophoneSet allophoneSet, GMMTrainerParams commonParams, int contextClassificationTypeIn) {
		// To do: Use contextClassificationType to actually create classes here
		contextClassificationType = contextClassificationTypeIn;

		if (allophoneSet != null) {
			Set<String> tmpPhonemes = allophoneSet.getAllophoneNames();

			allocate(tmpPhonemes.size());
			int count = 0;
			Allophone[] phns = new Allophone[tmpPhonemes.size()];
			for (Iterator<String> it = tmpPhonemes.iterator(); it.hasNext();) {
				phns[count] = allophoneSet.getAllophone(it.next());
				count++;

				if (count >= tmpPhonemes.size())
					break;
			}

			setClasses(phns, commonParams);
		} else {
			allocate(0);
		}
	}

	public static Allophone[] getAllophones(AllophoneSet allophoneSet) {
		Set<String> tmpPhonemes = allophoneSet.getAllophoneNames();

		int count = 0;
		Allophone[] phns = new Allophone[tmpPhonemes.size()];
		for (Iterator<String> it = tmpPhonemes.iterator(); it.hasNext();) {
			phns[count] = allophoneSet.getAllophone(it.next());
			count++;

			if (count >= tmpPhonemes.size())
				break;
		}

		return phns;
	}

	public ContextualGMMParams(AllophoneSet allophoneSet, GMMTrainerParams[] params, int contextClassificationTypeIn) {
		// To do: Use contextClassificationType to actually create classes here
		contextClassificationType = contextClassificationTypeIn;

		if (allophoneSet != null) {
			Allophone[] phns = getAllophones(allophoneSet);

			allocate(phns.length);

			setClasses(phns, params);
		} else {
			allocate(0);
		}
	}

	public ContextualGMMParams(int numPhonemeClasses) {
		allocate(numPhonemeClasses);
	}

	public ContextualGMMParams(ContextualGMMParams existing) {
		if (existing != null) {
			if (existing.phoneClasses != null && existing.classTrainerParams != null
					&& existing.phoneClasses.length == existing.classTrainerParams.length) {
				allocate(existing.phoneClasses.length);
				setClasses(existing.phoneClasses, existing.classTrainerParams);
			} else
				allocate(0);
		} else {
			allocate(0);
		}
	}

	public void allocate(int numPhonemeClasses) {
		if (numPhonemeClasses > 0) {
			phoneClasses = new String[numPhonemeClasses][];
			classTrainerParams = new GMMTrainerParams[numPhonemeClasses];
			for (int i = 0; i < numPhonemeClasses; i++)
				classTrainerParams[i] = new GMMTrainerParams();
		} else {
			phoneClasses = null;
			classTrainerParams = null;
		}
	}

	public void setClassFromSinglePhoneme(int classIndex, String phone) {
		setClassFromSinglePhoneme(classIndex, phone, null);
	}

	public void setClassFromSinglePhoneme(int classIndex, String phone, GMMTrainerParams currentClassTrainerParams) {
		String[] phones = new String[1];
		phones[0] = phone;

		setClass(classIndex, phones, currentClassTrainerParams);
	}

	public void setClasses(String[][] phoneClassesIn) {
		if (phoneClassesIn != null) {
			for (int i = 0; i < phoneClassesIn.length; i++)
				setClass(i, phoneClassesIn[i], null);
		}
	}

	public void setClasses(String[][] phoneClassesIn, GMMTrainerParams[] classTrainerParamsIn) {
		if (phoneClassesIn != null && classTrainerParamsIn != null) {
			for (int i = 0; i < Math.min(phoneClassesIn.length, classTrainerParamsIn.length); i++)
				setClass(i, phoneClassesIn[i], classTrainerParamsIn[i]);
		}
	}

	public void setClasses(Allophone[] phns, GMMTrainerParams commonParams) {
		GMMTrainerParams[] params = new GMMTrainerParams[1];
		params[0] = new GMMTrainerParams(commonParams);

		setClasses(phns, params);
	}

	public void setClasses(Allophone[] phns, GMMTrainerParams[] params) {
		if (phns != null) {
			int i;

			// Print phns to a text file for easy comparison
			// StringUtils.writeTextFile(phns, "d:/phns.txt");
			//

			if (contextClassificationType == NO_PHONEME_CLASS) // All phones go to the same class, this is identical to
																// non-contextual GMM training
			{
				phoneClasses = new String[1][phns.length];
				classTrainerParams = new GMMTrainerParams[1];
				classTrainerParams[0] = new GMMTrainerParams(params[0]);

				for (i = 0; i < phns.length; i++)
					phoneClasses[0][i] = phns[i].name();
			} else if (contextClassificationType == SILENCE_SPEECH) {
				int[] phonologyClasses = getPhonologyClasses(phns);
				int[] differentPhonologyClasses = StringUtils.getDifferentItemsList(phonologyClasses);
				int[][] inds = new int[2][];

				// Silences
				inds[0] = findIndices(phonologyClasses, PAUSE);

				// Remaining will be inds[1], i.e. speech
				int j;
				int totalOther = 0;
				for (i = 0; i < phonologyClasses.length; i++) {
					boolean bFound = false;
					for (j = 0; j < inds.length - 1; j++) {
						if (MathUtils.find(inds[j], MathUtils.EQUALS, i) != null) {
							bFound = true;
							break;
						}
					}

					if (!bFound)
						totalOther++;
				}

				int count = 0;
				if (totalOther > 0) {
					inds[1] = new int[totalOther];

					for (i = 0; i < phonologyClasses.length; i++) {
						boolean bFound = false;
						for (j = 0; j < inds.length - 1; j++) {
							if (MathUtils.find(inds[j], MathUtils.EQUALS, i) != null) {
								bFound = true;
								break;
							}
						}

						if (!bFound) {
							inds[1][count] = i;
							count++;
						}

						if (count >= totalOther)
							break;
					}
				}

				int total = 0;
				for (i = 0; i < inds.length; i++) {
					if (inds[i] != null)
						total++;
				}

				phoneClasses = new String[total][];
				classTrainerParams = new GMMTrainerParams[total];

				count = 0;
				for (i = 0; i < total; i++) {
					if (inds[i] != null) {
						phoneClasses[count] = new String[inds[i].length];
						for (j = 0; j < inds[i].length; j++)
							phoneClasses[count][j] = phns[inds[i][j]].name();

						if (i < params.length)
							classTrainerParams[count] = new GMMTrainerParams(params[i]);
						else
							classTrainerParams[count] = new GMMTrainerParams(params[0]);

						if (i == 0)
							classTrainerParams[count].totalComponents *= SILENCE_MULTIPLIER;
						else if (i == 1)
							classTrainerParams[count].totalComponents *= SPEECH_MULTIPLIER;

						count++;
					}
				}
			} else if (contextClassificationType == VOWEL_SILENCE_CONSONANT) {
				int[] phonologyClasses = getPhonologyClasses(phns);
				int[] differentPhonologyClasses = StringUtils.getDifferentItemsList(phonologyClasses);
				int[][] inds = new int[3][];

				// Vowels
				inds[0] = findIndices(phonologyClasses, VOWEL);
				// Silences
				inds[1] = findIndices(phonologyClasses, PAUSE);
				// Remaining will be inds[2], i.e. consonants

				int j;
				int totalOther = 0;
				for (i = 0; i < phonologyClasses.length; i++) {
					boolean bFound = false;
					for (j = 0; j < inds.length - 1; j++) {
						if (MathUtils.find(inds[j], MathUtils.EQUALS, i) != null) {
							bFound = true;
							break;
						}
					}

					if (!bFound)
						totalOther++;
				}

				int count = 0;
				if (totalOther > 0) {
					inds[2] = new int[totalOther];

					for (i = 0; i < phonologyClasses.length; i++) {
						boolean bFound = false;
						for (j = 0; j < inds.length - 1; j++) {
							if (MathUtils.find(inds[j], MathUtils.EQUALS, i) != null) {
								bFound = true;
								break;
							}
						}

						if (!bFound) {
							inds[2][count] = i;
							count++;
						}

						if (count >= totalOther)
							break;
					}
				}

				int total = 0;
				for (i = 0; i < inds.length; i++) {
					if (inds[i] != null)
						total++;
				}

				phoneClasses = new String[total][];
				if (params != null)
					classTrainerParams = new GMMTrainerParams[total];
				else
					classTrainerParams = null;

				count = 0;
				for (i = 0; i < total; i++) {
					if (inds[i] != null) {
						phoneClasses[count] = new String[inds[i].length];
						for (j = 0; j < inds[i].length; j++)
							phoneClasses[count][j] = phns[inds[i][j]].name();

						if (params != null) {
							if (i < params.length)
								classTrainerParams[count] = new GMMTrainerParams(params[i]);
							else
								classTrainerParams[count] = new GMMTrainerParams(params[0]);

							if (i == 0)
								classTrainerParams[count].totalComponents *= VOWEL_MULTIPLIER;
							else if (i == 1)
								classTrainerParams[count].totalComponents *= SILENCE_MULTIPLIER;
							else if (i == 2)
								classTrainerParams[count].totalComponents *= CONSONANT_MULTIPLIER;
						}

						count++;
					}
				}
			} else if (contextClassificationType == PHONOLOGY_CLASS) // Each phonology class goes into a separate class, however
																		// this cannot handle phone replications since labels do
																		// not have phonology information that could be used in
																		// transformation phase
			{
				int[] phonologyClasses = getPhonologyClasses(phns);
				int[] differentPhonologyClasses = StringUtils.getDifferentItemsList(phonologyClasses);

				phoneClasses = new String[differentPhonologyClasses.length][];
				classTrainerParams = new GMMTrainerParams[differentPhonologyClasses.length];

				int j;
				for (i = 0; i < differentPhonologyClasses.length; i++) {
					int[] indices = MathUtils.find(phonologyClasses, MathUtils.EQUALS, differentPhonologyClasses[i]);
					phoneClasses[i] = new String[indices.length];
					if (i < params.length)
						classTrainerParams[i] = new GMMTrainerParams(params[i]);
					else
						classTrainerParams[i] = new GMMTrainerParams(params[0]);

					for (j = 0; j < indices.length; j++)
						phoneClasses[i][j] = phns[indices[j]].name();
				}
			} else if (contextClassificationType == FRICATIVE_GLIDELIQUID_NASAL_PLOSIVE_VOWEL_OTHER) {
				int[] phonologyClasses = getPhonologyClasses(phns);
				int[] differentPhonologyClasses = StringUtils.getDifferentItemsList(phonologyClasses);
				int[][] inds = new int[6][];

				// Fricatives
				inds[0] = findIndices(phonologyClasses, FRICATIVE);
				// Glide or liquids
				int[] tmpInds1 = findIndices(phonologyClasses, GLIDE);
				int[] tmpInds2 = findIndices(phonologyClasses, LIQUID);
				int[] tmpInds = SignalProcUtils.merge(tmpInds1, tmpInds2);
				MathUtils.quickSort(tmpInds);
				inds[1] = StringUtils.getDifferentItemsList(tmpInds);

				// Nasals
				inds[2] = findIndices(phonologyClasses, NASAL);
				// Plosives
				inds[3] = findIndices(phonologyClasses, PLOSIVE);
				// Vowels
				inds[4] = findIndices(phonologyClasses, VOWEL);
				// Remaining will be other in inds[5]

				int j;
				int totalOther = 0;
				for (i = 0; i < phonologyClasses.length; i++) {
					boolean bFound = false;
					for (j = 0; j < inds.length - 1; j++) {
						if (MathUtils.find(inds[j], MathUtils.EQUALS, i) != null) {
							bFound = true;
							break;
						}
					}

					if (!bFound)
						totalOther++;
				}

				int count = 0;
				if (totalOther > 0) {
					inds[5] = new int[totalOther];

					for (i = 0; i < phonologyClasses.length; i++) {
						boolean bFound = false;
						for (j = 0; j < inds.length - 1; j++) {
							if (MathUtils.find(inds[j], MathUtils.EQUALS, i) != null) {
								bFound = true;
								break;
							}
						}

						if (!bFound) {
							inds[5][count] = i;
							count++;
						}

						if (count >= totalOther)
							break;
					}
				}

				int total = 0;
				for (i = 0; i < inds.length; i++) {
					if (inds[i] != null)
						total++;
				}

				phoneClasses = new String[total][];
				classTrainerParams = new GMMTrainerParams[total];

				count = 0;
				for (i = 0; i < total; i++) {
					if (inds[i] != null) {
						phoneClasses[count] = new String[inds[i].length];
						for (j = 0; j < inds[i].length; j++)
							phoneClasses[count][j] = phns[inds[i][j]].name();

						if (i < params.length)
							classTrainerParams[count] = new GMMTrainerParams(params[i]);
						else
							classTrainerParams[count] = new GMMTrainerParams(params[0]);

						if (i == 0)
							classTrainerParams[count].totalComponents *= FRICATIVE_MULTIPLIER;
						else if (i == 1)
							classTrainerParams[count].totalComponents *= GLIDELIQUID_MULTIPLIER;
						else if (i == 2)
							classTrainerParams[count].totalComponents *= NASAL_MULTIPLIER;
						else if (i == 3)
							classTrainerParams[count].totalComponents *= PLOSIVE_MULTIPLIER;
						else if (i == 4)
							classTrainerParams[count].totalComponents *= VOWEL_MULTIPLIER;
						else if (i == 5)
							classTrainerParams[count].totalComponents *= OTHER_MULTIPLIER;

						count++;
					}
				}
			} else if (contextClassificationType == PHONEME_IDENTITY) // Each phone goes into a separate class, phone replications
																		// are taken care of
			{
				String[] allPhonemes = new String[phns.length];
				for (i = 0; i < phns.length; i++)
					allPhonemes[i] = phns[i].name();

				String[] differentPhonemes = StringUtils.getDifferentItemsList(allPhonemes);

				phoneClasses = new String[differentPhonemes.length][1];
				classTrainerParams = new GMMTrainerParams[differentPhonemes.length];

				for (i = 0; i < differentPhonemes.length; i++) {
					phoneClasses[i][0] = differentPhonemes[i];
					if (i < params.length)
						classTrainerParams[i] = new GMMTrainerParams(params[i]);
					else
						classTrainerParams[i] = new GMMTrainerParams(params[0]);
				}
			} else {
				phoneClasses = null;
				classTrainerParams = null;
			}
		}
	}

	public static int[] getPhonologyClasses(Allophone[] phns) {
		int[] phonologyClasses = null;

		if (phns != null) {
			phonologyClasses = new int[phns.length];

			for (int i = 0; i < phns.length; i++) {
				phonologyClasses[i] = 0;
				if (phns[i].isFricative())
					phonologyClasses[i] += FRICATIVE;

				if (phns[i].isGlide())
					phonologyClasses[i] += GLIDE;

				if (phns[i].isLiquid())
					phonologyClasses[i] += LIQUID;

				if (phns[i].isNasal())
					phonologyClasses[i] += NASAL;

				if (phns[i].isPause())
					phonologyClasses[i] += PAUSE;

				if (phns[i].isPlosive())
					phonologyClasses[i] += PLOSIVE;

				if (phns[i].isSonorant())
					phonologyClasses[i] += SONORANT;

				if (phns[i].isSyllabic())
					phonologyClasses[i] += SYLLABIC;

				if (phns[i].isVoiced())
					phonologyClasses[i] += VOICED;

				if (phns[i].isVowel())
					phonologyClasses[i] += VOWEL;
			}
		}

		return phonologyClasses;
	}

	public static int[] findIndices(int[] phonologyClasses, int desiredClasses) {
		int[] indices = null;

		boolean[] desireds = new boolean[phonologyClasses.length];
		int i;
		int total = 0;
		for (i = 0; i < phonologyClasses.length; i++) {
			desireds[i] = StringUtils.isDesired(phonologyClasses[i], desiredClasses);
			if (desireds[i])
				total++;
		}

		if (total > 0) {
			indices = new int[total];
			int count = 0;
			for (i = 0; i < desireds.length; i++) {
				if (desireds[i]) {
					indices[count] = i;
					count++;

					if (count >= total)
						break;
				}
			}
		}

		return indices;
	}

	public void setClass(int classIndex, String[] phones) {
		setClass(classIndex, phones, null);
	}

	public void setClass(int classIndex, String[] phones, GMMTrainerParams currentClassTrainerParams) {
		if (phoneClasses != null && classTrainerParams != null && classIndex >= 0 && classIndex < phoneClasses.length
				&& phoneClasses.length == classTrainerParams.length) {
			phoneClasses[classIndex] = null;

			if (phones != null) {
				phoneClasses[classIndex] = new String[phones.length];
				for (int i = 0; i < phones.length; i++)
					phoneClasses[classIndex][i] = phones[i];
			}

			classTrainerParams[classIndex] = new GMMTrainerParams(currentClassTrainerParams);
		}
	}

	// Returns the zero based index of the class the phone belongs to
	// If it is not an element of any of the existing classes, -1 is returned
	public int getClassIndex(String phone) {
		int classInd = -1;

		if (phoneClasses != null) {
			int i, j;

			for (i = 0; i < phoneClasses.length; i++) {
				if (phoneClasses[i] != null) {
					for (j = 0; j < phoneClasses[i].length; j++) {
						if (phone.compareTo(phoneClasses[i][j]) == 0)
							return i;
					}
				}
			}
		}

		return classInd;
	}

	public void write(MaryRandomAccessFile stream) {
		if (stream != null) {
			if (phoneClasses != null) {
				int i, j;
				try {
					stream.writeInt(phoneClasses.length);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

				for (i = 0; i < phoneClasses.length; i++) {
					if (phoneClasses[i].length > 0) {
						try {
							stream.writeInt(phoneClasses[i].length);
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}

						for (j = 0; j < phoneClasses[i].length; j++) {
							if (phoneClasses[i][j].length() > 0) {
								try {
									stream.writeInt(phoneClasses[i][j].length());
								} catch (IOException e) {
									// TODO Auto-generated catch block
									e.printStackTrace();
								}

								try {
									stream.writeChar(phoneClasses[i][j].toCharArray());
								} catch (IOException e) {
									// TODO Auto-generated catch block
									e.printStackTrace();
								}
							} else {
								try {
									stream.writeInt(0);
								} catch (IOException e) {
									// TODO Auto-generated catch block
									e.printStackTrace();
								}
							}
						}
					} else {
						try {
							stream.writeInt(0);
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
				}

				if (classTrainerParams != null) {
					if (classTrainerParams.length > 0) {
						try {
							stream.writeInt(classTrainerParams.length);
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}

						for (i = 0; i < classTrainerParams.length; i++)
							classTrainerParams[i].write(stream);
					} else {
						try {
							stream.writeInt(0);
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
				} else {
					try {
						stream.writeInt(0);
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			} else {
				try {
					stream.writeInt(0);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
	}

	public void read(MaryRandomAccessFile stream) {
		if (stream != null) {
			int tmpLen = 0;
			try {
				tmpLen = stream.readInt();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			if (tmpLen > 0)
				phoneClasses = new String[tmpLen][];

			if (phoneClasses != null) {
				int i, j;

				for (i = 0; i < phoneClasses.length; i++) {
					tmpLen = 0;
					try {
						tmpLen = stream.readInt();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}

					if (tmpLen > 0)
						phoneClasses[i] = new String[tmpLen];

					if (phoneClasses[i].length > 0) {
						for (j = 0; j < phoneClasses[i].length; j++) {
							try {
								tmpLen = stream.readInt();
							} catch (IOException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}

							if (tmpLen > 0) {
								try {
									phoneClasses[i][j] = String.copyValueOf(stream.readChar(tmpLen));
								} catch (IOException e) {
									// TODO Auto-generated catch block
									e.printStackTrace();
								}
							}
						}
					}
				}

				tmpLen = 0;
				try {
					tmpLen = stream.readInt();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

				classTrainerParams = null;
				if (tmpLen > 0)
					classTrainerParams = new GMMTrainerParams[tmpLen];

				if (classTrainerParams.length > 0) {
					for (i = 0; i < classTrainerParams.length; i++)
						classTrainerParams[i] = new GMMTrainerParams(stream);
				}
			}
		}
	}
}
