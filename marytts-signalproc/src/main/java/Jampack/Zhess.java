package Jampack;

/**
   Zhess implements the unitary reduction to Hessenberg form
   by a unitary similarity transformation.  Specifically, given
   a square matrix A, there is a unitary matrix U such that
<pre>
*      H = U^H AU
</pre>
   is upper Hessenberg.
   Zhess represents U and H as Zmats.

   @version Pre-alpha
   @author G. W. Stewart
*/

public class Zhess{

/** The upper Hessenberg matrix */
     public Zmat H;

/** The unitary matrix */
    public Zmat U;

/** Creates a Zhess from a square Zmat. Throws a 
    JampackException for nonsquare matrx.

    @param     A A Zmat
    @return    The Hessenberg form of A
    @exception JampackException
               Thrown if A is not square.
*/

   public Zhess(Zmat A)
   throws JampackException{

      if (A.nr != A.nc){
         throw new JampackException
            ("Matrix not square");
      }

      H = new Zmat(A);
      U = Eye.o(H.nr);

      Z1 work = new Z1(H.nr);

      for (int k=H.bx; k<=H.cx-2; k++){
         Z1 u = House.genc(H, k+1, H.rx, k);
         House.ua(u, H, k+1, H.rx, k+1, H.cx, work);
         House.au(H, u, H.bx, H.rx, k+1, H.cx, work);
         House.au(U, u, U.bx, U.rx, k+1, U.cx, work);
      }
   }
}
