package de.dfki.lt.signalproc;

import de.dfki.lt.signalproc.util.MathUtils;
import de.dfki.lt.signalproc.util.MathUtils.Complex;

public class FFTMixedRadix {
    //  maxf must be >= the maximum prime factor of fftSize.
    private static int maxf = 10000;
    
    //  maxp must be > the number of prime factors of fftSize.
    private static int maxp = 10000;
    
    private static int [] nfac;
    private static int [] np;
    private static double [] at;
    private static double  [] ck;
    private static double  [] bt;
    private static double  [] sk;
    private static int factInd; //In original code: i
    private static int nt;
    private static int ks;
    private static int kspan;
    private static int nn;
    private static int jc;
    private static double  radf;
    private static int jf;
    private static double  sd;
    private static double  cd;
    private static int kk;
    private static int k1;
    private static int k2;
    private static double  ak;
    private static double  bk;
    private static double  c1;
    private static double  s1;
    private static double  aj;
    private static double  bj;
    private static int kspnn;
    private static int k3;
    private static int k4;
    private static double  akp;
    private static double  akm;
    private static double  ajp;
    private static double  ajm;
    private static double  bkp;
    private static double  bkm;
    private static double  bjp;
    private static double  bjm;
    private static double  c2;
    private static double  s2;
    private static double  c3;
    private static double  s3;
    private static double  aa;
    private static double  bb;
    private static int currentFactor; //In original code: k
    private static int jCount; //In original code: j
    private static int jj;
    private static int jn;
    private static int kt;
    private static int mCount;//In original code: m

    private static int inc;
    private static double  c72;
    private static double  s72;
    private static double  s120;
    private static double  rad;

    //FFT power spectrum of real valued data x
    public static double [] fftPowerSpectrum(double [] x)
    {   
        return fftPowerSpectrum(x, x.length);
    }
    //
    
    //fftSize point FFT power spectrum of real valued data x
    public static double [] fftPowerSpectrum(double [] x, int fftSize)
    {
        int xlen = x.length;
        
        Complex h = new Complex(fftSize);
        double [] Ps = new double[fftSize];
        int w;

        for (w=0; w<xlen; w++)
        {
            h.real[w] = x[w];
            h.imag[w] = 0.0;
        }

        for (w=xlen; w<fftSize; w++)
        {
            h.real[w] = 0.0;
            h.imag[w] = 0.0;
        }
        
        mixedRadixFFTBase(h.real, h.imag, fftSize, fftSize, fftSize, 1);

        for (w=0; w<fftSize; w++)
            h.imag[w] = -h.imag[w];
        
        for (w=0; w<fftSize; w++)
            Ps[w] = 10*MathUtils.log10(h.real[w]*h.real[w] + h.imag[w]*h.imag[w]);
        
        return Ps;
    }
    //
    
    // Absolute FFT spectrum of real valued data x
    public static double [] fftAbsSpectrum(double [] x)
    {
        return fftAbsSpectrum(x, x.length);
    }
    //
    
    // fftSize-point Absolute FFT spectrum of real valued data x
    public static double [] fftAbsSpectrum(double [] x, int fftSize)
    {
        int xlen = x.length;
        
        Complex h = new Complex(fftSize);
        double [] Ps = new double[fftSize];
        int w;

        for (w=0; w<xlen; w++)
        {
            h.real[w] = x[w];
            h.imag[w] = 0.0;
        }

        for (w=xlen; w<fftSize; w++)
        {
            h.real[w] = 0.0;
            h.imag[w] = 0.0;
        }
        
        mixedRadixFFTBase(h.real, h.imag, fftSize, fftSize, fftSize, 1);

        for (w=0; w<fftSize; w++)
            h.imag[w] = -h.imag[w];
        
        for (w=0; w<fftSize; w++)
            Ps[w] = Math.sqrt(h.real[w]*h.real[w] + h.imag[w]*h.imag[w]);
        
        return Ps;
    }
    //
    
    // xlen-point FFT of real valued data x of length xlen.
    // The result is returned as a pointer to a Complex object 
    //   which holds a real and an imag array of size xlen
    public static Complex fftReal(double [] x, int xlen)
    {
        Complex h = new Complex(xlen);

        int w;

        for (w=0; w<xlen; w++)
        {
            h.real[w] = x[w];
            h.imag[w] = 0.0;
        }

        mixedRadixFFTBase(h.real, h.imag, xlen, xlen, xlen, 1);

        for (w=0; w<xlen; w++)
            h.imag[w] = -h.imag[w];

        return h;
    }


    //  fftSize-point FFT of real valued data x of length xlen.
    //  The result is returned as a pointer to a Complex object 
    //      which holds a real and an imag array of size fftSize
    //  fftSize can be greater than, equal to, or less than xlen
    public static Complex fftReal(double [] x, int xlen, int fftSize)
    {
        if (xlen>fftSize)
            xlen = fftSize;

        double [] x2 = new double[fftSize];

        int w;
        for (w=0; w<xlen; w++)
            x2[w] = x[w];

        for (w=xlen; w<fftSize; w++)
            x2[w] = 0.0;

        Complex h = fftReal(x2, fftSize);

        return h;
    }

    //  fftSize-point FFT of complex valued data in x of length x.length.
    //  The result is returned as a pointer to a Complex object 
    //      which holds a real and an imag array of size fftSize
    //  fftSize can be greater than, equal to, or less than x.length.
    public static Complex fftComplex(Complex x, int fftSize)
    {
        Complex h = new Complex(fftSize);

        int w;
        for (w=0; w<Math.min(x.real.length, fftSize); w++)
        {
            h.real[w] = x.real[w];
            h.imag[w] = x.imag[w];
        }

        for (w=x.real.length; w<fftSize; w++)
        {
            h.real[w] = 0.0;
            h.imag[w] = 0.0;
        }

        mixedRadixFFTBase(h.real, h.imag, fftSize, fftSize, fftSize, 1);

        int midVal = (int)(Math.floor(fftSize/2)+1);

        double tmp;
        for (w = 1; w<midVal; w++)
        {
            tmp = h.real[w];
            h.real[w] = h.real[fftSize-w];
            h.real[fftSize-w] = tmp;

            tmp = h.imag[w];
            h.imag[w] = h.imag[fftSize-w];
            h.imag[fftSize-w] = tmp;
        }

        return h;
    }

    //  x.length-point FFT of complex valued data in x of length x.length.
    //  The result is returned as a pointer to a Complex object 
    //      which holds a real and an imag array of size x.length.
    //  fftSize can be greater than, equal to, or less than x.length.
    public static Complex fftComplex(Complex x)
    {
        return fftComplex(x, x.real.length);
    }

    //  ifftSize-point inverse FFT of complex valued data in x of length x.length.
    //  The result is returned as a pointer to a Complex object 
    //      which holds a real and an imag array of size ifftSize.
    //  ifftSize can be greater than, equal to, or less than x.length.
    public static Complex ifft(Complex x, int ifftSize)
    {
        Complex h = null;
        int w;

        if (x.real.length>ifftSize)
        {
            Complex h2 = ifft(x);
            h = new Complex(ifftSize);
            for (w=0; w<ifftSize; w++)
            {
                h.real[w] = h2.real[w];
                h.imag[w] = h2.imag[w];
            }
        }
        else if (x.real.length==ifftSize)
            h = ifft(x);
        else
        {
            Complex h2 = ifft(x);
            h = new Complex(ifftSize);
            for (w=0; w<h2.real.length; w++)
            {
                h.real[w] = h2.real[w];
                h.imag[w] = h2.imag[w];
            }
            for (w=h2.real.length; w<ifftSize; w++)
            {
                h.real[w] = 0.0;
                h.imag[w] = 0.0;
            }
        }

        return h;
    }

    //  x.length-point inverse FFT of complex valued data in x of length x.length.
    //  The result is returned as a pointer to a Complex object 
    //      which holds a real and an imag array of size x.length.
    public static Complex ifft(Complex x)
    {
        Complex h = new Complex(x.real.length);

        int w;
        for (w=0; w<x.real.length; w++)
        {
            h.real[w] = x.real[w]/x.real.length;
            h.imag[w] = x.imag[w]/x.real.length;
        }

        mixedRadixFFTBase(h.real, h.imag, x.real.length, x.real.length, x.real.length, -1);

        int midVal = (int)(Math.floor(x.real.length/2)+1);

        double tmp;
        for (w = 1; w<midVal; w++)
        {   
            tmp = h.real[w];
            h.real[w] = h.real[x.real.length-w];
            h.real[x.real.length-w] = tmp;

            tmp = h.imag[w];
            h.imag[w] = h.imag[x.real.length-w];
            h.imag[x.real.length-w] = tmp;
        }

        return h;
    }

