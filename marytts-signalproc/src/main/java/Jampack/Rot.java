package Jampack;

/**
   Rot generates and manipulates plane rotations.  Given a 2-vector
   compontents are x and y, there is a unitary matrix P such that
<pre>
*      P|x| =  |   c      s||x| = |z|
*       |y|    |-conj(s)  c||y|   |0|
</pre>
   The number c, which is always real, is the cosine of the rotation.
   The number s, which may be complex is the sine of the rotation.
<p>
   Comments: This suite will eventually contain methods for real
   rotations (two are already in place).  The only difference
   between real and complex rotations is that si and zi are zero
   for the former.  The final routines will do the efficient thing.

   @version Pre-alpha
   @author G. W. Stewart
*/   



public class Rot{

/** The cosine of the rotation */
    protected double c;
/** The real part of the sine of the rotation */
    public double sr;
/** The imaginary part of the sine of the rotation */
    public double si;
/** The real part of the first component of the transformed vector*/
    public double zr;
/** The imaginary part of the first component of the transformed vector*/
    public double zi;

/**
   Given the real and imaginary parts of a 2-vector, genc returns
   a plane rotation P such that
<pre>
*      P|x| =  |   c      s||x| = |z|
*       |y|    |-conj(s)  c||y|   |0|
</pre>
   @param xr  The real part of the first component of the 2-vector
   @param xi  The imaginary part of the first component of the 2-vector
   @param yr  The real part of the second component of the 2-vector
   @param yi  The imaginary part of the second component of the 2-vector
   @return    The rotation
*/

   public static Rot genc(double xr, double xi, double yr, double yi){

      double s, absx, absxy;

      Rot P = new Rot();

      if (xr == 0 && xi==0){
         P.c = 0.;
         P.sr = 1.;
         P.si = 0.;
         P.zr = yr;
         P.zi = yi;
         return P;
      }
      s = Math.abs(xr) + Math.abs(xi);
      absx = s*Math.sqrt((xr/s)*(xr/s) + (xi/s)*(xi/s));
      s = Math.abs(s) + Math.abs(yr) + Math.abs(yi);
      absxy = s*Math.sqrt((absx/s)*(absx/s) + (yr/s)*(yr/s) + (yi/s)*(yi/s));
      P.c = absx/absxy;
      xr = xr/absx;
      xi = xi/absx;
      P.sr = (xr*yr + xi*yi)/absxy;
      P.si = (xi*yr - xr*yi)/absxy;
      P.zr = xr*absxy;
      P.zi = xi*absxy;
      return P;
   }

/**
   Given the real and imaginary parts of a 2-vector, genc generates
   a plane rotation P such that
<pre>
*      P|x| =  |   c      s||x| = |z|
*       |y|    |-conj(s)  c||y|   |0|
</pre>
   @param xr  The real part of the first component of the 2-vector
   @param xi  The imaginary part of the first component of the 2-vector
   @param yr  The real part of the second component of the 2-vector
   @param yi  The imaginary part of the second component of the 2-vector
   @param P   The rotation (must be initialized)
*/

