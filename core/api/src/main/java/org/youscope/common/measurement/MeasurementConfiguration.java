/*******************************************************************************
 * Copyright (c) 2017 Moritz Lang.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 * 
 * Contributors:
 *     Moritz Lang - initial API and implementation
 *     Andreas P. Cuny - update API to support post-processors at measurement end
 ******************************************************************************/
/**
 * 
 */
package org.youscope.common.measurement;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map.Entry;

import org.youscope.common.MetadataProperty;
import org.youscope.common.configuration.Configuration;
import org.youscope.common.configuration.ConfigurationException;
import org.youscope.common.microscope.DeviceSetting;
import org.youscope.common.saving.SaveSettingsConfiguration;

import com.thoughtworks.xstream.annotations.XStreamAlias;

/**
 * Superclass of all configurations of measurements.
 * 
 * @author Moritz Lang
 */
public abstract class MeasurementConfiguration implements Configuration {
	/**
	 * Serial version UID.
	 */
	private static final long serialVersionUID = -4103638994655960751L;

	/**
	 * The name of the measurement.
	 */
	@XStreamAlias("name")
	private String name = "unnamed";

	/**
	 * Defines where and how the results of the measurement will be saved. If null,
	 * the results of the measurement will not be saved.
	 */
	@XStreamAlias("save-settings")
	private SaveSettingsConfiguration saveSettings = null;

	/**
	 * Maximal runtime of the measurement in milliseconds. If the maximal runtime is
	 * over, measurement
	 * will be automatically stopped. If -1, measurement will not be automatically
	 * stopped.
	 */
	@XStreamAlias("runtime")
	private int maxRuntime = -1;

	/**
	 * Device Settings which should be activated at the beginning of the
	 * measurement.
	 */
	@XStreamAlias("start-device-settings")
	private DeviceSetting[] deviseSettingsOn = new DeviceSetting[0];

	/**
	 * Device Settings which should be activated at the end of the measurement.
	 */
	@XStreamAlias("end-device-settings")
	private DeviceSetting[] deviseSettingsOff = new DeviceSetting[0];

	/**
	 * Metadata of the measurement, like temperature, species and similar.
	 */
	@XStreamAlias("metadata")
	private final HashMap<String, String> metadataProperties = new HashMap<>();

	/**
	 * Human readable description of the measurement.
	 */
	@XStreamAlias("description")
	private String description = "";

	/**
	 * Type identifiers of post-processors which should be run automatically once
	 * the measurement
	 * finished. Only post-processors whose factory implements
	 * {@code org.youscope.addon.postprocessing.AutomaticPostProcessorFactory} can
	 * be run this way.
	 * <p>
	 * Configurations saved before this field existed simply lack the element, in
	 * which case the field
	 * keeps its initial value and no post-processor runs.
	 * </p>
	 */
	@XStreamAlias("automatic-post-processors")
	private String[] automaticPostProcessors = new String[0];

	/**
	 * Returns the value of the metadata property with the given name, or null if
	 * this property does not exist.
	 * 
	 * @param propertyName The name of the metadata property.
	 * @return Value of the property, or null.
	 * @throws IllegalArgumentException Thrown if propertyName is null or empty.
	 */
	public String getMetadataPropertyValue(String propertyName) throws IllegalArgumentException {
		if (propertyName == null || propertyName.isEmpty())
			throw new IllegalArgumentException();
		return metadataProperties.get(propertyName);
	}

	/**
	 * Returns a human readable free-form description of the measurement, typically
	 * set by the user.
	 * Initially empty string.
	 * 
	 * @return Non-null description of the measurement.
	 */
	public String getDescription() {
		return description;
	}

	/**
	 * Sets a human readable free-form description of the measurement typically
	 * provided by the user.
	 * Set to null to delete description.
	 * 
	 * @param description Description of measurement, or null.
	 */
	public void setDescription(String description) {
		this.description = description == null ? "" : description;
	}

	/**
	 * Returns the metadata property with the given name, or null if this property
	 * does not exist.
	 * 
	 * @param propertyName The name of the metadata property.
	 * @return Metadata property with given name, or null.
	 * @throws IllegalArgumentException Thrown if propertyName is null or empty.
	 */
	public MetadataProperty getMetadataProperty(String propertyName) throws IllegalArgumentException {
		String value = getMetadataPropertyValue(propertyName);
		return value == null ? null : new MetadataProperty(propertyName, value);
	}

