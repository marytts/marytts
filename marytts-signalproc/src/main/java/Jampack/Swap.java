package Jampack;

/**
   Swap interchanges rows and columns of a matrix.

   @version Pre-alpha
   @author G. W. Stewart
*/

public class Swap{

/**
   Interchances two rows of a Zmat (altered).
   @param     A  The Zmat (altered)
   @param     r1 The index of the first row
   @param     r2 The index of the second row
   @exception JampackException
              Thrown for inconsistent row indices.
*/

   public static void rows(Zmat A, int r1, int r2)
   throws JampackException{

      A.getProperties();
      if (r1<A.bx || r1>A.rx || r2<A.bx || r2>A.rx){
         throw new JampackException
            ("Inconsistent row indices");
      }

      A.dirty = true;

      r1 = r1-A.bx;
      r2 = r2-A.bx;

      for (int j=0; j<A.nr; j++){
         double t = A.re[r1][j];
         A.re[r1][j] = A.re[r2][j];
         A.re[r2][j] = t;
         t = A.im[r1][j];
         A.im[r1][j] = A.im[r2][j];
         A.im[r2][j] = t;
      }
   }

/**
   Interchances two columns of a Zmat (altered).
   @param     A  The Zmat (altered)
   @param     c1 The index of the first column
   @param     c2 The index of the second column
   @exception JampackException
              Thrown for inconsistent column indices.
*/
   public static void cols(Zmat A, int c1, int c2)
   throws JampackException{

      A.getProperties();
      if (c1<A.bx || c1>A.cx || c2<A.bx || c2>A.cx){
         throw new JampackException
            ("Inconsistent row indices");
      }

      A.dirty = true;

      c1 = c1-A.bx;
      c2 = c2-A.bx;

      for (int i=0; i<A.nc; i++){
         double t = A.re[i][c1];
         A.re[i][c1] = A.re[i][c2];
         A.re[i][c2] = t;
         t = A.im[i][c1];
         A.im[i][c1] = A.im[i][c2];
         A.im[i][c2] = t;
      }
   }
}
