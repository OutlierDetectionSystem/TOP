package top.distributed.lts.explanation;

import top.core.lts.local.withviolation.withts.explanation.LocalFSDetectionWithTimeStampViolation;
import top.inputs.InputSequence;
import top.parameterspace.LocalParameterSpace;
import top.inputs.InputSequenceWithTS;
import top.parameterspace.LocalParameterSpaceWithTS;
import top.core.lts.local.withviolation.basic.ViolationSequence;
import top.utils.SQConfig;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.LongWritable;
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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.util.*;

public class GenerateLocalFSViolationWithTS {

	public static enum Counters {
		fsCompTime, fslengthSum, fsNum, startLen, numCountSL, candidateCount, runOutOfMemory;
	}

	public static class DDMapper extends Mapper<LongWritable, Text, IntWritable, Text> {

		/** number of divisions where data is divided into (set by user) */
		private int numPartitions = 50;

		protected void setup(Context context) throws IOException, InterruptedException {
			Configuration conf = context.getConfiguration();
			numPartitions = conf.getInt(SQConfig.strNumPartitions, 50);
		}

		public void map(LongWritable key, Text value, Context context) throws IOException, InterruptedException {
			String[] splitValue = value.toString().split(SQConfig.sepStrForKeyValue);
			// String newValue = "";
			// for (int i = 0; i < 3; i++) {
			// newValue += splitValue[i] + SQConfig.sepStrForIDDist;
			// }
			// newValue = newValue.substring(0, newValue.length() - 1);
			int id = Integer.parseInt(splitValue[0]);
			// Text dat = new Text(newValue);
			IntWritable key_id = new IntWritable(id % numPartitions);
			context.write(key_id, value);
		}
	}

	/**
	 * @author yizhouyan
	 *
	 */
	public static class DDReducer extends Reducer<IntWritable, Text, IntWritable, Text> {

		private static int minSupportLocal = 10;
		private static int itemGap = 1;
		 private static int seqGap = 10;
		private static long itemTimeInterval = 10000;
		private static long seqTimeInterval = 60000;
		private static int violationSupportLocal = 5;
		private HashSet<String> globalFreqElements;
		private MultipleOutputs mos;

