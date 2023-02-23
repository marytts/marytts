package Jampack;

/**
   Z1 implements a one-dimensional array of complex numbers as a
   two arrays of type double.  The addressing is zero based.
   It is necessary to provided one-dimensional complex arrays
   whose real and imaginary parts are contiguous in storage.

   @version Pre-alpha
   @author G. W. Stewart
*/

public class Z1{

   protected int n;
   protected double re[];
   protected double im[];

/**
   Creates a Z1 initializes to zero.

   @param     n a positive integer
   @return    A Z1 of length n
   @exception JampackException
              Thrown if n<=0.
*/

   public Z1(int n)
   throws JampackException{
      if (n <= 0){
          throw new JampackException
             ("Nonpositive dimension.");
      }

      this.n = n;
      re = new double[n];
      im = new double[n];
   }

/**
   Returns the ith element of a Z1 as a Z.

   @param     i an integer
   @return    The ith elemeent of this Z1
*/

   public Z get(int i){

      return new Z(re[i], im[i]);
   }

/**
   Sets the ith element of a Z1 to a Z.

   @param     i an integer
   @param     z a Z
   @return    resets the ith element of this Z1 to z
*/

   public void put(int i, Z z){

      re[i] = z.re;
      im[i] = z.im;

   }

/**
   Sets the real and imaginary parts of the ith element of a Z1.

   @param     i    an integer
   @param     real a double
   @param     imag a double
   @return    resets the ith component of this Z1 to
              real + i*imag
*/ 

   public void put(int i, double real, double imag){
      re[i] = real;
      im[i] = imag;
   }

/**
   Multiplies the ith element of a Z1 by a Z.

   @param     i an integer
   @param     z a Z
   @return    multiplies the ith element of this Z1 by z.
*/

   public void Times(int i, Z z){
      double t;

      t = re[i]*z.re - im[i]*z.im;
      im[i] = re[i]*z.im + im[i]*z.re;
      re[i] = t;
   }
}