    //  ifftSize-point inverse FFT of complex valued data in x of length x.length.
    //  The result is returned as a pointer to a real valued array of size ifftSize.
    //  This function is for inverse FFT of Fourier coefficients of real-valued data.
    //  Therefore, in order to obtain correct results, please make sure data in x 
    //      conforms to the properties of Fourier coefficients of real valued data
    //      (i.e. even-symmetry of real coefficients, odd-symmetry of imag coefficients).
    public static double [] ifftReal(Complex x, int ifftSize)
    {
        Complex h = ifft(x);
        double [] y = new double[ifftSize];
        int w;

        for (w=0; w<Math.min(h.real.length, ifftSize); w++)
            y[w] = h.real[w];

        for (w=Math.min(h.real.length, ifftSize); w<ifftSize; w++)
            y[w] = 0.0;

        return y;
    }



    //   In place mixed-radix FFT/IFFT algorithm
    //   a: Real part of sequence to be transformed
    //   b: Imaginary part of sequence to be transformed
    //   ntot = fftSize = nspan = length of a and b vectors = number of points in FFT (for simply computing FFT)
    //    (There are more complex uses of this function, for details refer to [Singleton, 1969])
    //    isn: 1  => FFT
    //         -1 => Inverse FFT
    //    The output is directly written to a and b vectors.
    //    Extra scaling and re-arranging of the output might be required for different cases.
    //    Please use wrapper functions fftComplex, fftReal, ifft, ifftReal for simplicity.
    //    Please refer to these functions if you want to add new functions calling mixedRadixFFTBase.
    private static void mixedRadixFFTBase(double [] a, double [] b, int ntot, int fftSize, int nspan, int isn)
    {    
        //Local variables for handling goto statements
        boolean bLoopLine924 = false;
        boolean bJumpToLine924 = false;

        boolean bJumpToLine950 = true;

        boolean bLoopLine730 = false;
        boolean bJumpToLine730 = false;

        boolean bLoopLine640 = false;
        boolean bJumpToLine640 = false;

        boolean bJumpToLine640_0 = true;

        boolean bLoopLine520 = false;
        boolean bJumpToLine520 = false;

        boolean bLoopLine230 = false;
        boolean bJumpToLine230 = false;

        boolean bLoopLine210 = false;
        boolean bJumpToLine210 = false;

        boolean bLoopLine820 = false;
        boolean bJumpToLine820 = false;

        boolean bLoopLine830 = false;
        boolean bJumpToLine830 = false;

        boolean bLoopLine840 = false;
        boolean bJumpToLine840 = false;

        boolean bLoopLine850 = false;
        boolean bJumpToLine850 = false;

        boolean bLoopLine870 = false;
        boolean bJumpToLine870 = false;

        boolean bLoopLine880 = false;
        boolean bJumpToLine880 = false;

        boolean bLoopLine910 = false;
        boolean bJumpToLine910 = false;

        boolean bJumpToLine914_0 = true;

        boolean bLoopLine914 = false;
        boolean bJumpToLine914 = false;

        boolean bLoopLine420 = false;
        boolean bJumpToLine420 = false;

        boolean bLoopLine440 = false;

        boolean bJumpToLine460 = false;

        boolean bJumpToLine450 = false;

        boolean bLoopLine430 = false;
        boolean bJumpToLine430 = false;

        boolean bLoopLine410 = false;
        boolean bJumpToLine410 = false;

        boolean bLoopLine320 = false;
        boolean bJumpToLine320 = false;
        boolean bLoopLine320Prev = false;
        boolean bJumpToLine320Prev = false;
        boolean bLoopLine320Prev_2 = false;
        boolean bJumpToLine320Prev_2 = false;
        boolean bLoopLine320Prev_3 = false;
        boolean bJumpToLine320Prev_3 = false;

        boolean bLoopLine100 = false;
        boolean bJumpToLine100 = false;
        boolean bJumpToLine100_2 = false;
        boolean bJumpToLine100_3 = false;

        boolean bJumpToLine400 = false;

        boolean bLoopLine510 = false;
        boolean bJumpToLine510 = false;
        boolean bLoopLine510Prev_2 = false;
        boolean bJumpToLine510Prev_2 = false;
        boolean bLoopLine510Prev_3 = false;
        boolean bJumpToLine510Prev_3 = false;

        boolean bLoopLine902 = false;
        boolean bJumpToLine902 = false;

        boolean bLoopLine904 = false;
        boolean bJumpToLine904 = false;

        boolean bJumpToLine906 = false;

        boolean bJumpToLine890 = false;

        boolean bJumpToLine800 = false;
        boolean bJumpToLine800_1 = false;

        boolean bLoopLine410Prev = false;
        boolean bLoopLine420Prev = false;
        boolean bLoopLine430Prev = false;
        boolean bLoopLine440Prev = false;
        boolean bJumpToLine410Prev = false;
        boolean bJumpToLine420Prev = false;
        boolean bJumpToLine430Prev = false;
        boolean bLoopLine410Prev_2 = false;
        boolean bLoopLine420Prev_2 = false;
        boolean bLoopLine430Prev_2 = false;
        boolean bLoopLine440Prev_2 = false;
        boolean bJumpToLine410Prev_2 = false;
        boolean bJumpToLine420Prev_2 = false;
        boolean bJumpToLine430Prev_2 = false;

        boolean bJumpToLine700 = false;
        //

        nfac = new int[200];
        np = new int[maxp];
        at = new double[maxf];
        ck = new double[maxf];
        bt = new double[maxf];
        sk = new double[maxf];

        inc=isn;
        c72=0.30901699437494742;
        s72=0.95105651629515357;
        s120=0.86602540378443865;
        rad=6.2831853071796;

        if (fftSize<2)
            return;

        if (isn < 0)
        {
            s72=-s72;
            s120=-s120;
            rad=-rad;
            inc=-inc;
        }

        nt=inc*ntot;
        ks=inc*nspan;
        kspan=ks;
        nn=nt-inc;
        jc=ks/fftSize;
        radf=rad*jc*0.5;
        factInd=0;
        jf=0;

        //Determine factors of fftSize
        mCount = 0;
        currentFactor = fftSize;

        if (fftSize<2)
            nfac[0] = fftSize;
        else
        {       
            while (currentFactor%16==0)
            {
                mCount++;
                nfac[mCount-1]=4;
                currentFactor /= 16;
            }

            jn=3;
            jj=9;
            while (currentFactor%jj==0)
            {
                mCount++;
                nfac[mCount-1]=jn;
                currentFactor/=jj;
            }

            jn+=2;
            jj=jn*jn;
            while (jj<=currentFactor)
            {
                while (currentFactor%jj==0)
                {
                    mCount++;
                    nfac[mCount-1]=jn;
                    currentFactor /= jj;
                }

                jn+=2;
                jj=jn*jn;
            }

            boolean bGoto80 = false;
            if (currentFactor<=4)
            {
                kt=mCount;
                nfac[mCount]=currentFactor;
                if (currentFactor!=1)
                    mCount++;

                bGoto80 = true;
                if (kt!=0)
                {
                    jn=kt;
                    boolean bGoto90=true;
                    while (jn!=0 || bGoto90)
                    {
                        bGoto90=false;
                        mCount++;
                        nfac[mCount-1]=nfac[jn-1];
                        jn--;
                    }
                }
            }

            if (!bGoto80)
            {
                if (currentFactor-Math.floor(currentFactor/4.0)*4==0)
                {
                    mCount++;
                    nfac[mCount-1]=2;
                    currentFactor/=4;
                }
                kt=mCount;
                jn=2;

                boolean bGoto60=true;
                while (jn<=currentFactor || bGoto60)
                {
                    bGoto60=false;
                    if (currentFactor%jn==0)
                    {
                        mCount++;
                        nfac[mCount-1]=jn;
                        currentFactor/=jn;
                    }

                    jn=(int)(Math.floor((jn+1.0)/2)*2+1);
                }

                if (kt!=0)
                {
                    jn=kt;
                    boolean bGoto90=true;
                    while (jn!=0 || bGoto90)
                    {
                        bGoto90=false;
                        mCount++;
                        nfac[mCount-1]=nfac[jn-1];
                        jn--;
                    }
                }
            }
        }
        //

        //compute fourier transform
        do{ //line100
            //if (fLog!=NULL)
            //    fprintf(fLog, "100\fftSize");

            bLoopLine100 = false;
            if (bJumpToLine100_2)
            {
                bJumpToLine100_2 = false;
                bLoopLine440 = bLoopLine440Prev_2;
                bLoopLine430 = bLoopLine430Prev_2;
                bLoopLine420 = bLoopLine420Prev_2;
                bLoopLine410 = bLoopLine410Prev_2;
                bLoopLine510 = bLoopLine510Prev_2;
                bLoopLine320 = bLoopLine320Prev_2;
                bJumpToLine410 = bJumpToLine410Prev_2;
                bJumpToLine420 = bJumpToLine420Prev_2;
                bJumpToLine430 = bJumpToLine430Prev_2;
                bJumpToLine510 = bJumpToLine510Prev_2;
                bJumpToLine320 = bJumpToLine320Prev_2;
            }

            if (bJumpToLine100_3)
            {
                bJumpToLine100_3 = false;
                bLoopLine510 = bLoopLine510Prev_3;
                bLoopLine320 = bLoopLine320Prev_3;
                bJumpToLine510 = bJumpToLine510Prev_3;
                bJumpToLine320 = bJumpToLine320Prev_3;
            }

            if (bJumpToLine100)
                bJumpToLine100 = false;

            sd=radf/kspan;
            cd=2.0*Math.sin(sd)*Math.sin(sd);
            sd=Math.sin(sd+sd);
            kk=1;
            factInd=factInd+1;
            if (nfac[factInd-1] != 2)
                bJumpToLine400 = true;
            else
                bJumpToLine400 = false;

            if (!bJumpToLine400)
            {
                //transform for factor of 2 including rotation factor
                kspan=kspan/2;
                k1=kspan+2;

                do{ //line210
                    //if (fLog!=NULL)
                    //    fprintf(fLog, "210\fftSize");

                    bLoopLine210 = false;
                    bJumpToLine210 = false;
                    k2=kk+kspan;
                    ak=a[k2-1];
                    bk=b[k2-1];
                    a[k2-1]=a[kk-1]-ak;
                    b[k2-1]=b[kk-1]-bk;
                    a[kk-1]=a[kk-1]+ak;
                    b[kk-1]=b[kk-1]+bk;
                    kk=k2+kspan;
                    if (kk <= nn)
                    {
                        bLoopLine210 = true;
                        bJumpToLine210 = true;
                    }
                    else
                    {
                        bLoopLine210 = false;
                        bJumpToLine210 = false;
                    }

                    if (!bJumpToLine210)
                    {
                        kk=kk-nn;
                        if (kk <= jc) 
                        {
                            bLoopLine210 = true;
                            bJumpToLine210 = true;
                        }
                        else
                        {
                            bLoopLine210 = false;
                            bJumpToLine210 = false;
                        }
                    }
                }while(bLoopLine210);

                if (kk > kspan)
                    bJumpToLine800 = true;
                else
                    bJumpToLine800 = false;

                if (!bJumpToLine800)
                {
                    do{ //line220
                        //if (fLog!=NULL)
                        //    fprintf(fLog, "220\fftSize");
                        c1=1.0-cd;
                        s1=sd;

                        do{ //line230
                            //if (fLog!=NULL)
                            //    fprintf(fLog, "230\fftSize");

                            bLoopLine230 = false;
                            bJumpToLine230 = false;
                            k2=kk+kspan;
                            ak=a[kk-1]-a[k2-1];
                            bk=b[kk-1]-b[k2-1];
                            a[kk-1]=a[kk-1]+a[k2-1];
                            b[kk-1]=b[kk-1]+b[k2-1];
                            a[k2-1]=c1*ak-s1*bk;
                            b[k2-1]=s1*ak+c1*bk;
                            kk=k2+kspan;

                            if (kk < nt) 
                            {
                                bLoopLine230 = true;
                                bJumpToLine230 = true;
                            }
                            else
                            {
                                bLoopLine230 = false;
                                bJumpToLine230 = false;
                            }

                            if (!bJumpToLine230)
                            {
                                k2=kk-nt;
                                c1=-c1;
                                kk=k1-k2;

                                if (kk > k2)
                                {
                                    bLoopLine230 = true;
                                    bJumpToLine230 = true;
                                }
                                else
                                {
                                    bLoopLine230 = false;
                                    bJumpToLine230 = false;
                                }

                                if (!bJumpToLine230)
                                {
                                    ak=c1-(cd*c1+sd*s1);
                                    s1=(sd*c1-cd*s1)+s1;
                                    c1=2.0-(ak*ak+s1*s1);
                                    s1=c1*s1;
                                    c1=c1*ak;
                                    kk=kk+jc;

                                    if (kk < k2)
                                    {
                                        bLoopLine230 = true;
                                        bJumpToLine230 = true;
                                    }
                                    else
                                    {
                                        bLoopLine230 = false;
                                        bJumpToLine230 = false;
                                    }
                                }
                            }
                        }while(bLoopLine230);

                        k1=k1+inc+inc;
                        kk=(k1-kspan)/2+jc;

                    }while(kk <= jc+jc);

                    bLoopLine100 = true;
                    bJumpToLine100 = true;
                }
            }

            if (!bJumpToLine100 || bJumpToLine400 || bJumpToLine800)
            {
                // transform for factor of 3 (optional code)
                do{
                    if (!bJumpToLine800)
                    {
                        if (!bJumpToLine400)
                        { //line320
                            //if (fLog!=NULL)
                            //    fprintf(fLog, "320\fftSize");

                            bLoopLine320 = false;
                            bJumpToLine320 = false;

                            k1=kk+kspan;
                            k2=k1+kspan;
                            ak=a[kk-1];
                            bk=b[kk-1];
                            aj=a[k1-1]+a[k2-1];
                            bj=b[k1-1]+b[k2-1];
                            a[kk-1]=ak+aj;
                            b[kk-1]=bk+bj;
                            ak=-0.5*aj+ak;
                            bk=-0.5*bj+bk;
                            aj=(a[k1-1]-a[k2-1])*s120;
                            bj=(b[k1-1]-b[k2-1])*s120;
                            a[k1-1]=ak-bj;
                            b[k1-1]=bk+aj;
                            a[k2-1]=ak+bj;
                            b[k2-1]=bk-aj;
                            kk=k2+kspan;
                            if (kk < nn)
                            {
                                bLoopLine320 = true;
                                bJumpToLine320 = true;
                            }
                            else
                            {
                                bLoopLine320 = false;
                                bJumpToLine320 = false;
                            }
                        }
                    }

                    if (!bJumpToLine320 || bJumpToLine400 || bJumpToLine800)
                    {
                        if (!bJumpToLine800)
                        {
                            if (!bJumpToLine400)
                            {
                                kk=kk-nn;
                                if (kk <= kspan)
                                {
                                    bLoopLine320 = true;
                                    bJumpToLine320 = true;
                                }
                                else
                                {
                                    bLoopLine320 = false;
                                    bJumpToLine320 = false;
                                }
                            }
                        }


                        if (!bJumpToLine320 || bJumpToLine400 || bJumpToLine800)
                        {
                            if (!bJumpToLine800)
                            {
                                if (!bJumpToLine700)
                                {
                                    if (!bJumpToLine400)
                                        bJumpToLine700 = true;
                                    else
                                        bJumpToLine700 = false;
                                }   
                            }

                            do{
                                if (!bJumpToLine700 || bJumpToLine400)
                                {
                                    if (!bJumpToLine800 || bJumpToLine400)
                                    {
                                        bJumpToLine400 = false;
                                        if (nfac[factInd-1] == 4 || bJumpToLine510)
                                        {
                                            if (!bJumpToLine510)
                                            {
                                                //if (fLog!=NULL)
                                                //    fprintf(fLog, "400\fftSize");

                                                kspnn=kspan;
                                                kspan=kspan/4;

                                                do{ //line410
                                                    //if (fLog!=NULL)
                                                    //    fprintf(fLog, "410\fftSize");

                                                    bLoopLine410 = false;
                                                    bJumpToLine410 = false;

                                                    c1=1.0;
                                                    s1=0;

                                                    do{ //line420
                                                        //if (fLog!=NULL)
                                                        //    fprintf(fLog, "420\fftSize");

                                                        bLoopLine420 = false;
                                                        bJumpToLine420 = false;

                                                        k1=kk+kspan;
                                                        k2=k1+kspan;
                                                        k3=k2+kspan;
                                                        akp=a[kk-1]+a[k2-1];
                                                        akm=a[kk-1]-a[k2-1];
                                                        ajp=a[k1-1]+a[k3-1];
                                                        ajm=a[k1-1]-a[k3-1];
                                                        a[kk-1]=akp+ajp;
                                                        ajp=akp-ajp;
                                                        bkp=b[kk-1]+b[k2-1];
                                                        bkm=b[kk-1]-b[k2-1];
                                                        bjp=b[k1-1]+b[k3-1];
                                                        bjm=b[k1-1]-b[k3-1];
                                                        b[kk-1]=bkp+bjp;
                                                        bjp=bkp-bjp;
                                                        if (isn < 0)
                                                            bJumpToLine450 = true;
                                                        else
                                                            bJumpToLine450 = false;

                                                        if (!bJumpToLine450)
                                                        {
                                                            akp=akm-bjm;
                                                            akm=akm+bjm;
                                                            bkp=bkm+ajm;
                                                            bkm=bkm-ajm;
                                                            if (s1 == 0) 
                                                                bJumpToLine460 = true;
                                                            else
                                                                bJumpToLine460 = false;
                                                        }

                                                        do{ //line430
                                                            //if (fLog!=NULL)
                                                            //    fprintf(fLog, "430\fftSize");
                                                            bLoopLine430 = false;
                                                            bJumpToLine430 = false;

                                                            if (!bJumpToLine460)
                                                            {
                                                                if (!bJumpToLine450)
                                                                {
                                                                    a[k1-1]=akp*c1-bkp*s1;
                                                                    b[k1-1]=akp*s1+bkp*c1;
                                                                    a[k2-1]=ajp*c2-bjp*s2;
                                                                    b[k2-1]=ajp*s2+bjp*c2;
                                                                    a[k3-1]=akm*c3-bkm*s3;
                                                                    b[k3-1]=akm*s3+bkm*c3;
                                                                    kk=k3+kspan;
                                                                    if (kk <= nt)
                                                                    {
                                                                        bLoopLine420 = true;
                                                                        bJumpToLine420 = true;
                                                                    }
                                                                    else
                                                                    {
                                                                        bLoopLine420 = false;
                                                                        bJumpToLine420 = false;
                                                                    }
                                                                }
                                                            }

                                                            if (!bJumpToLine420 || bJumpToLine460 || bJumpToLine450)
                                                            {
                                                                do{
                                                                    if (!bJumpToLine460)
                                                                    {
                                                                        if (!bJumpToLine450)
                                                                        {
                                                                            //if (fLog!=NULL)
                                                                            //fprintf(fLog, "440\fftSize");

                                                                            bLoopLine440 = false;

                                                                            c2=c1-(cd*c1+sd*s1);
                                                                            s1=(sd*c1-cd*s1)+s1;
                                                                            c1=2.0-(c2*c2+s1*s1);
                                                                            s1=c1*s1;
                                                                            c1=c1*c2;
                                                                            c2=c1*c1-s1*s1;
                                                                            s2=2.0*c1*s1;
                                                                            c3=c2*c1-s2*s1;
                                                                            s3=c2*s1+s2*c1;
                                                                            kk=kk-nt+jc;
                                                                            if (kk <= kspan) 
                                                                            {
                                                                                bLoopLine420 = true;
                                                                                bJumpToLine420 = true;
                                                                            }
                                                                            else
                                                                            {
                                                                                bLoopLine420 = false;
                                                                                bJumpToLine420 = false;
                                                                            }
                                                                        }
                                                                    }

                                                                    if (!bJumpToLine420 || bJumpToLine460 || bJumpToLine450)
                                                                    {
                                                                        if (!bJumpToLine460)
                                                                        {
                                                                            if (!bJumpToLine450)
                                                                            {
                                                                                kk=kk-kspan+inc;
                                                                                if (kk <= jc)
                                                                                {
                                                                                    bLoopLine410 = true;
                                                                                    bJumpToLine410 = true;
                                                                                    bLoopLine440 = false;
                                                                                    bLoopLine430 = false;
                                                                                    bJumpToLine430 = false;
                                                                                    bLoopLine420 = false;
                                                                                    bJumpToLine420 = false;
                                                                                }
                                                                                else
                                                                                {
                                                                                    bLoopLine410 = false;
                                                                                    bJumpToLine410 = false;
                                                                                }

                                                                                if (!bJumpToLine410)
                                                                                {   
                                                                                    if (kspan == jc) 
                                                                                    {
                                                                                        bJumpToLine800_1 = true;
                                                                                        bLoopLine410Prev = bLoopLine410;
                                                                                        bLoopLine420Prev = bLoopLine420;
                                                                                        bLoopLine430Prev = bLoopLine430;
                                                                                        bLoopLine440Prev = bLoopLine440;
                                                                                        bJumpToLine410Prev = bJumpToLine410;
                                                                                        bJumpToLine420Prev = bJumpToLine420;
                                                                                        bJumpToLine430Prev = bJumpToLine430;

                                                                                        bLoopLine410 = false;
                                                                                        bLoopLine420 = false;
                                                                                        bLoopLine430 = false;
                                                                                        bLoopLine440= false;
                                                                                        bJumpToLine410= false;
                                                                                        bJumpToLine420 = false;
                                                                                        bJumpToLine430 = false;
                                                                                    }
                                                                                    else
                                                                                        bJumpToLine800_1 = false;

                                                                                    if (!bJumpToLine800_1)
                                                                                    {
                                                                                        bLoopLine100 = true;
                                                                                        bJumpToLine100_2 = true;

                                                                                        bLoopLine410Prev_2 = bLoopLine410;
                                                                                        bLoopLine420Prev_2 = bLoopLine420;
                                                                                        bLoopLine430Prev_2 = bLoopLine430;
                                                                                        bLoopLine440Prev_2 = bLoopLine440;
                                                                                        bLoopLine510Prev_2 = bLoopLine510;
                                                                                        bLoopLine320Prev_2 = bLoopLine320;
                                                                                        bJumpToLine410Prev_2 = bJumpToLine410;
                                                                                        bJumpToLine420Prev_2 = bJumpToLine420;
                                                                                        bJumpToLine430Prev_2 = bJumpToLine430;
                                                                                        bJumpToLine510Prev_2 = bJumpToLine510;
                                                                                        bJumpToLine320Prev_2 = bJumpToLine320;

                                                                                        bLoopLine440 = false;
                                                                                        bLoopLine430 = false;
                                                                                        bLoopLine420 = false;
                                                                                        bLoopLine410 = false;
                                                                                        bLoopLine510 = false;
                                                                                        bLoopLine320 = false;
                                                                                        bJumpToLine410 = false;
                                                                                        bJumpToLine420 = false;
                                                                                        bJumpToLine430 = false;
                                                                                        bJumpToLine510 = false;
                                                                                        bJumpToLine320 = false;
                                                                                    }
                                                                                }
                                                                            }

                                                                            if (!bJumpToLine100_2)
                                                                            {
                                                                                if (!bJumpToLine800_1)
                                                                                {
                                                                                    if (!bJumpToLine410)
                                                                                    {
                                                                                        //if (fLog!=NULL)
                                                                                        //fprintf(fLog, "450\fftSize");

                                                                                        bJumpToLine450 = false;
                                                                                        akp=akm+bjm;
                                                                                        akm=akm-bjm;
                                                                                        bkp=bkm-ajm;
                                                                                        bkm=bkm+ajm;
                                                                                        if (s1 != 0) 
                                                                                        {
                                                                                            bLoopLine430 = true;
                                                                                            bJumpToLine430 = true;
                                                                                            bLoopLine440 = false;
                                                                                        }
                                                                                        else
                                                                                        {
                                                                                            bLoopLine430 = false;
                                                                                            bJumpToLine430 = false; 
                                                                                        }
                                                                                    }
                                                                                }
                                                                            }
                                                                        }

                                                                        if (!bJumpToLine100_2)
                                                                        {
                                                                            if (!bJumpToLine800_1)
                                                                            {
                                                                                if (!bJumpToLine430 && !bJumpToLine410)
                                                                                {
                                                                                    //if (fLog!=NULL)
                                                                                    //fprintf(fLog, "460\fftSize");

                                                                                    bJumpToLine460 = false;
                                                                                    a[k1-1]=akp;
                                                                                    b[k1-1]=bkp;
                                                                                    a[k2-1]=ajp;
                                                                                    b[k2-1]=bjp;
                                                                                    a[k3-1]=akm;
                                                                                    b[k3-1]=bkm;
                                                                                    kk=k3+kspan;
                                                                                    if (kk <= nt)
                                                                                    {
                                                                                        bLoopLine420 = true;
                                                                                        bJumpToLine420 = true;
                                                                                    }
                                                                                    else
                                                                                    {
                                                                                        bLoopLine420 = false;
                                                                                        bJumpToLine420 = false;
                                                                                    }

                                                                                    if (!bJumpToLine420)
                                                                                        bLoopLine440 = true;
                                                                                    else
                                                                                        bLoopLine440 = false;
                                                                                }
                                                                            }
                                                                        }
                                                                    }
                                                                }while(bLoopLine440);
                                                            }
                                                        }while(bLoopLine430);
                                                    }while(bLoopLine420);
                                                }while(bLoopLine410);
                                            }

                                            if (!bJumpToLine100_2)
                                            {
                                                if (!bJumpToLine800_1)
                                                {
                                                    //transform for factor of 5 (optional code)
                                                    if (bJumpToLine510)
                                                    {
                                                        bJumpToLine510 = false;
                                                        bLoopLine510 = false;
                                                        bJumpToLine320 = bJumpToLine320Prev;
                                                        bLoopLine320 = bLoopLine320Prev;
                                                    }

                                                    //if (fLog!=NULL)
                                                    //fprintf(fLog, "510\fftSize");

                                                    c2=c72*c72-s72*s72;
                                                    s2=2.0*c72*s72;

                                                    do{ //line520
                                                        //if (fLog!=NULL)
                                                        //fprintf(fLog, "520\fftSize");

                                                        bLoopLine520 = false;
                                                        bJumpToLine520 = false;
                                                        k1=kk+kspan;
                                                        k2=k1+kspan;
                                                        k3=k2+kspan;
                                                        k4=k3+kspan;
                                                        akp=a[k1-1]+a[k4-1];
                                                        akm=a[k1-1]-a[k4-1];
                                                        bkp=b[k1-1]+b[k4-1];
                                                        bkm=b[k1-1]-b[k4-1];
                                                        ajp=a[k2-1]+a[k3-1];
                                                        ajm=a[k2-1]-a[k3-1];
                                                        bjp=b[k2-1]+b[k3-1];
                                                        bjm=b[k2-1]-b[k3-1];
                                                        aa=a[kk-1];
                                                        bb=b[kk-1];
                                                        a[kk-1]=aa+akp+ajp;
                                                        b[kk-1]=bb+bkp+bjp;
                                                        ak=akp*c72+ajp*c2+aa;
                                                        bk=bkp*c72+bjp*c2+bb;
                                                        aj=akm*s72+ajm*s2;
                                                        bj=bkm*s72+bjm*s2;
                                                        a[k1-1]=ak-bj;
                                                        a[k4-1]=ak+bj;
                                                        b[k1-1]=bk+aj;
                                                        b[k4-1]=bk-aj;
                                                        ak=akp*c2+ajp*c72+aa;
                                                        bk=bkp*c2+bjp*c72+bb;
                                                        aj=akm*s2-ajm*s72;
                                                        bj=bkm*s2-bjm*s72;
                                                        a[k2-1]=ak-bj;
                                                        a[k3-1]=ak+bj;
                                                        b[k2-1]=bk+aj;
                                                        b[k3-1]=bk-aj;
                                                        kk=k4+kspan;
                                                        if (kk < nn) 
                                                        {
                                                            bLoopLine520 = true;
                                                            bJumpToLine520 = true;
                                                        }
                                                        else
                                                        {
                                                            bLoopLine520 = false;
                                                            bJumpToLine520 = false;
                                                        }

                                                        if (!bJumpToLine520)
                                                        {
                                                            kk=kk-nn;
                                                            if (kk <= kspan) 
                                                            {
                                                                bLoopLine520 = true;
                                                                bJumpToLine520 = true;
                                                            }
                                                            else
                                                            {
                                                                bLoopLine520 = false;
                                                                bJumpToLine520 = false;
                                                            }
                                                        }
                                                    }while(bLoopLine520);

                                                    bJumpToLine700 = true;
                                                }
                                            }
                                        }

                                        if (!bJumpToLine100_2 && !bJumpToLine800_1 && !bJumpToLine700)
                                        {
                                            //line600
                                            //if (fLog!=NULL)
                                            //fprintf(fLog, "600\fftSize");

                                            //transform for odd factors
                                            currentFactor=nfac[factInd-1];
                                            kspnn=kspan;
                                            kspan=kspan/currentFactor;
                                            if (currentFactor == 3) 
                                            {
                                                bLoopLine320 = true;
                                                bJumpToLine320 = true;
                                            }
                                            else
                                            {
                                                bLoopLine320 = false;
                                                bJumpToLine320 = false;
                                            }
                                        }
                                    }
                                }

                                if (!bJumpToLine100_2)
                                {
                                    if (!bJumpToLine320 || bJumpToLine800 || bJumpToLine800_1 || bJumpToLine700)
                                    {
                                        if (!bJumpToLine800 && !bJumpToLine800_1 && !bJumpToLine700)
                                        {
                                            if (currentFactor == 5) 
                                            {
                                                bLoopLine510 = true;
                                                bJumpToLine510 = true;
                                                bJumpToLine320Prev = bJumpToLine320;
                                                bLoopLine320Prev = bLoopLine320;
                                                bLoopLine320 = false;
                                                bJumpToLine320  = false;
                                            }
                                            else
                                            {
                                                bLoopLine510 = false;
                                                bJumpToLine510 = false;
                                            }
                                        }

                                        if (!bJumpToLine510 || bJumpToLine800 || bJumpToLine800_1 || bJumpToLine700)
                                        {
                                            if (!bJumpToLine800 && !bJumpToLine800_1)
                                            {
                                                if (!bJumpToLine700)
                                                {
                                                    if (currentFactor == jf) 
                                                        bJumpToLine640_0 = true;
                                                    else
                                                        bJumpToLine640_0 = false;

                                                    if (!bJumpToLine640_0)
                                                    {
                                                        jf=currentFactor;
                                                        s1=rad/currentFactor;
                                                        c1=Math.cos(s1);
                                                        s1=Math.sin(s1);
                                                        if (jf > maxf)
                                                        {
                                                            isn=0;
                                                            System.out.println("Array bounds exceeded within subroutine fft\fftSize");
                                                            return;
                                                        }

                                                        ck[jf-1]=1.0;
                                                        sk[jf-1]=0.0;

                                                        jCount=1;

                                                        do{ //line630   
                                                            //if (fLog!=NULL)
                                                            //fprintf(fLog, "630\fftSize");

                                                            ck[jCount-1]=ck[currentFactor-1]*c1+sk[currentFactor-1]*s1;
                                                            sk[jCount-1]=ck[currentFactor-1]*s1-sk[currentFactor-1]*c1;
                                                            currentFactor=currentFactor-1;
                                                            ck[currentFactor-1]=ck[jCount-1];
                                                            sk[currentFactor-1]=-sk[jCount-1];
                                                            jCount=jCount+1;
                                                        }while(jCount < currentFactor);
                                                    }

                                                    do{ //line640
                                                        //if (fLog!=NULL)
                                                        //fprintf(fLog, "640\fftSize");

                                                        bLoopLine640 = false;
                                                        bJumpToLine640 = false;
                                                        k1=kk;
                                                        k2=kk+kspnn;
                                                        aa=a[kk-1];
                                                        bb=b[kk-1];
                                                        ak=aa;
                                                        bk=bb;
                                                        jCount=1;
                                                        k1=k1+kspan;

                                                        do{ //line650
                                                            //if (fLog!=NULL)
                                                            //fprintf(fLog, "650\fftSize");

                                                            k2=k2-kspan;
                                                            jCount=jCount+1;
                                                            at[jCount-1]=a[k1-1]+a[k2-1];
                                                            ak=at[jCount-1]+ak;
                                                            bt[jCount-1]=b[k1-1]+b[k2-1];
                                                            bk=bt[jCount-1]+bk;
                                                            jCount=jCount+1;
                                                            at[jCount-1]=a[k1-1]-a[k2-1];
                                                            bt[jCount-1]=b[k1-1]-b[k2-1];
                                                            k1=k1+kspan;
                                                        }while(k1 < k2);

                                                        a[kk-1]=ak;
                                                        b[kk-1]=bk;
                                                        k1=kk;
                                                        k2=kk+kspnn;
                                                        jCount=1;

                                                        do{ //line660
                                                            //if (fLog!=NULL)
                                                            //fprintf(fLog, "660\fftSize");

                                                            k1=k1+kspan;
                                                            k2=k2-kspan;
                                                            jj=jCount;
                                                            ak=aa;
                                                            bk=bb;
                                                            aj=0.0;
                                                            bj=0.0;
                                                            currentFactor=1;

                                                            do{ //line670
                                                                //if (fLog!=NULL)
                                                                //fprintf(fLog, "670\fftSize");

                                                                currentFactor=currentFactor+1;
                                                                ak=at[currentFactor-1]*ck[jj-1]+ak;
                                                                bk=bt[currentFactor-1]*ck[jj-1]+bk;
                                                                currentFactor=currentFactor+1;

                                                                aj=at[currentFactor-1]*sk[jj-1]+aj;
                                                                bj=bt[currentFactor-1]*sk[jj-1]+bj;
                                                                jj=jj+jCount;
                                                                if (jj > jf)
                                                                    jj=jj-jf;
                                                            }while(currentFactor < jf);

                                                            currentFactor=jf-jCount;
                                                            a[k1-1]=ak-bj;
                                                            b[k1-1]=bk+aj;
                                                            a[k2-1]=ak+bj;
                                                            b[k2-1]=bk-aj;
                                                            jCount=jCount+1;
                                                        }while(jCount < currentFactor);

                                                        kk=kk+kspnn;
                                                        if (kk <= nn) 
                                                        {
                                                            bLoopLine640 = true;
                                                            bJumpToLine640 = true;
                                                        }
                                                        else
                                                        {
                                                            bLoopLine640 = false;
                                                            bJumpToLine640 = false;
                                                        }

                                                        if (!bJumpToLine640)
                                                        {
                                                            kk=kk-nn;
                                                            if (kk <= kspan)
                                                            {
                                                                bLoopLine640 = true;
                                                                bJumpToLine640 = true;
                                                            }
                                                            else
                                                            {
                                                                bLoopLine640 = false;
                                                                bJumpToLine640 = false;
                                                            }
                                                        }
                                                    }while(bLoopLine640);
                                                }

                                                //multiply by rotation factor [except for factors of 2 and 4-1]
                                                //line700
                                                //if (fLog!=NULL)
                                                //fprintf(fLog, "700\fftSize");

                                                bJumpToLine700 = false;
                                                if (factInd == mCount)
                                                    bJumpToLine800 = true;
                                                else
                                                    bJumpToLine800 = false;

                                                if (!bJumpToLine800)
                                                {
                                                    kk=jc+1;

                                                    do{ //line710
                                                        //if (fLog!=NULL)
                                                        //fprintf(fLog, "710\fftSize");
                                                        c2=1.0-cd;
                                                        s1=sd;

                                                        do{ //line720
                                                            //if (fLog!=NULL)
                                                            //fprintf(fLog, "720\fftSize");
                                                            c1=c2;
                                                            s2=s1;
                                                            kk=kk+kspan;

                                                            do{ //line730
                                                                //if (fLog!=NULL)
                                                                //fprintf(fLog, "730\fftSize");

                                                                bJumpToLine730 = false;
                                                                bLoopLine730 = false;

                                                                ak=a[kk-1];
                                                                a[kk-1]=c2*ak-s2*b[kk-1];
                                                                b[kk-1]=s2*ak+c2*b[kk-1];
                                                                kk=kk+kspnn;
                                                                if (kk <= nt)
                                                                {   
                                                                    bJumpToLine730 = true;
                                                                    bLoopLine730 = true;
                                                                }
                                                                else
                                                                {
                                                                    bJumpToLine730 = false;
                                                                    bLoopLine730 = false;
                                                                }

                                                                if (!bJumpToLine730)
                                                                {
                                                                    ak=s1*s2;
                                                                    s2=s1*c2+c1*s2;
                                                                    c2=c1*c2-ak;
                                                                    kk=kk-nt+kspan;

                                                                    if (kk <= kspnn) 
                                                                    {   
                                                                        bJumpToLine730 = true;
                                                                        bLoopLine730 = true;
                                                                    }
                                                                    else
                                                                    {
                                                                        bJumpToLine730 = false;
                                                                        bLoopLine730 = false;
                                                                    }
                                                                }

                                                            }while(bLoopLine730);

                                                            c2=c1-(cd*c1+sd*s1);
                                                            s1=s1+(sd*c1-cd*s1);
                                                            c1=2.0-(c2*c2+s1*s1);
                                                            s1=c1*s1;
                                                            c2=c1*c2;
                                                            kk=kk-kspnn+jc;
                                                        }while(kk <= kspan);

                                                        kk=kk-kspan+jc+inc;
                                                    }while(kk <= jc+jc);

                                                    bLoopLine100 = true;
                                                    bJumpToLine100_3 = true;

                                                    bLoopLine510Prev_3 = bLoopLine510;
                                                    bLoopLine320Prev_3 = bLoopLine320;
                                                    bJumpToLine510Prev_3 = bJumpToLine510;
                                                    bJumpToLine320Prev_3 = bJumpToLine320;

                                                    bLoopLine510 = false;
                                                    bLoopLine320 = false;
                                                    bJumpToLine510 = false;
                                                    bJumpToLine320 = false;
                                                }
                                            }

                                            if (!bJumpToLine100_3)
                                            {
                                                //permute the results to normal order---done in two stages
                                                // permutation for square factors of fftSize
                                                bJumpToLine800 = false;
                                                bJumpToLine800_1 = false;

                                                //line800
                                                //if (fLog!=NULL)
                                                //fprintf(fLog, "800\fftSize");

                                                np[1-1]=ks;
                                                if (kt == 0) 
                                                    bJumpToLine890 = true;

                                                if (!bJumpToLine890)
                                                {
                                                    currentFactor=kt+kt+1;
                                                    if (mCount < currentFactor)
                                                        currentFactor=currentFactor-1;
                                                    jCount=1;
                                                    np[currentFactor+1-1]=jc;

                                                    do{ //line810
                                                        //if (fLog!=NULL)
                                                        //fprintf(fLog, "810\fftSize");

                                                        np[jCount+1-1]=np[jCount-1]/nfac[jCount-1];
                                                        np[currentFactor-1]=np[currentFactor+1-1]*nfac[jCount-1];
                                                        jCount=jCount+1;
                                                        currentFactor=currentFactor-1;
                                                    }while(jCount < currentFactor);

                                                    k3=np[currentFactor+1-1];
                                                    kspan=np[2-1];
                                                    kk=jc+1;
                                                    k2=kspan+1;
                                                    jCount=1;
                                                }

                                                if (fftSize == ntot || !bJumpToLine890) 
                                                {
                                                    if (!bJumpToLine890)
                                                    {
                                                        //permutation for single-variate transform [optional code-1]
                                                        do{ //line820
                                                            //if (fLog!=NULL)
                                                            //fprintf(fLog, "820\fftSize");

                                                            bLoopLine820 = false;
                                                            bJumpToLine820 = false;
                                                            ak=a[kk-1];
                                                            a[kk-1]=a[k2-1];
                                                            a[k2-1]=ak;
                                                            bk=b[kk-1];
                                                            b[kk-1]=b[k2-1];
                                                            b[k2-1]=bk;
                                                            kk=kk+inc;
                                                            k2=kspan+k2;
                                                            if (k2 < ks)
                                                            {
                                                                bLoopLine820 = true;
                                                                bJumpToLine820 = true;
                                                            }
                                                            else
                                                            {
                                                                bLoopLine820 = false;
                                                                bJumpToLine820 = false;
                                                            }

                                                            if (!bJumpToLine820)
                                                            {
                                                                do{ //line830
                                                                    //if (fLog!=NULL)
                                                                    //fprintf(fLog, "830\fftSize");

                                                                    bLoopLine830 = false;
                                                                    bJumpToLine830 = false;

                                                                    k2=k2-np[jCount-1];
                                                                    jCount=jCount+1;
                                                                    k2=np[jCount+1-1]+k2;
                                                                    if (k2 > np[jCount-1]) 
                                                                    {
                                                                        bLoopLine830 = true;
                                                                        bJumpToLine830 = true;
                                                                    }
                                                                    else
                                                                    {
                                                                        bLoopLine830 = false;
                                                                        bJumpToLine830 = false;
                                                                    }

                                                                    if (!bJumpToLine830)
                                                                    {
                                                                        jCount=1;

                                                                        do{ //line840
                                                                            //if (fLog!=NULL)
                                                                            //fprintf(fLog, "840\fftSize");

                                                                            bLoopLine840 = false;
                                                                            bJumpToLine840 = false;
                                                                            if (kk < k2) 
                                                                            {
                                                                                bLoopLine820 = true;
                                                                                bJumpToLine820 = true;
                                                                                bLoopLine830 = false;
                                                                                bJumpToLine830 = false;
                                                                            }
                                                                            else
                                                                            {
                                                                                bLoopLine820 = false;
                                                                                bJumpToLine820 = false;
                                                                            }

                                                                            if (!bJumpToLine820)
                                                                            {
                                                                                kk=kk+inc;
                                                                                k2=kspan+k2;
                                                                                if (k2 < ks) 
                                                                                {
                                                                                    bLoopLine840 = true;
                                                                                    bJumpToLine840 = true;
                                                                                }
                                                                                else
                                                                                {
                                                                                    bLoopLine840 = false;
                                                                                    bJumpToLine840 = false;
                                                                                }

                                                                                if (!bJumpToLine840)
                                                                                {
                                                                                    if (kk < ks) 
                                                                                    {
                                                                                        bLoopLine830 = true;
                                                                                        bJumpToLine830 = true;
                                                                                    }
                                                                                    else
                                                                                    {
                                                                                        bLoopLine830 = false;
                                                                                        bJumpToLine830 = false;
                                                                                    }

                                                                                    if (!bJumpToLine830)
                                                                                        jc=k3;
                                                                                }
                                                                            }
                                                                        }while(bLoopLine840);
                                                                    }
                                                                }while(bLoopLine830);
                                                            }
                                                        }while(bLoopLine820);
                                                    }

                                                    bJumpToLine890 = true;
                                                }

                                                if (!bJumpToLine890)
                                                {
                                                    //permutation for multivariate transform
                                                    do{ //line850
                                                        //if (fLog!=NULL)
                                                        //fprintf(fLog, "850\fftSize");

                                                        bLoopLine850 = false;
                                                        bJumpToLine850 = false;

                                                        currentFactor=kk+jc;

                                                        do{ //line860
                                                            //if (fLog!=NULL)
                                                            //fprintf(fLog, "860\fftSize");

                                                            ak=a[kk-1];
                                                            a[kk-1]=a[k2-1];
                                                            a[k2-1]=ak;
                                                            bk=b[kk-1];
                                                            b[kk-1]=b[k2-1];
                                                            b[k2-1]=bk;
                                                            kk=kk+inc;
                                                            k2=k2+inc;
                                                        }while(kk < currentFactor);

                                                        kk=kk+ks-jc;
                                                        k2=k2+ks-jc;
                                                        if (kk < nt) 
                                                        {
                                                            bLoopLine850 = true;
                                                            bJumpToLine850 = true;
                                                        }
                                                        else
                                                        {
                                                            bLoopLine850 = false;
                                                            bJumpToLine850 = false;
                                                        }

                                                        if (!bJumpToLine850)
                                                        {
                                                            k2=k2-nt+kspan;
                                                            kk=kk-nt+jc;
                                                            if (k2 < ks) 
                                                            {
                                                                bLoopLine850 = true;
                                                                bJumpToLine850 = true;
                                                            }
                                                            else
                                                            {
                                                                bLoopLine850 = false;
                                                                bJumpToLine850 = false;
                                                            }

                                                            if (!bJumpToLine850)
                                                            {
                                                                do{ //line870
                                                                    //if (fLog!=NULL)
                                                                    //fprintf(fLog, "870\fftSize");

                                                                    bLoopLine870 = false;
                                                                    bJumpToLine870 = false;
                                                                    k2=k2-np[jCount-1];
                                                                    jCount=jCount+1;
                                                                    k2=np[jCount+1-1]+k2;
                                                                    if (k2 > np[jCount-1])
                                                                    {
                                                                        bLoopLine870 = true;
                                                                        bJumpToLine870 = true;
                                                                    }
                                                                    else
                                                                    {
                                                                        bLoopLine870 = false;
                                                                        bJumpToLine870 = false;
                                                                    }

                                                                    if (!bJumpToLine870)
                                                                    {
                                                                        jCount=1;

                                                                        do{ //line880
                                                                            //if (fLog!=NULL)
                                                                            //fprintf(fLog, "880\fftSize");

                                                                            bLoopLine880 = false;
                                                                            bJumpToLine880 = false;

                                                                            if (kk < k2)
                                                                            {
                                                                                bLoopLine850 = true;
                                                                                bJumpToLine850 = true;
                                                                            }
                                                                            else
                                                                            {
                                                                                bLoopLine850 = false;
                                                                                bJumpToLine850 = false;
                                                                            }

                                                                            if (!bJumpToLine850)
                                                                            {
                                                                                kk=kk+jc;
                                                                                k2=kspan+k2;
                                                                                if (k2 < ks) 
                                                                                {
                                                                                    bLoopLine880 = true;
                                                                                    bJumpToLine880 = true;
                                                                                }
                                                                                else
                                                                                {
                                                                                    bLoopLine880 = false;
                                                                                    bJumpToLine880 = false;
                                                                                }

                                                                                if (!bJumpToLine880)
                                                                                {
                                                                                    if (kk < ks)
                                                                                    {
                                                                                        bLoopLine870 = true;
                                                                                        bJumpToLine870 = true;
                                                                                    }
                                                                                    else
                                                                                    {
                                                                                        bLoopLine870 = false;
                                                                                        bJumpToLine870 = false;
                                                                                    }

                                                                                    if (!bJumpToLine870)
                                                                                        jc=k3;
                                                                                }
                                                                            }
                                                                        }while(bLoopLine880);
                                                                    }
                                                                }while(bLoopLine870);
                                                            }
                                                        }
                                                    }while(bLoopLine850);
                                                }

                                                //line890
                                                //if (fLog!=NULL)
                                                //fprintf(fLog, "890\fftSize");

                                                bJumpToLine890 = false;
                                                if (2*kt+1 >= mCount) 
                                                    return;
                                                kspnn=np[kt+1-1];
                                                //permutation for square-free factors of fftSize
                                                jCount=mCount-kt;
                                                nfac[jCount+1-1]=1;

                                                do{ //line900
                                                    //if (fLog!=NULL)
                                                    //fprintf(fLog, "900\fftSize");

                                                    nfac[jCount-1]=nfac[jCount-1]*nfac[jCount+1-1];
                                                    jCount=jCount-1;
                                                }while(jCount != kt);

                                                kt=kt+1;
                                                nn=nfac[kt-1]-1;
                                                if (nn > maxp)
                                                {
                                                    isn=0;
                                                    System.out.println("Array bounds exceeded within subroutine fft\fftSize");
                                                    return;
                                                }

                                                jj=0;
                                                jCount=0;
                                                bJumpToLine906 = true;

                                                do{
                                                    if (!bJumpToLine906)
                                                    {
                                                        //line902
                                                        //if (fLog!=NULL)
                                                        //fprintf(fLog, "902\fftSize");

                                                        bLoopLine902 = false;
                                                        bJumpToLine902 = false;
                                                        jj=jj-k2;
                                                        k2=kk;
                                                        currentFactor=currentFactor+1;
                                                        kk=nfac[currentFactor-1];
                                                    }

                                                    do{
                                                        if (!bJumpToLine906)
                                                        {
                                                            //line904
                                                            //if (fLog!=NULL)
                                                            //fprintf(fLog, "904\fftSize");

                                                            bLoopLine904 = false;
                                                            bJumpToLine904 = false;

                                                            jj=kk+jj;
                                                            if (jj >= k2) 
                                                            {
                                                                bLoopLine902 = true;
                                                                bJumpToLine902 = true;
                                                            }
                                                            else
                                                            {
                                                                bLoopLine902 = false;
                                                                bJumpToLine902 = false;
                                                            }
                                                        }

                                                        if (!bJumpToLine902 || bJumpToLine906)
                                                        {
                                                            if (!bJumpToLine906)
                                                                np[jCount-1]=jj;                    

                                                            //line906
                                                            //if (fLog!=NULL)
                                                            //fprintf(fLog, "906\fftSize");

                                                            bJumpToLine906 = false;
                                                            k2=nfac[kt-1];
                                                            currentFactor=kt+1;
                                                            kk=nfac[currentFactor-1];
                                                            jCount=jCount+1;
                                                            if (jCount <= nn)
                                                            {
                                                                bLoopLine904 = true;
                                                                bJumpToLine904 = true;
                                                            }
                                                            else
                                                            {
                                                                bLoopLine904 = false;
                                                                bJumpToLine904 = false;
                                                            }

                                                            if (!bJumpToLine904)
                                                            {
                                                                //determine the permutation cycles of length greater than 1
                                                                jCount=0;

                                                                bJumpToLine914_0 = true;

                                                                do{
                                                                    if (!bJumpToLine914_0)
                                                                    {
                                                                        //line910
                                                                        //if (fLog!=NULL)
                                                                        //fprintf(fLog, "910\fftSize");

                                                                        bLoopLine910 = false;
                                                                        bJumpToLine910 = false;
                                                                        currentFactor=kk;
                                                                        kk=np[currentFactor-1];
                                                                        np[currentFactor-1]=-kk;
                                                                        if (kk != jCount) 
                                                                        {
                                                                            bLoopLine910 = true;
                                                                            bJumpToLine910 = true;
                                                                        }
                                                                        else
                                                                        {
                                                                            bLoopLine910 = false;
                                                                            bJumpToLine910 = false;
                                                                        }
                                                                    }

                                                                    if (!bJumpToLine910 || bJumpToLine914_0)
                                                                    {
                                                                        if (!bJumpToLine914_0)
                                                                            k3=kk;

                                                                        do{ //line914
                                                                            //if (fLog!=NULL)
                                                                            //fprintf(fLog, "914\fftSize");

                                                                            bLoopLine914 = false;
                                                                            bJumpToLine914 = false;
                                                                            bJumpToLine914_0 =false;

                                                                            jCount=jCount+1;
                                                                            kk=np[jCount-1];
                                                                            if (kk < 0) 
                                                                            {
                                                                                bLoopLine914 = true;
                                                                                bJumpToLine914 = true;
                                                                            }
                                                                            else
                                                                            {
                                                                                bLoopLine914 = false;
                                                                                bJumpToLine914 = false;
                                                                            }

                                                                            if (!bJumpToLine914)
                                                                            {
                                                                                if (kk != jCount)
                                                                                {
                                                                                    bLoopLine910 = true;
                                                                                    bJumpToLine910 = true;
                                                                                }
                                                                                else
                                                                                {
                                                                                    bLoopLine910 = false;
                                                                                    bJumpToLine910 = false;
                                                                                }

                                                                                if (!bJumpToLine910)
                                                                                {
                                                                                    np[jCount-1]=-jCount;
                                                                                    if (jCount != nn)
                                                                                    {
                                                                                        bLoopLine914 = true;
                                                                                        bJumpToLine914 = true;
                                                                                    }
                                                                                    else
                                                                                    {
                                                                                        bLoopLine914 = false;
                                                                                        bJumpToLine914 = false;
                                                                                    }

                                                                                    if (!bJumpToLine914)
                                                                                    {
                                                                                        maxf=inc*maxf;
                                                                                        bJumpToLine950 = true;
                                                                                    }
                                                                                }
                                                                            }
                                                                        }while(bLoopLine914);
                                                                    }
                                                                }while(bLoopLine910);

                                                                //reorder a and b, following the permutation cycles
                                                                do{
                                                                    bLoopLine924 = false;
                                                                    bJumpToLine924 =false;
                                                                    if (!bJumpToLine950)
                                                                    { 
                                                                        //line924
                                                                        //if (fLog!=NULL)
                                                                        //fprintf(fLog, "924\fftSize");

                                                                        jCount=jCount-1;
                                                                        if (np[jCount-1] < 0) 
                                                                        {
                                                                            bLoopLine924 = true;
                                                                            bJumpToLine924 = true;
                                                                        }
                                                                        else
                                                                        {
                                                                            bLoopLine924 = false;
                                                                            bJumpToLine924 = false;
                                                                        }

                                                                        if (!bJumpToLine924)
                                                                        {
                                                                            jj=jc;

                                                                            do{ //line926
                                                                                //if (fLog!=NULL)
                                                                                //fprintf(fLog, "926\fftSize");

                                                                                kspan=jj;
                                                                                if (jj > maxf) 
                                                                                    kspan=maxf;
                                                                                jj=jj-kspan;
                                                                                currentFactor=np[jCount-1];
                                                                                kk=jc*currentFactor+factInd+jj;
                                                                                k1=kk+kspan;
                                                                                k2=0;

                                                                                do{ //line928
                                                                                    //if (fLog!=NULL)
                                                                                    //fprintf(fLog, "928\fftSize");

                                                                                    k2=k2+1;
                                                                                    at[k2-1]=a[k1-1];
                                                                                    bt[k2-1]=b[k1-1];
                                                                                    k1=k1-inc;
                                                                                }while(k1 != kk);

                                                                                do{ //line932
                                                                                    //if (fLog!=NULL)
                                                                                    //fprintf(fLog, "932\fftSize");

                                                                                    k1=kk+kspan;
                                                                                    k2=k1-jc*(currentFactor+np[currentFactor-1]);
                                                                                    currentFactor=-np[currentFactor-1];

                                                                                    do{ //line936
                                                                                        //if (fLog!=NULL)
                                                                                        //fprintf(fLog, "936\fftSize");

                                                                                        a[k1-1]=a[k2-1];
                                                                                        b[k1-1]=b[k2-1];
                                                                                        k1=k1-inc;
                                                                                        k2=k2-inc;
                                                                                    }while (k1 != kk);

                                                                                    kk=k2;
                                                                                }while(currentFactor != jCount);

                                                                                k1=kk+kspan;
                                                                                k2=0;

                                                                                do{ //line940
                                                                                    //if (fLog!=NULL)
                                                                                    //fprintf(fLog, "940\fftSize");

                                                                                    k2=k2+1;
                                                                                    a[k1-1]=at[k2-1];
                                                                                    b[k1-1]=bt[k2-1];
                                                                                    k1=k1-inc;
                                                                                }while(k1 != kk);
                                                                            }while(jj != 0);

                                                                            if (jCount != 1)
                                                                            {
                                                                                bLoopLine924 = true;
                                                                                bJumpToLine924 = true;
                                                                            }
                                                                            else
                                                                            {
                                                                                bLoopLine924 = false;
                                                                                bJumpToLine924 = false;
                                                                            }
                                                                        }
                                                                    }

                                                                    if (bJumpToLine950 || !bJumpToLine924)
                                                                    {
                                                                        //line950
                                                                        //if (fLog!=NULL)
                                                                        //fprintf(fLog, "950\fftSize");

                                                                        bJumpToLine950 = false;
                                                                        jCount=k3+1;
                                                                        nt=nt-kspnn;
                                                                        factInd=nt-inc+1;
                                                                        if (nt >= 0)
                                                                        {
                                                                            bLoopLine924 = true;
                                                                            bJumpToLine924 = true;
                                                                        }
                                                                        else
                                                                        {
                                                                            bLoopLine924 = false;
                                                                            bJumpToLine924 = false;
                                                                        }
                                                                    }
                                                                }while(bLoopLine924);
                                                            }
                                                        }
                                                    }while(bLoopLine904);
                                                }while(bLoopLine902);
                                            }
                                        }
                                    }
                                }
                            }while (bLoopLine510);
                        }
                    }
                }while(bLoopLine320);
            }
        }while(bLoopLine100);
    }