   public static void genc(double xr, double xi, double yr, double yi, Rot P){

      double s, absx, absxy;

      if (xr == 0 && xi==0){
         P.c = 0.;
         P.sr = 1.;
         P.si = 0.;
         P.zr = yr;
         P.zi = yi;
         return;
      }
      s = Math.abs(xr) + Math.abs(xi);
      absx = s*Math.sqrt((xr/s)*(xr/s) + (xi/s)*(xi/s));
      s = Math.abs(s) + Math.abs(yr) + Math.abs(yi);
      absxy = s*Math.sqrt((absx/s)*(absx/s) + (yr/s)*(yr/s) + (yi/s)*(yi/s));
      P.c = absx/absxy;
      xr = xr/absx;
      xi = xi/absx;
      P.sr = (xr*yr + xi*yi)/absxy;
      P.si = (xi*yr - xr*yi)/absxy;
      P.zr = xr*absxy;
      P.zi = xi*absxy;
   }

/**
   Given a real 2-vector, genc returns
   a real plane rotation P such that
<pre>
*      P|x| =  | c  s||x| = |z|
*       |y|    |-s  c||y|   |0|
</pre>
   @param x   The first component of the two vector
   @param y   The second component of the two vector
   @return    The rotation
*/
   public static Rot genc(double x, double y){

      Rot P = new Rot();

      P.si = 0.;
      P.zi = 0.;

      if (x==0 & y==0){
         P.c = 1;
         P.sr = 0.;
         P.zr = 0.;
         return P;
      }

      double s = Math.abs(x) + Math.abs(y);
      P.zr = s*Math.sqrt((x/s)*(x/s) + (y/s)*(y/s));
      P.c = x/P.zr;
      P.sr = y/P.zr;
      return P;
   }

/**
   Given a real 2-vectc, genc generates
   a real plane rotation P such that
<pre>
*      P|x| =  | c  s||x| = |z|
*       |y|    |-s  c||y|   |0|
</pre>
   @param x   The first component of the two vector
   @param y   The second component of the two vector
   @param P   The plane rotation
*/
   public static void genc(double x, double y, Rot P){

      P.si = 0.;
      P.zi = 0.;

      if (x==0 & y==0){
         P.c = 1;
         P.sr = 0.;
         P.zr = 0.;
         return;
      }

      double s = Math.abs(x) + Math.abs(y);
      P.zr = s*Math.sqrt((x/s)*(x/s) + (y/s)*(y/s));
      P.c = x/P.zr;
      P.sr = y/P.zr;
   }

/**
    Given a Zmat A, genc returns a plane rotation that on
    premultiplication into rows ii1 and ii2
    annihilates A(ii2,jj).  The element A(ii2,jj) is
    overwriten by zero and the element A(ii1,jj) is
    overwritten by its transformed value.
    @param A    The Zmat (altered)
    @param ii1  The row index of the first element
    @param ii2  The row index of the second element (the
                one that is annihilated
    @param jj   The column index of the elements
    @return     The plane rotation
*/
   public static Rot genc(Zmat A, int ii1, int ii2, int jj){

      A.dirty = true;

      int i1 = ii1 - A.basex;
      int i2 = ii2 - A.basex;
      int j = jj - A.basex;

      Rot P = Rot.genc(A.re[i1][j], A.im[i1][j], A.re[i2][j], A.im[i2][j]);
      A.re[i1][j] = P.zr;
      A.im[i1][j] = P.zi;
      A.re[i2][j] = 0;
      A.im[i2][j] = 0;
      return P;
   }

/**
    Given a Zmat A, genc generates a plane rotation that on
    premultiplication into rows ii1 and ii2
    annihilates A(ii2,jj).  The element A(ii2,jj) is
    overwriten by zero and the element A(ii1,jj) is
    overwritten by its transformed value.
    @param A    The Zmat (altered)
    @param ii1  The row index of the first element
    @param ii2  The row index of the second element (the
                one that is annihilated
    @param jj   The column index of the elements
    @param P    The plane rotation (must be initialized)
*/
   public static void genc(Zmat A, int ii1, int ii2, int jj, Rot P){

      A.dirty = true;

      int i1 = ii1 - A.basex;
      int i2 = ii2 - A.basex;
      int j = jj - A.basex;

      Rot.genc(A.re[i1][j], A.im[i1][j], A.re[i2][j], A.im[i2][j], P);
      A.re[i1][j] = P.zr;
      A.im[i1][j] = P.zi;
      A.re[i2][j] = 0;
      A.im[i2][j] = 0;
   }

/**
   Given the real and imaginary parts of a 2-vector, genr returns
   a plane rotation such that
<pre>
*      |x y|P = |x y||   c      s||x| = |z 0|
*                    |-conj(s)  c||y|
</pre>
   @param xr  The real part of the first component of the 2-vector
   @param xi  The imaginary part of the first component of the 2-vector
   @param yr  The real part of the second component of the 2-vector
   @param yi  The imaginary part of the second component of the 2-vector
   @return    The rotation
*/

