package top.centralized.cleandatasets;

import java.io.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Created by yizhouyan on 9/25/17.
 */
public class CleanCTDataConcatenate {

    public CleanCTDataConcatenate(){
    }

    public void cleanCTData(String inputFile, String outputFile){
        try {
            BufferedReader br = new BufferedReader(new FileReader(inputFile));
            BufferedWriter bw = new BufferedWriter(new FileWriter(new File(outputFile)));
            String str = "";
            int count = 0;
            int seqID = 0;
            String tempStr = "";


            while ((str = br.readLine()) != null) {
                String [] splits = str.split("\t");
                tempStr += splits[2] + ",";
                count += splits[2].split(",").length;

                if(count >= 100000){
                    tempStr = tempStr.substring(0, tempStr.length()-1);
                    bw.write(seqID + "\t" + seqID + "\t" + tempStr);
                    System.out.println(count);
                    bw.newLine();
                    count = 0;
                    tempStr = "";
                    seqID += 1;

                }
            }

            br.close();
            bw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String [] args){
//        String inputFile = args[0];
//        String outputFile = args[1];
        String inputFile = "data/inputData/CT_data_formatted_noTS.csv";
        String outputFile = "data/inputData/CT_data_formatted_noTS_concat.csv";
        CleanCTDataConcatenate cleanDataset = new CleanCTDataConcatenate();
        cleanDataset.cleanCTData(inputFile, outputFile);
    }
}
