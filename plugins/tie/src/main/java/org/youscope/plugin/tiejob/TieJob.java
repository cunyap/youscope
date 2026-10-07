package org.youscope.plugin.tiejob;

import java.rmi.RemoteException;

import org.youscope.common.ComponentRunningException;
import org.youscope.common.image.ImageProducer;
import org.youscope.common.job.CompositeJob;

/**
 * Four-camera TIE image acquisition and reconstruction job.
 *
 * @author Jonas Meier - original TIE job concept
 */
public interface TieJob extends CompositeJob, ImageProducer
{
    void setImageDescription(String description) throws RemoteException, ComponentRunningException;
}
