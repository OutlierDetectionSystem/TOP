package top.centralized.cleandatasets;

import org.apache.commons.math3.analysis.function.Sin;

import java.io.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Created by yizhouyan on 9/25/17.
 */
public class CleanCTData {
    private HashMap<String, String> urlCode;
    private HashMap<String, ArrayList<SingleItem>> stringInDevice;
    private HashSet<String> cannotPharse = new HashSet<>();
    private SimpleDateFormat formatter =  new SimpleDateFormat("yyyy-MM-dd' 'HH:mm:ss.SSS");

    public CleanCTData(){
    }

    private void initURLCode(String dicFile){
        this.urlCode = new HashMap<>();
        try {
            BufferedReader br = new BufferedReader(new FileReader(dicFile));
            String str = "";
            while ((str = br.readLine()) != null) {
                String [] splits = str.split("\t");
                urlCode.put(splits[1], splits[0]);
            }
            br.close();
            System.out.println("Dictionary Size: " + urlCode.size());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void addToDevices(String deviceId, SingleItem newItem){
        if(stringInDevice.containsKey(deviceId)){
            this.stringInDevice.get(deviceId).add(newItem);
        }else{
            this.stringInDevice.put(deviceId, new ArrayList<SingleItem>());
            this.stringInDevice.get(deviceId).add(newItem);
        }
    }

    private String findKeyForItem(String newItem){
        String trimmedItem = newItem.substring(2, newItem.length()-1);
        String [] splits = trimmedItem.split("\\/");
        if(splits.length >= 2) {
            String newItemForKey = splits[0] + "/" + splits[1];
            if (this.urlCode.containsKey(newItemForKey)) {
                return this.urlCode.get(newItemForKey);
            }
        }
        if(!cannotPharse.contains(newItem)) {
            System.out.println("Cannot pharse: " + newItem);
            cannotPharse.add(newItem);
        }
        return null;
    }

    public void sortByDate(ArrayList<SingleItem> sequences) {
        Collections.sort(sequences, new Comparator<SingleItem>() {
            public int compare(SingleItem str1, SingleItem str2) {
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

    private void readInputData(String inputFile){
        this.stringInDevice = new HashMap<String, ArrayList<SingleItem>>();
        try {
            BufferedReader br = new BufferedReader(new FileReader(inputFile));
            String str = "";
            br.readLine();
            int count = 0;
            while ((str = br.readLine()) != null) {
                String [] splits = str.split(",");
                Date date = formatter.parse(splits[0].substring(1,splits[0].length()-1));
                String keyForItem = this.findKeyForItem(splits[2]);
                if(keyForItem!=null){
                    addToDevices(splits[1], new SingleItem(date, keyForItem));
                }
                count++;
                if(count % 1000000 == 0){
                    System.out.println(count + " finished!");
                }
            }
            br.close();
            System.out.println("Dictionary Size: " + urlCode.size());
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }

    private void outputToFile(String outputFile){
        System.out.println("Num of Devices: " + stringInDevice.size());
        int maxSync = 0;
        try {
            BufferedWriter bw = new BufferedWriter(new FileWriter(new File(outputFile)));
            BufferedWriter bw2 = new BufferedWriter(new FileWriter(new File("countEvents.csv")));
            int deviceId = 0;
            for(Map.Entry<String, ArrayList<SingleItem>> curDevice: this.stringInDevice.entrySet()){
                StringBuilder deviceInStr = new StringBuilder();
                int maxS = 0;
                deviceInStr.append(deviceId + "\t" + curDevice.getKey()+ "\t");
                this.sortByDate(curDevice.getValue());
                for(SingleItem s: curDevice.getValue()){
//                    deviceInStr.append(s.getItem() + "|" + formatter.format(s.getDate()) + ",");
                    deviceInStr.append(s.getItem() + "|" + s.getDate().getTime() + ",");
                    if(s.getItem().equals("S")){
                        maxS++;
                    }
                }
                maxSync = Math.max(maxSync, maxS);
                String finalOutput = deviceInStr.toString();
                if(finalOutput.endsWith(",")){
                    finalOutput = finalOutput.substring(0, finalOutput.length()-1);
                }
                bw.write(finalOutput);
                bw.newLine();
                deviceId++;
                bw2.write(curDevice.getKey() + "," + curDevice.getValue().size());
                bw2.newLine();
            }
            bw.close();
            bw2.close();
            System.out.println("Max Sync Count: " + maxSync);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void cleanCTData(String dicFile, String inputFile, String outputFile){
        this.initURLCode(dicFile);
        this.readInputData(inputFile);
        this.outputToFile(outputFile);
    }
    public static void main(String [] args){
        String dicFile = "data/inputData/ct-dict.txt";
        String inputFile = "data/inputData/CT_data.csv";
        String outputFile = "data/inputData/CT_data_formatted.csv";
        CleanCTData cleanDataset = new CleanCTData();
        cleanDataset.cleanCTData(dicFile, inputFile, outputFile);
    }
}