   public static Rot genr(double xr, double xi, double yr, double yi){

      double s, absx, absxy;

      Rot P = new Rot();

      if (xr == 0 && xi==0){
         P.c = 0.;
         P.sr = 1.;
         P.si = 0.;
         P.zr = yr;
         P.zi = yi;
         return P;
      }
      s = Math.abs(xr) + Math.abs(xi);
      absx = s*Math.sqrt((xr/s)*(xr/s) + (xi/s)*(xi/s));
      s = Math.abs(s) + Math.abs(yr) + Math.abs(yi);
      absxy = s*Math.sqrt((absx/s)*(absx/s) + (yr/s)*(yr/s) + (yi/s)*(yi/s));
      P.c = absx/absxy;
      xr = xr/absx;
      xi = xi/absx;
      P.sr = -(xr*yr + xi*yi)/absxy;
      P.si = (xi*yr - xr*yi)/absxy;
      P.zr = xr*absxy;
      P.zi = xi*absxy;
      return P;
   }

/**
   Given the real and imaginary parts of a 2-vector, genr generates
   a plane rotation such that
<pre>
*      |x y|P = |x y||   c      s||x| = |z 0|
*                    |-conj(s)  c||y|
</pre>
   @param xr  The real part of the first component of the 2-vector
   @param xi  The imaginary part of the first component of the 2-vector
   @param yr  The real part of the second component of the 2-vector
   @param yi  The imaginary part of the second component of the 2-vector
   @param P   The plane rotation (must be initialized)
*/

   public static void genr(double xr, double xi, double yr, double yi, Rot P){

      double s, absx, absxy;

      if (xr == 0 && xi==0){
         P.c = 0.;
         P.sr = 1.;
         P.si = 0.;
         P.zr = yr;
         P.zi = yi;
         return;
      }
      s = Math.abs(xr) + Math.abs(xi);
      absx = s*Math.sqrt((xr/s)*(xr/s) + (xi/s)*(xi/s));
      s = Math.abs(s) + Math.abs(yr) + Math.abs(yi);
      absxy = s*Math.sqrt((absx/s)*(absx/s) + (yr/s)*(yr/s) + (yi/s)*(yi/s));
      P.c = absx/absxy;
      xr = xr/absx;
      xi = xi/absx;
      P.sr = -(xr*yr + xi*yi)/absxy;
      P.si = (xi*yr - xr*yi)/absxy;
      P.zr = xr*absxy;
      P.zi = xi*absxy;
   }

/**
    Given a Zmat A, genr returns a plane rotation that on
    postmultiplication into column jj1 and jj2
    annihilates A(ii,jj2).  The element A(ii,jj2) is
    overwirten by zero and the element A(ii,jj1) is
    overwritten by its transformed value.
    @param A    The Zmat (altered)
    @param ii   The index of the row containing the elements
    @param jj1  The column index of the first element
    @param jj2  The column index of the second element (the
                one that is annihilated)
    @return     The rotation
*/

   public static Rot genr(Zmat A, int ii, int jj1, int jj2){

      A.dirty = true;

      int i = ii - A.basex;
      int j1 = jj1 - A.basex;
      int j2 = jj2 - A.basex;

      Rot P = Rot.genr(A.re[i][j1], A.im[i][j1], A.re[i][j2], A.im[i][j2]);
      A.re[i][j1] = P.zr;
      A.im[i][j1] = P.zi;
      A.re[i][j2] = 0;
      A.im[i][j2] = 0;
      return P;
   }

/**
    Given a Zmat A, genr generates a plane rotation that on
    postmultiplication into column jj1 and jj2
    annihilates A(ii,jj2).  The element A(ii,jj2) is
    overwirten by zero and the element A(ii,jj1) is
    overwritten by its transformed value.
    @param A    The Zmat (altered)
    @param ii   The index of the row containing the elements
    @param jj1  The column index of the first element
    @param jj2  The column index of the second element (the
                one that is annihilated)
    @param P    The rotation
*/

