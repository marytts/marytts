package de.dfki.lt.signalproc;

import de.dfki.lt.signalproc.util.MathUtils.Complex;

public class FFTArbitraryLength {
    private static int maxPrimeFactor = 2999;
    private static int maxPrimeFactorDiv2 = (2999+1)/2;
    private static int maxFactorCount = 20;

    private static double c3_1 = -1.5000000000000E+00; /* c3_1 = cos(2*pi/3)-1;     */
    private static double c3_2 = 8.6602540378444E-01; /* c3_2 = sin(2*pi/3);      */

    private static double u5  = 1.2566370614359E+00; /* u5  = 2*pi/5;         */
    private static double c5_1 = -1.2500000000000E+00; /* c5_1 = (cos(u5)+cos(2*u5))/2-1;*/
    private static double c5_2 = 5.5901699437495E-01; /* c5_2 = (cos(u5)-cos(2*u5))/2; */
    private static double c5_3 = -9.5105651629515E-01; /* c5_3 = -sin(u5);        */
    private static double c5_4 = -1.5388417685876E+00; /* c5_4 = -(sin(u5)+sin(2*u5));  */
    private static double c5_5 = 3.6327126400268E-01; /* c5_5 = (sin(u5)-sin(2*u5));  */
    private static double c8  = 7.0710678118655E-01; /* c8 = 1/sqrt(2);  */

    private static double pi;
    private static int groupOffset,dataOffset,blockOffset,adr;
    private static int groupNo,dataNo,blockNo,twNo;
    private static double omega, tw_re,tw_im;

    private static double [] twiddleRe;
    private static double [] twiddleIm;
    private static double [] trigRe;
    private static double [] trigIm;
    private static double [] zRe;
    private static double [] zIm;
    private static double [] vRe;
    private static double [] vIm;
    private static double [] wRe;
    private static double [] wIm;
    private static double yReTmp;
    private static double yImTmp;

    protected static void factorize(int n, int [] nFact, int [] fact)
    {
        int i,j,k;
        int nRadix;
        int [] radices = new int[7];
        int [] factors = new int[maxFactorCount];

        nRadix  = 6; 
        radices[1]= 2;
        radices[2]= 3;
        radices[3]= 4;
        radices[4]= 5;
        radices[5]= 8;
        radices[6]= 10;

        if (n==1)
        {
            j=1;
            factors[1]=1;
        }
        else 
            j=0;

        i=nRadix;
        while ((n>1) && (i>0))
        {
            if ((n % radices[i]) == 0)
            {
                n=n / radices[i];
                j=j+1;
                factors[j]=radices[i];
            }
            else 
                i--;
        }

        if (factors[j] == 2)  /*substitute factors 2*8 with 4*4 */
        {  
            i = j-1;
            while ((i>0) && (factors[i] != 8)) i--;
            if (i>0)
            {
                factors[j] = 4;
                factors[i] = 4;
            }
        }
        if (n>1)
        {
            for (k=2; k<Math.sqrt((double)n)+1; k++)
                while ((n % k) == 0)
                {
                    n /= k;
                    j++;
                    factors[j]=k;
                }
            if (n>1)
            {
                j++;
                factors[j]=n;
            }
        }  

        for (i=1; i<=j; i++)     
        {
            fact[i] = factors[j-i+1];
        }

        nFact[0] = j;
    }


    //After N is factored the parameters that control the stages are generated.
    //For each stage we have:
    // sofar  : the product of the radices so far
    // actual : the radix handled in this stage
    // remain : the product of the remaining radices
    protected static void transTableSetup(int [] sofar, int [] actual, int [] remain, int [] nFact, int [] nPoints)
    {
        int i;

        factorize(nPoints[0], nFact, actual);
        if (actual[nFact[0]] > maxPrimeFactor)
        {
            System.out.println("Error! Prime factor of FFT length too large...");
            System.exit(0);
        }

        remain[0] = nPoints[0];
        sofar[1] = 1;
        remain[1] = nPoints[0]/actual[1];

        for (i=2; i<=nFact[0]; i++)
        {
            sofar[i]=sofar[i-1]*actual[i-1];
            remain[i]=remain[i-1] / actual[i];
        }
    }

