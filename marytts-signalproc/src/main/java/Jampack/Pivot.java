package Jampack;

/**
   Pivot applys a sequence of pivot operations to the
   rows of a matrix.  The pivot sequence is contained
   in an integer array pvt[], which determines a permution
   as follows:
<pre>
*      for (k=0; k&lt;pvt.length; k++)
*         swap k and pvt[k];
</pre>
   Both k and pvt[k] represent zero-based references
   to the rows of the matrix.
   Pivot also has a method to apply the inverse permutation.
   <p>
   Comments: Column pivoting will be added later.

   @version Pre-alpha
   @author G. W. Stewart
*/

public class Pivot{

/**
   Pivots the rows of a Zmat (altered) as specified by a pivot array.
   @param      A    The Zmat (altered)
   @param      pvt  The pivot array
   @return     The Zmat A with its rows permuted
   @exception  JampackException
               Thrown for inconsistent dimensions.
*/

   public static Zmat row(Zmat A, int pvt[])
   throws JampackException{

      int np = pvt.length;
      if (np > A.nrow)
         throw new JampackException
            ("Inconsistent array dimensions");

      A.dirty = true;

      for (int k=0; k<np; k++)
         for (int j=0; j<A.ncol; j++){
            double t = A.re[k][j];
            A.re[k][j] = A.re[pvt[k]][j];
            A.re[pvt[k]][j] = t;
            t = A.im[k][j];
            A.im[k][j] = A.im[pvt[k]][j];
            A.im[pvt[k]][j] = t;
         }
      return A;
   }

/**
   Pivots the rows of a Zmat (altered) as in the inverse order specified
   by a pivot array.
   @param      A    The Zmat (altered)
   @param      pvt  The pivot array
   @return     The Zmat A with its rows permuted
   @exception  JampackException
               Thrown for inconsitent dimensions.
*/

   public static Zmat rowi(Zmat A, int pvt[])
   throws  JampackException{

      int np = pvt.length;
      if (np > A.nrow)
         throw new JampackException
            ("Inconsistent array dimensions");

      A.dirty = true;

      for (int k=np-1; k>=0; k--)
         for (int j=0; j<A.nc; j++){
            double t = A.re[k][j];
            A.re[k][j] = A.re[pvt[k]][j];
            A.re[pvt[k]][j] = t;
            t = A.im[k][j];
            A.im[k][j] = A.im[pvt[k]][j];
            A.im[pvt[k]][j] = t;
         }
      return A;
   }
}

