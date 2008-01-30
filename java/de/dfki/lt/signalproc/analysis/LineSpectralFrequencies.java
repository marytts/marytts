/**
 * Copyright 2004-2006 DFKI GmbH.
 * All Rights Reserved.  Use is subject to license terms.
 * 
 * Permission is hereby granted, free of charge, to use and distribute
 * this software and its documentation without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of this work, and to
 * permit persons to whom this work is furnished to do so, subject to
 * the following conditions:
 * 
 * 1. The code must retain the above copyright notice, this list of
 *    conditions and the following disclaimer.
 * 2. Any modifications must be clearly marked as such.
 * 3. Original authors' names are not deleted.
 * 4. The authors' names are not used to endorse or promote products
 *    derived from this software without specific prior written
 *    permission.
 *
 * DFKI GMBH AND THE CONTRIBUTORS TO THIS WORK DISCLAIM ALL WARRANTIES WITH
 * REGARD TO THIS SOFTWARE, INCLUDING ALL IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS, IN NO EVENT SHALL DFKI GMBH NOR THE
 * CONTRIBUTORS BE LIABLE FOR ANY SPECIAL, INDIRECT OR CONSEQUENTIAL
 * DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR
 * PROFITS, WHETHER IN AN ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS
 * ACTION, ARISING OUT OF OR IN CONNECTION WITH THE USE OR PERFORMANCE OF
 * THIS SOFTWARE.
 */

package de.dfki.lt.signalproc.analysis;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Arrays;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;

import de.dfki.lt.signalproc.analysis.LPCAnalyser.LPCoeffs;
import de.dfki.lt.signalproc.util.AudioDoubleDataSource;
import de.dfki.lt.signalproc.util.Defaults;
import de.dfki.lt.signalproc.util.MaryRandomAccessFile;
import de.dfki.lt.signalproc.util.MathUtils;
import de.dfki.lt.signalproc.util.SignalProcUtils;
import de.dfki.lt.signalproc.window.DynamicWindow;
import de.dfki.lt.signalproc.window.Window;

/* Demonstration program to accompany the subroutines described in the        */
/* articles by J. Rothweiler, on computing the Line Spectral Frequencies.     */
/* From http://mysite.verizon.net/vzenxj75/myown1/joe/lsf/a2lsp.c             */

public class LineSpectralFrequencies
{
    /* Operations counters. Not needed in a real application. */
    static int mpy=0;
    static int add=0;
    static int ptr=0;

    /**
     * Convert filter coefficients to lsp coefficients.
     * @param oneMinusA A(z) = a0 - sum { ai * z^-i } . a0 = 1.
     * @param type which of the four methods for a2lsf conversion to perform
     * @return the lsf coefficients in the range 0 to 0.5*samplingRate,
     * as an array of doubles of length oneMinusA.length-1.
     */
    public static double[] lpc2lsfInHz(double[] oneMinusA, int samplingRate)
    {
        return lpc2lsfInHz(oneMinusA, samplingRate, 4);
    }
    
    public static double[] lpc2lsfInHz(double[] oneMinusA, int samplingRate, int type)
    {
        double [] lsp = lpc2lsf(oneMinusA, type);
        
        for (int i=0; i<lsp.length; i++)
            lsp[i] *= samplingRate;
        
        return lsp;
    }
    