    //The sequence y is the permuted input sequence x so that the following
    // transformations can be performed in-place, and the final result is the
    // normal order.
    protected static void permute(int nPoint, int nFact, int fact[], int remain[], 
            double xRe[], double xIm[],
            double yRe[], double yIm[])
    {
        int i,j,k;
        int [] count = new int[maxFactorCount]; 

        for (i=1; i<=nFact; i++) 
            count[i]=0;

        k=0;
        for (i=0; i<=nPoint-2; i++)
        {
            yRe[i] = xRe[k];
            yIm[i] = xIm[k];
            j=1;
            k=k+remain[j];
            count[1]++;
            while (count[j] >= fact[j])
            {
                count[j]=0;
                k=k-remain[j-1]+remain[j+1];
                j++;
                count[j]++;
            }
        }
        yRe[nPoint-1]=xRe[nPoint-1];
        yIm[nPoint-1]=xIm[nPoint-1];
    }

    //Twiddle factor multiplications and transformations are performed on a
    // group of data. The number of multiplications with 1 are reduced by skipping
    // the twiddle multiplication of the first stage and of the first group of the
    // following stages.
    protected static void initTrig(int radix)
    {
        int i;
        double w, xre, xim;

        w=2*pi/radix;
        trigRe[0]=1; 
        trigIm[0]=0;
        xre = Math.cos(w); 
        xim = -Math.sin(w);

        trigRe[1]=xre; 
        trigIm[1]=xim;

        for (i=2; i<radix; i++)
        {
            trigRe[i] = xre*trigRe[i-1] - xim*trigIm[i-1];
            trigIm[i] = xim*trigRe[i-1] + xre*trigIm[i-1];
        }
    }

    protected static void fft_4(double [] aRe, double [] aIm)
    {
        double t1_re,t1_im, t2_re,t2_im;
        double m2_re,m2_im, m3_re,m3_im;

        t1_re=aRe[0] + aRe[2]; 
        t1_im=aIm[0] + aIm[2];
        t2_re=aRe[1] + aRe[3]; 
        t2_im=aIm[1] + aIm[3];

        m2_re=aRe[0] - aRe[2]; 
        m2_im=aIm[0] - aIm[2];
        m3_re=aIm[1] - aIm[3]; 
        m3_im=aRe[3] - aRe[1];

        aRe[0]=t1_re + t2_re; 
        aIm[0]=t1_im + t2_im;
        aRe[2]=t1_re - t2_re; 
        aIm[2]=t1_im - t2_im;
        aRe[1]=m2_re + m3_re; 
        aIm[1]=m2_im + m3_im;
        aRe[3]=m2_re - m3_re; 
        aIm[3]=m2_im - m3_im;
    }

