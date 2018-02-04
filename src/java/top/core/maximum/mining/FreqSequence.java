package top.core.maximum.mining;

import java.util.ArrayList;
import java.util.HashSet;

public class FreqSequence implements Comparable {
	private ArrayList<ResItemArrayPair> itemPairList;

	private boolean isvalid = true;

	private int supportNum = 0;

	private ArrayList<Boolean> validIndexes;

	// only generate if used
	private ArrayList<ArrayList<Integer>> indexesForSequence = new ArrayList<ArrayList<Integer>>();

	public ArrayList<ArrayList<Integer>> getIndexesForSequence() {
		return indexesForSequence;
	}

	public boolean isEmptyIndexesForSequence() {
		return indexesForSequence.isEmpty();
	}

	public void generateIndexesForSequence() {
		for (int i = 0; i < supportNum; i++) {
			ArrayList<Integer> tempList = new ArrayList<Integer>();
			for (ResItemArrayPair resPair : itemPairList) {
				tempList.add(resPair.index.get(i));
			}
			indexesForSequence.add(tempList);
		}
	}

	public ArrayList<ResItemArrayPair> getItemPairList() {
		return itemPairList;
	}

	public void setItemPairList(ArrayList<ResItemArrayPair> itemPairList) {
		this.itemPairList = itemPairList;
	}

	public FreqSequence() {
		itemPairList = new ArrayList<ResItemArrayPair>();
	}

	public FreqSequence(ArrayList<ResItemArrayPair> itemPairList) {
		this.itemPairList = itemPairList;
	}

	public void addItemToSequence(ResItemArrayPair newLetter) {
		itemPairList.add(newLetter);
	}

	/**
	 * before doing pruning (that is , if ABC is frequent, then AB AC BC should
	 * remove the support which appears the same position with ABC) We have to
	 * generate total number of support for each frequent sequence For each
	 * support, we set up an index indicating whether it is valid or not
	 */
	public void generateSupportNumAndInitIndexes() {
		this.supportNum = this.itemPairList.get(0).index.size();
		this.validIndexes = new ArrayList<Boolean>();
		for (int i = 0; i < supportNum; i++) {
			this.validIndexes.add(true);
		}
	}
	public void generateSupportNum(){
		this.supportNum = this.itemPairList.get(0).index.size();
	}

	public int getItemNumInFreqSeq() {
		return this.itemPairList.size();
	}

	public String getFreqSeqInString() {
		String str = "";
		for (ResItemArrayPair curItem : this.itemPairList) {
			str += curItem.item + ",";
		}
		if (str.length() > 0)
			str = str.substring(0, str.length() - 1);
		return str;
	}

	/**
	 * copy a frequent sequence
	 *
	 * @return
	 */
	public FreqSequence copyFreqSeqence(ArrayList<Boolean> ifHasNewItemInPrevSeq,
                                        ArrayList<Integer> whichOneUsedInPrevFS, ArrayList<Integer[]> lastIndexes) {
		FreqSequence copySeq = new FreqSequence();

		for (int i = 0; i < this.itemPairList.size() - 1; i++) {
			ResItemArrayPair itemPair = this.itemPairList.get(i);
			ArrayList<Integer> deleteItemSets = new ArrayList<>();
			ResItemArrayPair tempItemPair;
			String tempLetter = itemPair.item;
			ArrayList<Integer> tempIndexes = new ArrayList<Integer>(itemPair.index);
			for (int j = 0; j < ifHasNewItemInPrevSeq.size(); j++) {
				if (!ifHasNewItemInPrevSeq.get(j))
					deleteItemSets.add(tempIndexes.get(j));
			}
			tempIndexes.removeAll(deleteItemSets);
			tempItemPair = new ResItemArrayPair(tempLetter, tempIndexes);
			copySeq.itemPairList.add(tempItemPair);
		}
		ResItemArrayPair itemPair = this.itemPairList.get(this.itemPairList.size() - 1);
		ArrayList<Integer> deleteItemSets = new ArrayList<>();
		ResItemArrayPair tempItemPair;
		String tempLetter = itemPair.item;
		ArrayList<Integer> tempIndexes = new ArrayList<Integer>(itemPair.index);
		for (int i = 0; i < ifHasNewItemInPrevSeq.size(); i++) {
			if (!ifHasNewItemInPrevSeq.get(i))
				deleteItemSets.add(tempIndexes.get(i));
			else if(whichOneUsedInPrevFS.get(i)!=0){
				tempIndexes.set(i, lastIndexes.get(i)[whichOneUsedInPrevFS.get(i)]);
			}
		}
		tempIndexes.removeAll(deleteItemSets);
		tempItemPair = new ResItemArrayPair(tempLetter, tempIndexes);
		copySeq.itemPairList.add(tempItemPair);

		return copySeq;
	}

