package top.distributed.cleanrealdata;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
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

public class CleanDataset {
	public static class DDMapper extends Mapper<LongWritable, Text, Text, Text> {

		public void map(LongWritable key, Text value, Context context) throws IOException, InterruptedException {
			String[] splitValue = value.toString().split(SQConfig.sepStrForRecord);
			context.write(new Text(splitValue[0]), value);
		}
	}

	/**
	 * @author yizhouyan
	 *
	 */
	public static class DDReducer extends Reducer<Text, Text, NullWritable, Text> {
		public SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd' 'HH:mm:ss.SSS");
		public SimpleDateFormat formatter2 = new SimpleDateFormat("yyyy-MM-dd' 'HH:mm:ss");

		public static final int maxSequenceLength = 200000;

		public void sortByDate(ArrayList<SingleSequence> sequences) {
			Collections.sort(sequences, new Comparator<SingleSequence>() {
				public int compare(SingleSequence str1, SingleSequence str2) {
					Date date1 = str1.getDate();
					Date date2 = str2.getDate();

					if (date1.before(date2))
						return -1;
					else if (date1.after(date2))
						return 1;
					else
						return 0;
				}
			});
		}

		public void reduce(Text key, Iterable<Text> values, Context context) throws IOException, InterruptedException {
			// collect data

			ArrayList<SingleSequence> sequencesInOneDevice = new ArrayList<SingleSequence>();
			for (Text oneValue : values) {
				String[] subs = oneValue.toString().split(",");
				if (subs.length < 3) {
					System.out.println(oneValue.toString());
					return;
				}
				Date date;
				try {
					// date = formatter.parse(subs[1]);
					if (subs[1].contains(".")) {
						date = formatter.parse(subs[1]);
					} else {
						date = formatter2.parse(subs[1]);
					}
					SingleSequence newSeq = new SingleSequence(date, Integer.parseInt(subs[2]));
					sequencesInOneDevice.add(newSeq);
				} catch (ParseException e) {
					System.out.println("Error formatting...." + oneValue.toString());
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			} // end collection data
				// sort and output
			this.sortByDate(sequencesInOneDevice);
			String outputStr = key.toString() + SQConfig.sepStrForKeyValue;
			String outputSeq = "";
			int countElements = 0;
			for (SingleSequence s : sequencesInOneDevice) {
				outputSeq += s.getSequence() + SQConfig.sepStrForRecord;
				countElements += 1;
				if (countElements >= this.maxSequenceLength)
					break;
			}
			if (outputSeq.length() > 0) {
				outputSeq = outputSeq.substring(0, outputSeq.length() - 1);
			}
			context.write(NullWritable.get(),
					new Text(outputStr + countElements + SQConfig.sepStrForKeyValue + outputSeq));

		} // end reduce function

	} // end reduce class

	public void run(String[] args) throws Exception {
		Configuration conf = new Configuration();
		conf.addResource(new Path("/usr/local/Cellar/hadoop/etc/hadoop/core-site.xml"));
		conf.addResource(new Path("/usr/local/Cellar/hadoop/etc/hadoop/hdfs-site.xml"));
		new GenericOptionsParser(conf, args).getRemainingArgs();
		/** set job parameter */
		Job job = Job.getInstance(conf, "Generate Local Frequent Sequences");

		job.setJarByClass(CleanDataset.class);
		job.setMapperClass(DDMapper.class);
		job.setReducerClass(DDReducer.class);
		job.setMapOutputKeyClass(Text.class);
		job.setMapOutputValueClass(Text.class);
		job.setOutputKeyClass(NullWritable.class);
		job.setOutputValueClass(Text.class);
		job.setNumReduceTasks(Integer.parseInt(conf.get(SQConfig.strNumReducers)));

		FileInputFormat.addInputPath(job, new Path(conf.get(SQConfig.strOriginalInput)));
		FileSystem fs = FileSystem.get(conf);
		String outputPath = conf.get(SQConfig.strInputDatasetLarge);
		fs.delete(new Path(outputPath), true);
		FileOutputFormat.setOutputPath(job, new Path(outputPath));

		/** print job parameter */
		System.err.println("input path: " + conf.get(SQConfig.strOriginalInput));
		System.err.println("output path: " + conf.get(SQConfig.strInputDatasetLarge));

		long begin = System.currentTimeMillis();
		job.waitForCompletion(true);
		long end = System.currentTimeMillis();
		long second = (end - begin) / 1000;
		System.err.println(job.getJobName() + " takes " + second + " seconds");
	}

	public static void main(String[] args) throws Exception {
		CleanDataset cleanData = new CleanDataset();
		cleanData.run(args);
	}
}
