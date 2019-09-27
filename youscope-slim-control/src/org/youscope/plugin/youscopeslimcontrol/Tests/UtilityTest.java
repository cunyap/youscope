/**
 * 
 */
package org.youscope.plugin.youscopeslimcontrol.Tests;
import  java.util.Arrays;

import static org.junit.Assert.*;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.youscope.plugin.youscopeslimcontrol.Utility;

import jdk.nashorn.internal.objects.annotations.Setter;
import junit.framework.AssertionFailedError;

/**
 * @author localadmin
 *
 */
public class UtilityTest {
	double[] commonArray;
	double[] infiniteArray;
	double[] emptyArray;
	double[] nullArray;

	@Before
	public void setUp() {
		commonArray = new double[] {-100, 0, Math.sqrt(2), 100};
		infiniteArray = new double[] {Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY};
		emptyArray = new double[0];
		nullArray = null;
	}


	/**
	 * Test method for assertions for {@link org.youscope.plugin.youscopeslimcontrol.Utility#findMin(double[])}.
	 */
	@Test(expected = AssertionError.class)
	public void testFindMinNullCase() {
		//When
		Utility.findMin(nullArray);
	}

	/**
	 * Test method for a correct array for {@link org.youscope.plugin.youscopeslimcontrol.Utility#findMin(double[])}.
	 */
	@Test
	public void testFindMinGeneralCase() {
		//When
		double result = Utility.findMin(commonArray);
		//Then
		assertEquals(-100, result, 1e-10);
	}

	/**
	 * Test method for an array containing infinite values for
	 * {@link org.youscope.plugin.youscopeslimcontrol.Utility#findMin(double[])}.
	 */
	@Test
	public void testFindMinInfiniteCases() {
		//When
		double result = Utility.findMin(infiniteArray);
		//Then
		assertEquals(Double.NEGATIVE_INFINITY, result, 1e-10);
	}

	/**
	 * Test method for an empty array for
	 * {@link org.youscope.plugin.youscopeslimcontrol.Utility#findMin(double[])}.
	 */
	@Test
	public void testFindMinEmptyArray() {
		//When
		double result = Utility.findMin(emptyArray);
		//Then
		assertEquals(Double.POSITIVE_INFINITY, result, 1e-10);
	}

	/**
	 * Test method for assertions for {@link org.youscope.plugin.youscopeslimcontrol.Utility#findMax(double[])}.
	 */
	@Test(expected = AssertionError.class)
	public void testFindMaxNullCase() {
		//When
		Utility.findMax(nullArray);
	}

	/**
	 * Test method for a correct array for {@link org.youscope.plugin.youscopeslimcontrol.Utility#findMax(double[])}.
	 */
	@Test
	public void testFindMaxGeneralCase() {
		//When
		double result = Utility.findMax(commonArray);
		//Then
		assertEquals(100, result, 1e-10);
	}

	/**
	 * Test method for an array containing infinite values for
	 * {@link org.youscope.plugin.youscopeslimcontrol.Utility#findMax(double[])}.
	 */
	@Test
	public void testfindMaxInfiniteCases() {
		//When
		double result = Utility.findMax(infiniteArray);
		//Then
		assertEquals(Double.POSITIVE_INFINITY, result, 1e-10);
	}

	/**
	 * Test method for an empty array for
	 * {@link org.youscope.plugin.youscopeslimcontrol.Utility#findMax(double[])}.
	 */
	@Test
	public void testfindMaxEmptyArray() {
		//When
		double result = Utility.findMax(emptyArray);
		//Then
		assertEquals(Double.NEGATIVE_INFINITY, result, 1e-10);
	}


	/**
	 * Test method for assertion for
	 * {@link org.youscope.plugin.youscopeslimcontrol.Utility#findClosest(double, double[])}.
	 */
	@Test(expected = AssertionError.class)
	public void testFindClosestNullCase() {
		//When
		Utility.findClosest(100, nullArray);
	}

	/**
	 * Test method for a standard dataset for 
	 * {@link org.youscope.plugin.youscopeslimcontrol.Utility#findClosest(double, double[])}.
	 */
	@Test
	public void testFindClosestGeneralCase() {
		//When
		double result = Utility.findClosest(100, commonArray);
		//Then 
		assertEquals(100, result, 1e-10);
	}

	/**
	 * Test method for an array containing infinite values for
	 * {@link org.youscope.plugin.youscopeslimcontrol.Utility#findClosest(double, double[])}.
	 */
	@Test
	public void testFindClosestInfiniteCases() {
		//When
		double result = Utility.findClosest(100, infiniteArray);
		//Then 
		assertEquals(Double.NaN, result, 1e-10);
	}

