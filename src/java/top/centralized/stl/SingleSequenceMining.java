package top.centralized.stl;

import top.core.stl.local.PrefixSpanToolWithFirstReplicate;
import top.parameterspace.LocalParameterSpace;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import top.core.datastructure.FreqSequence;
import top.core.datastructure.ResItemArrayPair;

public class SingleSequenceMining {
	private String inputString;
	LocalParameterSpace localParameterSpace;

	private ArrayList<FreqSequence> totalFrequentSeqs;
	private HashMap<String, FreqSequence> FreqSeqsInMap;
	private boolean[] available;

	public SingleSequenceMining(String inputString, LocalParameterSpace localParameterSpace) {
		this.setInputString(inputString);
		this.localParameterSpace = localParameterSpace;
		this.totalFrequentSeqs = new ArrayList<FreqSequence>();
		this.FreqSeqsInMap = new HashMap<String, FreqSequence>();
		this.available = new boolean[this.inputString.split(",").length];
		for (int i = 0; i < this.available.length; i++) {
			available[i] = false;
		}
	}


	public void updateSequenceLongToShort() {
		if (totalFrequentSeqs.size() == 0)
			return;
		int currentLength = totalFrequentSeqs.get(0).getItemNumInFreqSeq();
		int currentIndexInFS = 0;
		while (currentLength > 1 && currentIndexInFS < this.totalFrequentSeqs.size()) {
			// deal with all fs with size = currentLength;
			HashSet<Integer> tempUnavailableIndexes = new HashSet<Integer>();

			while (currentIndexInFS < this.totalFrequentSeqs.size()
					&& this.totalFrequentSeqs.get(currentIndexInFS).getItemNumInFreqSeq() == currentLength) {
				// check each index to see its new support value, if support >=
				// minSupport
				FreqSequence curFSObj = this.totalFrequentSeqs.get(currentIndexInFS);
				ArrayList<ResItemArrayPair> indexesForCurFS = curFSObj.getItemPairList();
				int curFinalSupport = 0;
				int curSupportTotal = indexesForCurFS.get(0).index.size();
				for (int i = 0; i < curSupportTotal; i++) {
					for (int j = 0; j < indexesForCurFS.size(); j++) {
						if (this.available[indexesForCurFS.get(j).index.get(i)]) {
							curFinalSupport++;
							break;
						}
					}
				}
				// add to results and add all indexes into tempUnavailable
				if (curFinalSupport >= localParameterSpace.getMinLocalSupport()) {
					this.FreqSeqsInMap.put(curFSObj.getFreqSeqInString(), curFSObj);
					curFSObj.setSupportNum(curFinalSupport);
					for (ResItemArrayPair resultPair : indexesForCurFS) {
						for (Integer deleteIndex : resultPair.index) {
							tempUnavailableIndexes.add(deleteIndex);
						}
					}
				}
				currentIndexInFS++;
			}

			for (Integer ii : tempUnavailableIndexes) {
				this.available[ii] = false;
			}
			if (currentIndexInFS < this.totalFrequentSeqs.size())
				currentLength = this.totalFrequentSeqs.get(currentIndexInFS).getItemNumInFreqSeq();
			else
				break;
		}

	}

	/**
	 * This one is the current version that excludes more (results == long to
	 * short algorithm)
	 *
	 * @param indexOfInput
	 * @return
	 */
	public Set<String> findFreqSeqInOneString(int indexOfInput,HashSet<String> globalFrequentElements) {
		// first compute frequent sequences
		long start = System.currentTimeMillis();
		PrefixSpanToolWithFirstReplicate pst = new PrefixSpanToolWithFirstReplicate(inputString, localParameterSpace);
		totalFrequentSeqs = pst.prefixSpanCalculate(globalFrequentElements);
		long end = System.currentTimeMillis()-start;
		// then sort by length of the string
		Collections.sort(totalFrequentSeqs);
		// init support number and a list of boolean for each sequence

		for (FreqSequence fs : totalFrequentSeqs) {
			for (ResItemArrayPair resultItemPair : fs.getItemPairList()) {
				for (Integer tempIndex : resultItemPair.index) {
					this.available[tempIndex] = true;
				}
			}
		}
		this.updateSequenceLongToShort();
		return FreqSeqsInMap.keySet();
	}

	
	public String getInputString() {
		return inputString;
	}
	public void setInputString(String inputString) {
		this.inputString = inputString;
	}


