package org.youscope.plugin.youscopeslimcontrol.LCPMCalibration;
import java.lang.Math;

import org.youscope.plugin.youscopeslimcontrol.Utility;

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
		int length = inputArray.length-1;
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
			ddmod[i] = (Utility.floatMod((deltasArray[i] + pi), (2*pi))) - pi ;
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
			clonedArray[i+1] = inputArray[i+1] + Utility.makeCumulativeSum(phase_correct)[i];
		}
		return clonedArray;	
	}




}
