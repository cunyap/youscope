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
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import org.youscope.addon.AddonException;
import org.youscope.addon.tool.ToolAddonUIAdapter;
import org.youscope.addon.tool.ToolMetadata;
import org.youscope.clientinterfaces.MetadataCategoryProvider;
import org.youscope.clientinterfaces.YouScopeClient;
import org.youscope.serverinterfaces.YouScopeServer;
import org.youscope.uielements.DynamicPanel;

/**
 * Tool variant of the FAIR export, reachable from the Tools menu.
 * <p>
 * Unlike the post-processing dialog, which is bound to a measurement that just
 * ran, this tool takes an
 * arbitrary measurement folder chosen by the user. It is the only path for
 * exporting data acquired
 * before the plugin existed, or on another machine, and for re-exporting after
 * fixing metadata.
 * </p>
 * <p>
 * It runs the identical {@link FairExportEngine} as the post-processor and the
 * automatic export, so
 * the three entry points cannot produce different results.
 * </p>
 * 
 * @author Andreas P. Cuny
 */
public class FairExportTool extends ToolAddonUIAdapter {
	/** Type identifier of the FAIR export tool. */
	public static final String TYPE_IDENTIFIER = "YouScope.FairExport.Tool";

	private final JTextField folderField = new JTextField();
	private final JCheckBox omeTiffField = new JCheckBox("OME-TIFF with embedded OME-XML", true);
	private final JCheckBox omeZarrField = new JCheckBox("OME-Zarr", false);
	private final JCheckBox roCrateField = new JCheckBox("RO-Crate manifest", true);
	private final JCheckBox useqField = new JCheckBox("useq acquisition plan (feedback/cell-detection only)", false);
	private final JCheckBox rootOnlyCrateField = new JCheckBox("RO-Crate only at store root", false);

	// Validator / viewer settings, persisted to graalpy_config.txt.
	private final JTextField validatorBaseField = new JTextField();
	private final JTextField vizPortField = new JTextField();
	private final JTextField idleTimeoutField = new JTextField();
	private final JTextField selfHostedValidatorField = new JTextField();
	private final JCheckBox openLauncherField = new JCheckBox(
			"Open the local launcher (napari-on-click) instead of the hosted validator", false);
	private final JButton saveSettingsButton = new JButton("Save validator/viewer settings");
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

	/** Local CORS server for browser tools; started on demand, stopped on close. */
	private final LocalDataServer dataServer = new LocalDataServer(System.getProperty("user.dir", "."));
	/**
	 * Output folder of the most recent export (or a picked folder) containing
	 * OME-Zarr.
	 */
	private File lastOutputFolder = null;

	/**
	 * Constructor.
	 * 
	 * @param client YouScope client.
	 * @param server YouScope server.
	 * @throws AddonException
	 */
	public FairExportTool(YouScopeClient client, YouScopeServer server) throws AddonException {
		super(getMetadata(), client, server);
		setMaximizable(true);
		setResizable(true);
	}

	static ToolMetadata getMetadata() {
		return new FairExportToolMetadata();
	}

