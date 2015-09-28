package Jampack;

/**

   Zutmat is a tag class of Zmat, which tells Jampack to expect an
   upper triangular matrix.  The user is entirely responsible for the
   matrix having the proper form, and Jampack programs do no checking.
   For the constructors, see the corresponding constructors for <a
   href="Zmat.html"> Zmat </a>.

   @version Pre-alpha
   @author G. W. Stewart

*/

public class Zutmat extends Zmat{


   public Zutmat(double re[][], double im[][])
   throws JampackException{
      super(re, im);
   }

   public Zutmat(Z A[][]){
      super(A);
   }

   public Zutmat(double A[][]){
      super(A);
   }

   public Zutmat(Zmat A){
      super(A);
   }

   public Zutmat(int nrow, int ncol){
      super(nrow, ncol);
   }
}
