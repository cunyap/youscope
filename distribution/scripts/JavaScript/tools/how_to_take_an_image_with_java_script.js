/* * * * * * * * * * * * * * * * * * * * * * * * * * Java Script Demo  * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
 * This JavaScript script demonstrates how to use the YouScope Script Engine with JavaScript to take a microscopy image
 * In the scripting console execute the methods 'captureAndSave.js', 'displayImage.js'. 'imageEventToArray.js', 'saveImage.js' 'takeImage.js'. The functions are saved under scripts/JavaScript/tools
 * 
 * This script was written by Andreas P. Cuny
 * and is licensed under the GNU GPL.
 * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * */

// Take an image and display it. Ajust the arguments to match your configuration.
var event = takeImage(youscopeServer, "Channel", "FITC", 20);
displayImage(event);

// Take an image and directly save it to disk. Ajust the arguments to match your configuration.
captureAndSave(youscopeServer, "Channel", "FITC", 100, "path/to/TStest.png");

