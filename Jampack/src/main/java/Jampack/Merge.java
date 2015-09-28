package Jampack;

/**
   Merge is a class containing programs to merge matrices
   into one big matrix.  The basic method (Merge.o) takes
   an array of Zmat's and merges them.  For conformity, the Zmats
   along a row of the array must have the same number of rows,
   and the Zmats along a column of the array must have the same
   number of columns.
   <p>
   For convenience a number of special routines (o12, o21, o22, o13, ...)
   are provided to merge the matrices in their argument list.

   @version Pre-alpha
   @author G. W. Stewart
*/

public class Merge{

/**
   Merges     the matrices in an array of Zmats
   @param     B[][]  The array of Zmats
   @return    The merged Zmat
   @exception JampackException 
              Thrown if there is a nonconformity.
*/

   public static Zmat o(Zmat[][] B)
   throws JampackException{

      int bi, bi0nr, bj, b0jnc, bnc, bnr, i, il, j, jl, nc, nr;

      Zmat Bij;

      bnr = B.length;
      bnc = B[0].length;

/*    Compute the number of columns in the result while
      testing for columnwise conformity. */

      nc = 0;
      for (bj=0; bj<bnc; bj++){
         b0jnc = B[0][bj].ncol;
         for (bi=1; bi<bnr; bi++){
            if (B[bi][bj].ncol != b0jnc){
               throw new JampackException
                  ("Blocks do not conform");
            }
         }
         nc = nc + b0jnc;
      }

/*    Compute the number of rows in the result while
      testing for rowwise conformity. */

      nr = 0;
      for (bi=0; bi<bnr; bi++){
         bi0nr = B[bi][0].nrow;
         for (bj=1; bj<bnc; bj++){
            if (B[bi][bj].nrow != bi0nr){
              throw new JampackException
                 ("Blocks do not conform");
            }
         }
         nr = nr + bi0nr;
      }

/*    Merge the matrices. */

      Zmat A = new Zmat(nr, nc);

      il = 0;
      for (bi=0; bi<bnr; bi++){
         jl = 0;
         for (bj=0; bj<bnc; bj++){
            Bij = B[bi][bj];
            for (i=il; i<il+Bij.nrow; i++){
               for (j=jl; j<jl+Bij.ncol; j++){
                  A.re[i][j] = Bij.re[i-il][j-jl];
                  A.im[i][j] = Bij.im[i-il][j-jl];
               }
            }
            jl = jl + Bij.ncol;
         }
         il = il + B[bi][0].nrow;
      }

      return A;
   }

/**
   Merges its arguments to create the Zmat
<pre>
*    A = | B00 B01 |
</pre>
   @param     Bij The Zmats to be merged
   @return    The composite Zmat A
   @exception JampackException
              Thrown if there is a nonconformity.
*/

   public static Zmat o12(Zmat B00, Zmat B01)
   throws JampackException{

      Zmat B[][] = new Zmat[1][2];

      B[0][0] = B00;
      B[0][1] = B01;

      return Merge.o(B);
   }

/**
   Merges its arguments to create the Zmat
<pre>
*    A = | B00 |
*        | B10 |
</pre>
   @param     Bij The Zmats to be merged
   @return    The composite Zmat A
   @exception JampackException
              Thrown if there is a nonconformity.
*/
   public static Zmat o21(Zmat B00,
                          Zmat B10)
   throws JampackException{

      Zmat B[][] = new Zmat[2][1];

      B[0][0] = B00;
      B[1][0] = B10;

      return Merge.o(B);
   }

/**
   Merges its arguments to create the matrix
<pre>
*    A = | B00 B01|
*        | B10 B11|
</pre>
   @param     Bij The Zmats to be merged
   @return    The composite Zmat A
   @exception JampackException
              Thrown if there is a nonconformity.
*/

   public static Zmat o22(Zmat B00, Zmat B01,
                          Zmat B10, Zmat B11)
   throws JampackException{


      Zmat B[][] = new Zmat[2][2];

      B[0][0] = B00;
      B[0][1] = B01;
      B[1][0] = B10;
      B[1][1] = B11;
      return Merge.o(B);
   }

/**
   Merges its arguments to create the Zmat
<pre>
*    A = | B00 B01 B02 |
</pre>
   @param     Bij The Zmats to be merged
   @return    The composite Zmat A
   @exception JampackException
              Thrown if there is a nonconformity.
*/

   public static Zmat o13(Zmat B00, Zmat B01, Zmat B02)
   throws JampackException{


      Zmat B[][] = new Zmat[1][3];

      B[0][0] = B00;
      B[0][1] = B01;
      B[0][2] = B02;

      return Merge.o(B);
   }

/**
   Merges its arguments to create the Zmat
<pre>
*    A = | B00 B01 B02 |
*        | B10 B11 B12 |
</pre>
   @param     Bij The Zmats to be merged
   @return    The composite Zmat A
   @exception JampackException
              Thown if there is a nonconformity.

*/

   public static Zmat o23(Zmat B00, Zmat B01, Zmat B02,
                          Zmat B10, Zmat B11, Zmat B12)
   throws JampackException{


      Zmat B[][] = new Zmat[2][3];

      B[0][0] = B00;
      B[0][1] = B01;
      B[0][2] = B02;
      B[1][0] = B10;
      B[1][1] = B11;
      B[1][2] = B12;

      return Merge.o(B);
   }

/**
   Merges its arguments to create the Zmat
<pre>
*    A = | B00 |
*        | B10 |
*        | B20 |
</pre>
   @param     Bij The Zmats to be merged
   @return    The composite Zmat A
   @exception JampackException
              Thrown if there is a nonconformity.
*/

   public static Zmat o31(Zmat B00,
                          Zmat B10,
                          Zmat B20)
   throws JampackException{


      Zmat B[][] = new Zmat[3][1];

      B[0][0] = B00;
      B[1][0] = B10;
      B[2][0] = B20;

      return Merge.o(B);
   }

/**
   Merges its arguments to create the Zmat
<pre>
*    A = | B00 B01 |
*        | B10 B11 |
*        | B20 B21 |
</pre>
   @param     Bij The Zmats to be merged
   @return    The composite Zmat A
   @exception JampackException
              Thrown if there is a nonconformity.
*/
   public static Zmat o32(Zmat B00, Zmat B01,
                          Zmat B10, Zmat B11,
                          Zmat B20, Zmat B21)
   throws JampackException{


      Zmat B[][] = new Zmat[3][2];

      B[0][0] = B00;
      B[0][1] = B01;
      B[1][0] = B10;
      B[1][1] = B11;
      B[2][0] = B20;
      B[2][1] = B21;

      return Merge.o(B);
   }

/**
   Merges its arguments to create the Zmat
<pre>
*    A = | B00 B01 B02 |
*        | B10 B11 B12 |
*        | B20 B21 B22 |
</pre>
   @param     Bij The Zmats to be merged
   @return    The composite Zmat A
   @exception JampackException
              Thrown if there is a nonconformity.
*/

   public static Zmat o33(Zmat B00, Zmat B01, Zmat B02,
                          Zmat B10, Zmat B11, Zmat B12,
                          Zmat B20, Zmat B21, Zmat B22)
   throws JampackException{

      Zmat B[][] = new Zmat[3][3];

      B[0][0] = B00;
      B[0][1] = B01;
      B[0][2] = B02;
      B[1][0] = B10;
      B[1][1] = B11;
      B[1][2] = B12;
      B[2][0] = B20;
      B[2][1] = B21;
      B[2][2] = B22;

      return Merge.o(B);
   }

   

}