    protected static void fft_5(double [] aRe, double [] aIm)
    {  
        double t1_re,t1_im, t2_re,t2_im, t3_re,t3_im;
        double t4_re,t4_im, t5_re,t5_im;
        double m2_re,m2_im, m3_re,m3_im, m4_re,m4_im;
        double m1_re,m1_im, m5_re,m5_im;
        double s1_re,s1_im, s2_re,s2_im, s3_re,s3_im;
        double s4_re,s4_im, s5_re,s5_im;

        t1_re=aRe[1] + aRe[4]; 
        t1_im=aIm[1] + aIm[4];
        t2_re=aRe[2] + aRe[3]; 
        t2_im=aIm[2] + aIm[3];
        t3_re=aRe[1] - aRe[4]; 
        t3_im=aIm[1] - aIm[4];
        t4_re=aRe[3] - aRe[2]; 
        t4_im=aIm[3] - aIm[2];
        t5_re=t1_re + t2_re; 
        t5_im=t1_im + t2_im;
        aRe[0]=aRe[0] + t5_re; 
        aIm[0]=aIm[0] + t5_im;
        m1_re=c5_1*t5_re; 
        m1_im=c5_1*t5_im;
        m2_re=c5_2*(t1_re - t2_re); 
        m2_im=c5_2*(t1_im - t2_im);

        m3_re=-c5_3*(t3_im + t4_im); 
        m3_im=c5_3*(t3_re + t4_re);
        m4_re=-c5_4*t4_im; 
        m4_im=c5_4*t4_re;
        m5_re=-c5_5*t3_im; 
        m5_im=c5_5*t3_re;

        s3_re=m3_re - m4_re; 
        s3_im=m3_im - m4_im;
        s5_re=m3_re + m5_re; 
        s5_im=m3_im + m5_im;
        s1_re=aRe[0] + m1_re; 
        s1_im=aIm[0] + m1_im;
        s2_re=s1_re + m2_re; 
        s2_im=s1_im + m2_im;
        s4_re=s1_re - m2_re; 
        s4_im=s1_im - m2_im;

        aRe[1]=s2_re + s3_re; 
        aIm[1]=s2_im + s3_im;
        aRe[2]=s4_re + s5_re; 
        aIm[2]=s4_im + s5_im;
        aRe[3]=s4_re - s5_re; 
        aIm[3]=s4_im - s5_im;
        aRe[4]=s2_re - s3_re; 
        aIm[4]=s2_im - s3_im;
    }

    protected static void fft_8()
    {
        double [] aRe = new double[4];
        double [] aIm = new double[4];
        double [] bRe = new double[4];
        double [] bIm = new double[4];
        double gem;

        aRe[0] = zRe[0];  bRe[0] = zRe[1];
        aRe[1] = zRe[2];  bRe[1] = zRe[3];
        aRe[2] = zRe[4];  bRe[2] = zRe[5];
        aRe[3] = zRe[6];  bRe[3] = zRe[7];

        aIm[0] = zIm[0];  bIm[0] = zIm[1];
        aIm[1] = zIm[2];  bIm[1] = zIm[3];
        aIm[2] = zIm[4];  bIm[2] = zIm[5];
        aIm[3] = zIm[6];  bIm[3] = zIm[7];

        fft_4(aRe, aIm); 
        fft_4(bRe, bIm);

        gem  = c8*(bRe[1] + bIm[1]);
        bIm[1] = c8*(bIm[1] - bRe[1]);
        bRe[1] = gem;
        gem  = bIm[2];
        bIm[2] =-bRe[2];
        bRe[2] = gem;
        gem  = c8*(bIm[3] - bRe[3]);
        bIm[3] =-c8*(bRe[3] + bIm[3]);
        bRe[3] = gem;

        zRe[0] = aRe[0] + bRe[0]; 
        zRe[4] = aRe[0] - bRe[0];
        zRe[1] = aRe[1] + bRe[1]; 
        zRe[5] = aRe[1] - bRe[1];
        zRe[2] = aRe[2] + bRe[2]; 
        zRe[6] = aRe[2] - bRe[2];
        zRe[3] = aRe[3] + bRe[3]; 
        zRe[7] = aRe[3] - bRe[3];

        zIm[0] = aIm[0] + bIm[0]; 
        zIm[4] = aIm[0] - bIm[0];
        zIm[1] = aIm[1] + bIm[1]; 
        zIm[5] = aIm[1] - bIm[1];
        zIm[2] = aIm[2] + bIm[2]; 
        zIm[6] = aIm[2] - bIm[2];
        zIm[3] = aIm[3] + bIm[3]; 
        zIm[7] = aIm[3] - bIm[3];
    }

