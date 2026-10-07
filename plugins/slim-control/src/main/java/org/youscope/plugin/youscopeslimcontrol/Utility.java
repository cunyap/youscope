/*******************************************************************************
 * Copyright (c) 2019 - 2020 Elisa Garulli.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0 OR the GNU Lesser General Public
 * License, Version 2.1 (or at your option any later version).
 *
 * You may choose which license you wish to use when copying, modifying,
 * or distributing this software.
 *
 * Licenses are available at:
 * - Apache 2.0: http://apache.org
 * - LGPL 2.1:   https://gnu.org
 *
 * SPDX-License-Identifier: Apache-2.0 OR LGPL-2.1-or-later
 *
 * Contributors:
 *     Elisa Garulli - initial API and implementation
 *     Andreas P. Cuny - migration to the current YouScope API
 ******************************************************************************/
package org.youscope.plugin.youscopeslimcontrol;

import java.util.Arrays;

public class Utility {
	/**
	 * Finds the element with minimum value in a double array.
	 * @param doubleArray
	 * @return the minimum value.
	 */
	public static double findMin(double[] doubleArray) {
		assert doubleArray != null : "Can not calculate the minimum value in a null array";
		double minValue = Double.POSITIVE_INFINITY;
		for (int i=0; i < doubleArray.length; i++) {
			if (doubleArray[i] < minValue) {
			minValue = doubleArray[i];
		}
		}
		return minValue;
	}

	/**
	 * Finds the element with maximum value in a double array.
	 * @param doubleArray
	 * @return the maximum value.
	 */
	public static double findMax(double[] doubleArray) {
		assert doubleArray != null : "Can not calculate the maximum value in a null array";
		double maxValue = Double.NEGATIVE_INFINITY;
		for (int i=0; i < doubleArray.length; i++) {
			if (doubleArray[i] > maxValue) {
			maxValue = doubleArray[i];
		}
		}
		return maxValue;
	}

	/**
	 * Finds the closest value to the given one, in a provided array.
	 * @param inputPoint
	 * @param inputArray
	 * @return the closest element of the array
	 */
	public static double findClosest(double inputPoint, double[] inputArray) {
		assert inputArray != null : "Can not find the closest value in a null array";
		double closest = Double.NaN;
		double difference = Double.POSITIVE_INFINITY;
		for (int i=0; i<inputArray.length; i++) {
			double currentDifference = Math.abs(inputArray[i] - inputPoint);
			if(currentDifference < difference) {
				difference = currentDifference;
				closest = inputArray[i];
			}
		}
		return closest;
	}

	/**
	 * Finds the index of a given double value in a given double array.
	 * @param inputPoint values of which one wants to find the index of.
	 * @param inputArray
	 * @return the index.
	 */
	public static int findIndex(double inputPoint, double[] inputArray) {
		assert inputArray != null : "Can not look for an element's index in a null array";
		int result = Integer.MAX_VALUE;
		for (int i=0; i < inputArray.length;  i++) {
			if (inputPoint == inputArray[i]) {
				result = i;
			}
		}
		return result;
	}

	/**
	 * Calculates the cumulative sum of a given double array
	 * @param input
	 * @return a double[] array in which the i element is the sum of the 0...i elements of the input array.
	 */
	public static double[] makeCumulativeSum(double[] input) {
		assert input != null : "Can not compute the cumulative sum of a null array";
		double[] output = new double[input.length];
		double total = 0;
		for (int i = 0; i < input.length; i++) {
			total += input[i];
			output[i] = total;
		}
		return output;
	}

	/**
	 * Workaround to have the corresponding of the % operator for double, even if negatives.
	 * @param x
	 * @param y
	 * @return the remainder
	 */
	public static double floatMod(double x, double y){
		return (x - Math.floor(x/y) * y);
	}
}
