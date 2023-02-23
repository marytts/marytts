package Jampack;

/**
   Solve solves linear systems of the form
<pre>
*      A*X = B
*      A<sup>H</sup>*X = B
*      X*A = B
*      X*A<sup>H</sup> = B
</pre>
   where A is a nonsingular Zmat, B is a Zmat, and
   '^H' denotes the conjugate transpose.  Appropriate
   action is taken for Zmats that are Zltmats, Zutmats,
   and Zpsdmats.  If a decomposition is computed and the
   <a href="Parameters.html"> History </a> parameter is set,
   then the decomposition is saved for reuse.
   <p>
   Comments: For triangular matrices only the systems AX=B
   and A^HX=B are solved by hard code, the other two being
   solved by wizardry involving transposed systems.  This
   requires the generation of new Zmats of the same size
   as B, which is inefficient if B is, say, square.  Later
   these methods will be implemented with hard code.

   @version Pre-alpha
   @author G. W. Stewart
*/

public class Solve{

/**
   Solves LX = B, where L is a Zltmat and B is a Zmat.

   @param     L  The matrix of the sysem
   @param     B  The right-hand side
   @return    L<sup>-1</sup>B
   @exception JampackException
              Thrown for nonsquare matrix or nonconformity.<br>
              Thrown for singular L.
*/

   public static Zmat aib(Zltmat L, Zmat B)
   throws JampackException{
      int i, j, k;
      Z x = new Z();
      L.getProperties();
      B.getProperties();

      if (L.nr != L.nc)
         throw new JampackException
            ("Rectangular matrix.");
      if (L.nr != B.nr)
         throw new JampackException
            ("Inconsistent dimensions.");
      Zmat X = new Zmat(B);

      for (i=0; i<L.nr; i++)
         for (j=0; j<B.nc; j++){
            for (k=0; k<i; k++){
               X.re[i][j] = X.re[i][j] - 
                            L.re[i][k]*X.re[k][j] + L.im[i][k]*X.im[k][j];
               X.im[i][j] = X.im[i][j] - 
                            L.im[i][k]*X.re[k][j] - L.re[i][k]*X.im[k][j];
            }
            if (L.re[i][i] == 0.0 && L.im[i][i] == 0.0)
                throw new JampackException
                  ("Zero diagonal in solving triangular system");
            X.put0(i, j, x.Div(X.get0(i,j), L.get0(i,i)));
         }
      return X;
   }

/**
   Solves L<sup>H</sup>X = B, where L is a Zltmat and B is a Zmat.

   @param     L  The matrix of the sysem
   @param     B  The right-hand side
   @return    L<sup>-H</sup>B
   @exception JampackException
              Thrown for nonsquare matrix or nonconformity.<br>
              Thrown for singular L.
*/

   public static Zmat ahib(Zltmat L, Zmat B)
   throws JampackException{
      int i, j, k;
      Z x = new Z();
      L.getProperties();
      B.getProperties();

      if (L.nr != L.nc)
         throw new JampackException
            ("Rectangular matrix.");
      if (L.nr != B.nr)
         throw new JampackException
            ("Inconsistent dimensions.");
      Zmat X = new Zmat(B);

      for (i=L.nr-1; i>=0; i--){
         if (L.re[i][i] == 0.0 && L.im[i][i] == 0.0)
            throw new JampackException
               ("Zero diagonal in solving triangular system");
         for (j=0; j<B.nc; j++){
            X.put0(i, j, x.Div(X.get0(i,j), x.Conj(L.get0(i,i))));
            for (k=0; k<i; k++){
               X.re[k][j] = X.re[k][j] -
                            X.re[i][j]*L.re[i][k] - X.im[i][j]*L.im[i][k];
               X.im[k][j] = X.im[k][j] +
                            X.re[i][j]*L.im[i][k] - X.im[i][j]*L.re[i][k];
            }
         }
      }
      return X;
   }

/**
   Solves XL = B, where L is a Zltmat and B is a Zmat.

   @param     B  The right-hand side
   @param     L  The matrix of the system
   @return    BL<sup>-1</sup>
   @exception JampackException
              Thrown for nonsquare matrix or nonconformity.<br>
              Passed from below.
*/