    protected static void fft_10()
    {
        double [] aRe = new double[5];
        double [] aIm = new double[5]; 
        double [] bRe = new double[5]; 
        double [] bIm = new double[5];

        aRe[0] = zRe[0];  
        bRe[0] = zRe[5];
        aRe[1] = zRe[2];  
        bRe[1] = zRe[7];
        aRe[2] = zRe[4];  
        bRe[2] = zRe[9];
        aRe[3] = zRe[6];  
        bRe[3] = zRe[1];
        aRe[4] = zRe[8];  
        bRe[4] = zRe[3];

        aIm[0] = zIm[0];  
        bIm[0] = zIm[5];
        aIm[1] = zIm[2];  
        bIm[1] = zIm[7];
        aIm[2] = zIm[4];  
        bIm[2] = zIm[9];
        aIm[3] = zIm[6];  
        bIm[3] = zIm[1];
        aIm[4] = zIm[8];  
        bIm[4] = zIm[3];

        fft_5(aRe, aIm); 
        fft_5(bRe, bIm);

        zRe[0] = aRe[0] + bRe[0]; 
        zRe[5] = aRe[0] - bRe[0];
        zRe[6] = aRe[1] + bRe[1]; 
        zRe[1] = aRe[1] - bRe[1];
        zRe[2] = aRe[2] + bRe[2]; 
        zRe[7] = aRe[2] - bRe[2];
        zRe[8] = aRe[3] + bRe[3]; 
        zRe[3] = aRe[3] - bRe[3];
        zRe[4] = aRe[4] + bRe[4]; 
        zRe[9] = aRe[4] - bRe[4];

        zIm[0] = aIm[0] + bIm[0]; 
        zIm[5] = aIm[0] - bIm[0];
        zIm[6] = aIm[1] + bIm[1]; 
        zIm[1] = aIm[1] - bIm[1];
        zIm[2] = aIm[2] + bIm[2]; 
        zIm[7] = aIm[2] - bIm[2];
        zIm[8] = aIm[3] + bIm[3]; 
        zIm[3] = aIm[3] - bIm[3];
        zIm[4] = aIm[4] + bIm[4]; 
        zIm[9] = aIm[4] - bIm[4];
    }

    protected static void fft_odd(int radix)
    {
        double rere, reim, imre, imim;
        int i, j, k, n, max;

        n = radix;
        max = (n + 1)/2;

        for (j=1; j < max; j++)
        {
            vRe[j] = zRe[j] + zRe[n-j];
            vIm[j] = zIm[j] - zIm[n-j];
            wRe[j] = zRe[j] - zRe[n-j];
            wIm[j] = zIm[j] + zIm[n-j];
        }

        for (j=1; j < max; j++)
        {
            zRe[j]=zRe[0]; 
            zIm[j]=zIm[0];
            zRe[n-j]=zRe[0]; 
            zIm[n-j]=zIm[0];
            k=j;

            for (i=1; i < max; i++)
            {
                rere = trigRe[k] * vRe[i];
                imim = trigIm[k] * vIm[i];
                reim = trigRe[k] * wIm[i];
                imre = trigIm[k] * wRe[i];

                zRe[n-j] += rere + imim;
                zIm[n-j] += reim - imre;
                zRe[j]  += rere - imim;
                zIm[j]  += reim + imre;

                k = k + j;

                if (k >= n) 
                    k = k - n;
            }
        }
        for (j=1; j < max; j++)
        {
            zRe[0]=zRe[0] + vRe[j]; 
            zIm[0]=zIm[0] + wIm[j];
        }
    }

    public static Complex fft(double [] x, int fftSize)
    {
        Complex xCo = new Complex(x.length);
        System.arraycopy(x, 0, xCo.real, 0, x.length);

        for (int i=0; i<x.length; i++)
            xCo.imag[i] = 0.0;

        return ftComplex(xCo.real, xCo.imag, fftSize, false);
    }
    
    public static Complex fft(double [] x)
    {
        return fft(x, x.length);
    }

    public static Complex fft(Complex x)
    {
        return fft(x, x.real.length);
    }
    
    public static Complex fft(Complex x, int fftSize)
    {
        return ftComplex(x.real, x.imag, fftSize, false);
    }

