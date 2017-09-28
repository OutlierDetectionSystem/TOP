package top.centralized.stl;

import top.inputs.InputFile;
import top.parameterspace.GlobalParameterSpace;
import top.utils.FileUtile;
import top.utils.Toolbox;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

public class MultipleTimeSeriesMining {
	private InputFile inputInfo;
	private GlobalParameterSpace globalParameterSpace;

	private HashSet<String> finalFreqSeqs;
	private ArrayList<Set<String>> freqSeqsEachTimeSeries;

	public MultipleTimeSeriesMining(InputFile inputInfo, GlobalParameterSpace globalParameterSpace) {
		this.inputInfo = inputInfo;
		this.globalParameterSpace = globalParameterSpace;
		finalFreqSeqs = new HashSet<String>();
		freqSeqsEachTimeSeries = new ArrayList<Set<String>>();
	}

	/**
	 * deal with each String and generate frequent sequence
	 *
	 */
	public void generateFrequentSequences(HashSet<String> globalFrequentElements) {

		HashMap<String, Integer> freqSeqs = new HashMap<String, Integer>();
		int count = 0;
		try {
			BufferedWriter out = new BufferedWriter(new FileWriter(new File("localFS-stl.txt")));

			for (String inputStr : inputInfo.getInputStringArray()) {
				// System.out.println("inputString:" + inputStr);
				SingleSequenceMining obj = new SingleSequenceMining(inputStr, globalParameterSpace.getLocalParameterSpace());
				Set<String> singleRes = obj.findFreqSeqInOneString(count, globalFrequentElements);
				freqSeqsEachTimeSeries.add(singleRes);
				out.write("#" + count);
				out.newLine();
				for (String oneStr : singleRes) {
					out.write(oneStr + "\t");
				}
				out.newLine();
				count++;
				if (count % 1 == 0) {
					System.out.println(count + " Finished!");
				}
			}
			out.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public static void main(String[] args) {
		long start = System.currentTimeMillis();
		// String inputPath = "smalltest.tsv";
		// String inputPath = "logfile.tsv";
		// String inputPath = "ConnectedLogs.tsv";
		// String inputPath = "extractedClean_less10000.tsv";
//		 String inputPath = "extractedClean_less100000_noId.tsv";
//		String inputPath = "8w-devices.tsv";
//		String inputPath = "smallTest.csv";
		// String inputPath = "input";
		String inputPath = "extracted_1000devices_noId.csv";
		File f = new File("OutlierViolations");
		f.delete();
		f = new File("SeqMatching/InFrequent-Results");
		f.delete();
		// first get lts frequent sequences using small local support
		int LocalMinSupport = 3;
		int globalMinSupport = 200;
		int itemgap = 0;
		int seqGap = 6;

		ArrayList<String> inputStringArray = FileUtile.readInDataset(inputPath);
		HashSet<String> globalFrequentElements = Toolbox.getGlobalFrequentElements(inputStringArray, globalMinSupport,
				LocalMinSupport);
		InputFile inputInfo = new InputFile(inputStringArray, null, null, false);
		GlobalParameterSpace globalParameterSpace = new GlobalParameterSpace(LocalMinSupport, itemgap, seqGap, globalMinSupport,
				2, 2);
		MultipleTimeSeriesMining mts = new MultipleTimeSeriesMining(inputInfo, globalParameterSpace);
		mts.generateFrequentSequences(globalFrequentElements);
		System.out.println("Takes " + ((System.currentTimeMillis() - start) / 1000) + "seconds");
	}
}
