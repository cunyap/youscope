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
package org.youscope.plugin.livemodifiablejob;

import java.rmi.RemoteException;

import org.youscope.common.PositionInformation;
import org.youscope.common.Well;

class JobHolder
{
    private volatile LiveModifiableJob job;
    JobHolder(final LiveModifiableJob job)
    {
        this.job = job;
    }
    public synchronized boolean isJobEqual(final LiveModifiableJob job)
    {
        if(this.job == null)
            return false;
        return this.job == job;
    }
    public synchronized void jobUninitilized()
    {
        job = null;
    }
    public synchronized LiveModifiableJob getJob()
    {
        return job;
    }
    
    @Override
    public String toString()
    {
        PositionInformation position;
        try
        {
            synchronized(this)
            {
                if(job == null)
                    return "uninitialized job";
                position = job.getPositionInformation();
            }
        } catch (@SuppressWarnings("unused") RemoteException e)
        {
            return "unknown job";
        }
        Well well = position.getWell();
        String jobDescription = "Jobs ";
        if(well != null)
            jobDescription += "well " + well.getWellName();
        for(int i=0; i<position.getNumPositions();i++)
        {
            String type = position.getPositionType(i);
            int pos = position.getPosition(i);
            if(i>0 || well != null)
                jobDescription +=", ";
            jobDescription += type +" "+Integer.toString(pos);
        }
        return jobDescription;
    }
}
