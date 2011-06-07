package Jampack;

class SolveTest{

   public static void main(String[] args)
   throws JampackException{

//      Parameters.SetBaseIndex(0);

      if (args[0].equals("lib")){

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

         Z Xa[][] = new Z[n][2];

         for (i=0; i<n; i++){
            Xa[i][0] = new Z(1,1);
            Xa[i][1] = new Z(i,i);
         }

         Zmat X = new Zmat(Xa);

         Zmat B = Times.o(L,X);

         Print.o(Norm.fro(Minus.o(X, Solve.aib(L, B))));
      }


      else if (args[0].equals("lhib")){

         int i, j, n=4;

         Z La[][] = new Z[n][n];
         for (i=0; i<n; i++){
            for (j=0; j<n; j++)
               if (j <= i)
                  La[i][j] = new Z(i+1,j+1);
               else
                  La[i][j] = new Z(0,0);
      }

         Zltmat L = new Zltmat(La);

         Z Xa[][] = new Z[n][2];

         for (i=0; i<n; i++){
            Xa[i][0] = new Z(1,1);
            Xa[i][1] = new Z(i,i);
         }

         Zmat X = new Zmat(Xa);

         Zmat B = Times.o(H.o(L), X);

         Print.o(Norm.fro(Minus.o(X, Solve.ahib(L, B))));
      }

      else if (args[0].equals("bli")){

         int i, j, n=4;

         Z La[][] = new Z[n][n];
         for (i=0; i<n; i++){
            for (j=0; j<n; j++)
               if (j <= i)
                  La[i][j] = new Z(i+1,j+1);
               else
                  La[i][j] = new Z(0,0);
      }

         Zltmat L = new Zltmat(La);

         Z Xa[][] = new Z[2][n];
         for (j=0; j<n; j++){
            Xa[0][j] = new Z(1,1);
            Xa[1][j] = new Z(j,j+1);
         }


         Zmat X = new Zmat(Xa);

         Zmat B = Times.o(X, L);

         Print.o(Norm.fro(Minus.o(X, Solve.bai(B, L))));
      }

      else if (args[0].equals("blhi")){

         int i, j, n=4;

         Z La[][] = new Z[n][n];
         for (i=0; i<n; i++){
            for (j=0; j<n; j++)
               if (j <= i)
                  La[i][j] = new Z(i+1,j+1);
               else
                  La[i][j] = new Z(0,0);
      }

         Zltmat L = new Zltmat(La);

         Z Xa[][] = new Z[2][n];
         for (j=0; j<n; j++){
            Xa[0][j] = new Z(1,1);
            Xa[1][j] = new Z(j,j+1);
         }


         Zmat X = new Zmat(Xa);

         Zmat B = Times.o(X, H.o(L));

         Print.o(Norm.fro(Minus.o(X, Solve.bahi(B, L))));
      }

      else if (args[0].equals("uib")){

         int i, j, n=3;

         Z Ua[][] = new Z[n][n];
         for (i=0; i<n; i++){
            for (j=0; j<n; j++)
               if (j >= i)
                  Ua[i][j] = new Z(i+1,j+1);
               else
                  Ua[i][j] = new Z(0,0);
      }

         Zutmat U = new Zutmat(Ua);

         Z Xa[][] = new Z[n][2];

         for (i=0; i<n; i++){
            Xa[i][0] = new Z(1,1);
            Xa[i][1] = new Z(i,i);
         }

         Zmat X = new Zmat(Xa);

         Zmat B = Times.o(U,X);
         Print.o(Norm.fro(Minus.o(X, Solve.aib(U, B))));
      }

      else if (args[0].equals("uhib")){

         int i, j, n=3;

         Z Ua[][] = new Z[n][n];
         for (i=0; i<n; i++){
            for (j=0; j<n; j++)
               if (j >= i)
                  Ua[i][j] = new Z(i+1,j+1);
               else
                  Ua[i][j] = new Z(0,0);
      }

         Zutmat U = new Zutmat(Ua);

         Z Xa[][] = new Z[n][2];

         for (i=0; i<n; i++){
            Xa[i][0] = new Z(1,1);
            Xa[i][1] = new Z(i,i);
         }

         Zmat X = new Zmat(Xa);

         Zmat B = Times.o(H.o(U), X);
         Print.o(Norm.fro(Minus.o(X, Solve.ahib(U, B))));
      }

      else if (args[0].equals("bui")){

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

         Z Xa[][] = new Z[2][n];
         for (j=0; j<n; j++){
            Xa[0][j] = new Z(1,1);
            Xa[1][j] = new Z(j,j+1);
         }


         Zmat X = new Zmat(Xa);

         Zmat B = Times.o(X, U);

         Print.o(Norm.fro(Minus.o(X, Solve.bai(B, U))));
      }


      else if (args[0].equals("buhi")){

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

         Z Xa[][] = new Z[2][n];
         for (j=0; j<n; j++){
            Xa[0][j] = new Z(1,1);
            Xa[1][j] = new Z(j,j+1);
         }


         Zmat X = new Zmat(Xa);

         Zmat B = Times.o(X, H.o(U));

         Print.o(Norm.fro(Minus.o(X, Solve.bahi(B, U))));
      }


      else if (args[0].equals("aib")){
         int i, j, n=5;

         Z Aa[][] = new Z[n][n];
         for (i=0; i<n; i++){
            for (j=0; j<n; j++)
                  Aa[i][j] = new Z(i+1,j+1);
            Aa[i][i] = new Z(1,1);
         }

         Zmat A = new Zmat(Aa);

         Z Xa[][] = new Z[n][2];

         for (i=0; i<n; i++){
            Xa[i][0] = new Z(1,1);
            Xa[i][1] = new Z(i,i);
         }

         Zmat X = new Zmat(Xa);

         Zmat B = Times.o(A,X);
         Print.o(Norm.fro(Minus.o(X, Solve.aib(A, B))));
      }


      else if (args[0].equals("ahib")){
         int i, j, n=10;

         Z Aa[][] = new Z[n][n];
         for (i=0; i<n; i++){
            for (j=0; j<n; j++)
                  Aa[i][j] = new Z(i+1,j+1);
            Aa[i][i] = new Z(1,1);
         }

         Zmat A = new Zmat(Aa);

         Z Xa[][] = new Z[n][2];

         for (i=0; i<n; i++){
            Xa[i][0] = new Z(1,1);
            Xa[i][1] = new Z(i,i);
         }

         Zmat X = new Zmat(Xa);
         Zmat B = Times.o(H.o(A), X);
         Print.o(Norm.fro(Minus.o(X, Solve.ahib(A, B))));
      }

      else if (args[0].equals("bai")){
         int i, j, n=10;

         Z Aa[][] = new Z[n][n];
         for (i=0; i<n; i++){
            for (j=0; j<n; j++)
                  Aa[i][j] = new Z(i+1,j+1);
            Aa[i][i] = new Z(1,1);
         }

         Zmat A = new Zmat(Aa);

         Z Xa[][] = new Z[2][n];

         for (j=0; j<n; j++){
            Xa[0][j] = new Z(1,1);
            Xa[1][j] = new Z(i,i);
         }

         Zmat X = new Zmat(Xa);
         Zmat B = Times.o(X, A);
         Print.o(Norm.fro(Minus.o(X, Solve.bai(B, A))));
      }

      else if (args[0].equals("bahi")){
         int i, j, n=10;

         Z Aa[][] = new Z[n][n];
         for (i=0; i<n; i++){
            for (j=0; j<n; j++)
                  Aa[i][j] = new Z(i+1,j+1);
            Aa[i][i] = new Z(1,1);
         }

         Zmat A = new Zmat(Aa);

         Z Xa[][] = new Z[2][n];

         for (j=0; j<n; j++){
            Xa[0][j] = new Z(1,1);
            Xa[1][j] = new Z(i,i);
         }

         Zmat X = new Zmat(Xa);
         Zmat B = Times.o(X, H.o(A));
         Print.o(Norm.fro(Minus.o(X, Solve.bahi(B, A))));
      }

      else if (args[0].equals("pdaib")){
      int i, j, n=5;

         Zpsdmat A = new Zpsdmat(n,n);
         for (i=0; i<n; i++){
            for (j=0; j<n; j++){
               A.re[i][j] = i;
               A.im[i][j] = i+j;
            }
            A.re[i][i] = 2*n;
            A.im[i][i] = 2*n;
         }
         A = new Zpsdmat(Times.o(H.o(A), A));

         Z Xa[][] = new Z[n][2];

         for (i=0; i<n; i++){
            Xa[i][0] = new Z(1,1);
            Xa[i][1] = new Z(i,i);
         }

         Zmat X = new Zmat(Xa);
         Zmat B = Times.o(A, X);
         Print.o(Norm.fro(Minus.o(X, Solve.aib(A, B))));
      }
   
      else if (args[0].equals("pdbai")){
      int i, j, n=5;

         Zpsdmat A = new Zpsdmat(n,n);
         for (i=0; i<n; i++){
            for (j=0; j<n; j++){
               A.re[i][j] = i;
               A.im[i][j] = i+j;
            }
            A.re[i][i] = 2*n;
            A.im[i][i] = 2*n;
         }
         A = new Zpsdmat(Times.o(H.o(A), A));

         Z Xa[][] = new Z[2][n];

         for (j=0; j<n; j++){
            Xa[0][j] = new Z(1,1);
            Xa[1][j] = new Z(i,i);
         }

         Zmat X = new Zmat(Xa);
         Zmat B = Times.o(X, A);
         Print.o(Norm.fro(Minus.o(X, Solve.bai(B, A))));
      }
   }

}




