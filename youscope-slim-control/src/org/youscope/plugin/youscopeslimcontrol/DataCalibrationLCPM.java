package org.youscope.plugin.youscopeslimcontrol;


import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.time.format.DateTimeFormatter;
import java.time.LocalDateTime; 
import javax.swing.JOptionPane;

public class DataCalibrationLCPM {
	String path = "/../CalibrationData/calibrationData_" + currentDateTime() + ".txt";
	int intensity;
	double exposure;
	double[][] lcpmCalibrationData;
	public DataCalibrationLCPM(int intensity, double exposure, double[][] lcpmCalibrationData) {
		this.intensity = intensity;
		this.exposure = exposure;
		this.lcpmCalibrationData = lcpmCalibrationData;
		File file = new File(path);
		if (!file.getParentFile().exists()) {
			file.getParentFile().mkdirs();
		}
		else {
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
		
		try (Writer writer = new BufferedWriter(new OutputStreamWriter(
	              new FileOutputStream(path), "utf-8"))) {
	   writer.write("a");
	   writer.write("b");
	}
		
		catch(IOException e) {
			e.printStackTrace();
			JOptionPane.showMessageDialog(null, "Could not write to calibration data", "Error",
					JOptionPane.ERROR_MESSAGE);
		}
	}
	}
 
	private String currentDateTime() {   

	   DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyyMMdd_HH-mm");
	   LocalDateTime now = LocalDateTime.now();
	   String timeStamp = (String) dtf.format(now);
	   return timeStamp;
	  }
	
	}