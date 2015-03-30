/*************************************************************************/
/*                                                                       */
/*                     Carnegie Mellon University                        */
/*                         Copyright (c) 2005                            */
/*                        All Rights Reserved.                           */
/*                                                                       */
/*  Permission is hereby granted, free of charge, to use and distribute  */
/*  this software and its documentation without restriction, including   */
/*  without limitation the rights to use, copy, modify, merge, publish,  */
/*  distribute, sublicense, and/or sell copies of this work, and to      */
/*  permit persons to whom this work is furnished to do so, subject to   */
/*  the following conditions:                                            */
/*   1. The code must retain the above copyright notice, this list of    */
/*      conditions and the following disclaimer.                         */
/*   2. Any modifications must be clearly marked as such.                */
/*   3. Original authors' names are not deleted.                         */
/*   4. The authors' names are not used to endorse or promote products   */
/*      derived from this software without specific prior written        */
/*      permission.                                                      */
/*                                                                       */
/*  CARNEGIE MELLON UNIVERSITY AND THE CONTRIBUTORS TO THIS WORK         */
/*  DISCLAIM ALL WARRANTIES WITH REGARD TO THIS SOFTWARE, INCLUDING      */
/*  ALL IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS, IN NO EVENT   */
/*  SHALL CARNEGIE MELLON UNIVERSITY NOR THE CONTRIBUTORS BE LIABLE      */
/*  FOR ANY SPECIAL, INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES    */
/*  WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN   */
/*  AN ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS ACTION,          */
/*  ARISING OUT OF OR IN CONNECTION WITH THE USE OR PERFORMANCE OF       */
/*  THIS SOFTWARE.                                                       */
/*                                                                       */
/*************************************************************************/
/*                                                                       */
/*            Authors: Kishore Prahallad                                 */
/*            Email:   skishore@cs.cmu.edu                               */
/*            Date Created: 05/25/2005                                   */
/*            Last Modified: 05/25/2005                                  */
/*            Purpose: A state class for HMM implementation              */
/*                                                                       */
/*************************************************************************/

#ifndef include
 #define include
 #include "header.h"
#endif

class stC {
  
  private: 	
    
    //Gaussian parameters.....	  
    int   ng; 		//no. gaussians
    double *pr;  	//priors

    //Emission parameters.....
    double **mn; 	//mean
    double **vr; 	//variance
    double **tsx; 	//Sum(X) 
    double **tsxx; 	//Sum(X*X)
    double *gauD;  	//Gaussian denominator
    double stD;  	//state denominator (= sum(g) gauD[g])
   	  
    //Transition parameters.....
    int   nc;  		//no. of connections [0-5]
    double *trp;	//tran. prob. - trn[nc]
    double *trx; 	//Accumulators for trp..
    int conBS;		//Connection begining state...

    //Feature parameters....
    int   dim;		//Dim of feat vectors..

    //Indicator Variables
    int   begS; 	//Begin state = word number else - 1
    int   endS;		//End state  = word number else -1;
    int   wid;		//word id
    int   sid;		//state id
    int   nullF; 	//Null Flag, indicating whether the state is null or not.

    //Temporary Array;
    double *comp; 	//comp[ng], use to store component prob..

    //Temporary hold parameters
    double cchP; //Cached Probability.
    int cchT;    //Cached TimeStamp..
     
  public:

     stC(); 
    //Allocates memory for all the arrays....
    //The last flag indicates whether accumulators need to be initialized or not.
    //		- useful while decoding....
    void Create(int, int, int, int, int, int, int, int, int, int);

    //Initialize the mean and variance values - random initialization..
    void RandomInit(int);

    //Give out the probability for a given vector....
    double GauProb(double*);
    double ComProb(double*, double*);

    //Nullify Accumulator Variables
    void NullifyAccum();

    //ReEstimate the Values;
    void ReEstimate();

    //Del Arrays...
    void FreeStorage(int);

