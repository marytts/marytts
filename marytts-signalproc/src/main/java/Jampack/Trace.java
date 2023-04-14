package Jampack;

class Trace{

   public static Z o(Zmat A){

      if (A.nc != A.nr){
         throw new RuntimeException
            ("Nonsquare matrix");
      }

      Z t = new Z();

      for (int i=0; i<A.nr; i++){
         t.re = t.re + A.re[i][i];
         t.im = t.im + A.im[i][i];
      }

      return t;
   }

   public static Z o(Zdiagmat D){

      Z t = new Z();

      for (int i=0; i<D.order; i++){
         t.re = t.re + D.re[i];
         t.im = t.im + D.im[i];
      }

      return t;
   }

}
