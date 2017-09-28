package top.distributed.generator.violations;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.util.GenericOptionsParser;
import top.utils.SQConfig;

import java.io.IOException;
import java.util.ArrayList;

public class GeneratePatternsAndOutliers {

	private int alphabetSize = 100;
	private int maxLengthFreqPatterns = 10;
	private int numPatterns = 5000;
	private int numFreqPatterns = 1000;
//	private double subsequencePercentage = 0.3;
	private ArrayList<String> frequentSequences;
	private ArrayList<String> patternCandidates;
	private String outputFileDic;

	public GeneratePatternsAndOutliers(int alphabetSize, int maxLengthFreqPatterns, int numFreqPatterns,
			int numPatterns, String outputFileDic) {
		this.alphabetSize = alphabetSize;
		this.maxLengthFreqPatterns = maxLengthFreqPatterns;
		this.numFreqPatterns = numFreqPatterns;
		this.numPatterns = numPatterns;
		this.outputFileDic = outputFileDic;
	}

	public void generatePatterns() {
		GeneratePatterns gp = new GeneratePatterns();
		int numRandomPatternCandidates =(int) (this.numPatterns-numFreqPatterns*0.5);
		this.patternCandidates = gp.generatePatternCandidates(numRandomPatternCandidates, this.maxLengthFreqPatterns,
				this.alphabetSize);
		this.frequentSequences = gp.generateFrequentPatterns(this.patternCandidates, this.numFreqPatterns);
		gp.outputPatternsToFile(patternCandidates, outputFileDic + "/" + "PatternCandidates.csv");
		gp.outputPatternsToFile(frequentSequences, outputFileDic + "/" + "FrequentPatterns.csv");
	}

	public static void main(String[] args) {
		Configuration conf = new Configuration();
		conf.addResource(new Path("/usr/local/Cellar/hadoop/etc/hadoop/core-site.xml"));
		conf.addResource(new Path("/usr/local/Cellar/hadoop/etc/hadoop/hdfs-site.xml"));
		try {
			new GenericOptionsParser(conf, args).getRemainingArgs();
			int alphabetSize = conf.getInt(SQConfig.strAlphabetSize, 1000);
			int maxLengthFreqPatterns = conf.getInt(SQConfig.strMaxLenFreqPatterns, 10);
			int numFreqPatterns = conf.getInt(SQConfig.strNumFreqPatterns, 1000);
			int numPatterns = conf.getInt(SQConfig.strNumPatterns, 5000);
			String outputFileDic = conf.get(SQConfig.strDictionaryPath) + "-"
					+ conf.getInt(SQConfig.strAlphabetSize, 10) + "-" + conf.getInt(SQConfig.strNumFreqPatterns, 1)
					+ "-" + conf.getInt(SQConfig.strSequenceLength, 1) + "-"
					+ conf.getInt(SQConfig.strMaxLenFreqPatterns, 10) + "-"
					+ conf.getInt(SQConfig.strMinLocalSupport, 5) + "-" + conf.getInt(SQConfig.strMinGlobalSupport, 50);
			System.out.println("Alphabet Size:" + alphabetSize);
			System.out.println("MaxLength of Frequent Patterns: " + maxLengthFreqPatterns);
			System.out.println("Number of Frequent Patterns: " + numFreqPatterns);
			System.out.println("Number of patterns: " + numPatterns);
			System.out.println("Min Local Support: " + conf.getInt(SQConfig.strMinLocalSupport, 10));
			System.out.println("Min Global Support: " + conf.getInt(SQConfig.strMinGlobalSupport, 10));
			System.out.println("Dictionary output: " + outputFileDic);
			GeneratePatternsAndOutliers dataGenerator = new GeneratePatternsAndOutliers(alphabetSize,
					maxLengthFreqPatterns, numFreqPatterns, numPatterns, outputFileDic);
			dataGenerator.generatePatterns();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
