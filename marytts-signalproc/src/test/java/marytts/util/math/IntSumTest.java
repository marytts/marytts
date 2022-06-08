package marytts.util.math;

import org.junit.Assert;
import org.junit.Test;

public class MathUtilsIntSumTest {
    @Test
    public void test_3_element(){
        float[] a = new int[] {0, 1, 2};
        Assert.assertEquals((int)3, MathUtils.sum(a),0);
    }
    
    @Test
    public void test_4_element(){
        float[] b = new int[] {-1, 0, 2, 5};
        Assert.assertEquals((int)6, MathUtils.sum(b),0);
    }
    
    @Test
    public void test_empty_array(){
        float[] c = new int[] {};
        Assert.assertEquals((int)0, MathUtils.sum(c),0);
    }
    
    @Test
    public void test_3_negative_element(){
        float[] d = new int[] {-1,-2,-3};
        Assert.assertEquals((int)-6, MathUtils.sum(d),0);
    }
}