    int getng();
    int getdim();
    int getnc();
    double trans(int);
    int getwid();
    double getstD();
    int getbegS();
    int getendS();
    int getconBS();
    void resetcchT(); //Reset the cached time stamp, to minimize time stamping conflicts..
    int getnullF();
    
    void AccumFeat(double*, double, int);
    //void AccumTran(double**, int, int);
    void AccumTran(double*, int);

    void StoreModel(char*);
    void ReadModel(ifstream&);
    void ReadForTrain(ifstream&);
    void CopyParam(int, int, int, double*, double*, double**, double**);
    void LoadParam(int&, int&, int&, double*&, double*&, double**&, double**&);

};

void stC::resetcchT() {
   cchT = -1;
   cchP = 0;
}

int stC::getconBS() {
   return conBS;
}

int stC::getbegS() { 
  return begS;
} 

int stC::getendS() { 
  return endS;
} 

int stC::getnullF() {
  return nullF;	
}

void stC::CopyParam(int ong, int odim, int onc, double *otrp, double *opr, double **omn, double **ovr) {

  if (ng > ong) {
     //Raise a Flag...
     cout<<"Error copying the parameters...."<<endl;
     cout<<"required Gaussians "<<ng<<" exceeed available: "<<ong<<endl;
     exit(1);
  }else if (nc > onc) {
    cout<<"Error copying the parameters...."<<endl;
    cout<<"required connections "<<nc<<" exceeed available: "<<onc<<endl;
    exit(1);
  }else if (dim != odim) {
    cout<<"Error copying the parameters...."<<endl;
    cout<<"required dimension "<<dim<<" exceeed available: "<<odim<<endl;
    exit(1);
  }

  //conBS = oconBS;
  //nullF = onullF;

  //Copy Priors......
  for (int i = 0; i < ng; i++) {
     pr[i] = opr[i];
  }

  for (int i = 0; i < nc; i++) {
     trp[i] = otrp[i];
  }
  
  for (int i = 0; i < ng; i++) {
     for (int j = 0; j < dim; j++) {
        mn[i][j] = omn[i][j];
	vr[i][j] = ovr[i][j];
     }
  }
}

void stC::LoadParam(int& ong, int& odim, int& onc, double*& otrp, double*& opr, double**& omn, double**& ovr) {

  //Loads parameters from the object parameters into the dummy arrays passed as arguments.

   ong = ng;
   odim = dim;
   onc = nc;

   //oconBS = conBS;
   //onullF = nullF;

   Alloc2f(omn, ng, dim);
   Alloc2f(ovr, ng, dim);
   Alloc1f(opr, ng);
   Alloc1f(otrp, nc);

  //Copy Priors......
  for (int i = 0; i < ng; i++) {
     opr[i] = pr[i];
  }

  for (int i = 0; i < nc; i++) {
     otrp[i] = trp[i];
  }
  
  for (int i = 0; i < ng; i++) {
     for (int j = 0; j < dim; j++) {
        omn[i][j] = mn[i][j];
	ovr[i][j] = vr[i][j];
     }
  }
}

void stC::StoreModel(char *mF) {

  ofstream fp_out;
  fp_out.open(mF, ios::app);
  fp_out<<sid<<" "<<wid<<" "<<begS<<" "<<endS<<" "<<conBS<<" "<<nullF<<endl;
  fp_out<<ng<<" "<<nc<<" "<<dim<<endl;
  for (int i = 0; i < ng; i++) {
     fp_out<<pr[i]<<" ";
  }
  fp_out<<endl;
  for (int i = 0; i < ng; i++) {
     for (int j = 0; j < dim; j++) {
        fp_out<<mn[i][j]<<" ";
     }
     fp_out<<endl;
     for (int j = 0; j < dim; j++) {
        fp_out<<vr[i][j]<<" ";
     }
     fp_out<<endl;
  }

  for (int i = 0; i < nc; i++) {
     fp_out<<trp[i]<<" ";
  }
  fp_out<<endl;

  fp_out.close();
}

