package org.youscope.plugin.tiejob;

import java.rmi.RemoteException;
import java.util.ArrayList;

import org.youscope.common.ComponentRunningException;
import org.youscope.common.ExecutionInformation;
import org.youscope.common.MeasurementContext;
import org.youscope.common.PositionInformation;
import org.youscope.common.image.ImageEvent;
import org.youscope.common.image.ImageListener;
import org.youscope.common.image.ImageProducer;
import org.youscope.common.job.CompositeJobAdapter;
import org.youscope.common.job.Job;
import org.youscope.common.job.JobException;
import org.youscope.common.microscope.Microscope;

/**
 * Runtime implementation of the modernized TIE job.
 *
 * <p>The job deliberately composes the existing ImagingJob. This reuses
 * YouScope's existing four-camera parallel acquisition implementation rather
 * than introducing another camera-control path.</p>
 *
 * @author Jonas Meier - original TIE job concept
 */
class TieJobImpl extends CompositeJobAdapter implements TieJob, ImageListener
{
    private static final long serialVersionUID = 1L;

    private final ArrayList<ImageListener> imageListeners = new ArrayList<ImageListener>();
    private volatile String imageDescription;
    private volatile String[] cameras = new String[0];
    private volatile double dzNear = 0.5e-6;
    private volatile double dzFar = 1.0e-6;

    private final ImageEvent<?>[] acquiredImages = new ImageEvent<?>[TieJobConfiguration.NUM_CAMERAS];

    TieJobImpl(PositionInformation positionInformation) throws RemoteException
    {
        super(positionInformation);
    }

    void setCameras(String[] cameras) throws ComponentRunningException
    {
        assertRunning();
        if (cameras == null || cameras.length != TieJobConfiguration.NUM_CAMERAS)
            throw new IllegalArgumentException("TIE requires exactly four cameras.");
        this.cameras = cameras.clone();
    }

    void setDefocusDistances(double dzNear, double dzFar) throws ComponentRunningException
    {
        assertRunning();
        this.dzNear = dzNear;
        this.dzFar = dzFar;
    }

    @Override
    public void initializeJob(Microscope microscope, MeasurementContext measurementContext)
            throws JobException, InterruptedException, RemoteException
    {
        super.initializeJob(microscope, measurementContext);
        Job[] jobs = getJobs();
        if (jobs == null || jobs.length != 1)
            throw new JobException("TIE requires exactly one ImagingJob child for four-camera acquisition.");
        if (!(jobs[0] instanceof ImageProducer))
            throw new JobException("TIE acquisition child is not an image producer.");
        ((ImageProducer) jobs[0]).addImageListener(this);
        clearImages();
    }

    @Override
    public void uninitializeJob(Microscope microscope, MeasurementContext measurementContext)
            throws JobException, InterruptedException, RemoteException
    {
        Job[] jobs = getJobs();
        if (jobs != null && jobs.length == 1 && jobs[0] instanceof ImageProducer)
            ((ImageProducer) jobs[0]).removeImageListener(this);
        clearImages();
        super.uninitializeJob(microscope, measurementContext);
    }

    @Override
    public void imageMade(ImageEvent<?> image) throws RemoteException
    {
        if (image == null)
            return;
        String camera = image.getCamera();
        for (int i = 0; i < cameras.length; i++)
        {
            if (cameras[i] != null && cameras[i].equals(camera))
            {
                synchronized (acquiredImages)
                {
                    acquiredImages[i] = image;
                }
                return;
            }
        }
    }

    @Override
    public String getImageDescription()
    {
        return imageDescription == null ? "TIE axial intensity derivative" : imageDescription;
    }

    @Override
    public void addImageListener(ImageListener listener)
    {
        if (listener == null)
            return;
        synchronized (imageListeners)
        {
            imageListeners.add(listener);
        }
    }

    @Override
    public void removeImageListener(ImageListener listener)
    {
        if (listener == null)
            return;
        synchronized (imageListeners)
        {
            imageListeners.remove(listener);
        }
    }

    @Override
    public void runJob(ExecutionInformation executionInformation, Microscope microscope,
            MeasurementContext measurementContext) throws JobException, InterruptedException, RemoteException
    {
        Job[] jobs = getJobs();
        if (jobs == null || jobs.length != 1)
            throw new JobException("TIE requires exactly one ImagingJob child.");

        clearImages();
        jobs[0].executeJob(executionInformation, microscope, measurementContext);

        ImageEvent<?>[] images = getImages();
        for (int i = 0; i < images.length; i++)
            if (images[i] == null)
                throw new JobException("Parallel TIE acquisition did not return an image for camera " + (i + 1) + " (" + cameras[i] + ").");

        final ImageEvent<?> tieImage;
        try
        {
            tieImage = TieHelper.calculateTieDerivativePreview(images, dzNear, dzFar);
        }
        catch (Exception e)
        {
            throw new JobException("Could not calculate the TIE axial intensity derivative preview.", e);
        }

        tieImage.setExecutionInformation(executionInformation);
        tieImage.setPositionInformation(getPositionInformation());
        tieImage.setCreationRuntime(measurementContext.getMeasurementRuntime());
        tieImage.setCamera(cameras[1]);
        sendImageToListeners(tieImage);
    }

    private void clearImages()
    {
        synchronized (acquiredImages)
        {
            for (int i = 0; i < acquiredImages.length; i++)
                acquiredImages[i] = null;
        }
    }

    private ImageEvent<?>[] getImages()
    {
        synchronized (acquiredImages)
        {
            return acquiredImages.clone();
        }
    }

    private void sendImageToListeners(ImageEvent<?> image)
    {
        synchronized (imageListeners)
        {
            for (int i = 0; i < imageListeners.size(); i++)
            {
                try
                {
                    imageListeners.get(i).imageMade(image);
                }
                catch (RemoteException e)
                {
                    imageListeners.remove(i);
                    i--;
                }
            }
        }
    }

    @Override
    protected String getDefaultName()
    {
        return "TIE job";
    }

    @Override
    public int getNumberOfImages()
    {
        return 1;
    }

    @Override
    public void setImageDescription(String description) throws ComponentRunningException
    {
        assertRunning();
        imageDescription = description;
    }
}
