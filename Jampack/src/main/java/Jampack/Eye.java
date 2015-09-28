package Jampack;

/**
   Eye generates a matrix whose diagonal elements are one
   and whose off diagonal elements are zero.
*/

public class Eye{
   
/**
   Generates an identity matrix of order <tt>n</tt>.
   @param <tt>n</tt>  The order of the matrx
*/
   public static Zmat o(int n){
      return o(n, n);
   }

/**
   Generates an <tt>mxn</tt> matrix whose diagonal elements are
   one and whose off diagonal elements are zero.
   @param <tt>m</tt>   The number of rows in the matrix
   @param <tt>n</tt>   The number of columns in the matrix
*/
   public static Zmat o(int m, int n){

      Zmat I = new Zmat(m, n);

      for (int i=0; i<Math.min(m, n); i++){
         I.re[i][i] = 1;
         I.im[i][i] = 0;
      }

      return I;
   }

}
