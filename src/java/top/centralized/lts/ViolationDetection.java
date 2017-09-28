package top.centralized.lts;

import java.io.*;
import java.util.ArrayList;
import java.util.HashSet;

public class ViolationDetection {
	private HashSet<String> finalFreqSeqs;
	private String tempViolationFileName;
	private String targetDirectory;

	public ViolationDetection(String targetDirectory, HashSet<String> finalFreqSeqs, String tempViolationFileName) {
		this.finalFreqSeqs = finalFreqSeqs;
		this.targetDirectory = targetDirectory;
		this.tempViolationFileName = tempViolationFileName;
	}

	public void detectViolations(File violationFile){
		BufferedWriter out;
		BufferedReader in;

		try {
			out = new BufferedWriter(new FileWriter(violationFile));
			in = new BufferedReader(new FileReader(new File(tempViolationFileName)));
			String str;
			String deviceId = "";
			ArrayList<String> validViolations = new ArrayList<String>();
			while((str = in.readLine()) != null){
				if(str.contains("#")) {
					if(!deviceId.equals("") && validViolations.size() > 0){
						// output valid
						out.write(deviceId);
						out.newLine();
						for(String oneViolation: validViolations){
							out.write(oneViolation);
							out.newLine();
						}
						validViolations.clear();
					}
					deviceId = str;
				}
				else{
					String [] subs = str.split("\\|");
					if(finalFreqSeqs.contains(subs[1]) && (!finalFreqSeqs.contains(subs[0]))) {
						validViolations.add(str);
					}
				}
			}
			if(!deviceId.equals("") && validViolations.size() > 0){
				// output valid
				out.write(deviceId);
				out.newLine();
				for(String oneViolation: validViolations){
					out.write(oneViolation);
					out.newLine();
				}
			}
			out.close();
			in.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
