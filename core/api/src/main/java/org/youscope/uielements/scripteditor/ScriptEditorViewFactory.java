/*******************************************************************************
 * Copyright (c) 2017 Moritz Lang.
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
 *     Moritz Lang - initial API and implementation
 ******************************************************************************/
/**
 * 
 */
package org.youscope.uielements.scripteditor;

import javax.swing.text.*;

/**
 * @author langmo
 * 
 */
class ScriptEditorViewFactory implements ViewFactory
{

	@Override
	public View create(Element elem)
	{
		String kind = elem.getName();
		if(kind != null)
		{
			if(kind.equals(AbstractDocument.ContentElementName))
			{
				return new LabelView(elem);
			}
			else if(kind.equals(AbstractDocument.ParagraphElementName))
			{
				//return new ScriptEditorParagraphView(elem);
				return new ParagraphView(elem);
			}
			else if(kind.equals(AbstractDocument.SectionElementName))
			{
				return new BoxView(elem, View.Y_AXIS);
			}
			else if(kind.equals(StyleConstants.ComponentElementName))
			{
				return new ComponentView(elem);
			}
			else if(kind.equals(StyleConstants.IconElementName))
			{
				return new IconView(elem);
			}
		}
		// default to text display
		return new LabelView(elem);

	}
}
