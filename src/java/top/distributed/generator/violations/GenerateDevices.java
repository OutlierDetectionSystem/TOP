package top.distributed.generator.violations;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;

import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Reducer.Context;
import org.apache.hadoop.mapreduce.lib.output.MultipleOutputs;
import top.utils.StdRandom;


public class GenerateDevices {
	private int sequenceLength = 10000;
	private int numDevices = 10000;
	private int globalSupport = 50;
	private int localSupport = 5;
	private String[] devices;
	private int[] numElementsInDevices;
	private boolean verbose = false;
	private int globalDeviceIndex = 0;

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

	public void addFrequentPatterns(ArrayList<String> frequentSequences, int alphabetSize,
			HashSet<String> allPatterns, HashSet<String> outlierPatterns, HashSet<String> violationPatterns) {
		int numFreqSeqs = frequentSequences.size();
		int numOutliers = (int) (numFreqSeqs * 0.2);
		int numViolations = (int) (numFreqSeqs * 0.2);
		// generate fs indexes for violations and local outliers
		HashSet<Integer> fsIndexesForOutliers = new HashSet<Integer>();
		HashSet<Integer> fsIndexesForViolations = new HashSet<Integer>();
		while (fsIndexesForOutliers.size() < numOutliers) {
			int randomIndex = StdRandom.random.nextInt(numFreqSeqs);
			fsIndexesForOutliers.add(randomIndex);
		}
		while (fsIndexesForViolations.size() < numViolations) {
			int randomIndex = StdRandom.random.nextInt(numFreqSeqs);
			fsIndexesForViolations.add(randomIndex);
		}

		for (int j = 0; j < numFreqSeqs; j++) {
			String currentFreqSeq_1 = frequentSequences.get(j).split("\\|")[0];
			String currentFreqSeq_2 = frequentSequences.get(j).split("\\|")[1];
			int currentFSLength_1 = currentFreqSeq_1.split(",").length;
			int currentFSLength_2 = currentFreqSeq_2.split(",").length;

			// first randomly setup indexes that contains |mGS| device ids
			HashSet<Integer> usedDevices = new HashSet<Integer>();

			while (usedDevices.size() < this.globalSupport) {
				int deviceId = StdRandom.random.nextInt(numDevices);
				if (!usedDevices.contains(deviceId)) {
					usedDevices.add(deviceId);
					int localS_1 = StdRandom.randomNumber(this.localSupport, this.localSupport * 3);
					for (int i = 0; i < localS_1; i++) {
						this.devices[deviceId] += currentFreqSeq_1 + ",";
						this.numElementsInDevices[deviceId] += currentFSLength_1;
					}
					int localS_2 = StdRandom.randomNumber(this.localSupport, this.localSupport * 3);
					for (int i = 0; i < localS_2; i++) {
						this.devices[deviceId] += currentFreqSeq_2 + ",";
						this.numElementsInDevices[deviceId] += currentFSLength_2;
					}
				}
			}
			Iterator<Integer> deviceIdIterator = usedDevices.iterator();
			if (fsIndexesForOutliers.contains(j)) {
				// add outliers
				String formattedOutlier;
				do {
					int extraElement = StdRandom.random.nextInt(alphabetSize);
					formattedOutlier = OutlierFormatter.changeFSToOutliers(currentFreqSeq_1, extraElement + "");
				} while ((outlierPatterns.contains(formattedOutlier)) || (allPatterns.contains(formattedOutlier)) ||
						(violationPatterns.contains(formattedOutlier)));
				outlierPatterns.add(formattedOutlier);
				int localS = StdRandom.randomNumber(this.localSupport, this.localSupport * 3);
				int deviceId = deviceIdIterator.next();

				int currentOutlierLength = formattedOutlier.split(",").length;
				for (int i = 0; i < localS; i++) {
					this.devices[deviceId] += formattedOutlier + ",";
					this.numElementsInDevices[deviceId] += currentOutlierLength;
				}
			}

			if (fsIndexesForViolations.contains(j)) {
				// add violations
				String formattedViolation;
				do {
					formattedViolation = OutlierFormatter.changeFSToViolations(currentFreqSeq_2);
				} while ((outlierPatterns.contains(formattedViolation)) || (allPatterns.contains(formattedViolation)) ||
						(violationPatterns.contains(formattedViolation)));
				violationPatterns.add(formattedViolation);
				int deviceId = deviceIdIterator.next();
				int currentViolationLength = formattedViolation.split(",").length;
				this.devices[deviceId] += formattedViolation + ",";
				this.numElementsInDevices[deviceId] += currentViolationLength;
			}
		}

		if (verbose) {
			System.out.println("Add frequent sequences");
			this.printCurrentDevices();
		}
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
				String currentFreqSeq = frequentSequences.get(selectedFS).split("\\|")[0];
				int currentFSLength = currentFreqSeq.split(",").length;
				int localS = StdRandom.randomNumber(this.localSupport, this.localSupport * 3);
				for (int s = 0; s < localS; s++) {
					this.devices[i] += currentFreqSeq + ",";
					this.numElementsInDevices[i] += currentFSLength;
				}
				currentFreqSeq = frequentSequences.get(selectedFS).split("\\|")[1];
				currentFSLength = currentFreqSeq.split(",").length;
				localS = StdRandom.randomNumber(this.localSupport, this.localSupport * 3);
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

	public void outputDeviceDataToFile(Context context, MultipleOutputs mos, double probability) {
		try {
			for (int i = 0; i < devices.length; i++) {
				String str = devices[i];
				if (str.length() > 0)
					str = str.substring(0, str.length() - 1);
				String outputString = str;
				context.write(NullWritable.get(), new Text(globalDeviceIndex+ "\t" + globalDeviceIndex + "\t" + outputString));
				globalDeviceIndex++;
				context.getCounter(GenerateLongSequences.Counters.deviceCount).increment(1);
			} // end
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}
}
