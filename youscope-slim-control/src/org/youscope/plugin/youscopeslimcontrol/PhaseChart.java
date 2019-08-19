package org.youscope.plugin.youscopeslimcontrol;
import javax.swing.BoxLayout;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextArea;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartMouseEvent;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.annotations.XYAnnotation;
import org.jfree.chart.annotations.XYDataImageAnnotation;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.entity.ChartEntity;
import org.jfree.chart.entity.XYItemEntity;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.chart.plot.Crosshair;
import org.jfree.chart.plot.DefaultDrawingSupplier;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.ChartMouseListener;
import org.jfree.chart.event.RendererChangeEvent;
import org.jfree.chart.labels.CrosshairLabelGenerator;
import org.jfree.chart.labels.StandardXYToolTipGenerator;
import org.jfree.chart.panel.CrosshairOverlay;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.jfree.ui.RectangleEdge;
import org.jfree.util.PublicCloneable;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.geom.Rectangle2D;
import java.util.Arrays;
/**
 * This class represents the graph given as output after the computation of the radiants from the images intensity.
 * Moreover it represents the commands needed to manually select the starting phaseshift and compute the four gray
 * values needed to conclude the LCPM Calibration.
 * 
 * ***************************************************
 * For some reason is not taking the clicked X value as the starting point to compute the phase shifts.
 * Gotta re-check the computation in particular compared to the ones already detailed by andreas on jupyter.
 * ***************************************************
 * @author Elisa Garulli
 *
 */

public class PhaseChart extends JPanel{
	
	double[][] positions;
	double pi = Math.PI;
	/**
	 * Constructor. Sets the crosshairs and the graphs. Moreover adds the listener for the mouse movement and click.
	 * @param x grey values 0-255
	 * @param y radiants corresponding to the grey values
	 */
	public PhaseChart(int[] x, double[] y) {
		//setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
		JFreeChart xylineChart = ChartFactory.createXYLineChart("LCPM Phase", "LCPM grayscale value",
				"Phase shift (rad)", createDataset(y,x), PlotOrientation.VERTICAL, false, true, false);

		ChartPanel chartPanel = new ChartPanel(xylineChart);
		chartPanel.setPreferredSize(new Dimension(400,300));
		final XYPlot plot = xylineChart.getXYPlot();
		final CrosshairOverlay crosshairOverlay = new CrosshairOverlay();
		final Crosshair yCrosshair = new Crosshair(Double.NaN, Color.RED, new BasicStroke(2f));
		yCrosshair.setLabelBackgroundPaint(new Color(1f, 1f, 1f, 0f));
		final Crosshair line1 = new Crosshair(Double.NaN, Color.DARK_GRAY, new BasicStroke(0f));
		line1.setLabelBackgroundPaint(new Color(1f, 1f, 1f, 0f));
		final Crosshair line2 = new Crosshair(Double.NaN, Color.DARK_GRAY, new BasicStroke(0f));
		line2.setLabelBackgroundPaint(new Color(1f, 1f, 1f, 0f));
		final Crosshair line3 = new Crosshair(Double.NaN, Color.DARK_GRAY, new BasicStroke(0f));
		line3.setLabelBackgroundPaint(new Color(1f, 1f, 1f, 0f));
		final Crosshair line4 = new Crosshair(Double.NaN, Color.DARK_GRAY, new BasicStroke(0f));
		line4.setLabelBackgroundPaint(new Color(1f, 1f, 1f, 0f));
		yCrosshair.setLabelOutlineVisible(false);
		yCrosshair.setLabelVisible(true);
		yCrosshair.setLabelGenerator(new CrosshairLabelGenerator() {
			@Override
			public String generateLabel(final Crosshair ch) {
				return (Double.toString(ch.getValue()));
			}	
		});

		crosshairOverlay.addDomainCrosshair(yCrosshair);
		crosshairOverlay.addDomainCrosshair(line1);
		crosshairOverlay.addDomainCrosshair(line2);
		crosshairOverlay.addDomainCrosshair(line3);
		crosshairOverlay.addDomainCrosshair(line4);

		chartPanel.addOverlay(crosshairOverlay);
		chartPanel.addChartMouseListener(new ChartMouseListener() {
			@Override
			public void chartMouseMoved(final ChartMouseEvent event) {
				final Rectangle2D dataArea = chartPanel.getScreenDataArea();
				final double posX = (int)Math.round(plot.getDomainAxis().java2DToValue(event.getTrigger().getX(), dataArea, 
						RectangleEdge.BOTTOM));
				yCrosshair.setValue(posX);
			}

			@Override
			public void chartMouseClicked(final ChartMouseEvent event) {
				final Rectangle2D dataArea = chartPanel.getScreenDataArea();
				final int posX = (int) Math.round(plot.getDomainAxis().java2DToValue(event.getTrigger().getX(),
						dataArea, RectangleEdge.BOTTOM));
				positions = getPhaseShift(y, posX);
				line1.setValue(positions[0][0]);
				line2.setValue(positions[1][0]);
				line3.setValue(positions[2][0]);
				line4.setValue(positions[3][0]);
				JOptionPane.showMessageDialog(null, "Phase Shifts " + positions[0][0] +", "+
				positions[1][0]+", "+positions[2][0] +", " + positions[3][0], "Error", JOptionPane.ERROR_MESSAGE);
			}
		});
		
//		JTextArea outputNumbers = new JTextArea("Output number: ");	
//		Font font = new Font("Courier New",Font.BOLD, 15);
//		outputNumbers.setFont(font);
//		outputNumbers.setForeground(Color.black);		
//		outputNumbers.append("Phase Shifts " + positions[0][0] +", "+
//				positions[1][0]+", "+positions[2][0] +", " + positions[3][0]);
//		outputNumbers.setOpaque(false);
//		outputNumbers.setEditable(false);
//		outputNumbers.setEnabled(false);
		add(chartPanel);
		//add(outputNumbers);
	}

