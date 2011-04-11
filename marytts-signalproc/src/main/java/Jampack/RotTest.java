package Jampack;

class RotTest{

   public static void main(String[] args){


      Zmat A = new Zmat(2, 1);
      A.put(1,1, new Z(-1,2));
      A.put(2,1, new Z(2,-1));
      Zmat B = new Zmat(A);
      Zmat C = H.o(A);
      Zmat D = new Zmat(C);
      Rot P = Rot.genc(A, 1, 2, 1);
      Rot.pa(P, B, 1, 2, 1, 1);
      Print.o(A);
      Print.o(B);
      Rot.pha(P,  B, 1, 2, 1, 1);
      Print.o(B);
      Rot.pha(P, B, 1, 2, 1, 1);
      Rot.pa(P, B, 1, 2, 1, 1);
      Print.o(B);

      Rot Q = Rot.genr(C, 1, 1, 2);
      Rot.ap(D, Q, 1, 1, 1, 2);
      Print.o(C);
      Print.o(D);
      Rot.aph(D, Q, 1, 1, 1, 2);
      Print.o(D);
      Rot.aph(D, Q, 1, 1, 1, 2);
      Rot.ap(D, Q, 1, 1, 1, 2);
      Print.o(D);
   }
}
