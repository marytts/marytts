package Jampack;

/**
  Computes a Householder QR decomposition.Specifically,
  given a matrix A there are is a unitary matrix U
  such that
<pre>
*    QA = R
</pre>
   where R is zero below its diagonal.  In constructing
   this decomposition, Zhqrd represents Q as a product
   of Householder transformations with each transformation
   represented by a Z1. R is represented by a Zutmat.
   Methods are provided to apply the transformations to
   other matrices.
<br>
   Comments: The routines to postmultiply by Q are soft coded and
   should ultimately be replaced.   

   @version Pre-alpha
   @author G. W. Stewart
*/

public class Zhqrd{

/** The number of rows in A */
    public int nrow;

/** The number of columns in A*/
    public int ncol;

/** The number of Householder transformations */
    public int ntran;

/** An array containing the generating vectors for the
    Householder transformations. */
    public Z1[] U;

/** The R factor.  If nrow&gt;ncol then R is square of order
    ncol.  Otherwise R has the same dimenstions as A. */
   
    public Zutmat R;

/**
   Computes a Householder QR decomposition of a Zmat

   @param     A A Zmat
   @return    The Zhqrd of A
   @exception JampackException
              Passed from below.
*/
   public Zhqrd(Zmat A)
   throws JampackException{

      A.getProperties();

      /* Initialize. */

      nrow = A.nr;
      ncol = A.nc;
      ntran = Math.min(A.nr, A.nc);
      U = new Z1[ntran];

      /* Perform the reduction in R */

      R  = new Zutmat(A);
      for (int k=A.bx; k<A.bx+ntran; k++){

         U[k-A.bx] = House.genc(R, k, A.rx, k);
         House.ua(U[k-A.bx], R, k, A.rx, k+1, A.cx);
      }
      if (nrow > ncol){// Chop off zeros at the bottom.
         R = new Zutmat(R.get(R.bx, R.cx, R.bx, R.cx));
      }
   }

/**
   Computes the product QB.  Throws JampackException for
   inconsistent dimenstions.

   @param     B A Zmat
   @return    QB
   @exception JampackException
              Thrown for inconsistent dimensions.
*/
   

   public Zmat qb(Zmat B)
   throws JampackException{


      if (B.ncol != ncol){
         throw new JampackException
            ("Inconsistent dimensions.");
      }  

      Zmat C = new Zmat(B);

      for (int k=ntran-1; k>=0; k--){
         House.ua(U[k], C, C.bx+k, C.rx, C.bx, C.cx);
      }

      return C;

   }

/**
   Computes the product Q<sup>H</sup>B.   Throws JampackException for
   inconsistent dimenstions.

   @param     B A Zmat
   @return    Q<sup>H</sup>B
   @exception JampackException
              Thrown for inconsistent dimensions.
*/
   public Zmat qhb(Zmat B)
   throws JampackException{

 
      if (B.ncol != ncol){
         throw new JampackException
            ("Inconsistent dimensions.");
      }


      Zmat C = new Zmat(B);

      for (int k=0; k<ntran; k++){
         House.ua(U[k], C, C.bx+k, C.rx, C.bx, C.cx);
      }

      return C;

   }

/**
   Computes the product BQ.   Throws JampackException for
   inconsistent dimenstions.

   @param     B A Zmat
   @return    BQ
   @exception JampackException
              Thrown for inconsistent dimensions.
*/

   public Zmat bq(Zmat B)
   throws JampackException{


      if (B.nrow != ncol){
         throw new JampackException
            ("Inconsistent dimensions.");
      }


      return(H.o(qhb(H.o(B))));
}

/**
   Computes the product BQ<sup>H</sup>.   Throws JampackException for
   inconsistent dimenstions.

   @param     B A Zmat
   @return    BQ<sup>H</sup>
   @exception JampackException
              Thrown for inconsistent dimensions.
*/

   public Zmat bqh(Zmat A, Zmat B)
   throws JampackException{


      if (B.nrow != ncol){
         throw new JampackException
            ("Inconsistent dimensions.");
      }

      return(H.o(qb(H.o(B))));
   }

}
