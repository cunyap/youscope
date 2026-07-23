/* * * * * * * * * * * * * * * * * * * * * * * * * * captureAndSave  * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
 * This JavaScript utility function takes a microscopy image and saves it to disk
 * 
 * This script was written by Andreas P. Cuny
 * and is licensed under the GNU GPL.
 * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * */

function captureAndSave(server,
                        channelGroup,
                        channel,
                        exposure,
                        filename) {

    var image = takeImage(
        server,
        channelGroup,
        channel,
        exposure
    );

    saveImage(image, filename);

    return image;
}