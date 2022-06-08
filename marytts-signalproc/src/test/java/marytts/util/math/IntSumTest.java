package marytts.util.math;

import org.junit.Assert;
import org.junit.Test;

public class MathUtilsIntSumTest {
    @Test
    public void TestOnePositiveElements(){
        //Partion 1: One Positive Element
        float[] a = new int[] {10};
        Assert.assertEquals(10, MathUtils.sum(a),0);
    }
    
    @Test
    public void TestManyPositiveElements(){
        //Partion 2: Many Positive Element
        float[] b = new int[] {1, 2, 3, 4, 5};
        Assert.assertEquals(15, MathUtils.sum(b),0);
    }
    
    @Test
    public void TestEmptyArray(){
        //Partion 3: Empty Array
        float[] c = new int[] {};
        Assert.assertEquals(0, MathUtils.sum(c),0);
    }
    
    @Test
    public void TestOneNegativeElements(){
        //Partion 4: One Negative Element
        float[] d = new int[] {-1};
        Assert.assertEquals(-1, MathUtils.sum(d),0);
    }

    @Test
    public void TestManyNegativeElements(){
        //Partion 5: Many Negative Element
        float[] e = new int[] {-1, -3, -4, -10};
        Assert.assertEquals(-18, MathUtils.sum(e),0);
    }

    @Test
    public void TestSimiliarPositiveElements(){
        //Partion 7: Similar Positve
        float[] f = new int[] {2, 2, 2, 2};
        Assert.assertEquals(8, MathUtils.sum(f),0);
    }

    @Test
    public void TestSimiliarNegativeElements(){
        //Partion 8: Similar Negative
        float[] g = new int[] {-3, -3, -3};
        Assert.assertEquals(-9, MathUtils.sum(g),0);
    }

    @Test
    public void TestNegativeAndPositiveElements(){
        //Partion 9: Negative & Positive
        float[] h = new int[] {-2, -1, 3, 4};
        Assert.assertEquals(4, MathUtils.sum(h),0);
    }

    @Test
    public void TestZeroElements(){
        //Partion 10: Zeroes
        float[] i = new int[] {0, 0, 0, 0};
        Assert.assertEquals(0, MathUtils.sum(i),0);
    }
}
