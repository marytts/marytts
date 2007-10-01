package de.dfki.lt.mary.htsengine;



/**
 * Slide Vector is a Vector that can be used with different offsets.
 * 
 * Java port and extension of HTS engine version 2.0
 * Extension: mixed excitation
 * @author Christoph Endres, Marcela charfuelan.
 */
public class SlideVector {
	
   private double[] content;
   private int content_size;
   
   /* offsets for vector c */ 
   private static int cc;
   private static int cinc;
   private static int d1;
   
   /** Constructor *
   * The size of the vector is specified when creating a new SlideVector */
   public SlideVector(int vector_size, int mcep_order, int pade_order) {
	   
	   content    = new double[vector_size];
	   content_size = vector_size;
	   
	   /* offset for vector c:
	    * vector_size = ( 3*(m+1) + 3*(pd+1) + pd*(m+2) ), m=mcep_vsize;
	    * vector_size = 623 with mcep_vsize=74
	    * c    =                              --> c[0]...  
	    * cc   = c+m+1                        --> c[75]... 
	    * cinc = cc+m+1   = (c+m+1)+m+1       --> c[150]... 
	    * d1   = cinc+m+1 = ((c+m+1)+m+1)+m+1 --> c[225]... */
	    cc   = 1*(mcep_order+1);
	    cinc = 2*(mcep_order+1);
	    d1   = 3*(mcep_order+1);
	   
   }
    
   public void clearContent(){
	 for(int i=0; i<content_size; i++)
		 content[i]=0.0;  
   }
   /* General methods for getting content */
   private double get(int index, int offset) { return content[offset+index]; }
   /* this is the general method to get content[] without offset */
   public double get(int index) {return content[index]; }

   /* Particular methods for getting content */
   public double getCC(int index) { return this.get(index, SlideVector.cc); }

   public double getCINC(int index) { return this.get(index, SlideVector.cinc); }

   public double getD1(int index) { return this.get(index, SlideVector.d1); }
   
   
   /* General method for setting content */
   private void set(int index, int offset, double value) { content[offset+index] = value; }
   /* this is the general method to set content[] without offset*/
   public void set(int index, double value){ content[index] = value; }
   
   /* Particular methods for setting content */
   public void setCC(int index, double value) { this.set(index, SlideVector.cc, value); }
  
   public void setCINC(int index, double value) { this.set(index, SlideVector.cinc, value); }

   public void setD1(int index, double value) { this.set(index, SlideVector.d1, value); }
   
   
}  /* class SlideVector */

