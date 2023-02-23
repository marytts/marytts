package Jampack;

/**
   Zludpp implements the LU decomposition with partial
   pivoting.  Specifically, given a matrix A, there
   is a permunation matrix P, a unit lower triangular matrix
   L whose subdiagonal elements are less than one in magnitude
   and a upper triangular matrix U such that
<pre>
*     A = PLU
</pre>
   Zludpp represents P as a pivot array (see <a href="Jampack.Pivot.html">
   Pivot.java </a>), L as a Zltmat, and U as a Zutmat.

   @version Pre-alpha
   @author G. W. Stewart
*/

public class Zludpp{


/** The number of rows in L */
   public int nrl;
/** The number of columns in L */
   public int ncl;
/** The number of rows in U */
   int nru;
/** The number of columns in U */
   int ncu;
/** The pivot array (see <a href="Jampack.Pivot.html"> Pivot.java </a>) */
   public int pvt[];
/** The lower triangular matrix L */
   public Zltmat L;
/** The upper triangular matrix U */
   public Zutmat U;

/**
   Computes the partially pivoted LU decompostion.

   @param     A A Zmat
   @return    The Zludpp of A
   @exception JampackException
              Passed from below.              
*/

   public Zludpp(Zmat A)
   throws JampackException{
      int i, j, k, nr, nc;
      double absi, mx, t;
      Zmat T;
      Z Tk[];

      A.getProperties();

      /* Set up L and U */

      nr = A.nr;
      nrl = nr;
      nc = A.nc;
      ncl = Math.min(A.nr, A.nc);
      nru = ncl;
      ncu = nc;


      L = new Zltmat(nrl, ncl);
      U = new Zutmat(nru, ncu);
      pvt = new int[ncl];


      /* Set up the matrix T in which the elimination will be
         performed and copy A to it.*/

      if (nrl>= ncu)
         T = L;
      else
         T = U;

      for (i=0; i<nr; i++){
         for (j=0; j<nc; j++){
            T.re[i][j] = A.re[i][j];
            T.im[i][j] = A.im[i][j];
         }
      }

      /* Outer elimination loop. */

      Tk = new Z[nrl];   // This should be repaced by a Z1.

      for (k=0; k< Math.min(nr,nc); k++){

         /* Find the pivot row. */

         mx = 0.;
         pvt[k] = k;
         for (i=k; i<nr; i++){
            Tk[i] = T.get0(i,k);
            if ((absi = Z.abs(Tk[i])) > mx){
               pvt[k] = i;
               mx = absi;
            }
         }
         if (mx == 0.0) continue;

         /* Perform the exchange. */

         Tk[k].Exch(Tk[pvt[k]]);
         for (j=0; j<nc; j++){
            t=T.re[k][j]; T.re[k][j]=T.re[pvt[k]][j]; T.re[pvt[k]][j]=t;
            t=T.im[k][j]; T.im[k][j]=T.im[pvt[k]][j]; T.im[pvt[k]][j]=t;
         }

         /* Compute multipliers and eliminate. */

         for (i=k+1; i<nr; i++){
            T.put0(i, k, Tk[i].Div(Tk[i],Tk[k]));
            for (j=k+1; j<nc; j++){
               T.re[i][j] = T.re[i][j]
                            - T.re[i][k]*T.re[k][j] + T.im[i][k]*T.im[k][j];
               T.im[i][j] = T.im[i][j]
                            - T.im[i][k]*T.re[k][j] - T.re[i][k]*T.im[k][j];
            }
         }
      }

      /* Finalize L and U */

      if (nr >= nc) // Copy U from T.
         for (i=0; i<nc; i++){
            for (j=0; j<nc; j++)
               if (i > j){
                  U.re[i][j] = 0.0; U.im[i][j] = 0.0;
               }
               else{
                  U.re[i][j] = T.re[i][j]; U.im[i][j] = T.im[i][j];
                  L.re[i][j] = 0.0; L.im[i][j] = 0.0;
               }
            L.re[i][i] = 1.0; L.im[i][i] = 0.0;
         }
      else // Copy L from T.
         for (i=0; i<nr; i++){
            for (j=0; j<nr; j++)
               if (i > j){
                  L.re[i][j] = T.re[i][j];  L.im[i][j] = T.im[i][j];
                  U.re[i][j] = 0.0; U.im[i][j] = 0.0;
               }
               else{
                  L.re[i][j] = 0.0; L.im[i][j] = 0.0;
               }
            L.re[i][i] = 1.0;    L.im[i][i] = 0.0;
         }
   }
}
