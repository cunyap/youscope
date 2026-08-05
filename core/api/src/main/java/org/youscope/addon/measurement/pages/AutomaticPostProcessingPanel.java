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
package org.youscope.addon.measurement.pages;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.ServiceLoader;

import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.youscope.addon.AddonException;
import org.youscope.addon.postprocessing.AutomaticPostProcessorFactory;
import org.youscope.addon.postprocessing.ConfigurablePostProcessorFactory;
import org.youscope.addon.postprocessing.ConfigurablePostProcessorFactory.ConfigurationPanel;
import org.youscope.addon.postprocessing.PostProcessorAddonFactory;
import org.youscope.common.measurement.MeasurementConfiguration;

/**
 * A reusable panel offering the user a checkbox for every post-processor which
 * can run automatically
 * once the measurement finished.
 * <p>
 * Extracted so that every measurement type's settings page can offer automatic
 * post-processing by
 * adding this one component and forwarding {@code loadData} / {@code saveData},
 * rather than each page
 * duplicating the ServiceLoader lookup. The list is built from the registered
 * {@link PostProcessorAddonFactory} implementations which also implement
 * {@link AutomaticPostProcessorFactory}, so a newly installed post-processor
 * appears automatically.
 * </p>
 * <p>
 * Type identifiers which are stored in a configuration but whose plugin is not
 * currently installed are
 * preserved across a load/save cycle, so that editing a configuration on a
 * machine lacking a plugin
 * does not silently discard the selection.
 * </p>
 * 
 * @author Andreas P. Cuny
 */
public class AutomaticPostProcessingPanel extends JPanel {
	/**
	 * Serial Version UID.
	 */
	private static final long serialVersionUID = -6602980118759806239L;

	private final ArrayList<JCheckBox> fields = new ArrayList<JCheckBox>();
	private final ArrayList<String> typeIdentifiers = new ArrayList<String>();
	private final ArrayList<String> unknownTypeIdentifiers = new ArrayList<String>();
	private final ArrayList<ConfigurationPanel> configuredPanels = new ArrayList<ConfigurationPanel>();

	/**
	 * Constructor. The panel is populated from the installed post-processors
	 * immediately.
	 */
	public AutomaticPostProcessingPanel() {
		setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
		setOpaque(false);

		JLabel header = new JLabel("Run automatically when the measurement finishes:");
		header.setAlignmentX(LEFT_ALIGNMENT);
		add(header);

		ServiceLoader<PostProcessorAddonFactory> factories = ServiceLoader.load(PostProcessorAddonFactory.class,
				AutomaticPostProcessingPanel.class.getClassLoader());
		for (PostProcessorAddonFactory factory : factories) {
			if (!(factory instanceof AutomaticPostProcessorFactory))
				continue;
			AutomaticPostProcessorFactory automaticFactory = (AutomaticPostProcessorFactory) factory;
			for (String typeIdentifier : factory.getSupportedTypeIdentifiers()) {
				if (!automaticFactory.isSupportingAutomaticExecution(typeIdentifier))
					continue;
				// If the factory contributes its own configuration panel for this identifier
				// (options to
				// be chosen with the protocol), embed that panel instead of a bare checkbox.
				if (factory instanceof ConfigurablePostProcessorFactory) {
					ConfigurablePostProcessorFactory configurable = (ConfigurablePostProcessorFactory) factory;
					if (configurable.isConfigurable(typeIdentifier)) {
						ConfigurationPanel panel = configurable.createConfigurationPanel(typeIdentifier);
						if (panel != null) {
							configuredPanels.add(panel);
							java.awt.Component component = panel.getComponent();
							if (component instanceof javax.swing.JComponent)
								((javax.swing.JComponent) component).setAlignmentX(LEFT_ALIGNMENT);
							add(component);
							continue;
						}
					}
				}
				String name;
				String description;
				try {
					name = factory.getPostProcessorMetadata(typeIdentifier).getName();
					description = factory.getPostProcessorMetadata(typeIdentifier).getDescription();
				} catch (@SuppressWarnings("unused") AddonException e) {
					name = typeIdentifier;
					description = null;
				}
				JCheckBox field = new JCheckBox(name, false);
				field.setOpaque(false);
				field.setAlignmentX(LEFT_ALIGNMENT);
				if (description != null)
					field.setToolTipText("<html><div style=\"width:400px\">" + description.replace("\n", "<br />")
							+ "</div></html>");
				fields.add(field);
				typeIdentifiers.add(typeIdentifier);
				add(field);
			}
		}
		if (fields.isEmpty() && configuredPanels.isEmpty()) {
			JLabel emptyLabel = new JLabel("<html><i>No automatic post-processors are installed.</i></html>");
			emptyLabel.setAlignmentX(LEFT_ALIGNMENT);
			add(emptyLabel);
		}
	}

	/**
	 * Returns true if at least one automatic post-processor is installed, i.e. if
	 * this panel offers any
	 * choice. A page may use this to hide the panel entirely when it would be
	 * empty.
	 * 
	 * @return True if the panel contains at least one checkbox.
	 */
	public boolean hasChoices() {
		return !fields.isEmpty() || !configuredPanels.isEmpty();
	}

	/**
	 * Sets the checkboxes from the given configuration.
	 * 
	 * @param configuration Configuration to read the selection from.
	 */
	public void loadData(MeasurementConfiguration configuration) {
		List<String> selected = Arrays.asList(configuration.getAutomaticPostProcessors());
		for (int i = 0; i < fields.size(); i++)
			fields.get(i).setSelected(selected.contains(typeIdentifiers.get(i)));
		unknownTypeIdentifiers.clear();
		for (String typeIdentifier : selected) {
			if (!typeIdentifiers.contains(typeIdentifier))
				unknownTypeIdentifiers.add(typeIdentifier);
		}
		for (ConfigurationPanel panel : configuredPanels)
			panel.loadData(configuration);
	}

	/**
	 * Writes the current selection into the given configuration. Identifiers whose
	 * plugin is not
	 * installed are preserved.
	 * 
	 * @param configuration Configuration to write the selection to.
	 */
	public void saveData(MeasurementConfiguration configuration) {
		ArrayList<String> selected = new ArrayList<String>();
		for (int i = 0; i < fields.size(); i++) {
			if (fields.get(i).isSelected())
				selected.add(typeIdentifiers.get(i));
		}
		selected.addAll(unknownTypeIdentifiers);
		configuration.setAutomaticPostProcessors(selected.toArray(new String[selected.size()]));
		// Contributed panels persist their own identifiers (with encoded options),
		// preserving whatever
		// this panel just wrote.
		for (ConfigurationPanel panel : configuredPanels)
			panel.saveData(configuration);
	}
}