	/**
	 * Test method for an empty array for
	 * {@link org.youscope.plugin.youscopeslimcontrol.Utility#findClosest(double, double[])}.
	 */
	@Test
	public void testFindClosestEmptyArray() {
		//When
		double result = Utility.findClosest(100, emptyArray);
		//Then 
		assertEquals(Double.NaN, result, 1e-10);
	}

	/**
	 * Test method for assertions for 
	 * {@link org.youscope.plugin.youscopeslimcontrol.Utility#findIndex(double, double[])}.
	 */
	@Test(expected = AssertionError.class)
	public void testFindIndexNullCase() {
		Utility.findIndex(100, nullArray);
	}

	/**
	 * Test method for a standard dataset for 
	 * {@link org.youscope.plugin.youscopeslimcontrol.Utility#findIndex(double, double[])}.
	 */
	@Test
	public void testFindIndexGeneralCase() {
		//When
		int result = Utility.findIndex(100, commonArray);
		//Then 
		assertEquals(3, result);
	}

	/**
	 * Test method for an array containing infinite values for
	 * {@link org.youscope.plugin.youscopeslimcontrol.Utility#findIndex(double, double[])}.
	 */
	@Test
	public void testFindIndexInfiniteCases() {
		//When
		int result = Utility.findIndex(100, infiniteArray);
		//Then 
		assertEquals(Integer.MAX_VALUE, result);
	}

	/**
	 * Test method for an empty array for
	 * {@link org.youscope.plugin.youscopeslimcontrol.Utility#findIndex(double, double[])}.
	 */
	@Test
	public void testFindIndexEmptyArray() {
		//When
		int result = Utility.findIndex(100, emptyArray);
		//Then 
		assertEquals(Integer.MAX_VALUE, result);
	}

	/**
	 * Test method for the same absolute value for
	 * {@link org.youscope.plugin.youscopeslimcontrol.Utility#floatMod(double, double)}.
	 */
	@Test
	public void testFloatModSameNumber() {
		//When
		double result = Utility.floatMod(100, -100);
		//Then
		assertEquals(0, result, 1e-10);
	}

	/**
	 * Test method for the same absolute value for
	 * {@link org.youscope.plugin.youscopeslimcontrol.Utility#floatMod(double, double)}.
	 */
	@Test
	public void testFloatModZeroCase() {
		//When
		double result = Utility.floatMod(100, 0);
		//Then
		assertEquals(Double.NaN, result, 1e-10);
	}

	/**
	 * Test method for the same absolute value for
	 * {@link org.youscope.plugin.youscopeslimcontrol.Utility#floatMod(double, double)}.
	 */
	@Test
	public void testFloatModGeneralCase() {
		//When
		double result = Utility.floatMod(100, -101);
		//Then
		assertEquals(-1, result, 1e-10);
	}

	/**
	 * Test method for assertions for 
	 * {@link org.youscope.plugin.youscopeslimcontrol.Utility#makeCumulativeSum(double[])}.
	 */
	@Test(expected = AssertionError.class)
	public void testMakeCumulativeSumNullCase() {
		Utility.makeCumulativeSum(nullArray);

	}
	
	/**
	 * Test method for a correct array for 
	 * {@link org.youscope.plugin.youscopeslimcontrol.Utility#makeCumulativeSum(double[])}.
	 */
	@Test
	public void testMakeCumulativeSumGeneralCase() {
		//When
		double[] result = Utility.makeCumulativeSum(commonArray);
		double[] expectedResult = new double[] {-100, -100, (Math.sqrt(2)-100), Math.sqrt(2)};
		//Then
		assertArrayEquals(expectedResult, result, 1e-10);
	}

	/**
	 * Test method for an array containing infinite values for
	 * {@link org.youscope.plugin.youscopeslimcontrol.Utility#makeCumulativeSum(double[])}.
	 */
	@Test
	public void testMakeCumulativeSumInfiniteCases() {
		//When
		double[] result = Utility.makeCumulativeSum(infiniteArray);
		double[] expectedResult = new double[] {Double.NEGATIVE_INFINITY,Double.NaN};
		//Then
		assertArrayEquals(expectedResult, result, 1e-10);
	}

	/**
	 * Test method for an empty array for
	 * {@link org.youscope.plugin.youscopeslimcontrol.Utility#makeCumulativeSum(double[])}.
	 */
	@Test
	public void testMakeCumulativeSumEmptyArray() {
		//When
		double[] result = Utility.makeCumulativeSum(emptyArray);
		double[] expectedResult = new double[] {};
		//Then
		assertArrayEquals(expectedResult, result, 1e-10);
	}

}
