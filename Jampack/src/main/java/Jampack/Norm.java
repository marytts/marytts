package Jampack;

/**
   Norm computes norms of matrices.
   <p>
   Comments: At this point only the Frobenius norm is calculated.  Later the
   1, 2, and infinity norms will be added.

   @version Pre-alpha
   @author G. W. Stewart
*/

public class Norm{

/**
   Computes the Frobenius norm of a the submatrix (ii1:ii2, jj1,jj2)
   of a Zmat.
   @param A    The zmat
   @param ii1  The lower row index
   @param ii2  The upper row index
   @param jj1  The lower column index
   @param jj2  The upper column index
   @return     The Frobenius norm of A(ii1:ii2, jj1:jj2)
*/
   public static double fro(Zmat A, int ii1, int ii2, int jj1, int jj2)
   {
      int i, i1, i2, j, j1, j2;
      double fac, nrm, scale;

      i1 = ii1 - A.basex;
      i2 = ii2 - A.basex;
      j1 = jj1 - A.basex;
      j2 = jj2 - A.basex;

      scale = 0.0;
      for (i=i1; i<=i2; i++){
         for (j=j1; j<=j2; j++){
            scale = Math.max(scale, 
                             Math.abs(A.re[i][j])+Math.abs(A.im[i][j]));
         }
      }
      if (scale == 0){
         return 0.0;
      }
      if (scale < 1){
         scale = scale*1.0e20;
      }
      scale = 1/scale;
      nrm = 0;
      for (i=i1; i<=i2; i++){
         for (j=j1; j<=j2; j++){
             fac = scale*A.re[i][j];
             nrm = nrm + fac*fac;
             fac = scale*A.im[i][j];
             nrm = nrm + fac*fac;
         }
      }
      return Math.sqrt(nrm)/scale;
   }

/**
   Computes the Frobenius norm of a Zmat.
   @param A  The Zmat
   @return   The Frobenius norm of A
*/
   public static double fro(Zmat A)
   {
      A.getProperties();
      return Norm.fro(A, A.bx, A.rx, A.bx, A.cx);
   }

/**
   Computes the Frobenius norm of a Z1.
   @param u  The Z1
   @return   The Frobenius norm of u
*/
   public static double fro(Z1 u)
   {
      int i;
      double fac, nrm, scale;

      int n = u.n;

      scale = 0.0;
      for (i=0; i<n; i++){
         scale = Math.max(scale, 
             Math.abs(u.re[i]) + Math.abs(u.im[i]));
      }
      if (scale == 0){
         return 0.0;
      }
      if (scale < 1){
         scale = scale*1.0e20;
      }
      scale = 1/scale;
      nrm = 0;

      for (i=0; i<n; i++){
         fac = scale*u.re[i];
         nrm = nrm + fac*fac;
         fac = scale*u.im[i];
         nrm = nrm + fac*fac;
      }

      return Math.sqrt(nrm)/scale;
   }

/**
   Computes the Frobenius norm of a Zdiagmat.
   @param D  The Zdiagmat
   @regurn   The Frobenius norm of D
*/

   public static double fro(Zdiagmat D)
   {
      int i;
      double fac, nrm, scale;

      int n = D.order;

      scale = 0.0;
      for (i=0; i<n; i++){
         scale = Math.max(scale, 
             Math.abs(D.re[i]) + Math.abs(D.im[i]));
      }
      if (scale == 0){
         return 0.0;
      }
      if (scale < 1){
         scale = scale*1.0e20;
      }
      scale = 1/scale;
      nrm = 0;

      for (i=0; i<n; i++){
         fac = scale*D.re[i];
         nrm = nrm + fac*fac;
         fac = scale*D.im[i];
         nrm = nrm + fac*fac;
      }

      return Math.sqrt(nrm)/scale;
   }

}


