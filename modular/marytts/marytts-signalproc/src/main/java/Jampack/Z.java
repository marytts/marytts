package Jampack;

/**
 * Z is a mutable complex variable class.  It is designed to
 * perform complex arithmetic without creating a new Z at
 * each operation.  Specifically, binary operations have the
 * form c.op(a,b), in which a, b, and c need not be different.
 * The method  places the complex number a.op.b in
 * c.  The method also returns a pointer to c.  Thus the
 * class supports two styles of programming.  For example
 * to compute e = a*b + c*d you can write <p>
 * 
 * z1.Times(a,b) <br>
 * z2.Times(c,d) <br>
 * e.Plus(z1,z2) <p>
 * 
 * or <p>
 *
 * e.Plus(z1.Times(a,b), z2.Times(a,b)) <p>
 *
 * Since objects of class Z are mutable, the use of the assignment
 * operator "=" with these objects is deprecated. Use Eq. <p>
 *
 * The functions are reasonably resistent to overflow and underflow.
 * But the more complicated ones could almost certainly be improved.

   @version Pre-alpha
   @author G. W. Stewart
*/

public class Z{

/** Complex 1. */
    public static final Z ONE = new Z(1,0);

/** Complex 0. */
    public static final Z ZERO = new Z(0,0);

/** Imaginary unit. */
    public static final Z I = new Z(0,1);


/** The real part of Z. */
    public double re;

/** The imaginary part of Z. */
    public double im;


/**
 * Creates a Z and initializes it to zero.

   @return a Z initialized to zero.
*/
   public Z(){
      re = 0.;
      im = 0.;
   }

/**
 * Creates a Z and initializes its real and imaginary parts.

   @param     x a double
   @param     y a double
   @return    x + iy
*/

   public Z(double x, double y){
      re = x;
      im = y;
   }

/**
 * Creates a Z and initializes its real part.

   @param     x a double
   @return    x + i*0
*/

   public Z(double x){
      re = x;
      im = 0;
   }

/**
 * Creates a Z and initializes it to another Z.

   @param     a a Z
   @return    a
*/

   public Z(Z a){
      re = a.re;
      im = a.im;
   }

/**
 * Tests two Z'z for equality.

   @param     z1 a Z
   @param     z2 a Z
   @return    true if z1=z2, otherwise false
*/

   public boolean IsEqual(Z z1, Z z2){
      if (z1.re == z2.re && z1.im == z2.im){
         return true;
      }
      else{
         return false;
      }
   }

/**
 * Resets the real and imaginary parts of a Z to those of another Z.

   @param     a a Z
   @return    this = a;   
*/  

   public Z Eq(Z a){
      re = a.re;
      im = a.im;   
      return this;
   }

/**
 * Resets the real and imaginary parts of a Z.

   @param     a a double
   @param     b a double
   @return    this = a + ib
*/

   public Z Eq(double a, double b){
      re = a;
      im = b;
      return this;
   }

/**
 * Interchanges the real and imaginary parts of two Z's.

   @param     a a Z
   @return    this = a, with a set to the original
              value of this.
*/

   public Z Exch(Z a){
      double t;
      t = re; re = a.re; a.re = t;
      t = im; im = a.im; a.im = t;
      return this;
   }

/**
   Computes the 1-norm of a Z
*/
   public static double abs1(Z z){
      return Math.abs(z.re) + Math.abs(z.im);
   }

/**
   Computes the absolute value of a Z.

   @param     z a Z
   @return    the absolute vaue of Z
*/

   public static double abs(Z z){
      double are, aim, rho;
      are = Math.abs(z.re);
      aim = Math.abs(z.im);
      if (are+aim == 0) return 0;
      if (are >= aim){
         rho = aim/are;
         return are*Math.sqrt(1+rho*rho);
      }
      else{
         rho = are/aim;
         return aim*Math.sqrt(1+rho*rho);
      }
   }

/**
 * Computes the conjugate of a Z.

   @param     a a Z
   @return    this = conj(a);
*/

   public Z Conj(Z a){
      re = a.re;
      im = -a.im;
      return this;
   }

/**
 * Computes unary minus of a Z.
   @param     a a Z
   @return    this = -a;
*/

   public Z Minus(Z a){
      re = -a.re;
      im = -a.im;
      return this;
   }


/**
 * Computes the sum of two Z's.

   @param     a a Z
   @param     b a Z
   @return    this = a + b
*/
   public Z Plus(Z a, Z b){
      re = a.re + b.re;
      im = a.im + b.im;
      return this;
   }

/**
 * Computes the difference of two Z's.

   @param     a a Z
   @param     b a Z
   @return    this = a - b
*/

   public Z Minus(Z a, Z b){
      re = a.re - b.re;
      im = a.im - b.im;
      return this;
   }

/**
 * Computes the product of two Z's.

   @param     a a Z
   @param     b a Z
   @return    this = ab
*/


   public Z Times(Z a, Z b){
      double tre;
      tre = a.re*b.re - a.im*b.im;
      im = a.im*b.re + a.re*b.im;
      re = tre;
      return this;
   }

/**
 *  Computes the product of a double and a Z.

   @param     a a double
   @param     b a Z
   @return    this = ab
*/

   public Z  Times(double a, Z b){

      re = a*b.re;
      im = a*b.im;
      return this;
   }

/**
 * Computes the quotient of two Z's.  Throws a JampackException if
 * the denominator is zero.

   @param     a a Z
   @param     b a Z
   @return    this = a/b
   @exception JampackException
              Thrown if b is zero.
*/

   public Z Div(Z a, Z b)
   throws JampackException{
      double avi, t, tre, tim;
         avi = abs(b);
         if (avi == 0){
            throw new JampackException
               ("Divide by zero.");
         }
         avi = 1./avi;
         tre = b.re*avi;
         tim = -b.im*avi;
         t = (a.re*tre - a.im*tim)*avi;
         im = (a.im*tre + a.re*tim)*avi;
         re = t;
         return this;
   }

/**
 * Computes the quotient of a Z and a double.  Throws a JampackException
 * if the denominator is zero.
   @param     a a Z
   @param     b a double
   @return    this = a/b
   @exception JampackException
              Thrown if b is zero.
*/

   public Z Div(Z a, double b)
   throws JampackException{
      if (b == 0){
         throw new JampackException
            ("Divide by zero.");
      }
      re = a.re/b;
      im = a.im/b;
      return this;
   }

/**
 * Computes the principal value of the square root of a Z.

   @param     a a Z
   @param     this = sqrt(a)
*/


   public Z Sqrt(Z a){
      double t, tre, tim;

      t = Z.abs(a);

      if (Math.abs(a.re) <= Math.abs(a.im)){
         // No cancellation in these formulas
         tre = Math.sqrt(0.5*(t+a.re));
         tim = Math.sqrt(0.5*(t-a.re));
      }
      else{
         // Stable computation of the above formulas
         if (a.re > 0){
            tre = t + a.re;
            tim = Math.abs(a.im)*Math.sqrt(0.5/tre);
            tre = Math.sqrt(0.5*tre);
         }
         else{
            tim = t - a.re;
            tre = Math.abs(a.im)*Math.sqrt(0.5/tim);
            tim = Math.sqrt(0.5*tim);
         }
      }
      if (a.im < 0)
         tim = -tim;
      re = tre;
      im = tim;
      return this;

   }

}