   public static void genr(Zmat A, int ii, int jj1, int jj2, Rot P){

      A.dirty = true;

      int i = ii - A.basex;
      int j1 = jj1 - A.basex;
      int j2 = jj2 - A.basex;

      Rot.genr(A.re[i][j1], A.im[i][j1], A.re[i][j2], A.im[i][j2], P);
      A.re[i][j1] = P.zr;
      A.im[i][j1] = P.zi;
      A.re[i][j2] = 0;
      A.im[i][j2] = 0;
   }

/**
   Given  a real 2-vector, genr returns
   a plane rotation such that
<pre>
*      |x y|P = |x y|| c  s||x| = |z 0|
*                    |-s  c||y|
</pre>
   @param x   The first component of the 2-vector
   @param y   The second component of the 2-vector
   @return    The rotation
*/

public static Rot genr(double x, double y){

      Rot P = new Rot();

      P.si = 0.;
      P.zi = 0.;

      double s = Math.abs(x) + Math.abs(y);

      if (s == 0.){
         P.c = 1.;
         P.sr = 0.;
         P.zr = 0.;
         return P;
      }

      P.zr = s*Math.sqrt((x/s)*(x/s) + (y/s)*(y/s));
      P.c = x/P.zr;
      P.sr = -y/P.zr;
      return P;
   }


/**
   Given  a real 2-vector, genr generates
   a plane rotation such that
<pre>
*      |x y|P = |x y|| c  s||x| = |z 0|
*                    |-s  c||y|
</pre>
   @param x   The first component of the 2-vector
   @param y   The second component of the 2-vector
   @param P   The rotation
*/
public static void genr(double x, double y, Rot P){

      P.si = 0.;
      P.zi = 0.;

      double s = Math.abs(x) + Math.abs(y);

      if (s == 0.){
         P.c = 1.;
         P.sr = 0.;
         P.zr = 0.;
         return;
      }

      P.zr = s*Math.sqrt((x/s)*(x/s) + (y/s)*(y/s));
      P.c = x/P.zr;
      P.sr = -y/P.zr;
   }


/**
   Multiplies rows (ii1,jj1:jj2) and (ii2,jj1:jj2)
   of a Zmat (altered) by a plane rotation.
   @param P    The plane rotation
   @param A    The Zmat (altered)
   @param ii1  The row index of the first row.
   @param ii2  The row index of the second row.
   @param jj1  The first index of the range of the rows
   @param jj2  The second index of the range of the rows
*/
   