	public static void main(String[] args) {
		// String s = "A,B,C,A,B,C,A,B,A,C,B,A,B";
		// String s = "A,A,B,C,D,A,B,C,C,B,A,C,B,A,A,B,C";
		// String s = "D,E,F,Z,A,B,C,D,E,F,W,A,B,C,W,C,D";
		// String s = "a,b,b,b,c,z,z,z,a,b,c";
//		String s = "2,1,1,1,1,1";
		String s = "A,A,A,A,B,B,C,C,A,A,A,A,B,B,C,B";
		// String s =
		// "1,5,1,5,1,5,6,1,0,1,0,0,0,1,5,1,5,6,1,5,6,1,5,5,5,6,1,5,5,5,5,5,6,1,5,6,1,5,5,6,1,5,5,5,5,5,5,6,1,5,5,5,6,1,5,1,5,1,5,1,5,5,5,6,1,5,1,5,5,6,1,5,6,1,5,1,5,1,5,6,1,5,5,5,6,1,0,1,5,6,1,5,5,5,6,1,5,6,1,5";
		// String s =
		// "21,22,23,24,25,26,27,28,24,14,27,29,30,27,31,30,27,31,30,27,31,30,27,31,30,32,27,28,24,14,33,34,0,24,35,36,14,37,38,14,14,39,40,41,42,43,44,45,46,47,26,32,27,28,24,26,32,26,32,26,32,40,26,32,48,14,14,49,50,51,52,48,14,14,49,50,53,54,42,55,56,57,58,59,60,61,62,63,26,32,26,32,26,32,26,32,26,32,64,23,24,25,27,28,24,14,27,29,30,27,31,30,27,31,30,27,31,30,27,31,30,26,32,27,28,24,14,27,28,24,14,27,29,30,27,31,30,27,31,30,33,34,0,24,35,36,14,37,38,14,40,41,14,39,47,42,43,44,45,46,26,32,27,28,24,26,32,26,32,26,32,40,26,32,48,14,14,49,50,51,52,48,14,14,49,50,53,54,42,55,56,57,58,59,63,60,61,62,26,32,26,32,26,32,26,32,26,32,64,26,23,24,25,27,28,24,14,27,29,30,27,31,30,27,31,30,27,31,30,32,27,31,30,33,34,0,24,35,36,14,37,38,14,40,41,65,14,66,42,43,67,67,44,45,46,47,68,69,70,26,32,71,72,73,71,72,73,0,74,75,76,77,78,79,78,79,80,26,32,81,69,4,11,76,21,22,26,32,68,21,22,68,21,22,77,78,79,78,79,80,26,32,68,21,22,68,40,21,22,77,78,79,78,79,80,26,32,68,21,22,48,14,14,82,50,51,52,48,14,14,82,50,53,54,42,55,56,57,58,59,68,63,21,22,60,61,62,68,77,78,79,78,79,78,79,80,26,32,21,22,68,21,22,68,21,22,77,78,79,78,79,80,26,32,68,21,22,68,21,77,78,79,78,79,80,26,32,22,68,21,22,68,21,22,77,78,79,78,79,80,26,32,68,21,22,68,21,22,77,78,79,78,79,80,26,32,64,27,28,24,14,68,83,84,69,77,78,79,80,26,32,4,11,21,22";
		// System.out.println("Previous sequence size: " + s.split(",").length);
		int localSupport = 2;
		int itemGap = 0;
		int seqGap = 10;
		ArrayList<ArrayList<String>> cleanInputString = new ArrayList<ArrayList<String>>();
		ArrayList<ArrayList<Integer>> cleanIndexesForString = new ArrayList<ArrayList<Integer>>();
		String[] globalFE = { "A", "B", "C", "D" };
		HashSet<String> globalFrequentElements = new HashSet(Arrays.asList(globalFE));
		LocalParameterSpace localParameterSpace = new LocalParameterSpace(localSupport, itemGap, seqGap);
		SingleSequenceMining obj = new SingleSequenceMining(s, localParameterSpace);
		obj.findFreqSeqInOneString(0, globalFrequentElements);
	}
}
