package org.youscope.plugin.youscopeslimcontrol;

import java.util.Arrays;

public class Utility {
	/**
	 * Finds the element with minimum value in a double array.
	 * @param doubleArray
	 * @return the minimum value.
	 */
	public static double findMin(double[] doubleArray) {
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
		double closest = Double.NEGATIVE_INFINITY;
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
		int result = Integer.MAX_VALUE;
		for (int i=0; i < inputArray.length;  i++) {
			if (inputPoint == inputArray[i]) {
				result = i;
			}
		}
		return result;
	}
}
