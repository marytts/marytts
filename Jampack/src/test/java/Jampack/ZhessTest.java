package Jampack;

class ZhessTest{

   public static void main(String[] args)
   throws JampackException{

      Parameters.setBaseIndex(0);

      int n = 5;

      Zmat A = new Zmat(n,n);

      for (int i=A.bx; i<=A.rx; i++){
         for (int j=A.bx; j<=A.cx; j++){
            A.put(i, j, new Z(i+j, i-j));
         }
         A.put(i,i, new Z(2*n, 2*n));
      }

      Zhess B = new Zhess(A);

      Print.o(B.H);
      Print.o(Times.o(H.o(B.U), Times.o(A, B.U)));
      Print.o(Norm.fro(Minus.o(A,Times.o(B.U, Times.o(B.H, H.o(B.U))))));
   }
}
