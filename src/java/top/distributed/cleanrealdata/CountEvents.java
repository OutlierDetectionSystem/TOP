package top.distributed.cleanrealdata;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map.Entry;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.util.GenericOptionsParser;
import top.utils.SQConfig;

public class CountEvents {
	public static class DDMapper extends Mapper<LongWritable, Text, Text, IntWritable> {
		HashMap<String, Integer> countEventsInTS = new HashMap<String, Integer>();

		public void map(LongWritable key, Text value, Context context) throws IOException, InterruptedException {
			String[] splitValue = value.toString().split(SQConfig.sepStrForKeyValue);
			String[] elements = splitValue[2].split(SQConfig.sepStrForRecord);
			HashSet<String> uniqueElements = new HashSet<String>();
			uniqueElements.addAll(Arrays.asList(elements));
			for (String str : uniqueElements) {
				if (countEventsInTS.containsKey(str))
					countEventsInTS.put(str, countEventsInTS.get(str) + 1);
				else
					countEventsInTS.put(str, 1);
			}

		}

		protected void cleanup(Context context) throws IOException, InterruptedException {
			for (Entry<String, Integer> entry : countEventsInTS.entrySet()) {
				context.write(new Text(entry.getKey()), new IntWritable(entry.getValue()));
			}
		}

	}

	/**
	 * @author yizhouyan
	 *
	 */
	public static class DDReducer extends Reducer<Text, IntWritable, Text, IntWritable> {
		public void reduce(Text key, Iterable<IntWritable> values, Context context) throws IOException, InterruptedException {
			int totalNumber = 0;
			for (IntWritable oneValue : values) {
				totalNumber+= oneValue.get();
			}
			context.write(key, new IntWritable(totalNumber));
		}

	} // end reduce class

	public void run(String[] args) throws Exception {
		Configuration conf = new Configuration();
		conf.addResource(new Path("/usr/local/Cellar/hadoop/etc/hadoop/core-site.xml"));
		conf.addResource(new Path("/usr/local/Cellar/hadoop/etc/hadoop/hdfs-site.xml"));
		new GenericOptionsParser(conf, args).getRemainingArgs();
		/** set job parameter */
		Job job = Job.getInstance(conf, "Generate Local Frequent Sequences");

		job.setJarByClass(CountEvents.class);
		job.setMapperClass(DDMapper.class);
		job.setReducerClass(DDReducer.class);
		job.setMapOutputKeyClass(Text.class);
		job.setMapOutputValueClass(IntWritable.class);
		job.setOutputKeyClass(Text.class);
		job.setOutputValueClass(IntWritable.class);
		job.setNumReduceTasks(1);

		FileInputFormat.addInputPath(job, new Path(conf.get(SQConfig.strInputDataset)));
		FileSystem fs = FileSystem.get(conf);
		String outputPath = "/fs/input/real-10000-statistics";
		fs.delete(new Path(outputPath), true);
		FileOutputFormat.setOutputPath(job, new Path(outputPath));

		/** print job parameter */
		System.err.println("input path: " + conf.get(SQConfig.strInputDataset));
		System.err.println("output path: " + outputPath);

		long begin = System.currentTimeMillis();
		job.waitForCompletion(true);
		long end = System.currentTimeMillis();
		long second = (end - begin) / 1000;
		System.err.println(job.getJobName() + " takes " + second + " seconds");
	}

	public static void main(String[] args) throws Exception {
		CountEvents cleanData = new CountEvents();
		cleanData.run(args);
	}
}
