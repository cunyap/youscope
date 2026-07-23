/* * * * * * * * * * * * * * * * * * * * * * * * * * imageEventToArray  * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
 * This JavaScript function converts an imageEvent to an Array
 * 
 * This script was written by Andreas P. Cuny
 * and is licensed under the GNU GPL.
 * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * */

function imageEventToArray(imageEvent) {

    var width = imageEvent.getWidth();
    var height = imageEvent.getHeight();

    var bytesPerPixel = imageEvent.getBytesPerPixel();
    var bitDepth = imageEvent.getBitDepth();

    var data = imageEvent.getImageData();

    var image = [];

    var maxValue = Math.pow(2, bitDepth) - 1;

    if (bytesPerPixel === 1) {

        for (var y = 0; y < height; y++) {

            image[y] = [];

            for (var x = 0; x < width; x++) {

                image[y][x] = data[y * width + x] & 0xff;
            }
        }

    } else if (bytesPerPixel === 2) {

        var bb = java.nio.ByteBuffer.wrap(data);
        bb.order(java.nio.ByteOrder.LITTLE_ENDIAN);

        for (var y = 0; y < height; y++) {

            image[y] = [];

            for (var x = 0; x < width; x++) {

                var v = bb.getShort() & 0xffff;

                image[y][x] = Math.floor(v * 255 / maxValue);
            }
        }

    }

    return image;
}