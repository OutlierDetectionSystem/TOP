package top.centralized.lts;

import top.parameterspace.LocalParameterSpace;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import top.inputs.InputSequence;
import top.core.lts.local.noviolation.LocalFSDetection;
import top.utils.FileUtile;
import top.utils.Toolbox;

public class GlobalFSDetection {
	private LocalParameterSpace localParameterSpace;
	private ArrayList<String> inputStringArray;

	public GlobalFSDetection(int minSupportLocal, int itemGap, int seqGap,
			List<String> inputStringArray) {
		this.localParameterSpace = new LocalParameterSpace(minSupportLocal, itemGap, seqGap);
		this.inputStringArray = (ArrayList<String>) inputStringArray;
	}

	/**
	 * deal with each String and generate frequent sequence
	 *
	 */
	public void generateLocalFrequentSequences(HashSet<String> globalFrequentElements) {
		try {
			BufferedWriter out = new BufferedWriter(
					new FileWriter(new File("localFS-lts-" + localParameterSpace.getMinLocalSupport() + ".txt")));

			for (int i = 0; i < inputStringArray.size(); i++) {
				String inputStr = inputStringArray.get(i);
				InputSequence inputSequence = new InputSequence(inputStr);
				LocalFSDetection localFS = new LocalFSDetection(this.localParameterSpace, inputSequence);
				localFS.Initialization(globalFrequentElements);
				HashMap<String, Integer> tempLocalFS = localFS.LocalFrequentSequenceMining(i);
				out.write("#" + i);
				out.newLine();
				for (String oneStr : tempLocalFS.keySet()) {
					out.write(oneStr + "\t" );
				}
				out.newLine();
			}

			out.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void findLocalOutliers(HashSet<String> globalFrequentElements, int thresholdForLocalFSOutliers,
			HashMap<String, String> metaDataMapping) {
		try {
			BufferedWriter out = new BufferedWriter(
					new FileWriter(new File("localFS-lts-" + localParameterSpace.getMinLocalSupport() + ".txt")));
			HashMap<Integer, HashMap<String, Integer>> fsInEachSeq = new HashMap<Integer, HashMap<String, Integer>>();

			for (int i = 0; i < inputStringArray.size(); i++) {
				String inputStr = inputStringArray.get(i);
				InputSequence inputSequence = new InputSequence(inputStr);
				LocalFSDetection localFS = new LocalFSDetection(localParameterSpace, inputSequence);
				localFS.Initialization(globalFrequentElements);
				HashMap<String, Integer> tempLocalFS = localFS.LocalFrequentSequenceMining(i);
				if (tempLocalFS.size() > 0) {
					fsInEachSeq.put(i, tempLocalFS);
					out.write("#" + i + " , Sequence Size:" + inputStringArray.get(i).split(",").length);
					out.newLine();
					for (String oneStr : tempLocalFS.keySet()) {
						out.write(oneStr  + "(" + tempLocalFS.get(oneStr) + ")" + "\t");
					}
					out.newLine();
				}
				if (i % 100 == 0) {
					System.out.println(i + " Finished!");
				}
			}

			File outlierFile = new File("outliers-" + localParameterSpace.getMinLocalSupport() + ".txt");
			FileUtile.findLocalOutliersAndSaveInFile(outlierFile, fsInEachSeq, thresholdForLocalFSOutliers, false, null,
					metaDataMapping);

			out.close();
		} catch (

		IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public ArrayList<String> getInputStringArray() {
		return inputStringArray;
	}

	public void setInputStringArray(ArrayList<String> inputStringArray) {
		this.inputStringArray = inputStringArray;
	}

	public static void main(String[] args) {
		// String inputPath = "logfile.tsv";
		// String inputPath = "ConnectedLogs.tsv";
		// String inputPath = "extractedClean_less100000_noId.tsv";
		// String inputPath = "8w-devices.tsv";
		String inputPath = "extracted_1000devices_noId.csv";
//		String inputPath = "extractedClean_less100000_withId";
		String extractedMetaLarge = "extractedMetaLarge.tsv";
		long startTime = System.currentTimeMillis();
		// String inputPath = "extractedClean_less10000.tsv";
		// String inputPath = "extractedClean_less10000.tsv";
		// String inputPath = "SmallLogFile.tsv";
		// String inputPath = "smallTest.csv";

		int LocalMinSupport = 3;
		int globalMinSupport = 200;
		int itemgap = 0;
		int seqGap = 6;

		System.out.println("LocalSupport = " + LocalMinSupport);
		ArrayList<String> inputStringArray = FileUtile.readInDataset(inputPath);
		HashSet<String> globalFrequentElements = Toolbox.getGlobalFrequentElements(inputStringArray, globalMinSupport,
				LocalMinSupport);
		GlobalFSDetection globalFS = new GlobalFSDetection(LocalMinSupport, itemgap, seqGap,
				inputStringArray);
		globalFS.generateLocalFrequentSequences(globalFrequentElements);
		
//		HashMap<String, String> metaDataMapping = FileUtile.readInLargeMetaDataToMemory(FileUtile.readInDataset(extractedMetaLarge));
//		globalFS.findLocalOutliers(globalFrequentElements, 2, metaDataMapping);

		System.out.println("Compute Global Frequent Sequence takes " + (System.currentTimeMillis() - startTime) / 1000
				+ " seconds!");

	}

}
