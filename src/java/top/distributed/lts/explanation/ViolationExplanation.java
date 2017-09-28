package top.distributed.lts.explanation;

import top.utils.SQConfig;
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
import org.apache.hadoop.mapreduce.lib.input.MultipleInputs;
import org.apache.hadoop.mapreduce.lib.input.TextInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.util.GenericOptionsParser;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;

public class ViolationExplanation {

	public static enum Counters {
		violationNum;
	}

	public static class ReadInputDataMapper extends Mapper<LongWritable, Text, IntWritable, Text> {
		HashSet<String> devicesWithViolation;

		protected void setup(Context context) throws IOException, InterruptedException {
			this.devicesWithViolation = new HashSet<String>();
			Configuration conf = context.getConfiguration();
			try {
				URI[] cacheFiles = context.getCacheArchives();

				if (cacheFiles == null || cacheFiles.length < 2) {
					System.out.println("not enough cache files");
					return;
				}
				for (URI path : cacheFiles) {
					String filename = path.toString();
					FileSystem fs = FileSystem.get(conf);

					FileStatus[] stats = fs.listStatus(new Path(filename));
					for (int i = 0; i < stats.length; ++i) {
						if (!stats[i].isDirectory() && stats[i].getPath().toString().contains("violation-devices")) {
							System.out.println("Reading devices from " + stats[i].getPath().toString());
							FSDataInputStream currentStream;
							BufferedReader currentReader;
							currentStream = fs.open(stats[i].getPath());
							currentReader = new BufferedReader(new InputStreamReader(currentStream));
							String line;
							while ((line = currentReader.readLine()) != null) {
								this.devicesWithViolation.add(line);
							}
						}
					} // end for (int i = 0; i < stats.length; ++i)
					System.out.println("Num of devices with violations: " + this.devicesWithViolation.size());
				} // end for (URI path : cacheFiles)
			} catch (IOException ioe) {
				System.err.println("Caught exception while getting cached files");
			}
		}

		public void map(LongWritable key, Text value, Context context) throws IOException, InterruptedException {
			String[] splitValue = value.toString().split(SQConfig.sepStrForKeyValue);
			if (this.devicesWithViolation.contains(splitValue[0])) {
				// System.out.println("Input Data: " + splitValue[0] + "," +
				// splitValue[1]);
				int id = Integer.parseInt(splitValue[0]);
				context.write(new IntWritable(id), new Text("Data:" + value.toString()));
			}
		}
	}

	public static class ReadViolationMapper extends Mapper<LongWritable, Text, IntWritable, Text> {
		public void map(LongWritable key, Text value, Context context) throws IOException, InterruptedException {
			String[] splitValue = value.toString().split(SQConfig.sepStrForKeyValue);
			int id = Integer.parseInt(splitValue[0]);
			context.write(new IntWritable(id), new Text("Violation:" + value.toString()));
		}
	}

	/**
	 * @author yizhouyan
	 *
	 */
	public static class DDReducer extends Reducer<IntWritable, Text, NullWritable, Text> {
		private static int seqGap = 10;
		private HashMap<String, String> metaDataMapping;

