/*******************************************************************************
 * Copyright (c) 2026 Andreas P. Cuny.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 *
 * Contributors:
 *     Andreas P. Cuny - initial API and implementation
 ******************************************************************************/
package org.youscope.plugin.cameditor.ui;

import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.List;
import java.util.prefs.Preferences;

import javax.swing.BorderFactory;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.SwingWorker;

import org.youscope.plugin.cameditor.detect.DetectedWell;
import org.youscope.plugin.cameditor.detect.WellDetector;
import org.youscope.plugin.cameditor.image.ImageSources;
import org.youscope.plugin.cameditor.image.LoadedImage;
import org.youscope.plugin.cameditor.image.dxf.DxfImageLoader;
import org.youscope.plugin.cameditor.io.CsbIo;
import org.youscope.plugin.cameditor.model.CameLayoutModel;
import org.youscope.plugin.cameditor.model.EditableWell;
import org.youscope.plugin.cameditor.model.ScaleModel;
import org.youscope.plugin.custommicroplates.CustomArbitraryMicroplateDefinition;
import org.youscope.plugin.custommicroplates.WellShape;
import org.youscope.uielements.DynamicPanel;

/**
 * The control column beside the {@link CameCanvas}: image loading, detection parameters, scale (with a
 * measure-a-known-distance tool), the fine grid + snap toggles, a precise px/µm numeric inspector for
 * the selected well, and saving to the microplate library or to a {@code .csb} file.
 *
 * @author Andreas P. Cuny
 */
public class CameInspectorPanel extends JScrollPane implements CameCanvas.Listener
{
	private static final long serialVersionUID = 1L;

	/** Simple sink for status/error messages so this panel does not depend on YouScope client APIs. */
	public interface StatusSink { void status(String message); void error(String message, Throwable t); }

	private final CameCanvas canvas;
	private final CameLayoutModel model;
	private final ScaleModel scale;
	private final StatusSink status;

	// Remembers the last directories used for images and .csb files, across restarts.
	private static final Preferences PREFS = Preferences.userRoot().node("org/youscope/plugin/cameditor");
	private static final String KEY_IMAGE_DIR = "lastImageDir";
	private static final String KEY_CSB_DIR = "lastCsbDir";

	// scale / detection
	private final JTextField micronsPerPixelField = new JTextField("1.0");
	private final JTextField thresholdField = new JTextField("100");
	private final JTextField minAreaField = new JTextField("100");
	private final JCheckBox darkForegroundBox = new JCheckBox("Structures are darker than background", true);
	private final JTextField closingField = new JTextField("1");

	// grid (on by default)
	private final JCheckBox showGridBox = new JCheckBox("Show grid", true);
	private final JCheckBox snapBox = new JCheckBox("Snap to grid");
	private final JCheckBox centerGridBox = new JCheckBox("Center grid on selected well");
	private final JTextField gridSpacingField = new JTextField("50");

	// plate
	private final JTextField plateNameField = new JTextField("unnamed");
	private final JComboBox<WellShape> defaultShapeBox = new JComboBox<WellShape>(WellShape.values());

	// per-well inspector
	private final JTextField wellXField = new JTextField();
	private final JTextField wellYField = new JTextField();
	private final JTextField wellWField = new JTextField();
	private final JTextField wellHField = new JTextField();
	private final JLabel wellUmLabel = new JLabel(" ");
	private final JLabel wellIdLabel = new JLabel("(none selected)");
	private final JComboBox<WellShape> wellShapeBox = new JComboBox<WellShape>(WellShape.values());
	private final JCheckBox wellActiveBox = new JCheckBox("Active (imaged)");

