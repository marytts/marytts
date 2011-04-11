package Jampack;


class ZhqrdTest{

   public static void main(String[] args)
   throws JampackException{

      Zmat v = new Zmat(4,1);
      for (int i=0; i<4; i++){
         v.re[i][0] = 1;
         v.im[i][0] = i;
      }
      Z1 u = House.genc(v, v.bx, v.rx, v.bx);
      Zmat uu = new Zmat(u);
      Zmat A = new Zmat(Minus.o(Eye.o(4), Times.o(uu, H.o(uu))));
      Zmat B = A.get(A.bx, A.rx-1, A.bx, A.cx);
      Zhqrd QR = new Zhqrd(B);
      Print.o(QR.R);
      Print.o(Norm.fro(Minus.o(Times.o(H.o(B), B), Times.o(H.o(QR.R),QR.R))));
      Print.o(Norm.fro(QR.R), 20, 16);
      QR = new Zhqrd(A);
      Print.o(Norm.fro(Minus.o(A, QR.qb(QR.R))));
      Print.o(Norm.fro(Minus.o(QR.R, QR.qhb(A))));
   }
}
