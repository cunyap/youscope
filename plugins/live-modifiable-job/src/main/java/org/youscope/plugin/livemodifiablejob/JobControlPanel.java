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
package org.youscope.plugin.livemodifiablejob;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.border.TitledBorder;

import org.youscope.clientinterfaces.YouScopeClient;
import org.youscope.clientinterfaces.YouScopeFrame;
import org.youscope.common.job.JobConfiguration;
import org.youscope.serverinterfaces.YouScopeServer;
import org.youscope.uielements.DynamicPanel;
import org.youscope.uielements.JobsDefinitionPanel;

class JobControlPanel extends DynamicPanel
{
    /**
     * Serial Version UID.
     */
    private static final long serialVersionUID = -2205972694294578191L;
    private final JobHolder jobHolder;
    
    public JobControlPanel(final YouScopeClient client, final YouScopeServer server, final YouScopeFrame frame, final JobHolder jobHolder) throws Exception
    {
        this.jobHolder = jobHolder;
        final JobsDefinitionPanel childJobsField = new JobsDefinitionPanel(client, server, frame);
        final JCheckBox jobEnabledField = new JCheckBox("JobEnabled");
        
        LiveModifiableJob job = jobHolder.getJob();
        if(job == null)
        {
            throw new Exception("Job already uninitialized.");
        }
        JobConfiguration[] childJobConfigs = job.getChildJobConfigurations();
        childJobsField.setJobs(childJobConfigs);
        jobEnabledField.setSelected(job.isEnabled());
        
        setBorder(new TitledBorder("Control of job " + jobHolder.toString()));
        add(jobEnabledField);
        addFill(childJobsField);
        final JButton updateChildJobsButton = new JButton("Update job configuration");
        add(updateChildJobsButton);
        updateChildJobsButton.addActionListener(new ActionListener()
            {                
                @Override
                public void actionPerformed(ActionEvent e)
                {
                    try
                    {
                        updateChildJobs(childJobsField.getJobs());
                    } catch (Exception e1)
                    {
                        client.sendError("Could not update child jobs.", e1);
                    }
                }
            });
        jobEnabledField.addActionListener(new ActionListener()
        {

            @Override
            public void actionPerformed(ActionEvent e)
            {
                try
                {
                    setJobEnabled(jobEnabledField.isSelected());
                } catch (Exception e1)
                {
                    client.sendError("Could not change enable state.", e1);
                }
            }
        });
    }
    
    public boolean isStillValid()
    {
        return this.jobHolder.getJob() != null;
    }
    private void updateChildJobs(JobConfiguration[] jobs) throws Exception
    {
        LiveModifiableJob job = jobHolder.getJob();
        if(job == null)
        {
            throw new Exception("Job already uninitialized.");
        }
        job.setChildJobConfigurations(jobs);
    }
    private void setJobEnabled(final boolean enabled) throws Exception
    {
        LiveModifiableJob job = jobHolder.getJob();
        if(job == null)
        {
            throw new Exception("Job already uninitialized.");
        }
        job.setEnabled(enabled);
    }
    
}
