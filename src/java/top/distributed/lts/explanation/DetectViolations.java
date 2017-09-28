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
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.util.GenericOptionsParser;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashSet;

public class DetectViolations {
	public static enum Counters {
		numViolations;
	}

	public static class DDMapper extends Mapper<LongWritable, Text, IntWritable, Text> {

		public void map(LongWritable key, Text value, Context context) throws IOException, InterruptedException {
			String[] subs = value.toString().split(SQConfig.sepStrForKeyValue);
			IntWritable key_id = new IntWritable(Integer.parseInt(subs[0]));
			context.write(key_id, value);
		}
	}

	/**
	 * @author yizhouyan
	 *
	 */
	public static class DDReducer extends Reducer<IntWritable, Text, NullWritable, Text> {
		private HashSet<String> finalGlobalFreqSeqs;

		public void setup(Context context) {
			/** get configuration from file */
			Configuration conf = context.getConfiguration();
			this.finalGlobalFreqSeqs = new HashSet<String>();
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
							System.out
									.println("Reading global frequent sequence from " + stats[i].getPath().toString());
							FSDataInputStream currentStream;
							BufferedReader currentReader;
							currentStream = fs.open(stats[i].getPath());
							currentReader = new BufferedReader(new InputStreamReader(currentStream));
							String line;
							while ((line = currentReader.readLine()) != null) {
								this.finalGlobalFreqSeqs.add(line);
							}
						}
					} // end for (int i = 0; i < stats.length; ++i)
				} // end for (URI path : cacheFiles)

			} catch (IOException ioe) {
				System.err.println("Caught exception while getting cached files");
			}
		}

		public void reduce(IntWritable key, Iterable<Text> values, Context context) throws IOException {
			String violationForOneDevice = values.iterator().next().toString();
			int deviceId = key.get();
			String[] splitValues = violationForOneDevice.split(SQConfig.sepStrForKeyValue);
			String deviceInfo = splitValues[1];
			String outputStr = "";

			for (int i = 2; i < splitValues.length; i++) {
				String[] splitViolationsThisDevice = splitValues[i].split(SQConfig.sepSplitForIDDist);
				String violationSeqWithOccurrences = splitViolationsThisDevice[0];
				int beginOccurrenceIndex = violationSeqWithOccurrences.indexOf("[");
				String violationSeq = violationSeqWithOccurrences.substring(0, beginOccurrenceIndex);
				String violationOccurrences = violationSeqWithOccurrences.substring(beginOccurrenceIndex,
						violationSeqWithOccurrences.length());
				// System.out.println("Complete: " +
				// violationSeqWithOccurrences);
				// System.out.println("Violation: " + violationSeq);
				// System.out.println("Occurrences: " + violationOccurrences);
				if (finalGlobalFreqSeqs.contains(violationSeq)){
//					System.out.println("Remove Violating Sequence:" + deviceId + "," + violationSeq);
					continue;
				}
				ArrayList<String> remainViolatedSeq = new ArrayList<String>();
				for (int j = 1; j < splitViolationsThisDevice.length; j++) {
					if (finalGlobalFreqSeqs.contains(splitViolationsThisDevice[j])) {
						remainViolatedSeq.add(splitViolationsThisDevice[j]);
					}
//					else{
//						System.out.println("Remove Violated Sequence:" + deviceId + "," + splitViolationsThisDevice[j]);
//					}
				}
				if(remainViolatedSeq.size() > 0){
					outputStr += violationSeqWithOccurrences;
					for(String remainViolation: remainViolatedSeq)
						outputStr += "|" + remainViolation;
					outputStr += SQConfig.sepStrForKeyValue;
					context.getCounter(Counters.numViolations).increment(remainViolatedSeq.size());
				}
			}
			if (outputStr.length() > 0) {
				try {
					context.write(NullWritable.get(), new Text(deviceId + SQConfig.sepStrForKeyValue + deviceInfo
							+ SQConfig.sepStrForKeyValue + outputStr));
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		} // end reduce function

	} // end reduce class

	public void run(String[] args) throws Exception {
		Configuration conf = new Configuration();
		conf.addResource(new Path("/usr/local/Cellar/hadoop/etc/hadoop/core-site.xml"));
		conf.addResource(new Path("/usr/local/Cellar/hadoop/etc/hadoop/hdfs-site.xml"));
		new GenericOptionsParser(conf, args).getRemainingArgs();
		/** set job parameter */
		Job job = Job.getInstance(conf, "Detect Local Outliers");

		job.setJarByClass(DetectViolations.class);
		job.setMapperClass(DDMapper.class);
		job.setReducerClass(DDReducer.class);
		job.setMapOutputKeyClass(IntWritable.class);
		job.setMapOutputValueClass(Text.class);
		job.setOutputKeyClass(NullWritable.class);
		job.setOutputValueClass(Text.class);
		job.setNumReduceTasks(conf.getInt(SQConfig.strNumReducers, 100));

		String strFSName = conf.get("fs.default.name");
		String inputPath = conf.get(SQConfig.strLocalViolationOutput) + "-" + conf.getInt(SQConfig.strLocalSupport, 10)
				+ "-" + conf.getInt(SQConfig.strEventGap, 1) + "-" + conf.getInt(SQConfig.strSeqGap, 1);
		FileInputFormat.addInputPath(job, new Path(inputPath));
		FileSystem fs = FileSystem.get(conf);
		String outputPath = conf.get(SQConfig.strFinalViolationOutput) + "-" + conf.getInt(SQConfig.strLocalSupport, 10)
				+ "-" + conf.getInt(SQConfig.strGlobalSupport, 10) + "-" + conf.getInt(SQConfig.strEventGap, 1) + "-"
				+ conf.getInt(SQConfig.strSeqGap, 1);
		fs.delete(new Path(outputPath), true);
		FileOutputFormat.setOutputPath(job, new Path(outputPath));
		job.addCacheArchive(new URI(strFSName + conf.get(SQConfig.strGlobalFSOutputForViolation) + "-"
				+ conf.getInt(SQConfig.strLocalSupport, 10) + "-" + conf.getInt(SQConfig.strEventGap, 1) + "-"
				+ conf.getInt(SQConfig.strSeqGap, 1)));

		/** print job parameter */
		System.err.println("local support: " + conf.getInt(SQConfig.strLocalSupport, 10));
		System.err.println("lts support: " + conf.getInt(SQConfig.strGlobalSupport, 10));
		System.err.println("event gap: " + conf.getInt(SQConfig.strEventGap, 1));
		System.err.println("sequence gap: " + conf.getInt(SQConfig.strSeqGap, 10));
		System.err.println("Violation local support" + conf.getInt(SQConfig.strViolationLocalSupport, 10));
		long begin = System.currentTimeMillis();
		job.waitForCompletion(true);
		long end = System.currentTimeMillis();
		long second = (end - begin) / 1000;
		System.err.println(job.getJobName() + " takes " + second + " seconds");
		org.apache.hadoop.mapreduce.Counters cn = job.getCounters();
		Counter c1 = cn.findCounter(Counters.numViolations);
		System.out.println(c1.getValue());
	}

	public static void main(String[] args) throws Exception {
		DetectViolations detectViolations = new DetectViolations();
		detectViolations.run(args);
	}
}
