package Jampack;

class HouseTest{

   public static void main(String[] args)
   throws JampackException{

      Parameters.setBaseIndex(0);

      if (args[0].equals("genc") || args[0].equals("ua")){

         int i, j, n=5;

         Z Aa[][] = new Z[n][n];
         for (i=0; i<n; i++){
            for (j=0; j<n; j++)
                  Aa[i][j] = new Z(i+1,j+1);
            Aa[i][i] = new Z(1,1);
         }

         Zmat A = new Zmat(Aa);
         Zmat AA = new Zmat(A);

         Zmat A2 = A.get(A.bx, A.rx, 2, 2);
         Z1 u = House.genc(A, A.bx, A.rx, 2);
         Zmat uu = new Zmat(n, 1);
         for (i=uu.bx; i<=uu.rx; i++)
            uu.put(i, uu.bx, u.get(i-uu.bx));
         Zmat U = Minus.o(Eye.o(n), Times.o(uu, H.o(uu)));
         Print.o(Norm.fro(Minus.o(Eye.o(n), Times.o(U,U))));
         Print.o(Norm.fro(Minus.o(A.get(A.bx, A.rx, 2, 2),Times.o(U, A2))));
         if (args[0].equals("ua")){
            Zmat B = Times.o(U, AA);
            Zmat C = House.ua(u, AA, AA.bx, AA.rx, AA.bx, AA.rx);
            Print.o(Norm.fro(Minus.o(B, C)));
         }
      }

      else if (args[0].equals("genr") || args[0].equals("au")){

         int i, j, n=5;

         Z Aa[][] = new Z[n][n];
         for (i=0; i<n; i++){
            for (j=0; j<n; j++)
                  Aa[i][j] = new Z(i+1,j+1);
            Aa[i][i] = new Z(1,1);
         }

         Zmat A = new Zmat(Aa);
         Zmat AA = new Zmat(A);

         Zmat A2 = A.get(2,2,A.bx,A.cx);
         Z1 u = House.genr(A, 2, A.bx, A.cx);
         Zmat uu = new Zmat(n, 1);
         for (i=uu.bx; i<=uu.rx; i++)
            uu.put(i, uu.bx, u.get(i-uu.bx));
         Zmat U = Minus.o(Eye.o(n), Times.o(uu, H.o(uu)));
         Print.o(Norm.fro(Minus.o(Eye.o(n), Times.o(U,U))));
         Print.o(Norm.fro(Minus.o(A.get(2, 2, A.bx, A.cx),Times.o(A2,U))));
         if (args[0].equals("au")){
            Zmat B = Times.o(AA,U);
            Zmat C = House.au(AA, u, A.bx, A.rx, A.bx, A.cx);
            Print.o(Norm.fro(Minus.o(B, C)));
         }
      }
   }

}