	private XYDataset createDataset(double[] radiants, int[] grayValues) {
		final XYSeries LCPMPhaseSeries = new XYSeries("LCPMPhase");
		for (int i=0; i<radiants.length; i++) {
			LCPMPhaseSeries.add(grayValues[i], radiants[i]);
		}
		final XYSeriesCollection dataset = new XYSeriesCollection();
		dataset.addSeries(LCPMPhaseSeries);
		return dataset;
	}

	/**
	 * calculates the phaseshifts values starting from the first gray value manually selected
	 * @param dataPoints radiants
	 * @param initialX initial X selected manually
	 * @return an array of the x,y values corresponding to the desired 4 phase sifts.
	 */
	private double[][] getPhaseShift(double[] dataPoints, int initialX) {

		double initialY = dataPoints[initialX];
		double[] wantedPhaseShifts = {0, pi/2, pi, 3*pi/2};
		int l = wantedPhaseShifts.length;

		double[] phaseShifts = new double[wantedPhaseShifts.length];
		for(int i=0; i < l;  i++) {
			phaseShifts[i] = initialY + wantedPhaseShifts[i];
		}


		double[][] grayValues = new double[wantedPhaseShifts.length][wantedPhaseShifts.length];
		for(int i=0; i < l;  i++) {
			grayValues[i][1] = findClosest(phaseShifts[i], dataPoints);
			grayValues[i][0] = findIndex(grayValues[i][1], dataPoints);
		}
		return grayValues;

	}

	private double findMin(double[] doubleArray) {
		double[] dummyArray = doubleArray.clone();
		Arrays.sort(dummyArray);
		return dummyArray[0];
	}

	private int findIndex(double inputPoint, double[] inputArray) {
		int result = -1;
		for (int i=0; i < inputArray.length;  i++) {
			if (inputPoint == inputArray[i]) {
				result = i;
			}
		}
		return result;
	}

	/**
	 * Finds the closest value to the given one, in a provided array.
	 * @param inputPoint
	 * @param inputArray
	 * @return the closest element of the array
	 */
	private double findClosest(double inputPoint, double[] inputArray) {
		double closest = -20;
		double difference = 100;
		for (int i=0; i<inputArray.length; i++) {
			double currentDifference = Math.abs(inputArray[i] - inputPoint);
			if(currentDifference < difference) {
				difference = currentDifference;
				closest = inputArray[i];
			}
		}
		return closest;
	}

}
