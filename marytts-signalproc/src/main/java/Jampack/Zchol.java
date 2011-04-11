package Jampack;

/**
   Zchol implements the Cholesky decomposition of a positive definite
   matrix.  Specifically if A is (Hermitian) positive definite then
   there is an upper triangular matrix R with positive diagonal
   elements such that
<pre>
*     A = R^H R
</pre>
   The matrix R is implemented as a Zutmat.

   @version Pre-alpha
   @author G. W. Stewart
*/

public class Zchol{

/** The order of A and R */
   public int n;

/** The Cholesky factor */
   public Zutmat R;

/**
    Constructs a Zchol from a Zmat A.  The matrix that
    is actually decomposed is taken from the upper triangle
    of $A$ and the imaginary part of its diagonal is set to
    zero.  Throws a JampackException for inconsistent dimensions
    on failure of the algorithm to complete.

    @param     A The matrix whose Cholesky decomposition is
                 to be computed.
    @return    The Cholesky decomposition of A
    @exception JampackException
               Thrown if A is not square or Hermitian.<br>
               Thrown if the doecomposition does not exist.
*/

   public Zchol(Zmat A)
   throws JampackException{

      double mu;
      int i, j, k;
      A.getProperties();

      if (A.nr != A.nc){
         throw new JampackException
            ("Matrix not square.");
      }

      n = A.nr;

      /* Set up R from the upper triangle of A */

      R = new Zutmat(A);

      /* Check for A Hermitian and initialize R. */

      for (i=0; i<n; i++){
         if (R.im[i][i] != 0){
            throw new JampackException("Matrix not Hermitian");
         }
         for (j=0; j<i; j++){
            if (R.re[i][j]!=R.re[j][i] || R.im[i][j]!=-R.im[j][i]){
               throw new JampackException("Matrix not Hermitian");
            }
            R.im[i][j] = 0;
            R.re[i][j] = 0;
         }
      }

      /* Compute the decomposition */

      for (k=0; k<n; k++){
         if (R.re[k][k] <= 0){
            throw new JampackException
               ("Nonpositive diagonal entry during reduction.");
         }
         R.re[k][k] = Math.sqrt(R.re[k][k]);
         mu = 1/R.re[k][k];
         for (j=k+1; j<n; j++){
            R.re[k][j] = mu*R.re[k][j];
            R.im[k][j] = mu*R.im[k][j];
         }
         for (i=k+1; i<n; i++){
            for (j=i; j<n; j++){
               R.re[i][j] = R.re[i][j] -
                            R.re[k][i]*R.re[k][j] - R.im[k][i]*R.im[k][j];
               R.im[i][j] = R.im[i][j] -
                            R.re[k][i]*R.im[k][j] + R.im[k][i]*R.re[k][j];
            }
            R.im[i][i] = 0;
         }
      }
   }
}
