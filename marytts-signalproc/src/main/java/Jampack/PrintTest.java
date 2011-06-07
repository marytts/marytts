package Jampack;

class PrintTest
{

   public static void main(String[] args)
   throws JampackException{

      int itest = -5;
      Print.o(itest, 10);

      int ia[] = {1, 2, -3, 4, 5, 6};
      Print.o(ia, 20);

      Print.o(Math.PI, 15, 7);

      double a[] = {1, 3, -3, 4, Math.PI, Math.E};
      Print.o(a, 15, 7);

      double A[][]= {{1, 3, -3, 4, Math.PI, Math.E},
                    {1, 3, -3, 4, Math.PI, Math.E},
                    {1, 3, -3, 4, Math.PI, Math.E}};
      Print.o(A, 15, 7);

      Print.o(new Z(3, -7), 15, 7);

      Z c[] = new Z[5];
      for (int i=0; i<5; i++)
         c[i] = new Z(i,i+1);
      Print.o(c, 15, 7);


      Z C[][] = new Z[3][5];
      for (int i=0; i<3; i++)
         for (int j=0; j<5; j++)
            C[i][j] = new Z(i,j);
      Print.o(C, 15, 7);

      Z1 z1 = new Z1(5);
      for (int i=0; i<5; i++){
         z1.re[i] = i; z1.im[i]=-i;
      }
      Print.o(z1, 15, 7);

      Zmat Z = new Zmat(C);
      Print.o(Z, 15, 7);

      Zdiagmat D = new Zdiagmat(z1);
      Print.o(D, 15, 7);

   }

}