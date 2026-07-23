/* * * * * * * * * * * * * * * * * * * * * * * * * * takeImage  * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
 * This JavaScript function takes an image and returns an imageEvent
 * 
 * This script was written by Andreas P. Cuny
 * and is licensed under the GNU GPL.
 * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * */


function takeImage(server, channelGroup, channel, exposure, cameraID) {

    var microscope = server.getMicroscope();

    var camera;

    if (cameraID === undefined || cameraID === null)
        camera = microscope.getCameraDevice();
    else
        camera = microscope.getCameraDevice(cameraID);

    return camera.makeImage(channelGroup, channel, exposure);
}