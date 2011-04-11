package Jampack;

/**

   Zpsdmat is a tag class of Zmat, which tells Jampack to expect a
   (Hermitian) positive semidefinite matrix.  The user is entirely
   responsible for the matrix having the proper form, and Jampack
   programs do no checking.  For the constructors, see the
   corresponding constructors for <a href="Zmat.html"> Zmat </a>.

   @version Pre-alpha
   @author G. W. Stewart
*/

public class Zpsdmat extends Zmat{


   public Zpsdmat(double re[][], double im[][])
   throws JampackException{
      super(re, im);
   }

   public Zpsdmat(Z A[][]){
      super(A);
   }

   public Zpsdmat(double A[][]){
      super(A);
   }

   public Zpsdmat(Zmat A){
      super(A);
   }

   public Zpsdmat(int nrow, int ncol){
      super(nrow, ncol);
   }
}
