package top.distributed.maximum;

/**
 * Created by yizhouyan on 2/2/18.
 */

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FileStatus;
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
import top.core.lts.local.withviolation.basic.ViolationSequence;
import top.core.maximum.mining.SingleSequenceMiningTS;
import top.core.maximum.mining.violations.DetectInfrequentViolations;
import top.inputs.InputSequence;
import top.inputs.InputSequenceWithTS;
import top.parameterspace.LocalParameterSpace;
import top.parameterspace.LocalParameterSpaceWithTS;
import top.utils.SQConfig;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class GenerateViolationsWithTS {

    public static enum Counters {
        fsCompTime, fslengthSum, fsNum, startLen, numCountSL, candidateCount, runOutOfMemory;
    }

    public static class DDMapper extends Mapper<LongWritable, Text, IntWritable, Text> {

        /**
         * number of divisions where data is divided into (set by user)
         */
        private int numPartitions = 50;

        protected void setup(Context context) throws IOException, InterruptedException {
            Configuration conf = context.getConfiguration();
            numPartitions = conf.getInt(SQConfig.strNumPartitions, 50);
        }

        public void map(LongWritable key, Text value, Context context) throws IOException, InterruptedException {
            String[] splitValue = value.toString().split(SQConfig.sepStrForKeyValue);
            int id = Integer.parseInt(splitValue[0]);
            IntWritable key_id = new IntWritable(id % numPartitions);
            context.write(key_id, value);
        }
    }

    /**
     * @author yizhouyan
     */
    public static class DDReducer extends Reducer<IntWritable, Text, IntWritable, Text> {
        private static int minSupportLocal = 10;
        private static int itemGap = 1;
        private static int seqGap = 10;
        private static long itemTimeInterval = 10000;
        private static long seqTimeInterval = 60000;
        private static int violationSupportLocal = 5;
        private HashSet<String> globalFreqPatterns;
        private MultipleOutputs mos;

        public void setup(Context context) {
            /** get configuration from file */
            Configuration conf = context.getConfiguration();
            mos = new MultipleOutputs(context);
            this.minSupportLocal = conf.getInt(SQConfig.strLocalSupport, 10);
            this.itemGap = conf.getInt(SQConfig.strEventGap, 1);
            this.seqGap = conf.getInt(SQConfig.strSeqGap, 10);
            this.itemTimeInterval = conf.getLong(SQConfig.strEventTimeInterval, 1000);
            this.seqTimeInterval = conf.getLong(SQConfig.strSeqTimeInterval, 60000);
            this.violationSupportLocal = conf.getInt(SQConfig.strViolationLocalSupport, 5);
            this.globalFreqPatterns = new HashSet<String>();

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
                            System.out.println("Reading global frequent patterns from " + stats[i].getPath().toString());
                            FSDataInputStream currentStream;
                            BufferedReader currentReader;
                            currentStream = fs.open(stats[i].getPath());
                            currentReader = new BufferedReader(new InputStreamReader(currentStream));
                            String line;
                            while ((line = currentReader.readLine()) != null) {
                                /** parse line */
                                this.globalFreqPatterns.add(line);
                            }
                        }
                    } // end for (int i = 0; i < stats.length; ++i)
                } // end for (URI path : cacheFiles)
                System.out.println("Number of Global Frequent Patterns: " + this.globalFreqPatterns.size());
            } catch (IOException ioe) {
                System.err.println("Caught exception while getting cached files");
            }
        }

        public void reduce(IntWritable key, Iterable<Text> values, Context context) throws IOException {
            // collect data
            for (Text oneValue : values) {
                try {
                    String line = oneValue.toString();
                    String[] splitValues = line.split(SQConfig.sepStrForKeyValue);
                    int seqId = Integer.parseInt(splitValues[0]);
                    String deviceId = splitValues[1];
                    String inputString = splitValues[2];

                    LocalParameterSpaceWithTS localParameterSpace = new LocalParameterSpaceWithTS(this.minSupportLocal, itemGap, seqGap,
                            this.itemTimeInterval, this.seqTimeInterval);
                    InputSequenceWithTS inputSequence = new InputSequenceWithTS(inputString);
                    DetectInfrequentViolations localViolations = new DetectInfrequentViolations(
                            inputSequence, localParameterSpace, this.violationSupportLocal);
                    localViolations.Initialization(this.globalFreqPatterns);

                    HashMap<String, ViolationSequence> violations = localViolations.ViolationPatternMining();

                    String outputResult = deviceId + SQConfig.sepStrForKeyValue;
                    // output format: violation seq (occurrences) | violated seq1 | violated seq2 "tab"
                    for (Map.Entry<String, ViolationSequence> tempViolation : violations.entrySet()) {
                        outputResult += tempViolation.getValue().printViolation() + SQConfig.sepStrForKeyValue;
                    }
                    IntWritable outputKey = new IntWritable();
                    Text outputValue = new Text();
                    if (outputResult.length() > 0)
                        outputResult = outputResult.substring(0, outputResult.length() - 1);
                    outputKey.set(seqId);
                    outputValue.set(outputResult);
                    Configuration conf = context.getConfiguration();
                    String outputLocalViolationPath = conf.get(SQConfig.strLocalViolationOutput) + "-"
                            + conf.getInt(SQConfig.strLocalSupport, 10) + "-" + conf.getInt(SQConfig.strEventGap, 1)
                            + "-" + conf.getInt(SQConfig.strSeqGap, 1);
                    try {
                        mos.write(outputKey, outputValue,
                                outputLocalViolationPath + "/LocalViolation-" + context.getTaskAttemptID());
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                } catch (Error e) {
                    String line = oneValue.toString();
                    String[] splitValues = line.split(SQConfig.sepStrForKeyValue);
                    int seqId = Integer.parseInt(splitValues[0]);
                    String deviceId = splitValues[1];
                    String inputString = splitValues[2];
                    e.printStackTrace();
                    System.out.println("Catch Run out of memory exception " + line.length() + "," + seqId + "," + deviceId);
                    context.getCounter(top.distributed.lts.explanation.GenerateLocalFSViolationWithTS.Counters.runOutOfMemory).increment(1);
                } catch (Exception e) {
                    String line = oneValue.toString();
                    String[] splitValues = line.split(SQConfig.sepStrForKeyValue);
                    int seqId = Integer.parseInt(splitValues[0]);
                    String deviceId = splitValues[1];
                    String inputString = splitValues[2];
                    e.printStackTrace();
                    System.out.println("Catch Run out of memory exception " + line.length() + "," + seqId + "," + deviceId);
                    context.getCounter(top.distributed.lts.explanation.GenerateLocalFSViolationWithTS.Counters.runOutOfMemory).increment(1);
                }
            } // end collection data

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
        Job job = Job.getInstance(conf, "Generate Violations using Max");

        job.setJarByClass(GenerateViolationsWithTS.class);
        job.setMapperClass(GenerateViolationsWithTS.DDMapper.class);
        job.setReducerClass(GenerateViolationsWithTS.DDReducer.class);
        job.setMapOutputKeyClass(IntWritable.class);
        job.setMapOutputValueClass(Text.class);
        job.setOutputKeyClass(IntWritable.class);
        job.setOutputValueClass(Text.class);
        job.setNumReduceTasks(Integer.parseInt(conf.get(SQConfig.strNumReducers)));

        String strFSName = conf.get("fs.default.name");
        FileInputFormat.addInputPath(job, new Path(conf.get(SQConfig.strInputDataset)));
        FileSystem fs = FileSystem.get(conf);

        String outputLocalViolationPath = conf.get(SQConfig.strLocalViolationOutput) + "-"
                + conf.getInt(SQConfig.strLocalSupport, 10) + "-" + conf.getInt(SQConfig.strEventGap, 1) + "-"
                + conf.getInt(SQConfig.strSeqGap, 1);
        fs.delete(new Path(outputLocalViolationPath), true);
        FileOutputFormat.setOutputPath(job, new Path(outputLocalViolationPath));
        String violationGlocalFSPath = conf.get(SQConfig.strGlobalFSOutputForViolation) + "-" + conf.getInt(SQConfig.strLocalSupport, 10)
                + "-" + conf.getInt(SQConfig.strEventGap, 1) + "-" + conf.getInt(SQConfig.strSeqGap, 1);

        job.addCacheArchive(new URI(strFSName + violationGlocalFSPath));

        /** print job parameter */
        System.err.println("local support: " + conf.getInt(SQConfig.strLocalSupport, 10));
        System.err.println("event gap: " + conf.getInt(SQConfig.strEventGap, 1));
        System.err.println("sequence gap: " + conf.getInt(SQConfig.strSeqGap, 10));
        System.err.println("lts support: " + conf.getInt(SQConfig.strGlobalSupport, 10));
        long begin = System.currentTimeMillis();
        job.waitForCompletion(true);
        long end = System.currentTimeMillis();
        long second = (end - begin) / 1000;
        System.err.println(job.getJobName() + " takes " + second + " seconds");
        org.apache.hadoop.mapreduce.Counters cn = job.getCounters();


        System.out.println(conf.getInt(SQConfig.strSeqGap, 10) + "\t" + conf.getInt(SQConfig.strEventGap, 1) + "\t"
                + conf.getInt(SQConfig.strLocalSupport, 10) + "\t" + conf.getInt(SQConfig.strGlobalSupport, 10) +
                "\t" + second);
    }

    public static void main(String[] args) throws Exception {
        GenerateViolationsWithTS generateLocalFS = new GenerateViolationsWithTS();
        generateLocalFS.run(args);
    }
}