    /**
     * Convert filter coefficients to lsp coefficients.
     * @param oneMinusA A(z) = a0 - sum { ai * z^-i } . a0 = 1.
     * @param type which of the four methods for a2lsf conversion to perform
     * @return the lsf coefficients in the range 0 to 0.5,
     * as an array of doubles of length oneMinusA.length-1.
     */
    public static double[] lpc2lsf(double[] oneMinusA, int type)
    {
        int order = oneMinusA.length - 1;
        double[] g1 = new double[100];
        double[] g2 = new double[100];
        double[] g1r = new double[100];
        double[] g2r = new double[100];
        boolean even;
        int g1_order, g2_order;
        int orderd2;
        
        int i, j;
        int swap;
        double Factor;

        /* Compute the lengths of the x polynomials. */

        even = (order & 1) == 0;  /* True if order is even. */
        if(even) g1_order = g2_order = order/2;
        else {
            g1_order = (order+1)/2;
            g2_order = g1_order - 1;
            throw new IllegalArgumentException("Odd order not implemented yet");
        }

        /* Compute the first half of K & R F1 & F2 polynomials. */

        /* Compute half of the symmetric and antisymmetric polynomials. */
        /* Remove the roots at +1 and -1. */

        orderd2=(order+1)/2;
        g1[orderd2] = oneMinusA[0];
        for(i=1;i<=orderd2;i++) g1[g1_order-i] = oneMinusA[i]+oneMinusA[order+1-i];
        g2[orderd2] = oneMinusA[0];
        for(i=1;i<=orderd2;i++) g2[orderd2-i] = oneMinusA[i]-oneMinusA[order+1-i];

        if(even) {
            for(i=1; i<=orderd2;i++) g1[orderd2-i] -= g1[orderd2-i+1];
            for(i=1; i<=orderd2;i++) g2[orderd2-i] += g2[orderd2-i+1];
        } else {
            for(i=2; i<=orderd2;i++) g2[orderd2-i] += g2[orderd2-i+2];   /* Right? */
        }

        /* Convert into polynomials in cos(alpha) */

        if(type == 1) {
            //System.out.println("Implementing chebyshev reduction\n");
            cheby1(g1,g1_order);
            cheby1(g2,g2_order);
            Factor = 0.5;
        } else if(type == 2) {
            //System.out.println("Implementing first alternate chebyshev reduction\n");
            cheby2(g1,g1_order);
            cheby2(g2,g2_order);
            Factor = 0.5;
        } else if(type == 3) {
            //System.out.println("Implementing second alternate chebyshev reduction\n");
            cheby3(g1,g1_order);
            cheby3(g2,g2_order);
            Factor = 1.0;
        } else if(type == 4) {
            //System.out.println("Implementing DID reduction\n");
            kw(g1,g1_order);
            kw(g2,g2_order);
            Factor = 0.5;
        } else {
            throw new IllegalArgumentException("valid type values are 1 to 4.\n");
        }
        /* Print the polynomials to be reduced. */
        //for(i=0;i<=g1_order;i++) {
        //    System.out.printf("%3d: %14.6g", new Object[] {new Integer(i), new Double(g1[i])});
        //    if(i<=g2_order) System.out.printf(" %14.6g",new Object[] {new Double(g2[i])});
        //    System.out.println();
        //}

        /* Find the roots of the 2 even polynomials.*/

        cacm283(g1,g1_order,g1r);
        cacm283(g2,g2_order,g2r);

        /* Convert back to angular frequencies in the range 0 to 0.5 */
        double[] lsp = new double[order];
        for(i=0, j=0 ; ; ) {
            lsp[j++] = Math.acos(Factor * g1r[i])/MathUtils.TWOPI;
            if(j >= order) break;
            lsp[j++] = Math.acos(Factor * g2r[i])/MathUtils.TWOPI;
            if(j >= order) break;
            i++;
        }
        return lsp;
    }

    /* The transformation as proposed in the paper. */
    static void cheby1(double[] g, int ord) {
        int i, j;
        int k;

        for(i=2; i<= ord; i++) {
            for(j=ord; j > i; j--) {
                g[j-2] -= g[j];           add++;
            }
            g[j-2] -= 2.0*g[j];           mpy++; add++;
            /* for(k=0;k<=ord;k++) printf(" %6.3f",g[k]); printf("\n"); */
        }
    }

    /* An alternate transformation giving roots between -2 and +2. */
    static void cheby2(double[] g, int ord) {
        int i, j;
        int k;

        g[0] *= 0.5;                              mpy++;
        for(i=2; i<= ord; i++) {
            for(j=ord; j >= i; j--) {
                g[j-2] -= g[j];           add++;
            }
            g[i-1] *= 0.5;                    mpy++;
            /* for(k=0;k<=ord;k++) printf(" %6.3f",g[k]); printf("\n"); */
        }
        g[ord] *= 0.5;                    mpy++;
    }

