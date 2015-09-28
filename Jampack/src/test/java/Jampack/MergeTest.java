package Jampack;

class MergeTest{

   public static void main(String[] args)
   throws JampackException{

      Parameters.setBaseIndex(0);

      Zmat A = new Zmat(10,9);

      int bx = A.bx;
      int rx = A.rx;
      int cx = A.cx;

      for (int i=bx; i<=rx; i++){
         for (int j=bx; j<=cx; j++){
            A.put(i, j, new Z(i,j));
         }
      }

      Zmat B = Merge.o12(A.get(bx,rx,bx,5), A.get(bx,rx,6,cx));
      Print.o(Norm.fro(Minus.o(A,B)));

      B = Merge.o21(A.get(bx,5,bx,cx),
                    A.get(6,rx,bx,cx));
      Print.o(Norm.fro(Minus.o(A,B)));

      B = Merge.o22(A.get(bx,5,bx,5), A.get(bx,5,6,cx),
                    A.get(6,rx,bx,5), A.get(6,rx,6,cx));
      Print.o(Norm.fro(Minus.o(A,B)));

      B = Merge.o13
           (A.get(bx,rx,bx,3), A.get(bx,rx,4,6), A.get(bx,rx,7,cx));
      Print.o(Norm.fro(Minus.o(A,B)));

      B = Merge.o23
           (A.get(bx,5,bx,3), A.get(bx,5,4,6), A.get(bx,5,7,cx),
            A.get(6,rx,bx,3), A.get(6,rx,4,6), A.get(6,rx,7,cx));
      Print.o(Norm.fro(Minus.o(A,B)));

      B = Merge.o31(A.get(bx,3,bx,cx),
                    A.get(4,6,bx,cx),
                    A.get(7,rx,bx,cx));
      Print.o(Norm.fro(Minus.o(A,B)));

      B = Merge.o32(A.get(bx,3,bx,5), A.get(bx,3,6,cx),
                    A.get(4,6,bx,5), A.get(4,6,6,cx),
                    A.get(7,rx,bx,5), A.get(7,rx,6,cx));
      Print.o(Norm.fro(Minus.o(A,B)));

      B = Merge.o33
           (A.get(bx,3,bx,3), A.get(bx,3,4,6), A.get(bx,3,7,cx),
            A.get(4,5,bx,3), A.get(4,5,4,6), A.get(4,5,7,cx),
            A.get(6,rx,bx,3), A.get(6,rx,4,6), A.get(6,rx,7,cx));
      Print.o(Norm.fro(Minus.o(A,B)));

   }
}
