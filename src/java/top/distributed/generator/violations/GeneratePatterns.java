package top.distributed.generator.violations;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.HashSet;

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
		while(patternCandidates.size() < numPatterns){
			String tempStr = "";
			int curLength = 0;
			curLength = StdRandom.gaussian(maxLengthFreqPatterns, 2, (int) Math.round(maxLengthFreqPatterns * 2.0 / 3),
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

	/**
	 * Generate 50% from candidates, generate 50% subpatterns from frequent patterns
	 * @param patternCandidates
	 * @param numFreqPatterns
	 * @return
	 */
	public ArrayList<String> generateFrequentPatterns(ArrayList<String> patternCandidates, int numFreqPatterns) {
		int numSubsequenceOfFS = (int) (0.5 * numFreqPatterns);
		int numRandomFS = numFreqPatterns- numSubsequenceOfFS;
		int numPatterns = patternCandidates.size();
		ArrayList<String> frequentSequences = new ArrayList<String>();
		HashSet<String> existingFreqSequences = new HashSet<String>();
		while (frequentSequences.size() < numRandomFS) {
			int item = StdRandom.random.nextInt(numPatterns);
			if (!existingFreqSequences.contains(patternCandidates.get(item))) {
				// generate subpattern of this frequent pattern
				String curPattern = patternCandidates.get(item);
				String newFS = OutlierFormatter.removeElements(curPattern);
				if(newFS.split(",").length > 2 && !patternCandidates.contains(newFS) && !existingFreqSequences.contains(newFS)){
					frequentSequences.add(curPattern + "|" + newFS);
					patternCandidates.add(newFS);
					existingFreqSequences.add(newFS);
					existingFreqSequences.add(curPattern);
				}
			}
		}
		return frequentSequences;
	}

	public void outputPatternsToFile(ArrayList<String> patternCandidates, String fileName) {
		Configuration conf = new Configuration();
		conf.addResource(new Path("/usr/local/Cellar/hadoop/etc/hadoop/core-site.xml"));
		conf.addResource(new Path("/usr/local/Cellar/hadoop/etc/hadoop/hdfs-site.xml"));
		try {
			FileSystem fs = FileSystem.get(conf);
			FSDataOutputStream outputStream = fs.create(new Path(fileName), true);
			BufferedWriter writer = null;
			writer = new BufferedWriter(new OutputStreamWriter(outputStream));
			// BufferedWriter writer = new BufferedWriter(new FileWriter(new
			// File(fileName)));
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
}
