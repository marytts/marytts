package Jampack;

class InvTest{

   public static void main(String[] args)
   throws JampackException{

//   Parameters.setBaseIndex(0);

      if (args[0].equals("zli")){

         int i, j, n=3;

         Z La[][] = new Z[n][n];
         for (i=0; i<n; i++){
            for (j=0; j<n; j++)
               if (j <= i)
                  La[i][j] = new Z(i+1,j+1);
               else
                  La[i][j] = new Z(0,0);
         }

         Zltmat L = new Zltmat(La);

         Zltmat Li = Inv.o(L);
         Print.o(Li);
         Print.o(Norm.fro(Minus.o(Eye.o(L.nr),Times.o(L,Li))));
      }
      if (args[0].equals("zui")){

         int i, j, n=4;

         Z Ua[][] = new Z[n][n];
         for (i=0; i<n; i++){
            for (j=0; j<n; j++)
               if (j >= i)
                  Ua[i][j] = new Z(i+1,j+1);
               else
                  Ua[i][j] = new Z(0,0);
         }

         Zutmat U = new Zutmat(Ua);

         Zutmat Ui = Inv.o(U);

         Print.o(Ui);
         Print.o(Norm.fro(Minus.o(Eye.o(U.nr),Times.o(U,Ui))));
      }

      else if (args[0].equals("zai")){
         int i, j, n=5;

         Z Aa[][] = new Z[n][n];
         for (i=0; i<n; i++){
            for (j=0; j<n; j++)
                  Aa[i][j] = new Z(i+1,j+1);
            Aa[i][i] = new Z(.1,.1);
         }

         Zmat A = new Zmat(Aa);

         Zmat Ai = Inv.o(A);

         Print.o(Ai);
         Print.o(Norm.fro(Minus.o(Eye.o(A.nr),Times.o(A,Ai))));

      }
      else if (args[0].equals("zpdai")){
         int i, j, n=5;

         Z Aa[][] = new Z[n][n];
         for (i=0; i<n; i++){
            for (j=0; j<n; j++)
                  Aa[i][j] = new Z(i+1,j+1);
            Aa[i][i] = new Z(.1,.1);
         }

         Zmat B = new Zmat(Aa);
         Zpsdmat A = new Zpsdmat(Times.aha(B));
         Zpsdmat Ai = Inv.o(A);

         Print.o(Ai);
         Print.o(Norm.fro(Minus.o(Eye.o(A.nr),Times.o(A,Ai))));

      }

      else if (args[0].equals("zdi")){

         int n=3;

         Zdiagmat D = new Zdiagmat(n);

         for (int i=0; i<n; i++){
            D.re[i] = i;
            D.im[i] = i+3;
         }

         Zdiagmat Di = Inv.o(D);
         Print.o(Times.o(Di, D));
      }
   }
}
