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
package org.youscope.plugin.cameditor;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSplitPane;

import org.youscope.addon.AddonException;
import org.youscope.addon.tool.ToolAddonUIAdapter;
import org.youscope.addon.tool.ToolMetadata;
import org.youscope.addon.tool.ToolMetadataAdapter;
import org.youscope.clientinterfaces.YouScopeClient;
import org.youscope.serverinterfaces.YouScopeServer;
import org.youscope.plugin.cameditor.model.CameLayoutModel;
import org.youscope.plugin.cameditor.model.ScaleModel;
import org.youscope.plugin.cameditor.ui.CameCanvas;
import org.youscope.plugin.cameditor.ui.CameInspectorPanel;

/**
 * The "Custom Arbitrary Microplate Editor" (CAME) tool: loads a chip image (tif/png/svg/pdf/dxf),
 * auto-detects wells, lets the user set the µm/px scale, draw / move / resize / delete / (de)activate
 * wells with a fine grid and precise numeric inspector, and export a YouScope custom-arbitrary
 * microplate definition (with display-only shapes and inactive/structural positions).
 * <p>
 * It is a separate plugin that depends on the {@code custom-microplates} plugin and writes the same,
 * now-public {@code CustomArbitraryMicroplateDefinition}, so saved plates immediately appear as
 * selectable microplate types (mirroring how {@code cellx} depends on {@code cell-detection}).
 *
 * @author Andreas P. Cuny
 */
class CustomArbitraryMicroplateEditorTool extends ToolAddonUIAdapter
{
	public final static String TYPE_IDENTIFIER = "YouScope.CustomArbitraryMicroplateEditor";

	private final ScaleModel scale = new ScaleModel();
	private final CameLayoutModel model = new CameLayoutModel();

	public CustomArbitraryMicroplateEditorTool(YouScopeClient client, YouScopeServer server) throws AddonException
	{
		super(getMetadata(), client, server);
	}

	static ToolMetadata getMetadata()
	{
		return new ToolMetadataAdapter(TYPE_IDENTIFIER, "Custom Arbitrary Microplate Editor", null,
				"Create a custom arbitrary microplate by loading a chip image or CAD drawing, auto-detecting wells, calibrating the µm/pixel scale, and interactively drawing, resizing and (de)activating wells. Exports a standard YouScope custom microplate definition.",
				"icons/came-icon.png");
	}

	@Override
	public Component createUI() throws AddonException
	{
		setMaximizable(true);
		setResizable(true);
		setTitle("Custom Arbitrary Microplate Editor");
		setShowCloseButton(true);

		final CameCanvas canvas = new CameCanvas(model);
		final JLabel statusBar = new JLabel(" Ready. Load a chip image to begin.");
		statusBar.setBorder(javax.swing.BorderFactory.createEmptyBorder(3, 6, 3, 6));

		CameInspectorPanel inspector = new CameInspectorPanel(canvas, model, scale, new CameInspectorPanel.StatusSink()
		{
			@Override
			public void status(String message)
			{
				statusBar.setText(" " + message);
			}

			@Override
			public void error(String message, Throwable t)
			{
				statusBar.setText(" " + message);
				try
				{
					getClient().sendError(message, t);
				}
				catch(Exception e)
				{
					// fall back to the status bar only
				}
			}
		});

		JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, canvas, inspector);
		split.setResizeWeight(1.0);
		split.setContinuousLayout(true);
		split.setDividerLocation(900);

		JPanel content = new JPanel(new BorderLayout());
		content.add(split, BorderLayout.CENTER);
		content.add(statusBar, BorderLayout.SOUTH);
		content.setPreferredSize(new Dimension(1240, 720));
		return content;
	}
}
