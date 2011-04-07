package Jampack;

import java.io.InputStreamReader;
import java.io.StreamTokenizer;

class Ztest{

   public static void main(String[] args)
   throws JampackException
   {
      Z z1 = new Z();
      System.out.println(z1.re);     //   0
      System.out.println(z1.im);     //   0
      z1 = new Z(1,-1);
      System.out.println(z1.re);     //   1
      System.out.println(z1.im);     //  -1
      z1 = new Z(z1);
      System.out.println(z1.re);     //   1
      System.out.println(z1.im);     //  -1
      System.out.println(Z.abs(z1));  //   sqrt(2)
      Z z2 = new Z(2,-5);
      z2.Eq(z1.Plus(z1,z2));
      System.out.println(z1.re);     //   3
      System.out.println(z1.im);     //  -6
      System.out.println(z2.re);     //   3
      System.out.println(z2.im);     //  -6
      z2.Minus(z1,z2.Eq(1,-1));
      System.out.println(z2.re);     //   2
      System.out.println(z2.im);     //  -5
      z2.Minus(z2);
      System.out.println(z2.re);     //  -2
      System.out.println(z2.im);     //   5
      Z z3 = new Z();
      z3.Times(z1,z2.Conj(z1));
      System.out.println(z3.re);     //  45
      System.out.println(z3.im);     //   0
      z1.Eq(2,1);
      z2.Eq(1,-1);
      z1.Times(z1,z2);
      z3.Div(z1,z2);
      System.out.println(z3.re);     //   2
      System.out.println(z3.im);     //   1
      z1.Eq(1, -2);
      z1.Times(z1,z1);
      z1.Sqrt(z1);
      System.out.println(z1.re);     //   1
      System.out.println(z1.im);     //  -2
      z1.Eq(-2.1, 1.2e-9);
      z1.Times(z1,z1);
      z1.Sqrt(z1);
      Print.o(z1.re, 22, 15);
      Print.o(z1.im, 22,15);
      z1.Eq(-1,0);
      z1.Sqrt(z1);
      System.out.println(z1.re);     //   0
      System.out.println(z1.im);     //   1

   }
}
