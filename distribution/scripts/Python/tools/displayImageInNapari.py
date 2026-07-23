event = takeImage(youscopeServer, "Channel", "FITC", 20)
image = toNumpyImage(event)
exec_in_cpython_background(
    "import napari, numpy as np\n"
    "v = napari.Viewer()\n"
    "v.add_image(np.array(image, dtype='uint8'))\n"
    "napari.run()\n",
    image=image)