   public static Zmat bai(Zmat B, Zltmat L)
   throws JampackException{
      int i, j, k;
      Z x = new Z();
      L.getProperties();
      B.getProperties();

      if (L.nr != L.nc)
         throw new JampackException
            ("Rectangular matrix.");
      if (L.nr != B.nc)
         throw new JampackException
            ("Inconsistent dimensions.");
      return H.o(Solve.ahib(L, H.o(B)));
   }


/**
   Solves XL<sup>H</sup> = B, where L is a Zltmat and B is a Zmat.

   @param     B  The right-hand side
   @param     L  The matrix of the system
   @return    BL<sup>-H</sup>
   @exception JampackException
              Thrown for nonsquare matrix or nonconformity.<br>
              Passed from below.
*/

   public static Zmat bahi(Zmat B, Zltmat L)
   throws JampackException{
      int i, j, k;
      Z x = new Z();
      L.getProperties();
      B.getProperties();

      if (L.nr != L.nc)
         throw new JampackException
            ("Rectangular matrix.");
      if (L.nc != B.nc)
         throw new JampackException
            ("Inconsistent dimensions.");
      return H.o(Solve.aib(L, H.o(B)));
   }


/**
   Solves UX = B, where U is a Zutmat and B is a Zmat.

   @param     U  The matrix of the system
   @param     B  The right-hand side
   @return    U<sup>-1</sup>B
   @exception JampackException
              Thrown for nonsquare matrix or nonconformity.<br>
              Thrown for singular U.
*/

   public static Zmat aib(Zutmat U, Zmat B)
   throws JampackException{
      int i, j, k;
      Z x = new Z();
      U.getProperties();
      B.getProperties();


      if (U.nr != U.nc)
         throw new JampackException
            ("Rectangular matrix.");
      if (U.nr != B.nr)
         throw new JampackException
            ("Inconsistent dimensions.");
      Zmat X = new Zmat(B);

      for (i=U.nr-1; i>=0; i--)
         for (j=0; j<B.nc; j++){
            for (k=i+1; k<U.nc; k++){
               X.re[i][j] = X.re[i][j] - 
                            U.re[i][k]*X.re[k][j] + U.im[i][k]*X.im[k][j];
               X.im[i][j] = X.im[i][j] - 
                            U.im[i][k]*X.re[k][j] - U.re[i][k]*X.im[k][j];
            }
            if (U.re[i][i] == 0.0 && U.im[i][i] == 0.0)
                throw new JampackException
                   ("Zero diagonal in solving riangular system");
            X.put0(i, j, x.Div(X.get0(i,j), U.get0(i,i)));
         }
      return X;
   }

/**
   Solves U<sup>H</sup>X = B, where U is a Zutmat and B is a Zmat.

   @param     U  The matrix of the system
   @param     B  The right-hand side
   @return    U<sup>-H</sup>B
   @exception JampackException
              Thrown for nonsquare matrix or nonconformity.<br>
              Thrown for singular U.
*/

