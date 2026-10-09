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
 *     Moritz Lang - initial API and implementation
 ******************************************************************************/
/**
 * 
 */
package org.youscope.starter;

import java.awt.BorderLayout;
import java.awt.Dimension;

import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

/**
 * @author langmo
 */
class ErrorConsumer
{
    static void consumeException(String description, Exception exception)
    {
    	exception.printStackTrace();
        JPanel panel = new JPanel(new BorderLayout());
        panel.add(new JLabel(description), BorderLayout.NORTH);
        String message = "";
        Throwable iter = exception;
        while(iter != null)
        {
            message += iter.getClass().getSimpleName() + ": " + iter.getMessage() + "\n";
            iter = iter.getCause();
        }
        if(exception.getStackTrace() != null)
        {
        	message += "\nStack Trace:";
        	for(StackTraceElement element: exception.getStackTrace())
        	{
        		if(element != null && !element.isNativeMethod() && element.getFileName() != null)
        			message+="\n"+element.getFileName()+", Line "+element.getLineNumber();
        	}
        }
        JTextArea exceptionDescription = new JTextArea(message);
        exceptionDescription.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(exceptionDescription);
        scrollPane.setPreferredSize(new Dimension(700, 200));
        panel.add(scrollPane, BorderLayout.CENTER);
        JOptionPane.showMessageDialog(null, panel, "Error while starting YouScope", JOptionPane.ERROR_MESSAGE);
        System.exit(1);
    }
}
