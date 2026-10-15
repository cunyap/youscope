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

package org.youscope.plugin.argolight;

import org.youscope.addon.microplate.MicroplateConfiguration;
import org.youscope.common.configuration.YSConfigAlias;

import com.thoughtworks.xstream.annotations.XStreamAlias;

/**
 * Microplate configuration whose layout is an Argolight pattern map. Only the
 * name of the {@link ArgolightPlateDefinition} is stored; the actual geometry
 * is
 * (re)loaded from XML by {@link ArgolightMicroplateResource} at runtime, so the
 * saved measurement stays small and layouts can be edited independently.
 *
 * @author Andreas P. Cuny
 */
@YSConfigAlias("Argolight Plate")
@XStreamAlias("argolight-microplate")
public class ArgolightMicroplateConfiguration extends MicroplateConfiguration {
	private static final long serialVersionUID = 1L;

	/** Type identifier of the Argolight microplate component. */
	public static final String TYPE_IDENTIFIER = "YouScope.microplate.ArgolightPlate";

	@XStreamAlias("definition-name")
	private String definitionName = ArgolightPlateManager.DEFAULT_DEFINITION_NAME;

	/** @return name of the referenced Argolight plate definition. */
	public String getDefinitionName() {
		return definitionName;
	}

	/** @param definitionName name of the referenced Argolight plate definition. */
	public void setDefinitionName(String definitionName) {
		this.definitionName = definitionName;
	}

	@Override
	public String getTypeIdentifier() {
		return TYPE_IDENTIFIER;
	}
}
