/*******************************************************************************
 * Copyright (c) 2017 Moritz Lang.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 * 
 * Contributors:
 *     Moritz Lang - initial API and implementation
 ******************************************************************************/
package org.youscope.plugin.slimjob;

import org.youscope.clientinterfaces.YouScopeClient;

/**
 * Class to conveniently get the value of the properties belonging to SLIM.
 * @author mlang
 *
 */
public class SlimProperties 
{
	private final YouScopeClient client;
	private final static String ATTENUATION_FACTOR_PROPERTY = "YouScope.SLIM.attenuationFactor";
	private final static String PHASESHIFT0_PROPERTY = "YouScope.SLIMControl.phaseShift0";
	private final static String PHASESHIFT1_PROPERTY = "YouScope.SLIMControl.phaseShift1";
	private final static String PHASESHIFT2_PROPERTY = "YouScope.SLIMControl.phaseShift2";
	private final static String PHASESHIFT3_PROPERTY = "YouScope.SLIMControl.phaseShift3";
	/**
	 * Constructor.
	 * @param client YouScope client.
	 */
	public SlimProperties(YouScopeClient client)
	{
		this.client = client;
	}
	/**
	 * Returns the last identified attenuation factor, or 1 if yet not identified.
	 * @return Last identified attenuation factor.
	 */
	public double getAttenuationFactor()
	{
		return client.getPropertyProvider().getProperty(ATTENUATION_FACTOR_PROPERTY, 1.0);
	}
	/**
	 * Sets the attenuation factor.
	 * @param attenuationFactor Attenuation factor.
	 */
	public void setAttenuationFactor(double attenuationFactor)
	{
		client.getPropertyProvider().setProperty(ATTENUATION_FACTOR_PROPERTY, attenuationFactor);
	}
	
	public void setPhaseShiftMask(int maskID, int phaseShift)
	{
		if (maskID == 0) {
		client.getPropertyProvider().setProperty(PHASESHIFT0_PROPERTY, phaseShift);
		}
		if (maskID == 1) {
		client.getPropertyProvider().setProperty(PHASESHIFT1_PROPERTY, phaseShift);
		}
		if (maskID == 2) {
		client.getPropertyProvider().setProperty(PHASESHIFT2_PROPERTY, phaseShift);
		}
		if (maskID == 3) {
		client.getPropertyProvider().setProperty(PHASESHIFT3_PROPERTY, phaseShift);
		}
	}
	
	public double[] getPhaseShiftMask() {
		 double firstValue = client.getPropertyProvider().getProperty(PHASESHIFT0_PROPERTY, 0.0);
		 double secondValue = client.getPropertyProvider().getProperty(PHASESHIFT1_PROPERTY, 0.0);
		 double thirdValue = client.getPropertyProvider().getProperty(PHASESHIFT2_PROPERTY, 0.0);
		 double forthValue = client.getPropertyProvider().getProperty(PHASESHIFT3_PROPERTY, 0.0);
		 double[] phaseShifts = {firstValue, secondValue, thirdValue, forthValue};
		 return phaseShifts;
		 }
	}