    // Run a random number of tests:
    // By creating a real-valued sequence of random size filled in with random values
    // Taking a random point FFT (fftSize>=total real points)
    // Taking a random point IFFT (ifftSize>=total real points)
    // Comparing the original random real sequence with the IFFT output
    public static void test_fft_ifft_real_random()
    {
        String strMessage;

        int i, fftSize, ifftSize;
        int totalPoints = (int)(10*Math.random()+50);
        int [] numPoints = new int[totalPoints];

        for (i=0; i<totalPoints; i++)
            numPoints[i] = (int)(6000*Math.random()+23);

        for (i=0; i<totalPoints; i++)
        {
            fftSize = numPoints[i]+(int)(100*Math.random());
            ifftSize = numPoints[i]+(int)(100*Math.random());

            double [] x = new double[numPoints[i]];

            for (int j=0; j<numPoints[i]; j++)
                x[j] = 40000*(Math.random()-0.5);

            Complex h = fftReal(x, numPoints[i], fftSize);

            strMessage = fftSize + "-point FFT computed from " + numPoints[i] + " random real values...";
            System.out.println(strMessage);

            double [] y  = ifftReal(h, ifftSize);

            strMessage = ifftSize + "-point IFFT computed...";
            System.out.println(strMessage);

            for (int j=0; j<numPoints[i]; j++)
            {
                if (Math.abs(x[j]-y[j])>1e-4)
                    System.out.println("Detected difference...");
            }

            strMessage = "Test #" + (i+1) + " of " + totalPoints + " complete for " + numPoints[i] + " random values...\n";
            System.out.println(strMessage);
        }
    }

    /**
     * @param args
     */
    public static void main(String[] args) {
        test_fft_ifft_real_random();
    }

}
