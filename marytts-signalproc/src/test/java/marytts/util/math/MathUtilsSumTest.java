package marytts.util.math;

import org.junit.Assert;
import org.junit.Test;

public class MathUtilsSumTest {

    @Test
    public void testSumOnePostiveElement(){
        //Partion 1 : One Element / Positive
        int[] a = new int[] {10};
        Assert.assertEquals(10, MathUtils.sum(a), 0);
    }

    @Test
    public void testSumManyPostiveElements(){
        //Partion 2 : Many Elements / Positive
        int[] a = new int[] {1, 2, 3, 3, 5, 0};
        Assert.assertEquals(14, MathUtils.sum(a), 0);
    }

    @Test
    public void testSumOnenegativeElement(){
        //Partion 3 : One Element / Negative
        int[] a = new int[] {-5};
        Assert.assertEquals(-5, MathUtils.sum(a), 0);
    }

    @Test
    public void testSumManyNegativeElements(){
        //Partion 4 : Many Elements / Negative
        int[] a = new int[] {-6, -7, -8, -8, -1, 0};
        Assert.assertEquals(-30, MathUtils.sum(a), 0);
    }
    
    @Test
    public void testSumNegativeAndPositiveElements(){
        //Partion 5 : Many Elements / Negative & Positive
        int[] a = new int[] {-1, -2, 0, 3, 4};
        Assert.assertEquals(4, MathUtils.sum(a), 0);
    }

    @Test
    public void testSumEmptyArray(){
        //Partion 6 : Empty Array
        int[] a = new int[] {};
        Assert.assertEquals(0, MathUtils.sum(a), 0);
    }
}
