package org.youscope.plugin.youscopeslimcontrol.LCPMCalibration;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.time.format.DateTimeFormatter;
import java.time.LocalDateTime; 
import javax.swing.JOptionPane;
/**
 * This class provides the methods to save LCPM Calibration data
 * @author Elisa Garulli
 *
 */
public class CalibrationLCPMData {
	String path = "./CalibrationData/calibrationData_" + currentDateTime() + ".txt";

	/**
	 * Contructor. Saves the important data of the calibration run in the folder CalibrationData
	 * @param intensity
	 * @param exposure
	 * @param lcpmCalibrationData
	 */
	public CalibrationLCPMData(String intensity, String exposure, double[] lcpmCalibrationData) {
		File file = new File(path);
		if (!file.getParentFile().exists()) {
			file.getParentFile().mkdirs();
		}
		if (!file.exists()) {
			try {  
				file.createNewFile();
			}
			catch(IOException e) {
				e.printStackTrace();
				JOptionPane.showMessageDialog(null, "Could not save calibration data", "Error",
						JOptionPane.ERROR_MESSAGE);
			}
		}
		try (BufferedWriter writer = new BufferedWriter (new FileWriter(path))){
			writer.write("##IntensityValue: " + intensity + "\n");
			writer.write("##ExposureValue: " + exposure + "\n");
			writer.write("##CalibrationData:\n");
			writer.write("Grayscale Value, Mean Intensity [radians]\n");
			for(int i=0; i < lcpmCalibrationData.length; i++) {
				writer.write("# " + i + "," + Double.toString(lcpmCalibrationData[i]) + "\n");
			}
		}

		catch(Exception e) {
			e.printStackTrace();
			JOptionPane.showMessageDialog(null, "Could not write to calibration data", "Error",
					JOptionPane.ERROR_MESSAGE);
		}
	}

	/**
	 * Provides the current date and time to be included in the name of the file
	 * @return date and time in the format yyyyMMdd_HH-mm
	 */
	private String currentDateTime() {   
		DateTimeFormatter format = DateTimeFormatter.ofPattern("yyyyMMdd_HH-mm-ss");
		LocalDateTime now = LocalDateTime.now();
		String timeStamp = (String) format.format(now);
		return timeStamp;
	}

}