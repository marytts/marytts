package Jampack;

class ZsvdTest{

   public static void main(String[] args)
   throws JampackException{

      Parameters.setBaseIndex(0);

//    Zsvd.MAXITER = 2;

      int m = 8;
      int n = 10;

      Z Ary[][] = new Z[m][n];
      for (int i=0; i<m; i++){
         for (int j=0; j<n; j++){
            double di = i+1;
            double dj = j+1;
            Ary[i][j] = new  Z(di/dj, i-j);
         }
         if (i < n){
            Ary[i][i].re = Ary[i][i].re + 2*i + 1;
         }
      }

      Zmat X = new Zmat(Ary);
      Zsvd SVD = new Zsvd(X);

      Zmat XX = Times.o(Times.o(H.o(SVD.U),X),SVD.V);
      m = Math.min(XX.rx, XX.cx);
      Zmat Xl = XX.get(XX.bx, m, XX.bx, m);
      Xl = Minus.o(Xl, SVD.S);
      XX.put(XX.bx, m, XX.bx, m, Xl);
      Print.o(Norm.fro(XX));
   }
}