    /* Another transformation giving roots between -1 and +1. */
    static void cheby3(double[] g, int ord) {
        int i, j;
        int k;

        g[0] *= 0.5;                              mpy++;
        for(i=2; i<= ord; i++) {
            for(j=ord; j >= i; j--) {
                g[j-2] -= g[j];           add++;
                g[j] += g[j];             add++;
            }
            /* for(k=0;k<=ord;k++) printf(" %6.3f",g[k]); printf("\n"); */
        }
    }

    /* The transformation as proposed by Wu and Chen. */
    static void kw(double[] r, int n) {
        double[] s = new double[100];
        double[] c = new double[100];
        int i, j, k;

        s[0] = 1.0;
        s[1] = -2.0;
        s[2] = 2.0;
        for(i=3;i<=n/2;i++) s[i] = s[i-2];

        for(k=0;k<=n;k++) {
            c[k] = r[k];
            j = 1;
            for(i=k+2;i<=n;i+=2) {
                c[k] += s[j]*r[i];        mpy++; add++;
                s[j] -= s[j-1];           add++;
                j++;                      ptr++;
            }
        }
        for(k=0;k<=n;k++) r[k] = c[k];
    }


    /* A simple rootfinding algorithm, as published in the Collected Algorithms of*/
    /* The Association for Computing Machinery, CACM algorithm 283.               */
    /* It's basically a Newton iteration, that applies optimization steps to all  */
    /* root estimates together. It is stated to work for polynomials whose roots  */
    /* are all real and distinct.  I know of no proof of global convergence, but  */
    /* in practice it has always worked for the LSF rootfinding problem, although */
    /* there may be an initial period of wild divergence before it starts         */
    /* converging. */
    static void cacm283(
    double[] a,    /* Input array of coefficients. Length ord+1. */
    int ord,
    double[] r     /* Holds the found roots. */
    )
    {
        int i, k;
        double val, p, delta, error;
        double rooti;
        int swap;

        for(i=0; i<ord;i++) r[i] = 2.0 * (i+0.5) / ord - 1.0;

        for(error=1 ; error > 1.e-12; ) {

            error = 0;
            for( i=0; i<ord; i++) {  /* Update each point. */
                rooti = r[i];
                val = a[ord];
                p = a[ord];
                for(k=ord-1; k>= 0; k--) {
                    val = val * rooti + a[k];
                    if (k != i) p *= rooti - r[k];
                }
                delta = val/p;
                r[i] -= delta;
                error += delta*delta;
            }
        }

        /* Do a simple bubble sort to get them in the right order. */
        do {
            double tmplsp;
            swap = 0;
            for(i=0; i<ord-1;i++) {
                if(r[i] < r[i+1]) {
                    tmplsp = r[i];
                    r[i]=r[i+1];
                    r[i+1]=tmplsp;
                    swap++;
                }
            }
        } while (swap > 0);
    }

