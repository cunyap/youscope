package org.youscope.plugin.youscopeslimcontrol;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartMouseEvent;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.Crosshair;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.ChartMouseListener;
import org.jfree.chart.labels.CrosshairLabelGenerator;
import org.jfree.chart.panel.CrosshairOverlay;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.jfree.ui.RectangleEdge;
import org.youscope.clientinterfaces.YouScopeClient;
import org.youscope.common.microscope.CameraDevice;
import org.youscope.common.microscope.Device;
import org.youscope.plugin.slimjob.SlimJobConfiguration;
import org.youscope.plugin.slimjob.SlimProperties;
import org.youscope.serverinterfaces.YouScopeServer;
import org.youscope.uielements.DoubleTextField;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.geom.Rectangle2D;
import java.util.Arrays;
import java.util.stream.IntStream;
/**
 * This class represents the graph given as output after the computation of the radiants from the images intensity.
 * Moreover it represents the commands needed to manually select the starting phaseshift and, once the input is given 
 * from the user, it compute the four gray values needed to conclude the LCPM Calibration. Finally it saves the values
 * in the property file.
 * 
 * @author Elisa Garulli
 *
 */

public class PhaseChart extends JPanel{
	
	YouScopeClient client;
	YouScopeServer server;
	CameraDevice camera;
	String channelName;
	String channelGroup;
	Device LCPM;
	private final static String PHASESHIFT0_PROPERTY = "YouScope.SLIMControl.phaseShift0";
	private final static String PHASESHIFT1_PROPERTY = "YouScope.SLIMControl.phaseShift1";
	private final static String PHASESHIFT2_PROPERTY = "YouScope.SLIMControl.phaseShift2";
	private final static String PHASESHIFT3_PROPERTY = "YouScope.SLIMControl.phaseShift3";
	double[][] positions;
	double pi = Math.PI;
	
	/**
	 * Constructor. Sets the crosshairs and the graphs. Moreover adds the listener for the mouse movement and click.
	 * Calls the calibration process to generate the dataset.
	 * @param x grey values 0-255
	 * @param y radiants corresponding to the grey values
	 */
	public PhaseChart(YouScopeClient client, YouScopeServer server, CameraDevice camera, String channelName, 
			String channelGroup, Device LCPM) {
		new DataCalibrationLCPM(9,9.0,positions);
		this.client = client;
		this.server = server;
		this.camera = camera;
		this.channelName = channelName;
		this.LCPM = LCPM;
		LCPMCal calibration = new LCPMCal(client, server, camera, channelName, channelGroup, LCPM);
		int[] x = IntStream.rangeClosed(0, 255).toArray();
		double[] y = calibration.returnRadians();
		setLayout(new BorderLayout());
		JFreeChart xylineChart = ChartFactory.createXYLineChart("LCPM Phase", "LCPM grayscale value",
				"Phase shift (rad)", createDataset(y,x), PlotOrientation.VERTICAL, false, true, false);

		ChartPanel chartPanel = new ChartPanel(xylineChart);
		chartPanel.setPreferredSize(new Dimension(400,300));
		chartPanel.setOpaque(false);
		Color trans = new Color(0xFF, 0xFF, 0xFF, 0);
		xylineChart.setBackgroundPaint(trans);
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
				final double posX = (int) Math.round(plot.getDomainAxis().java2DToValue(event.getTrigger().getX(), dataArea, 
						RectangleEdge.BOTTOM));
				yCrosshair.setValue(posX);
			}
			JTextArea outputNumbers = new JTextArea();
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
				String[] positionString = new String[positions.length];
				for (int i=0 ; i < positions.length; i++) {
					if (positions[i][0] > 255) {
						positionString[i] = "--";
					}
					else {
						positionString[i] = Integer.toString((int)Math.round(positions[i][0]));
					}
				}
				outputNumbers.setText("Phase Shifts " + positionString[0] +", "+
						positionString[1]+", "+positionString[2] +", " + positionString[3]);
				outputNumbers.setEditable(false);
				outputNumbers.setEnabled(false);
				outputNumbers.setOpaque(false);
				repaint();
				add(outputNumbers, BorderLayout.SOUTH);
				revalidate();
				setPhaseShiftProperty(positionString);

				SlimJobConfiguration.setPhaseShiftMask(0, (int)Math.round(positions[0][0]));
				SlimJobConfiguration.setPhaseShiftMask(1,(int)Math.round(positions[1][0]));
				SlimJobConfiguration.setPhaseShiftMask(2,(int)Math.round(positions[2][0]));
				SlimJobConfiguration.setPhaseShiftMask(3,(int)Math.round(positions[3][0]));
			}
		});

		add(chartPanel, BorderLayout.NORTH);

	}
	
	/**
	 * Generates a dataset compatible with a JFreeChart.
	 * @param radiants i.e. the unwrapped intensity values.
	 * @param grayValues 0-255.
	 * @return the dataset.
	 */
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
	 * calculates the phase shifts values starting from the first gray value manually selected
	 * @param dataPoints radiants i.e. the unwrapped intensity values.
	 * @param initialX initial x selected manually
	 * @return an array of the x, y values corresponding to the desired 4 phase shifts.
	 */
	private double[][] getPhaseShift(double[] dataPoints, int initialX) {

		double initialY = dataPoints[initialX];
		double[] dataPointsPlusOne = new double[dataPoints.length + 1];
		for (int i = 0; i < dataPoints.length; i++) {
			dataPointsPlusOne[i] = dataPoints[i];
		}
		dataPointsPlusOne[dataPoints.length] = findMax(dataPoints) + 1;
		double[] wantedPhaseShifts = {0, pi/2, pi, 3*pi/2};
		int l = wantedPhaseShifts.length;

		double[] phaseShifts = new double[wantedPhaseShifts.length];
		for(int i=0; i < l;  i++) {
			phaseShifts[i] = initialY + wantedPhaseShifts[i];
		}

		double[][] grayValues = new double[wantedPhaseShifts.length][wantedPhaseShifts.length];
		for(int i=0; i < l;  i++) {
			grayValues[i][1] = findClosest(phaseShifts[i], dataPointsPlusOne);
			if (grayValues[i][1] == dataPointsPlusOne[dataPointsPlusOne.length -1]) {
				grayValues[i][0] = 1000;
			}
			else {
				grayValues[i][0] = findIndex(grayValues[i][1], dataPoints);
			}
		}
		return grayValues;

	}
	
	/**
	 * Finds highest value of a double array.
	 * @param doubleArray
	 * @return the highest value.
	 */
	private double findMax(double[] doubleArray) {
		double[] dummyArray = doubleArray.clone();
		Arrays.sort(dummyArray);
		return dummyArray[doubleArray.length-1];
	}

	/**
	 * Finds the index of a given double value in a given double array.
	 * @param inputPoint values of which one wants to find the index of.
	 * @param inputArray
	 * @return the index.
	 */
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

	/**
	 * Saves the four phase shift in the property file.
	 * @param positions the four computed x position on the graph (corresponding to the gray values).
	 */
	public void setPhaseShiftProperty(String[] positions)
	{
		client.getPropertyProvider().setProperty(PHASESHIFT0_PROPERTY, positions[0]);
		client.getPropertyProvider().setProperty(PHASESHIFT1_PROPERTY, positions[1]);
		client.getPropertyProvider().setProperty(PHASESHIFT2_PROPERTY, positions[2]);
		client.getPropertyProvider().setProperty(PHASESHIFT3_PROPERTY, positions[3]);
	}

}
