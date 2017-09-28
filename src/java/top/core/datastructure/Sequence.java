package top.core.datastructure;

import java.util.ArrayList;
import java.util.HashSet;

public class Sequence implements Comparable{
	// a sequence is an array of itempairs
	private ArrayList<ItemPair> itemPairList;

	public Sequence() {
		itemPairList = new ArrayList<ItemPair>();
	}

	public Sequence(ArrayList<ItemPair> itemPairList) {

		this.itemPairList = itemPairList;
	}

	public ArrayList<ItemPair> getItemPairList() {
		return itemPairList;
	}

	public void setItemPairList(ArrayList<ItemPair> itemPairList) {
		this.itemPairList = (ArrayList<ItemPair>) itemPairList.clone();
	}
	public HashSet<String> getAllItemsInString(){
		HashSet<String> returnList = new HashSet<String>();
		for(ItemPair temp: itemPairList)
			returnList.add(temp.getItem());
		return returnList;
	}
	/**
	 * check if the sequence contains the given string
	 *
	 *            given string
	 * @return
	 */
	public boolean strIsContained(String checkStr, ArrayList<Integer[]> tempFS, boolean addToFS, int itemGap) {
		boolean isContained = false;
		ArrayList<Integer> tempIndexes = new ArrayList<Integer>();

		for (ItemPair itemPair : itemPairList) {
			// isContained = false;
			if (checkStr.equals(itemPair.getItem())) {
				isContained = true;
				if (addToFS) {
					Integer[] indexesOfLast = new Integer[itemGap + 1];
					for(int i = 0; i<= itemGap; i++){
						indexesOfLast[i] = -1;
					}
					indexesOfLast[0] = itemPair.getIndex();
					tempFS.add(indexesOfLast);
				}
				break;
			}
		}
		return isContained;
	}

	/**
	 * check if the sequence contains the given string
	 * 
	 * @param checkStr
	 *            given string
	 * @return
	 */
	public boolean strIsContained(String checkStr, ArrayList<Integer[]> tempFS, boolean addToFS, int previousIndex,
			int firstIndex, int itemGap, int seqGap) {
		boolean isContained = false;
		ArrayList<Integer> tempIndexes = new ArrayList<Integer>();
		Integer [] indexesOfMatches = new Integer[itemGap+1];
		for(int i = 0; i<= itemGap; i++){
			indexesOfMatches[i] = -1;
		}
		int count = 0;
		for (ItemPair itemPair : itemPairList) {
//			isContained = false;
			if (itemPair.getIndex() - previousIndex > itemGap + 1 || itemPair.getIndex() - firstIndex > seqGap + 1)
				break;
			if (checkStr.equals(itemPair.getItem())) {
				isContained = true;
				indexesOfMatches[count] = itemPair.getIndex();
				count++;
			}
		}
		if (isContained && addToFS)
			tempFS.add(indexesOfMatches);
		return isContained;
	}

	/**
	 * the new sequence after string "s"
	 * 
	 * @param s
	 *            extract items after string s
	 */
	public Sequence extractItem(String s) {
		Sequence extractSeq = this.copySeqence();
		ArrayList<ItemPair> deleteItemSets = new ArrayList<>();
		ArrayList<String> tempItems = new ArrayList<>();

		for (int k = 0; k < extractSeq.itemPairList.size(); k++) {
			ItemPair itemPair = extractSeq.itemPairList.get(k);
			if (itemPair.getItem().equals(s)) {
				extractSeq.itemPairList.remove(k);
				break;
			} else {
				deleteItemSets.add(itemPair);
			}
		}
		extractSeq.itemPairList.removeAll(deleteItemSets);
		return extractSeq;
	}

	/**
	 * copy a sequence
	 * 
	 * @return
	 */
	public Sequence copySeqence() {
		Sequence copySeq = new Sequence();
		ItemPair tempItemPair;

		for (ItemPair itemPair : this.itemPairList) {
			tempItemPair = new ItemPair(itemPair.getItem(), itemPair.getIndex());
			copySeq.getItemPairList().add(tempItemPair);
		}

		return copySeq;
	}

	@Override
	public int compareTo(Object o) {
		Sequence other  = (Sequence) o;
		if(this.getItemPairList().get(0).getIndex() > other.getItemPairList().get(0).getIndex())
			return 1;
		else if(this.getItemPairList().get(0).getIndex() < other.getItemPairList().get(0).getIndex())
			return -1;
		else
			return 0;
	}
}
