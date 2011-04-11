package Jampack;

class ZspecTest{

   public static void main(String[] args)
   throws JampackException{

      int i, j;
      int n = 5;
      Z t = new Z();

      Z Ary[][] = new Z[n][n];

      for (i=0; i<n; i++){
         for (j=i; j<n; j++){
            Ary[i][j] = new Z(1./(i+j+1),  i-j);
            Ary[j][i] = new Z(Ary[i][j].re, -Ary[i][j].im);
         }
         Ary[i][i].im = 0.;
         Ary[i][i].re = 1.;
      }
      Zmat A = new Zmat(Ary);
      Zspec B = new Zspec(A);
      Zmat C = Times.o(Times.o(H.o(B.U), A), B.U);

      for (i=0; i<A.nr; i++){
         C.re[i][i] = C.re[i][i] - B.D.re[i];
         C.im[i][i] = C.im[i][i] - B.D.im[i];
      }
      Print.o(Norm.fro(C));
   }
}