void stC::ReadModel(ifstream& fp_in) {

   //Change MBR

   fp_in>>sid>>wid>>begS>>endS>>conBS>>nullF;	
   fp_in>>ng>>nc>>dim;

   //cout<<"SID: "<<sid<<" WID: "<<wid<<" begS: "<<begS<<" endS: "<<endS<<endl;
   //cout<<"NG: "<<ng<<" NC: "<<nc<<" DIM: "<<dim<<endl;

   Create(ng, nc, dim, begS, endS, sid, wid, conBS, nullF, 0); 

   for (int i = 0; i < ng; i++) {
     fp_in>>pr[i];
     //cout<<pr[i]<<" ";
   }
   //cout<<endl;

   for (int i = 0; i < ng; i++) {
      for (int j = 0; j < dim; j++) {
         fp_in>>mn[i][j];
	 //cout<<mn[i][j]<<" ";
      }
      //cout<<endl;

      for (int j = 0; j < dim; j++) {
         fp_in>>vr[i][j];
	 //cout<<vr[i][j]<<" ";
      }
      //cout<<endl;
   }

   for (int i = 0; i < nc; i++) {
      fp_in>>trp[i];
      //cout<<trp[i]<<" ";
   }
   //cout<<endl;
}

void stC::ReadForTrain(ifstream& fp_in) {

  //Change MBR

   fp_in>>sid>>wid>>begS>>endS>>conBS>>nullF;	
   fp_in>>ng>>nc>>dim;

   //cout<<"SID: "<<sid<<" WID: "<<wid<<" begS: "<<begS<<" endS: "<<endS<<endl;
   //cout<<"NG: "<<ng<<" NC: "<<nc<<" DIM: "<<dim<<endl;

   Create(ng, nc, dim, begS, endS, sid, wid, conBS, nullF, 1); 

   for (int i = 0; i < ng; i++) {
     fp_in>>pr[i];
     //cout<<pr[i]<<" ";
   }
   //cout<<endl;

   for (int i = 0; i < ng; i++) {
      for (int j = 0; j < dim; j++) {
         fp_in>>mn[i][j];
	 //cout<<mn[i][j]<<" ";
      }
      //cout<<endl;

      for (int j = 0; j < dim; j++) {
         fp_in>>vr[i][j];
	 //cout<<vr[i][j]<<" ";
      }
      //cout<<endl;
   }

   for (int i = 0; i < nc; i++) {
      fp_in>>trp[i];
      //cout<<trp[i]<<" ";
   }
   //cout<<endl;
}


double stC::getstD() {
   return stD;
}

int stC::getwid() {
   return wid;
}

void stC::AccumFeat(double *vec, double gam, int t) {

  //m is the component...
  //xi is the weigtage 
  //vec is the vectorl
  
  if (1 == nullF) {	//Don't accumulate for Null States.....
    return;
  }
	
  if (IsNAN(gam)) {
    cout<<"GAMMA is NAN"<<endl;
  }
  
  double prb;

  
  if (cchT == t) {
    prb = cchP;  //Store previous computed values, to avoid going to Gaussian Computation again.
                 //Initialize cchT to -1, so that it would not hold any backtimings....
		 //comp[] would have had the values prestored anyways.....
  //cout<<"SAME: CCH T: "<<cchT<<" t: "<<t<<" "<<sid<<endl;
  } else {  
    prb = ComProb(vec, comp);
    cchP = prb;
    cchT = t;
  }  

  double xi;
  for (int m = 0; m < ng; m++) {
     if (prb == 0) {
        xi = 0;
     } else {
        xi = gam * comp[m]/prb;

	if (IsNAN(xi)) {
	  cout<<"prb likely..."<<endl;
          cout<<"GAM: "<<gam<<" comp[m]: "<<comp[m]<<" PRB: "<<prb<<endl;
	}

     }

     for (int i = 0; i < dim; i++) {
       tsx[m][i] += vec[i] * xi;
       tsxx[m][i] += vec[i] * vec[i] * xi;
     }
     gauD[m] += xi;
  }
  stD += gam;
}

