package Jampack;

class TimesTest{

   public static void main(String[] args)
   throws JampackException{

      if (args[0].equals("zmzm")){

         int nr = 3;
         int nc = 4;

         Zmat A = new Zmat(nr, nc);
         Zmat B = new Zmat(nc, nr);
         for (int i=0; i<nr; i++){
            for (int j=0; j<nc; j++){
               A.re[i][j] = i;
               A.im[i][j] = j;
               B.re[j][i] = i+j;
               B.re[j][i] = i-j;
            }
         }

         Print.o
            (Norm.fro(Minus.o(Times.o(A, B), H.o(Times.o(H.o(B),H.o(A))))));
      }

      if (args[0].equals("zmhzm")){

         int nr = 3;
         int nc = 4;

         Zmat A = new Zmat(nr, nc);
         Zmat B = new Zmat(nc, nr);
         for (int i=0; i<nr; i++){
            for (int j=0; j<nc; j++){
               A.re[i][j] = i;
               A.im[i][j] = j;
            }
         }

         Print.o
            (Norm.fro(Minus.o(Times.aha(A), Times.o(H.o(A),A))));
      }

      if (args[0].equals("zmzmh")){

         int nr = 3;
         int nc = 4;

         Zmat A = new Zmat(nr, nc);
         Zmat B = new Zmat(nc, nr);
         for (int i=0; i<nr; i++){
            for (int j=0; j<nc; j++){
               A.re[i][j] = i;
               A.im[i][j] = j;
            }
         }

         Print.o
            (Norm.fro(Minus.o(Times.aah(A), Times.o(A,H.o(A)))));
      }

      else if (args[0].equals("zdmzm")){

         int nr = 3;
         int nc = 4;

         Zdiagmat D = new Zdiagmat(nr);
         Zmat A = new Zmat(nr, nc);
         for (int i=0; i<nr; i++){
            for (int j=0; j<nc; j++){
               A.re[i][j] = i+j;
               A.im[i][j] = j;
            }
            D.re[i] = i;
            D.re[i] = i;
         }

         Print.o
            (Norm.fro(Minus.o(Times.o(D, A), H.o(Times.o(H.o(A),H.o(D))))));
      }
      else if (args[0].equals("zdmzdm")){

         int n = 4;

         Zdiagmat D = new Zdiagmat(n);
         for (int i=0; i<n; i++){
            D.re[i] = i;
            D.im[i] = i+5;
         }
         Print.o(Norm.fro(Minus.o(Times.o(D, H.o(D)), Times.o(H.o(D), D))));
      }
   }
}

