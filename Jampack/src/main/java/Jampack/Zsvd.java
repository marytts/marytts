package Jampack;

/**
   Zsvd implements the singular value decomposion of a Zmat.
   Specifically if X is an mxn matrix with m&gt;=n there are unitary
   matrices U and V such that
<pre>
*     U^H*X*V = | S |
*               | 0 |
</pre>
   where S = diag(s1,...,sm) with
<pre>
*     s1 >= s2 >= ... >= sn >=0.
</pre>
   If m&lt;n the decomposition has the form
<pre>
*     U^H*X*V = | S  0 |,
</pre>

   where S is diagonal of order m.  The diagonals of S are the
   singular values of A.  The columns of U are the left singular
   vectors of A and the columns of V are the right singular vectors.

   @version Pre-alpha
   @author G. W. Stewart
*/

public class Zsvd{

/** Limits the number of iterations in the SVD algorithm */
    public static int MAXITER = 30;

/** The matrix of left singular vectors */
    public Zmat U;

/** The matrix of right singular vectore */
    public Zmat V;

/** The diagonal matrix of singular values */

    public Zdiagmat S;

/**
    Computes the SVD of a Zmat XX.  Throws a JampackException
    if the maximum number of iterations is exceeded.

    @param     XX A Zmat
    @return    The Zsvd of XX
    @exception JampackException
               Thrown if maximimum number of iterations is
               exceeded.<br>
               Passed from below.
*/

