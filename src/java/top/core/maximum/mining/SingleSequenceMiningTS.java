package top.core.maximum.mining;

import top.inputs.InputSequenceWithTS;
import top.parameterspace.LocalParameterSpaceWithTS;

import java.util.*;

public class SingleSequenceMiningTS {
	private InputSequenceWithTS inputSequence;
	private LocalParameterSpaceWithTS localParameterSpace;

	private ArrayList<FreqSequence> totalFrequentSeqs;
	private HashMap<ArrayList<String>, FreqSequence> FreqSeqsInMap;

	public SingleSequenceMiningTS(InputSequenceWithTS inputSequence, LocalParameterSpaceWithTS localParameterSpace) {
	    this.inputSequence = inputSequence;
	    this.localParameterSpace = localParameterSpace;

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
	 * @return
	 */
	public Set<String> findFreqSeqInOneString(HashSet<String> globalFrequentElements) {
		// first compute frequent sequences
		long start = System.currentTimeMillis();

		PrefixSpanToolWithFirstReplicateTS pst = new PrefixSpanToolWithFirstReplicateTS(inputSequence, localParameterSpace);
		totalFrequentSeqs = pst.prefixSpanCalculate(globalFrequentElements);

		// then sort by length of the string
		Collections.sort(totalFrequentSeqs);
		// init support number and a list of boolean for each sequence
		this.updateSequenceLongToShort();

		HashSet<String> finalReturnFS = new HashSet<String>();
		for(ArrayList<String> curFS: this.FreqSeqsInMap.keySet()){
			String finalSeq = "";
			for(String str: curFS){
				finalSeq+= str+ ",";
			}
			if(finalSeq.length() > 0)
				finalSeq = finalSeq.substring(0, finalSeq.length()-1);
			finalReturnFS.add(finalSeq);
//			System.out.println(finalSeq + "\t" + this.FreqSeqsInMap.get(curFS).getItemPairList().get(0).index.size());
		}
		return finalReturnFS;
//		return FreqSeqsInMap.keySet();
	}

	public static void main(String[] args) {
		// String s = "A,B,C,A,B,C,A,B,A,C,B,A,B";
        String s= "A|1473902373698,B|1473902433758,C|1473902433815,A|1473902433843,B|1473902485507,C|1473902485507,A|1473902485507,C|1473902485512,B|1473902485512,C|1473902485532," +
                "A|1473902485574,C|1473902485628,B|1473902485633,A|1473902486252,C|1473902487718,D|1473902487072";
        int localSupport = 3;
        int itemGap = 0;
        int seqGap = 10;
        long itemInterval = 10000;
        long sequenceInterval = 60000;
        LocalParameterSpaceWithTS localParameterSpace = new LocalParameterSpaceWithTS(localSupport, itemGap, seqGap, itemInterval, sequenceInterval);
        InputSequenceWithTS inputSequence = new InputSequenceWithTS(s);

		SingleSequenceMiningTS obj = new SingleSequenceMiningTS(inputSequence, localParameterSpace);

		String [] globalFE = {"A","B","C","D"};
		HashSet<String> globalFrequentElements = new HashSet(Arrays.asList(globalFE));
        obj.findFreqSeqInOneString(globalFrequentElements);
	}
}
