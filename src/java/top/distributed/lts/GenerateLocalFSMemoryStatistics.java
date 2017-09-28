package top.distributed.lts;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.util.HashMap;
import java.util.HashSet;

import top.core.lts.local.noviolation.LocalFSDetection;
import top.inputs.InputSequence;
import top.parameterspace.LocalParameterSpace;
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

import top.utils.SQConfig;

public class GenerateLocalFSMemoryStatistics {

	public static enum Counters {
		fsCompTime, fslengthSum, fsNum, startLen, numCountSL, candidateCount, averageMemoryUse, countMemory;
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
			String newValue = "";
			for (int i = 0; i < 3; i++) {
				newValue += splitValue[i] + SQConfig.sepStrForIDDist;
			}
			newValue = newValue.substring(0, newValue.length() - 1);
			int id = Integer.parseInt(splitValue[0]);
			Text dat = new Text(newValue);
			IntWritable key_id = new IntWritable(id % numPartitions);
			context.write(key_id, dat);
		}
	}

	/**
	 * @author yizhouyan
	 *
	 */
	public static class DDReducer extends Reducer<IntWritable, Text, NullWritable, LongWritable> {

		private static int minSupportLocal = 10;
		private static int itemGap = 1;
		private static int seqGap = 10;
		private HashSet<String> globalFreqElements;
		public static long maxUsedMemory = 0;
		public static long maxUsedMemoryPerDevice = 0;

		public void setup(Context context) {
			/** get configuration from file */
			Configuration conf = context.getConfiguration();
			this.minSupportLocal = conf.getInt(SQConfig.strLocalSupport, 10);
			this.itemGap = conf.getInt(SQConfig.strEventGap, 1);
			this.seqGap = conf.getInt(SQConfig.strSeqGap, 10);
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
							System.out
									.println("Reading lts frequent elements from " + stats[i].getPath().toString());
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
				maxUsedMemoryPerDevice = 0;
				String line = oneValue.toString();
				String[] splitValues = line.split(SQConfig.sepSplitForIDDist);
				int seqId = Integer.parseInt(splitValues[0]);
				String deviceId = splitValues[1];
				String inputString = splitValues[2];
				long start = System.currentTimeMillis();
				InputSequence inputSequence = new InputSequence(inputString);
				LocalParameterSpace localParameterSpace = new LocalParameterSpace(this.minSupportLocal,itemGap, seqGap);
				LocalFSDetection localFS = new LocalFSDetection(localParameterSpace, inputSequence);
				localFS.Initialization(globalFreqElements);
				HashMap<String, Integer> tempLocalFS = localFS.LocalFrequentSequenceMining(seqId);

				long timeCost = (System.currentTimeMillis() - start)/1000;
//				context.getCounter(Counters.fsNum).increment(tempLocalFS.size());
//				for (String str : tempLocalFS.keySet()) {
//					String[] subs = str.split(SQConfig.sepStrForRecord);
//					context.getCounter(Counters.fsNum).increment(1);
//					context.getCounter(Counters.fslengthSum).increment(subs.length);
//				}
				context.getCounter(Counters.fsCompTime).increment(timeCost);
				context.getCounter(Counters.averageMemoryUse).increment(maxUsedMemoryPerDevice);
				maxUsedMemory = Math.max(maxUsedMemory, maxUsedMemoryPerDevice);
				context.getCounter(Counters.countMemory).increment(1);
			} // end collection data

		} // end reduce function

		@Override
		public void cleanup(Context context) throws IOException, InterruptedException {
			try {
				context.write(NullWritable.get(), new LongWritable(maxUsedMemory));
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	} // end reduce class

	public void run(String[] args) throws Exception {
		Configuration conf = new Configuration();
		conf.addResource(new Path("/usr/local/Cellar/hadoop/etc/hadoop/core-site.xml"));
		conf.addResource(new Path("/usr/local/Cellar/hadoop/etc/hadoop/hdfs-site.xml"));
		new GenericOptionsParser(conf, args).getRemainingArgs();
		/** set job parameter */
		Job job = Job.getInstance(conf, "Generate Local Frequent Sequences");

		job.setJarByClass(GenerateLocalFSMemoryStatistics.class);
		job.setMapperClass(DDMapper.class);
		job.setReducerClass(DDReducer.class);
		job.setMapOutputKeyClass(IntWritable.class);
		job.setMapOutputValueClass(Text.class);
		job.setOutputKeyClass(NullWritable.class);
		job.setOutputValueClass(LongWritable.class);
		job.setNumReduceTasks(Integer.parseInt(conf.get(SQConfig.strNumReducers)));

		String strFSName = conf.get("fs.default.name");
		FileInputFormat.addInputPath(job, new Path(conf.get(SQConfig.strInputDataset)));
		FileSystem fs = FileSystem.get(conf);
		String outputPath = conf.get(SQConfig.strLocalFSOutputLTS) + "Memory-"
				+ conf.getInt(SQConfig.strLocalSupport, 10) + "-" + conf.getInt(SQConfig.strEventGap, 1) + "-"
				+ conf.getInt(SQConfig.strSeqGap, 1);
		fs.delete(new Path(outputPath), true);
		FileOutputFormat.setOutputPath(job, new Path(outputPath));
		job.addCacheArchive(new URI(strFSName + conf.get(SQConfig.strGlobalFreqElements)));

		/** print job parameter */
		System.err.println("local support: " + conf.getInt(SQConfig.strLocalSupport, 10));
		System.err.println("event gap: " + conf.getInt(SQConfig.strEventGap, 1));
		System.err.println("sequence gap: " + conf.getInt(SQConfig.strSeqGap, 10));
		System.err.println("lts support: " + conf.getInt(SQConfig.strGlobalSupport, 10));
		System.err.println(LocalFSDetection.class.toString());
		long begin = System.currentTimeMillis();
		job.waitForCompletion(true);
		long end = System.currentTimeMillis();
		long second = (end - begin) / 1000;
		System.err.println(job.getJobName() + " takes " + second + " seconds");
		org.apache.hadoop.mapreduce.Counters cn = job.getCounters();
		Counter c1 = cn.findCounter(Counters.averageMemoryUse);
		Counter c2 = cn.findCounter(Counters.countMemory);
		Counter c3 = cn.findCounter(Counters.fsCompTime);
		Counter c4 = cn.findCounter(Counters.fsNum);
		Counter c5 = cn.findCounter(Counters.fslengthSum);
		double avg_time = c4.getValue() == 0 ? 0.0 : (c5.getValue() * 1.0 / c4.getValue());
		System.err.println("average memory usage: " + c1.getValue() * 1.0 / c2.getValue());

		System.out.println(conf.getInt(SQConfig.strSeqGap, 10) + "\t" + conf.getInt(SQConfig.strEventGap, 1) + "\t"
				+ conf.getInt(SQConfig.strLocalSupport, 10) + "\t" + conf.getInt(SQConfig.strGlobalSupport, 10) + "\t"
				+ c1.getValue() + "\t" + c2.getValue() + "\t" + c1.getValue() * 1.0 / c2.getValue() + "\t"
				+ c3.getValue() + "\t" + second);
		// + "\t"
		// + (c5.getValue() * 1.0 / c6.getValue()) + "\t" + c7.getValue());
	}

	public static void main(String[] args) throws Exception {
		GenerateLocalFSMemoryStatistics generateLocalFS = new GenerateLocalFSMemoryStatistics();
		generateLocalFS.run(args);
	}
}
