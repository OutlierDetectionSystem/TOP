package top.distributed.cleanrealdata;

import java.io.IOException;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.util.GenericOptionsParser;

import top.utils.SQConfig;

public class PostProcessingDataset {
	public static class DDMapper extends Mapper<LongWritable, Text, NullWritable, Text> {
		public static final int maxSequenceLength = 200000;
		
		public void map(LongWritable key, Text value, Context context) throws IOException, InterruptedException {
			String[] splitValue = value.toString().split(SQConfig.sepStrForKeyValue);
			String [] sequenceSplits = splitValue[2].split(SQConfig.sepStrForRecord);
			if( sequenceSplits.length <= maxSequenceLength){
				context.write(NullWritable.get(), new Text(splitValue[0] + SQConfig.sepStrForKeyValue + splitValue[2]));
			}else{
				String newSequence = "";
				for(int i = 0; i< maxSequenceLength; i++){
					newSequence += sequenceSplits[i] + ",";
				}
				newSequence= newSequence.substring(0,newSequence.length()-1);
				context.write(NullWritable.get(), new Text(splitValue[0] + SQConfig.sepStrForKeyValue + newSequence));
			}
		}
	}
		
	public void run(String[] args) throws Exception {
		Configuration conf = new Configuration();
		conf.addResource(new Path("/usr/local/Cellar/hadoop/etc/hadoop/core-site.xml"));
		conf.addResource(new Path("/usr/local/Cellar/hadoop/etc/hadoop/hdfs-site.xml"));
		new GenericOptionsParser(conf, args).getRemainingArgs();
		/** set job parameter */
		Job job = Job.getInstance(conf, "Generate Local Frequent Sequences");

		job.setJarByClass(PostProcessingDataset.class);
		job.setMapperClass(DDMapper.class);
		
		job.setMapOutputKeyClass(NullWritable.class);
		job.setMapOutputValueClass(Text.class);
		job.setNumReduceTasks(0);

		FileInputFormat.addInputPath(job, new Path(conf.get(SQConfig.strInputDatasetLarge)));
		FileSystem fs = FileSystem.get(conf);
		String outputPath = conf.get(SQConfig.strInputDataset);
		fs.delete(new Path(outputPath), true);
		FileOutputFormat.setOutputPath(job, new Path(outputPath));

		/** print job parameter */
		System.err.println("input path: " + conf.get(SQConfig.strInputDatasetLarge));
		System.err.println("output path: " + conf.get(SQConfig.strInputDataset));

		long begin = System.currentTimeMillis();
		job.waitForCompletion(true);
		long end = System.currentTimeMillis();
		long second = (end - begin) / 1000;
		System.err.println(job.getJobName() + " takes " + second + " seconds");
	}

	public static void main(String[] args) throws Exception {
		PostProcessingDataset cleanData = new PostProcessingDataset();
		cleanData.run(args);
	}
}
