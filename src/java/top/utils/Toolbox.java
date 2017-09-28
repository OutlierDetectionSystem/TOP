package top.utils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.Map.Entry;

public class Toolbox {
	public static HashSet<String> getGlobalFrequentElements(ArrayList<String> inputStringArray, int globalSupport,
			int localSupport) {
		System.out.println("I'm new Function!! Revised One~~~");
		HashMap<String, Integer> globalFrequency = new HashMap<String, Integer>();

		for (String inputString : inputStringArray) {
			HashMap<String, Integer> itemMap = new HashMap<>();
			String[] subItems = inputString.split(",");
			for (String temp : subItems) {
				if (!itemMap.containsKey(temp))
					itemMap.put(temp, 1);
				else
					itemMap.put(temp, itemMap.get(temp) + 1);
			}

			for (Map.Entry entry : itemMap.entrySet()) {
				String key = (String) entry.getKey();
				int count = (int) entry.getValue();

				if (count >= localSupport) {
					if (!globalFrequency.containsKey(key))
						globalFrequency.put(key, 1);
					else
						globalFrequency.put(key, globalFrequency.get(key) + 1);
				}
			}
		}
		HashSet<String> frequentItems = new HashSet<String>();
		for (Entry<String, Integer> entry : globalFrequency.entrySet()) {
			if (entry.getValue() >= globalSupport) {
				frequentItems.add(entry.getKey());
			}
		}
		return frequentItems;
	}

	public static Long dateTypeToLong(String str){
		SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd' 'HH:mm:ss.SSS");
		SimpleDateFormat formatter2 = new SimpleDateFormat("yyyy-MM-dd' 'HH:mm:ss");
		Date date = null;
		try {
			if (str.contains(".")) {
				date = formatter.parse(str);
			} else {
				date = formatter2.parse(str);
			}
		}
		catch (ParseException e) {
			e.printStackTrace();
		}
		return date.getTime();
	}
	private static final long MEGABYTE = 1024L * 1024L;

    public static long bytesToMegabytes(long bytes) {
        return bytes / MEGABYTE;
    }
    
    public static long getMaxMemory(){
    	Runtime runtime = Runtime.getRuntime();
    	return  bytesToMegabytes(runtime.maxMemory());
    }
    public static long checkCurrentUsedMemory(long maxUsedMemory){
    	Runtime runtime = Runtime.getRuntime();
		// Run the garbage collector
//		runtime.gc();
		// Calculate the used memory
		long memory = runtime.totalMemory() - runtime.freeMemory();
		if (bytesToMegabytes(memory) > maxUsedMemory) {
//			System.out.println("Used memory is bytes: " + memory);
//			System.out
//					.println("Used memory is megabytes: " + bytesToMegabytes(memory));
			maxUsedMemory = bytesToMegabytes(memory);
		}
		return maxUsedMemory;
    }
	
	public static void main(String [] args){
		ArrayList<String> dataset = new ArrayList<String>();
		dataset.add("a,b,c,a,c,a");
		dataset.add("a,c,d,a,b,a");
		HashSet<String> results = Toolbox.getGlobalFrequentElements(dataset, 2, 3);
		for(String str: results)
			System.out.println(str);
	}

}