    public static double[] ifftReal(Complex x)
    {
        Complex y = ifftComplex(x);

        return y.real;
    }
    
    public static Complex ifft(Complex x)
    {
        return ifftComplex(x);
    }

    public static Complex ifftComplex(Complex x)
    {
        return ftComplex(x.real, x.imag, x.real.length, true);
    }

    public static double[] ifft(Complex x, int len)
    {  
        Complex y = ifftComplex(x, len);

        return y.real;
    }

    public static Complex ifftComplex(Complex x, int len)
    {
        Complex y = ftComplex(x.real, x.imag, x.real.length, true);

        Complex yOut = new Complex(len);

        if (len<=y.real.length)
        {
            System.arraycopy(y.real, 0, yOut.real, 0, len);
            System.arraycopy(y.imag, 0, yOut.imag, 0, len);
        }
        else
        {
            System.arraycopy(y.real, 0, yOut.real, 0, y.real.length);
            System.arraycopy(y.imag, 0, yOut.imag, 0, y.real.length);

            for (int i=y.real.length; i<len; i++)
            {
                yOut.real[i] = 0.0;
                yOut.imag[i] = 0.0;
            }
        } 

        return yOut;
    }

    public static Complex ftComplex(double [] xRe, double [] xIm, int fftSize, boolean bInverse)
    {
        assert xRe.length==xIm.length;
        assert fftSize>0;

        int i;
        double [] xReTmp = null;
        double [] xImTmp = null;

        xReTmp = new double[fftSize];
        xImTmp = new double[fftSize];
        Complex y = new Complex(fftSize);

        if (fftSize>=xRe.length)
        {
            System.arraycopy(xRe, 0, xReTmp, 0, xRe.length);
            System.arraycopy(xIm, 0, xImTmp, 0, xIm.length);
        }
        else //Cut signal as shorter fftSize is specified
        {
            System.arraycopy(xRe, 0, xReTmp, 0, fftSize);
            System.arraycopy(xIm, 0, xImTmp, 0, fftSize);
        }  

        //Zero-padding if necessary
        for (i=xRe.length; i<fftSize; i++)
        {
            xReTmp[i] = 0.0;
            xImTmp[i] = 0.0;
        }

        if (bInverse)
        {
            for (i=0; i<fftSize; i++)
                xImTmp[i] = -xImTmp[i];
            
            fftBase(fftSize, xReTmp, xImTmp, y.real, y.imag);
            
            for (i=0; i<fftSize; i++)
            {
                y.real[i] /= fftSize;
                y.imag[i] = -y.imag[i]/fftSize;
            }      
        }
        else
            fftBase(fftSize, xReTmp, xImTmp, y.real, y.imag);

        return y;
    }

    protected static void ifft(int n, double [] xRe, double [] xIm, double [] yRe, double [] yIm)
    {
        for (int i=0; i<xIm.length; i++)
            
        fftBase(n, xRe, xIm, yRe, yIm);
        
        
    }