	@Override
	public Component createUI() throws AddonException {
		setTitle("FAIR Export");
		setShowCloseButton(true);

		DynamicPanel contentPanel = new DynamicPanel();
		contentPanel.add(new JLabel("Measurement folder to export:"));

		JPanel folderPanel = new JPanel(new BorderLayout(5, 0));
		folderPanel.setOpaque(false);
		folderPanel.add(folderField, BorderLayout.CENTER);
		JButton browseButton = new JButton("Browse...");
		browseButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent event) {
				chooseFolder();
			}
		});
		folderPanel.add(browseButton, BorderLayout.EAST);
		contentPanel.add(folderPanel);

		contentPanel.add(new JLabel("Formats to write:"));
		omeTiffField.setOpaque(false);
		omeZarrField.setOpaque(false);
		roCrateField.setOpaque(false);
		contentPanel.add(omeTiffField);
		contentPanel.add(omeZarrField);
		useqField.setOpaque(false);
		useqField.setToolTipText("Write sequence.useq.json / .yaml. Only produced for cell-detection / "
				+ "feedback measurements; a plain scan is skipped with a note.");
		contentPanel.add(useqField);

		// RO-Crate options grouped together in a titled box.
		rootOnlyCrateField.setOpaque(false);
		rootOnlyCrateField.setToolTipText("By default a minimal RO-Crate is also written into each plate well "
				+ "and image sub-group, so the OME-NGFF validator is satisfied if you point it at a sub-group. "
				+ "Tick to write the crate only at the store root (spec-compliant).");
		JPanel roCrateBox = new JPanel();
		roCrateBox.setLayout(new javax.swing.BoxLayout(roCrateBox, javax.swing.BoxLayout.Y_AXIS));
		roCrateBox.setBorder(javax.swing.BorderFactory.createTitledBorder("RO-Crate"));
		roCrateBox.setOpaque(false);
		roCrateBox.add(roCrateField);
		roCrateBox.add(rootOnlyCrateField);
		contentPanel.add(roCrateBox);

		contentPanel.add(new JLabel("OME-TIFF layout:"));
		contentPanel.add(tiffSplitField);
		contentPanel.add(new JLabel("OME-Zarr chunk size (pixels):"));
		chunkSizeField.setSelectedIndex(1);
		contentPanel.add(chunkSizeField);
		shardingField.setOpaque(false);
		contentPanel.add(shardingField);

		// Validator / viewer settings: collapsed by default (an expert feature),
		// expandable via a
		// disclosure toggle so the common workflow stays clean.
		final JPanel settingsBox = new JPanel();
		settingsBox.setLayout(new javax.swing.BoxLayout(settingsBox, javax.swing.BoxLayout.Y_AXIS));
		settingsBox.setOpaque(false);
		JPanel settings = new JPanel(new java.awt.GridLayout(0, 2, 6, 3));
		settings.setOpaque(false);
		settings.add(new JLabel("OME-NGFF validator URL:"));
		validatorBaseField.setToolTipText("URL of the OME-NGFF validator web app. Leave as the default "
				+ "unless you host your own; this is NOT the local data server address.");
		settings.add(validatorBaseField);
		settings.add(new JLabel("Local server port (0 = auto):"));
		settings.add(vizPortField);
		settings.add(new JLabel("Idle timeout (seconds, 0 = never):"));
		settings.add(idleTimeoutField);
		settings.add(new JLabel("Self-hosted validator folder (optional):"));
		settings.add(selfHostedValidatorField);
		settingsBox.add(settings);
		openLauncherField.setOpaque(false);
		openLauncherField.setToolTipText("Open the local launcher page (lists stores, napari-on-click, viewer "
				+ "links) instead of sending the browser straight to the validator.");
		settingsBox.add(openLauncherField);
		saveSettingsButton.setToolTipText("Persist these settings to graalpy_config.txt (fairexport_* keys).");
		saveSettingsButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent event) {
				saveAdvancedSettings();
			}
		});
		settingsBox.add(saveSettingsButton);
		settingsBox.setVisible(false);

		final JButton settingsToggle = new JButton("\u25B8 Validator / viewer settings (advanced)");
		settingsToggle.setToolTipText("Show or hide the advanced validator / viewer settings.");
		settingsToggle.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
		settingsToggle.setBorderPainted(false);
		settingsToggle.setContentAreaFilled(false);
		settingsToggle.setFocusPainted(false);
		settingsToggle.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent event) {
				boolean show = !settingsBox.isVisible();
				settingsBox.setVisible(show);
				settingsToggle.setText((show ? "\u25BE" : "\u25B8") + " Validator / viewer settings (advanced)");
				settingsBox.revalidate();
			}
		});
		contentPanel.add(settingsToggle);
		contentPanel.add(settingsBox);
		loadAdvancedSettings();

		contentPanel.add(new JLabel("Result:"));
		resultField.setEditable(false);
		resultField.setLineWrap(true);
		resultField.setWrapStyleWord(true);
		resultField
				.setText("Choose a measurement folder (the one containing information.xml) and press Export.\n\n"
						+ "Values which cannot be determined unambiguously are reported here and left out of the exported "
						+ "metadata rather than guessed.");
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
		viewButton.setToolTipText("Serve the exported OME-Zarr locally and open it in the OME-NGFF validator "
				+ "(which links on to vizarr, neuroglancer and other viewers).");
		viewButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent event) {
				openInValidator();
			}
		});
		contentPanel.add(viewButton);

		napariButton.setEnabled(false);
		napariButton.setToolTipText("Open the exported OME-Zarr directly in napari (needs napari and "
				+ "napari-ome-zarr in the configured Python environment).");
		napariButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent event) {
				openInNapari();
			}
		});
		contentPanel.add(napariButton);

		// Stop the local server when the tool window closes
		try {
			getContainingFrame().addFrameListener(new org.youscope.clientinterfaces.YouScopeFrameListener() {
				@Override
				public void frameClosed() {
					dataServer.stop();
				}

				@Override
				public void frameOpened() {
					// nothing to do
				}
			});
		} catch (@SuppressWarnings("unused") Exception e) {
			// if the frame is not available yet, the idle-timeout still stops the server
		}

		JPanel panel = new JPanel(new BorderLayout());
		panel.add(contentPanel, BorderLayout.CENTER);
		panel.setPreferredSize(new Dimension(640, 560));
		return panel;
	}

	/** Opens the exported OME-Zarr in napari (local path, no server needed). */
	private void openInNapari() {
		File store = pickStore();
		if (store == null)
			return;
		try {
			NapariLauncher.open(System.getProperty("user.dir", "."), store);
			resultField.append("\nLaunching napari on " + store.getName()
					+ " (first start can take a while)...\n");
		} catch (Exception e) {
			String message = e.getMessage();
			resultField.append("\nCould not launch napari: " + (message == null ? e.toString() : message) + "\n");
		}
	}

	/** Resolves which {@code *.ome.zarr} store to act on (asks if several). */
	private File pickStore() {
		if (lastOutputFolder == null) {
			// No export in this session: try to resolve from whatever folder is in the
			// field (the user
			// may have picked a measurement or output folder and pressed Validate/napari
			// directly).
			String folderText = folderField.getText();
			if (folderText != null && !folderText.trim().isEmpty())
				detectExistingExport(new File(folderText.trim()));
		}
		if (lastOutputFolder == null) {
			resultField.append("\nNo exported OME-Zarr available yet. Run an export with OME-Zarr enabled "
					+ "first, or choose a folder that already contains a .ome.zarr store (or a fair-export "
					+ "sub-folder).\n");
			return null;
		}
		java.util.List<File> stores = OmeZarrTools.findOmeZarrStores(lastOutputFolder);
		if (stores.isEmpty()) {
			resultField.append("\nNo .ome.zarr store found in " + lastOutputFolder.getAbsolutePath()
					+ ". Enable OME-Zarr and export again.\n");
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
		for (File s : stores)
			if (s.getName().equals(choice))
				return s;
		return stores.get(0);
	}

	/**
	 * Serves {@link #lastOutputFolder} locally (starting the server if needed) and
	 * opens the OME-NGFF validator pointed at the exported {@code *.ome.zarr}. If
	 * several stores are present, asks which one.
	 */
	private void openInValidator() {
		File store = pickStore();
		if (store == null)
			return;
		try {
			// Root the server at the store's parent so the store is always a direct child
			// of the served
			// folder (the data URL is <base>/<store-name>). This is the fair-export folder
			// in practice,
			// and rooting here rather than at a separately-tracked folder avoids any drift.
			File serveRoot = store.getParentFile() != null ? store.getParentFile() : lastOutputFolder;
			String base = dataServer.start(serveRoot, 20000);
			String userDir = System.getProperty("user.dir", ".");
			if (OmeZarrTools.isOpenLauncherPreferred(userDir)) {
				// Self-hosted launcher: lists the stores with an "Open in napari" button
				// (napari opens
				// like the YouScope button, via the local server) plus
				// validator/vizarr/neuroglancer/vol-e
				// links. Works offline for napari; the web viewers still need internet unless
				// self-hosted.
				String viewerUrl = dataServer.getViewerUrl();
				if (viewerUrl == null) {
					resultField.append("\nThe local server did not report a URL; cannot open the launcher.\n");
					return;
				}
				OmeZarrTools.openInBrowser(viewerUrl);
				resultField.append("\nServing " + serveRoot.getAbsolutePath()
						+ "\nOpened the local viewer/launcher: " + viewerUrl + "\n");
			} else {
				String dataUrl = OmeZarrTools.buildDataUrl(base, store);
				String validatorBase = readValidatorBase();
				String url = OmeZarrTools.buildValidatorUrl(validatorBase, dataUrl);
				OmeZarrTools.openInBrowser(url);
				resultField.append("\nServing " + store.getName() + " at " + dataUrl
						+ "\nOpened validator: " + url + "\n");
			}
		} catch (Exception e) {
			String message = e.getMessage();
			resultField.append("\nCould not open the OME-Zarr viewer: "
					+ (message == null ? e.toString() : message) + "\n");
		}
	}

	/** Reads the configured validator base URL, or the GitHub default. */
	private static String readValidatorBase() {
		String base = FairExportPython.readConfig(System.getProperty("user.dir", "."), "fairexport_validator_base");
		return (base == null || base.trim().isEmpty()) ? OmeZarrTools.DEFAULT_VALIDATOR_BASE : base.trim();
	}

	/**
	 * Loads the validator/viewer settings from graalpy_config.txt into the UI
	 * fields.
	 */
	private void loadAdvancedSettings() {
		String dir = System.getProperty("user.dir", ".");
		String base = FairExportPython.readConfig(dir, "fairexport_validator_base");
		// If the stored value is missing or not actually a validator URL (e.g. a bare
		// host that was
		// mistakenly entered), show the correct default so a re-save heals it.
		validatorBaseField.setText(looksLikeValidator(base) ? base : OmeZarrTools.DEFAULT_VALIDATOR_BASE);
		vizPortField.setText(Integer.toString(FairExportPython.readConfigInt(dir, "fairexport_viz_port", 0)));
		idleTimeoutField.setText(
				Integer.toString(FairExportPython.readConfigInt(dir, "fairexport_viz_idle_timeout_seconds", 1800)));
		String selfHosted = FairExportPython.readConfig(dir, "fairexport_self_hosted_validator");
		selfHostedValidatorField.setText(selfHosted == null ? "" : selfHosted);
		openLauncherField.setSelected(OmeZarrTools.isOpenLauncherPreferred(dir));
	}

	/** Persists the validator/viewer settings back to graalpy_config.txt. */
	private void saveAdvancedSettings() {
		String dir = System.getProperty("user.dir", ".");
		java.util.LinkedHashMap<String, String> entries = new java.util.LinkedHashMap<String, String>();
		// Only persist a validator base that is a real validator URL and differs from
		// the default; an
		// empty value removes the key so the built-in default (GitHub-hosted) is used.
		String base = validatorBaseField.getText().trim();
		if (!looksLikeValidator(base) || base.equals(OmeZarrTools.DEFAULT_VALIDATOR_BASE))
			base = "";
		entries.put("fairexport_validator_base", base);
		entries.put("fairexport_viz_port", sanitizeInt(vizPortField.getText(), "0"));
		entries.put("fairexport_viz_idle_timeout_seconds", sanitizeInt(idleTimeoutField.getText(), "1800"));
		entries.put("fairexport_self_hosted_validator", selfHostedValidatorField.getText().trim());
		entries.put("fairexport_open_launcher", openLauncherField.isSelected() ? "1" : "0");
		try {
			FairExportPython.writeConfig(dir, entries);
			resultField.append("\nSaved validator/viewer settings to graalpy_config.txt.\n");
		} catch (java.io.IOException e) {
			resultField.append(
					"\nCould not save settings: " + (e.getMessage() == null ? e.toString() : e.getMessage()) + "\n");
		}
	}

	/**
	 * A usable validator base is an http(s) URL with a path (e.g.
	 * {@code .../ome-ngff-validator/}); a bare
	 * host such as {@code http://127.0.0.1} is not a validator (it is typically the
	 * data server entered by
	 * mistake). Mirrors the same check in {@code cors_server.py}.
	 */
	private static boolean looksLikeValidator(String base) {
		if (base == null || base.trim().isEmpty())
			return false;
		try {
			java.net.URI u = new java.net.URI(base.trim());
			String scheme = u.getScheme();
			if (scheme == null || !(scheme.equals("http") || scheme.equals("https")) || u.getHost() == null)
				return false;
			String path = u.getPath();
			return path != null && !path.isEmpty() && !path.equals("/");
		} catch (@SuppressWarnings("unused") java.net.URISyntaxException e) {
			return false;
		}
	}

	private static String sanitizeInt(String value, String fallback) {
		if (value == null)
			return fallback;
		String trimmed = value.trim();
		try {
			Integer.parseInt(trimmed);
			return trimmed;
		} catch (@SuppressWarnings("unused") NumberFormatException e) {
			return fallback;
		}
	}

	private void chooseFolder() {
		JFileChooser chooser = new JFileChooser();
		chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		chooser.setDialogTitle("Select measurement folder");
		String current = folderField.getText();
		if (current != null && !current.trim().isEmpty()) {
			File currentFolder = new File(current.trim());
			if (currentFolder.isDirectory())
				chooser.setCurrentDirectory(currentFolder);
		}
		if (chooser.showOpenDialog(exportButton) == JFileChooser.APPROVE_OPTION) {
			folderField.setText(chooser.getSelectedFile().getAbsolutePath());
			detectExistingExport(chooser.getSelectedFile());
		}
	}

	/**
	 * Enables the validate/view and napari buttons when the chosen measurement
	 * folder already has an
	 * OME-Zarr export (in its {@code fair-export} sub-folder, or directly in the
	 * folder if the user
	 * picked the output folder itself), so a previously-exported measurement can be
	 * viewed/validated
	 * without exporting again.
	 * 
	 * @param chosenFolder The folder the user selected.
	 */
	private void detectExistingExport(File chosenFolder) {
		if (chosenFolder == null || !chosenFolder.isDirectory())
			return;
		File candidate = null;
		if (!OmeZarrTools.findOmeZarrStores(chosenFolder).isEmpty())
			candidate = chosenFolder; // user pointed straight at an output folder
		else {
			File sub = new File(chosenFolder, FairExportEngine.DEFAULT_OUTPUT_FOLDER_NAME);
			if (sub.isDirectory() && !OmeZarrTools.findOmeZarrStores(sub).isEmpty())
				candidate = sub;
		}
		if (candidate == null)
			return;
		lastOutputFolder = candidate;
		viewButton.setEnabled(true);
		napariButton.setEnabled(true);
		resultField.setText("A previous OME-Zarr export was found in:\n" + candidate.getAbsolutePath()
				+ "\n\nYou can validate/view it or open it in napari without exporting again. "
				+ "Press Export only to re-write the files.");
		resultField.setCaretPosition(0);
	}

	private void runExport() {
		String folderText = folderField.getText();
		if (folderText == null || folderText.trim().isEmpty()) {
			resultField.setText("Please choose a measurement folder first.");
			return;
		}
		final File measurementFolder = new File(folderText.trim());
		if (!measurementFolder.isDirectory()) {
			resultField.setText("The selected path is not a folder:\n" + measurementFolder.getAbsolutePath());
			return;
		}
		if (!new File(measurementFolder, "information.xml").isFile()) {
			resultField.setText("The selected folder does not contain an information.xml, so it is not a measurement "
					+ "folder saved by YouScope R2022 or later:\n" + measurementFolder.getAbsolutePath()
					+ "\n\nChoose the folder which directly contains information.xml, configuration.csb and the images.");
			return;
		}

		exportButton.setEnabled(false);
		progressBar.setValue(0);
		resultField.setText("Exporting...\n");

		final FairExportEngine.Options options = new FairExportEngine.Options();
		options.writeOmeTiff = omeTiffField.isSelected();
		options.writeOmeZarr = omeZarrField.isSelected();
		options.writeRoCrate = roCrateField.isSelected();
		options.writeUseq = useqField.isSelected();
		options.rootOnlyStoreCrate = rootOnlyCrateField.isSelected();
		options.tiffSplit = tiffSplitField.getSelectedIndex() == 1
				? FairExportEngine.TiffSplit.PER_CHANNEL
				: FairExportEngine.TiffSplit.COMBINED;
		options.zarrChunkSize = chunkSizeField.getSelectedIndex() == 0 ? 512
				: (chunkSizeField.getSelectedIndex() == 2 ? 2048 : 1024);
		options.zarrSharding = shardingField.isSelected();

		Thread worker = new Thread(new Runnable() {
			@Override
			public void run() {
				MetadataCategoryProvider categoryProvider = null;
				Object provider = getClient().getMeasurementMetadataProvider();
				if (provider instanceof MetadataCategoryProvider)
					categoryProvider = (MetadataCategoryProvider) provider;

				final FairExportEngine.Result result = new FairExportEngine(categoryProvider)
						.export(measurementFolder, options, new FairExportEngine.ProgressListener() {
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
		}, "FAIR export tool");
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
			builder.append('\n').append("Images (one per well and image save name):\n");
			for (org.youscope.plugin.fairexport.model.ExperimentMetadata.ImageSeries series : result.metadata.series) {
				builder.append("  ").append(series.name)
						.append("   Z=").append(series.sizeZ).append(" C=").append(series.sizeC).append(" T=")
						.append(series.sizeT)
						.append("   pixel size ")
						.append(series.physicalSizeXMicrons == null ? "unknown" : series.physicalSizeXMicrons + " um")
						.append('\n');
			}
		}

		if (result.completenessSummary != null)
			builder.append('\n').append(result.completenessSummary).append('\n');

		if (!result.warnings.isEmpty()) {
			builder.append('\n').append(result.warnings.size()).append(" warning(s):\n");
			for (String warning : result.warnings)
				builder.append("  - ").append(warning).append('\n');
			builder.append('\n');
			builder.append("Warnings do not mean the export failed. Fix the cause and export again.\n");
		}
		// enable the validator/viewer action if this export produced an OME-Zarr store
		lastOutputFolder = result.outputFolder;
		boolean hasZarr = result.outputFolder != null && !OmeZarrTools.findOmeZarrStores(result.outputFolder).isEmpty();
		viewButton.setEnabled(hasZarr);
		napariButton.setEnabled(hasZarr);
		if (hasZarr)
			builder.append('\n').append("OME-Zarr written - use \"Validate / view OME-Zarr...\" to open it in the "
					+ "OME-NGFF validator and viewers.\n");

		resultField.setText(builder.toString());
		resultField.setCaretPosition(0);
	}

	/**
	 * Metadata of the FAIR export tool.
	 */
	static class FairExportToolMetadata implements ToolMetadata {
		@Override
		public String getName() {
			return "FAIR Export";
		}

		@Override
		public String getDescription() {
			return "Exports any saved measurement folder into community standard formats (OME-TIFF, OME-Zarr, "
					+ "RO-Crate). Use this to export measurements acquired before the FAIR export plugin was "
					+ "installed, or to re-export after correcting metadata.";
		}

		@Override
		public String getTypeIdentifier() {
			return TYPE_IDENTIFIER;
		}

		@Override
		public javax.swing.Icon getIcon() {
			return org.youscope.uielements.ImageLoadingTools.getResourceIcon("icons/database-export.png",
					"FAIR Export");
		}

		@Override
		public String[] getClassification() {
			// Appear directly in the Tools menu (like Focus Score, Stage and Focus
			// Position) rather than
			// nested under a submenu, so users can find FAIR Export without digging.
			return new String[] { "Export" };
		}
	}
}
