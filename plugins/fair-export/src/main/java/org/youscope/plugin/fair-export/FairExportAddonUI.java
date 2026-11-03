/*******************************************************************************
 * Copyright © 2026 ETH Zürich
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
 *     Andreas P. Cuny - initial API and implementation
 ******************************************************************************/
package org.youscope.plugin.fairexport;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;

import org.youscope.addon.AddonException;
import org.youscope.addon.AddonMetadata;
import org.youscope.addon.AddonUIAdapter;
import org.youscope.clientinterfaces.MetadataCategoryProvider;
import org.youscope.clientinterfaces.YouScopeClient;
import org.youscope.common.saving.MeasurementFileLocations;
import org.youscope.serverinterfaces.YouScopeServer;
import org.youscope.uielements.DynamicPanel;

/**
 * Post-processing dialog for the FAIR export.
 * <p>
 * The dialog does no work itself: it collects options, hands them to
 * {@link FairExportEngine} and
 * displays the result. The same engine runs when a measurement is configured to
 * export automatically,
 * so the two paths cannot diverge.
 * </p>
 * <p>
 * This entry point stays useful even once automatic export exists. Measurements
 * acquired before the
 * plugin existed can be exported; an export can be repeated after correcting
 * metadata or resolving an
 * ambiguous pixel size configuration; and when an unattended export reports a
 * problem, this is where
 * it gets fixed and re-run. Because the export only reads saved files,
 * repeating it is always safe.
 * </p>
 * 
 * @author Andreas P. Cuny
 */
class FairExportAddonUI extends AddonUIAdapter<AddonMetadata> {
	private final MeasurementFileLocations measurementFileLocations;

	private final JCheckBox omeTiffField = new JCheckBox("OME-TIFF with embedded OME-XML", true);
	private final JCheckBox omeZarrField = new JCheckBox("OME-Zarr", false);
	private final JCheckBox roCrateField = new JCheckBox("RO-Crate manifest", true);
	private final JCheckBox useqField = new JCheckBox("useq acquisition plan (feedback/cell-detection only)", false);
	private final JCheckBox rootOnlyCrateField = new JCheckBox("RO-Crate only at store root", false);
	private final javax.swing.JComboBox<String> tiffSplitField = new javax.swing.JComboBox<String>(
			new String[] { "Combined (multi-channel per well)",
					"Per channel (single-channel files, e.g. for Argolight)" });
	private final javax.swing.JComboBox<String> chunkSizeField = new javax.swing.JComboBox<String>(
			new String[] { "512", "1024", "2048 (full plane)" });
	private final JCheckBox shardingField = new JCheckBox("Use sharding (fewer files for large images)", false);
	private final JTextArea resultField = new JTextArea(14, 60);
	private final JProgressBar progressBar = new JProgressBar(0, 100);
	private final JButton exportButton = new JButton("Export");
	private final JButton viewButton = new JButton("Validate / view OME-Zarr...");
	private final JButton napariButton = new JButton("Open in napari...");

	private final org.youscope.plugin.fairexport.LocalDataServer dataServer = new org.youscope.plugin.fairexport.LocalDataServer(
			System.getProperty("user.dir", "."));
	private java.io.File lastOutputFolder = null;

	FairExportAddonUI(YouScopeClient client, YouScopeServer server, MeasurementFileLocations measurementFileLocations)
			throws AddonException {
		super(new FairExportFactory.FairExportMetadata(), client, server);
		this.measurementFileLocations = measurementFileLocations;
		setTitle("FAIR Export");
		setResizable(true);
		setMaximizable(true);
		setShowCloseButton(true);
		setCloseButtonLabel("Close");
		setDescription(getAddonMetadata().getDescription());
	}

