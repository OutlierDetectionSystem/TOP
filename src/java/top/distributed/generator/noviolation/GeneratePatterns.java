package top.distributed.generator.noviolation;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.TreeSet;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import top.utils.StdRandom;


public class GeneratePatterns {
	public GeneratePatterns() {
	}

	public ArrayList<String> generatePatternCandidates(int numPatterns, int maxLengthFreqPatterns, int alphabetSize) {
		ArrayList<String> patternCandidates = new ArrayList<String>();
		for (int i = 0; i < numPatterns; i++) {
			String tempStr = "";
			int curLength = 0;
//			curLength = StdRandom.gaussian(maxLengthFreqPatterns, 2, (int) Math.round(maxLengthFreqPatterns * 2.0 / 3),
//					(int) Math.round(maxLengthFreqPatterns / 5));
			curLength = StdRandom.gaussian(maxLengthFreqPatterns, 2, (int) Math.round(maxLengthFreqPatterns * 1.0 / 2),
					(int) Math.round(maxLengthFreqPatterns / 5));
			HashSet<Integer> alreadyAdded = new HashSet<Integer>();
			for (int j = 0; j < curLength; j++) {
				int item = StdRandom.random.nextInt(alphabetSize);
				while (alreadyAdded.contains(item)) {
					item = StdRandom.random.nextInt(alphabetSize);
				}
				alreadyAdded.add(item);
				tempStr += item + ",";
			}
			if (tempStr.length() > 0)
				tempStr = tempStr.substring(0, tempStr.length() - 1);
			patternCandidates.add(tempStr);
		}
		return patternCandidates;
	}

	public ArrayList<String> generateFrequentPatterns(ArrayList<String> patternCandidates, int numFreqPatterns) {
		int numPatterns = patternCandidates.size();
		ArrayList<String> frequentSequences = new ArrayList<String>();
		HashSet<String> existingFreqSequences = new HashSet<String>();
		while (frequentSequences.size() < numFreqPatterns) {
			int item = StdRandom.random.nextInt(numPatterns);
			if (!existingFreqSequences.contains(patternCandidates.get(item))) {
				frequentSequences.add(patternCandidates.get(item));
				existingFreqSequences.add(patternCandidates.get(item));
			}
		}
		return frequentSequences;
	}

	public TreeSet<String> generateOutliers(ArrayList<String> patternCandidates, int numOutliers,
			ArrayList<String> frequentSequences, int alphabetSize) {
		HashSet<String> allPatterns = new HashSet(patternCandidates);
		int numFreqPatterns = frequentSequences.size();
		TreeSet<String> outlierSequences = new TreeSet<String>();
		while (outlierSequences.size() < numOutliers) {
			int item = StdRandom.random.nextInt(numFreqPatterns);
			int extraElement = StdRandom.random.nextInt(alphabetSize);
			String formattedOutlier = OutlierFormatter.changeFSToOutliers(frequentSequences.get(item),
					extraElement + "");
			if ((!outlierSequences.contains(formattedOutlier)) && (!allPatterns.contains(formattedOutlier))) {
				outlierSequences.add(formattedOutlier);
			}
		}
		// for (String str : outlierSequences) {
		// System.out.println("Outlier: " + str);
		// }
		return outlierSequences;
	}

	public void outputPatternCandidatesToFile(ArrayList<String> patternCandidates, String fileName) {
		Configuration conf = new Configuration();
		conf.addResource(new Path("/usr/local/Cellar/hadoop/etc/hadoop/core-site.xml"));
		conf.addResource(new Path("/usr/local/Cellar/hadoop/etc/hadoop/hdfs-site.xml"));
		try {
			FileSystem fs = FileSystem.get(conf);
			FSDataOutputStream outputStream = fs.create(new Path(fileName), true);
			BufferedWriter writer = null;
			writer = new BufferedWriter(new OutputStreamWriter(outputStream));
//			BufferedWriter writer = new BufferedWriter(new FileWriter(new File(fileName)));
			for (String curFS : patternCandidates) {
				writer.write(curFS);
				writer.newLine();
			}
			writer.close();
			outputStream.close();
			fs.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void outputFrequentPatternsToFile(ArrayList<String> frequentSequences, String fileName) {
		Configuration conf = new Configuration();
		conf.addResource(new Path("/usr/local/Cellar/hadoop/etc/hadoop/core-site.xml"));
		conf.addResource(new Path("/usr/local/Cellar/hadoop/etc/hadoop/hdfs-site.xml"));
		try {
			FileSystem fs = FileSystem.get(conf);
			FSDataOutputStream outputStream = fs.create(new Path(fileName), true);
			BufferedWriter writer = null;
			writer = new BufferedWriter(new OutputStreamWriter(outputStream));
//			BufferedWriter writer = new BufferedWriter(new FileWriter(new File(fileName)));
			for (String curFS : frequentSequences) {
				writer.write(curFS);
				writer.newLine();
			}
			writer.close();
			outputStream.close();
			fs.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void outputOutliersToFile(TreeSet<String> outliers, String fileName) {
		Configuration conf = new Configuration();
		conf.addResource(new Path("/usr/local/Cellar/hadoop/etc/hadoop/core-site.xml"));
		conf.addResource(new Path("/usr/local/Cellar/hadoop/etc/hadoop/hdfs-site.xml"));
		try {
			FileSystem fs = FileSystem.get(conf);
			FSDataOutputStream outputStream = fs.create(new Path(fileName), true);
			BufferedWriter writer = null;
			writer = new BufferedWriter(new OutputStreamWriter(outputStream));
//			BufferedWriter writer = new BufferedWriter(new FileWriter(new File(fileName)));
			for (String curFS : outliers) {
				writer.write(curFS);
				writer.newLine();
			}
			writer.close();
			outputStream.close();
			fs.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
