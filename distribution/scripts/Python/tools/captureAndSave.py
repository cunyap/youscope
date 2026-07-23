# * * * * * * * * * * * * * * * * * * * * * * * * * * captureAndSave  * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
# * This JavaScript function takes a microscopy image and saves it to disk
# *
# * This script was written by Andreas P. Cuny
# * and is licensed under the GNU GPL.
# * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * */


def captureAndSave(server,
                   channelGroup,
                   channel,
                   exposure,
                   filename):

    imageEvent = takeImage(
        server,
        channelGroup,
        channel,
        exposure
    )

    saveImage(imageEvent, filename)

    return imageEvent