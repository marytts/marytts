package Jampack;

/**
   Inv computes the inverse of a matrix.
   <p>
   Comments:  Inv computes the inverse of A by using Solve to solve
   the system AX = I.  This is inefficient, though
   not inordinately so.  Eventually these methods will be
   replaced.

   @version Pre-alphs
   @author G. W. Stewart
*/

public class Inv{

/**
   Computes the inverse of a Zltmat.
   @param L   The Zltmat
   @return    The inverse of L
   @exception JampackException
                 Thrown if L is not square.<br>
                 Passed from below.
*/

   public static Zltmat o(Zltmat L)
   throws JampackException{

      if (L.nrow != L.ncol)
         throw new JampackException
             ("Cannot compute the inverse of a rectangular matrix.");
      return new Zltmat(Solve.aib(L, Eye.o(L.nrow)));
   }

/**
   Computes the inverse of a Zutmat.
   @param U   The Zutmat
   @return    The inverse of U
   @exception JampackException
                 Thrown if U is not square.<br>
                 Passed from below.
*/

   public static Zutmat o(Zutmat U)
   throws JampackException{

      if (U.nrow != U.ncol)
         throw new JampackException
             ("Cannot compute the inverse of a rectangular matrix.");

      return new Zutmat(Solve.aib(U, Eye.o(U.nrow)));
   }

/**
   Computes the inverse of a square Zmat
   @param A   The Zmat
   @return    The inverse of A
   @exception JampackException
                 Thrown if A is not square.<br>
                 Passed from below.
*/         

   public static Zmat o(Zmat A)
   throws JampackException{

      if (A.nrow != A.ncol)
         throw new JampackException
             ("Cannot compute the inverse of a rectangular matrix.");

      return Solve.aib(A, Eye.o(A.nrow));
   }

/**
   Computes the inverse of a Zpsdmat.
   @param A   The Zpsdmat
   @return    The inverse of A
   @exception JampackException
                 Thrown if A is not square.<br>
                 Passed from below.                 
*/

   public static Zpsdmat o(Zpsdmat A)
   throws JampackException{

      if (A.nrow != A.ncol)
         throw new JampackException
             ("Cannot compute the inverse of a rectangular matrix.");

      Zpsdmat B = new Zpsdmat( Solve.aib(A, Eye.o(A.nrow)));

      for (int i=0; i<B.ncol; i++){
         for (int j=i+1; j<B.ncol; j++){
            B.re[j][i] = B.re[i][j];
            B.im[j][i] = -B.im[i][j];
         }
         B.im[i][i] = 0.0;
      }
      return B;
   }

/**
   Computes the inverse of a Zdiagmat.
   @param D The Zdiagmat
   @return  The inverse of D
   @exception JampackException
                Thrown if D singular.
*/

   public static Zdiagmat o(Zdiagmat D)
   throws JampackException{

      Zdiagmat Di = new Zdiagmat(D.n);
      for (int i=0; i<D.order; i++){
         Z d = new Z(D.re[i], D.im[i]);
         if(d.re==0 && d.im==0){
            throw new JampackException
               ("Singuar matrix.");
         }
         d.Div(Z.ONE, d);
         Di.re[i] = d.re;
         Di.im[i] = d.im;
      }   

      return Di;
   }
}