   public static void pa(Rot P, Zmat A, int ii1, int ii2, int jj1, int jj2 ){

      double t1r, t1i, t2r, t2i;

      A.dirty = true;

      int i1 = ii1 - A.basex;
      int i2 = ii2 - A.basex;
      int j1 = jj1 - A.basex;
      int j2 = jj2 - A.basex;


      for (int j=j1; j<=j2; j++){
         t1r = P.c*A.re[i1][j] + P.sr*A.re[i2][j] - P.si*A.im[i2][j];
         t1i = P.c*A.im[i1][j] + P.sr*A.im[i2][j] + P.si*A.re[i2][j];
         t2r = P.c*A.re[i2][j] - P.sr*A.re[i1][j] - P.si*A.im[i1][j];
         t2i = P.c*A.im[i2][j] - P.sr*A.im[i1][j] + P.si*A.re[i1][j];
         A.re[i1][j] = t1r;
         A.im[i1][j] = t1i;
         A.re[i2][j] = t2r;
         A.im[i2][j] = t2i;
      }  
   }

/**
   Multiplies rows (ii1,jj1:jj2) and (ii2,jj1:jj2)
   of a Zmat (altered) by the conjugate transpose of a plane rotation.
   @param P    The plane rotation
   @param A    The Zmat (altered)
   @param ii1  The row index of the first row.
   @param ii2  The row index of the second row.
   @param jj1  The first index of the range of the rows
   @param jj2  The second index of the range of the rows
*/
   public static void pha(Rot P, Zmat A, int ii1, int ii2, int jj1, int jj2 ){

      double t1r, t1i, t2r, t2i;

      A.dirty = true;

      int i1 = ii1 - A.basex;
      int i2 = ii2 - A.basex;
      int j1 = jj1 - A.basex;
      int j2 = jj2 - A.basex;


      for (int j=j1; j<=j2; j++){
         t1r = P.c*A.re[i1][j] - P.sr*A.re[i2][j] + P.si*A.im[i2][j];
         t1i = P.c*A.im[i1][j] - P.sr*A.im[i2][j] - P.si*A.re[i2][j];
         t2r = P.c*A.re[i2][j] + P.sr*A.re[i1][j] + P.si*A.im[i1][j];
         t2i = P.c*A.im[i2][j] + P.sr*A.im[i1][j] - P.si*A.re[i1][j];
         A.re[i1][j] = t1r;
         A.im[i1][j] = t1i;
         A.re[i2][j] = t2r;
         A.im[i2][j] = t2i;
      }  
   }


/**
   Multiplies columns (ii1:ii2,jj1) and A(ii2:ii2,jj1)
   of a Zmat (altered) by a plane rotation.
   @param A    The Zmat (altered)
   @param P    The rotation
   @param ii1  The first index of the column range
   @param ii2  The second index of the column range
   @param jj1  The index of the first column
   @param jj2  The index of the second column
*/

   public static void ap(Zmat A, Rot P, int ii1, int ii2, int jj1, int jj2 ){

      double t1r, t1i, t2r, t2i;

      A.dirty = true;

      int i1 = ii1 - A.basex;
      int i2 = ii2 - A.basex;
      int j1 = jj1 - A.basex;
      int j2 = jj2 - A.basex;


      for (int i=i1; i<=i2; i++){
         t1r = P.c*A.re[i][j1] - P.sr*A.re[i][j2] - P.si*A.im[i][j2];
         t1i = P.c*A.im[i][j1] - P.sr*A.im[i][j2] + P.si*A.re[i][j2];
         t2r = P.c*A.re[i][j2] + P.sr*A.re[i][j1] - P.si*A.im[i][j1];
         t2i = P.c*A.im[i][j2] + P.sr*A.im[i][j1] + P.si*A.re[i][j1];
         A.re[i][j1] = t1r;
         A.im[i][j1] = t1i;
         A.re[i][j2] = t2r;
         A.im[i][j2] = t2i;
      }  
   }

/**
   Multiplies columns (ii1:ii2,jj1) and A(ii2:ii2,jj1)
   of a Zmat (altered) by the conjugate transpose of plane rotation.
   @param A    The Zmat (altered)
   @param P    The rotation
   @param ii1  The first index of the column range
   @param ii2  The second index of the column range
   @param jj1  The index of the first column
   @param jj2  The index of the second column
*/

   public static void aph(Zmat A, Rot P, int ii1, int ii2, int jj1, int jj2 ){

      double t1r, t1i, t2r, t2i;

      A.dirty = true;

      int i1 = ii1 - A.basex;
      int i2 = ii2 - A.basex;
      int j1 = jj1 - A.basex;
      int j2 = jj2 - A.basex;


      for (int i=i1; i<=i2; i++){
         t1r = P.c*A.re[i][j1] + P.sr*A.re[i][j2] + P.si*A.im[i][j2];
         t1i = P.c*A.im[i][j1] + P.sr*A.im[i][j2] - P.si*A.re[i][j2];
         t2r = P.c*A.re[i][j2] - P.sr*A.re[i][j1] + P.si*A.im[i][j1];
         t2i = P.c*A.im[i][j2] - P.sr*A.im[i][j1] - P.si*A.re[i][j1];
         A.re[i][j1] = t1r;
         A.im[i][j1] = t1i;
         A.re[i][j2] = t2r;
         A.im[i][j2] = t2i;
      }
   }


}