		public void setup(Context context) {
			/** get configuration from file */
			Configuration conf = context.getConfiguration();
			mos = new MultipleOutputs(context);
			this.minSupportLocal = conf.getInt(SQConfig.strLocalSupport, 10);
			this.itemGap = conf.getInt(SQConfig.strEventGap, 1);
			this.seqGap = conf.getInt(SQConfig.strSeqGap, 10);
			this.itemTimeInterval = conf.getLong(SQConfig.strEventTimeInterval, 1000);
			this.seqTimeInterval = conf.getLong(SQConfig.strSeqTimeInterval, 60000);
			// this.seqGap = conf.getInt(SQConfig.strSeqGap, 10);
			this.violationSupportLocal = conf.getInt(SQConfig.strViolationLocalSupport, 5);
			this.globalFreqElements = new HashSet<String>();

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
						if (!stats[i].isDirectory()) {
							System.out.println("Reading lts frequent elements from " + stats[i].getPath().toString());
							FSDataInputStream currentStream;
							BufferedReader currentReader;
							currentStream = fs.open(stats[i].getPath());
							currentReader = new BufferedReader(new InputStreamReader(currentStream));
							String line;
							while ((line = currentReader.readLine()) != null) {
								/** parse line */
								this.globalFreqElements.add(line);
							}
						}
					} // end for (int i = 0; i < stats.length; ++i)
				} // end for (URI path : cacheFiles)

			} catch (IOException ioe) {
				System.err.println("Caught exception while getting cached files");
			}
		}

		public void reduce(IntWritable key, Iterable<Text> values, Context context) throws IOException {
			// collect data

			for (Text oneValue : values) {
				try {
					String line = oneValue.toString();
					String[] splitValues = line.split(SQConfig.sepStrForKeyValue);
					int seqId = Integer.parseInt(splitValues[0]);
					String deviceId = splitValues[1];
					String inputString = splitValues[2];
					// System.out.println(inputString.length());
					long start = System.currentTimeMillis();
					LocalParameterSpace localParameterSpace = new LocalParameterSpaceWithTS(this.minSupportLocal, itemGap, seqGap,
							this.itemTimeInterval, this.seqTimeInterval);
					InputSequence inputSequence = new InputSequenceWithTS(inputString);
					LocalFSDetectionWithTimeStampViolation localFS = new LocalFSDetectionWithTimeStampViolation(
							inputSequence, localParameterSpace, this.violationSupportLocal);
					localFS.Initialization(globalFreqElements);
					ArrayList<String> tempLocalFS = localFS.LocalFrequentSequenceMining(seqId);
					long timeCost = (System.currentTimeMillis() - start);
					context.getCounter(Counters.fsCompTime).increment(timeCost);
					// output local frequent sequences
					Configuration conf = context.getConfiguration();
					String outputOutlierPath = conf.get(SQConfig.strLocalFSOutputLTS) + "-"
							+ conf.getInt(SQConfig.strLocalSupport, 10) + "-" + conf.getInt(SQConfig.strEventGap, 1)
							+ "-" + conf.getInt(SQConfig.strSeqGap, 1);
					IntWritable outputKey = new IntWritable();
					Text outputValue = new Text();
					String outputResult = deviceId + SQConfig.sepStrForKeyValue;
					for (String str : tempLocalFS) {
						outputResult += str + SQConfig.sepStrForIDDist;
						String[] subs = str.split(SQConfig.sepStrForRecord);
						context.getCounter(Counters.fsNum).increment(1);
						context.getCounter(Counters.fslengthSum).increment(subs.length);
					}
					if (outputResult.length() > 0)
						outputResult = outputResult.substring(0, outputResult.length() - 1);
					outputValue.set(outputResult);
					outputKey.set(seqId);
					try {
						mos.write(outputKey, outputValue, outputOutlierPath + "/LocalFS-" + context.getTaskAttemptID());
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					// output local violation sequences
					HashMap<String, ViolationSequence> violations = localFS.getViolations();
					outputResult = deviceId + SQConfig.sepStrForKeyValue;
					// output format: violation seq (occurrences) | violated seq1 | violated seq2 "tab"
					for (Map.Entry<String, ViolationSequence> tempViolation : violations.entrySet()) {
						outputResult += tempViolation.getValue().printViolation() + SQConfig.sepStrForKeyValue;
					}
//					if (outputResult.length() > 0)
//						outputResult = outputResult.substring(0, outputResult.length() - 1);
					outputValue.set(outputResult);
					String outputLocalViolationPath = conf.get(SQConfig.strLocalViolationOutput) + "-"
							+ conf.getInt(SQConfig.strLocalSupport, 10) + "-" + conf.getInt(SQConfig.strEventGap, 1)
							+ "-" + conf.getInt(SQConfig.strSeqGap, 1);
					try {
						mos.write(outputKey, outputValue,
								outputLocalViolationPath + "/LocalViolation-" + context.getTaskAttemptID());
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				} catch (Error e) {
					String line = oneValue.toString();
					String[] splitValues = line.split(SQConfig.sepStrForKeyValue);
					int seqId = Integer.parseInt(splitValues[0]);
					String deviceId = splitValues[1];
					String inputString = splitValues[2];
					e.printStackTrace();
					System.out.println("Catch Run out of memory exception " + line.length() + "," + seqId + "," + deviceId);
					context.getCounter(Counters.runOutOfMemory).increment(1);
				} catch (Exception e) {
					String line = oneValue.toString();
					String[] splitValues = line.split(SQConfig.sepStrForKeyValue);
					int seqId = Integer.parseInt(splitValues[0]);
					String deviceId = splitValues[1];
					String inputString = splitValues[2];
					e.printStackTrace();
					System.out.println("Catch Run out of memory exception " + line.length() + "," + seqId + "," + deviceId);
					context.getCounter(Counters.runOutOfMemory).increment(1);
				}
			} // end collection data

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
		Job job = Job.getInstance(conf, "Generate Local Frequent Sequences--- With Violation and time stamp");

		job.setJarByClass(GenerateLocalFSViolationWithTS.class);
		job.setMapperClass(DDMapper.class);
		job.setReducerClass(DDReducer.class);
		job.setMapOutputKeyClass(IntWritable.class);
		job.setMapOutputValueClass(Text.class);
		job.setOutputKeyClass(IntWritable.class);
		job.setOutputValueClass(Text.class);
		job.setNumReduceTasks(Integer.parseInt(conf.get(SQConfig.strNumReducers)));

		String strFSName = conf.get("fs.default.name");
		FileInputFormat.addInputPath(job, new Path(conf.get(SQConfig.strInputDataset)));
		FileSystem fs = FileSystem.get(conf);
		// FileOutputFormat.setOutputPath(job, new Path(outputPath));
		MultipleOutputs.addNamedOutput(job, "localFS", TextOutputFormat.class, IntWritable.class, Text.class);
		MultipleOutputs.addNamedOutput(job, "violations", TextOutputFormat.class, IntWritable.class, Text.class);

		String outputPath = conf.get(SQConfig.strLocalFSOutputLTS) + "-" + conf.getInt(SQConfig.strLocalSupport, 10)
				+ "-" + conf.getInt(SQConfig.strEventGap, 1) + "-" + conf.getInt(SQConfig.strSeqGap, 1);
		fs.delete(new Path(outputPath), true);
		FileOutputFormat.setOutputPath(job, new Path(outputPath));
		String outputLocalViolationPath = conf.get(SQConfig.strLocalViolationOutput) + "-"
				+ conf.getInt(SQConfig.strLocalSupport, 10) + "-" + conf.getInt(SQConfig.strEventGap, 1) + "-"
				+ conf.getInt(SQConfig.strSeqGap, 1);
		fs.delete(new Path(outputLocalViolationPath), true);

		job.addCacheArchive(new URI(strFSName + conf.get(SQConfig.strGlobalFreqElements)));

		/** print job parameter */
		System.err.println("local support: " + conf.getInt(SQConfig.strLocalSupport, 10));
		System.err.println("event gap: " + conf.getInt(SQConfig.strEventGap, 1));
		System.err.println("sequence gap: " + conf.getInt(SQConfig.strSeqGap, 10));
		System.err.println("lts support: " + conf.getInt(SQConfig.strGlobalSupport, 10));
		long begin = System.currentTimeMillis();
		job.waitForCompletion(true);
		long end = System.currentTimeMillis();
		long second = (end - begin) / 1000;
		System.err.println(job.getJobName() + " takes " + second + " seconds");
		org.apache.hadoop.mapreduce.Counters cn = job.getCounters();
		Counter c1 = cn.findCounter(Counters.fsCompTime);
		Counter c2 = cn.findCounter(Counters.fsNum);
		Counter c4 = cn.findCounter(Counters.fslengthSum);
		Counter c5 = cn.findCounter(Counters.startLen);
		Counter c6 = cn.findCounter(Counters.numCountSL);
		Counter c7 = cn.findCounter(Counters.candidateCount);
		System.err.println("total time: " + c1.getValue());
		System.err.println("Number of fs: " + c2.getValue());
		double avg_time = c2.getValue() == 0 ? 0.0 : (c4.getValue() * 1.0 / c2.getValue());
		System.err.println("Avg fs length: " + avg_time);
		// System.out.println(
		// "sequence gap \t event gap \t local support \t lts support \t total
		// time \t end to end \t Num FS \t Avg length");
		System.out.println(conf.getInt(SQConfig.strSeqGap, 10) + "\t" + conf.getInt(SQConfig.strEventGap, 1) + "\t"
				+ conf.getInt(SQConfig.strLocalSupport, 10) + "\t" + conf.getInt(SQConfig.strGlobalSupport, 10) + "\t"
				+ c1.getValue() * 1.0 / 1000 + "\t" + second + "\t" + c2.getValue() + "\t" + avg_time + "\t");
		// + "\t"
		// + (c5.getValue() * 1.0 / c6.getValue()) + "\t" + c7.getValue());
	}

	public static void main(String[] args) throws Exception {
		GenerateLocalFSViolationWithTS generateLocalFS = new GenerateLocalFSViolationWithTS();
		generateLocalFS.run(args);
	}
}