	@Override
	protected Component createUI() throws AddonException {
		File measurementFolder = getMeasurementFolder();

		DynamicPanel contentPanel = new DynamicPanel();
		contentPanel.add(new JLabel("Measurement folder:"));
		JTextArea folderField = new JTextArea(
				measurementFolder == null ? "unknown" : measurementFolder.getAbsolutePath());
		folderField.setEditable(false);
		folderField.setLineWrap(true);
		folderField.setOpaque(false);
		contentPanel.add(folderField);

		contentPanel.add(new JLabel("Formats to write:"));
		contentPanel.add(omeTiffField);
		contentPanel.add(omeZarrField);
		contentPanel.add(roCrateField);
		useqField.setOpaque(false);
		useqField.setToolTipText("Write sequence.useq.json / .yaml. Only produced for cell-detection / "
				+ "feedback measurements; a plain scan is skipped with a note.");
		contentPanel.add(useqField);
		rootOnlyCrateField.setOpaque(false);
		rootOnlyCrateField.setToolTipText("By default a minimal RO-Crate is also written into each plate well "
				+ "and image sub-group so the validator is satisfied at any level. Tick for store-root only.");
		contentPanel.add(rootOnlyCrateField);
		contentPanel.add(new JLabel("OME-TIFF layout:"));
		contentPanel.add(tiffSplitField);
		contentPanel.add(new JLabel("OME-Zarr chunk size (pixels):"));
		chunkSizeField.setSelectedIndex(1);
		contentPanel.add(chunkSizeField);
		shardingField.setOpaque(false);
		contentPanel.add(shardingField);

		contentPanel.add(new JLabel("Result:"));
		resultField.setEditable(false);
		resultField.setLineWrap(true);
		resultField.setWrapStyleWord(true);
		resultField.setText("Press Export to analyse the measurement and write the metadata.\n\n"
				+ "Values which cannot be determined unambiguously are reported here and left out of the\n"
				+ "exported metadata rather than guessed.");
		contentPanel.addFill(new JScrollPane(resultField));

		progressBar.setStringPainted(true);
		progressBar.setString("");
		contentPanel.add(progressBar);

		exportButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent event) {
				runExport();
			}
		});
		contentPanel.add(exportButton);

		viewButton.setEnabled(false);
		viewButton.setToolTipText("Serve the exported OME-Zarr locally and open it in the OME-NGFF validator.");
		viewButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent event) {
				openInValidator();
			}
		});
		contentPanel.add(viewButton);

		napariButton.setEnabled(false);
		napariButton.setToolTipText("Open the exported OME-Zarr directly in napari.");
		napariButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent event) {
				openInNapari();
			}
		});
		contentPanel.add(napariButton);

		try {
			getContainingFrame().addFrameListener(new org.youscope.clientinterfaces.YouScopeFrameListener() {
				@Override
				public void frameClosed() {
					dataServer.stop();
				}

				@Override
				public void frameOpened() {
					/* nothing */ }
			});
		} catch (@SuppressWarnings("unused") Exception e) {
			/* idle-timeout still stops the server */ }

		JPanel panel = new JPanel(new BorderLayout());
		panel.add(contentPanel, BorderLayout.CENTER);
		panel.setPreferredSize(new Dimension(640, 520));

		// If this measurement was already exported (e.g. the dialog is opened again
		// after an automatic
		// export ran), enable the validate/view and napari buttons pointing at the
		// existing output
		// folder, so the user can view/validate without re-exporting.
		detectExistingExport(measurementFolder);

		return panel;
	}

	/**
	 * Enables the validate/view and napari buttons when a previous export with
	 * OME-Zarr already exists
	 * for this measurement, without re-running the export. Looks in the default
	 * output sub-folder
	 * ({@code fair-export}) of the measurement folder.
	 * 
	 * @param measurementFolder The measurement folder, or null.
	 */
	private void detectExistingExport(File measurementFolder) {
		if (measurementFolder == null)
			return;
		File outputFolder = new File(measurementFolder, FairExportEngine.DEFAULT_OUTPUT_FOLDER_NAME);
		if (!outputFolder.isDirectory())
			return;
		boolean hasZarr = !org.youscope.plugin.fairexport.OmeZarrTools.findOmeZarrStores(outputFolder).isEmpty();
		if (!hasZarr)
			return;
		lastOutputFolder = outputFolder;
		viewButton.setEnabled(true);
		napariButton.setEnabled(true);
		resultField.setText("A previous export with OME-Zarr was found in:\n" + outputFolder.getAbsolutePath()
				+ "\n\nYou can validate/view it or open it in napari without exporting again. "
				+ "Press Export only to re-write the files (e.g. after correcting metadata).");
		resultField.setCaretPosition(0);
	}

	/**
	 * Runs the export on a background thread. The export walks the whole image
	 * table and will write, so it must never run on the event dispatch thread
	 * because a frozen UI during
	 * a long export looks like a crash and invites the user to kill YouScope
	 * mid-write.
	 */
	private void runExport() {
		final File measurementFolder = getMeasurementFolder();
		if (measurementFolder == null) {
			resultField.setText("The measurement folder could not be determined, so there is nothing to export.");
			return;
		}
		exportButton.setEnabled(false);
		progressBar.setValue(0);
		resultField.setText("Exporting...\n");

		final FairExportEngine.Options options = new FairExportEngine.Options();
		options.writeOmeTiff = omeTiffField.isSelected();
		options.writeOmeZarr = omeZarrField.isSelected();
		options.tiffSplit = tiffSplitField.getSelectedIndex() == 1
				? FairExportEngine.TiffSplit.PER_CHANNEL
				: FairExportEngine.TiffSplit.COMBINED;
		options.zarrChunkSize = chunkSizeField.getSelectedIndex() == 0 ? 512
				: (chunkSizeField.getSelectedIndex() == 2 ? 2048 : 1024);
		options.zarrSharding = shardingField.isSelected();
		options.writeRoCrate = roCrateField.isSelected();
		options.writeUseq = useqField.isSelected();
		options.rootOnlyStoreCrate = rootOnlyCrateField.isSelected();

		Thread worker = new Thread(new Runnable() {
			@Override
			public void run() {
				MetadataCategoryProvider categoryProvider = null;
				Object provider = getClient().getMeasurementMetadataProvider();
				if (provider instanceof MetadataCategoryProvider)
					categoryProvider = (MetadataCategoryProvider) provider;

				final FairExportEngine.Result result = new FairExportEngine(categoryProvider).export(measurementFolder,
						options, new FairExportEngine.ProgressListener() {
							@Override
							public void progress(final String step, final double progress) {
								SwingUtilities.invokeLater(new Runnable() {
									@Override
									public void run() {
										progressBar.setString(step);
										if (progress >= 0)
											progressBar.setValue((int) Math.round(progress * 100));
									}
								});
							}
						});

				SwingUtilities.invokeLater(new Runnable() {
					@Override
					public void run() {
						showResult(result);
						exportButton.setEnabled(true);
					}
				});
			}
		}, "FAIR export");
		worker.setDaemon(true);
		worker.start();
	}

	private void showResult(FairExportEngine.Result result) {
		StringBuilder builder = new StringBuilder();
		if (result.successful)
			builder.append("Export completed.\n");
		else {
			builder.append("Export failed.\n");
			if (result.failure != null) {
				String message = result.failure.getMessage();
				builder.append(message == null || message.isEmpty() ? result.failure.toString() : message);
				builder.append('\n');
			}
		}
		if (result.outputFolder != null)
			builder.append("Output folder: ").append(result.outputFolder.getAbsolutePath()).append('\n');

		if (result.metadata != null) {
			builder.append('\n');
			builder.append("Images (one per well and image save name):\n");
			for (org.youscope.plugin.fairexport.model.ExperimentMetadata.ImageSeries series : result.metadata.series) {
				builder.append("  ").append(series.name)
						.append("   Z=").append(series.sizeZ).append(" C=").append(series.sizeC).append(" T=")
						.append(series.sizeT)
						.append("   pixel size ")
						.append(series.physicalSizeXMicrons == null ? "unknown" : series.physicalSizeXMicrons + " um")
						.append('\n');
			}
		}

		if (!result.warnings.isEmpty()) {
			builder.append('\n').append(result.warnings.size()).append(" warning(s):\n");
			for (String warning : result.warnings)
				builder.append("  - ").append(warning).append('\n');
			builder.append('\n');
			builder.append("Warnings do not mean the export failed. Fix the cause and export again; the\n");
			builder.append("measurement does not need to be repeated.\n");
		}
		lastOutputFolder = result.outputFolder;
		boolean hasZarr = result.outputFolder != null
				&& !org.youscope.plugin.fairexport.OmeZarrTools.findOmeZarrStores(result.outputFolder).isEmpty();
		viewButton.setEnabled(hasZarr);
		napariButton.setEnabled(hasZarr);
		if (hasZarr)
			builder.append("\nOME-Zarr written. Use the buttons below to validate/view it or open it in napari.\n");

		resultField.setText(builder.toString());
		resultField.setCaretPosition(0);
	}

	private void openInValidator() {
		java.io.File store = pickStore();
		if (store == null)
			return;
		try {
			java.io.File serveRoot = store.getParentFile() != null ? store.getParentFile() : lastOutputFolder;
			String base = dataServer.start(serveRoot, 20000);
			String userDir = System.getProperty("user.dir", ".");
			if (org.youscope.plugin.fairexport.OmeZarrTools.isOpenLauncherPreferred(userDir)) {
				String viewerUrl = dataServer.getViewerUrl();
				if (viewerUrl == null) {
					resultField.append("\nThe local server did not report a URL; cannot open the launcher.\n");
					return;
				}
				org.youscope.plugin.fairexport.OmeZarrTools.openInBrowser(viewerUrl);
				resultField.append("\nServing " + serveRoot.getAbsolutePath()
						+ "\nOpened the local viewer/launcher: " + viewerUrl + "\n");
			} else {
				String dataUrl = org.youscope.plugin.fairexport.OmeZarrTools.buildDataUrl(base, store);
				String url = org.youscope.plugin.fairexport.OmeZarrTools.buildValidatorUrl(
						org.youscope.plugin.fairexport.OmeZarrTools.DEFAULT_VALIDATOR_BASE, dataUrl);
				org.youscope.plugin.fairexport.OmeZarrTools.openInBrowser(url);
				resultField.append(
						"\nServing " + store.getName() + " at " + dataUrl + "\nOpened validator: " + url + "\n");
			}
		} catch (Exception e) {
			String m = e.getMessage();
			resultField.append("\nCould not open the OME-Zarr viewer: " + (m == null ? e.toString() : m) + "\n");
		}
	}

	private void openInNapari() {
		java.io.File store = pickStore();
		if (store == null)
			return;
		try {
			org.youscope.plugin.fairexport.NapariLauncher.open(System.getProperty("user.dir", "."), store);
			resultField.append("\nLaunching napari on " + store.getName() + " (first start can take a while)...\n");
		} catch (Exception e) {
			String m = e.getMessage();
			resultField.append("\nCould not launch napari: " + (m == null ? e.toString() : m) + "\n");
		}
	}

	private java.io.File pickStore() {
		if (lastOutputFolder == null) {
			// No export in this session and none detected on disk: let the user point at a
			// folder that
			// already contains an OME-Zarr export (e.g. one produced on another machine).
			java.io.File picked = promptForZarrFolder();
			if (picked == null) {
				resultField.append("\nNo exported OME-Zarr available. Export with OME-Zarr enabled first, "
						+ "or pick a folder that already contains a .ome.zarr store.\n");
				return null;
			}
			lastOutputFolder = picked;
			viewButton.setEnabled(true);
			napariButton.setEnabled(true);
		}
		java.util.List<java.io.File> stores = org.youscope.plugin.fairexport.OmeZarrTools
				.findOmeZarrStores(lastOutputFolder);
		if (stores.isEmpty()) {
			resultField.append("\nNo .ome.zarr store found in " + lastOutputFolder.getAbsolutePath() + ".\n");
			return null;
		}
		if (stores.size() == 1)
			return stores.get(0);
		String[] names = new String[stores.size()];
		for (int i = 0; i < stores.size(); i++)
			names[i] = stores.get(i).getName();
		Object choice = javax.swing.JOptionPane.showInputDialog(viewButton, "Which OME-Zarr store?",
				"OME-Zarr", javax.swing.JOptionPane.QUESTION_MESSAGE, null, names, names[0]);
		if (choice == null)
			return null;
		for (java.io.File st : stores)
			if (st.getName().equals(choice))
				return st;
		return stores.get(0);
	}

	/**
	 * Returns the folder of the measurement to export.
	 */
	/**
	 * Asks the user for a folder that contains an OME-Zarr export (or an export
	 * sub-folder), for the
	 * case where the validate/napari action is used without a fresh export in this
	 * session. Accepts
	 * either a folder that directly holds {@code *.ome.zarr} stores or one whose
	 * {@code fair-export}
	 * sub-folder does.
	 * 
	 * @return the folder to serve, or null if cancelled / nothing suitable was
	 *         chosen.
	 */
	private java.io.File promptForZarrFolder() {
		javax.swing.JFileChooser chooser = new javax.swing.JFileChooser();
		chooser.setFileSelectionMode(javax.swing.JFileChooser.DIRECTORIES_ONLY);
		chooser.setDialogTitle("Select a folder containing an OME-Zarr export");
		if (lastOutputFolder != null && lastOutputFolder.getParentFile() != null)
			chooser.setCurrentDirectory(lastOutputFolder.getParentFile());
		if (chooser.showOpenDialog(viewButton) != javax.swing.JFileChooser.APPROVE_OPTION)
			return null;
		java.io.File chosen = chooser.getSelectedFile();
		if (chosen == null || !chosen.isDirectory())
			return null;
		if (!org.youscope.plugin.fairexport.OmeZarrTools.findOmeZarrStores(chosen).isEmpty())
			return chosen;
		java.io.File sub = new java.io.File(chosen, FairExportEngine.DEFAULT_OUTPUT_FOLDER_NAME);
		if (sub.isDirectory() && !org.youscope.plugin.fairexport.OmeZarrTools.findOmeZarrStores(sub).isEmpty())
			return sub;
		resultField.append("\nNo .ome.zarr store found in " + chosen.getAbsolutePath()
				+ " (nor in its fair-export sub-folder).\n");
		return null;
	}

	private File getMeasurementFolder() {
		if (measurementFileLocations == null)
			return null;
		String folder = measurementFileLocations.getMeasurementBaseFolder();
		return folder == null ? null : new File(folder);
	}
}
