package top.utils;

import java.util.ArrayList;

import top.core.datastructure.ItemPair;

public class generateCombine {
	public static void comb(Object[] a, int[] b, int start, int k, ArrayList<ArrayList<Integer>> finalRes, int gap,
							int previousIndex, ArrayList<ItemPair> itemPairList) {
		if (k == 1) {
			ArrayList<Integer> tempRes = new ArrayList<Integer>();
			for (int i = 0; i < b.length - 1; i++) {
				tempRes.add((Integer) a[b[i]]);
			}
			for (int j = start; j < a.length; j++) {
				if (itemPairList.get((Integer) a[j]).getIndex() - previousIndex - 1 > gap) {
					break;
				}
				ArrayList<Integer> finalTemp = new ArrayList<Integer>(tempRes);
				finalTemp.add((Integer) a[j]);
				finalRes.add(finalTemp);
			}

		} else {
			for (int i = start; i <= a.length - k; i++) {
				int curIndex = b.length - k;
				b[curIndex] = i;
				if (itemPairList.get((Integer) a[b[curIndex]]).getIndex() - previousIndex - 1 > gap) {
					break;
				}
				comb(a, b, i + 1, k - 1, finalRes, gap, itemPairList.get((Integer) a[b[curIndex]]).getIndex(), itemPairList);
			}
		}
	}

	public static void main(String[] args) {
		ArrayList<Integer> originalList = new ArrayList<Integer>();
		originalList.add(1);
		originalList.add(2);
		originalList.add(3);
		originalList.add(4);
		originalList.add(5);
		int[] b = new int[3];
		ArrayList<ArrayList<Integer>> finalRes = new ArrayList<ArrayList<Integer>>();
//		testCombine.comb(originalList.toArray(), b, 0, 3, finalRes, 0, 0);
		// testCombine.comb(originalList.toArray(), b, 5, 3, 3, finalRes, 0);
		for (ArrayList<Integer> finalTemp : finalRes) {
			for (Integer str : finalTemp) {
				System.out.print(str + ",");
			}
			System.out.println();
		}
	}
}
