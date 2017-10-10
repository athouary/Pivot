package fr.ece;

import java.io.IOException;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.TreeMap;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;

/**
 * Implementation of Map and Reduce to compute a table pivot from a .csv file.
 * @author athouary
 */
public class Pivot {

  public static class ColumnNumMapper extends Mapper<LongWritable, Text, IntWritable, Text>{
    private Text line = new Text();
    private IntWritable keyOut = new IntWritable();
    private Text valueOut = new Text();
    
    /**
     * Maps the future line number as key and the cell content + the future column number as value.
	 * @param key
	 * @param value
	 * @param context
	 * @throws IOException
	 * @throws InterruptedException
     */
    public void map(LongWritable key, Text value, Context context) throws IOException, InterruptedException {
      StringTokenizer itr = new StringTokenizer(value.toString());
      long column = key.get(); 
      while (itr.hasMoreTokens()) {
        line.set(itr.nextToken());
        String[] splitLine = line.toString().split(",");
        for (int i=0;  i < splitLine.length; i++) {
        		keyOut.set(i);
        		valueOut.set((splitLine[i] + "," + Long.toString(column)));
        		context.write(keyOut, valueOut);
         }
      }
    }
  }

  public static class ColumnReducer extends Reducer<IntWritable,Text,IntWritable,Text> {
    private Text textOut = new Text();
	private String[] splitValues;
	
	/**
	 * Reduces the values to a single line with "," separators.
	 * Uses a TreeMap to get the values ordered according to the column number previously stored.
	 * @param key
	 * @param values
	 * @param context
	 * @throws IOException
	 * @throws InterruptedException
	 */
    public void reduce(IntWritable key, Iterable<Text> values, Context context) throws IOException, InterruptedException {
    		Map<Integer, String> valuesOut = new TreeMap<Integer, String>();
        String lineOut = new String();
    		for(Text value : values) {
    			splitValues = value.toString().split(",");
    		    valuesOut.put(Integer.parseInt(splitValues[1]),splitValues[0]);
    		}
    		for(Map.Entry<Integer,String> entry : valuesOut.entrySet()) {
    			lineOut += entry.getValue() + ",";
    		}
    		textOut.set(lineOut.substring(0, lineOut.length()-1));
    		context.write(key, textOut);
    }
  }

  public static void main(String[] args) throws Exception {
    Configuration conf = new Configuration();
    Job job = Job.getInstance(conf, "Pivot");
    job.setJarByClass(Pivot.class);
    job.setMapperClass(ColumnNumMapper.class);
    job.setReducerClass(ColumnReducer.class);
    job.setMapOutputKeyClass(IntWritable.class);
    job.setMapOutputValueClass(Text.class);
    job.setOutputKeyClass(IntWritable.class);
    job.setOutputValueClass(Text.class);
    FileInputFormat.addInputPath(job, new Path(args[0]));
    FileOutputFormat.setOutputPath(job, new Path(args[1]));
    System.exit(job.waitForCompletion(true) ? 0 : 1);
  }
}