   public static Zmat ahib(Zutmat U, Zmat B)
   throws JampackException{
      int i, j, k;
      Z x = new Z();
      U.getProperties();
      B.getProperties();

      if (U.nr != U.nc)
         throw new JampackException
            ("Rectangular matrix.");
      if (U.nr != B.nr)
         throw new JampackException
            ("Inconsistent dimensions.");
      Zmat X = new Zmat(B);

      for (i=0; i<U.nr; i++){
         if (U.re[i][i] == 0.0 && U.im[i][i] == 0)
            throw new JampackException
               ("Zero diagonal in solving lower triangular system");
         for (j=0; j<B.nc; j++){
            X.put0(i,j, x.Div(X.get0(i,j), x.Conj(U.get0(i,i))));
            for (k=i+1; k<U.nr; k++){
               X.re[k][j] = X.re[k][j] -
                            X.re[i][j]*U.re[i][k] - X.im[i][j]*U.im[i][k];
               X.im[k][j] = X.im[k][j] -
                            X.im[i][j]*U.re[i][k] + X.re[i][j]*U.im[i][k];
                    }
         }
      }

      return X;
   }

/**
   Solves XU = B, where U is a Zutmat and B is a Zmat.

   @param     B  The right-hand side
   @param     U  The matrix of the system
   @return    BU<sup>-1</sup>
   @exception JampackException
              Thrown for nonsquare matrix or nonconformity.<br>
              Passed from below.
*/

   public static Zmat bai(Zmat B, Zutmat U)
   throws JampackException{
      int i, j, k;
      Z x = new Z();
      U.getProperties();
      B.getProperties();

      if (U.nr != U.nc)
         throw new JampackException
            ("Rectangular matrix.");
      if (U.nr != B.nc)
         throw new JampackException
            ("Inconsistent dimensions.");
      return H.o(Solve.ahib(U, H.o(B)));
   }

/**
   Solves XU<sup>H</sup> = B, where U is a Zutmat and B is a Zmat.

   @param     B  The right-hand side
   @param     U  The matrix of the system
   @return    BU<sup>-H</sup>
   @exception JampackException
              Thrown for nonsquare matrix or nonconformity.<br>
              Passed from below.
*/

   public static Zmat bahi(Zmat B, Zutmat U)
   throws JampackException{
      int i, j, k;
      Z x = new Z();
      U.getProperties();
      B.getProperties();

      if (U.nr != U.nc)
         throw new JampackException
            ("Rectangular matrix.");
      if (U.nc != B.nc)
         throw new JampackException
            ("Inconsistent dimensions.");
      return H.o(Solve.aib(U, H.o(B)));
   }


/**
   Solves AX = B, where A is a Zmat and B is a Zmat.

   @param     A  The matrix of the sysem
   @param     B  The right-hand side
   @return    A<sup>-1</sup>B
   @exception JampackException
              Thrown for nonsquare matrix or nonconformity.<br>
              Passed from below.
*/

   public static Zmat aib(Zmat A, Zmat B)
   throws JampackException{
      Zludpp LU;
      A.getProperties();
      B.getProperties();
      
      if (A.nr != A.nc)
         throw new JampackException
            ("Rectangular matrix.");
      if (A.nr != B.nr)
         throw new JampackException
            ("Inconsistent dimensions.");

      if (Parameters.History){
         A.clean();
         if (A.LU == null)
            A.LU = new Zludpp(A);
         LU = A.LU;
      }
      else
         LU = new Zludpp(A);

      Zmat X = new Zmat(B);
      Pivot.row(X, LU.pvt);
      return Solve.aib(LU.U, Solve.aib(LU.L, X));
   }
      
/**
   Solve A<sup>H</sup>X = B, where A is a Zmat and B is a Zmat.

   @param     A  The matrix of the sysem
   @param     B  The right-hand side
   @return    A<sup>-H</sup>B
   @exception JampackException
              Thrown for nonsquare matrix or nonconformity.<br>
              Passed from below.
*/

   public static Zmat ahib(Zmat A, Zmat B)
   throws JampackException{
      Zludpp LU;
      A.getProperties();
      B.getProperties();
      
      
      if (A.nr != A.nc)
         throw new JampackException
            ("Rectangular matrix.");
      if (A.nr != B.nr)
         throw new JampackException
            ("Inconsistent dimensions.");

      if (Parameters.History){
         A.clean();
         if (A.LU == null)
            A.LU = new Zludpp(A);
         LU = A.LU;
      }
      else
         LU = new Zludpp(A);

      return Pivot.rowi(Solve.ahib(LU.L, Solve.ahib(LU.U, B)), LU.pvt);
   }

/**
   Solve XA = B, where A is a Zmat and B is a Zmat.

   @param     B  The right-hand side
   @param     A  The matrix of the sysem
   @return    BA<sup>-1</sup>
   @exception JampackException
              Thrown for nonsquare matrix or nonconformity.<br>
              Passed from below.
*/

