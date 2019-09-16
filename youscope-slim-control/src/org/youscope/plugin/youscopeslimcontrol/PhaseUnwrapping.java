package org.youscope.plugin.youscopeslimcontrol;
import java.lang.Math;

/**
 * This class represents the Java translation of numpy.unwrap for unidimensional arrays.
 * @author Elisa Garulli
 *
 */
public class PhaseUnwrapping {
	/** 
	 * Calls the functions to unwrap the data. The computation is based on numpy unwrap function 
	 * (Reference: https://github.com/numpy/numpy/blob/v1.17.0/numpy/lib/function_base.py#L1471-L1525)
	 * @param inputArray
	 * @return
	 */
	public static double[] unwrap(double[] inputArray) {
		double pi = Math.PI;
		int length = 255;
		double[] clonedArray = new double[length];
		double[] deltasArray = new double[length];
		double[] ddmod = new double[length];
		boolean[] mask1 = new boolean[length]; 
		double[] phase_correct = new double[length];
		boolean[] mask2 = new boolean[length];
		
		clonedArray = inputArray.clone();
		for (int i = 0; i < length; i++ ) {		
			deltasArray[i] = inputArray[i+1] - inputArray[i];
		}
		for (int i = 0; i < length; i++ ) {
			ddmod[i] = (floatMod((deltasArray[i] + pi), (2*pi))) - pi ;
		}
		for (int i = 0; i < length; i++ ) {
			mask1[i] = (ddmod[i] == -pi) && (deltasArray[i] > 0) ;
		}
		for (int i = 0; i < length; i++ ) {
			if (mask1[i]) {
				ddmod[i] = pi;
			}
		}
		for (int i = 0; i < length; i++ ) {
			phase_correct[i] = ddmod[i] - deltasArray[i];
		}
		for (int i = 0; i < length; i++ ) {
			mask2[i] = Math.abs(deltasArray[i]) < pi ;
		}
		for (int i = 0; i < length; i++ ) {
			if (mask2[i]) {
				phase_correct[i] = 0;
			}
		}
		for (int i = 0; i < length; i++ ) {
			clonedArray[i+1] = inputArray[i+1] + makeCumulativeSum(phase_correct)[i];
		}
		return clonedArray;	
	}

	/**
	 * Calculates the cumulative sum of a given double array
	 * @param input
	 * @return a double[] array in which the i element is the sum of the 0...i elements of the input array.
	 */
	private static double[] makeCumulativeSum(double[] input) {
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
	private static double floatMod(double x, double y){
		return (x - Math.floor(x/y) * y);
	}


}
