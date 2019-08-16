package org.youscope.plugin.youscopeslimcontrol;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

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
import java.awt.geom.Rectangle2D;
import java.util.Arrays;


public class PhaseChart extends JPanel{
	double pi = Math.PI;
	public PhaseChart(int[] x, double[] y) {
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
	
	crosshairOverlay.addRangeCrosshair(yCrosshair);
	crosshairOverlay.addRangeCrosshair(line1);
	crosshairOverlay.addRangeCrosshair(line2);
	crosshairOverlay.addRangeCrosshair(line3);
	crosshairOverlay.addRangeCrosshair(line4);
	
	chartPanel.addOverlay(crosshairOverlay);
	chartPanel.addChartMouseListener(new ChartMouseListener() {
	    @Override
	    public void chartMouseMoved(final ChartMouseEvent event) {
	        final Rectangle2D dataArea = chartPanel.getScreenDataArea();
	        final double posY = plot.getRangeAxis().java2DToValue(event.getTrigger().getY(), dataArea, RectangleEdge.LEFT);
	        yCrosshair.setValue(posY);
	    }

	    @Override
	    public void chartMouseClicked(final ChartMouseEvent event) {
	        final Rectangle2D dataArea = chartPanel.getScreenDataArea();
	    	final double posY = plot.getRangeAxis().java2DToValue(event.getTrigger().getY(), dataArea, RectangleEdge.LEFT);
	        double[][] positions = getPhaseShift(y, posY);
	    	line1.setValue(positions[0][1]);
	        line2.setValue(positions[1][1]);
	        line3.setValue(positions[2][1]);
	        line4.setValue(positions[3][1]);
			JOptionPane.showMessageDialog(null, "Phase Shifts " +positions[0][0] +", "+ positions[1][0]+", "+positions[2][0] 
					+", " + positions[3][0], "Error", JOptionPane.ERROR_MESSAGE);
	    }
	});
	    

	add(chartPanel);
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
	
	private double[][] getPhaseShift(double[] dataPoints, double initialY) {
		double[] wantedPhaseShifts = {0, pi/2, pi, 3*pi/2};
		int l = wantedPhaseShifts.length;
		double[] normalizedData = new double[l];
		for(int i=0; i < l;  i++) {
			normalizedData[i] = Math.abs(dataPoints[i]- initialY);
		}
		double startPhaseShift = findMin(normalizedData);
		double[] phaseShifts = new double[wantedPhaseShifts.length];
		for(int i=0; i < l;  i++) {
			phaseShifts[i] = startPhaseShift + wantedPhaseShifts[i];
		}
		int[][] surroundingPoints = new int[wantedPhaseShifts.length][2];
		for(int i=0; i < l;  i++) {
			surroundingPoints[i][0] = (int)Math.floor(phaseShifts[i]);
			surroundingPoints[i][1] = (int)Math.ceil(phaseShifts[i]);
		}
		
		double[][] grayValues = new double[wantedPhaseShifts.length][wantedPhaseShifts.length];
		for(int i=0; i < l;  i++) {
			double y0 = surroundingPoints[i][0];
			double y1 = surroundingPoints[i][1];
			double y = phaseShifts[i];
			double x0 = findIndex(y0, dataPoints);
			double x1 = findIndex(y1, dataPoints);
			grayValues[i][0] = ((x1 - x0)*(y - y0) / (y1 -y0)) + x0;
			grayValues[i][1] = (phaseShifts[i]);
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
	
	private 
	
}
