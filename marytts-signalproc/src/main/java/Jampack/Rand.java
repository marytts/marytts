package Jampack;

import java.util.Random;
/**
   The rand suite generates random objects with elements
   distributed randomly on [0,1] or normally with mean zero
   and standard deviation one.

   @version Pre-alpha
   @author G. W. Stewart
*/

public class Rand{

   private static Random R = new Random();

/**
   Sets the seed for the random number generator.

   @param     seed  The seed
*/
   public static void setSeed(long seed){
      R.setSeed(seed);
   }

/**
   Generates a random uniform double.

   @return   a uniform random double
*/
   public static double ud(){
      return R.nextDouble();
   }

/**
   Generates a one-dimensional array of
   uniform random doubles.

   @param    n  The length of the array.
   @return   The array of uniform doubles.
*/
   public static double[] udary(int n){
      double d[] = new double[n];
      for (int i=0; i<n; i++){
         d[i] = R.nextDouble();
      }
      return d;
   }

/**
   Generates a two-dimensional array of
   uniform random doubles.

   @param    m  The number of rows in the array.
   @param    n  The number of columns in the array.
   @return   The array of uniform doubles.
*/
   public static double[][] udary(int m, int n){
      double d[][] = new double[m][n];
      for (int i=0; i<m; i++){
         for (int j=0; j<n; j++){
            d[i][j] = R.nextDouble();
         }
      }
      return d;
   }

/**
   Generates a uniform random complex number, i.e., a complex
   number whose real and imaginary parts are random.

   @return  The uniform random Z
*/
   public static Z uz(){
      return new Z(R.nextDouble(), R.nextDouble());
   }

/**
   Generates a uniform random Z1.
   @param     n  The length of the Z1
   @return    The uniform random Z1
   @exception JampackException
              Passed from below.
*/
   public static Z1 uz1(int n)
   throws JampackException{

      Z1 zone = new Z1(n);
      for (int i=0; i<n; i++){
         zone.re[i] = R.nextDouble();
         zone.im[i] = R.nextDouble();
      }
      return zone;
   }

/**
   Generates  a uniform random  Zmat.
   @param     m   The number of rows in the Zmat
   @param     n   The number of columns in the Zmat
   @return    The uniform random Zmat
   @exception JampackException
              Passed from below.

*/
   public static Zmat uzmat(int m, int n)
   throws JampackException{
      Zmat zm = new Zmat(m, n);
      for (int i=0; i<m; i++){
         for (int j=0; j<n; j++){
            zm.re[i][j] = R.nextDouble();
            zm.im[i][j] = R.nextDouble();
         }
      }
      return zm;
   }

/**
   Generates a normal random double.

   @return   a normal random double
*/
   public static double nd(){
      return R.nextGaussian();
   }

/**
   Generates a one-dimensional array of
   normal random doubles.

   @param    n  The length of the array.
   @return   The array of normal doubles.
*/
   public static double[] ndary(int n){
      double d[] = new double[n];
      for (int i=0; i<n; i++){
         d[i] = R.nextGaussian();
      }
      return d;
   }

/**
   Generates a two-dimensional array of
   normal random doubles.

   @param    m  The number of rows in the array.
   @param    n  The number of columns in the array.
   @return   The array of normal doubles.
*/
   public static double[][] ndary(int m, int n){
      double d[][] = new double[m][n];
      for (int i=0; i<m; i++){
         for (int j=0; j<n; j++){
            d[i][j] = R.nextGaussian();
         }
      }
      return d;
   }

/**
   Generates a normal random complex number, i.e., a complex
   number whose real and imaginary parts are random.

   @return  The normal random Z
*/
   public static Z nz(){
      return new Z(R.nextGaussian(), R.nextGaussian());
   }

/**
   Generates  a normal random Z1.
   @param     n  The length of the Z1
   @return    The normal random Z1
   @exception JampackException
              Passed from below.

*/
   public static Z1 nz1(int n)
   throws JampackException{

      Z1 zone = new Z1(n);
      for (int i=0; i<n; i++){
         zone.re[i] = R.nextGaussian();
         zone.im[i] = R.nextGaussian();
      }
      return zone;
   }

/**
   Generates  a normal random  Zmat.
   @param     m   The number of rows in the Zmat
   @param     n   The number of columns in the Zmat
   @return    The normal random Zmat
   @exception JampackException
              Passed from below.

*/
   public static Zmat nzmat(int m, int n)
   throws JampackException{
      Zmat zm = new Zmat(m, n);
      for (int i=0; i<m; i++){
         for (int j=0; j<n; j++){
            zm.re[i][j] = R.nextGaussian();
            zm.im[i][j] = R.nextGaussian();
         }
      }
      return zm;
   }
}


