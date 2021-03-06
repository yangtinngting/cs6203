package mylab0;


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.StringTokenizer;

import org.apache.hadoop.io.IntWritable.Comparator;
import org.apache.hadoop.io.WritableComparable;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapred.MapReduceBase;
import org.apache.hadoop.mapred.OutputCollector;
import org.apache.hadoop.mapred.Reporter;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.map.InverseMapper;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.input.KeyValueTextInputFormat;
import org.apache.hadoop.mapreduce.lib.input.MultipleInputs;
import org.apache.hadoop.mapreduce.lib.input.TextInputFormat;
import org.apache.hadoop.mapreduce.lib.output.TextOutputFormat;
import org.apache.hadoop.mapreduce.lib.input.SequenceFileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.SequenceFileOutputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;

import org.apache.hadoop.util.GenericOptionsParser;

public class CommonWords {

	// tokenize file 1
	public static class TokenizerWCMapper1 extends
			Mapper<Object, Text, Text, Text> {

		Set<String> stopwords = new HashSet<String>();

		@Override
		protected void setup(Context context) {
			//Read stopwords from HDFS and put it into the Set<String> stopwords
			Configuration conf = context.getConfiguration();
			try {
			Path path = new Path("/user/hadoop/commonwords/stopwords/sw4.txt");
			FileSystem fs= FileSystem.get(new Configuration());
			BufferedReader br = new BufferedReader(new InputStreamReader(fs.open(path)));
			String word = null;
			while ((word= br.readLine())!= null) {
			stopwords.add(word);
			}
			} catch (IOException e) {
			e.printStackTrace();
			}
		}

		private Text word = new Text();
		private final static Text identifier = new Text("f1");
		private String pattern = "[^\\w]"; //represent the left characters except all the digits,capital and lowercase letters		
		
		public void map(Object key, Text value, Context context)
				throws IOException, InterruptedException {
			//StringTokenizer itr = new StringTokenizer(line," . ! ? - : ; [ ] , ' \r \n \r \f \" $ * ( )");
			String line = value.toString().toLowerCase();  //convert to lowercase letters
			line = line.replaceAll(pattern," ");//replace the elements in pattern with spaces
			StringTokenizer itr = new StringTokenizer(line);
			while (itr.hasMoreTokens()) {
				word.set(itr.nextToken());
				//check whether the word is in stopword list
				if(stopwords.contains(word.toString()))
					continue;
				//write out <word, f1>
				context.write(word,identifier);

			}
		}
	}

	// tokenize file 2
	public static class TokenizerWCMapper2 extends
			Mapper<Object, Text, Text, Text> {

		Set<String> stopwords = new HashSet<String>();

		@Override
		protected void setup(Context context) {
			//Read stopwords from HDFS and put it into the Set<String> stopwords
			Configuration conf = context.getConfiguration();
			try {
			Path path = new Path("/user/hadoop/commonwords/stopwords/sw4.txt");
			FileSystem fs= FileSystem.get(new Configuration());
			BufferedReader br = new BufferedReader(new InputStreamReader(fs.open(path)));
			String word = null;
			while ((word= br.readLine())!= null) {
			stopwords.add(word);
			}
			} catch (IOException e) {
			e.printStackTrace();
			}
		}

		private Text word = new Text();
		private final static Text identifier = new Text("f2");
		private String pattern = "[^\\w]"; //represent the left characters except all the digits,capital and lowercase letters		
       
		public void map(Object key, Text value, Context context)
				throws IOException, InterruptedException {
			String line = value.toString().toLowerCase();  //convert to lowercase letters
			//StringTokenizer itr = new StringTokenizer(line," . ! ?  - : ; [ ] , ' \r \n \r \f \" $ * ( )");
			line = line.replaceAll(pattern," ");//replace the elements in pattern with spaces
			StringTokenizer itr = new StringTokenizer(line);
			while (itr.hasMoreTokens()) {
				word.set(itr.nextToken());
				//check whether the word is in stopword list
				if(stopwords.contains(word.toString()))
					continue;
				//write out <word, f2>
				context.write(word, identifier);
			}
		}
	}

