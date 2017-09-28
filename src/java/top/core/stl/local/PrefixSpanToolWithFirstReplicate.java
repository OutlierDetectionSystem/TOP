package top.core.stl.local;

import top.core.datastructure.FreqSequence;
import top.core.datastructure.ItemPair;
import top.core.datastructure.ResItemArrayPair;
import top.core.datastructure.Sequence;
import top.parameterspace.LocalParameterSpace;

import java.util.*;

public class PrefixSpanToolWithFirstReplicate {
	// orginal sequence data, used to generate totalSeqences
	private String InputTimeSequence;
	// orginal index (since we truncate the string, we have to save the previous
	// index)
	private ArrayList<Integer> originalIndexes;
	// mininal support
	private LocalParameterSpace localParameterSpace;
	// save all frequent sequences
	private ArrayList<FreqSequence> totalFrequentSeqs = new ArrayList<FreqSequence>();
	// All single items, used for enumerate
	private ArrayList<String> singleItems;
	private int maxLengthOriginal = 0;

	public PrefixSpanToolWithFirstReplicate(String originalSeq, LocalParameterSpace localParameterSpace) {
		this.InputTimeSequence = originalSeq;
		this.localParameterSpace = localParameterSpace;
	}

	/**
	 * Remove single item that does not satisfy minSupport
	 */
	private void removeInitSeqsItemAndGlobalInFreq(HashSet<String> globalFrequentElements) {
		HashMap<String, Integer> itemMap = new HashMap<>();
		this.singleItems = new ArrayList<>();
		String[] subItems = InputTimeSequence.split(",");
		this.maxLengthOriginal = subItems.length;
		for (String temp : subItems) {
			if (!itemMap.containsKey(temp))
				itemMap.put(temp, 1);
			else
				itemMap.put(temp, itemMap.get(temp) + 1);
		}

		for (Map.Entry entry : itemMap.entrySet()) {
			String key = (String) entry.getKey();
			int count = (int) entry.getValue();

			if (count >= localParameterSpace.getMinLocalSupport()  && globalFrequentElements.contains(key)) {
				singleItems.add(key);
			}
		}
		// to do : modify input string: remove all infrequent items
		String newSequence = "";
		this.originalIndexes = new ArrayList<Integer>();

		for (int i = 0; i < subItems.length; i++) {
			if (globalFrequentElements.contains(subItems[i]) && singleItems.contains(subItems[i])) {
				newSequence += subItems[i] + ",";
				originalIndexes.add(i);
			}
		}
		if (newSequence.length() > 0)
			newSequence = newSequence.substring(0, newSequence.length() - 1);
		this.InputTimeSequence = newSequence;
		// System.out.println("New String: " + this.InputTimeSequence);
	}

	/**
	 * generate set of sequences from original string
	 */
	private ArrayList<Sequence> generateSeqencesWithPrefix(String prefix) {
		ArrayList<Sequence> sequenceArray = new ArrayList<Sequence>();
		ArrayList<ItemPair> previousSeq = new ArrayList<ItemPair>();

		String[] subItems = InputTimeSequence.split(",");
		int startIndex = 0;
		// first search for starting index
		for (int i = 0; i < subItems.length; i++) {
			if (subItems[i].equals(prefix)) {
				startIndex = i;
				break;
			}
		}

		for (int i = startIndex; i < subItems.length; i++) {
			if (subItems[i].equals(prefix) && previousSeq.size() != 0) {
				int tempInc = i;
				while(previousSeq.size() < localParameterSpace.getSeqGap() + 2 &&
						tempInc < subItems.length){
					previousSeq.add(new ItemPair(subItems[tempInc], this.originalIndexes.get(tempInc)));
					tempInc++;
				}
				// save previous sequence to the final sequence array
				sequenceArray.add(new Sequence((ArrayList<ItemPair>) previousSeq.clone()));
				previousSeq.clear();
			}
			previousSeq.add(new ItemPair(subItems[i], this.originalIndexes.get(i)));
		}
		if (!previousSeq.isEmpty()) {
			sequenceArray.add(new Sequence((ArrayList<ItemPair>) previousSeq.clone()));
		}
		return sequenceArray;
	}


