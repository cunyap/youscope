# * * * * * * * * * * * * * * * * * * * * * * * * * * Python Script Demo  * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
# * This Python script demonstrates how to use the YouScope Script Engine with Python to take a microscopy image
# * In the scripting console execute the methods 'captureAndSave.py', 'displayImage.py'. 'imageEventToArray.py', 'saveImage.py' 'takeImage.py'. The functions are saved under scripts/Python/tools
# * 
# * This script was written by Andreas P. Cuny
# * and is licensed under the GNU GPL.
# * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * */


# Take an image and display it. Ajust the arguments to match your configuration.
event = takeImage(youscopeServer, "Channel", "FITC", 20)
displayImage(event)

# Take an image and directly save it to disk. Ajust the arguments to match your configuration.
captureAndSave(youscopeServer, "Channel", "FITC", 100, "path/to/TStest.png");


# Display without Matplotlib but with openCV
import cv2

image = toNumpyImage(event)
cv2.imshow("Image", image)
cv2.waitKey(0)

# Display without Matplotlib but with napari
# Install in PowerShell
#$uv = 'C:\Program Files\YouScope28\uv\uv.exe'
#$py = 'C:\Program Files\YouScope28\cpython-env\Scripts\python.exe'
#& $uv pip install napari --python $py

import napari

image = toNumpyImage(event)
viewer = napari.Viewer()
viewer.add_image(image)
napari.run()

# Save as OME-TIFF
import tifffile as tiff

image = toNumpyImage(event)
tiff.imwrite(
    "image.ome.tif",
    image,
    metadata={
        "axes": "YX"
    }
)
