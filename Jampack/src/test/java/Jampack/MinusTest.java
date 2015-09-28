package Jampack;

class MinusTest{

   public static void main(String[] args)
   throws JampackException{

      if (args[0].equals("zmzm")){

         int nr = 4;
         int nc = 3;

         Zmat A = new Zmat(nr,nc);
         Zmat B = new Zmat(nr,nc);
         for (int i=0; i<nr; i++){
            for (int j=0; j<nc; j++){
               A.re[i][j] = i;
               B.re[i][j] = i;
               A.im[i][j] = j;
               B.im[i][j] = j;
            }
         }
         Print.o(Minus.o(A,B));
         Print.o(Minus.o(A));
      }
   
      else if (args[0].equals("zmzdm")){

         int n = 3;

         Zmat A = new Zmat(n,n);
         Zdiagmat D = new Zdiagmat(n);
         for (int i=0; i<n; i++){
            for (int j=0; j<n; j++){
               A.re[i][j] = i;
               A.im[i][j] = j;
            }
            D.re[i] = i;
            D.im[i] = i;
         }
         Print.o(Minus.o(A,D));
         Print.o(Minus.o(D,A));
      }

      else if (args[0].equals("zdmzdm")){

         int n = 3;

         Zdiagmat D1 = new Zdiagmat(n);
         Zdiagmat D2 = new Zdiagmat(n);
         for (int i=0; i<n; i++){
            D1.re[i] = i;
            D1.im[i] = i;
            D2.re[i] = i;
            D2.im[i] = i;
         }
         Print.o(Minus.o(D1,D2));
         Print.o(Minus.o(D1));
      }

   }

}
