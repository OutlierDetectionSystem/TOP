package top.utils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;


public class FilterResults {
	public static void reformatViolations(File inputFile, File outputFile, HashMap<String, String> metaDataMapping) {
		try {
			BufferedReader in = new BufferedReader(new FileReader(inputFile));

			String str;
			HashMap<String, Integer> sequence = new HashMap<String, Integer>();
			while ((str = in.readLine()) != null) {
				if (str.contains("#")) {
					continue;
				} else {
					if (sequence.containsKey(str))
						sequence.put(str, sequence.get(str) + 1);
					else {
						sequence.put(str, 1);
					}
				}
			}
			in.close();
			in = new BufferedReader(new FileReader(inputFile));
			BufferedWriter out = new BufferedWriter(new FileWriter(outputFile));
			String strId = "";
			while ((str = in.readLine()) != null) {
				if (str.contains("#")) {
					strId = str;
				} else {
					if (sequence.get(str) < 10 && !(str.contains("77") || str.contains("78"))) {
						out.write(strId);
						out.newLine();
						out.write(str);
						out.newLine();
						sequence.put(str, 100);
						String[] subStr = str.split("\\|");
						// parse violation sequence
						out.write("Sequence:");
						out.newLine();
						String originalMeta = "";
						for (String tempStr : subStr[0].split(",")) {
							out.write(metaDataMapping.get(tempStr) + "\t");
							originalMeta += metaDataMapping.get(tempStr) + " ,";
						}

						out.newLine();
						out.newLine();
						// parse lts sequence
						out.write("Violates Sequence:");
						out.newLine();
						String violationMeta = "";
						for (String tempStr : subStr[1].split(",")) {
							out.write(metaDataMapping.get(tempStr) + "\t");
							violationMeta += metaDataMapping.get(tempStr) + " ,";
						}
						out.newLine();
						out.newLine();
						if (originalMeta.length() > 0)
							originalMeta = originalMeta.substring(0, originalMeta.length() - 1);
						if (violationMeta.length() > 0)
							violationMeta = violationMeta.substring(0, violationMeta.length() - 1);

					}
				}
			}
			in.close();
			out.close();
		} catch (IOException e) {
			e.getStackTrace();
		}
	}
}
