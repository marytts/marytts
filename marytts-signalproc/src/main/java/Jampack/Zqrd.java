package Jampack;

/**
  Implements a QR decomposition.  Specifically,
  given a matrix A there are is a unitary matrix Q
  such that
<pre>
*    Q<sup>H</sup>A = R
</pre>
   where R is zero below its diagonal.  In constructing
   this decomposition, Zqrd represents Q as a Zmat.
   R is represented by a Zutmat.
<p>
   At a later stage an economical version of the decomposition
   will be implemented, in which only A.nc columns of Q
   are returned.

   @version Pre-alpha
   @author G. W. Stewart
*/

public class Zqrd{

/** The unitary matrix Q */
  
    public Zmat Q;

/** The R factor.  If A.nr&gt;A.nc then R is square of order
    A.nc.  Otherwise R has the same dimensions as A. */
  
    public Zutmat R;

/**
   Constructs a Zqrd from a Zmat.

   @param     A A Zmat
   @return    The Zqrd of A
   @exception JampackException
              Passed from below.
*/

   public Zqrd(Zmat A)
   throws JampackException{

      Zhqrd hqr;

      A.getProperties();
      if(A.HQR == null){
         hqr = new Zhqrd(A);
      }
      else{
         hqr = A.HQR;
      }

      R = hqr.R;
      if (A.nr > A.nc){
         R = new Zutmat(Merge.o21(R, new Zmat(A.nr-A.nc, A.nc)));
      }
      Q = Eye.o(A.nr);
      for (int k=hqr.ntran-1; k>=0; k--){
         House.ua(hqr.U[k], Q,  k+A.bx, A.rx, k+A.bx, A.rx);
      }
   }

}
