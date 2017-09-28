package top.centralized.lts;

import top.inputs.InputFile;
import top.parameterspace.GlobalParameterSpace;
import top.utils.FileUtile;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

/**
 * Created by yizhouyan on 6/8/17.
 */
public class FSOutlierDetectionDriver {
    private InputFile inputInfo;
    private GlobalParameterSpace globalParameterSpace;

    public FSOutlierDetectionDriver(InputFile inputInfo, GlobalParameterSpace globalParameterSpace){
        this.inputInfo = inputInfo;
        this.globalParameterSpace = globalParameterSpace;
    }

    public void searchFSAndViolationDetection(String targetDirectory) {
        String violationFileNameTemp = "violationsLocal";
        HashSet<String> finalFreqSeq = this.searchLocalFrequentSequence(targetDirectory, violationFileNameTemp);
        this.violationDetection(targetDirectory, finalFreqSeq, violationFileNameTemp);
    }

    public HashSet<String> searchLocalFrequentSequence(String targetDirectory,
                                            String violationFileNameTemp) {
        GlobalFSDetectionViolation globalFS = new GlobalFSDetectionViolation(globalParameterSpace, inputInfo);
        globalFS.generateLocalFrequentSequences(violationFileNameTemp);
        File localResult = new File(targetDirectory, "Local-Outlier-Sequences.txt");
        globalFS.findLocalOutliersAndSaveInFile(localResult);
        File globalFile = new File(targetDirectory, "Global-Frequent-Sequences.txt");
        globalFS.saveGlobalFrequentSequences(globalFile);
        HashSet<String> finalFreqSeq = globalFS.getFinalGlobalFreqSeqs();
        return finalFreqSeq;
    }

    public void violationDetection(String targetDirectory, HashSet<String> finalFreqSeq,
                                 String tempViolationFileName) {
        // delete previous saved files
        System.out.println("Start Detecting Violations");
        File violationFile = new File(targetDirectory, "Violation-Outlier-Results.txt");
        violationFile.delete();

        ViolationDetection detectOutliers = new ViolationDetection(targetDirectory, finalFreqSeq, tempViolationFileName);
        detectOutliers.detectViolations(violationFile);
        // filter results
        File violationOutputFile = new File(targetDirectory, "Violation-Outlier-Results-filter.txt");
        this.reformatViolations(violationFile, violationOutputFile);
    }

    public void reformatViolations(File inputFile, File outputFile) {
        try {
            BufferedReader in = new BufferedReader(new FileReader(inputFile));

            String str;
            HashMap<String, Integer> sequence = new HashMap<String, Integer>();
            while ((str = in.readLine()) != null) {
                if (str.contains("#")) {
                    continue;
                } else {
                    if (sequence.containsKey(str))
                        sequence.put(str, sequence.get(str) + 1);
                    else {
                        sequence.put(str, 1);
                    }
                }
            }
            in.close();
            in = new BufferedReader(new FileReader(inputFile));
            BufferedWriter out = new BufferedWriter(new FileWriter(outputFile));
            String strId = "";
            while ((str = in.readLine()) != null) {
                if (str.contains("#")) {
                    strId = str;
                } else {
                    if (sequence.get(str) < 5 && str.split("\\|")[0].split(",").length > 1) {
//                        if(containDeviceId)
//                            out.write(deviceIdMap.get(Integer.parseInt(strId.substring(1,strId.length()))));
//                        else
                            out.write(strId);
                        out.newLine();
                        String [] subS = str.split("\\|");
                        out.write("Sequence:" + subS[0]);
                        out.newLine();
                        out.write("Violates Sequence: " + subS[1]);
//						out.write(str);
                        out.newLine();
                        out.newLine();
                        sequence.put(str, 100);
                        if(inputInfo.getMetaDataMapping()!=null) {
                            String[] subStr = str.split("\\|");
                            // parse violation sequence
                            out.write("Sequence:");
                            out.newLine();
                            String originalMeta = "";
                            for (String tempStr : subStr[0].split(",")) {
                                out.write(inputInfo.getMetaDataMapping().get(tempStr.trim()) + "\t");
                                originalMeta += inputInfo.getMetaDataMapping().get(tempStr.trim()) + " ,";
                            }

                            out.newLine();
                            out.newLine();
                            // parse backup sequence
                            out.write("Violates Sequence:");
                            out.newLine();
                            String violationMeta = "";
                            for (String tempStr : subStr[1].split(",")) {
                                out.write(inputInfo.getMetaDataMapping().get(tempStr.trim()) + "\t");
                                violationMeta += inputInfo.getMetaDataMapping().get(tempStr.trim()) + " ,";
                            }
                            out.newLine();
                            out.newLine();

                            if (originalMeta.length() > 0)
                                originalMeta = originalMeta.substring(0, originalMeta.length() - 1);
                            if (violationMeta.length() > 0)
                                violationMeta = violationMeta.substring(0, violationMeta.length() - 1);
                        }
                    }
                }
            }
            in.close();
            out.close();
        } catch (IOException e) {
            e.getStackTrace();
        }
    }

    public static void main(String [] args){
        String inputPath = "data/inputData/vlc-data.txt";
//        String inputPath = "extracted_1000devices_noId.csv";
        ArrayList<String> inputStringArray = new ArrayList<String>();
        HashMap<Integer, String> deviceIdMap = new HashMap<Integer, String>();
        FileUtile.readInDatasetWithDeviceIds(inputPath, inputStringArray, deviceIdMap);
        String metaDataFile = "data/inputData/vlc-dict.txt";
        HashMap<String, String> metaDataMapping = FileUtile.readInMetaDataToMemory(FileUtile.readInDataset(metaDataFile));

        InputFile inputInfo = new InputFile(inputStringArray, deviceIdMap, metaDataMapping, true);
        int localSupport = 5;
        int globalSupport = 10;
        int seqGap = 6;
        int eventGap = 0;
        GlobalParameterSpace globalParameterSpace = new GlobalParameterSpace(localSupport, eventGap, seqGap, globalSupport,
                (int) Math.floor(localSupport/2), 2);
        FSOutlierDetectionDriver driver = new FSOutlierDetectionDriver(inputInfo, globalParameterSpace);
        String outputFolder = "results-" + localSupport + "-" + globalSupport+ "-" + seqGap + "-"+ eventGap;
        new File(outputFolder).delete();
        new File(outputFolder).mkdir();
        driver.searchFSAndViolationDetection(outputFolder);
    }
}
