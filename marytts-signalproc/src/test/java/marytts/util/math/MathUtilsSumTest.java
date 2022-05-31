package marytts.util.math;

import org.junit.Assert;
import org.junit.Test;

public class MathUtilsSumTest {
    @Test
    public void testSumfunction(){
        float[] a = new float[] {1, 0, 3 };
        Assert.assertEquals((float)4.0, MathUtils.sum(a),0);
        float[] v = new float[] {1, 0, 3,5,6 };
        Assert.assertEquals((float)15.0, MathUtils.sum(v),0);
        float[] f = new float[] {};
        Assert.assertEquals((float)0, MathUtils.sum(f),0);
        float[] b = new float[] {-3,-4,-5};
        Assert.assertEquals((float)-12, MathUtils.sum(b),0);

    }

     
}
