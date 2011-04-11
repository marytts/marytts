package Jampack;

class ZmatTest{

   public static void main(String[] args)
   throws JampackException{
      Z B[][] = new Z[4][3];
      for (int i=0; i<4; i++)
         for (int j=0; j<3; j++)
            B[i][j] = new Z(i,j);

//      Parameters.SetBaseIndex(0);

      if (args[0].equals("t1")){

         Zmat A = new Zmat(4,5);
         System.out.print(A.nrow + " ");
         System.out.print(A.ncol);
         Print.o(A);
      }

      else if (args[0].equals("t2")){
      
         Zmat A = new Zmat(B);
         System.out.print(A.nrow + " ");
         System.out.print(A.ncol);
         Print.o(A);
      }

      else if (args[0].equals("t3")){
         Zmat A = new Zmat(B);
         Zmat C = new Zmat(A);
         System.out.print(C.nrow + " ");
         System.out.print(C.ncol + " ");
         System.out.print(C.basex);
         Print.o(C);
      }

      else if (args[0].equals("t4")){
         Zmat A = new Zmat(B);
         Zmat C = new Zmat(A.re, A.im);
         System.out.print(C.nrow + " ");
         System.out.print(C.ncol + " ");
         System.out.print(C.basex);
         Print.o(C);
      }

      else if (args[0].equals("t5")){
         Zmat A = new Zmat(B);
         Zmat C = new Zmat(A.re);
         System.out.print(C.nrow + " ");
         System.out.print(C.ncol + " ");
         System.out.print(C.basex);
         Print.o(C);
      }

      else if (args[0].equals("t6")){
         Zmat A = new Zmat(B);
         double[][] C = A.getRe();
         double[][] D = A.getIm();
         System.out.print(A.nrow + " ");
         System.out.print(A.ncol + " ");
         Print.o(C);
         Print.o(D);
      }

      else if (args[0].equals("t7")){
         Zmat A = new Zmat(B);
         Z C[][] = A.getZ();
         System.out.print(A.nrow + " ");
         System.out.print(A.ncol);
         Print.o(C);
      }

      else if (args[0].equals("t8")){
         Zmat A = new Zmat(B);
         Z ell = new Z();
         ell.Eq(A.get(3,2));
         System.out.print(ell.re + " ");
         System.out.print(ell.im);
         System.out.print("\n");
         A.put(3, 2, Z.ZERO);
         ell.Eq(A.get(3,2));
         System.out.print(ell.re + " ");
         System.out.print(ell.im);
         System.out.print("\n");
         Print.o(A);
      }

      else if (args[0].equals("t9")){
         Zmat A = new Zmat(B);
         Zmat C = A.get(1,3,1,2);
         Print.o(C);
         C = new Zmat(3,2);
         A.put(1, 3, 1, 2, C);
         C = A.get(1, 3, 1, 2);
         Print.o(C);
         Print.o(A);
      }

      else if (args[0].equals("t10")){
         Zmat A = new Zmat(B);
         int[] ir = new int[] {A.rx, A.bx};
         Zmat C = A.get(ir,1,2);
         Print.o(C);
         C = new Zmat(2,2);
         A.put(ir, 1, 2, C);
         C = A.get(ir, 1, 2);
         Print.o(C);
         Print.o(A);
      }

      else if (args[0].equals("t11")){
         Zmat A = new Zmat(B);
         int[] jr = new int[] {A.cx, A.bx};
         Zmat C = A.get(1, 3, jr);
         Print.o(C);
         C = new Zmat(3,2);
         A.put(1, 3, jr, C);
         C = A.get(1, 3, jr);
         Print.o(C);
         Print.o(A);
      }

      else if (args[0].equals("t12")){
         Zmat A = new Zmat(B);
         int[] ir = new int[] {A.rx,A.bx};
         int[] jr = new int[] {A.cx,A.bx};
         Zmat C = A.get(ir, jr);
         Print.o(C);
         C = new Zmat(2,2);
         A.put(ir, jr, C);
         C = A.get(ir, jr);
         Print.o(C);
         Print.o(A);
      }
   }
}
