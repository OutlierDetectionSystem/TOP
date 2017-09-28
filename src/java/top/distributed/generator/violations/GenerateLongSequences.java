package top.distributed.generator.violations;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Counter;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.mapreduce.lib.output.MultipleOutputs;
import org.apache.hadoop.mapreduce.lib.output.TextOutputFormat;
import org.apache.hadoop.util.GenericOptionsParser;
import top.utils.SQConfig;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashSet;

public class GenerateLongSequences {
	public static enum Counters {
		deviceCount, outlierCount, frequentSequenceCount;
	}

	public static class DDMapper extends Mapper<LongWritable, Text, IntWritable, Text> {

		/** number of divisions where data is divided into (set by user) */
		private int numPartitions = 50;

		protected void setup(Context context) throws IOException, InterruptedException {
			Configuration conf = context.getConfiguration();
			numPartitions = conf.getInt(SQConfig.strNumPartitions, 50);
		}

		public void map(LongWritable key, Text value, Context context) throws IOException, InterruptedException {
			for (int i = 0; i < numPartitions; i++) {
				context.write(new IntWritable(i), value);
			}
		}
	}

	/**
	 * @author yizhouyan
	 *
	 */
	public static class DDReducer extends Reducer<IntWritable, Text, NullWritable, Text> {

		private int alphabetSize = 100;
		private int sequenceLength = 10000;
		// private int maxLengthFreqPatterns = 10;
		private int numDevices = 10000;
		private int numPartitions = 100;
		private int minLocalSupport = 5;
		private int minGlobalSupport = 50;
		/** Probability of generating long sequences */
		private double probability = 0.000001;
		private MultipleOutputs mos;

		private ArrayList<String> frequentSequences;
		private ArrayList<String> patternCandidates;

		public void setup(Context context) {
			/** get configuration from file */
			Configuration conf = context.getConfiguration();
			mos = new MultipleOutputs(context);
			this.alphabetSize = conf.getInt(SQConfig.strAlphabetSize, 10);
			this.sequenceLength = conf.getInt(SQConfig.strSequenceLength, 1);
			this.numDevices = conf.getInt(SQConfig.strNumDevices, 10000);
			this.numPartitions = conf.getInt(SQConfig.strNumPartitions, 100);
			this.minLocalSupport = conf.getInt(SQConfig.strMinLocalSupport, 5);
			this.minGlobalSupport = conf.getInt(SQConfig.strMinGlobalSupport, 50);
			// this.outlierSequences = new ArrayList<String>();
			this.frequentSequences = new ArrayList<String>();
			this.patternCandidates = new ArrayList<String>();
			try {
				URI[] cacheFiles = context.getCacheArchives();

				if (cacheFiles == null || cacheFiles.length < 1) {
					System.out.println("not enough cache files");
					return;
				}
				for (URI path : cacheFiles) {
					String filename = path.toString();
					FileSystem fs = FileSystem.get(conf);

					FileStatus[] stats = fs.listStatus(new Path(filename));
					for (int i = 0; i < stats.length; ++i) {
						if (!stats[i].isDirectory() && stats[i].getPath().toString().contains("FrequentPatterns.csv")) {
							System.out.println("Reading frequent patterns from " + stats[i].getPath().toString());
							FSDataInputStream currentStream;
							BufferedReader currentReader;
							currentStream = fs.open(stats[i].getPath());
							currentReader = new BufferedReader(new InputStreamReader(currentStream));
							String line;
							while ((line = currentReader.readLine()) != null) {
								this.frequentSequences.add(line);
							}
							System.out.println("Size of frequent Patterns: " + this.frequentSequences.size());

						} else if (!stats[i].isDirectory()
								&& stats[i].getPath().toString().contains("PatternCandidates.csv")) {
							System.out.println("Reading Pattern candidates from " + stats[i].getPath().toString());
							FSDataInputStream currentStream;
							BufferedReader currentReader;
							currentStream = fs.open(stats[i].getPath());
							currentReader = new BufferedReader(new InputStreamReader(currentStream));
							String line;
							while ((line = currentReader.readLine()) != null) {
								this.patternCandidates.add(line);
							}
							System.out.println("Size of pattern candidates: " + this.patternCandidates.size());

						}
					} // end for (int i = 0; i < stats.length; ++i)

				} // end for (URI path : cacheFiles)

			} catch (IOException ioe) {
				System.err.println("Caught exception while getting cached files");
			}
		}

