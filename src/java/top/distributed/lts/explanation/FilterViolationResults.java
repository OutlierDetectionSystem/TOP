package top.distributed.lts.explanation;

import top.utils.SQConfig;
import org.apache.hadoop.conf.Configuration;
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

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map.Entry;

public class FilterViolationResults {
	public static enum Counters {
		numViolations;
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
		private MultipleOutputs mos;

		public void setup(Context context) {
			/** get configuration from file */
			Configuration conf = context.getConfiguration();
			mos = new MultipleOutputs(context);
		}

		public void reduce(IntWritable key, Iterable<Text> values, Context context) throws IOException {
			HashMap<String, HashMap<Integer, String>> sequenceFrequency = new HashMap<String, HashMap<Integer, String>>();
			HashMap<Integer, String> deviceInfo = new HashMap<Integer, String>();
			HashSet<Integer> devicesWithViolations = new HashSet<Integer>();
			for (Text oneValue : values) {
				String line = oneValue.toString();
				String[] splitValues = line.split(SQConfig.sepStrForKeyValue);
				int deviceId = Integer.parseInt(splitValues[0]);
				String deviceStr = splitValues[1];
				// ArrayList<String> violationsForOne = new ArrayList<String>();
				for (int i = 2; i < splitValues.length; i++) {
					// violationsForOne.add(splitValues[i]);
					String[] splitViolationsThisDevice = splitValues[i].split(SQConfig.sepSplitForIDDist);
					String violationSeqWithOccurrences = splitViolationsThisDevice[0];
					int beginOccurrenceIndex = violationSeqWithOccurrences.indexOf("[");
					String violationSeq = violationSeqWithOccurrences.substring(0, beginOccurrenceIndex);
					String violationOccurrences = violationSeqWithOccurrences.substring(beginOccurrenceIndex,
							violationSeqWithOccurrences.length());

					for (int j = 1; j < splitViolationsThisDevice.length; j++) {
						String finalViolationSeqLookUp = violationSeq + "|" + splitViolationsThisDevice[j];
						if (sequenceFrequency.containsKey(finalViolationSeqLookUp))
							sequenceFrequency.get(finalViolationSeqLookUp).put(deviceId, violationOccurrences);
						else {
							HashMap<Integer, String> newSeq = new HashMap<Integer, String>();
							newSeq.put(deviceId, violationOccurrences);
							sequenceFrequency.put(finalViolationSeqLookUp, newSeq);
						}
					} // end traversing
				}
				deviceInfo.put(deviceId, deviceStr);
			} // end collection data

			for (Entry<String, HashMap<Integer, String>> tempEntry : sequenceFrequency.entrySet()) {
				if (tempEntry.getValue().size() < 2 && tempEntry.getKey().split("\\|")[0].split(",").length > 1) {
					// output filtered results
					try {
						for (Entry<Integer, String> device : tempEntry.getValue().entrySet()) {
							String devicesInfo = device.getKey() + "\t" + deviceInfo.get(device.getKey()) + "\t"
									+ tempEntry.getKey() + "\t" + device.getValue();
							context.write(NullWritable.get(), new Text(devicesInfo));
							context.getCounter(Counters.numViolations).increment(1);
							devicesWithViolations.add(device.getKey());
						}
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}
			Configuration conf = context.getConfiguration();
			String outputDevicePath = conf.get(SQConfig.strDevicesWithViolations) + "-"
					 + conf.getInt(SQConfig.strLocalSupport, 10) + "-"+ conf.getInt(SQConfig.strGlobalSupport, 10) + "-"
					+ conf.getInt(SQConfig.strEventGap, 1) + "-" + conf.getInt(SQConfig.strSeqGap, 1);
			// output devices with violations
			for (Integer deviceId : devicesWithViolations) {
				try {
					mos.write(NullWritable.get(), new Text(deviceId + ""),
							outputDevicePath + "/Devices-" + context.getTaskAttemptID());
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
		Job job = Job.getInstance(conf, "Detect Local Outliers");

		job.setJarByClass(FilterViolationResults.class);
		job.setMapperClass(DDMapper.class);
		job.setReducerClass(DDReducer.class);
		job.setMapOutputKeyClass(IntWritable.class);
		job.setMapOutputValueClass(Text.class);
		job.setOutputKeyClass(NullWritable.class);
		job.setOutputValueClass(Text.class);
		job.setNumReduceTasks(1);

		String strFSName = conf.get("fs.default.name");
		String inputPath = conf.get(SQConfig.strFinalViolationOutput) + "-" + conf.getInt(SQConfig.strLocalSupport, 10)
				+ "-" + conf.getInt(SQConfig.strGlobalSupport, 10) + "-" + conf.getInt(SQConfig.strEventGap, 1) + "-"
				+ conf.getInt(SQConfig.strSeqGap, 1);
		FileInputFormat.addInputPath(job, new Path(inputPath));
		FileSystem fs = FileSystem.get(conf);
		MultipleOutputs.addNamedOutput(job, "Devices", TextOutputFormat.class, NullWritable.class, Text.class);
		String outputDevicePath = conf.get(SQConfig.strDevicesWithViolations) + "-"
				 + conf.getInt(SQConfig.strLocalSupport, 10) + "-"+ conf.getInt(SQConfig.strGlobalSupport, 10) + "-"
				+ conf.getInt(SQConfig.strEventGap, 1) + "-" + conf.getInt(SQConfig.strSeqGap, 1);
		fs.delete(new Path(outputDevicePath), true);
		
		String outputPath = conf.get(SQConfig.strFinalFilteredViolationOutput) + "-"
				+ conf.getInt(SQConfig.strLocalSupport, 10) + "-" + conf.getInt(SQConfig.strGlobalSupport, 10) + "-"
				+ conf.getInt(SQConfig.strEventGap, 1) + "-" + conf.getInt(SQConfig.strSeqGap, 1);
//		+ "-"
//				+ conf.getLong(SQConfig.strEventTimeInterval, 5000) + "-"
//				+ conf.getLong(SQConfig.strSeqTimeInterval, 60000);
		fs.delete(new Path(outputPath), true);
		FileOutputFormat.setOutputPath(job, new Path(outputPath));
		
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
		org.apache.hadoop.mapreduce.Counters cn = job.getCounters();
		Counter c1 = cn.findCounter(Counters.numViolations);
		System.out.println(c1.getValue());
	}

	public static void main(String[] args) throws Exception {
		FilterViolationResults generateLocalFS = new FilterViolationResults();
		generateLocalFS.run(args);
	}
}
