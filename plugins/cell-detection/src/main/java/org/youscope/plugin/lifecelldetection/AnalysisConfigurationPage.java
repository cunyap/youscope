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
/**
 * 
 */
package org.youscope.plugin.lifecelldetection;

import java.awt.BorderLayout;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;

import org.youscope.addon.measurement.MeasurementAddonUIPage;
import org.youscope.clientinterfaces.YouScopeClient;
import org.youscope.clientinterfaces.YouScopeFrame;
import org.youscope.serverinterfaces.YouScopeServer;

/**
 * Wizard page letting the user choose the (optional) quantification algorithm,
 * the (optional) online tracking algorithm, and the additional quantification
 * imaging channels for a cell-detection measurement.
 *
 * The three panels ({@link QuantificationPanel},
 * {@link QuantificationAlgorithmConfigurationPanel},
 * {@link TrackingAlgorithmConfigurationPanel}) are the very ones used by the
 * cell-detection <i>job</i> UI and are typed to
 * {@link CellDetectionJobConfiguration}.
 * To reuse them unchanged, this page keeps a throwaway job-configuration
 * <i>carrier</i>: on {@link #loadData} it is populated from the measurement
 * configuration and the panels are (re)built from it; on {@link #saveData} the
 * panels commit into the carrier and the relevant parts are copied back to the
 * measurement configuration.
 */
class AnalysisConfigurationPage extends MeasurementAddonUIPage<CellDetectionMeasurementConfiguration> {
	private static final long serialVersionUID = 7745032169844529854L;

	private final YouScopeClient client;
	private final YouScopeServer server;
	private YouScopeFrame parentFrame;

	private QuantificationAlgorithmConfigurationPanel quantificationAlgorithmPanel;
	private TrackingAlgorithmConfigurationPanel trackingAlgorithmPanel;
	private QuantificationPanel quantificationImagesPanel;

	/** Carrier used only to drive the job-typed panels. */
	private CellDetectionJobConfiguration carrier = new CellDetectionJobConfiguration();

	AnalysisConfigurationPage(YouScopeClient client, YouScopeServer server) {
		this.client = client;
		this.server = server;
	}

	@Override
	public void createUI(YouScopeFrame parentFrame) {
		this.parentFrame = parentFrame;
		buildPanels();
	}

	/** (Re)builds the three panels from the current carrier. */
	private void buildPanels() {
		removeAll();
		setLayout(new BorderLayout());

		quantificationImagesPanel = new QuantificationPanel(carrier, client, server, parentFrame);
		quantificationAlgorithmPanel = new QuantificationAlgorithmConfigurationPanel(carrier, client, parentFrame);
		trackingAlgorithmPanel = new TrackingAlgorithmConfigurationPanel(carrier, client, parentFrame);

		JTabbedPane tabs = new JTabbedPane();
		tabs.addTab("Quantification Images", wrap(quantificationImagesPanel,
				"Add one imaging job per additional quantification channel. Leave empty to image only the "
						+ "detection channel (burst mode); add one or more to acquire one coherent frame set per cycle."));
		tabs.addTab("Quantification Algorithm", wrap(quantificationAlgorithmPanel,
				"Optionally measure per-cell features. Runs on the detection image, plus any quantification "
						+ "channels above."));
		tabs.addTab("Tracking Algorithm", wrap(trackingAlgorithmPanel,
				"Optionally link cells across frames, adding cell.track.id."));
		add(tabs, BorderLayout.CENTER);
		revalidate();
		repaint();
	}

	private static JPanel wrap(JPanel content, String hint) {
		JPanel p = new JPanel(new BorderLayout());
		JLabel label = new JLabel("<html><i>" + hint + "</i></html>");
		p.add(label, BorderLayout.NORTH);
		p.add(new JScrollPane(content), BorderLayout.CENTER);
		return p;
	}

	@Override
	public void loadData(CellDetectionMeasurementConfiguration configuration) {
		carrier = new CellDetectionJobConfiguration();
		carrier.setQuantificationAlgorithmConfiguration(configuration.getQuantificationAlgorithmConfiguration());
		carrier.setTrackingAlgorithmConfiguration(configuration.getTrackingAlgorithmConfiguration());
		carrier.setJobs(configuration.getQuantificationJobs());
		// If createUI already ran we have a frame and can rebuild now; otherwise
		// createUI will build the panels from this freshly-populated carrier.
		if (parentFrame != null)
			buildPanels();
	}

	@Override
	public boolean saveData(CellDetectionMeasurementConfiguration configuration) {
		if (quantificationImagesPanel == null)
			return true;
		quantificationImagesPanel.commitChanges(carrier);
		quantificationAlgorithmPanel.commitChanges(carrier);
		trackingAlgorithmPanel.commitChanges(carrier);

		configuration.setQuantificationAlgorithmConfiguration(carrier.getQuantificationAlgorithmConfiguration());
		configuration.setTrackingAlgorithmConfiguration(carrier.getTrackingAlgorithmConfiguration());
		configuration.setQuantificationJobs(carrier.getJobs());
		return true;
	}

	@Override
	public void setToDefault(CellDetectionMeasurementConfiguration configuration) {
		// no defaults to seed: all three are optional and start empty/unselected
	}

	@Override
	public String getPageName() {
		return "Quantification & Tracking";
	}
}
