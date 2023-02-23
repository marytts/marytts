package Jampack;

/**
   Block contains a static method for partitioning a matrix.
   into a block matrix.

   @version Pre-alpha
   @author G. W. Stewart
*/

public class Block{

/**
   This method takes a Zmat A and two arrays ii and jj of length m and
   n and produces an (m-1)x(n-1) block matrix Zmat[m-1][n-1], whose
   (i,j)-element is A.get(ii[i], ii[i+1]-1, jj[j], jj[j+1]-1).
   Throws a JampackException if

   @param     A    The matrix to be partitioned
   @param     ii[] The row indices of the partition
   @param     jj[] The column indices of the partition
   @return    The block Zmat
   @exception JampackException
                Thrown if the submatrices are not conformable.
*/
   public static Zmat[][] o(Zmat A, int ii[], int jj[])
   throws JampackException
   {

      int i, j;

      A.getProperties();

      int m = ii.length;
      int n = jj.length;

      /* Check the row indices */

      if (ii[0] < A.bx || ii[m-1]>A.rx+1){
         throw new JampackException
            ("Illegal row array.");
         }
      for (i=1; i<m; i++){
         if (ii[i-1]>=ii[i]){
            throw new JampackException
               ("Illegal row array.");
         }
      }

      /* Check the column indices */

      if (jj[0] < A.bx || jj[n-1]>A.cx+1){
         throw new JampackException
            ("Illegal column array.");
         }
      for (j=1; j<n; j++){
         if (jj[j-1]>=jj[j]){
            throw new JampackException
               ("Illegal column array.");
         }
      }

      /* Create and fill the block matrix with the parition
         of A. */

      Zmat B[][] = new Zmat[m-1][n-1];
      
      for (i=0; i<m-1; i++){
         for (j=0; j<n-1; j++){
            B[i][j] = A.get(ii[i], ii[i+1]-1, jj[j], jj[j+1]-1);
         }
      }
      return B;
   }
}