   public static Zmat bai(Zmat B, Zmat A)
   throws JampackException{
      Zludpp LU;
      A.getProperties();
      B.getProperties();
      
      
      if (A.nr != A.nc)
         throw new JampackException
            ("Rectangular matrix.");
      if (A.nr != B.nc)
         throw new JampackException
            ("Inconsistent dimensions.");

      if (Parameters.History){
         A.clean();
         if (A.LU == null)
            A.LU = new Zludpp(A);
         LU = A.LU;
      }
      else
         LU = new Zludpp(A);

      return H.o(Solve.ahib(A, H.o(B)));
   }

/**
   Solve XA^H = B, where A is a Zmat and B is a Zmat.

   @param     B  The right-hand side
   @param     A  The matrix of the sysem
   @return    BA<sup>-H</sup>
   @exception JampackException
              Thrown for nonsquare matrix or nonconformity.<br>
              Passed from below.
*/

   public static Zmat bahi(Zmat B, Zmat A)
   throws JampackException{
      Zludpp LU;
      A.getProperties();
      B.getProperties();
      
      if (A.nr != A.nc)
         throw new JampackException
            ("Rectangular matrix.");
      if (A.nr != B.nc)
         throw new JampackException
            ("Inconsistent dimensions.");

      if (Parameters.History){
         A.clean();
         if (A.LU == null)
            A.LU = new Zludpp(A);
         LU = A.LU;
      }
      else
         LU = new Zludpp(A);

      return H.o(Solve.aib(A, H.o(B)));
   }
/**
   Solves AX = B, where A is a Zpsdmat and B is a Zmat.

   @param     A  The matrix of the sysem
   @param     B  The right-hand side
   @return    A<sup>-1</sup>B
   @exception JampackException
              Thrown for nonsquare matrix or nonconformity.<br>
              Passed from below.
*/

   public static Zmat aib(Zpsdmat A, Zmat B)
   throws JampackException{

      Zchol CHOL;
      A.getProperties();
      B.getProperties();
      
      if (A.nr != A.nc)
         throw new JampackException
            ("Rectangular matrix.");
      if (A.nr != B.nr)
         throw new JampackException
            ("Inconsistent dimensions.");

      if (Parameters.History){
         A.clean();
         if (A.CHOL == null)
            A.CHOL = new Zchol(A);
         CHOL = A.CHOL;
      }
      else
         CHOL = new Zchol(A);

      return Solve.aib(CHOL.R, Solve.ahib(CHOL.R, B));
   }
      
/**
   Solves XA = B, where A is a Zpsdmat and B is a Zmat.
   @param     B  The right-hand side
   @param     A  The matrix of the sysem
   @return    BA<sup>-1</sup>
   @exception JampackException
              Thrown for nonsquare matrix or nonconformity.<br>
              Passed from below.
*/

   public static Zmat bai(Zmat B, Zpsdmat A)
   throws JampackException{
      Zchol CHOL;
      A.getProperties();
      B.getProperties();
      
      if (A.nr != A.nc)
         throw new JampackException
            ("Rectangular matrix.");
      if (A.nr != B.nc)
         throw new JampackException
            ("Inconsistent dimensions.");

      if (Parameters.History){
         A.clean();
         if (A.CHOL == null)
            A.CHOL = new Zchol(A);
         CHOL = A.CHOL;
      }
      else
         CHOL = new Zchol(A);

      return Solve.bahi(Solve.bai(B, CHOL.R), CHOL.R);
   }
      
}
