package top.utils;

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

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

public class GenerateGlobalFrequentElementsTS {

	public static class DDMapper extends Mapper<LongWritable, Text, IntWritable, Text> {
		public void map(LongWritable key, Text value, Context context) throws IOException, InterruptedException {
			String[] splitValue = value.toString().split(SQConfig.sepStrForKeyValue);
			Text dat = new Text(splitValue[2]);
			context.write(new IntWritable(0), dat);
		}
	}

	/**
	 * @author yizhouyan
	 *
	 */
	public static class DDReducer extends Reducer<IntWritable, Text, NullWritable, Text> {

		static enum Counters {
			numberElements;
		}

		private static int minSupportLocal = 10;
		private static int minSupportGlobal = 10;

		public void setup(Context context) {
			/** get configuration from file */
			Configuration conf = context.getConfiguration();
			this.minSupportLocal = conf.getInt(SQConfig.strLocalSupport, 10);
			this.minSupportGlobal = conf.getInt(SQConfig.strGlobalSupport, 10);
		}

		public void reduce(IntWritable key, Iterable<Text> values, Context context) throws IOException {
			// collect data
			HashMap<String, Integer> globalFrequency = new HashMap<String, Integer>();

			for (Text oneValue : values) {
				String inputString = oneValue.toString();
				HashMap<String, Integer> itemMap = new HashMap<>();
				String[] subItems = inputString.split(",");
				for (String tempTS : subItems) {
					String temp = tempTS.split(SQConfig.sepSplitForIDDist)[0];
					if (!itemMap.containsKey(temp))
						itemMap.put(temp, 1);
					else
						itemMap.put(temp, itemMap.get(temp) + 1);
				}

				for (Map.Entry entry : itemMap.entrySet()) {
					String keyMap = (String) entry.getKey();
					int count = (int) entry.getValue();

					if (count >= minSupportLocal) {
						if (!globalFrequency.containsKey(keyMap))
							globalFrequency.put(keyMap, 1);
						else
							globalFrequency.put(keyMap, globalFrequency.get(keyMap) + 1);
					}
				}
			}
			HashSet<String> frequentItems = new HashSet<String>();
			for (Map.Entry<String, Integer> entry : globalFrequency.entrySet()) {
				if (entry.getValue() >= minSupportGlobal) {
					frequentItems.add(entry.getKey());
				}
			}
			try {
				for (String str : frequentItems)
					context.write(NullWritable.get(), new Text(str));
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} // end reduce function

	} // end reduce class

	public void run(String[] args) throws Exception {
		Configuration conf = new Configuration();
		conf.addResource(new Path("/usr/local/Cellar/hadoop/etc/hadoop/core-site.xml"));
		conf.addResource(new Path("/usr/local/Cellar/hadoop/etc/hadoop/hdfs-site.xml"));
		new GenericOptionsParser(conf, args).getRemainingArgs();
		/** set job parameter */
		Job job = Job.getInstance(conf, "Generate lts frequent elements");

		job.setJarByClass(GenerateGlobalFrequentElementsTS.class);
		job.setMapperClass(DDMapper.class);
		job.setReducerClass(DDReducer.class);
		job.setMapOutputKeyClass(IntWritable.class);
		job.setMapOutputValueClass(Text.class);
		job.setOutputKeyClass(NullWritable.class);
		job.setOutputValueClass(Text.class);
		job.setNumReduceTasks(1);

		String strFSName = conf.get("fs.default.name");
		FileInputFormat.addInputPath(job, new Path(conf.get(SQConfig.strInputDataset)));
		FileSystem fs = FileSystem.get(conf);
		String outputPath = conf.get(SQConfig.strGlobalFreqElements);
		fs.delete(new Path(outputPath), true);
		FileOutputFormat.setOutputPath(job, new Path(outputPath));

		/** print job parameter */
		System.err.println("local support: " + conf.getInt(SQConfig.strLocalSupport, 10));
		System.err.println("lts support: " + conf.getInt(SQConfig.strGlobalSupport, 10));
		long begin = System.currentTimeMillis();
		job.waitForCompletion(true);
		long end = System.currentTimeMillis();
		long second = (end - begin) / 1000;
		System.err.println(job.getJobName() + " takes " + second + " seconds");
	}

	public static void main(String[] args) throws Exception {
		GenerateGlobalFrequentElementsTS generateLocalFS = new GenerateGlobalFrequentElementsTS();
		generateLocalFS.run(args);
	}
}