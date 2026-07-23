# * * * * * * * * * * * * * * * * * * * * * * * * * * saveImage  * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
# * This JavaScript function takes an imageEvent and a file path and saves an image to disk
# *
# * This script was written by Andreas P. Cuny
# * and is licensed under the GNU GPL.
# * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * */


import imageio.v3 as iio

def saveImage(imageEvent, filename):

    image = toNumpyImage(imageEvent)

    iio.imwrite(filename, image)