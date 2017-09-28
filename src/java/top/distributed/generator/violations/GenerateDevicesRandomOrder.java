package top.distributed.generator.violations;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Reducer.Context;
import org.apache.hadoop.mapreduce.lib.output.MultipleOutputs;
import top.utils.SQConfig;
import top.utils.StdRandom;

public class GenerateDevicesRandomOrder {
	private int sequenceLength = 10000;
	private int numDevices = 10000;
	private int globalSupport = 50;
	private int localSupport = 5;
	private String[] devices;
	private int[] numElementsInDevices;
	private boolean verbose = false;

	public GenerateDevicesRandomOrder(int numDevices, int localSupport, int globalSupport, int sequenceLength, boolean verbose) {
		this.numDevices = numDevices;
		this.localSupport = localSupport;
		this.verbose = verbose;
		this.globalSupport = globalSupport;
		this.sequenceLength = sequenceLength;
		this.devices = new String[this.numDevices];
		for (int i = 0; i < this.numDevices; i++) {
			this.devices[i] = "";
		}
		this.numElementsInDevices = new int[this.numDevices];
	}

	public HashSet<String> addFrequentPatterns(ArrayList<String> frequentSequences, int alphabetSize,
			HashSet<String> allPatterns) {
		int numFreqSeqs = frequentSequences.size();
		int numOutliers = (int) (numFreqSeqs * 0.2);
		HashSet<Integer> fsIndexesForOutliers = new HashSet<Integer>();
		while (fsIndexesForOutliers.size() < numOutliers) {
			int randomIndex = StdRandom.random.nextInt(numFreqSeqs);
			fsIndexesForOutliers.add(randomIndex);
		}

		HashSet<String> outlierPatterns = new HashSet<String>();
		for (int j = 0; j < numFreqSeqs; j++) {
			String currentFreqSeq = frequentSequences.get(j);
			int currentFSLength = currentFreqSeq.split(",").length;
			// first randomly setup indexes that contains |mGS| device ids
			HashSet<Integer> usedDevices = new HashSet<Integer>();
			while (usedDevices.size() < this.globalSupport) {
				int deviceId = StdRandom.random.nextInt(numDevices);
				if (!usedDevices.contains(deviceId)) {
					usedDevices.add(deviceId);
					int localS = StdRandom.randomNumber(this.localSupport, this.localSupport * 3);
					for (int i = 0; i < localS; i++) {
						int pos = 0;
						if (devices[deviceId].contains("|")) {
							int indexOfBar = -1;
//							System.out.println(devices[deviceId]);
							do {
								int randomNum = StdRandom.random.nextInt(devices[deviceId].length());
								indexOfBar = devices[deviceId].indexOf("|", randomNum);
//								System.out.println(indexOfBar);
							} while (indexOfBar == -1);
							pos = indexOfBar + 1;
						}
						this.devices[deviceId] = this.devices[deviceId].substring(0, pos) + currentFreqSeq + "|"
								+ this.devices[deviceId].substring(pos, this.devices[deviceId].length());
						// this.devices[deviceId] += currentFreqSeq + ",";
						this.numElementsInDevices[deviceId] += currentFSLength;
					}
				}
			}
			if (fsIndexesForOutliers.contains(j)) {
				// add outliers
				String formattedOutlier;
				do {
					int extraElement = StdRandom.random.nextInt(alphabetSize);
					formattedOutlier = OutlierFormatter.changeFSToOutliers(currentFreqSeq, extraElement + "");
				} while ((outlierPatterns.contains(formattedOutlier)) || (allPatterns.contains(formattedOutlier)));
				outlierPatterns.add(formattedOutlier);
				int localS = StdRandom.randomNumber(this.localSupport, this.localSupport * 3);
				int deviceId = usedDevices.iterator().next();

				int currentOutlierLength = formattedOutlier.split(",").length;
				for (int i = 0; i < localS; i++) {
					int pos = 0;
					if (devices[deviceId].contains("|")) {
						int indexOfBar = -1;
						do {
							int randomNum = StdRandom.random.nextInt(devices[deviceId].length());
							indexOfBar = devices[deviceId].indexOf("|", randomNum);
						} while (indexOfBar == -1);
						pos = indexOfBar + 1;
					}
					this.devices[deviceId] = this.devices[deviceId].substring(0, pos) + formattedOutlier + "|"
							+ this.devices[deviceId].substring(pos, this.devices[deviceId].length());
					this.numElementsInDevices[deviceId] += currentOutlierLength;
				}
			}

		}

		if (verbose) {
			System.out.println("Add frequent sequences");
			this.printCurrentDevices();
		}
		return outlierPatterns;
	}

