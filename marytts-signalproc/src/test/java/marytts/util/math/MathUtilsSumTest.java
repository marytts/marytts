package marytts.util.math;

import org.junit.Assert;
import org.junit.Test;


public class MathUtilsSumTest {
    @Test
    public void testSumOnePostiveElements(){
        //Partion 1 : One Element - Positive
        float[] a = new float[] {(float)1.5};
        Assert.assertEquals((float)1.5, MathUtils.sum(a),0.001);
    }
    @Test
    public void testSumManyPostiveElements(){
        //Partion 2 : Many Elements - Positive
        float[] v = new float[] {1, 0, 3, 5, 6};
        Assert.assertEquals((float)15.0, MathUtils.sum(v),0);
    }
    @Test
    public void testSumZeroElements(){ 
        //Partion 3 : Zero Element 
        float[] f = new float[] {};
        Assert.assertEquals((float)0, MathUtils.sum(f),0);
    }
    @Test
    public void testSumZero(){ 
        //Partion 4 : Element = 0
        float[] f = new float[] {0, 0, 0, 0};
        Assert.assertEquals((float)0, MathUtils.sum(f),0);
    }
    @Test
    public void testSumOneNegativeElements(){  
        //Partion 5 : One Elements - Negative 
        float[] b = new float[] {-3};
        Assert.assertEquals((float)-3, MathUtils.sum(b),0);
    }
    @Test
    public void testSumManyNegativeElements(){  
        //Partion 6 : Many Elements - Negative 
        float[] b = new float[] {-3, -4, -5};
        Assert.assertEquals((float)-12, MathUtils.sum(b),0);
    }
    @Test
    public void testNegativeAndPostiveElements(){
        //Partion 7 : Many Elements - Negative, Positive, 0
        float[] c = new float[] {(float) -3.4, 23,(float) 0.45, -5,(float) 0.33, 0};
        Assert.assertEquals((float)15.38, MathUtils.sum(c),0.001);   
    }


}
