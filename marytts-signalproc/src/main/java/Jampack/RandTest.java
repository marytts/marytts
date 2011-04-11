package Jampack;

class RandTest{

   public static void main(String[] args)
   throws JampackException{

      Print.o(Rand.uzmat(5,3));
      Rand.setSeed(69);
      Print.o(Rand.uzmat(5,3));

      Print.o(Rand.nzmat(5,3));
      Rand.setSeed(69);
      Print.o(Rand.nzmat(5,3));

   }
}