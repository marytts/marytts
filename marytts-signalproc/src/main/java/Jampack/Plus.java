package Jampack;

/**
   Plus Computes the sum of two matrices.

   @version Pre-alpha
   @author G. W. Stewart
*/

public class Plus{

/**
   Computes the sum of two Zmats
   @param     A  The first Zmat
   @param     B  The second Zmat
   @return    A + B
   @exception JampackException
              Thrown for nonconformity.
*/
   public static Zmat o(Zmat A, Zmat B)
   throws JampackException{
      if (A.nrow!=B.nrow || A.ncol != B.ncol){
         throw new JampackException("Matrices not conformable for addition");
      }
      Zmat C = new Zmat(A.nr, A.nc);

      for (int i=0; i<A.nrow; i++)
         for (int j=0; j<A.ncol; j++){
            C.re[i][j] = A.re[i][j] + B.re[i][j];
            C.im[i][j] = A.im[i][j] + B.im[i][j];
         }
      return C;
   }

/**
   Computes the sum of a Zmat and a Zdiagmat.
   @param     A The Zmat
   @param     D The Zdiagmat
   @return    A + D
   @exception JampackException
              Thrown for nonconformity.
*/

   public static Zmat o(Zmat A, Zdiagmat D)
   throws JampackException{

      if (D.order != A.nrow || D.order != A.ncol){
         throw new JampackException("Matrices not conformable for addition");
      }
      Zmat C = new Zmat(A);
      for (int i=0; i<A.nrow; i++){
         C.re[i][i] = C.re[i][i] + D.re[i];
         C.im[i][i] = C.im[i][i] + D.im[i];
      }
      return C;
   }

/**
   Computes the sum of a Zdiagmat and a Zmat.
   @param     D  The Zdiagmat
   @param     A  The Zmat
   @return    D + A
   @exception JampackException
              Thrown for nonconformity.
*/

   public static Zmat o(Zdiagmat D, Zmat A)
   throws JampackException{

      if (D.order != A.nrow || D.order != A.ncol){
         throw new JampackException("Matrices not conformable for addition");
      }
      Zmat C = new Zmat(A);
      for (int i=0; i<D.order; i++){
         C.re[i][i] = C.re[i][i] + D.re[i];
         C.im[i][i] = C.im[i][i] + D.im[i];
      }
      return C;
   }

/**
   Computes the sum of a Zdiagmat and a Zdiagmat.
   @param     D1  The first Zdiagmat
   @param     D2  The second Zdiagmat
   @return    D1 + D2
   @exception JampackException
              Thrown for nonconformity.
*/

   public static Zdiagmat o(Zdiagmat D1, Zdiagmat D2)
   throws JampackException{

      if (D1.order != D2.order){
         throw new JampackException("Matrices not conformable for addition");
      }
      Zdiagmat C = new Zdiagmat(D1);
      for (int i=0; i<D1.order; i++){
         C.re[i] = C.re[i] + D2.re[i];
         C.im[i] = C.im[i] + D2.im[i];
      }
      return C;
   }
}
