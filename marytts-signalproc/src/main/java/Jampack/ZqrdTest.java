package Jampack;

class ZqrdTest{


   public static void main(String[] args)
   throws JampackException{

      int i, j, m=10, n=10;

      Parameters.setBaseIndex(0);

      Z Aa[][] = new Z[m][n];
      for (i=0; i<m; i++){
         for (j=0; j<n; j++){
            Aa[i][j] = new Z(i+1, j+1);
         }
      }

      Zmat A = new Zmat(Aa);

      Zqrd X = new Zqrd(A);


      Print.o(Norm.fro(Minus.o(Eye.o(X.Q.nc), Times.o(H.o(X.Q), X.Q))));

      Print.o(Norm.fro(Minus.o(A, Times.o(X.Q, X.R))));
   }
}

