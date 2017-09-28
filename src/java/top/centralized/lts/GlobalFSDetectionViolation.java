package top.centralized.lts;

import top.inputs.InputFile;
import top.parameterspace.LocalParameterSpace;
import top.parameterspace.GlobalParameterSpace;
import top.inputs.InputSequence;
import top.core.lts.local.withviolation.nots.LocalFSDetectionWithViolation;
import top.utils.Toolbox;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.Map.Entry;

public class GlobalFSDetectionViolation {
	private GlobalParameterSpace globalParameterSpace;
	private HashSet<String> finalGlobalFreqSeqs;
	private HashMap<Integer, ArrayList<String>> localFrequentSeqsStatic;
	private InputFile inputInfo;
	private int maxLength = -1;

	public GlobalFSDetectionViolation(GlobalParameterSpace globalParameterSpace,
									 InputFile inputInfo) {
		this.globalParameterSpace = globalParameterSpace;
		this.finalGlobalFreqSeqs = new HashSet<String>();
		this.localFrequentSeqsStatic = new HashMap<Integer, ArrayList<String>>();
		this.inputInfo = inputInfo;
	}

	/**
	 * deal with each String and generate frequent sequence
	 */
	public void generateLocalFrequentSequences(String violationFileNameTemp) {
		HashSet<String> globalFrequentElements = Toolbox.getGlobalFrequentElements(inputInfo.getInputStringArray(),
				globalParameterSpace.getMinGlobalSupport(),
				globalParameterSpace.getLocalParameterSpace().getMinLocalSupport());
		new File(violationFileNameTemp).delete();
		try {
			BufferedWriter out = new BufferedWriter(
					new FileWriter(new File("localFS-lts-" +
							globalParameterSpace.getLocalParameterSpace().getMinLocalSupport() + ".txt")));

			HashMap<String, Integer> globalFreqSeqInMap = new HashMap<String, Integer>();
			for (int i = 0; i < inputInfo.getInputStringArray().size(); i++) {
				String inputStr = inputInfo.getInputStringArray().get(i);
                InputSequence inputSequence = new InputSequence(inputStr);
                LocalParameterSpace localParameterSpace = this.globalParameterSpace.getLocalParameterSpace();
                LocalFSDetectionWithViolation localFS = new LocalFSDetectionWithViolation(inputSequence, localParameterSpace,
						this.globalParameterSpace.getViolationLocalSupport());
				localFS.Initialization(globalFrequentElements);
				ArrayList<String> tempLocalFS = localFS.LocalFrequentSequenceMining(i);
				// output temp violations
				this.outputLocalViolations(localFS.getViolations(), i, violationFileNameTemp);
				out.write("#" + i);
				out.newLine();
				if (tempLocalFS.size() > 0) {
					this.localFrequentSeqsStatic.put(i, tempLocalFS);
					for (String str : tempLocalFS) {
						out.write(str + "\t" );
						if (globalFreqSeqInMap.containsKey(str))
							globalFreqSeqInMap.put(str, globalFreqSeqInMap.get(str) + 1);
						else
							globalFreqSeqInMap.put(str, 1);
					}
				}
				if (i % 1000 == 0) {
					System.out.println(i + " Finished!");
				}
				out.newLine();
			}
			// generate global frequent sequence
			for(Entry<String, Integer> entry: globalFreqSeqInMap.entrySet()){
//			System.out.println(entry.getKey() + "\t" + entry.getValue());
				if(entry.getValue() >= globalParameterSpace.getMinGlobalSupport())
					this.finalGlobalFreqSeqs.add(entry.getKey());
			}
		}catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}


	}

	public void outputLocalViolations(HashMap<String, Set<String>> violations, int sequenceId, String fileName){
		BufferedWriter bw;
		try {
			bw = new BufferedWriter(new FileWriter(new File(fileName), true));
			bw.write("#" + sequenceId);
			bw.newLine();
			for(Entry<String, Set<String>> tempViolation: violations.entrySet()){
				for(String str: tempViolation.getValue()){
					bw.write(tempViolation.getKey() + "|" + str);
					bw.newLine();
				}
			}
			bw.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public HashSet<String> getFinalGlobalFreqSeqs() {
		return finalGlobalFreqSeqs;
	}

	public void setFinalGlobalFreqSeqs(HashSet<String> finalGlobalFreqSeqs) {
		this.finalGlobalFreqSeqs = finalGlobalFreqSeqs;
	}

	public void saveGlobalFrequentSequences(File newFile) {
		// String fileName = "SeqMatching/Global-Freq-Sequences";
		try {
			BufferedWriter out = new BufferedWriter(new FileWriter(newFile));
			for (String str : this.finalGlobalFreqSeqs) {
				out.write(str);
				out.newLine();
				out.newLine();
				if(inputInfo.getMetaDataMapping()!=null) {
					String[] subStr = str.split(",");
					for (String tempStr : subStr) {
						out.write(inputInfo.getMetaDataMapping().get(tempStr.trim()) + "\t");
					}
					out.newLine();
					out.newLine();
				}
			}
			out.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void findLocalOutliersAndSaveInFile(File localResult) {
//		System.out.println(this.thresholdForLocalFSOutliers);
		HashMap<String, Integer> gbFreqSeqs = new HashMap<String, Integer>();
		for (Entry<Integer, ArrayList<String>> curElement : this.localFrequentSeqsStatic.entrySet()) {
			for (String str : curElement.getValue()) {
				// System.out.println("Temp Result: " + str);
				if (gbFreqSeqs.containsKey(str))
					gbFreqSeqs.put(str, gbFreqSeqs.get(str) + 1);
				else
					gbFreqSeqs.put(str, 1);
			}
		}

		HashSet<String> finalGBFreqSeq = new HashSet<String>();
		for (String str : gbFreqSeqs.keySet()) {
			if (gbFreqSeqs.get(str) >= globalParameterSpace.getThresholdForLocalFSOutliers()) {
				finalGBFreqSeq.add(str);
			}
		}
		try {
			// BufferedWriter out = new BufferedWriter(new
			// FileWriter("OutlierRes/" + fileName + ".tsv"));
			BufferedWriter out = new BufferedWriter(new FileWriter(localResult));
			int numOutlierTimeSeries = 0;
			int numOutliersInTotal = 0;
			int maxOutlierInOneSeries = 0;
			for (Entry<Integer, ArrayList<String>> curElement : this.localFrequentSeqsStatic.entrySet()) {
				HashSet<String> tempOutliers = new HashSet<String>();
				for (String curStr : curElement.getValue()) {
					if (!finalGBFreqSeq.contains(curStr))
						tempOutliers.add(curStr);
				}
				if (tempOutliers.size() != 0) {
					numOutlierTimeSeries++;
					numOutliersInTotal += tempOutliers.size();
					maxOutlierInOneSeries = Math.max(maxOutlierInOneSeries, tempOutliers.size());
					out.write("Sequence # " + curElement.getKey());
					out.newLine();
					for (String curStr : tempOutliers) {
						out.write(curStr);
						out.newLine();
//						out.newLine();
						if(inputInfo.getMetaDataMapping()!=null) {
//							System.out.println("Meta not null");
							String[] subStr = curStr.split(",");
							for (String tempStr : subStr) {
								out.write(inputInfo.getMetaDataMapping().get(tempStr.trim()) + "\t");
								// System.out.println(metaDataMapping.get(tempStr));
							}

							out.newLine();
							out.newLine();
							out.newLine();
						}
					}
					out.newLine();
				}
			}
			out.close();
			System.out.println(numOutlierTimeSeries + " , " + numOutliersInTotal + " , " + maxOutlierInOneSeries);


		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}
}