	/**
	 * Returns all defined metadata properties.
	 * 
	 * @return List of defined metadata properties.
	 */
	public Collection<MetadataProperty> getMetadataProperties() {
		ArrayList<MetadataProperty> result = new ArrayList<>(metadataProperties.size());
		for (Entry<String, String> entry : metadataProperties.entrySet()) {
			result.add(new MetadataProperty(entry.getKey(), entry.getValue()));
		}
		Collections.sort(result);
		return result;
	}

	/**
	 * Sets the metadata properties of this measurement. Removes all metadata
	 * properties defined before.
	 * 
	 * @param properties Properties to set.
	 * @throws IllegalArgumentException thrown if properties or one element of
	 *                                  properties is null.
	 * @see #clearMetadataProperties()
	 */
	public void setMetadataProperties(Collection<MetadataProperty> properties) throws IllegalArgumentException {
		if (properties == null)
			throw new IllegalArgumentException();
		for (MetadataProperty property : properties) {
			if (property == null)
				throw new IllegalArgumentException();
		}
		metadataProperties.clear();
		for (MetadataProperty property : properties) {
			metadataProperties.put(property.getName(), property.getValue());
		}
	}

	/**
	 * Sets the metadata property to the provided property. If property with the
	 * same name already exists, it is overwritten.
	 * 
	 * @param property Property to set.
	 * @throws IllegalArgumentException thrown if property is null
	 */
	public void setMetadataProperty(MetadataProperty property) throws IllegalArgumentException {
		if (property == null)
			throw new IllegalArgumentException();
		metadataProperties.put(property.getName(), property.getValue());
	}

	/**
	 * Sets the value of the metadata property with the given name.
	 * 
	 * @param propertyName  The name of the metadata property.
	 * @param propertyValue The value of the metadata property.
	 * @throws IllegalArgumentException thrown if either propertyName or
	 *                                  propertyValue are null, or if propertyName
	 *                                  is empty.
	 */
	public void setMetadataProperty(String propertyName, String propertyValue) throws IllegalArgumentException {
		if (propertyName == null || propertyName.isEmpty() || propertyValue == null)
			throw new IllegalArgumentException();
		metadataProperties.put(propertyName, propertyValue);
	}

	/**
	 * Removes all metadata properties.
	 */
	public void clearMetadataProperties() {
		metadataProperties.clear();
	}

	/**
	 * Deletes the metadata property with the given name. Does nothing if the
	 * property does not exist.
	 * 
	 * @param propertyName The name of the metadata property.
	 * @return True if property was deleted, false if property did not exist.
	 * @throws IllegalArgumentException Thrown if propertyName is null or empty.
	 */
	public boolean deleteMetadataProperty(String propertyName) throws IllegalArgumentException {
		if (propertyName == null || propertyName.isEmpty())
			throw new IllegalArgumentException();
		return metadataProperties.remove(propertyName) != null;
	}

	/**
	 * Returns the name of the measurement.
	 * 
	 * @return the name of the measurement.
	 */
	public String getName() {
		return name;
	}

	/**
	 * Sets the name of the measurement.
	 * 
	 * @param name Non-null name of the measurement.
	 * @throws IllegalArgumentException Thrown if name is null.
	 */
	public void setName(String name) throws IllegalArgumentException {
		if (name == null)
			throw new IllegalArgumentException();
		this.name = name;
	}

	/**
	 * Returns the type identifiers of the post-processors which are run
	 * automatically once the
	 * measurement finished.
	 * 
	 * @return Type identifiers of the automatic post-processors. Never null,
	 *         possibly empty.
	 */
	public String[] getAutomaticPostProcessors() {
		return automaticPostProcessors == null ? new String[0] : automaticPostProcessors.clone();
	}

	/**
	 * Sets the post-processors which should run automatically once the measurement
	 * finished.
	 * <p>
	 * Automatic post-processing happens after all measurement data was written, and
	 * must not be able to
	 * influence the outcome of the measurement itself: a post-processor which fails
	 * leaves the acquired
	 * data untouched, and can be run again manually afterwards.
	 * </p>
	 * 
	 * @param typeIdentifiers Type identifiers of the post-processors to run, or
	 *                        null for none.
	 * @throws IllegalArgumentException Thrown if one of the type identifiers is
	 *                                  null or empty.
	 */
	public void setAutomaticPostProcessors(String[] typeIdentifiers) throws IllegalArgumentException {
		if (typeIdentifiers == null) {
			this.automaticPostProcessors = new String[0];
			return;
		}
		for (String typeIdentifier : typeIdentifiers) {
			if (typeIdentifier == null || typeIdentifier.isEmpty())
				throw new IllegalArgumentException("Post-processor type identifiers must not be null or empty.");
		}
		this.automaticPostProcessors = typeIdentifiers.clone();
	}

