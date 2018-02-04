package top.core.maximum.mining;

import top.core.datastructure.FreqSequence;
import top.parameterspace.LocalParameterSpace;

import java.util.*;

public class SingleSequenceMining {
	private String inputString;
	private int minSupport;
	private int itemGap;
	private int seqGap;
	private ArrayList<FreqSequence> totalFrequentSeqs;
	private HashMap<ArrayList<String>, FreqSequence> FreqSeqsInMap;

	public SingleSequenceMining(String inputString, int minSupport, int itemGap, int seqGap) {
		this.setInputString(inputString);
		this.setMinSupport(minSupport);
		this.itemGap = itemGap;
		this.seqGap = seqGap;
		this.totalFrequentSeqs = new ArrayList<FreqSequence>();
		this.FreqSeqsInMap = new HashMap<ArrayList<String>, FreqSequence>();
	}

	public void updateSequenceLongToShort() {
		if (totalFrequentSeqs.size() == 0)
			return;
		int currentLength = totalFrequentSeqs.get(0).getItemNumInFreqSeq();
		int currentIndexInFS = 0;
		while (currentLength > 1 && currentIndexInFS < this.totalFrequentSeqs.size()) {
			// deal with all fs with size = currentLength;
			HashMap<ArrayList<String>, FreqSequence> tempFreqSeqInMap = new HashMap<ArrayList<String>, FreqSequence>();
			while (currentIndexInFS < this.totalFrequentSeqs.size()
					&& this.totalFrequentSeqs.get(currentIndexInFS).getItemNumInFreqSeq() == currentLength) {
				FreqSequence curFSObj = this.totalFrequentSeqs.get(currentIndexInFS);
				ArrayList<String> curFreqSeq = new ArrayList<String>(Arrays.asList(curFSObj.getFreqSeqInString().split(",")));
				
				if(!this.isSubString(this.FreqSeqsInMap.keySet(), curFreqSeq)){
					tempFreqSeqInMap.put(curFreqSeq, curFSObj);
				}
				currentIndexInFS++;
			}
			
			this.FreqSeqsInMap.putAll(tempFreqSeqInMap);
			if (currentIndexInFS < this.totalFrequentSeqs.size())
				currentLength = this.totalFrequentSeqs.get(currentIndexInFS).getItemNumInFreqSeq();
			else
				break;
		}

	}

