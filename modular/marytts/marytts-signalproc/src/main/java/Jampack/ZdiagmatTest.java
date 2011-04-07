package Jampack;

class ZdiagmatTest{

   public static void main(String[] args)
   throws JampackException{

//      Parameters.SetBaseIndex(0);

      if (args[0].equals("t1")){
         Print.o(new Zdiagmat(5));
      }

      else if (args[0].equals("t2")){
         Print.o(new Zdiagmat(3, new Z(3,-1)));
      }

      else if (args[0].equals("t3")){
         Z1 val = new Z1(3);
         for (int i=0; i<3; i++){
            val.re[i] = i;
            val.im[i] = 1;
         }
         Print.o(new Zdiagmat(val));
      }
      else if (args[0].equals("t4")){
         Zmat A = new Zmat(3,3);
         for (int i=A.bx; i<=A.rx; i++){
            for (int j=A.bx; j<=A.cx; j++){
               A.put(i, j, new Z(i,j));
            }
         }
         Print.o(new Zdiagmat(A));
         Print.o(new Zdiagmat(A,1));
         Print.o(new Zdiagmat(A,2));
         Print.o(new Zdiagmat(A,-1));
         Print.o(new Zdiagmat(A,-2));
      }
      else if (args[0].equals("t5")){
         Z1 val = new Z1(3);
         for (int i=0; i<3; i++){
            val.re[i] = i;
            val.im[i] = -i;
         }
         Print.o(new Zdiagmat(new Zdiagmat(val)));
      }
      else if (args[0].equals("t6")){
         Z1 val = new Z1(3);
         for (int i=0; i<3; i++){
            val.re[i] = i;
            val.im[i] = 1;
         }
         Zdiagmat D = new Zdiagmat(val);
         D.put(D.dx, D.get(D.bx));
         Print.o(D);
      }

   }
}
