package top.core.lts.local.noviolation;

import top.core.lts.local.base.LocalFSDetectionBase;
import top.core.lts.local.base.SingleSequenceBase;
import top.core.lts.local.datamodel.HashMapList;
import top.distributed.lts.GenerateLocalFSMemoryStatistics;
import top.inputs.InputSequence;
import top.parameterspace.LocalParameterSpace;
import top.utils.Toolbox;

import java.util.*;
import java.util.Map.Entry;

public class LocalFSDetection extends LocalFSDetectionBase{
	protected HashMap<String, SinglePrefixClass> prefixSeqArray;
	protected HashMap<String, Integer> frequentSequences;

	public LocalFSDetection(LocalParameterSpace localParameterSpace, InputSequence inputSequence) {
		super(localParameterSpace, inputSequence);
		this.frequentSequences = new HashMap<String, Integer>();
	}

	public void Initialization(HashSet<String> globalFrequentElements) {
		inputSequence.removeInitSeqsItemIncludeGlobalInFreq(globalFrequentElements, localParameterSpace.getMinLocalSupport());
		this.prefixSeqArray = new HashMap<String, SinglePrefixClass>();
		for (String currentPrefix : inputSequence.getSingleItems()) {
			generateSeqencesWithPrefix(currentPrefix);
		}
	}

	/**
	 * generate set of sequences from original string
	 */
	protected void generateSeqencesWithPrefix(String prefix) {
		HashMapList<Integer, SingleSequenceBase> sequenceOrderbyLength =  new HashMapList<Integer, SingleSequenceBase>();
		int maxLengthOfString = prepareSinglePrefixClass(prefix, sequenceOrderbyLength);
		SinglePrefixClass tempPrefix = new SinglePrefixClass(prefix, sequenceOrderbyLength.getHashMapForCountResult(),
				maxLengthOfString);
		tempPrefix.adjustMaxLengthOfSeq(localParameterSpace.getMinLocalSupport());
		this.prefixSeqArray.put(prefix, tempPrefix);
		this.maxLength = Math.max(maxLength, tempPrefix.getMaxLengthOfSeq());
	}

	public HashMap<String, Integer> LocalFrequentSequenceMining(int indexOfInput) {
		// if the max length is larger than 0 and there are sequences in the
		// array
		while (this.maxLength > 1 && prefixSeqArray.size() > 0) {
			// traverse the arraylist in each prefix and see if there is
			// anything that can be eliminated
			HashSet<Integer> eliminateIndexes = new HashSet<Integer>();
			for (Entry<String, SinglePrefixClass> currentPrefix : this.prefixSeqArray.entrySet()) {
				currentPrefix.getValue().findFrequentSequences(inputSequence, eliminateIndexes,
						this.frequentSequences, this.maxLength, localParameterSpace);
			}
			GenerateLocalFSMemoryStatistics.DDReducer.maxUsedMemoryPerDevice = Toolbox
					.checkCurrentUsedMemory(GenerateLocalFSMemoryStatistics.DDReducer.maxUsedMemoryPerDevice);
			if (eliminateIndexes.size() > 0) {
				for (Integer i : eliminateIndexes) {
					inputSequence.getAvailable().set(i, false);
				}
				// then update each array, delete those strings that has nothing
				// available
				ArrayList<String> deletePrefix = new ArrayList<String>();
				for (Entry<String, SinglePrefixClass> currentPrefix : this.prefixSeqArray.entrySet()) {
					if (!currentPrefix.getValue().updateSequences(inputSequence.getAvailable(), localParameterSpace.getMinLocalSupport()))
						deletePrefix.add(currentPrefix.getKey());
					GenerateLocalFSMemoryStatistics.DDReducer.maxUsedMemoryPerDevice = Toolbox
							.checkCurrentUsedMemory(GenerateLocalFSMemoryStatistics.DDReducer.maxUsedMemoryPerDevice);
				}
				for (String str : deletePrefix) {
					this.prefixSeqArray.remove(str);
				}
			}
			GenerateLocalFSMemoryStatistics.DDReducer.maxUsedMemoryPerDevice = Toolbox
					.checkCurrentUsedMemory(GenerateLocalFSMemoryStatistics.DDReducer.maxUsedMemoryPerDevice);
			this.maxLength--;
		}
		return this.frequentSequences;
	}

	public HashMap<String, SinglePrefixClass> getPrefixSeqArray() {
		return prefixSeqArray;
	}

	public void setPrefixSeqArray(HashMap<String, SinglePrefixClass> prefixSeqArray) {
		this.prefixSeqArray = prefixSeqArray;
	}

	public static void main(String[] args) {
		String s = "A,B,A,B,A,B,A,B";
		int localSupport = 2;
		int itemGap = 0;
		int seqGap = 5;
		String[] globalFE = { "A", "B"};
		HashSet<String> globalFrequentElements = new HashSet(Arrays.asList(globalFE));
		InputSequence inputSequence = new InputSequence(s);
		LocalParameterSpace localParameterSpace = new LocalParameterSpace(localSupport, itemGap, seqGap);
		LocalFSDetection localFS = new LocalFSDetection(localParameterSpace, inputSequence);
		localFS.Initialization(globalFrequentElements);
		HashMap<String, Integer> freqSeqRes = localFS.LocalFrequentSequenceMining(0);
		System.out.println("Frequent Sequence: ");
		for (Map.Entry<String, Integer> entry : freqSeqRes.entrySet()) {
			System.out.println(entry.getKey() + "," + entry.getValue());
		}
	}
}
