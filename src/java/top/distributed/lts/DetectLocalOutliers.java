package top.distributed.lts;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map.Entry;

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

public class DetectLocalOutliers {
	public static enum Counters {
		numOutliers, sumLength;
	}

	public static class DDMapper extends Mapper<LongWritable, Text, IntWritable, Text> {

		public void map(LongWritable key, Text value, Context context) throws IOException, InterruptedException {
			IntWritable key_id = new IntWritable(0);
			context.write(key_id, value);
		}
	}

	/**
	 * @author yizhouyan
	 *
	 */
	public static class DDReducer extends Reducer<IntWritable, Text, NullWritable, Text> {

		private static int thresholdForLocalFSOutliers = 2;
		private HashMap<String, String> metaDataMapping;

		public void setup(Context context) {
			/** get configuration from file */
			Configuration conf = context.getConfiguration();

			this.thresholdForLocalFSOutliers = conf.getInt(SQConfig.strOutlierThreshold, 2);
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
						if (!stats[i].isDirectory()) {
							System.out.println("Reading meta data from " + stats[i].getPath().toString());
							FSDataInputStream currentStream;
							BufferedReader currentReader;
							currentStream = fs.open(stats[i].getPath());
							currentReader = new BufferedReader(new InputStreamReader(currentStream));
							String line;
							while ((line = currentReader.readLine()) != null) {
								/** parse line */
								String[] splitsStr = line.split(SQConfig.sepStrForKeyValue);
//								this.metaDataMapping.put(splitsStr[0], "(" + splitsStr[1] + ")" + splitsStr[2]);
								this.metaDataMapping.put(splitsStr[0], splitsStr[1]);
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
			HashMap<Integer, String[]> allFreqSeqs = new HashMap<Integer, String[]>();
			HashMap<Integer, String> alldevices = new HashMap<Integer, String>();
			HashMap<String, Integer> gbFreqSeqs = new HashMap<String, Integer>();

			for (Text oneValue : values) {
				String line = oneValue.toString();
				String[] splitValues = line.split(SQConfig.sepStrForKeyValue);
				if (splitValues.length == 2)
					continue;
				int seqId = Integer.parseInt(splitValues[0]);
				String deviceId = splitValues[1];
				String inputString = splitValues[2];
				alldevices.put(seqId, deviceId);
				String[] tempFreqSeqOneStr = inputString.split(SQConfig.sepSplitForIDDist);
				allFreqSeqs.put(seqId, tempFreqSeqOneStr);
				for (String str : tempFreqSeqOneStr) {
					if (gbFreqSeqs.containsKey(str))
						gbFreqSeqs.put(str, gbFreqSeqs.get(str) + 1);
					else
						gbFreqSeqs.put(str, 1);
				}

			} // end collection data

			HashSet<String> finalGBFreqSeq = new HashSet<String>();
			for (String str : gbFreqSeqs.keySet()) {
				if (gbFreqSeqs.get(str) >= this.thresholdForLocalFSOutliers) {
					finalGBFreqSeq.add(str);
				}
			}

			// detect outliers by traversing the dataset again
			for (Entry<Integer, String[]> freqSeqOneStr : allFreqSeqs.entrySet()) {
				HashSet<String> tempOutliers = new HashSet<String>();
				for (String curStr : freqSeqOneStr.getValue()) {
					if (!finalGBFreqSeq.contains(curStr))
						tempOutliers.add(curStr);
				}
				try {
					if (tempOutliers.size() != 0) {
						context.getCounter(Counters.numOutliers).increment(tempOutliers.size());
						context.write(NullWritable.get(), new Text("Seq id: " + freqSeqOneStr.getKey() + "\n"
								+ "Device id: " + alldevices.get(freqSeqOneStr.getKey()) + "\n"));
						for (String curStr : tempOutliers) {
							String outputMeta = "";
							String[] subStr = curStr.split(SQConfig.sepStrForRecord);
							context.getCounter(Counters.sumLength).increment(subStr.length);
							for (String tempStr : subStr) {
								outputMeta += metaDataMapping.get(tempStr) + "\t";
								// System.out.println(metaDataMapping.get(tempStr));
							}

							context.write(NullWritable.get(), new Text(curStr + "\n" + outputMeta + "\n"));

						}
						context.write(NullWritable.get(), new Text("\n"));
					}
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

		job.setJarByClass(DetectLocalOutliers.class);
		job.setMapperClass(DDMapper.class);
		job.setReducerClass(DDReducer.class);
		job.setMapOutputKeyClass(IntWritable.class);
		job.setMapOutputValueClass(Text.class);
		job.setOutputKeyClass(NullWritable.class);
		job.setOutputValueClass(Text.class);
		job.setNumReduceTasks(1);

		String strFSName = conf.get("fs.default.name");
		String inputPath = conf.get(SQConfig.strLocalFSOutputLTS) + "-" + conf.getInt(SQConfig.strLocalSupport, 10)
				+ "-" + conf.getInt(SQConfig.strEventGap, 1) + "-" + conf.getInt(SQConfig.strSeqGap, 1);
		FileInputFormat.addInputPath(job, new Path(inputPath));
		FileSystem fs = FileSystem.get(conf);
		String outputPath = conf.get(SQConfig.strLocalOutlierOutput) + "-" + conf.getInt(SQConfig.strLocalSupport, 10)
				+ "-" + conf.getInt(SQConfig.strGlobalSupport, 10) + "-" + conf.getInt(SQConfig.strEventGap, 1)
				+ "-" + conf.getInt(SQConfig.strSeqGap, 1);
		fs.delete(new Path(outputPath), true);
		FileOutputFormat.setOutputPath(job, new Path(outputPath));
		job.addCacheArchive(new URI(strFSName + conf.get(SQConfig.strMetaDataInput)));

		/** print job parameter */
		System.err.println("local support: " + conf.getInt(SQConfig.strLocalSupport, 10));
		System.err.println("lts support: " + conf.getInt(SQConfig.strGlobalSupport, 10));
		System.err.println("event gap: " + conf.getInt(SQConfig.strEventGap, 1));
		System.err.println("sequence gap: " + conf.getInt(SQConfig.strSeqGap, 10));
		long begin = System.currentTimeMillis();
		job.waitForCompletion(true);
		long end = System.currentTimeMillis();
		long second = (end - begin) / 1000;
		System.err.println(job.getJobName() + " takes " + second + " seconds");
		org.apache.hadoop.mapreduce.Counters cn=job.getCounters();
		Counter c1 = cn.findCounter(Counters.numOutliers);
		Counter c2 = cn.findCounter(Counters.sumLength);
		System.out.println(c1.getValue() + "\t" + c2.getValue()*1.0/c1.getValue());
	}

	public static void main(String[] args) throws Exception {
		DetectLocalOutliers generateLocalFS = new DetectLocalOutliers();
		generateLocalFS.run(args);
	}
}
