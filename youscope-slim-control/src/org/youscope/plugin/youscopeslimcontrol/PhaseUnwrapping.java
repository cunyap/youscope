package org.youscope.plugin.youscopeslimcontrol;
import java.lang.Math;

public class PhaseUnwrapping {
	int length = 255;
	double pi = Math.PI;
	double[] dd = new double[length];
	double[] ddmod = new double[length];
	boolean[] mask1 = new boolean[length]; 
	double[] ph_correct = new double[length];
	boolean[] mask2 = new boolean[length];
	double[] up = new double[length];
	double[] phase_array = {2,2,3,4,5,6,7,8,9,0};

	public double[] unwrap(double[] p) {
		up = p.clone();
		for (int i = 0; i < length; i++ ) {		
			dd[i] = p[i+1] - p[i];
		}
		for (int i = 0; i < length; i++ ) {
			ddmod[i] = (floatMod((dd[i] + pi), (2*pi))) - pi ;
		}
		for (int i = 0; i < length; i++ ) {
			mask1[i] = (ddmod[i] == -pi) && (dd[i] > 0) ;
		}
		for (int i = 0; i < length; i++ ) {
			if (mask1[i]) {
				ddmod[i] = pi;
			}
		}
		for (int i = 0; i < length; i++ ) {
			ph_correct[i] = ddmod[i] - dd[i];
		}
		for (int i = 0; i < length; i++ ) {
			mask2[i] = Math.abs(dd[i]) < pi ;
		}
		for (int i = 0; i < length; i++ ) {
			if (mask2[i]) {
				ph_correct[i] = 0;
			}
		}
		for (int i = 0; i < length; i++ ) {
			up[i+1] = p[i+1] + makeCumulativeSum(ph_correct)[i];
		}
		return up;	
	}

	public double[] makeCumulativeSum(double[] input) {
		double[] output = new double[input.length];
		double total = 0;
		for (int i = 0; i < input.length; i++) {
			total += input[i];
			output[i] = total;
		}
		return output;
	}

	double floatMod(double x, double y){
		return (x - Math.floor(x/y) * y);
	}
	
	
}
