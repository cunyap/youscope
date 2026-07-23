# * * * * * * * * * * * * * * * * * * * * * * * * * * displayImage  * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
# * This JavaScript function takes an imageEvent and displays using matplotlib
# *
# * This script was written by Andreas P. Cuny
# * and is licensed under the GNU GPL.
# * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * */


import matplotlib.pyplot as plt


def displayImage(imageEvent):

    img = toNumpyImage(imageEvent)

    plt.figure(figsize=(6,6))
    plt.imshow(img, cmap="gray")
    plt.axis("off")
    plt.show()