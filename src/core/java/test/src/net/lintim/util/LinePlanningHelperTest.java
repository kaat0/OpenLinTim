package net.lintim.util;

import org.junit.Test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.assertEquals;

/**
 */
public class LinePlanningHelperTest {

    @Test
    public void testPrimeFactors() {
        Set<Integer> result = new HashSet<>();
        assertEquals("1 has no prime factors", result, LinePlanningHelper.determinePossibleSystemFrequencies(-1, 1));
        result.add(2);
        assertEquals("Can not compute prime factors", result, LinePlanningHelper.determinePossibleSystemFrequencies
            (2, 60));
        assertEquals("Can compute prime factors of 16", result, LinePlanningHelper
            .determinePossibleSystemFrequencies(-1, 16));
        result.addAll(Arrays.asList(3, 5));
        assertEquals("Can compute prime factors of 60", result, LinePlanningHelper
            .determinePossibleSystemFrequencies(-1, 60));
    }
}
