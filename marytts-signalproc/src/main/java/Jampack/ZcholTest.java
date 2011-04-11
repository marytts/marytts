package Jampack;

class ZcholTest{

   public static void main(String[] args)
   throws JampackException{

      int n=5;

      Zmat A = new Zmat(n,n);
      for (int i=0; i<n; i++){
         for (int j=0; j<n; j++){
            A.re[i][j] = i;
            A.im[i][j] = i+j;
         }
         A.re[i][i] = 2*n;
         A.im[i][i] = 2*n;
      }
      A = Times.o(H.o(A), A);
      Zchol CH = new Zchol(A);
      Print.o(Norm.fro(Minus.o(A, Times.o(H.o(CH.R), CH.R))));
   }
}