    public static void main2(String[] argv) {
        /* Random set of coefficients. Should represent a stable filter */
            /* For any order up to 24. */
        double[] awc = {
            1.0,  0.1077, -0.0424, 0.1737, -0.0278,  0.1759,
        -0.1990, -0.0333, -0.1904, 0.0759,  0.0278, -0.0568,
            -0.1325,   0.001, -0.001,  0.001,  -0.001,   0.001,
            -0.0005,   0.001, -0.001,  0.001,  -0.001,   0.001,
             0.0002,
        };
        double[] a = new double[25];
        double[]lsp = new double[24];
        int i;
        int type = 0;
        int ord = 12;

        if(argv.length < 1) {
            System.err.println("command: al2sp type order\n");
            System.err.println("type is 1 to 4\n");
            System.err.println("order is 2 to 24\n");
            System.exit(2);
        }
        /* Allow different types of computation, and model orders. */
        if(argv.length >= 1) type = Integer.parseInt(argv[0]);  /* 1-4 are valid types. */
        if(argv.length >= 2) ord = Integer.parseInt(argv[1]);

        if(ord > 24) {
            throw new IllegalArgumentException("Model order is 24 max.\n");
        } 

        /* Copy the coefficients into a working array. */
        a[0] = 1.0;
        for(i=1;i<=ord;i++) a[i] = -awc[i];
        lsp = lpc2lsf(a,type);

        for(i=0;i<ord;i++) System.out.println(i+": "+lsp[i]);
        System.out.println("mpy " + mpy + " add " + add + " ptr " + ptr);
    }
    
//  The usage is:
//  lsf_to_pc(lsf, pc, P, fs/2.0f);
//  where <lsf> and <pc> are arrays of size <P>
//  and <P> is the linear prediction order
//  bandwidth should be taken as half the sampling rate
    /**
     * Convert LSF frequencies into LPC coefficients.
     * The analysis filter may be reconstructed:
     *       A(z) = 1/2 [ P(z) + Q(z) ]
     * @param lsf the array of lsf coefficients, in Hertz frequencies
     * @param samplingRate the sampling rate of the underlying audio data
     * @return an array of length lsf.length+1, containing the LPC coefficients
     * as in A(z) = a0 - sum { ai * z^-i } . a0 = 1.
     */
    public static double[] lsfInHz2lpc(double[] lsf, int samplingRate)
    {
     
        double[] normalised_lsf = new double[lsf.length];
        for (int i=0; i<lsf.length; i++) {
            normalised_lsf[i] = lsf[i]/samplingRate;
            assert 0 <= normalised_lsf[i];
            assert normalised_lsf[i] <= 0.5;
        }
        return lsf2lpc(normalised_lsf);
    }


    /**
     * Convert LSF frequencies into LPC coefficients.
     * The analysis filter may be reconstructed:
     *       A(z) = 1/2 [ P(z) + Q(z) ]
     * @param lsf the array of lsf coefficients, in the range 0 to 0.5
     * @return an array of length lsf.length+1, containing the LPC coefficients
     * as in A(z) = a0 - sum { ai * z^-i } . a0 = 1.
     */
    public static double[] lsf2lpc(double[] lsf)
    {
        int P = lsf.length;
        int half_order = P/2;
        int i, j;
        double xf, xx;
        double[] a = new double[P/2 +1];
        double[] a1 = new double[P/2 +1];
        double[] a2 = new double[P/2 +1];
        double[] b = new double[P/2 +1];
        double[] b1 = new double[P/2 +1];
        double[] b2 = new double[P/2 +1];
        double[] p = new double[P/2];
        double[] q = new double[P/2];
     
        // Result array to be constructed, as A(z) = a0 - sum { ai * z^-i } . a0 = 1.
        double[] oneMinusA = new double[P+1];
        oneMinusA[0] = 1.;
     
        //  Check input for ill-conditioned cases
        if( (lsf[0] <= 0.0) || (lsf[0] >= 0.5) )   {
            throw new IllegalArgumentException("LSFs out of bounds; lsf[0] = "+lsf[0]);
        }
        for(i=1; i<P; i++) {
            if(lsf[i] <= lsf[i-1])
                throw new IllegalArgumentException("nonmonotonic LSFs");
            if( (lsf[i] <= 0.0) || (lsf[i] >= 0.5) )
                throw new IllegalArgumentException("LSFs out of bounds; lsf["+i+"] = "+lsf[i]);
        }
     
        // LSF filter parameters
        for(i=0;i<half_order;i++) {
            p[i] = -2 * Math.cos(MathUtils.TWOPI * lsf[2 * i]);
            q[i] = -2 * Math.cos(MathUtils.TWOPI * lsf[2 * i + 1]);
        }
     
        // Impulse response of analysis filter
        xf = 0.0;
        for(i=0;i<=P;i++) {
            if (i == 0) xx = 1.0;
            else xx = 0.0; 
            a[0] = xx + xf;
            b[0] = xx - xf;
            xf = xx;
            for(j=0;j<half_order;j++) {
                a[j+1] = a[j] + p[j] * a1[j] + a2[j];
                b[j+1] = b[j] + q[j] * b1[j] + b2[j];
                a2[j] = a1[j];
                a1[j] = a[j];
                b2[j] = b1[j];
                b1[j] = b[j];
            }
            if(i > 0) oneMinusA[i] = 0.5 * (a[half_order] + b[half_order]);
        }  
        return oneMinusA;
    }
    