	private boolean findMatchSequence(String s, ArrayList<Sequence> seqList, ArrayList<Integer[]> tempFS,
			ArrayList<Boolean> ifHasNewItemInPrevSeq, ArrayList<Integer[]> lastIndexes, ArrayList<Integer> firstIndexes,
			ArrayList<Integer> whichOneUsedInPreviousTempFS) {
		boolean isLarge = false;
		int count = 0;

		for (int i = 0; i < seqList.size(); i++) {
			boolean containsCurPrefix = false;
			for (int j = 0; j <= localParameterSpace.getItemGap(); j++) {
				if (lastIndexes.get(i)[j] != -1 && seqList.get(i).strIsContained(s, tempFS, true, lastIndexes.get(i)[j],
						firstIndexes.get(i), localParameterSpace.getItemGap(), localParameterSpace.getSeqGap())) {
					whichOneUsedInPreviousTempFS.add(j);
					containsCurPrefix = true;
					break;
				}
			}
			if (containsCurPrefix) {
				count++;
				ifHasNewItemInPrevSeq.add(true);
			} else {
				ifHasNewItemInPrevSeq.add(false);
				whichOneUsedInPreviousTempFS.add(-1);
			}
		}

		if (count >= localParameterSpace.getMinLocalSupport()) {
			isLarge = true;
		}

		return isLarge;
	}

	private boolean findMatchSequence(String s, ArrayList<Sequence> seqList, ArrayList<Integer[]> tempFS) {
		boolean isLarge = false;
		int count = 0;
//		Collections.sort(seqList);
		for (Sequence seq : seqList) {
			if (seq.strIsContained(s, tempFS, true, localParameterSpace.getItemGap())) {
				count++;
			}
		}

		if (count >= localParameterSpace.getMinLocalSupport()) {
			isLarge = true;
		}

		return isLarge;
	}

	public ArrayList<Integer> extractAllFirstIndexes(ArrayList<Integer[]> tempFS) {
		ArrayList<Integer> extractedIndexes = new ArrayList<Integer>();
		for (int i = 0; i < tempFS.size(); i++) {
			extractedIndexes.add(tempFS.get(i)[0]);
		}
		return extractedIndexes;
	}

	/**
	 * frequent pattern mining for each single item, generate sequence sets from
	 * the original string then mining each sequence separately
	 */
	private void frequentSequenceMining() {
		for (String currentPrefix : singleItems) {
			ArrayList<Sequence> currentSeqArray = generateSeqencesWithPrefix(currentPrefix);
			// printSeqList(currentSeqArray);
			ArrayList<Integer[]> tempFS = new ArrayList<Integer[]>();
			boolean isLargerThanSup = findMatchSequence(currentPrefix, currentSeqArray, tempFS);
			if (isLargerThanSup) {
				// add the new frequent sequence and indexes to the final list
				FreqSequence newFS = new FreqSequence();
				ResItemArrayPair newPair = new ResItemArrayPair(currentPrefix);
				newPair.setIndexes(extractAllFirstIndexes(tempFS));
				newFS.addItemToSequence(newPair);
				totalFrequentSeqs.add(newFS);

				// truncate current sequences and generate new available
				// sequence list
				Sequence tempSeq;
				HashSet<String> newSingleItems = new HashSet<String>();
				ArrayList<Sequence> tempSeqList = new ArrayList<>();
				for (Sequence s2 : currentSeqArray) {
					// check if the sequence contains currentPrefix
					if (s2.strIsContained(currentPrefix, tempFS, false, localParameterSpace.getItemGap())) {
						tempSeq = s2.extractItem(currentPrefix);
						tempSeqList.add(tempSeq);
						// newSingleItems.addAll(tempSeq.getAllItemsInString());
					}
				}
				newSingleItems.addAll(singleItems);
				recursiveSearchSeqs(newFS, tempSeqList, newSingleItems, tempFS);
			}
		}
	}

