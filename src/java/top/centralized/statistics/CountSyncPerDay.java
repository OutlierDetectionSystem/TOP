package top.centralized.statistics;

import top.centralized.cleandatasets.SingleItem;
import top.utils.ConfigBasicStatistics;

import java.io.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by yizhouyan on 9/27/17.
 */
public class CountSyncPerDay {
    private int support = 50;
    private long timeInterval = 86400000; // one day (24 hours)
    private String event = "S";
    private HashMap<String, Integer> maxSyncPerDay = new HashMap<>();
    private HashMap<String, Long> maxSyncDate = new HashMap<>();
    private SimpleDateFormat formatter =  new SimpleDateFormat("yyyy-MM-dd' 'HH:mm:ss.SSS");

    public void countMaxEventOccurrence(String deviceStr, String inputString){
        String[] splitInputs = inputString.split(ConfigBasicStatistics.sepStrForRecord);
        String[] splitInputValues = new String[splitInputs.length];
        long[] splitInputTimeStamps = new long[splitInputs.length];
        // phrase data
        for (int i = 0; i < splitInputs.length; i++) {
            String[] subs = splitInputs[i].split(ConfigBasicStatistics.sepSplitForIDDist);
            splitInputValues[i] = subs[0];
            splitInputTimeStamps[i] = Long.parseLong(subs[1]);
        }
        int maxCountEvent = 0;
        long dateForMax = 0;
        for (int i = 0; i< splitInputs.length; i++){
            if(splitInputValues[i].equals(event)){
                // extend 24 hours and count events
                int countEventNum = 0;
                for(int j = i+1; j< splitInputs.length; j++){
                    if(splitInputTimeStamps[j]-splitInputTimeStamps[i] > timeInterval)
                        break;
                    if(splitInputValues[j].equals(event)) {
                        countEventNum++;
                    }
                }
                if(countEventNum > maxCountEvent){
                    maxCountEvent = countEventNum;
                    dateForMax = splitInputTimeStamps[i];
                }
            }
        }
        if(maxCountEvent > support){
            maxSyncPerDay.put(deviceStr, maxCountEvent);
            maxSyncDate.put(deviceStr, dateForMax);
        }
    }

    public void readInAndProcess(String inputFile){
        try {
            BufferedReader br = new BufferedReader(new FileReader(inputFile));
            String str = "";
            int count = 0;
            br.readLine();
            while ((str = br.readLine()) != null) {
                String [] splits = str.split("\t");
                countMaxEventOccurrence(splits[1], splits[2]);
                count++;
                if(count % 10000 == 0){
                    System.out.println(count + " finished!");
                }
            }
            br.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void outputToFile(String outputFile){
        System.out.println("Num of Devices have more than "  + support  + " number of "+ event + ":" + this.maxSyncPerDay.size());
        int maxSync = 0;
        try {
            BufferedWriter bw = new BufferedWriter(new FileWriter(new File(outputFile)));
            for(Map.Entry<String,Integer> curDevice: this.maxSyncPerDay.entrySet()){
                Date date=new Date(maxSyncDate.get(curDevice.getKey()));
                String dateText = formatter.format(date);
                // "," + maxSyncDate.get(curDevice.getKey()) +
                bw.write(curDevice.getKey() + "," + curDevice.getValue() + "," + dateText);
                bw.newLine();
            }
            bw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void CountSyncPerDay(String inputFile, String outputFile){
        readInAndProcess(inputFile);
        outputToFile(outputFile);
    }
    public static void main(String [] args){
        String inputFile = "data/inputData/CT_data_formatted.csv";
        String outputFile = "countSynSupport.csv";
        CountSyncPerDay cs = new CountSyncPerDay();
        cs.CountSyncPerDay(inputFile, outputFile);
        System.out.println(System.currentTimeMillis());
    }
}
