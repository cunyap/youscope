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
package org.youscope.plugin.cellprofilerquantifier;

import org.youscope.addon.AddonException;
import org.youscope.addon.celldetection.QuantificationAddon;
import org.youscope.addon.component.ComponentAddonFactoryAdapter;
import org.youscope.addon.component.CustomAddonCreator;
import org.youscope.common.PositionInformation;
import org.youscope.common.configuration.ConfigurationException;
import org.youscope.serverinterfaces.ConstructionContext;

/**
 * YouScope addon factory for the CellProfiler (Mode A) quantifier. Registers the
 * addon under the {@link QuantificationAddon} interface, like the CellX / minimal
 * quantifiers.
 */
public class CellProfilerQuantifierAddonFactory extends ComponentAddonFactoryAdapter
{
	private static final CustomAddonCreator<CellProfilerQuantifierConfiguration, QuantificationAddon> CREATOR =
		new CustomAddonCreator<CellProfilerQuantifierConfiguration, QuantificationAddon>()
	{
		@Override
		public QuantificationAddon createCustom(PositionInformation positionInformation,
				CellProfilerQuantifierConfiguration configuration, ConstructionContext constructionContext)
				throws ConfigurationException, AddonException
		{
			try
			{
				return new CellProfilerQuantifierAddon(positionInformation, configuration);
			}
			catch(Exception e)
			{
				throw new AddonException("Could not create CellProfiler quantifier addon.", e);
			}
		}

		@Override
		public Class<QuantificationAddon> getComponentInterface()
		{
			return QuantificationAddon.class;
		}
	};

	/** Constructor: Called by YouScope's plugin loader at server startup. */
	public CellProfilerQuantifierAddonFactory()
	{
		super(CellProfilerQuantifierConfigurationAddon.class, CREATOR,
				CellProfilerQuantifierConfigurationAddon.getMetadata());
	}
}