	/**
	 * recursively search for sequences
	 * 
	 * @param beforeSeq
	 *            previous generate frequent sequence
	 * @param afterSeqList
	 *            the sequences after filtering
	 * @param newSingleItems
	 *            the sequences without the first prefix
	 * @param lastIndexes
	 *            the indexes of previous item, used for gap...if(current
	 *            -previous index) < gap, then stop
	 * 
	 */
	private void recursiveSearchSeqs(FreqSequence beforeSeq, ArrayList<Sequence> afterSeqList,
			HashSet<String> newSingleItems, ArrayList<Integer[]> lastIndexes) {
		for (String s : newSingleItems) {
			ArrayList<Integer[]> tempFS = new ArrayList<Integer[]>();
			ArrayList<Integer> whichOneUsedInPrevFS = new ArrayList<Integer>();
			ArrayList<Boolean> ifHasNewItemInPrevSeq = new ArrayList<Boolean>();
			boolean isLargerThanSup = findMatchSequence(s, afterSeqList, tempFS, ifHasNewItemInPrevSeq, lastIndexes,
					beforeSeq.getItemPairList().get(0).index, whichOneUsedInPrevFS);
			if (isLargerThanSup) {
				// add the new frequent sequence and indexes to the final list
				FreqSequence newFS = beforeSeq.copyFreqSeqence(ifHasNewItemInPrevSeq, whichOneUsedInPrevFS,
						lastIndexes);
				ResItemArrayPair newPair = new ResItemArrayPair(s);
				newPair.setIndexes(extractAllFirstIndexes(tempFS));
				newFS.addItemToSequence(newPair);
				// truncate current sequences and generate new available
				// sequence list
				Sequence tempSeq;
				ArrayList<Sequence> tempSeqList = new ArrayList<>();

				for (int i = 0; i < afterSeqList.size(); i++) {
					// check if the sequence contains currentPrefix
					for (int j = 0; j <= localParameterSpace.getItemGap(); j++) {
						if (lastIndexes.get(i)[j] != -1
								&& afterSeqList.get(i).strIsContained(s, tempFS, false, lastIndexes.get(i)[j],
								beforeSeq.getItemPairList().get(0).index.get(i), localParameterSpace.getItemGap(),
								localParameterSpace.getSeqGap())) {
							tempSeq = afterSeqList.get(i).extractItem(s);
							tempSeqList.add(tempSeq);
							// newnewSingleItems.addAll(tempSeq.getAllItemsInString());
							break;
						}
					}
				}
				newFS.filterSharedOccurrences(tempFS, tempSeqList);
				if(newFS.getItemPairList().get(0).index.size() >= localParameterSpace.getMinLocalSupport()) {
					totalFrequentSeqs.add(newFS);
					recursiveSearchSeqs(newFS, tempSeqList, newSingleItems, tempFS);
				}
			}
		}
	}

	/**
	 * frequent sequence mining
	 */
	public ArrayList<FreqSequence> prefixSpanCalculate(HashSet<String> globalFrequentElements) {
		removeInitSeqsItemAndGlobalInFreq(globalFrequentElements);
		if (this.originalIndexes.size() > 0)
			frequentSequenceMining();
		return totalFrequentSeqs;
	}

	/**
	 * print final results
	 */
	private void printTotalFreSeqs() {
		System.out.println("Frequent Sequence Results");

		ArrayList<FreqSequence> seqList;
		HashMap<String, ArrayList<FreqSequence>> seqMap = new HashMap<>();
		for (String s : singleItems) {
			seqList = new ArrayList<>();
			for (FreqSequence seq : totalFrequentSeqs) {
				if (seq.getItemPairList().get(0).item.equals(s)) {
					seqList.add(seq);
				}
			}
			seqMap.put(s, seqList);
		}

		int count = 0;
		for (String s : singleItems) {
			count = 0;
			System.out.println();
			System.out.println();

			seqList = (ArrayList<FreqSequence>) seqMap.get(s);
			for (FreqSequence tempSeq : seqList) {
				count++;
				System.out.print("<");
				for (ResItemArrayPair itemPair : tempSeq.getItemPairList()) {
					System.out.print(itemPair.item + ", ");
					// generate index
					System.out.print("[");
					for (Integer indexForLetter : itemPair.index)
						System.out.print(indexForLetter + " ");
					System.out.print("] ");
				}
				System.out.print(">, ");

				if (count == 5) {
					count = 0;
					System.out.println();
				}
			}
		}
	}

