# * * * * * * * * * * * * * * * * * * * * * * * * * * toNumpyImage  * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
# * This Python function takes an imageEvent and returns a Numpy array
# *
# * This script was written by Andreas P. Cuny
# * and is licensed under the GNU GPL.
# * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * */


import numpy as np


def toNumpyImage(imageEvent):

    width = imageEvent.getWidth()
    height = imageEvent.getHeight()

    bytesPerPixel = imageEvent.getBytesPerPixel()
    bitDepth = imageEvent.getBitDepth()

    data = bytes(imageEvent.getImageData())

    if bytesPerPixel == 1:

        image = np.frombuffer(
            data,
            dtype=np.uint8
        ).reshape(height, width)

    elif bytesPerPixel == 2:

        image = np.frombuffer(
            data,
            dtype="<u2"      # little-endian uint16
        ).reshape(height, width)

        image = (
            image.astype(np.float32)
            * 255
            / (2**bitDepth - 1)
        ).astype(np.uint8)

    else:
        raise ValueError("Unsupported pixel format")

    if imageEvent.isTransposeY():
        image = np.flipud(image)

    if imageEvent.isTransposeX():
        image = np.fliplr(image)

    if imageEvent.isSwitchXY():
        image = image.T

    return image