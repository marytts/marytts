package Jampack;

class EigTest{

   public static void main(String[] args)
      throws JampackException{

      Parameters.setBaseIndex(1);

      int n = 5;
      Z z = new Z();
      Zmat A = new Zmat(n,n);
      
      for (int i=A.bx; i<=A.rx; i++){
         for (int j=A.bx; j<=A.cx; j++){
            A.put(i, j, new Z(i+j, i-2*j));
         }
         A.put(i,i, new Z(2*n, 2*n));
      }

      Eig B = new Eig(A);
      Print.o(B.D);
      Print.o(new Zdiagmat(Times.o(H.o(B.X),B.X)));
      Print.o(Norm.fro(Minus.o(Times.o(A, B.X), Times.o(B.X, B.D))));
   }
}