	/**
	 * in order to have diverse support values (global support & local support),
	 * for each device, select a set of frequent patterns and add to it for LS
	 * times,
	 * 
	 * @param frequentSequences
	 */
	public void addMoreFrequentPatterns(ArrayList<String> frequentSequences) {
		int numFreqSeqs = frequentSequences.size();
		int numberFSForEachDevice = (int) Math.ceil(numFreqSeqs * globalSupport * 1.0 / numDevices);
		for (int i = 0; i < numDevices; i++) {
			for (int j = 0; j < numberFSForEachDevice; j++) {
				// select one frequent sequence based on Gaussian distribution
				int selectedFS = StdRandom.gaussian(numFreqSeqs - 1, 0, numFreqSeqs / 2, numFreqSeqs / 5);
				String currentFreqSeq = frequentSequences.get(selectedFS);
				int currentFSLength = currentFreqSeq.split(",").length;
				int localS = StdRandom.randomNumber(this.localSupport, this.localSupport * 3);
				for (int s = 0; s < localS; s++) {
					int pos = 0;
					if (devices[i].contains("|")) {
						int indexOfBar = -1;
						do {
							int randomNum = StdRandom.random.nextInt(devices[i].length());
							indexOfBar = devices[i].indexOf("|", randomNum);
						} while (indexOfBar == -1);
						pos = indexOfBar + 1;
					}
//					System.out.println("Pos: " + pos);
//					System.out.println(this.devices[i]);
					this.devices[i] = this.devices[i].substring(0, pos) + currentFreqSeq + "|"
							+ this.devices[i].substring(pos, this.devices[i].length());
//					System.out.println(this.devices[i]);
					// this.devices[i] += currentFreqSeq + ",";
					this.numElementsInDevices[i] += currentFSLength;
				}
			}
		}
		if (verbose) {
			System.out.println("Add more frequent sequences");
			this.printCurrentDevices();
		}
	}

	public void addPatternCandidates(ArrayList<String> patternCandidates, int sequenceLength) {
		int numPatterns = patternCandidates.size();
		for (int i = 0; i < numDevices; i++) {
			int finalNumElementsInDevice = StdRandom.randomNumber((int) (sequenceLength * 0.9),
					(int) (sequenceLength * 1.1));
			while (this.numElementsInDevices[i] < finalNumElementsInDevice) {
				int selectedPC = StdRandom.gaussian(numPatterns - 1, 0, numPatterns / 2, numPatterns / 5);
				String currentPC = patternCandidates.get(selectedPC);
				int currentPCLength = currentPC.split(",").length;
				devices[i] = devices[i] + currentPC  + "|";
				// this.devices[i] += currentPC + ",";
				this.numElementsInDevices[i] += currentPCLength;
			}
		}
		if (verbose) {
			System.out.println("Add pattern candidates");
			this.printCurrentDevices();
		}
	}

	public void printCurrentDevices() {
		for (int i = 0; i < this.numDevices; i++) {
			System.out.println("# of elements: " + this.numElementsInDevices[i] + ",sequence: " + this.devices[i]);
		}
	}

	public void outputDeviceDataToFile(Context context, MultipleOutputs mos, double probability) {
		try {
			// BufferedWriter writer = new BufferedWriter(new FileWriter(new
			// File(outputFilePath)));
			Configuration conf = context.getConfiguration();
			String outputPath = conf.get(SQConfig.strInputDataset) + "-" + conf.getInt(SQConfig.strAlphabetSize, 10)
					+ "-" + conf.getInt(SQConfig.strNumFreqPatterns, 1) + "-"
					+ conf.getInt(SQConfig.strSequenceLength, 1) + "-" + conf.getInt(SQConfig.strMaxLenFreqPatterns, 10)
					+ "-" + conf.getInt(SQConfig.strMinLocalSupport, 5) + "-"
					+ conf.getInt(SQConfig.strMinGlobalSupport, 50);

			for (int i = 0; i < devices.length; i++) {
//				System.out.println(devices[i]);
				String str = devices[i].replaceAll("\\|", ",");
				if(str.endsWith(","))
					str = str.substring(0, str.length()-1);
//				System.out.println(str);
				String[] items = str.split(",");
				String outputString = "";
				for (String item : items)
//					if (StdRandom.bernoulli(probability)) {
						outputString += item + ",";
//					}
				if (outputString.length() > 0)
					outputString = outputString.substring(0, outputString.length() - 1);
//				// mos.write(NullWritable.get(), new Text(str), outputPath +
//				// "/data");
				
				context.write(NullWritable.get(), new Text(outputString));
				context.getCounter(GenerateLongSequences.Counters.deviceCount).increment(1);
				// writer.write(outputString);
				// writer.newLine();
			} // end
				// for
				// writer.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}
}