	/**
	 * If ABABAB generates ABAB and ABAB, it must shared one occurrence of AB, remain the first one, remove the second one from the final result list
	 */
	public void filterSharedOccurrences(ArrayList<Integer[]> tempFS, ArrayList<Sequence> tempSeqList){
		ArrayList<Integer []> removeTempFS = new ArrayList<>();
		ArrayList<Sequence> removeTempSeqList =  new ArrayList<>();
		ArrayList<ResItemArrayPair> newItemPairList = new ArrayList<>();
		HashSet<Integer> usedIndexes = new HashSet<>();
		// add first occurrence into the result list (no conflicts)
		for(int i = 0; i< itemPairList.size(); i++){
			ResItemArrayPair newRes = new ResItemArrayPair(itemPairList.get(i).item);
			newRes.addToIndex(itemPairList.get(i).index.get(0));
			newItemPairList.add(newRes);
			usedIndexes.add(itemPairList.get(i).index.get(0));
		}
		// traverse other occurrences
		for(int i = 1; i< itemPairList.get(0).index.size(); i++){
			// check if used?
			boolean used = false;
			for(int j = 0; j< itemPairList.size(); j++){
				int curIndex = itemPairList.get(j).index.get(i);
				if(usedIndexes.contains(curIndex)){
					used = true;
					break;
				}
			}
			if(!used){
				for(int j = 0; j< newItemPairList.size(); j++){
					newItemPairList.get(j).addToIndex(itemPairList.get(j).index.get(i));
					usedIndexes.add(itemPairList.get(j).index.get(i));
				}
			}else {
				removeTempFS.add(tempFS.get(i));
				removeTempSeqList.add(tempSeqList.get(i));
			}
		}
		this.itemPairList = newItemPairList;
		tempFS.removeAll(removeTempFS);
		tempSeqList.removeAll(removeTempSeqList);
	}

	public boolean isIsvalid() {
		return isvalid;
	}

	public void setIsvalid(boolean isvalid) {
		this.isvalid = isvalid;
	}

	public int getSupportNum() {
		return supportNum;
	}

	public void setSupportNum(int supportNum) {
		this.supportNum = supportNum;
	}

	public ArrayList<Boolean> getValidIndexes() {
		return validIndexes;
	}

	public void setValidIndexes(ArrayList<Boolean> validIndexes) {
		this.validIndexes = validIndexes;
	}

	@Override
	public int compareTo(Object o) {
		// TODO Auto-generated method stub
		if (this.getItemNumInFreqSeq() > ((FreqSequence) o).getItemNumInFreqSeq())
			return -1;
		else if (this.getItemNumInFreqSeq() < ((FreqSequence) o).getItemNumInFreqSeq())
			return 1;
		else
			return 0;
	}

	public void setInvalid() {
		this.isvalid = false;
	}

	public boolean isInvalid(int minSupport) {
		if (isvalid == false)
			return true;
		int count = 0;
		for (Boolean i : this.validIndexes)
			if (i == true)
				count++;
		if (count < minSupport) {
			isvalid = false;
			return true;
		}
		return false;
	}

	public boolean isSubArray(ArrayList<Integer> longArray, ArrayList<Integer> shortArray) {
		for (int shortItem : shortArray) {
			int startPos = longArray.indexOf(shortItem);
			if (startPos < 0)
				return false;
		}
		return true;
	}

	public void setPartInValid(FreqSequence longSeq) {
		// for each long sequence, set the part with the same index invalid
		// first find which position to check, eg: longSeq: ABC currentSeq: AC,
		// return {0,2}
		if (longSeq.isEmptyIndexesForSequence())
			longSeq.generateIndexesForSequence();
		if (this.isEmptyIndexesForSequence())
			this.generateIndexesForSequence();
		for (ArrayList<Integer> longArrayForIndexes : longSeq.getIndexesForSequence()) {
			for (int i = 0; i < validIndexes.size(); i++) {
				if (validIndexes.get(i) == true && isSubArray(longArrayForIndexes, this.indexesForSequence.get(i)))
					validIndexes.set(i, false);
			}
		}

	}
}