	/**
	 * Returns true if the post-processor with the given type identifier runs
	 * automatically once the
	 * measurement finished.
	 * 
	 * @param typeIdentifier Type identifier of the post-processor.
	 * @return True if the post-processor runs automatically.
	 */
	public boolean isAutomaticPostProcessor(String typeIdentifier) {
		if (typeIdentifier == null || automaticPostProcessors == null)
			return false;
		for (String current : automaticPostProcessors) {
			if (typeIdentifier.equals(current))
				return true;
		}
		return false;
	}

	/**
	 * Returns the maximal runtime in ms after which the measurement is
	 * automatically stopped.
	 * 
	 * @return the measurementRuntime Maximal runtime of the measurement, or -1 if
	 *         measurement does not have a maximal runtime.
	 */
	public int getMaxRuntime() {
		return maxRuntime;
	}

	/**
	 * Sets the maximal runtime in ms after which the measurement is automatically
	 * stopped.
	 * If the provided value is zero or negative, the maximal runtime is set to -1
	 * to indicate that the measurement is not automatically stopped.
	 * 
	 * @param maxRuntime Maximal runtime of the measurement, or -1 if measurement
	 *                   does not have a maximal runtime.
	 */
	public void setMaxRuntime(int maxRuntime) {
		this.maxRuntime = maxRuntime <= 0 ? -1 : maxRuntime;
	}

	/**
	 * Returns the device settings which should be applied when measurement starts.
	 * 
	 * @return the deviseSettingsOn Device settings applied when measurement starts.
	 */
	public DeviceSetting[] getDeviseSettingsOn() {
		return deviseSettingsOn;
	}

	/**
	 * Sets the device settings which should be applied when measurement starts.
	 * Set to null if no device settings should be applied.
	 * 
	 * @param deviceSettings device settings which should be applied, or null if no
	 *                       settings should be applied.
	 * @throws IllegalArgumentException Thrown if one of the provided device
	 *                                  settings is null.
	 */
	public void setDeviseSettingsOn(DeviceSetting[] deviceSettings) throws IllegalArgumentException {
		if (deviceSettings == null)
			deviceSettings = new DeviceSetting[0];
		for (DeviceSetting setting : deviceSettings) {
			if (setting == null)
				throw new IllegalArgumentException();
		}
		this.deviseSettingsOn = deviceSettings;
	}

	/**
	 * Returns the device settings which should be applied when measurement stops.
	 * 
	 * @return the deviseSettingsOn Device settings applied when measurement stops.
	 */
	public DeviceSetting[] getDeviseSettingsOff() {
		return deviseSettingsOff;
	}

	/**
	 * Sets the device settings which should be applied when measurement stops.
	 * Set to null if no device settings should be applied.
	 * 
	 * @param deviceSettings device settings which should be applied, or null if no
	 *                       settings should be applied.
	 * @throws IllegalArgumentException Thrown if one of the provided device
	 *                                  settings is null.
	 */
	public void setDeviseSettingsOff(DeviceSetting[] deviceSettings) {
		if (deviceSettings == null)
			deviceSettings = new DeviceSetting[0];
		for (DeviceSetting setting : deviceSettings) {
			if (setting == null)
				throw new IllegalArgumentException();
		}
		this.deviseSettingsOff = deviceSettings;
	}

	/**
	 * Defines how the measurement should be saved. Set to null if the measurement
	 * should not be saved.
	 * 
	 * @param saveSettings Definition how the measurement should be saved.
	 */
	public void setSaveSettings(SaveSettingsConfiguration saveSettings) {
		this.saveSettings = saveSettings;
	}

	/**
	 * Returns the definition how the measurement should be saved. Returns null if
	 * the measurement should not be saved.
	 * 
	 * @return Definition how the measurement should be saved.
	 */
	public SaveSettingsConfiguration getSaveSettings() {
		return saveSettings;
	}

	@Override
	public void checkConfiguration() throws ConfigurationException {
		// Automatic post-processing requires the measurement to be saved: a
		// post-processor reads the
		// files of the finished measurement, so without save settings there is nothing
		// to process.
		if (automaticPostProcessors != null && automaticPostProcessors.length > 0 && saveSettings == null) {
			throw new ConfigurationException(
					"Automatic post-processing is configured, but the measurement is not saved. "
							+ "Either choose save settings, or remove the automatic post-processors.");
		}
	}
}
