package Jampack;

/**
   Zspec implements the spectral (eigenvalue-eigenvector) decomposition
   of a Hermitian matrix.  Specifically, given a Hermitian matrix
   A there is a unitary matrix A and a real diagonal matrix D
   such that

<pre>
*      D = U<sup>H</sup>AU.
</pre>

   Zspec implements U as a Zmat and D as a Zdiagmat.  It returns
   a JampackException if A is not Hermitian.

<p>
   Comments: The decomposition is computed using <a
   href="Jampack.Schur.html">.  Schur. </a> Eventually, there will be
   code that takes advantage of symmetry.

<br>
   Since the diagonal matrix is real, it will be reimplemented as a
   Ddiagmat later.

   @version Pre-alpha
   @author G. W. Stewart
*/

public class Zspec{

/** The matrix of eigenvectors */
    public Zmat U;

/** The matrix of eigenvalues */
    public Zdiagmat  D;

/**
   Creates a Zspec from Zmat.  Throws a JampackException if the
   matrix is not Hermitian.

   @param     AA A Zmat
   @return   The spectral decomposition of A
   @exception JampackException
              Thown if AA is not Hermitian.<br>
              Passed from below.
*/

   public Zspec(Zmat AA)
   throws JampackException{

      int i, j;

      if (AA.nrow != AA.ncol){
         throw new RuntimeException
            ("Matrix not square.");
      }

      Zmat A = new Zmat(AA);

      /* Check for A Hermitian. */

      for (i=0; i<A.nrow; i++){
         if (A.im[i][i] != 0){
            throw new JampackException("Matrix not Hermitian");
         }
         for (j=0; j<i; j++){
            if (A.re[i][j]!=A.re[j][i] || A.im[i][j]!=-A.im[j][i]){
               throw new JampackException("Matrix not Hermitian");
            }
         }
      }


   Schur S = new Schur(A);

   D = new Zdiagmat(S.T);
   for (i=0; i<D.n; i++){
      D.im[i] = 0.;
   }
   U = S.U;
   }
}


