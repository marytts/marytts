package marytts.util.math;

import org.junit.Assert;
import org.junit.Test;

public class MathUtilsSumTest {
    @Test
    public void test_sum_3_elements(){
        float[] a = new float[] {1, 0, 3};
        Assert.assertEquals((float)4.0, MathUtils.sum(a),0);
    }
    @Test
    public void test_sum_5_elements(){
        float[] v = new float[] {1, 0, 3, 5, 6};
        Assert.assertEquals((float)15.0, MathUtils.sum(v),0);
    }
    @Test
    public void test_sum_0_elements(){    
        float[] f = new float[] {};
        Assert.assertEquals((float)0, MathUtils.sum(f),0);
    }
    @Test
    public void test_sum_negative_elements(){   
        float[] b = new float[] {-3, -4, -5};
        Assert.assertEquals((float)-12, MathUtils.sum(b),0);
    }
    @Test
    public void test_z_array(){
        float[] c = new float[] {(float) -3.4, 23,(float) 0.45, -5,(float) 0.33, 0};
        Assert.assertEquals((float)15.38, MathUtils.sum(c),0.001);   
    }
}
