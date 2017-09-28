package top.distributed.statistics;

import org.apache.hadoop.conf.Configuration;
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
import top.utils.ConfigBasicStatistics;

import java.io.IOException;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

public class CountSyncPerDay {

	public static enum Counters {
		moreSyncCount;
	}

	public static class DDMapper extends Mapper<LongWritable, Text, IntWritable, Text> {

		/** number of divisions where data is divided into (set by user) */
		private int numPartitions = 50;

		protected void setup(Context context) throws IOException, InterruptedException {
			Configuration conf = context.getConfiguration();
			numPartitions = conf.getInt(ConfigBasicStatistics.strNumPartitions, 50);
		}

		public void map(LongWritable key, Text value, Context context) throws IOException, InterruptedException {
			String[] splitValue = value.toString().split(ConfigBasicStatistics.sepStrForKeyValue);
			int id = Integer.parseInt(splitValue[0]);
			IntWritable key_id = new IntWritable(id % numPartitions);
			context.write(key_id, value);
		}
	}

	/**
	 * @author yizhouyan
	 *
	 */
	public static class DDReducer extends Reducer<IntWritable, Text, IntWritable, Text> {
		private int support = 17;
		private long timeInterval = 86400000; // one day (24 hours)
		private String event = "S";
		private HashMap<String, Integer> maxSyncPerDay = new HashMap<>();

		protected void setup(Mapper.Context context) throws IOException, InterruptedException {
			Configuration conf = context.getConfiguration();
			support = conf.getInt(ConfigBasicStatistics.strSupport, 20);
			event = conf.get(ConfigBasicStatistics.strEventType,"S");
			timeInterval = conf.getLong(ConfigBasicStatistics.strTimeInterval, 86400000);
		}

		public int countMaxEventOccurrence(String inputString){
			String[] splitInputs = inputString.split(ConfigBasicStatistics.sepStrForRecord);
			String[] splitInputValues = new String[splitInputs.length];
			long[] splitInputTimeStamps = new long[splitInputs.length];
			// phrase data
			for (int i = 0; i < splitInputs.length; i++) {
				String[] subs = splitInputs[i].split(ConfigBasicStatistics.sepSplitForIDDist);
				splitInputValues[i] = subs[0];
				splitInputTimeStamps[i] = Long.parseLong(subs[1]);
			}
			int maxCountEvent = 0;
			for (int i = 0; i< splitInputs.length; i++){
				if(splitInputValues[i].equals(event)){
					// extend 24 hours and count events
					int countEventNum = 0;
					for(int j = i+1; j< splitInputs.length; j++){
						if(splitInputTimeStamps[j]-splitInputTimeStamps[i] > timeInterval)
							break;
						if(splitInputValues[j].equals(event))
							countEventNum++;
					}
					maxCountEvent = Math.max(maxCountEvent, countEventNum);
				}
			}
			return maxCountEvent;
		}
		public void reduce(IntWritable key, Iterable<Text> values, Context context) throws IOException {
			// collect data
			for (Text oneValue : values) {
				String line = oneValue.toString();
				String[] splitValues = line.split(ConfigBasicStatistics.sepStrForKeyValue);
				String deviceId = splitValues[1];
				String inputString = splitValues[2];
				int maxCountEvent = countMaxEventOccurrence(inputString);
				if(maxCountEvent > support){
					maxSyncPerDay.put(deviceId, maxCountEvent);
					context.getCounter(Counters.moreSyncCount).increment(1);
				}
			} // end collection data
		} // end reduce function

		public void cleanup(Reducer.Context context) throws IOException, InterruptedException {
			for(Map.Entry<String, Integer> entry: maxSyncPerDay.entrySet())
				context.write(new Text(entry.getKey()), new IntWritable(entry.getValue()));
		}
	} // end reduce class

	public void run(String[] args) throws Exception {
		Configuration conf = new Configuration();
		conf.addResource(new Path("/usr/local/Cellar/hadoop/etc/hadoop/core-site.xml"));
		conf.addResource(new Path("/usr/local/Cellar/hadoop/etc/hadoop/hdfs-site.xml"));
		new GenericOptionsParser(conf, args).getRemainingArgs();
		/** set job parameter */
		Job job = Job.getInstance(conf, "Generate Local Frequent Sequences--- With Violation and time stamp");

		job.setJarByClass(CountSyncPerDay.class);
		job.setMapperClass(DDMapper.class);
		job.setReducerClass(DDReducer.class);
		job.setMapOutputKeyClass(IntWritable.class);
		job.setMapOutputValueClass(Text.class);
		job.setOutputKeyClass(IntWritable.class);
		job.setOutputValueClass(Text.class);
		job.setNumReduceTasks(Integer.parseInt(conf.get(ConfigBasicStatistics.strNumReducers)));

		String strFSName = conf.get("fs.default.name");
		FileInputFormat.addInputPath(job, new Path(conf.get(ConfigBasicStatistics.strInputDataset)));
		FileSystem fs = FileSystem.get(conf);
		// FileOutputFormat.setOutputPath(job, new Path(outputPath));

		String outputPath = conf.get(ConfigBasicStatistics.strCountSyncPerDayOutput);
		fs.delete(new Path(outputPath), true);
		FileOutputFormat.setOutputPath(job, new Path(outputPath));

		/** print job parameter */
		System.err.println("support: " + conf.getInt(ConfigBasicStatistics.strSupport, 20));
		System.err.println("time interval: " + conf.getLong(ConfigBasicStatistics.strTimeInterval, 86400000));
		job.waitForCompletion(true);
	}

	public static void main(String[] args) throws Exception {
		CountSyncPerDay generateLocalFS = new CountSyncPerDay();
		generateLocalFS.run(args);
	}
}
