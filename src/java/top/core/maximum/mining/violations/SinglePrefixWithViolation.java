package top.core.maximum.mining.violations;

import top.core.lts.local.datamodel.HashMapSet;
import top.inputs.InputSequence;
import top.parameterspace.LocalParameterSpace;

import java.util.*;

public class SinglePrefixWithViolation{
	protected String prefix;
	protected HashMap<Integer, ArrayList<SingleSequence>> originalSeqsOrderbyLength;
	protected int maxLengthOfSeq = -1;
	protected ArrayList<SingleSequence> candidateSeqs;
	protected int candidateLengthTo = Integer.MAX_VALUE;
	protected int absoluteMaxLength = -1;
	public SinglePrefixWithViolation(String prefix, HashMap<Integer, ArrayList<SingleSequence>> originalSeqs,
                                     int maxLength) {
		this.prefix = prefix;
		this.originalSeqsOrderbyLength = originalSeqs;
		this.maxLengthOfSeq = maxLength;
		this.absoluteMaxLength = maxLength;
	}

	public void generateCandidateSequences(int absoluteMaxLength, int curLength){
		if (this.candidateSeqs == null) {
			this.candidateSeqs = new ArrayList<SingleSequence>();
			for (int i = absoluteMaxLength; i >= curLength; i--) {
				if (originalSeqsOrderbyLength.containsKey(i)) {
					this.candidateSeqs.addAll(originalSeqsOrderbyLength.get(i));
				}
			}
			this.candidateLengthTo = curLength;
		} else {
			for (int i = this.candidateLengthTo - 1; i >= curLength; i--) {
				if (originalSeqsOrderbyLength.containsKey(i)) {
					this.candidateSeqs.addAll(originalSeqsOrderbyLength.get(i));
				}
				this.candidateLengthTo = curLength;
			}
		}
	}
	public void adjustMaxLengthOfSeq(int minSupport) {
		ArrayList<Integer> allLengthArray = new ArrayList<Integer>(originalSeqsOrderbyLength.keySet());
		Collections.sort(allLengthArray, Collections.reverseOrder());
		int count = 0;
		for (Integer length : allLengthArray) {
			count += originalSeqsOrderbyLength.get(length).size();
			if (count >= minSupport) {
				this.maxLengthOfSeq = length;
				break;
			}
		}
	}

	public boolean updateSequences(ArrayList<Boolean> availability, int localSupport) {
		int maxLength = -1;
		HashMap<Integer, ArrayList<SingleSequence>> remainSeqsInMap = new HashMap<Integer, ArrayList<SingleSequence>>();
		int currentSizeSeqs = 0;
		for (Map.Entry<Integer, ArrayList<SingleSequence>> entry : this.originalSeqsOrderbyLength.entrySet()) {
			ArrayList<SingleSequence> remainSeqs = new ArrayList<SingleSequence>();
			for (SingleSequence seq : entry.getValue()) {
				if (seq.checkAvailablity(availability)) {
					remainSeqs.add(seq);
					maxLength = Math.max(maxLength, seq.getItemPairList().size());
				} else {
					if(this.candidateSeqs !=null)
						this.candidateSeqs.remove(seq);
				}
			} // end for entry.getvalue()
			if (remainSeqs.size() > 0) {
				remainSeqsInMap.put(entry.getKey(), remainSeqs);
				currentSizeSeqs += remainSeqs.size();
			}
		}
		this.originalSeqsOrderbyLength = remainSeqsInMap;
		this.maxLengthOfSeq = Math.min(this.maxLengthOfSeq, maxLength);
		if (currentSizeSeqs >= localSupport)
			return true;
		else
			return false;

	}
	public void findFrequentSequences(InputSequence inputSequence,
									  int curLength,
									  LocalParameterSpace localParameterSpace,
									  HashMap<String, ArrayList<String>> violationCandidatesCurrentLen,
									  int violationLocalSupport) {
		// if there is no longer sequences under this prefix, return
		if (this.maxLengthOfSeq < curLength)
			return;
		generateCandidateSequences(absoluteMaxLength, curLength);
		// first generate possible frequent sequences and save subsequences
		HashMap<String, Integer> possibleFSWithFrequency = new HashMap<String, Integer>();
		Collections.sort(this.candidateSeqs);
		HashMapSet<String,Integer> usedIndexBySubs = new HashMapSet<String, Integer>();
		for (SingleSequence singleSeq : this.candidateSeqs) {
			singleSeq.generateSubSequences(inputSequence, possibleFSWithFrequency, curLength, localParameterSpace, usedIndexBySubs);
		}
		// then for each possible fs, traverse the sequence to check if there is
		// enough support
		for (String tempFS : possibleFSWithFrequency.keySet()) {
			if (possibleFSWithFrequency.get(tempFS) <= violationLocalSupport){
				// find occurrences (absolute occurrences)
				ArrayList<String> occurrencesOfViolation = new ArrayList<String>();
				for(SingleSequence singleSeq : this.candidateSeqs){
					String occurrence = singleSeq.containStringAsSub(tempFS, inputSequence.getOriginalIndexes());
					if(occurrence!=null)
						occurrencesOfViolation.add(occurrence);
				}
				violationCandidatesCurrentLen.put(tempFS, occurrencesOfViolation);
//				System.out.println(tempFS);
			}
		}
	}

	public int getMaxLengthOfSeq() {
		return maxLengthOfSeq;
	}

	public void setMaxLengthOfSeq(int maxLengthOfSeq) {
		this.maxLengthOfSeq = maxLengthOfSeq;
	}
}
