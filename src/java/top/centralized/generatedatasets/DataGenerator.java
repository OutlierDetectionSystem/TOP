package top.centralized.generatedatasets;

import java.util.TreeSet;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import java.util.ArrayList;

public class DataGenerator {

	private int alphabetSize = 1000;
	private int sequenceLength = 10000;
	private int maxLengthFreqPatterns = 10;
	private int numDevices = 10000;
	private int numPatterns = 5000;
	private int numFreqPatterns = 1000;
	private int globalSupport = 50;
	private int localSupport = 5;
	private int numOutliers = 200;
	private double noiseRate = 0.001;

	private String outputFilePath;
	private boolean verbose;
	private TreeSet<String> outlierSequences;
	private ArrayList<String> frequentSequences;
	private ArrayList<String> patternCandidates;

	public DataGenerator(int alphabetSize, int sequenceLength, int maxLengthFreqPatterns, int numDevices,
			int numPatterns, int numFreqPatterns, int globalSupport, int localSupport, double noiseRate,
			String outputFilePath, boolean verbose) {
		this.alphabetSize = alphabetSize;
		this.sequenceLength = sequenceLength;
		this.maxLengthFreqPatterns = maxLengthFreqPatterns;
		this.numDevices = numDevices;
		this.numPatterns = numPatterns;
		this.numFreqPatterns = numFreqPatterns;
		this.globalSupport = globalSupport;
		this.localSupport = localSupport;
		this.numOutliers = (int) (numFreqPatterns * 0.2);
		this.outputFilePath = outputFilePath;
		this.noiseRate = noiseRate;
		this.verbose = verbose;
		this.checkValidation();
	}

	public void checkValidation() {
		if (((this.numFreqPatterns * this.globalSupport / this.numDevices * 2 + 1) * 1.5 * this.localSupport
				* this.maxLengthFreqPatterns > this.sequenceLength)
				|| (1.5 * this.maxLengthFreqPatterns * this.localSupport
						* (this.numFreqPatterns * this.globalSupport * 2 + this.numOutliers) > this.sequenceLength
								* this.numDevices)) {
			System.out.println("Invalid parameter setting!");
			System.exit(0);
		}
	}

	public void generatePatterns() {
		GeneratePatterns gp = new GeneratePatterns();
		this.patternCandidates = gp.generatePatternCandidates(this.numPatterns, this.maxLengthFreqPatterns,
				this.alphabetSize);
		gp.outputPatternCandidatesToFile(patternCandidates, "PatternCandidates.csv");
		this.frequentSequences = gp.generateFrequentPatterns(this.patternCandidates, this.numFreqPatterns);
		gp.outputFrequentPatternsToFile(frequentSequences, "FrequentPatterns.csv");
		this.outlierSequences = gp.generateOutliers(patternCandidates, numOutliers, frequentSequences, alphabetSize);
	}

	public void generateDevices() {
		GenerateDevices gd = new GenerateDevices(numDevices, localSupport, globalSupport, sequenceLength, verbose);
		gd.addOutliers(outlierSequences);
		gd.addFrequentPatterns(frequentSequences);
		gd.addMoreFrequentPatterns(frequentSequences);
		gd.addPatternCandidates(patternCandidates, sequenceLength);
		gd.outputDeviceDataToFile(outputFilePath, 1-this.noiseRate);
	}

	public void generateData() {
		// build up a set of pattern candidates
		generatePatterns();
		generateDevices();
	}

	public static void main(String[] args) {
		Options options = new Options();
		options.addOption("a", true, "alphabet size");
		options.addOption("s", true, "sequence length");
		options.addOption("m", true, "max length of frequent patterns");
		options.addOption("d", true, "number of devices");
		options.addOption("p", true, "number of pattern candidates");
		options.addOption("f", true, "number of frequent patterns");
		options.addOption("g", true, "lts support");
		options.addOption("l", true, "local support");
		options.addOption("o", true, "output file directory");

		CommandLineParser parser = new DefaultParser();
		CommandLine cmd;

		try {
			cmd = parser.parse(options, args);
			int alphabetSize = Integer.parseInt(cmd.getOptionValue("a"));
			int sequenceLength = Integer.parseInt(cmd.getOptionValue("s"));
			int maxLengthFreqPatterns = Integer.parseInt(cmd.getOptionValue("m"));
			int numDevices = Integer.parseInt(cmd.getOptionValue("d"));
			int numPatterns = Integer.parseInt(cmd.getOptionValue("p"));
			int numFreqPatterns = Integer.parseInt(cmd.getOptionValue("f"));
			int globalSupport = Integer.parseInt(cmd.getOptionValue("g"));
			int localSupport = Integer.parseInt(cmd.getOptionValue("l"));
			String outputFileDic = cmd.getOptionValue("o");

			String outputFilePath = outputFileDic + "synthetic-" + numDevices + "-" + sequenceLength + "-"
					+ alphabetSize + "-" + maxLengthFreqPatterns + "-" + numFreqPatterns + "-" + globalSupport + "-"
					+ localSupport + ".csv";
			boolean verbose = true;
			DataGenerator dataGenerator = new DataGenerator(alphabetSize, sequenceLength, maxLengthFreqPatterns,
					numDevices, numPatterns, numFreqPatterns, globalSupport, localSupport, 0.001, outputFilePath,
					false);
			dataGenerator.generateData();
		} catch (ParseException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
	}

}