	public static void main(String[] args) {
		// String s =
		// "A,B,C,D,1,2,3,4,5,6,7,8,A,B,C,D,11,12,35,36,57,89,100,D,C,B,A,22,23,24,26,27,28,29,30,D,C,B,A,31,32,33,43,54,67,87,A,B,D,C";
		String s = "A,A,A,A,B,B,C,C,A,A,A,A,B,B,C,B";
		// String s = "a,b,c,a,b,c,a,c,b,a,c,d";
		// String s = "a,b,b,b,c,z,z,z,z,a,b,c";
		// String s = "2,1,1,1,1,1";
		// String s =
		// "21,22,23,24,25,26,27,28,24,14,27,29,30,27,31,30,27,31,30,27,31,30,27,31,30,32,27,28,24,14,33,34,0,24,35,36,14,37,38,14,14,39,40,41,42,43,44,45,46,47,26,32,27,28,24,26,32,26,32,26,32,40,26,32,48,14,14,49,50,51,52,48,14,14,49,50,53,54,42,55,56,57,58,59,60,61,62,63,26,32,26,32,26,32,26,32,26,32,64,23,24,25,27,28,24,14,27,29,30,27,31,30,27,31,30,27,31,30,27,31,30,26,32,27,28,24,14,27,28,24,14,27,29,30,27,31,30,27,31,30,33,34,0,24,35,36,14,37,38,14,40,41,14,39,47,42,43,44,45,46,26,32,27,28,24,26,32,26,32,26,32,40,26,32,48,14,14,49,50,51,52,48,14,14,49,50,53,54,42,55,56,57,58,59,63,60,61,62,26,32,26,32,26,32,26,32,26,32,64,26,23,24,25,27,28,24,14,27,29,30,27,31,30,27,31,30,27,31,30,32,27,31,30,33,34,0,24,35,36,14,37,38,14,40,41,65,14,66,42,43,67,67,44,45,46,47,68,69,70,26,32,71,72,73,71,72,73,0,74,75,76,77,78,79,78,79,80,26,32,81,69,4,11,76,21,22,26,32,68,21,22,68,21,22,77,78,79,78,79,80,26,32,68,21,22,68,40,21,22,77,78,79,78,79,80,26,32,68,21,22,48,14,14,82,50,51,52,48,14,14,82,50,53,54,42,55,56,57,58,59,68,63,21,22,60,61,62,68,77,78,79,78,79,78,79,80,26,32,21,22,68,21,22,68,21,22,77,78,79,78,79,80,26,32,68,21,22,68,21,77,78,79,78,79,80,26,32,22,68,21,22,68,21,22,77,78,79,78,79,80,26,32,68,21,22,68,21,22,77,78,79,78,79,80,26,32,64,27,28,24,14,68,83,84,69,77,78,79,80,26,32,4,11,21,22";

		int localSupport = 2;
		int itemGap = 0;
		int seqGap = 10;
		LocalParameterSpace localParameterSpace = new LocalParameterSpace(localSupport, itemGap, seqGap);
		PrefixSpanToolWithFirstReplicate pst = new PrefixSpanToolWithFirstReplicate(s, localParameterSpace);
		String[] globalFE = { "A", "B", "C", "D" };
		HashSet<String> globalFrequentElements = new HashSet(Arrays.asList(globalFE));
		pst.prefixSpanCalculate(globalFrequentElements);
		pst.printTotalFreSeqs();
	}
}
