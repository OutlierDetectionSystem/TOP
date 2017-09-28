package top.centralized.generatedatasets;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.TreeSet;

import top.utils.StdRandom;

public class GenerateDevices {
	private int sequenceLength = 10000;
	private int numDevices = 10000;
	private int globalSupport = 50;
	private int localSupport = 5;
	private String[] devices;
	private int[] numElementsInDevices;
	private boolean verbose = false;

	public GenerateDevices(int numDevices, int localSupport, int globalSupport, int sequenceLength, boolean verbose) {
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

	public void addOutliers(TreeSet<String> outlierSequences) {
		String curOutlier;
		while ((curOutlier = outlierSequences.pollFirst()) != null) {
			int randomIndex = StdRandom.random.nextInt(numDevices);
			int localS = StdRandom.randomNumber(this.localSupport, this.localSupport * 3);
			int currentOutlierLength = curOutlier.split(",").length;
			for (int i = 0; i < localS; i++) {
				this.devices[randomIndex] += curOutlier + ",";
				this.numElementsInDevices[randomIndex] += currentOutlierLength;
			}
		}
		if (verbose) {
			System.out.println("Outliers added: ");
			this.printCurrentDevices();
		}
	}

	public void addFrequentPatterns(ArrayList<String> frequentSequences) {
		int numFreqSeqs = frequentSequences.size();
		for (String currentFreqSeq : frequentSequences) {
			int currentFSLength = currentFreqSeq.split(",").length;
			// first randomly setup indexes that contains |mGS| device ids
			HashSet<Integer> usedDevices = new HashSet<Integer>();
			while (usedDevices.size() < this.globalSupport) {
				int deviceId = StdRandom.random.nextInt(numDevices);
				if (!usedDevices.contains(deviceId)) {
					usedDevices.add(deviceId);
					int localS = StdRandom.randomNumber(this.localSupport, this.localSupport * 3);
					for (int i = 0; i < localS; i++) {
						this.devices[deviceId] += currentFreqSeq + ",";
						this.numElementsInDevices[deviceId] += currentFSLength;
					}
				}
			}
		}

		if (verbose) {
			System.out.println("Add frequent sequences");
			this.printCurrentDevices();
		}
	}

	/**
	 * in order to have diverse support values (lts support & local support),
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
					this.devices[i] += currentFreqSeq + ",";
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
				this.devices[i] += currentPC + ",";
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

	public void outputDeviceDataToFile(String outputFilePath, double probability) {
		try {
			BufferedWriter writer = new BufferedWriter(new FileWriter(new File(outputFilePath)));
			for (int i = 0; i < devices.length; i++) {
				String str = devices[i];
				if (str.length() > 0)
					str = str.substring(0, str.length() - 1);
				String[] items = str.split(",");
				if (items.length != this.numElementsInDevices[i])
					System.out.println("Error occurs....");
				String outputString = "";
				for (String item : items)
					if (StdRandom.bernoulli(probability)) {
						outputString += item + ",";
					}
				if (outputString.length() > 0)
					outputString = str.substring(0, outputString.length() - 1);
				writer.write(outputString);
				writer.newLine();
			} // end
				// for
			writer.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}
}
