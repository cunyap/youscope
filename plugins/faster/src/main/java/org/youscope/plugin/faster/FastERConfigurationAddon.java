/*******************************************************************************
 * Copyright (c) 2026 Andreas P. Cuny.
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
/**
 * 
 */
package org.youscope.plugin.faster;

import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.io.File;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import org.youscope.addon.AddonException;
import org.youscope.addon.component.ComponentAddonUIAdapter;
import org.youscope.addon.component.ComponentMetadataAdapter;
import org.youscope.clientinterfaces.YouScopeClient;
import org.youscope.serverinterfaces.YouScopeServer;

/**
 * Minimal configuration UI for the FastER segmenter: pick the FastER executable,
 * the trained model file, and (optionally) a working directory.
 */
class FastERConfigurationAddon extends ComponentAddonUIAdapter<FastERConfiguration>
{
	private final JTextField binaryField = new JTextField();
	private final JTextField modelField = new JTextField();
	private final JTextField svmField = new JTextField();
	private final JTextField workDirField = new JTextField();

	FastERConfigurationAddon(YouScopeClient client, YouScopeServer server) throws AddonException
	{
		super(getMetadata(), client, server);
	}

	static ComponentMetadataAdapter<FastERConfiguration> getMetadata()
	{
		return new ComponentMetadataAdapter<FastERConfiguration>(
				FastERConfiguration.TYPE_IDENTIFIER,
				FastERConfiguration.class,
				FastERAddon.class,
				"FastER Cell-Detection",
				new String[] { "Cell Detection" },
				"Segments cells with the external FastER binary (headless) and a "
						+ "pre-trained model, producing a label image consumed by the "
						+ "selected quantifier/tracker. Headless mode requires FastER "
						+ "v1.4.7 or higher.",
				"icons/faster-icon.png");
	}

	@Override
	protected Component createUI(FastERConfiguration configuration) throws AddonException
	{
		setTitle("FastER segmenter");
		setResizable(true);

		JPanel panel = new JPanel(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		c.insets = new Insets(4, 4, 4, 4);
		c.fill = GridBagConstraints.HORIZONTAL;

		binaryField.setText(configuration.getFasterBinary());
		modelField.setText(configuration.getFasterModel());
		svmField.setText(configuration.getFasterSvm());
		workDirField.setText(configuration.getWorkDir());

		addRow(panel, c, 0, "FastER binary:", binaryField, false);
		addRow(panel, c, 1, "Trained model (.fastER):", modelField, false);
		addRow(panel, c, 2, "SVM sidecar (.fastER_svm):", svmField, false);
		addRow(panel, c, 3, "Work directory (optional):", workDirField, true);

		c.gridx = 0; c.gridy = 4; c.gridwidth = 3; c.weightx = 1;
		panel.add(new JLabel("<html><i>The .fastER_svm file is written next to the .fastER model on "
				+ "headless export; it is auto-detected when you pick the model. Leave blank to use the "
				+ "&lt;model&gt;_svm sibling.<br>Headless mode requires <b>FastER v1.4.7 or higher</b>.</i></html>"), c);
		c.gridwidth = 1;

		return panel;
	}

	/** When the model file is picked, auto-fill the SVM field with the sibling
	 *  {@code <model>_svm} if it exists and the SVM field is empty. */
	private void autodetectSvm(String modelPath)
	{
		if(modelPath == null || modelPath.trim().isEmpty())
			return;
		if(!svmField.getText().trim().isEmpty())
			return;
		File sib = new File(modelPath.trim() + "_svm");
		if(sib.isFile())
			svmField.setText(sib.getAbsolutePath());
	}

	private void addRow(JPanel panel, GridBagConstraints c, int row, String label,
			final JTextField field, final boolean directory)
	{
		c.gridx = 0;
		c.gridy = row;
		c.weightx = 0;
		panel.add(new JLabel(label), c);

		c.gridx = 1;
		c.weightx = 1;
		panel.add(field, c);

		c.gridx = 2;
		c.weightx = 0;
		JButton browse = new JButton("Browse...");
		browse.addActionListener(evt ->
		{
			JFileChooser chooser = new JFileChooser();
			chooser.setFileSelectionMode(directory ? JFileChooser.DIRECTORIES_ONLY : JFileChooser.FILES_ONLY);
			String cur = field.getText();
			if(cur != null && !cur.trim().isEmpty())
			{
				File f = new File(cur.trim());
				chooser.setCurrentDirectory(f.isDirectory() ? f : f.getParentFile());
			}
			if(chooser.showOpenDialog(panel) == JFileChooser.APPROVE_OPTION)
			{
				field.setText(chooser.getSelectedFile().getAbsolutePath());
				if(field == modelField)
					autodetectSvm(field.getText());
			}
		});
		panel.add(browse, c);
	}

	@Override
	protected void initializeDefaultConfiguration(FastERConfiguration configuration) throws AddonException
	{
		// nothing to seed; paths are user-provided
	}

	@Override
	protected void commitChanges(FastERConfiguration configuration)
	{
		configuration.setFasterBinary(binaryField.getText().trim());
		configuration.setFasterModel(modelField.getText().trim());
		configuration.setFasterSvm(svmField.getText().trim());
		configuration.setWorkDir(workDirField.getText().trim());
	}
}
