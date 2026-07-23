/* * * * * * * * * * * * * * * * * * * * * * * * * * displayImage  * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
 * This JavaScript function takes an imageEvent and displays it to the user in a JFrame
 * 
 * This script was written by Andreas P. Cuny
 * and is licensed under the GNU GPL.
 * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * */

function displayImage(imageEvent) {

    var BufferedImage = Java.type("java.awt.image.BufferedImage");
    var JFrame = Java.type("javax.swing.JFrame");
    var ImageIcon = Java.type("javax.swing.ImageIcon");
    var JLabel = Java.type("javax.swing.JLabel");

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

    var frame = new JFrame("Microscope Image");

    frame.getContentPane().add(
        new JLabel(new ImageIcon(img))
    );

    frame.pack();
    frame.setVisible(true);
}