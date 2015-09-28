package Jampack;

class SchurTest{

   public static void main(String[] args)
   throws JampackException{

      Parameters.setBaseIndex(1);

      int n = 50;

//      Schur.MAXITER = 2;

      Z z = new Z();
      Zmat A = new Zmat(n,n);
      
      for (int i=A.bx; i<=A.rx; i++){
         for (int j=A.bx; j<=A.cx; j++){
            A.put(i, j, new Z(i+j, i-2*j));
         }
         A.put(i,i, new Z(2*n, 2*n));
      }

      Schur B = new Schur(A);
      Print.o(Z.abs(z.Minus(Trace.o(A), Trace.o(B.T))));
      Print.o(Norm.fro(Minus.o(A,Times.o(B.U, Times.o(B.T, H.o(B.U))))));
   }
}
