/*******************************************************************************
 * Copyright (c) 2017 Moritz Lang.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 * 
 * Contributors:
 *     Moritz Lang - initial API and implementation
 *     Andreas P. Cuny - update API to support tube lens and additional magnification
 ******************************************************************************/
/**
 * 
 */
package org.youscope.common.microscope;

import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * @author Moritz Lang
 * 
 */
public interface PixelSize extends Remote {
	/**
	 * Returns the ID of the pixel size setting.
	 * 
	 * @return The ID of the pixel size setting.
	 * @throws RemoteException
	 */
	public String getPixelSizeID() throws RemoteException;

	/**
	 * Returns all device settings corresponding to this pixel size.
	 * 
	 * @return Set of device settings necessary for the pixel size setting to get
	 *         active.
	 * @throws RemoteException
	 */
	DeviceSetting[] getPixelSizeSettings() throws RemoteException;

	/**
	 * Sets all device settings corresponding to this pixel size. All previously set
	 * settings get deleted.
	 * 
	 * @param newSettings device settings which have to be active for this pixel
	 *                    size setting to be actual.
	 * @throws MicroscopeLockedException
	 * @throws SettingException
	 * @throws RemoteException
	 */
	void setPixelSizeSettings(DeviceSetting[] newSettings)
			throws MicroscopeLockedException, SettingException, RemoteException;

	/**
	 * Adds a setting to the list of settings.
	 * 
	 * @param setting Setting to add.
	 * @throws MicroscopeLockedException
	 * @throws SettingException
	 * @throws RemoteException
	 */
	void addPixelSizeSetting(DeviceSetting setting) throws MicroscopeLockedException, SettingException, RemoteException;

	/**
	 * Returns the pixel size in micro meters.
	 * 
	 * @return Pixel size in mico meters.
	 * @throws RemoteException
	 */
	double getPixelSize() throws RemoteException;

	/**
	 * Sets the pixel size in micro meters.
	 * 
	 * @param pixelSize Pixel size in mico meters.
	 * @throws MicroscopeLockedException
	 * @throws SettingException
	 * @throws RemoteException
	 */
	void setPixelSize(double pixelSize) throws MicroscopeLockedException, SettingException, RemoteException;

	// -------------------------------------------------------------------------------------------------
	// Optional magnification components.
	//
	// A pixel size can optionally record the three physical quantities it is
	// derived from. When all
	// three are present, getPixelSize() equals cameraPixelPitch /
	// (objectiveMagnification *
	// additionalMagnification), and the stored value is kept in sync. When any is
	// absent (null), the
	// components are considered unknown and getPixelSize() keeps whatever value was
	// set directly -- so
	// existing configurations, which have no components, behave exactly as before.
	//
	// Components are Double (not double) so that "unknown" is representable and
	// cannot be confused with
	// a legitimate zero.
	// -------------------------------------------------------------------------------------------------

	/**
	 * Returns the physical pixel pitch of the camera in micro meters, or null if
	 * unknown.
	 * 
	 * @return Camera pixel pitch in micro meters, or null.
	 * @throws RemoteException
	 */
	Double getCameraPixelPitchMicrons() throws RemoteException;

	/**
	 * Sets the physical pixel pitch of the camera in micro meters. Set to null to
	 * mark it unknown.
	 * 
	 * @param pitchMicrons Camera pixel pitch in micro meters, or null.
	 * @throws MicroscopeLockedException
	 * @throws SettingException
	 * @throws RemoteException
	 */
	void setCameraPixelPitchMicrons(Double pitchMicrons)
			throws MicroscopeLockedException, SettingException, RemoteException;

	/**
	 * Returns the objective magnification (e.g. 40 for a 40x objective), or null if
	 * unknown.
	 * 
	 * @return Objective magnification, or null.
	 * @throws RemoteException
	 */
	Double getObjectiveMagnification() throws RemoteException;

	/**
	 * Sets the objective magnification. Set to null to mark it unknown.
	 * 
	 * @param magnification Objective magnification, or null.
	 * @throws MicroscopeLockedException
	 * @throws SettingException
	 * @throws RemoteException
	 */
	void setObjectiveMagnification(Double magnification)
			throws MicroscopeLockedException, SettingException, RemoteException;

	/**
	 * Returns the additional magnification in the light path (tube lens,
	 * magnification changer, ...),
	 * or null if unknown. A value of 1.0 means no additional magnification.
	 * 
	 * @return Additional magnification, or null.
	 * @throws RemoteException
	 */
	Double getAdditionalMagnification() throws RemoteException;

	/**
	 * Sets the additional magnification in the light path. Set to null to mark it
	 * unknown.
	 * 
	 * @param magnification Additional magnification, or null.
	 * @throws MicroscopeLockedException
	 * @throws SettingException
	 * @throws RemoteException
	 */
	void setAdditionalMagnification(Double magnification)
			throws MicroscopeLockedException, SettingException, RemoteException;

	/**
	 * Returns true if all three magnification components are set, in which case the
	 * pixel size is
	 * derived from them and is read-only in the user interface.
	 * 
	 * @return True if the pixel size is derived from its components.
	 * @throws RemoteException
	 */
	boolean hasMagnificationComponents() throws RemoteException;
}
