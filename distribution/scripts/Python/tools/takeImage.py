# * * * * * * * * * * * * * * * * * * * * * * * * * * takeImage  * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
# * This JavaScript function takes an image and returns an imageEvent
# *
# * This script was written by Andreas P. Cuny
# * and is licensed under the GNU GPL.
# * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * */


def takeImage(server,
              channelGroup,
              channel,
              exposure,
              cameraID=None):
    """
    Acquire an image from the microscope.

    Returns
    -------
    ImageEvent
    """

    microscope = server.getMicroscope()

    if cameraID is None:
        camera = microscope.getCameraDevice()
    else:
        camera = microscope.getCameraDevice(cameraID)

    return camera.makeImage(channelGroup,
                            channel,
                            exposure)