    public static double[][] lsfAnalyzeWavFile(String wavFile, LsfFileHeader params) throws UnsupportedAudioFileException, IOException
    {
        AudioInputStream inputAudio = AudioSystem.getAudioInputStream(new File(wavFile));
        params.samplingRate = (int)inputAudio.getFormat().getSampleRate();
        
        int ws =  (int)Math.floor(params.winsize*params.samplingRate+0.5);
        int ss = (int)Math.floor(params.skipsize*params.samplingRate+0.5);

        if (params.lpOrder<1)
            params.lpOrder = SignalProcUtils.getLPOrder(params.samplingRate);
        
        AudioDoubleDataSource signal = new AudioDoubleDataSource(inputAudio);
        double[] x = signal.getAllData();
        
        double[] frm = new double[ws];
        int numfrm = (int)Math.floor((x.length-ws)/((double)ss)+0.5);
        if (numfrm>0)
            params.numfrm = numfrm;
        else
            params.numfrm = 0;
        
        double[][] lsfs = new double[params.numfrm][params.lpOrder];
        
        double[] wgt;
        int j;
        for (int i=0; i<params.numfrm; i++)
        {
            Arrays.fill(frm, 0.0);
            System.arraycopy(x, i*ss, frm, 0, Math.min(ws, x.length-i*ss));
           
            lsfs[i] = nonPreemphasizedFrame2LsfsInHz(frm, params.lpOrder, params.samplingRate, params.windowType, params.preCoef);
        }
       
        return lsfs;
    }
    
    public static double[] nonPreemphasizedFrame2Lpcs(double[] nonPreemphasizedFrame, int lpOrder, int samplingRate, int windowType, float preCoef)
    {
        double[] preemphasizedFrame = SignalProcUtils.applyPreemphasis(nonPreemphasizedFrame, preCoef);
        
        return preemphasizedFrame2Lpcs(preemphasizedFrame, lpOrder, samplingRate, windowType);
    }
    
    public static double[] preemphasizedFrame2Lpcs(double[] preemphasizedFrame, int lpOrder, int samplingRate, int windowType)
    {                    
        DynamicWindow window = new DynamicWindow(windowType);
        double[] wgt = window.values(preemphasizedFrame.length);
        double[] windowedAndPreemphasizedFrame = new double[preemphasizedFrame.length];
        
        for (int j=0; j<preemphasizedFrame.length; j++)
            windowedAndPreemphasizedFrame[j] = preemphasizedFrame[j]*wgt[j]; //Windowing


        return windowedAndPreemphasizedFrame2Lpcs(windowedAndPreemphasizedFrame, lpOrder, samplingRate);
    }
    
    public static double[] windowedAndPreemphasizedFrame2Lpcs(double[] windowedAndPreemphasizedFrame, int lpOrder, int samplingRate)
    {
        //LPC and LSF analysis
        LPCoeffs l = LPCAnalyser.calcLPC(windowedAndPreemphasizedFrame, lpOrder);
        return l.getOneMinusA();
    }
    
    public static double[] nonPreemphasizedFrame2LsfsInHz(double[] nonPreemphasizedFrame, int lpOrder, int samplingRate, int windowType, float preCoef)
    {
        double[] preemphasizedFrame = SignalProcUtils.applyPreemphasis(nonPreemphasizedFrame, preCoef);

        return preemphasizedFrame2LsfsInHz(preemphasizedFrame, lpOrder, samplingRate, windowType);
    }
    
    public static double[] preemphasizedFrame2LsfsInHz(double[] preemphasizedFrame, int lpOrder, int samplingRate, int windowType)
    {  
        DynamicWindow window = new DynamicWindow(windowType);
        double[] wgt = window.values(preemphasizedFrame.length);
        double[] windowedAndPreemphasizedFrame = new double[preemphasizedFrame.length];
        
        for (int j=0; j<windowedAndPreemphasizedFrame.length; j++)
            windowedAndPreemphasizedFrame[j] = preemphasizedFrame[j]*wgt[j]; //Windowing
        

        return windowedAndPreemphasizedFrame2LsfsInHz(windowedAndPreemphasizedFrame, lpOrder, samplingRate);
    }
    
