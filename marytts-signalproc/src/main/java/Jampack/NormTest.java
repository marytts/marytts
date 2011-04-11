package Jampack;

class NormTest{

   public static void main(String[] args)
   throws JampackException
   {
      double nrm, ss;

      Z Ary[][] = new Z[3][2];
      for (int i=0; i<3; i++)
         for (int j=0; j<2; j++)
            Ary[i][j] = new Z(i,j);

      Zmat A = new Zmat(Ary);
      nrm = Norm.fro(A);
      A = Times.o(H.o(A), A);
      ss = 0.0;
      for (int i=0; i<A.nc; i++)
         ss = ss + A.re[i][i];
      Print.o(nrm*nrm);
      Print.o(ss);
   }
}
