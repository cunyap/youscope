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
import org.youscope.serverinterfaces.YouScopeServer;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.geom.Rectangle2D;
import java.util.Arrays;
import java.util.stream.IntStream;
/**
 * This class represents the graph given as output after the computation of the radians from the images intensity.
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
	double[] y;
	int[] x;
	double pi = Math.PI;
	JFreeChart xylineChart;
	ChartPanel chartPanel;
	Crosshair line1;
	Crosshair line2;
	Crosshair line3;
	Crosshair line4;
	Crosshair yCrosshair;
	String[] positionString;
	JTextArea outputText = new JTextArea();
	
	/**
	 * Constructor. Sets the crosshairs and the graphs. Moreover adds the listener for the mouse movement and click.
	 * Calls the calibration process to generate the dataset.
	 * @param x grey values 0-255
	 * @param y radians corresponding to the grey values
	 */
	public PhaseChart(YouScopeClient client, YouScopeServer server, CameraDevice camera, String channelName, 
			String channelGroup, Device LCPM) {
		this.client = client;
		this.server = server;
		this.camera = camera;
		this.channelName = channelName;
		this.LCPM = LCPM;
		LCPMCalibrator calibration = new LCPMCalibrator(client, server, camera, channelName, channelGroup, LCPM);
		setLayout(new BorderLayout());
		createChart(calibration);
		setCrosshair();
		crosshairController();
		add(chartPanel, BorderLayout.NORTH);

	}
	/**
	 * Creates the charts and sets the graphical settings, given a calibration object.
	 */
	private void createChart(LCPMCalibrator calibration) {
		x = IntStream.rangeClosed(0, 255).toArray();
		y = calibration.runCalibration();
		xylineChart = ChartFactory.createXYLineChart("LCPM Phase", "LCPM grayscale value",
				"Phase shift (rad)", createDataset(y,x), PlotOrientation.VERTICAL, false, true, false);
		chartPanel = new ChartPanel(xylineChart);
		chartPanel.setPreferredSize(new Dimension(400,300));
		chartPanel.setOpaque(false);
		Color trans = new Color(0xFF, 0xFF, 0xFF, 0);
		xylineChart.setBackgroundPaint(trans);

	}
	
	/**
	 * Creates the crosshair and sets their graphical appearance
	 */
	private void setCrosshair() {
		final CrosshairOverlay crosshairOverlay = new CrosshairOverlay();
		yCrosshair = new Crosshair(Double.NaN, Color.RED, new BasicStroke(2f));
		yCrosshair.setLabelBackgroundPaint(new Color(1f, 1f, 1f, 0f));
		line1 = new Crosshair(Double.NaN, Color.DARK_GRAY, new BasicStroke(0f));
		line1.setLabelBackgroundPaint(new Color(1f, 1f, 1f, 0f));
		line2 = new Crosshair(Double.NaN, Color.DARK_GRAY, new BasicStroke(0f));
		line2.setLabelBackgroundPaint(new Color(1f, 1f, 1f, 0f));
		line3 = new Crosshair(Double.NaN, Color.DARK_GRAY, new BasicStroke(0f));
		line3.setLabelBackgroundPaint(new Color(1f, 1f, 1f, 0f));
		line4 = new Crosshair(Double.NaN, Color.DARK_GRAY, new BasicStroke(0f));
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
	}
	
	/**
	 * Controls the position of the crosshair in the graphs by setting listeners for the movement and for the click 
	 * of the mouse in the graph area.
	 */
	private void crosshairController() {
		final XYPlot plot = xylineChart.getXYPlot();
		chartPanel.addChartMouseListener(new ChartMouseListener() {
			@Override
			public void chartMouseMoved(final ChartMouseEvent event) {
				final Rectangle2D dataArea = chartPanel.getScreenDataArea();
				final double posX = (int) Math.round(plot.getDomainAxis().java2DToValue(event.getTrigger().getX(), dataArea, 
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
				outputText.repaint();
				writeResults();
				add(outputText, BorderLayout.SOUTH);
				outputText.revalidate();
				setPhaseShiftProperty(positionString);
				setSLIMProperties();
			}
		});
	}
	
	/**
	 * Sets the corresponding properties in a SLIM job to the computed phase shifts.
	 */
	public void setSLIMProperties() {
		SlimJobConfiguration.setPhaseShiftMask(0, (int)Math.round(positions[0][0]));
		SlimJobConfiguration.setPhaseShiftMask(1,(int)Math.round(positions[1][0]));
		SlimJobConfiguration.setPhaseShiftMask(2,(int)Math.round(positions[2][0]));
		SlimJobConfiguration.setPhaseShiftMask(3,(int)Math.round(positions[3][0]));
		SlimJobConfiguration.setPhaseShiftOutside((int)Math.round(positions[0][0]));
	}
	
	/**
	 * Creates a user-friendly string with the results and sets it in the output text area.
	 */
	public void writeResults() {
		positionString = new String[positions.length];
		outputText.setEditable(false);
		outputText.setEnabled(false);
		outputText.setOpaque(false);
		for (int i=0 ; i < positions.length; i++) {
			if (positions[i][0] > 255 | positions[i][0] < 0) {
				positionString[i] = "--";
			}
			else {
				positionString[i] = Integer.toString((int)Math.round(positions[i][0]));
			}
		}
		outputText.setText("Phase Shifts " + positionString[0] +", "+
				positionString[1]+", "+positionString[2] +", " + positionString[3]);
	}
	
	/**
	/**
	 * Generates a dataset compatible with a JFreeChart.
	 * @param radians i.e. the unwrapped intensity values.
	 * @param grayValues 0-255.
	 * @return the dataset.
	 */
	private XYDataset createDataset(double[] radians, int[] grayValues) {
		final XYSeries LCPMPhaseSeries = new XYSeries("LCPMPhase");
		for (int i=0; i<radians.length; i++) {
			LCPMPhaseSeries.add(grayValues[i], radians[i]);
		}
		final XYSeriesCollection dataset = new XYSeriesCollection();
		dataset.addSeries(LCPMPhaseSeries);
		return dataset;
	}

	/**
	 * Calculates the phase shifts values starting from the first gray value manually selected
	 * @param dataPoints radians i.e. the unwrapped intensity values.
	 * @param initialX initial x selected manually
	 * @return an array of the x, y values corresponding to the desired 4 phase shifts.
	 */
	private double[][] getPhaseShift(double[] dataPoints, int initialX) {
		double initialY = Double.NEGATIVE_INFINITY;
		try{
			initialY = dataPoints[initialX];
		}
		catch(Exception e) {
			client.sendError("The value must be less than or equal to 255");
		}
		double[] dataPointsPlusOne = new double[dataPoints.length + 1];
		for (int i = 0; i < dataPoints.length; i++) {
			dataPointsPlusOne[i] = dataPoints[i];
		}
		dataPointsPlusOne[dataPoints.length] = Utility.findMax(dataPoints) + 1;
		double[] wantedPhaseShifts = {0, pi/2, pi, 3*pi/2};
		int l = wantedPhaseShifts.length;

		double[] phaseShifts = new double[wantedPhaseShifts.length];
		for(int i=0; i < l;  i++) {
			phaseShifts[i] = initialY + wantedPhaseShifts[i];
		}

		double[][] grayValues = new double[wantedPhaseShifts.length][wantedPhaseShifts.length];
		for(int i=0; i < l;  i++) {
			grayValues[i][1] = Utility.findClosest(phaseShifts[i], dataPointsPlusOne);
			if (grayValues[i][1] == dataPointsPlusOne[dataPointsPlusOne.length -1]) {
				grayValues[i][0] = 1000;
			}
			else {
				grayValues[i][0] = Utility.findIndex(grayValues[i][1], dataPoints);
			}
		}
		return grayValues;

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

	/**
	 * Returns the radians
	 * @return radians values
	 */
	public double[] returnCalibrationData(){
		return y;
	}
}