//void stC::AccumTran(double **gtrn, int s, int bs) {
void stC::AccumTran(double *gtrn, int bs) {

   if (1 == nullF) {	//Don't accumulate for NULL states....
     return;	   
   }

   int n = 0;	

   for (int i = 0; i < nc; i++) {
       n = bs + i;	   
       //trx[i] += gtrn[s][n]; 
       trx[i] += gtrn[n]; 
   }
}

int stC::getng() {
   return ng;
}

int stC::getdim() {
  return dim;
}

int stC::getnc() {
  return nc;
}

double stC::trans(int idx) {
   double rv;

   if (idx >= 0 && idx < nc) {
      rv = trp[idx];
   }else {
     rv = -1;
     cout<<"Passed idx "<<idx<<" > "<<nc<<endl;
     cout<<"State id: "<<sid<<" Word id: "<<wid<<endl;
     exit(1);
   }

   return rv;
}

stC::stC() {

}

double stC::GauProb(double *vec) {

  double x, xx, sx, ex, den, cpr, sum;
  double prb = 0;

  if (1 == nullF) {
    prb = 1;
  } else {
     for (int i = 0; i < ng; i++) {
        sum = 1;
        for (int j = 0; j < dim; j++) {
	   x = vec[j] - mn[i][j];
	   xx = x * x;
	   sx = xx / (2 * vr[i][j]);
	   ex = exp(-sx);
	   den = 1.0 / sqrt(2 * _pi * vr[i][j]);
	   cpr = den * ex;
	   sum = sum * cpr;
        }
        prb += pr[i] * sum;
    }
 }   

 return(prb);

}

double stC::ComProb(double *vec, double *com) {

  double x, xx, sx, ex, den, cpr, sum;
  double prb = 0;

  if (1 == nullF) {
    prb = 1;
    for (int i = 0; i < ng; i++) {
       com[i] = 1 / ng;	    
    }
  } else {
     for (int i = 0; i < ng; i++) {
       sum = 1;
       for (int j = 0; j < dim; j++) {
	  x = vec[j] - mn[i][j];
	  xx = x * x;
	  sx = xx / (2 * vr[i][j]);
	  ex = exp(-sx);
	  den = 1.0 / sqrt(2 * _pi * vr[i][j]);
	  cpr = den * ex;
	  sum = sum * cpr;
       }
       com[i] = pr[i] * sum;
       prb += pr[i] * sum;
     }
  }    
  return(prb);
}



void stC::Create(int t_ng, int t_nc, int t_dim, int t_bs, int t_es, int t_sid, int t_wid, int t_conBS, int t_nullF, int t_vf) { 

   ng = t_ng;	
   nc = t_nc;
   dim = t_dim;
   begS = t_bs;
   endS = t_es;
   sid = t_sid;
   wid = t_wid;
   conBS = t_conBS;
   nullF = t_nullF;

   Alloc2f(mn, ng, dim);
   Alloc2f(vr, ng, dim);

   Alloc1f(pr, ng);
   Alloc1f(trp, nc);

   //Allocating memory for accumulators...
   if (1 == t_vf) {

     Alloc2f(tsx, ng, dim);
     Alloc2f(tsxx, ng, dim);
     Alloc1f(gauD, ng);
     Alloc1f(trx, nc);
     Alloc1f(comp, ng);

   }
}


