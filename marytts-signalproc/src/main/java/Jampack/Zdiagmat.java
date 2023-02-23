package Jampack;

/**
   Zdiagmat is a storage efficient representation of a complex
   diagonal matrix.

   @version Pre-alpha
   @author G. W. Stewart
*/

public class Zdiagmat{

/** The order of the matrix */
    protected int order;

/** The base index */
    protected int  basex;

/** The real part of the diagonal */
    protected double re[];

/** The imaginary part of the diagonal */
    protected double im[];

/** The order of the matrix (public) */
    public int n;

/** The base index (public) */
    public int bx;

/** The  index of the last diagonal (public) */
    public int dx;






/**
   Constructs a Zdiagmat and initializes it to zero.

   @param    order The order of the new Zdiagmat
   @return   A Zdiagmat initialized to zero.    
*/

   public Zdiagmat(int order){
      Parameters.BaseIndexNotChangeable = true;
      basex = Parameters.BaseIndex;
      this.order = order;
      getProperties();
      re = new double[n];
      im = new double[n];
   }

/**
   Constructs a Zdiagmat and initializes it to a constant.

   @param     order The order of the new Zdiagmat
   @param     val   The value to which the diagonal
                    is to be initialized
   @return    A Zdiagmat whose diagonal is val.
*/

   public Zdiagmat(int order, Z val){
      Parameters.BaseIndexNotChangeable = true;
      basex = Parameters.BaseIndex;
      this.order = order;
      getProperties();
      re = new double[n];
      im = new double[n];
      for (int i=0; i<n; i++){
        re[i] = val.re;
        im[i] = val.im;
      }
   }

/**
   Constructs a Zdiagmat and initializes it to a Z1.

   @param     val A Z1
   @return    A Zdiagmat whose diagonal elements are
              the elements of val.
*/

   public Zdiagmat(Z1 val){
      Parameters.BaseIndexNotChangeable = true;
      bx = Parameters.BaseIndex;
      order = val.re.length;
      getProperties();
      re = new double[n];
      im = new double[n];
      for (int i=0; i<n; i++){
        re[i] = val.re[i];
        im[i] = val.im[i];
      }
   }

/**
   Constructs a Zdiagmat and initializes it to the diagonal of a Zmat.

   @param     A The Zmat
   @param     k The diagonal.  For k=0 gives the princpal diagonal;
                k>0, the kth superdiagonal; k<0, the kth subdiagonal.
   @return    The Zdiagmat consisting of the selected diagonal of A
   @exception JampackException
              Thrown for k to large or small.
*/

   public Zdiagmat(Zmat A, int k)
   throws JampackException{
      Parameters.BaseIndexNotChangeable = true;
      basex = Parameters.BaseIndex;
      if (k >= 0){
         if (k >= A.ncol){
            throw new JampackException
               ("Diagonal out of range.");
         }
         order = Math.min(A.nrow, A.ncol-k);
         re = new double[order];
         im = new double[order];
         for (int i=0; i<order; i++){
            re[i] = A.re[i][i+k];
            im[i] = A.im[i][i+k];
         }
      }
      else{
         k = -k;
         if (k >= A.nrow){
            throw new JampackException
               ("Diagonal out of range.");
         }
         order = Math.min(A.nrow-k, A.ncol);
         re = new double[order];
         im = new double[order];
         for (int i=0; i<order; i++){
            re[i] = A.re[i+k][i];
            im[i] = A.im[i+k][i];
         }
      }
      getProperties();
   }
/**
   Constructs a Zdiagmat and initializes it to the principal diagonal
   of a Zmat.

   @param     A A Zmat
   @returns   A Zdiagmat whose diagonal is that of A
   @exception JampackException
              Passed from below.
*/

   public Zdiagmat(Zmat A)
   throws JampackException{
      this(A, 0);
   }

/**
   Constructs a Zdiagmat and initializes it to another Zdiagmat.

   @param     D A Zdiagmat
   @returns   A Zdiagmat that is a copy of D.
*/

   public Zdiagmat(Zdiagmat D){

      Parameters.BaseIndexNotChangeable = true;
      basex = Parameters.BaseIndex;
      order = D.order;
      getProperties();
      re = new double[n];
      im = new double[n];

      for (int i=0; i<n; i++){
         re[i] = D.re[i];
         im[i] = D.im[i];
      }
   }

/**
   Sets the public parameters.
*/

   public void getProperties(){
      bx = basex;
      dx = bx + order - 1;
      n = order;
   }


/**
   Gets the ii-th diagonal element of a Zdiagmat.

   @param     ii An integer
   @return    The ii-th element of this Zdiagmat
*/

   public Z get(int ii){

      return new Z(re[ii-bx], im[ii-bx]);
   }

/**
   Gets the <tt>i</tt>th diagonal of a of a Zdiagmat
   (0-based).
*/

   public Z get0(int i){

      return new Z(re[i], im[i]);
   }


/**
   Writes the ii-th diagonal element of a Zdiagmat.

   @param     ii  An integer
   @param     val A Z
   @return    Resets the ii-th diagonal element of this Zdiagmat
              to val.
*/

   public void put(int ii, Z val){

      re[ii-bx] = val.re;
      im[ii-bx] = val.im;
   }

/**
   Writes the <tt>i</tt>th diagonal element  of a Zdiagmat
   (0-based).
*/

   public void put0(int i, Z val){

      re[i] = val.re;
      im[i] = val.im;
   }

}

