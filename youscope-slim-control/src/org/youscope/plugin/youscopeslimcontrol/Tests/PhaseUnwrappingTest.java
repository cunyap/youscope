/**
 * 
 */
package org.youscope.plugin.youscopeslimcontrol.Tests;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;
import org.youscope.plugin.youscopeslimcontrol.LCPMCalibration.PhaseUnwrapping;

/**
 * @author localadmin
 *
 */
public class PhaseUnwrappingTest {

	double[] inputArray;
	double[] numpyOutputArray;
	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {
		inputArray = new double[] {0., 0.78539816, 1.57079633, 5.49778714, 6.28318531};
		numpyOutputArray = new double[] { 0., 0.78539816, 1.57079633, -0.78539816, 0.};
	}

	/**
	 * 
	 */
	@Test
	public void test() {
		//When
		double[] result = PhaseUnwrapping.unwrap(inputArray);
		//Then
		assertArrayEquals(numpyOutputArray, result, 1e-8);
	}

}
