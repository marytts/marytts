package Jampack;

class SwapTest{

   public static void main(String[] args)
   throws JampackException{

      int i, j;
      Z z = new Z();

      Zmat A = new Zmat(3, 3);
      for (i=A.bx; i<=A.rx; i++){
         for (j=A.bx; j<=A.cx; j++){
            A.put(i,j, z.Eq(i,j));
         }
      }

      Zmat AA = new Zmat(A);

      for (i=AA.bx; i<AA.rx; i++){
         Swap.rows(AA, i, i+1);
      }
      for (i=AA.rx-1; i>=AA.bx; i--){
         Swap.rows(AA, i+1, i);
      }
      Print.o(Norm.fro(Minus.o(AA,A)));

      for (j=AA.bx; j<AA.cx; j++){
         Swap.cols(AA, j, j+1);
      }
      for (j=AA.cx-1; j>=AA.bx; j--){
         Swap.cols(AA, j+1, j);
      }
      Print.o(Norm.fro(Minus.o(AA,A)));

   }
}
