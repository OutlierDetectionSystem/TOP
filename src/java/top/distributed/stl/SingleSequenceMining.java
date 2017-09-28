package top.distributed.stl;

import java.util.*;

import org.apache.hadoop.mapreduce.Reducer.Context;

import top.core.datastructure.FreqSequence;
import top.core.datastructure.ResItemArrayPair;
import top.core.stl.local.PrefixSpanToolWithFirstReplicate;
import top.distributed.stl.GenerateLocalFS.Counters;
import top.parameterspace.LocalParameterSpace;
import top.utils.Toolbox;

public class SingleSequenceMining {
	private String inputString;
	private int minSupport;
	private int itemGap;
	private int seqGap;
	private ArrayList<FreqSequence> totalFrequentSeqs;
	private HashMap<String, FreqSequence> FreqSeqsInMap;
	private boolean[] available;

	public SingleSequenceMining(String inputString, int minSupport, int itemGap, int seqGap) {
		this.setInputString(inputString);
		this.setMinSupport(minSupport);
		this.itemGap = itemGap;
		this.seqGap = seqGap;
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
				if (curFinalSupport >= this.minSupport) {
					this.FreqSeqsInMap.put(curFSObj.getFreqSeqInString(), curFSObj);
					curFSObj.setSupportNum(curFinalSupport);
					for (ResItemArrayPair resultPair : indexesForCurFS) {
						for (Integer deleteIndex : resultPair.index) {
							tempUnavailableIndexes.add(deleteIndex);
						}
					}
				}
				currentIndexInFS++;
				GenerateLocalFSMemory.DDReducer.maxUsedMemoryPerDevice = Toolbox
						.checkCurrentUsedMemory(GenerateLocalFSMemory.DDReducer.maxUsedMemoryPerDevice);
			}

			for (Integer ii : tempUnavailableIndexes) {
				this.available[ii] = false;
			}
			GenerateLocalFSMemory.DDReducer.maxUsedMemoryPerDevice = Toolbox
					.checkCurrentUsedMemory(GenerateLocalFSMemory.DDReducer.maxUsedMemoryPerDevice);
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
	public Set<String> findFreqSeqInOneString(int indexOfInput,HashSet<String> globalFrequentElements, Context context) {
		// first compute frequent sequences
		long start = System.currentTimeMillis();
		LocalParameterSpace localParameterSpace = new LocalParameterSpace(minSupport, itemGap, seqGap);
		PrefixSpanToolWithFirstReplicate pst = new PrefixSpanToolWithFirstReplicate(inputString,localParameterSpace);
		totalFrequentSeqs = pst.prefixSpanCalculate(globalFrequentElements);
		
		long timeCost = (System.currentTimeMillis() - start);
		context.getCounter(Counters.generateCompTime).increment(timeCost);
		GenerateLocalFSMemory.DDReducer.maxUsedMemoryPerDevice = Toolbox
				.checkCurrentUsedMemory(GenerateLocalFSMemory.DDReducer.maxUsedMemoryPerDevice);
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
		GenerateLocalFSMemory.DDReducer.maxUsedMemoryPerDevice = Toolbox
				.checkCurrentUsedMemory(GenerateLocalFSMemory.DDReducer.maxUsedMemoryPerDevice);
		this.updateSequenceLongToShort();
		return FreqSeqsInMap.keySet();
	}

	public HashMap<String, Integer> getFrequentSequences(){
		HashMap<String, Integer> frequentSequences = new HashMap<>();
		for(Map.Entry<String, FreqSequence> single: this.FreqSeqsInMap.entrySet()){
			frequentSequences.put(single.getKey(), single.getValue().getSupportNum());
		}
		return frequentSequences;
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
	}
}