    public static double[] windowedAndPreemphasizedFrame2LsfsInHz(double[] windowedAndPreemphasizedFrame, int lpOrder, int samplingRate)
    {
        double [] lpcs = windowedAndPreemphasizedFrame2Lpcs(windowedAndPreemphasizedFrame, lpOrder, samplingRate);
        
        return LineSpectralFrequencies.lpc2lsfInHz(lpcs, samplingRate);
    }
    
    public static void lsfAnalyzeWavFile(String wavFileIn, String lsfFileOut, LsfFileHeader params) throws IOException
    {
        double[][] lsfs = null;
        try {
            lsfs = lsfAnalyzeWavFile(wavFileIn, params);
        } catch (UnsupportedAudioFileException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        
        if (lsfs!=null)
        {
            params.numfrm = lsfs.length;
            writeLsfFile(lsfs, lsfFileOut, params);
        }
        else
            params.numfrm = 0;
    }
    
    public static void writeLsfFile(double[][] lsfs, String lsfFileOut, LsfFileHeader params) throws IOException
    {
        params.numfrm = lsfs.length;
        MaryRandomAccessFile stream = params.writeLsfHeader(lsfFileOut, true);
        writeLsfs(stream, lsfs);
    }
    
    public static void writeLsfs(MaryRandomAccessFile stream, double[][] lsfs) throws IOException
    {
        if (stream!=null && lsfs!=null && lsfs.length>0)
        {
            for (int i=0; i<lsfs.length; i++)
                stream.writeDouble(lsfs[i]);
            
            stream.close();
        }
    }
    
    public static double[][] readLsfFile(String lsfFile) throws IOException
    {
        LsfFileHeader params = new LsfFileHeader();
        MaryRandomAccessFile stream = params.readLsfHeader(lsfFile, true);
        return readLsfs(stream, params);
    }
    
    public static double[][] readLsfs(MaryRandomAccessFile stream, LsfFileHeader params) throws IOException
    {
        double[][] lsfs = null;
        
        if (stream!=null && params.numfrm>0 && params.lpOrder>0)
        {
            lsfs = new double[params.numfrm][];
            
            for (int i=0; i<lsfs.length; i++)
                lsfs[i] = stream.readDouble(params.lpOrder);
            
            stream.close();
        }
        
        return lsfs;
    }
    
    public static void main(String[] args) throws Exception
    {
        int windowSize = Defaults.getWindowSize();
        int windowType = Defaults.getWindowType();
        int fftSize = Defaults.getFFTSize();
        int frameShift = Defaults.getFrameShift();
        int p = Integer.getInteger("signalproc.lpcorder", 24).intValue();
        int pre = p;
        AudioInputStream inputAudio = AudioSystem.getAudioInputStream(new File(args[0]));
        int samplingRate = (int)inputAudio.getFormat().getSampleRate();
        AudioDoubleDataSource signal = new AudioDoubleDataSource(inputAudio);
        LPCAnalyser lpcAnalyser = new LPCAnalyser(signal, windowSize, frameShift, samplingRate);
        FrameBasedAnalyser.FrameAnalysisResult[] results = lpcAnalyser.analyseAllFrames();
        for (int i=0; i<results.length; i++) {
            System.out.println("Line spectral frequencies for frame "+i+":");            
            double[] lpc = ((LPCAnalyser.LPCoeffs)results[i].get()).getOneMinusA();
            double[] lsf = lpc2lsf(lpc,4);
            for (int j=0; j<lsf.length; j++) {
                System.out.println(j+": "+lsf[j]+" = "+lsf[j]*samplingRate);
            }
            double[] lpc_reconstructed = lsf2lpc(lsf);
            System.out.println("LPC coefficients (orig/reconstructed from LSF):");
            for (int j=0; j<lpc.length; j++) {
                System.out.println(lpc[j] + " " + lpc_reconstructed[j]);
            }
        }
    }
}
