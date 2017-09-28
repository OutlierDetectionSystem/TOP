package top.distributed.lts;

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
		private HashMap<String, String> metaDataMapping;

		public void setup(Context context) {
			/** get configuration from file */
			Configuration conf = context.getConfiguration();
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
								this.metaDataMapping.put(splitsStr[0], "(" + splitsStr[1] + ")" + splitsStr[2]);
							}
						}
					} // end for (int i = 0; i < stats.length; ++i)
				} // end for (URI path : cacheFiles)

			} catch (IOException ioe) {
				System.err.println("Caught exception while getting cached files");
			}
		}

		public void reduce(IntWritable key, Iterable<Text> values, Context context) throws IOException {
			HashMap<String, HashSet<Integer>> sequenceFrequency = new HashMap<String, HashSet<Integer>>();
			// HashMap<Integer, ArrayList<String>> allViolations = new
			// HashMap<Integer, ArrayList<String>>();
			HashMap<Integer, String> deviceInfo = new HashMap<Integer, String>();
			for (Text oneValue : values) {
				String line = oneValue.toString();
				String[] splitValues = line.split(SQConfig.sepStrForKeyValue);
				int deviceId = Integer.parseInt(splitValues[0]);
				String deviceStr = splitValues[1];
				// ArrayList<String> violationsForOne = new ArrayList<String>();
				for (int i = 2; i < splitValues.length; i++) {
					// violationsForOne.add(splitValues[i]);
					if (sequenceFrequency.containsKey(splitValues[i]))
						sequenceFrequency.get(splitValues[i]).add(deviceId);
					else {
						HashSet<Integer> newSeq = new HashSet<Integer>();
						newSeq.add(deviceId);
						sequenceFrequency.put(splitValues[i], newSeq);
					}
				}
				deviceInfo.put(deviceId, deviceStr);
			} // end collection data

			for (Entry<String, HashSet<Integer>> tempEntry : sequenceFrequency.entrySet()) {
				if (tempEntry.getValue().size() < 2 && tempEntry.getKey().split("\\|")[0].split(",").length > 1) {
					context.getCounter(Counters.numViolations).increment(1);
					// output filtered results
					try {
						// context.write(NullWritable.get(), new
						// Text(tempEntry.getKey()+","+
						// deviceInfo.get(tempEntry.getKey())));
						String devicesInfo = "";
						for(Integer device: tempEntry.getValue()){
							devicesInfo += deviceInfo.get(device) + ",";
						}
						if(devicesInfo.length() > 0)
							devicesInfo = devicesInfo.substring(0, devicesInfo.length()-1);
						context.write(NullWritable.get(), new Text("Devices with this violation: " + devicesInfo));
						String[] subS = tempEntry.getKey().split(SQConfig.sepSplitForIDDist);
						context.write(NullWritable.get(), new Text("Sequence: " + subS[0]));
						context.write(NullWritable.get(), new Text("Violates BCS Sequence: " + subS[1] + "\n"));
						if (metaDataMapping != null) {
							// parse violation sequence
							context.write(NullWritable.get(), new Text("Sequence: "));
							String originalMeta = "";
							for (String tempStr : subS[0].split(SQConfig.sepStrForRecord)) {
								originalMeta += metaDataMapping.get(tempStr.trim()) + SQConfig.sepStrForKeyValue;
							}
							context.write(NullWritable.get(), new Text(originalMeta + "\n"));
							context.write(NullWritable.get(), new Text("Violates BCS Sequence: "));
							String violationMeta = "";
							for (String tempStr : subS[1].split(SQConfig.sepStrForRecord)) {
								violationMeta += metaDataMapping.get(tempStr.trim()) + SQConfig.sepStrForKeyValue;
							}
							context.write(NullWritable.get(), new Text(violationMeta + "\n"));
						}
						
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
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
		String outputPath = conf.get(SQConfig.strFinalFilteredViolationOutput) + "-"
				+ conf.getInt(SQConfig.strLocalSupport, 10) + "-" + conf.getInt(SQConfig.strGlobalSupport, 10) + "-"
				+ conf.getInt(SQConfig.strEventGap, 1) + "-" + conf.getInt(SQConfig.strSeqGap, 1) + "-" + 
				conf.getLong(SQConfig.strEventTimeInterval, 5000) + "-" + conf.getLong(SQConfig.strSeqTimeInterval, 60000);
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
		org.apache.hadoop.mapreduce.Counters cn = job.getCounters();
		Counter c1 = cn.findCounter(Counters.numViolations);
		System.out.println(c1.getValue());
	}

	public static void main(String[] args) throws Exception {
		FilterViolationResults generateLocalFS = new FilterViolationResults();
		generateLocalFS.run(args);
	}
}