		public void reduce(IntWritable key, Iterable<Text> values, Context context) throws IOException {
			// collect data

			System.out.println("Generate Device data: " + this.numDevices / this.numPartitions);
			int dataNumber = this.numDevices / this.numPartitions;
			int partitionNumber = key.get();
			int numFSPerPartition = this.frequentSequences.size() / this.numPartitions;
			System.out.println("Partition id: " + partitionNumber);
			ArrayList<String> tempFSList = new ArrayList<String>(this.frequentSequences
					.subList(partitionNumber * numFSPerPartition, (partitionNumber + 1) * numFSPerPartition));
			context.getCounter(Counters.frequentSequenceCount).increment(tempFSList.size());
			GenerateDevices gd = new GenerateDevices(dataNumber, this.minLocalSupport, this.minGlobalSupport,
					sequenceLength, false);
			HashSet<String> patternCandidatesSet = new HashSet<String>(this.patternCandidates);
			HashSet<String> newOutliers = new HashSet<String>();
			HashSet<String> newViolations = new HashSet<String>();
			gd.addFrequentPatterns(tempFSList, this.alphabetSize, patternCandidatesSet, newOutliers, newViolations);
			context.getCounter(Counters.outlierCount).increment(newOutliers.size());
			gd.addMoreFrequentPatterns(tempFSList);
			gd.addPatternCandidates(patternCandidates, sequenceLength);
			gd.outputDeviceDataToFile(context, mos, 1 - this.probability);
			Configuration conf = context.getConfiguration();
			String outputOutlierPath = conf.get(SQConfig.strOutlierPath) + "-"
					+ conf.getInt(SQConfig.strAlphabetSize, 10) + "-" + conf.getInt(SQConfig.strNumFreqPatterns, 1) + "-"
					+ conf.getInt(SQConfig.strSequenceLength, 1) + "-" + conf.getInt(SQConfig.strMaxLenFreqPatterns, 10)
					+ "-" + conf.getInt(SQConfig.strMinLocalSupport, 5) + "-"
					+ conf.getInt(SQConfig.strMinGlobalSupport, 50);
//					context.getConfiguration().get(SQConfig.strOutlierPath);
			for (String str : newOutliers) {
				try {
					mos.write(NullWritable.get(), new Text(str), outputOutlierPath + "/outlier");
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			String outputViolationPath = conf.get(SQConfig.strViolationPath) + "-"
					+ conf.getInt(SQConfig.strAlphabetSize, 10) + "-" + conf.getInt(SQConfig.strNumFreqPatterns, 1) + "-"
					+ conf.getInt(SQConfig.strSequenceLength, 1) + "-" + conf.getInt(SQConfig.strMaxLenFreqPatterns, 10)
					+ "-" + conf.getInt(SQConfig.strMinLocalSupport, 5) + "-"
					+ conf.getInt(SQConfig.strMinGlobalSupport, 50);
			for (String str : newViolations) {
				try {
					mos.write(NullWritable.get(), new Text(str), outputViolationPath + "/violation");
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}

		} // end reduce function

		public void cleanup(Context context) throws IOException, InterruptedException {
			mos.close();
		}

	} // end reduce class

	public void run(String[] args) throws Exception {
		Configuration conf = new Configuration();
		conf.addResource(new Path("/usr/local/Cellar/hadoop/etc/hadoop/core-site.xml"));
		conf.addResource(new Path("/usr/local/Cellar/hadoop/etc/hadoop/hdfs-site.xml"));
		new GenericOptionsParser(conf, args).getRemainingArgs();
		/** set job parameter */
		Job job = Job.getInstance(conf, "Generate Dataset");

		job.setJarByClass(GenerateLongSequences.class);
		job.setMapperClass(DDMapper.class);
		job.setReducerClass(DDReducer.class);
		job.setMapOutputKeyClass(IntWritable.class);
		job.setMapOutputValueClass(Text.class);
		job.setOutputKeyClass(NullWritable.class);
		job.setOutputValueClass(Text.class);
		job.setNumReduceTasks(Integer.parseInt(conf.get(SQConfig.strNumReducers)));

		String strFSName = conf.get("fs.default.name");
		FileInputFormat.addInputPath(job, new Path(conf.get(SQConfig.strDummyInput)));
		FileSystem fs = FileSystem.get(conf);

		/** set multiple output path */
		MultipleOutputs.addNamedOutput(job, "data", TextOutputFormat.class, NullWritable.class, Text.class);
		MultipleOutputs.addNamedOutput(job, "outliers", TextOutputFormat.class, LongWritable.class, Text.class);

		String outputPath = conf.get(SQConfig.strInputDataset) + "-" + conf.getInt(SQConfig.strAlphabetSize, 10) + "-"
				+ conf.getInt(SQConfig.strNumFreqPatterns, 1) + "-" + conf.getInt(SQConfig.strSequenceLength, 1) + "-"
				+ conf.getInt(SQConfig.strMaxLenFreqPatterns, 10) + "-" + conf.getInt(SQConfig.strMinLocalSupport, 5)
				+ "-" + conf.getInt(SQConfig.strMinGlobalSupport, 50);
		fs.delete(new Path(outputPath), true);
		FileOutputFormat.setOutputPath(job, new Path(outputPath));
		
		String outputOutlierPath = conf.get(SQConfig.strOutlierPath) + "-"
				+ conf.getInt(SQConfig.strAlphabetSize, 10) + "-" + conf.getInt(SQConfig.strNumFreqPatterns, 1) + "-"
				+ conf.getInt(SQConfig.strSequenceLength, 1) + "-" + conf.getInt(SQConfig.strMaxLenFreqPatterns, 10)
				+ "-" + conf.getInt(SQConfig.strMinLocalSupport, 5) + "-"
				+ conf.getInt(SQConfig.strMinGlobalSupport, 50);
		fs.delete(new Path(outputOutlierPath), true);

		String outputViolationPath = conf.get(SQConfig.strViolationPath) + "-"
				+ conf.getInt(SQConfig.strAlphabetSize, 10) + "-" + conf.getInt(SQConfig.strNumFreqPatterns, 1) + "-"
				+ conf.getInt(SQConfig.strSequenceLength, 1) + "-" + conf.getInt(SQConfig.strMaxLenFreqPatterns, 10)
				+ "-" + conf.getInt(SQConfig.strMinLocalSupport, 5) + "-"
				+ conf.getInt(SQConfig.strMinGlobalSupport, 50);
		fs.delete(new Path(outputViolationPath), true);
		
		job.addCacheArchive(new URI(strFSName + conf.get(SQConfig.strDictionaryPath) + "-"
				+ conf.getInt(SQConfig.strAlphabetSize, 10) + "-" + conf.getInt(SQConfig.strNumFreqPatterns, 1) + "-"
				+ conf.getInt(SQConfig.strSequenceLength, 1) + "-" + conf.getInt(SQConfig.strMaxLenFreqPatterns, 10)
				+ "-" + conf.getInt(SQConfig.strMinLocalSupport, 5) + "-"
				+ conf.getInt(SQConfig.strMinGlobalSupport, 50)));
		
		/** print job parameter */
		System.err.println("alphabet size: " + conf.getInt(SQConfig.strAlphabetSize, 10));
		System.err.println("number of frequent patterns: " + conf.getInt(SQConfig.strNumFreqPatterns, 1));
		System.err.println("sequence length: " + conf.getInt(SQConfig.strSequenceLength, 10));
		System.err.println("max length of frequent patterns: " + conf.getInt(SQConfig.strMaxLenFreqPatterns, 10));
		System.err.println("number of pattern candidates: " + conf.getInt(SQConfig.strNumPatterns, 10));
		System.err.println("number of partitions: " + conf.getInt(SQConfig.strNumPartitions, 10));
		System.err.println("Local Support: " + conf.getInt(SQConfig.strLocalSupport, 10));
		System.err.println("Global Support:" + conf.getInt(SQConfig.strGlobalSupport, 50));
		System.err.println("Min Local Support: " + conf.getInt(SQConfig.strMinLocalSupport, 10));
		System.err.println("Min Global Support:" + conf.getInt(SQConfig.strMinGlobalSupport, 50));
		System.err.println("Output Path: " + outputPath);

		long begin = System.currentTimeMillis();
		job.waitForCompletion(true);
		long end = System.currentTimeMillis();
		long second = (end - begin) / 1000;
		System.err.println(job.getJobName() + " takes " + second + " seconds");
		org.apache.hadoop.mapreduce.Counters cn = job.getCounters();
		Counter c1 = cn.findCounter(Counters.deviceCount);
		System.err.println("Number of devices: " + c1.getValue());
	}

	public static void main(String[] args) {
		GenerateLongSequences generateLocalFS = new GenerateLongSequences();
		try {
			generateLocalFS.run(args);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