		public void setup(Context context) {
			/** get configuration from file */
			Configuration conf = context.getConfiguration();
			this.seqGap = conf.getInt(SQConfig.strSeqGap, 10);
			this.metaDataMapping = new HashMap<String, String>();

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
						if (!stats[i].isDirectory() && stats[i].getPath().toString().contains("metadata")) {
							System.out.println("Reading meta data from " + stats[i].getPath().toString());
							FSDataInputStream currentStream;
							BufferedReader currentReader;
							currentStream = fs.open(stats[i].getPath());
							currentReader = new BufferedReader(new InputStreamReader(currentStream));
							String line;
							while ((line = currentReader.readLine()) != null) {
								/** parse line */
								String[] splitsStr = line.split(SQConfig.sepStrForKeyValue);
								this.metaDataMapping.put(splitsStr[0], "(" + splitsStr[1] + ")" + splitsStr[2]);
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
			int deviceId = key.get();
			String originalInputData = "";
			String deviceInStr = "";
			ArrayList<String> violationsAndOccurrences = new ArrayList<String>();
			for (Text oneValue : values) {
				String line = oneValue.toString();
				String[] splitValues = line.split(SQConfig.sepStrForKeyValue);
				deviceInStr = splitValues[1];
				if (splitValues[0].contains("Data")) {
					originalInputData = splitValues[2];
				} else if (splitValues[0].contains("Violation")) {
					violationsAndOccurrences.add(splitValues[2] + "\t" + splitValues[3]);
				}
			} // end collection data
			ArrayList<String> originalDataSplit = new ArrayList<String>(Arrays.asList(originalInputData.split(",")));
			// for each violation, find original sequence
			for (String oneViolation : violationsAndOccurrences) {
				String[] subs = oneViolation.split("\t");
				String[] subsubs = subs[0].split("\\|");
				String violationSeq = subsubs[0];
				String violatedSeq = subsubs[1];
				String[] occurrences = subs[1].replaceAll("]", "").split("\\[");
				HashMap<String, String> occurrenceMapOriginalSeq = new HashMap<String, String>();
				// check each occurrence, if one holds, then this violation
				// holds,
				// keep the original sequence of the ones that holds
				for (String oneOccurrence : occurrences) {
					if (oneOccurrence.length() > 0) {
						String validateRes = checkViolation(violationSeq, violatedSeq, originalDataSplit,
								oneOccurrence);
						if (validateRes != null) {
							occurrenceMapOriginalSeq.put(oneOccurrence, validateRes);
						}
					}
				}
				if (occurrenceMapOriginalSeq.size() > 0) {
					// output violation and originalSequence
					try {
						context.getCounter(Counters.violationNum).increment(1);
						context.write(NullWritable.get(), new Text("Device: " + deviceInStr + " Seq Id: " + deviceId));
						context.write(NullWritable.get(), new Text("Sequence: " + violationSeq));
						context.write(NullWritable.get(), new Text("Violates BCS Sequence: " + violatedSeq));
						context.write(NullWritable.get(), new Text("Original Sequence Snapshots: "));
						for (String snapshot : occurrenceMapOriginalSeq.values()) {
							context.write(NullWritable.get(), new Text(snapshot));
						}
						context.write(NullWritable.get(), new Text(""));
						if (metaDataMapping != null) {
							// parse violation sequence
							context.write(NullWritable.get(), new Text("Sequence: "));
							String originalMeta = "";
							for (String tempStr : violationSeq.split(SQConfig.sepStrForRecord)) {
								originalMeta += metaDataMapping.get(tempStr.trim()) + SQConfig.sepStrForKeyValue;
							}
							context.write(NullWritable.get(), new Text(originalMeta + "\n"));
							context.write(NullWritable.get(), new Text("Violates BCS Sequence: "));
							String violationMeta = "";
							for (String tempStr : violatedSeq.split(SQConfig.sepStrForRecord)) {
								violationMeta += metaDataMapping.get(tempStr.trim()) + SQConfig.sepStrForKeyValue;
							}
							context.write(NullWritable.get(), new Text(violationMeta + "\n\n\n"));
						}
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
		} // end reduce function

		public String checkViolation(String violationSeq, String violatedSeq, ArrayList<String> originalDataSplit,
				String oneOccurrence) {
			String[] splitOneOccurrence = oneOccurrence.split(",");
			int startIndex = Math.max(0, Integer.parseInt(splitOneOccurrence[0]) - (this.seqGap + 1));
			int endIndex = Math.min(originalDataSplit.size() - 1,
					Integer.parseInt(splitOneOccurrence[splitOneOccurrence.length - 1]) + (this.seqGap + 1));
			// String[] splitViolationSeq = violationSeq.split(",");
			ArrayList<String> splitViolatedSeq = new ArrayList<String>(Arrays.asList(violatedSeq.split(",")));
			ArrayList<String> expandedViolationSeq = new ArrayList<String>();
			for (int i = startIndex; i < endIndex; i++) {
				expandedViolationSeq.add(originalDataSplit.get(i).split("\\|")[0]);
			}

			if (!strArrayContains(expandedViolationSeq, splitViolatedSeq)) {
				String returnSeq = "";
				for (int i = startIndex; i < endIndex; i++) {
					returnSeq += originalDataSplit.get(i).split("\\|")[0] + ",";
				}
				returnSeq = returnSeq.substring(0, returnSeq.length() - 1);
//				System.out.println(returnSeq);
				return returnSeq;
				// System.out.println();
			}
			return null;
		}

		public boolean strArrayContains(ArrayList<String> strList1, ArrayList<String> strList2) {
			boolean isContained = false;

			for (int i = 0; i < strList1.size() - strList2.size() + 1; i++) {
				int k = i;
				int j = 0;
				while (k < strList1.size() && j < strList2.size()) {
					if (strList1.get(k).equals(strList2.get(j))) {
						k++;
						j++;
					} else {
						k++;
					}
				}
				if (j == strList2.size()) {
					isContained = true;
					break;
				}
			}

			return isContained;

		}

	} // end reduce class

	public void run(String[] args) throws Exception {
		Configuration conf = new Configuration();
		conf.addResource(new Path("/usr/local/Cellar/hadoop/etc/hadoop/core-site.xml"));
		conf.addResource(new Path("/usr/local/Cellar/hadoop/etc/hadoop/hdfs-site.xml"));
		new GenericOptionsParser(conf, args).getRemainingArgs();
		/** set job parameter */
		Job job = Job.getInstance(conf, "Violation Explanation");
		job.setJarByClass(ViolationExplanation.class);
		MultipleInputs.addInputPath(job, new Path(conf.get(SQConfig.strInputDataset)), TextInputFormat.class,
				ReadInputDataMapper.class);
		String filteredViolationPath = conf.get(SQConfig.strFinalFilteredViolationOutput) + "-"
				+ conf.getInt(SQConfig.strLocalSupport, 10) + "-" + conf.getInt(SQConfig.strGlobalSupport, 10) + "-"
				+ conf.getInt(SQConfig.strEventGap, 1) + "-" + conf.getInt(SQConfig.strSeqGap, 1);
		MultipleInputs.addInputPath(job, new Path(filteredViolationPath), TextInputFormat.class,
				ReadViolationMapper.class);
		job.setReducerClass(DDReducer.class);
		job.setMapOutputKeyClass(IntWritable.class);
		job.setMapOutputValueClass(Text.class);
		job.setOutputKeyClass(NullWritable.class);
		job.setOutputValueClass(Text.class);
		// job.setNumReduceTasks(Integer.parseInt(conf.get(SQConfig.strNumReducers)));
		job.setNumReduceTasks(1);
		String strFSName = conf.get("fs.default.name");
		FileSystem fs = FileSystem.get(conf);

		String outputPath = conf.get(SQConfig.strViolationWithExplanationOutput) + "-"
				+ conf.getInt(SQConfig.strLocalSupport, 10) + "-" + conf.getInt(SQConfig.strGlobalSupport, 10) + "-"
				+ conf.getInt(SQConfig.strEventGap, 1) + "-" + conf.getInt(SQConfig.strSeqGap, 1);
		fs.delete(new Path(outputPath), true);
		FileOutputFormat.setOutputPath(job, new Path(outputPath));
		String deviceWithViolationPath = conf.get(SQConfig.strDevicesWithViolations) + "-"
				+ conf.getInt(SQConfig.strLocalSupport, 10) + "-" + conf.getInt(SQConfig.strGlobalSupport, 10) + "-"
				+ conf.getInt(SQConfig.strEventGap, 1) + "-" + conf.getInt(SQConfig.strSeqGap, 1);
		job.addCacheArchive(new URI(strFSName + deviceWithViolationPath));
		job.addCacheArchive(new URI(strFSName + conf.get(SQConfig.strMetaDataInput)));

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
		Counter c1 = cn.findCounter(Counters.violationNum);
		System.out.println(c1.getValue());
	}

	public static void main(String[] args) throws Exception {
		ViolationExplanation explanation = new ViolationExplanation();
		explanation.run(args);
	}
}
