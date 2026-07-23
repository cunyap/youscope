# * * * * * * * * * * * * * * * * * * * * * * * * * * saveRaw16  * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
# * This JavaScript function takes an imageEvent and a file path and saves an image to disk as raw 16bit image
# *
# * This script was written by Andreas P. Cuny
# * and is licensed under the GNU GPL.
# * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * */


def saveRaw16(imageEvent, filename):

    data = bytes(imageEvent.getImageData())

    image = np.frombuffer(
        data,
        dtype="<u2"
    ).reshape(
        imageEvent.getHeight(),
        imageEvent.getWidth()
    )

    iio.imwrite(filename, image)