	public CameInspectorPanel(CameCanvas canvas, CameLayoutModel model, ScaleModel scale, StatusSink status)
	{
		this.canvas = canvas;
		this.model = model;
		this.scale = scale;
		this.status = status;
		canvas.addListener(this);

		DynamicPanel panel = new DynamicPanel();

		panel.add(section("1. Backdrop image"));
		JButton loadImageButton = new JButton("Load image (tif/png/svg/pdf/dxf)...");
		loadImageButton.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { loadImage(); } });
		panel.add(loadImageButton);

		panel.add(section("2. Scale (\u00b5m per pixel)"));
		panel.add(labeled("\u00b5m / pixel:", micronsPerPixelField));
		micronsPerPixelField.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { applyScaleField(); } });
		JButton measureButton = new JButton("Measure a known distance...");
		measureButton.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { canvas.setMode(CameCanvas.Mode.MEASURE); status.status("Measure: click two points a known distance apart."); } });
		panel.add(measureButton);

		panel.add(section("3. Auto-detect wells"));
		panel.add(labeled("Intensity threshold (0-255):", thresholdField));
		panel.add(labeled("Min area (px):", minAreaField));
		panel.add(labeled("Closing iterations:", closingField));
		panel.add(darkForegroundBox);
		JButton detectButton = new JButton("Detect wells");
		detectButton.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { detect(); } });
		panel.add(detectButton);

		panel.add(section("4. Edit mode"));
		panel.add(buildModePanel());

		panel.add(section("5. Grid"));
		panel.add(showGridBox);
		panel.add(snapBox);
		panel.add(centerGridBox);
		panel.add(labeled("Grid spacing (px):", gridSpacingField));
		showGridBox.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { canvas.setShowGrid(showGridBox.isSelected()); } });
		snapBox.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { canvas.setSnapToGrid(snapBox.isSelected()); } });
		centerGridBox.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { canvas.setCenterGridOnSelection(centerGridBox.isSelected()); } });
		gridSpacingField.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { canvas.setGridSpacingScene(parseDouble(gridSpacingField.getText(), 50)); } });

		panel.add(section("6. Selected well"));
		panel.add(wellIdLabel);
		panel.add(labeled("X (px):", wellXField));
		panel.add(labeled("Y (px):", wellYField));
		panel.add(labeled("Width (px):", wellWField));
		panel.add(labeled("Height (px):", wellHField));
		panel.add(wellUmLabel);
		panel.add(labeled("Shape:", wellShapeBox));
		panel.add(wellActiveBox);
		JButton deleteButton = new JButton("Delete selected well");
		deleteButton.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { canvas.deleteSelected(); } });
		panel.add(deleteButton);
		wireWellInspector();

		panel.add(section("7. Plate & save"));
		panel.add(labeled("Plate name:", plateNameField));
		panel.add(labeled("Default shape:", defaultShapeBox));
		defaultShapeBox.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { model.setDefaultShape((WellShape) defaultShapeBox.getSelectedItem()); } });
		JButton saveLibraryButton = new JButton("Save to microplate library");
		saveLibraryButton.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { save(true); } });
		panel.add(saveLibraryButton);
		JButton saveFileButton = new JButton("Save to .csb file...");
		saveFileButton.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { save(false); } });
		panel.add(saveFileButton);
		JButton openFileButton = new JButton("Open existing .csb...");
		openFileButton.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { openCsb(); } });
		panel.add(openFileButton);

		panel.addFillEmpty();
		setViewportView(panel);
		setPreferredSize(new Dimension(320, 640));
		getVerticalScrollBar().setUnitIncrement(16);
	}

	private JPanel buildModePanel()
	{
		JPanel modePanel = new JPanel(new GridLayout(0, 1));
		JRadioButton select = new JRadioButton("Select / move / resize", true);
		JRadioButton add = new JRadioButton("Add wells (Click and drag)");
		JRadioButton measure = new JRadioButton("Measure");
		ButtonGroup group = new ButtonGroup();
		group.add(select); group.add(add); group.add(measure);
		select.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { canvas.setMode(CameCanvas.Mode.SELECT); } });
		add.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { canvas.setMode(CameCanvas.Mode.ADD); } });
		measure.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { canvas.setMode(CameCanvas.Mode.MEASURE); } });
		modePanel.add(select); modePanel.add(add); modePanel.add(measure);
		return modePanel;
	}

	private void wireWellInspector()
	{
		ActionListener apply = new ActionListener()
		{
			public void actionPerformed(ActionEvent e) { applyWellFields(); }
		};
		wellXField.addActionListener(apply);
		wellYField.addActionListener(apply);
		wellWField.addActionListener(apply);
		wellHField.addActionListener(apply);
		wellShapeBox.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { canvas.setSelectedShape((WellShape) wellShapeBox.getSelectedItem()); updateUmLabel(); } });
		wellActiveBox.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { EditableWell w = canvas.getSelected(); if(w != null) { w.setActive(wellActiveBox.isSelected()); canvas.repaint(); } } });
	}

	// Actions

	private void loadImage()
	{
		JFileChooser chooser = chooserAt(KEY_IMAGE_DIR);
		chooser.setFileFilter(new FileNameExtensionFilter(
				"Chip images (tif, png, jpg, bmp, gif, svg, pdf, dxf)",
				"tif", "tiff", "png", "jpg", "jpeg", "bmp", "gif", "svg", "pdf", "dxf"));
		if(chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION)
			return;
		final File file = chooser.getSelectedFile();
		rememberDir(KEY_IMAGE_DIR, file);
		if(!ImageSources.isSupported(file))
		{
			status.status("Unsupported file type: " + file.getName());
			return;
		}
		status.status("Loading " + file.getName() + "...");
		new SwingWorker<Object, Void>()
		{
			private java.awt.image.BufferedImage image;
			private List<DetectedWell> dxfWells;
			private double knownMpp = -1;

			@Override
			protected Object doInBackground() throws Exception
			{
				if(ImageSources.isDxf(file))
				{
					DxfImageLoader.Result result = new DxfImageLoader().load(file);
					image = result.image.getImage();
					dxfWells = result.wells;
					knownMpp = result.image.getKnownMicronsPerPixel();
				}
				else
				{
					LoadedImage loaded = ImageSources.load(file);
					image = loaded.getImage();
					knownMpp = loaded.getKnownMicronsPerPixel();
				}
				return null;
			}

			@Override
			protected void done()
			{
				try
				{
					get();
					canvas.setBackdrop(image);
					if(knownMpp > 0)
					{
						scale.setMicronsPerPixel(knownMpp);
						micronsPerPixelField.setText(format(knownMpp));
						status.status("Loaded " + file.getName() + " (scale " + format(knownMpp) + " \u00b5m/px from file).");
					}
					else
						status.status("Loaded " + file.getName() + ".");
					if(dxfWells != null && !dxfWells.isEmpty())
					{
						model.setFromDetections(dxfWells, false);
						canvas.repaint();
						status.status("Imported " + dxfWells.size() + " wells from DXF geometry.");
					}
				}
				catch(Exception e)
				{
					status.error("Could not load image.", e);
				}
			}
		}.execute();
	}

	private void detect()
	{
		final java.awt.image.BufferedImage backdrop = canvas.getBackdrop();
		if(backdrop == null)
		{
			status.status("Load a backdrop image first.");
			return;
		}
		final WellDetector.Parameters params = new WellDetector.Parameters(
				(int) parseDouble(thresholdField.getText(), 100),
				(int) parseDouble(minAreaField.getText(), 100),
				darkForegroundBox.isSelected(),
				(int) parseDouble(closingField.getText(), 1));
		status.status("Detecting wells...");
		new SwingWorker<List<DetectedWell>, Void>()
		{
			@Override
			protected List<DetectedWell> doInBackground()
			{
				return new WellDetector().detect(backdrop, params);
			}

			@Override
			protected void done()
			{
				try
				{
					List<DetectedWell> detected = get();
					model.setFromDetections(detected, false);
					canvas.setSelected(null);
					canvas.repaint();
					status.status("Detected " + detected.size() + " wells.");
				}
				catch(Exception e)
				{
					status.error("Detection failed.", e);
				}
			}
		}.execute();
	}

	private void applyScaleField()
	{
		scale.setMicronsPerPixel(parseDouble(micronsPerPixelField.getText(), scale.getMicronsPerPixel()));
		micronsPerPixelField.setText(format(scale.getMicronsPerPixel()));
		updateUmLabel();
	}

	private void applyWellFields()
	{
		EditableWell well = canvas.getSelected();
		if(well == null)
			return;
		well.setBoundsPx(
				parseDouble(wellXField.getText(), well.getXPx()),
				parseDouble(wellYField.getText(), well.getYPx()),
				parseDouble(wellWField.getText(), well.getWidthPx()),
				parseDouble(wellHField.getText(), well.getHeightPx()));
		model.reassignIdentifiers();
		canvas.repaint();
		updateUmLabel();
	}

	private void save(boolean toLibrary)
	{
		model.setName(plateNameField.getText());
		model.setDefaultShape((WellShape) defaultShapeBox.getSelectedItem());
		applyScaleField();
		CustomArbitraryMicroplateDefinition def = model.toDefinition(scale);
		try
		{
			if(toLibrary)
			{
				CsbIo.saveToLibrary(def);
				status.status("Saved \"" + def.getCustomMicroplateName() + "\" to the microplate library (" + def.getNumWells() + " wells).");
			}
			else
			{
				JFileChooser chooser = chooserAt(KEY_CSB_DIR);
				chooser.setFileFilter(new FileNameExtensionFilter("YouScope microplate (.csb)", "csb"));
				chooser.setSelectedFile(new File(def.getCustomMicroplateName() + ".csb"));
				if(chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION)
					return;
				File target = chooser.getSelectedFile();
				rememberDir(KEY_CSB_DIR, target);
				CsbIo.saveToFile(def, target);
				status.status("Saved to " + target.getName() + " (" + def.getNumWells() + " wells).");
			}
		}
		catch(Exception e)
		{
			status.error("Could not save microplate.", e);
		}
	}

	private void openCsb()
	{
		JFileChooser chooser = chooserAt(KEY_CSB_DIR);
		chooser.setFileFilter(new FileNameExtensionFilter("YouScope microplate (.csb)", "csb"));
		if(chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION)
			return;
		File source = chooser.getSelectedFile();
		rememberDir(KEY_CSB_DIR, source);
		try
		{
			CustomArbitraryMicroplateDefinition def = CsbIo.loadFromFile(source);
			applyScaleField();
			model.fromDefinition(def, scale);
			plateNameField.setText(def.getCustomMicroplateName());
			defaultShapeBox.setSelectedItem(def.getDefaultWellShape());
			canvas.setSelected(null);
			canvas.fitToView();
			canvas.repaint();
			status.status("Opened " + def.getCustomMicroplateName() + " (" + def.getNumWells() + " wells).");
		}
		catch(Exception e)
		{
			status.error("Could not open .csb file.", e);
		}
	}

	// CameCanvas.Listener

	@Override
	public void selectionChanged(EditableWell selected)
	{
		boolean has = selected != null;
		wellXField.setEnabled(has);
		wellYField.setEnabled(has);
		wellWField.setEnabled(has);
		wellHField.setEnabled(has);
		wellShapeBox.setEnabled(has);
		wellActiveBox.setEnabled(has);
		if(has)
		{
			wellXField.setText(format(selected.getXPx()));
			wellYField.setText(format(selected.getYPx()));
			wellWField.setText(format(selected.getWidthPx()));
			wellHField.setText(format(selected.getHeightPx()));
			wellShapeBox.setSelectedItem(selected.getShape());
			wellActiveBox.setSelected(selected.isActive());
			wellIdLabel.setText(selected.hasIdentifier()
					? "Well " + new org.youscope.common.Well(Math.max(0, selected.getWellY()), Math.max(0, selected.getWellX())).getWellName()
					: "Well (unassigned)");
		}
		else
		{
			wellIdLabel.setText("(none selected)");
			wellUmLabel.setText(" ");
		}
		updateUmLabel();
	}

	@Override
	public void distanceMeasured(double pixelDistance)
	{
		if(pixelDistance <= 0)
		{
			canvas.clearMeasure();
			return;
		}
		String input = JOptionPane.showInputDialog(this,
				String.format("Measured %.1f px.%nEnter the real distance in micrometres:", pixelDistance),
				"Calibrate scale", JOptionPane.QUESTION_MESSAGE);
		if(input != null)
		{
			double microns = parseDouble(input, -1);
			if(microns > 0)
			{
				scale.setFromMeasurement(pixelDistance, microns);
				micronsPerPixelField.setText(format(scale.getMicronsPerPixel()));
				status.status(String.format("Scale set to %s \u00b5m/px.", format(scale.getMicronsPerPixel())));
				updateUmLabel();
			}
		}
		// clear the two crosshairs + line; stay in Measure mode so another measurement is one click away
		canvas.clearMeasure();
	}

	@Override
	public void modelChanged()
	{
		EditableWell selected = canvas.getSelected();
		if(selected != null)
		{
			wellXField.setText(format(selected.getXPx()));
			wellYField.setText(format(selected.getYPx()));
			wellWField.setText(format(selected.getWidthPx()));
			wellHField.setText(format(selected.getHeightPx()));
		}
		updateUmLabel();
	}

	private void updateUmLabel()
	{
		EditableWell selected = canvas.getSelected();
		if(selected == null)
		{
			wellUmLabel.setText(" ");
			return;
		}
		double mpp = scale.getMicronsPerPixel();
		wellUmLabel.setText(String.format("<html>= %.1f \u00d7 %.1f \u00b5m at %s \u00b5m/px</html>",
				selected.getWidthPx() * mpp, selected.getHeightPx() * mpp, format(mpp)));
	}

	// Helpers
	
	/** Creates a file chooser positioned at the directory last used for {@code prefKey}. */
	private static JFileChooser chooserAt(String prefKey)
	{
		JFileChooser chooser = new JFileChooser();
		String dir = PREFS.get(prefKey, null);
		if(dir != null)
		{
			File d = new File(dir);
			if(d.isDirectory())
				chooser.setCurrentDirectory(d);
		}
		return chooser;
	}

	/** Remembers the directory of {@code file} under {@code prefKey} for next time. */
	private static void rememberDir(String prefKey, File file)
	{
		if(file == null)
			return;
		File dir = file.isDirectory() ? file : file.getParentFile();
		if(dir != null)
			PREFS.put(prefKey, dir.getAbsolutePath());
	}

	private static JLabel section(String title)
	{
		JLabel label = new JLabel(title);
		// Bold, using the default label foreground so it stays readable in both light and dark themes
		label.setFont(label.getFont().deriveFont(java.awt.Font.BOLD));
		label.setBorder(BorderFactory.createEmptyBorder(8, 0, 2, 0));
		return label;
	}

	private static JPanel labeled(String text, java.awt.Component field)
	{
		JPanel row = new JPanel(new java.awt.BorderLayout(6, 0));
		row.add(new JLabel(text), java.awt.BorderLayout.WEST);
		row.add(field, java.awt.BorderLayout.CENTER);
		return row;
	}

	private static double parseDouble(String text, double fallback)
	{
		if(text == null)
			return fallback;
		try
		{
			return Double.parseDouble(text.trim());
		}
		catch(NumberFormatException e)
		{
			return fallback;
		}
	}

	private static String format(double value)
	{
		if(value == Math.rint(value) && !Double.isInfinite(value))
			return Long.toString((long) value);
		return String.format("%.4g", value);
	}
}