    protected static void twiddleTransform(int sofarRadix, int radix, int remainRadix, double [] yRe, double [] yIm)
    {
        double cosw, sinw, gem;
        double t1_re,t1_im, t2_re,t2_im, t3_re,t3_im;
        double t4_re,t4_im, t5_re,t5_im;
        double m2_re,m2_im, m3_re,m3_im, m4_re,m4_im;
        double m1_re,m1_im, m5_re,m5_im;
        double s1_re,s1_im, s2_re,s2_im, s3_re,s3_im;
        double s4_re,s4_im, s5_re,s5_im;

        initTrig(radix);
        omega = 2*pi/(double)(sofarRadix*radix);
        cosw = Math.cos(omega);
        sinw = -1*Math.sin(omega);

        tw_re = 1.0;
        tw_im = 0;
        dataOffset = 0;
        groupOffset = dataOffset;
        adr = groupOffset;

        for (dataNo=0; dataNo<sofarRadix; dataNo++)
        {
            if (sofarRadix>1)
            {          
                twiddleRe[0] = 1.0; 
                twiddleIm[0] = 0.0;
                twiddleRe[1] = tw_re;
                twiddleIm[1] = tw_im;

                for (twNo=2; twNo<radix; twNo++)
                {
                    twiddleRe[twNo] = tw_re*twiddleRe[twNo-1] - tw_im*twiddleIm[twNo-1];
                    twiddleIm[twNo] = tw_im*twiddleRe[twNo-1] + tw_re*twiddleIm[twNo-1];
                }

                gem  = cosw*tw_re - sinw*tw_im;
                tw_im = sinw*tw_re + cosw*tw_im;
                tw_re = gem;           
            }
            for (groupNo=0; groupNo<remainRadix; groupNo++)
            {
                if ((sofarRadix>1) && (dataNo > 0))
                {
                    zRe[0]=yRe[adr];
                    zIm[0]=yIm[adr];
                    blockNo=1;

                    do {
                        adr = adr + sofarRadix;
                        zRe[blockNo] = twiddleRe[blockNo] * yRe[adr] - twiddleIm[blockNo] * yIm[adr];
                        zIm[blockNo] = twiddleRe[blockNo] * yIm[adr] + twiddleIm[blockNo] * yRe[adr]; 

                        blockNo++;
                    } while (blockNo < radix);
                }
                else
                {
                    for (blockNo=0; blockNo<radix; blockNo++)
                    {
                        zRe[blockNo] = yRe[adr];
                        zIm[blockNo] = yIm[adr];
                        adr += sofarRadix;
                    }
                }

                switch(radix) 
                {
                case 2: 
                    gem=zRe[0] + zRe[1];
                    zRe[1]=zRe[0] - zRe[1]; zRe[0]=gem;
                    gem=zIm[0] + zIm[1];
                    zIm[1]=zIm[0] - zIm[1]; zIm[0]=gem;
                    break;
                case 3: 
                    t1_re=zRe[1] + zRe[2]; t1_im=zIm[1] + zIm[2];
                    zRe[0]=zRe[0] + t1_re; zIm[0]=zIm[0] + t1_im;
                    m1_re=c3_1*t1_re; m1_im=c3_1*t1_im;
                    m2_re=c3_2*(zIm[1] - zIm[2]); 
                    m2_im=c3_2*(zRe[2] - zRe[1]);
                    s1_re=zRe[0] + m1_re; s1_im=zIm[0] + m1_im;
                    zRe[1]=s1_re + m2_re; zIm[1]=s1_im + m2_im;
                    zRe[2]=s1_re - m2_re; zIm[2]=s1_im - m2_im;
                    break;
                case 4 : 
                    t1_re=zRe[0] + zRe[2]; t1_im=zIm[0] + zIm[2];
                    t2_re=zRe[1] + zRe[3]; t2_im=zIm[1] + zIm[3];

                    m2_re=zRe[0] - zRe[2]; m2_im=zIm[0] - zIm[2];
                    m3_re=zIm[1] - zIm[3]; m3_im=zRe[3] - zRe[1];

                    zRe[0]=t1_re + t2_re; zIm[0]=t1_im + t2_im;
                    zRe[2]=t1_re - t2_re; zIm[2]=t1_im - t2_im;
                    zRe[1]=m2_re + m3_re; zIm[1]=m2_im + m3_im;
                    zRe[3]=m2_re - m3_re; zIm[3]=m2_im - m3_im;
                    break;
                case 5 : 
                    t1_re=zRe[1] + zRe[4]; t1_im=zIm[1] + zIm[4];
                    t2_re=zRe[2] + zRe[3]; t2_im=zIm[2] + zIm[3];
                    t3_re=zRe[1] - zRe[4]; t3_im=zIm[1] - zIm[4];
                    t4_re=zRe[3] - zRe[2]; t4_im=zIm[3] - zIm[2];
                    t5_re=t1_re + t2_re; t5_im=t1_im + t2_im;
                    zRe[0]=zRe[0] + t5_re; zIm[0]=zIm[0] + t5_im;
                    m1_re=c5_1*t5_re; m1_im=c5_1*t5_im;
                    m2_re=c5_2*(t1_re - t2_re); 
                    m2_im=c5_2*(t1_im - t2_im);

                    m3_re=-c5_3*(t3_im + t4_im); 
                    m3_im=c5_3*(t3_re + t4_re);
                    m4_re=-c5_4*t4_im; m4_im=c5_4*t4_re;
                    m5_re=-c5_5*t3_im; m5_im=c5_5*t3_re;

                    s3_re=m3_re - m4_re; s3_im=m3_im - m4_im;
                    s5_re=m3_re + m5_re; s5_im=m3_im + m5_im;
                    s1_re=zRe[0] + m1_re; s1_im=zIm[0] + m1_im;
                    s2_re=s1_re + m2_re; s2_im=s1_im + m2_im;
                    s4_re=s1_re - m2_re; s4_im=s1_im - m2_im;

                    zRe[1]=s2_re + s3_re; zIm[1]=s2_im + s3_im;
                    zRe[2]=s4_re + s5_re; zIm[2]=s4_im + s5_im;
                    zRe[3]=s4_re - s5_re; zIm[3]=s4_im - s5_im;
                    zRe[4]=s2_re - s3_re; zIm[4]=s2_im - s3_im;
                    break;
                case 8 : 
                    fft_8(); 
                    break;
                case 10 : 
                    fft_10(); 
                    break;
                default : 
                    fft_odd(radix); 
                break;
                }

                adr = groupOffset;

                for (blockNo=0; blockNo<radix; blockNo++)
                {
                    yRe[adr]=zRe[blockNo]; yIm[adr]=zIm[blockNo];
                    adr=adr+sofarRadix;
                }
                groupOffset += sofarRadix*radix;
                adr = groupOffset;
            }
            dataOffset++;
            groupOffset = dataOffset;
            adr = groupOffset;
        }
    }

