package top.distributed.cleanrealdata;

import java.io.IOException;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.util.GenericOptionsParser;

import top.utils.SQConfig;

public class RemoveRepeat {
	enum Counters{
		totalRepeats;
	}
	public static class DDMapper extends Mapper<LongWritable, Text, IntWritable, Text> {
		public void map(LongWritable key, Text value, Context context) throws IOException, InterruptedException {
			String[] splitValue = value.toString().split(SQConfig.sepStrForKeyValue);
			context.write(new IntWritable(Integer.parseInt(splitValue[0])), value);
		}
	}

	/**
	 * @author yizhouyan
	 *
	 */
	public static class DDReducer extends Reducer<IntWritable, Text, NullWritable, Text> {
		public void reduce(IntWritable key, Iterable<Text> values, Context context) throws IOException, InterruptedException {
			for (Text oneValue : values) {
//				System.out.println(oneValue.toString().length());
				String previousStr = "";
				String[] idValues = oneValue.toString().split("\t");
				String seqNum = idValues[0];
				String deviceId = idValues[1];
				String[] subStr = idValues[2].split(",");
				String outputValues = "";
				int countRepeat = 0;
				for (String sub : subStr) {
					if (sub.equals(previousStr)) {
						countRepeat++;
					} else {
						outputValues += sub + ",";
					}
					previousStr = sub;
				}
				if (outputValues.length() > 0)
					outputValues = outputValues.substring(0, outputValues.length() - 1);
				if (countRepeat > 0) {
					System.out.println(seqNum + "\t" + deviceId + "\t" + subStr.length + "\t" + countRepeat + "\t"
							+ countRepeat * 1.0 / subStr.length);
					context.getCounter(Counters.totalRepeats).increment(1);
				}
				context.write(NullWritable.get(),
					new Text(seqNum + "\t" + deviceId + "\t" + outputValues));
			}
		} // end reduce function

	} // end reduce class

	public void run(String[] args) throws Exception {
		Configuration conf = new Configuration();
		conf.addResource(new Path("/usr/local/Cellar/hadoop/etc/hadoop/core-site.xml"));
		conf.addResource(new Path("/usr/local/Cellar/hadoop/etc/hadoop/hdfs-site.xml"));
		new GenericOptionsParser(conf, args).getRemainingArgs();
		/** set job parameter */
		Job job = Job.getInstance(conf, "Remove repeat");

		job.setJarByClass(RemoveRepeat.class);
		job.setMapperClass(DDMapper.class);
		job.setReducerClass(DDReducer.class);
		job.setMapOutputKeyClass(IntWritable.class);
		job.setMapOutputValueClass(Text.class);
		job.setOutputKeyClass(NullWritable.class);
		job.setOutputValueClass(Text.class);
		job.setNumReduceTasks(Integer.parseInt(conf.get(SQConfig.strNumReducers), 10));

		FileInputFormat.addInputPath(job, new Path(conf.get(SQConfig.strInputDataset)));
		FileSystem fs = FileSystem.get(conf);
		String outputPath = conf.get(SQConfig.strInputDatasetLarge);
		fs.delete(new Path(outputPath), true);
		FileOutputFormat.setOutputPath(job, new Path(outputPath));

		/** print job parameter */
		System.err.println("input path: " + conf.get(SQConfig.strInputDataset));
		System.err.println("output path: " + conf.get(SQConfig.strInputDatasetLarge));

		long begin = System.currentTimeMillis();
		job.waitForCompletion(true);
		long end = System.currentTimeMillis();
		long second = (end - begin) / 1000;
		System.err.println(job.getJobName() + " takes " + second + " seconds");
	}

	public static void main(String[] args) throws Exception {
		RemoveRepeat cleanData = new RemoveRepeat();
		cleanData.run(args);
	}
}
