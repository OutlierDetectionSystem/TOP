package top.core.lts.local.withviolation.nots;

import top.core.lts.local.base.SinglePrefixBase;
import top.core.lts.local.base.SingleSequenceBase;
import top.core.lts.local.datamodel.HashMapSet;
import top.inputs.InputSequence;
import top.parameterspace.LocalParameterSpace;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;

public class SinglePrefixWithViolation extends SinglePrefixBase{
	public SinglePrefixWithViolation(String prefix, HashMap<Integer, ArrayList<SingleSequenceBase>> originalSeqs,
									 int maxLength) {
		super(prefix, originalSeqs, maxLength);
	}

	public void findFrequentSequences(InputSequence inputSequence,
									  HashSet<Integer> eliminateIndexes, HashMap<String, Integer> frequentSequences, int curLength,
									  LocalParameterSpace localParameterSpace, HashSet<String> violationCandidatesCurrentLen,
									  int violationLocalSupport) {
		// if there is no longer sequences under this prefix, return
		if (this.maxLengthOfSeq < curLength)
			return;
		generateCandidateSequences(absoluteMaxLength,  curLength);
		// first generate possible frequent sequences and save subsequences
		HashMap<String, Integer> possibleFSWithFrequency = new HashMap<String, Integer>();
		Collections.sort(this.candidateSeqs);
		HashMapSet<String,Integer> usedIndexBySubs = new HashMapSet<String, Integer>();
		for (SingleSequenceBase singleSeq : this.candidateSeqs) {
			singleSeq.generateSubSequences(inputSequence, possibleFSWithFrequency, curLength, localParameterSpace, usedIndexBySubs);
		}
		// then for each possible fs, traverse the sequence to check if there is
		// enough support
		for (String tempFS : possibleFSWithFrequency.keySet()) {
			if (possibleFSWithFrequency.get(tempFS) >= localParameterSpace.getMinLocalSupport()) {
				HashSet<Integer> tempEliminates = new HashSet<Integer>();
				for (SingleSequenceBase singleSeq : this.candidateSeqs)
					singleSeq.containStringAsSub(tempFS, tempEliminates);

				eliminateIndexes.addAll(tempEliminates);
//				System.out.println("Frequent Sequence:" + tempFS);
				frequentSequences.put(tempFS, possibleFSWithFrequency.get(tempFS));
			} else if (possibleFSWithFrequency.get(tempFS) <= violationLocalSupport){
				violationCandidatesCurrentLen.add(tempFS);
			}
		}
	}
}