	// get the number of common words
	public static class CommonWordsReducer extends
			Reducer<Text, Text, Text, IntWritable> {

		private IntWritable commoncount = new IntWritable();

		public void reduce(Text key, Iterable<Text> values, Context context)
				throws IOException, InterruptedException {
			//maintain two counts for file 1 and file 2
			int count1 = 0;
			int count2 = 0;
			for (Text val : values){			
				// increase count1 or count1
				if (val.equals(new Text("f1"))) ++count1;
				if (val.equals(new Text("f2"))) ++count2;

			}
			if (count1 != 0 && count2 != 0) {
				//It is a common word here. Output its name and count
			context.write(key,new IntWritable(count1>count2?count2:count1));
			}
		}
	
	}
	
	//output in decreasing order on key
	public static class IntWritableDecreasingComparator extends Comparator{
		public int compare(WritableComparable a, WritableComparable b){
			return -super.compare(a,b);
		}
		@Override
		public int compare( byte[] b1,int s1,int l1,byte[] b2,int s2,int l2){
			return -super.compare(b1,s1,l1,b2,s2,l2);
		}
	}


	/*public static class SortReducer extends
			Reducer<IntWritable, Text, IntWritable, Text> {
  
		public void reduce(IntWritable key, Iterable<Text> values,
				Context context) throws IOException, InterruptedException {
			//write out <key (word frequency) and value (word)>
			Iterator<Text> itr=values.iterator();
			while(itr.hasNext()){
				context.write(key,itr.next());			
			}
		}
	}*/
	

	public static void main(String[] args) throws IOException,
			InterruptedException, ClassNotFoundException {
		// Provide filenames
		Configuration conf = new Configuration();
		String[] otherArgs = new GenericOptionsParser(conf, args).getRemainingArgs();
		if (otherArgs.length != 4) {
			System.err
					.println("Usage: TopKCommonWords <input1> <input2> <output1> "
							+ "<output2>");
			System.exit(2);
		}
        
		//Configure job 1 for counting common words
		Job job1 = new Job(conf, "Count Commond Words");
		job1.setJarByClass(CommonWords.class);
		//set input1 and input2 as input and <output1> as output for this MapReduce job.
		MultipleInputs.addInputPath(job1, new Path(otherArgs[0]),TextInputFormat.class,TokenizerWCMapper1.class);
		MultipleInputs.addInputPath(job1, new Path(otherArgs[1]),TextInputFormat.class,TokenizerWCMapper2.class);
		FileOutputFormat.setOutputPath(job1,new Path(otherArgs[2]));
		//set Mapper and Reduce class, output key, value class 
		job1.setMapOutputKeyClass(Text.class);
		job1.setMapOutputValueClass(Text.class);
		job1.setOutputKeyClass(Text.class);
		job1.setOutputValueClass(IntWritable.class);
		//job1.setCombinerClass(CommonWordsReducer.class);
                job1.setReducerClass(CommonWordsReducer.class);
                job1.setOutputFormatClass(SequenceFileOutputFormat.class);
		job1.waitForCompletion(true);
		//Configure job 2 for sorting
		Job job2 = new Job(conf, "sort");
		job2.setJarByClass(CommonWords.class);
		//set input and output for MapReduce job 2 here
		FileInputFormat.setInputPaths(job2,new Path(otherArgs[2]));
		FileOutputFormat.setOutputPath(job2,new Path(otherArgs[3]));
		//set Mapper and Reduce class, output key, value class
		job2.setMapOutputKeyClass(IntWritable.class);
		job2.setMapOutputValueClass(Text.class);
		job2.setOutputKeyClass(IntWritable.class);
		job2.setOutputValueClass(Text.class);
		job2.setMapperClass(InverseMapper.class);
		//job2.setCombinerClass(SortReducer.class);
                //job2.setReducerClass(SortReducer.class);
                job2.setInputFormatClass(SequenceFileInputFormat.class);
                job2.setSortComparatorClass(IntWritableDecreasingComparator.class);
		System.exit(job2.waitForCompletion(true) ? 0 : 1);
		
	}
}
