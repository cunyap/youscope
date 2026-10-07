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

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Rectangle;

import javax.swing.text.Element;
import javax.swing.text.ParagraphView;
import javax.swing.text.View;

/**
 * @author langmo
 *
 */
class ScriptEditorParagraphView extends ParagraphView
{
	private static final short NUMBERS_WIDTH = 25;

    public ScriptEditorParagraphView(Element e) 
    {
        super(e);
        short top = 0;
        short left = 0;
        short bottom = 0;
        short right = 0;
        this.setInsets(top, left, bottom, right);
    }

    @Override
    protected void setInsets(short top, short left, short bottom, short right) 
    {
    	
    	super.setInsets(top,(short)(left + NUMBERS_WIDTH + 2), bottom,right);
    }

    @Override
    public void paintChild(Graphics g, Rectangle r, int n) 
    {
        super.paintChild(g, r, n);
        int previousLineCount = getPreviousLineCount();
        g.setColor(Color.LIGHT_GRAY);
        g.fillRect(r.x-getLeftInset(), r.y, NUMBERS_WIDTH, r.height);
        g.setColor(Color.DARK_GRAY);
        String text = Integer.toString(previousLineCount + n + 1);
        int stringWidth = g.getFontMetrics().stringWidth(text);
        g.drawString(text, r.x - getLeftInset() + NUMBERS_WIDTH - stringWidth - 1, r.y + r.height - 5);
    }

    public int getPreviousLineCount()
    {
        int lineCount = 0;
        View parent = this.getParent();
        int count = parent.getViewCount();
        for (int i = 0; i < count; i++) 
        {
            if (parent.getView(i) == this) 
            {
                break;
            }
			lineCount += parent.getView(i).getViewCount();
        }
        return lineCount;
    }
}
