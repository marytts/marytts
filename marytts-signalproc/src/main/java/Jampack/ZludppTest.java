package Jampack;

class ZludppTest{

   public static void main(String[] args)
   throws JampackException{
      int nr=5, nc=7;

      Parameters.setBaseIndex(0);

      Z B[][] = new Z[nr][nc];
      for (int i=0; i<nr; i++){
         for (int j=0; j<nc; j++)
            B[i][j] = new Z(i+1,j+1);
         if (i < nc)
            B[i][i] = new Z(1,1);
      }


      Zmat A = new Zmat(B);

      Zludpp LU = new Zludpp(A);

      Print.o(LU.pvt, 4);

      Pivot.row(A, LU.pvt);

      Print.o(Norm.fro(Minus.o(A, Times.o(LU.L, LU.U))));

   }
}
