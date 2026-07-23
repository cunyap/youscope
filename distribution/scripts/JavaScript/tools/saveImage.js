/* * * * * * * * * * * * * * * * * * * * * * * * * * saveImage  * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
 * This JavaScript function takes an imageEvent and a file path and saves an image to disk
 * 
 * This script was written by Andreas P. Cuny
 * and is licensed under the GNU GPL.
 * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * */

function saveImage(imageEvent, filename) {

    var BufferedImage = Java.type("java.awt.image.BufferedImage");
    var ImageIO = Java.type("javax.imageio.ImageIO");
    var File = Java.type("java.io.File");

    var width = imageEvent.getWidth();
    var height = imageEvent.getHeight();

    var img = new BufferedImage(
        width,
        height,
        BufferedImage.TYPE_BYTE_GRAY
    );

    img.getRaster().setDataElements(
        0,
        0,
        width,
        height,
        imageEvent.getImageData()
    );

    ImageIO.write(img, "png", new File(filename));
}