    protected static void fftBase(int n, double [] xRe, double [] xIm, double [] yRe, double [] yIm)
    {
        twiddleRe = new double[maxPrimeFactor];
        twiddleIm = new double[maxPrimeFactor];
        trigRe = new double[maxPrimeFactor]; 
        trigIm = new double[maxPrimeFactor];
        zRe = new double[maxPrimeFactor];
        zIm = new double[maxPrimeFactor];
        vRe = new double[maxPrimeFactorDiv2];
        vIm = new double[maxPrimeFactorDiv2];
        wRe = new double[maxPrimeFactorDiv2];
        wIm = new double[maxPrimeFactorDiv2]; 

        int [] sofarRadix = new int[maxFactorCount];
        int [] actualRadix = new int[maxFactorCount];
        int [] remainRadix = new int[maxFactorCount];
        int [] nFactor = new int[1];
        int count;
        int [] nTmp = new int[1];
        nTmp[0] = n;

        pi = 4*Math.atan(1.0);  

        transTableSetup(sofarRadix, actualRadix, remainRadix, nFactor, nTmp);

        permute(nTmp[0], nFactor[0], actualRadix, remainRadix, xRe, xIm, yRe, yIm);

        for (count=1; count<=nFactor[0]; count++)
            twiddleTransform(sofarRadix[count], actualRadix[count], remainRadix[count], yRe, yIm);

        n = nTmp[0];
    }

    public static void main(String[] args) throws Exception
    {
        double [] x = {1,2,3,4,5,6};
        Complex y = FFTArbitraryLength.fft(x,6);
        double [] x2 = FFTArbitraryLength.ifft(y,6);
        
        double [] x3 = {1,2,3,4,5,6,7,8,9,10,11,12};
        y = FFTArbitraryLength.fft(x3,26);
        double [] x4 = FFTArbitraryLength.ifft(y,17);
        
        System.out.println("FFT test");
    }
}