   public Zsvd(Zmat XX)
   throws JampackException{

      int i, il, iu, iter, j, k, kk, m, mc;

      double as, at, au, axkk, axkk1, dmax, dmin, ds, ea,
             es, shift, ss, t, tre;

      Z xkk, xkk1, xk1k1, ukj, vik1;

      Rot P = new Rot();

/*    Initialization */

      Z scale = new Z();
      Z zr = new Z();

      Zmat X = new Zmat(XX);
      
      Z1 h;
      Z1 temp = new Z1(Math.max(X.nr,X.nc));

      mc = Math.min(X.nr, X.nc);
      double d[] = new double[mc];
      double e[] = new double[mc];

      S = new Zdiagmat(mc);
      U = Eye.o(X.nr);
      V = Eye.o(X.nc);

      m = Math.min(X.rx, X.cx);
      

/*
      Reduction to Bidiagonal form.
*/

      for (k=X.bx; k<=m; k++){

         h = House.genc(X, k, X.rx, k);
         House.ua(h, X, k, X.rx, k+1, X.cx, temp);
         House.au(U, h, U.bx, U.rx, k, U.cx, temp);
         
         if (k != X.cx){
            h = House.genr(X, k, k+1, X.cx);
            House.au(X, h, k+1, X.rx, k+1, X.cx, temp);
            House.au(V, h, V.bx, V.rx, k+1, V.cx, temp);
         }
      }

/*
      Scale the bidiagonal matrix so that its elements are
      real.
*/

      for (k=X.bx; k<=m; k++){
         kk = k-X.bx;
         xkk = X.get(k,k);
         axkk = Z.abs(xkk);
         X.put(k, k, new Z(axkk));
         d[kk] = axkk;
         scale.Div(scale.Conj(xkk), axkk);
         if (k<X.cx){
            xkk1 = X.get(k,k+1);
            X.put(k, k+1, xkk1.Times(scale, xkk1));
         }
         scale.Conj(scale);
         for (i=U.bx; i<=U.rx; i++){
            U.put(i, k, zr.Times(U.get(i, k), scale));
         }

         if (k<X.cx){

            xkk1 = X.get(k,k+1);
            axkk1 = Z.abs(xkk1);
            X.put(k, k+1, new Z(axkk1));
            e[kk] = axkk1;
            scale.Div(scale.Conj(xkk1), axkk1);
            if (k<X.rx){
               xk1k1 = X.get(k+1,k+1);
               X.put(k+1, k+1, xk1k1.Times(scale, xk1k1));
            }
            for (i=V.bx; i<=V.rx; i++){
               V.put(i, k+1, zr.Times(V.get(i, k+1), scale));
            }
         }
      }

      m = m - X.bx;  // Zero based loops from here on.
/*
      If X has more columns than rows, rotate out the extra
      superdiagonal element.
*/
      if (X.nr < X.nc){
         t = e[m];
         for (k=m; k>=0; k--){
            Rot.genr(d[k], t, P);
            d[k] = P.zr;
            if (k != 0){
               t = P.sr*e[k-1];
               e[k-1] = P.c*e[k-1];
            }
            Rot.ap(V, P, V.bx, V.rx, k+V.bx, X.rx+1);
            Rot.ap(X, P, X.bx, X.rx, k+X.bx, X.rx+1);
         }
      }
/*
      Caculate the singular values of the bidiagonal matrix.
*/
      iu = m;
      iter = 0;
      while (true){
/*
         These two loops determine the rows (il to iu) to
         iterate on.
*/
         while (iu > 0){
            if (Math.abs(e[iu-1]) > 
                         1.0e-16*(Math.abs(d[iu]) + Math.abs(d[iu-1])))
               break;
            e[iu-1] = 0.;
            iter = 0;
            iu = iu - 1;
         }
         iter = iter+1;
         if (iter > MAXITER){
            throw new JampackException
               ("Maximum number of iterations exceeded.");
         }
         if (iu == 0) break;

         il = iu-1;
         while(il > 0){
            if(Math.abs(e[il-1]) <=
                        1.0e-16*(Math.abs(d[il]) + Math.abs(d[il-1])))
               break;
            il = il-1;
         }
         if (il != 0){
            e[il-1] = 0.;
         }
/*
         Compute the shift (formulas adapted from LAPACK).
*/
         dmax = Math.max(Math.abs(d[iu]), Math.abs(d[iu-1]));
         dmin = Math.min(Math.abs(d[iu]), Math.abs(d[iu-1]));
         ea = Math.abs(e[iu-1]);
         if (dmin == 0.){
            shift = 0.;
         }
         else if(ea < dmax){
            as = 1. + dmin/dmax;
            at = (dmax-dmin)/dmax;
            au = ea/dmax;
            au = au*au;
            shift =dmin*(2./(Math.sqrt(as*as+au) + Math.sqrt(at*at+au)));
         }
         else{
            au = dmax/ea;
            if (au == 0.){
               shift = (dmin*dmax)/ea;
            }
            else{
               as = 1. + dmin/dmax;
               at = (dmax-dmin)/dmax;
               t = 1./(Math.sqrt(1.+(as*au)*(as*au))+
                       Math.sqrt(1.+(at*au)*(at*au)));
               shift = (t*dmin)*au;
            }
         }
/*
        Perform the implicitly shifted QR step.
*/
         t = Math.max(Math.max(Math.abs(d[il]),Math.abs(e[il])), shift);
         ds = d[il]/t; es=e[il]/t; ss = shift/t;
         Rot.genr((ds-ss)*(ds+ss), ds*es, P);
         for (i=il; i<iu; i++){
            t = P.c*d[i] - P.sr*e[i];
            e[i] = P.sr*d[i] + P.c*e[i];
            d[i] = t;
            t = -P.sr*d[i+1];
            d[i+1] =  P.c*d[i+1];
            Rot.ap(V, P, V.bx, V.rx, V.bx+i, V.bx+i+1);
            Rot.genc(d[i], t, P);
            d[i] = P.zr;
            t = P.c*e[i] + P.sr*d[i+1];
            d[i+1] = P.c*d[i+1] - P.sr*e[i];
            e[i] = t;
            Rot.aph(U, P, U.bx, U.rx, U.bx+i, U.bx+i+1);
            if (i != iu-1){
               t = P.sr*e[i+1];
               e[i+1] = P.c*e[i+1];
               Rot.genr(e[i], t, P);
               e[i] = P.zr;
            }
         }
      }

/*
      Sort the singular values, setting negative values of d
      to positive.
*/
      for (k=m; k>=0; k--){
         if (d[k] < 0){
            d[k] = -d[k];
            for (i=0; i<X.nc; i++){
               V.re[i][k] = -V.re[i][k];
               V.im[i][k] = -V.im[i][k];
            }
         }
         for (j=k; j<m; j++){
            if(d[j] < d[j+1]){
               t = d[j];
               d[j] = d[j+1];
               d[j+1] = t;
               for (i=0; i<X.nr; i++){
                  t = U.re[i][j];
                  U.re[i][j] = U.re[i][j+1];
                  U.re[i][j+1] = t;
                  t = U.im[i][j];
                  U.im[i][j] = U.im[i][j+1];
                  U.im[i][j+1] = t;
               }
               for (i=0; i<X.nc; i++){
                  t = V.re[i][j];
                  V.re[i][j] = V.re[i][j+1];
                  V.re[i][j+1] = t;
                  t = V.im[i][j];
                  V.im[i][j] = V.im[i][j+1];
                  V.im[i][j+1] = t;
               }
            }
         }
      }
/*
      Return the decompostion;
*/
      S.re = d;
      return;
   }
}