	public boolean isSubString(Set<ArrayList<String>> existingFreqSeqs, ArrayList<String> curFreqSeq) {
		for (ArrayList<String> existingFS : existingFreqSeqs) {
			if (this.strArrayContains(existingFS, curFreqSeq)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * check sub array
	 *
	 * @param strList1
	 * @param strList2
	 * @return
	 */
	public boolean strArrayContains(ArrayList<String> strList1, ArrayList<String> strList2) {
		boolean isContained = false;

		for (int i = 0; i < strList1.size() - strList2.size() + 1; i++) {
			int k = i;
			int j = 0;
			while (k < strList1.size() && j < strList2.size()) {
				if (strList1.get(k).equals(strList2.get(j))) {
					k++;
					j++;
				} else {
					k++;
				}
			}
			if (j == strList2.size()) {
				isContained = true;
				break;
			}
		}

		return isContained;

	}

	/**
	 * This one is the current version that excludes more (results == long to
	 * short algorithm)
	 * 
	 * @param indexOfInput
	 * @return
	 */
	public Set<String> findFreqSeqInOneString(int indexOfInput, HashSet<String> globalFrequentElements) {
		// first compute frequent sequences
		long start = System.currentTimeMillis();
		LocalParameterSpace localParameterSpace = new LocalParameterSpace(minSupport, itemGap, seqGap);
		PrefixSpanToolWithFirstReplicate pst = new PrefixSpanToolWithFirstReplicate(inputString, localParameterSpace);
		totalFrequentSeqs = pst.prefixSpanCalculate(globalFrequentElements);

		// then sort by length of the string
		Collections.sort(totalFrequentSeqs);
		// init support number and a list of boolean for each sequence
		this.updateSequenceLongToShort();
		// for (String str : FreqSeqsInMap.keySet()) {
		// System.out.println("Result for one: " + str + ", Frequency: " +
		// FreqSeqsInMap.get(str).getSupportNum());
		// }
		// LocalFSOutlierDetection localOutlierDetection = new
		// LocalFSOutlierDetection(inputString, itemGap, seqGap,
		// FreqSeqsInMap);
		// localOutlierDetection.findOutliersInSequences(indexOfInput);
		// generateCleanInputString(indexOfInput, cleanInputString,
		// cleanIndexesForString);
		HashSet<String> finalReturnFS = new HashSet<String>();
		for(ArrayList<String> curFS: this.FreqSeqsInMap.keySet()){
			String finalSeq = "";
			for(String str: curFS){
				finalSeq+= str+ ",";
			}
			if(finalSeq.length() > 0)
				finalSeq = finalSeq.substring(0, finalSeq.length()-1);
			finalReturnFS.add(finalSeq);
			System.out.println(finalSeq + "\t" + this.FreqSeqsInMap.get(curFS).getItemPairList().get(0).index.size());
		}
		return finalReturnFS;
//		return FreqSeqsInMap.keySet();
	}

	public String getInputString() {
		return inputString;
	}

	public void setInputString(String inputString) {
		this.inputString = inputString;
	}

	public int getMinSupport() {
		return minSupport;
	}

	public void setMinSupport(int minSupport) {
		this.minSupport = minSupport;
	}

	public static void main(String[] args) {
		// String s = "A,B,C,A,B,C,A,B,A,C,B,A,B";
		String s = "A,A,B,C,D,A,B,C,C,B,A,C,B,A,A,B,C";
		// String s = "D,E,F,Z,A,B,C,D,E,F,W,A,B,C,W,C,D";
		// String s = "a,b,b,b,c,z,z,z,a,b,c";
		// String s = "2,1,1,1,1,1";
		// String s =
		// "1,5,1,5,1,5,6,1,0,1,0,0,0,1,5,1,5,6,1,5,6,1,5,5,5,6,1,5,5,5,5,5,6,1,5,6,1,5,5,6,1,5,5,5,5,5,5,6,1,5,5,5,6,1,5,1,5,1,5,1,5,5,5,6,1,5,1,5,5,6,1,5,6,1,5,1,5,1,5,6,1,5,5,5,6,1,0,1,5,6,1,5,5,5,6,1,5,6,1,5";
		// String s =
		// "21,22,23,24,25,26,27,28,24,14,27,29,30,27,31,30,27,31,30,27,31,30,27,31,30,32,27,28,24,14,33,34,0,24,35,36,14,37,38,14,14,39,40,41,42,43,44,45,46,47,26,32,27,28,24,26,32,26,32,26,32,40,26,32,48,14,14,49,50,51,52,48,14,14,49,50,53,54,42,55,56,57,58,59,60,61,62,63,26,32,26,32,26,32,26,32,26,32,64,23,24,25,27,28,24,14,27,29,30,27,31,30,27,31,30,27,31,30,27,31,30,26,32,27,28,24,14,27,28,24,14,27,29,30,27,31,30,27,31,30,33,34,0,24,35,36,14,37,38,14,40,41,14,39,47,42,43,44,45,46,26,32,27,28,24,26,32,26,32,26,32,40,26,32,48,14,14,49,50,51,52,48,14,14,49,50,53,54,42,55,56,57,58,59,63,60,61,62,26,32,26,32,26,32,26,32,26,32,64,26,23,24,25,27,28,24,14,27,29,30,27,31,30,27,31,30,27,31,30,32,27,31,30,33,34,0,24,35,36,14,37,38,14,40,41,65,14,66,42,43,67,67,44,45,46,47,68,69,70,26,32,71,72,73,71,72,73,0,74,75,76,77,78,79,78,79,80,26,32,81,69,4,11,76,21,22,26,32,68,21,22,68,21,22,77,78,79,78,79,80,26,32,68,21,22,68,40,21,22,77,78,79,78,79,80,26,32,68,21,22,48,14,14,82,50,51,52,48,14,14,82,50,53,54,42,55,56,57,58,59,68,63,21,22,60,61,62,68,77,78,79,78,79,78,79,80,26,32,21,22,68,21,22,68,21,22,77,78,79,78,79,80,26,32,68,21,22,68,21,77,78,79,78,79,80,26,32,22,68,21,22,68,21,22,77,78,79,78,79,80,26,32,68,21,22,68,21,22,77,78,79,78,79,80,26,32,64,27,28,24,14,68,83,84,69,77,78,79,80,26,32,4,11,21,22";
		// System.out.println("Previous sequence size: " + s.split(",").length);
		int localSupport = 1;
		int itemGap = 0;
		int seqGap = 5;
		SingleSequenceMining obj = new SingleSequenceMining(s, localSupport, itemGap, seqGap);
		// String[] list1 = { "B", "C" };
		// String[] list2 = { "B","D","C" };
		// ArrayList<String> strList1 = new
		// ArrayList<String>(Arrays.asList(list1));
		// ArrayList<String> strList2 = new
		// ArrayList<String>(Arrays.asList(list2));
		// System.out.println(obj.strArrayContains(strList1, strList2));
		String [] globalFE = {"A","B","C","D"};
		HashSet<String> globalFrequentElements = new HashSet(Arrays.asList(globalFE));
		 obj.findFreqSeqInOneString(0,globalFrequentElements);
	}
}