void stC::RandomInit(int prbF) {  //perturb Flag....
  
   double uf = 1.0/ng;
   double *comI;

   //comI = new double[ng];
   Alloc1f(comI, ng);

   if (1 == prbF) { //A slightly perturbed start 
	            //Useful for fully connected cases...
      for (int i = 0; i < ng; i++) {
         comI[i]  = 0.1 * (double) rand()/RAND_MAX;
      }
   } else {
      for (int i = 0; i < ng; i++) {
          comI[i]  = (i * 0.01) + 0.01;   //Perturbations for Gaussians inside a mixture
      }
   }

   for (int i = 0; i < ng; i++) { 	 
      pr[i] = uf;     //Priors
      for (int j = 0; j < dim; j++) {	   
          mn[i][j] = comI[i];  //Means
	  vr[i][j] = 16;       //Std
      }
   }

   double ut;

   ut = 1.0/nc;
   //nc == 1 in case of last nodes in the word model...
   if (1 == nc)  { ut = 0.8;
                 }
   for (int j = 0; j < nc; j++) {
      trp[j] = ut;    //Transitions..
   }

   if (1 == nullF) {
     trp[0] = 0;  //Self transition is zero for null state....
     if (nc > 1) {
       ut = 1.0 / (nc - 1);
       for (int j = 1; j < nc; j++) {
          trp[j] = ut;    //Transitions..
       }
     }	
   }

   Delete1f(comI);
}

void stC::NullifyAccum() {

   for (int i = 0; i < ng; i++) {
      for (int j = 0; j < dim; j++) {
         tsx[i][j] = 0;
	 tsxx[i][j] = 0;
      }
      gauD[i] = 0;
      comp[i] = 0;
   }

   for (int j = 0; j < nc; j++) {
      trx[j]  = 0; 
   }
   stD = 0;
}

void stC::ReEstimate() {

  if (1 == nullF) { //No Reestimation for Null State..
     return;
  }

  for (int i = 0; i < ng; i++) {

    double den = gauD[i];	  

    if (den == 0) {
       cout<<"Den == 0 - SID: "<<sid<<" wrd: "<<wid<<" GauDen:"<<gauD[i]<<endl;
       //exit(1);
       cout<<"Forcing means to be 0.01 and variance to be 1"<<endl;
       for (int j = 0; j < dim; j++) {
           //mn[i][j] = 0.01;
           vr[i][j] = 0.001;
       }
    } else { 

       for (int j = 0; j < dim; j++) {
          mn[i][j] = tsx[i][j] / den;
          vr[i][j] = tsxx[i][j] / den - (mn[i][j] * mn[i][j]);

          //Flooring....
          if (vr[i][j] < 0.001) { 
	    cout<<"Note: Floored: word:state:gau:dim "<<wid<<":"<<sid<<":"<<i<<":"<<j<<endl;
            cout<<"FLOORED : "<<vr[i][j]<<" TO 0.001"<<endl;
            vr[i][j] = 0.001;
          }
       }
    }
    
    if (stD == 0) {
    } else {
       pr[i] = den / stD;
    } 

    if (IsNAN(pr[i])) {
       cout<<"stD likely.....";
       cout<<"PR[i]: "<<pr[i]<<" Den: "<<den<<" STD: "<<stD<<endl;
     }
  }

  double ts = 0;
  if (stD == 0) { 
  } else { 
     for (int j = 0; j < nc; j++) {
        trp[j] = trx[j] / stD;	  
        ts += trp[j];
        if (IsNAN(trp[j])) {
            cout<<"stD likely.....";
           cout<<"trp[j]: "<<trp[j]<<" STD: "<<stD<<endl;
        }  
     }
  }
}


void stC::FreeStorage(int t_vf) {

   for (int i = 0; i < ng; i++) { 
      delete [] mn[i];	   
      delete [] vr[i];
   }
   delete [] pr;
   delete [] mn;
   delete [] vr;
   delete [] trp;

   if (1 == t_vf) {	
     for (int i = 0; i < ng; i++) { 
         delete [] tsx[i];
	 delete [] tsxx[i];
     }
     delete [] trx;
     delete [] tsx;
     delete [] tsxx;
     delete [] gauD;
   }
}
