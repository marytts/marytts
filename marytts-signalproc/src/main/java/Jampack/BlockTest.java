package Jampack;

class BlockTest{

   public static void main(String[] args)
   throws JampackException{

      Parameters.setBaseIndex(1);

      Zmat A = new Zmat(10,9);

      int bx = A.bx;
      int rx = A.rx;
      int cx = A.cx;

      for (int i=bx; i<=rx; i++){
         for (int j=bx; j<=cx; j++){
            A.put(i, j, new Z(i,j));
         }
      }


      int ii[] = {A.bx,  5, 6, A.rx+1};
      int jj[] = {A.bx, 3, 4,  A.cx+1};

      Zmat B[][] = Block.o(A,  new int[] {A.bx, 5, 6, A.rx+1},
                               new int[] {A.bx, 4, 7, A.cx+1});
      
      Print.o(Norm.fro(Minus.o(A, Merge.o(B))));
   }
}
