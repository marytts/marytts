package Jampack;

/**
   Schur implements the Schur decomposition of a matrix.  Specifically,
   given a square matrix A, there is a unitary matrix U such that
<pre>
*      T = U^H AU
</pre>
   is upper triangular.  Schur represents T as a Zutmat and U as a Zmat.

   @version Pre-alpha
   @author G. W. Stewart
*/

public class Schur{

/** The upper triangular matrix. */
    public Zutmat T;

/** The unitary matrix. */
    public Zmat U;

/** Limits the number of interations in the QR algorithm */
    public static int MAXITER = 30;

/**
   Creats a Schur decomposition from a square Zmat.
   @param     A  The Zmat whose Schur decomposition is to be computed
   @exception JampackException
              Thrown for nonsquare matrix.<br>
              Thrown for maximum iteration count exceeded.
*/
   public Schur(Zmat A)
      throws JampackException{

      int i, il, iter, iu, k;
      double d, sd, sf;
      Z  b = new Z(), c = new Z(), disc = new Z(), kappa = new Z(), 
      p, q, r, r1 = new Z(), r2 = new Z(), s, z1 = new Z(), z2 = new Z();
      Rot P = new Rot();

      if (A.nr != A.nc){
         throw new JampackException
            ("Nonsquare matrix");
      }

      /* Reduce to Hessenberg form and set up T and U */

      Zhess H = new Zhess(A);
      T = new Zutmat(H.H);
      U = H.U;

      iu = T.rx;
      iter = 0;
      while(true){

         // Locate the range in which to iterate.

         while (iu > T.bx){
            d = Z.abs1(T.get(iu,iu)) + Z.abs1(T.get(iu-1,iu-1));
            sd = Z.abs1(T.get(iu,iu-1));
            if (sd >= 1.0e-16*d) break;
            T.put(iu, iu-1, Z.ZERO);
            iter = 0;
            iu = iu-1;
         }
         if (iu == T.bx) break;

         iter = iter+1;
         if (iter >= MAXITER){
            throw new JampackException
               ("Maximum number of iterations exceeded.");
         }
         il  = iu-1;
         while (il > T.bx){
            d = Z.abs1(T.get(il,il)) + Z.abs1(T.get(il-1,il-1));
            sd = Z.abs1(T.get(il,il-1));
            if (sd < 1.0e-16*d) break;
            il = il-1;
         }
         if(il != T.bx){
            T.put(il, il-1, Z.ZERO);
         }
         
         // Compute the shift.

         p = T.get(iu-1,iu-1);
         q = T.get(iu-1,iu);
         r = T.get(iu,iu-1);
         s = T.get(iu,iu);

         sf = Z.abs1(p) + Z.abs1(q) + Z.abs1(r) + Z.abs1(s);
         p.Div(p, sf);
         q.Div(q, sf);
         r.Div(r, sf);
         s.Div(s, sf);

         c.Minus(z1.Times(p, s), z2.Times(r, q));
         b.Plus(p, s);

         disc.Sqrt(disc.Minus(z1.Times(b,b), z2.Times(4,c)));
         r1.Div(r1.Plus(b, disc), 2);
         r2.Div(r2.Minus(b, disc), 2);
         if (Z.abs1(r1) > Z.abs1(r2)){
            r2.Div(c, r1);
         }
         else{
            r1.Div(c, r2);
         }
         if (Z.abs1(z1.Minus(r1, s)) < Z.abs1(z2.Minus(r2, s))){
            kappa.Times(sf, r1);
         }
         else{
            kappa.Times(sf, r2);
         }

        // Perform the QR step.

         p.Minus(T.get(il,il), kappa);
         q.Eq(T.get(il+1,il));
         Rot.genc(p.re, p.im, q.re, q.im, P);
         for (i=il; i<iu; i++){
            Rot.pa(P, T, i, i+1, i, T.cx);
            Rot.aph(T, P, T.bx, Math.min(i+2,iu), i, i+1);
            Rot.aph(U, P, U.bx, U.rx, i, i+1);
            if (i != iu-1){
               Rot.genc(T, i+1, i+2, i, P);
            }
         }
      }
   }
}
