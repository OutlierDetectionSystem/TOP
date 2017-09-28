package top.distributed.lts.explanation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;

public class ViolationOccurrences implements Comparable {
	private String violationSeq;
	private String[] occurrences;
	private HashMap<String, String> occurrenceMapOriginalSeq;
	private int violationLength;

	public ViolationOccurrences(String violationSeq, String[] occurrences, int violationLength) {
		this.setViolationSeq(violationSeq);
		this.setOccurrences(occurrences);
		this.setOccurrenceMapOriginalSeq(new HashMap<String, String>());
		this.setViolationLength(violationLength);
	}

	public void filterSubViolations(ViolationOccurrences superViolation) {
		if (strArrayContains(superViolation.getViolationSeq(), this.getViolationSeq())) {
			HashMap<String, String> filteredOccurrenceMapOriginalSeq = new HashMap<String, String>();
			for (String oneOccurrence : this.occurrenceMapOriginalSeq.keySet()) {
				boolean validViolation = true;
				for (String superOccurrence : superViolation.getOccurrenceMapOriginalSeq().keySet()) {
					if (strArrayContains(superOccurrence, oneOccurrence)) {
						validViolation = false;
						break;
					}
				}
				if (validViolation) {
					filteredOccurrenceMapOriginalSeq.put(oneOccurrence,
							this.occurrenceMapOriginalSeq.get(oneOccurrence));
				}
			} // end for
			this.occurrenceMapOriginalSeq = filteredOccurrenceMapOriginalSeq;
		}
	}

	public boolean strArrayContains(String str1, String str2) {
		ArrayList<String> strList1 = new ArrayList<String>(Arrays.asList(str1.split(",")));
		ArrayList<String> strList2 = new ArrayList<String>(Arrays.asList(str2.split(",")));
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

	public String getViolationSeq() {
		return violationSeq;
	}

	public void setViolationSeq(String violationSeq) {
		this.violationSeq = violationSeq;
	}

	public String[] getOccurrences() {
		return occurrences;
	}

	public void setOccurrences(String[] occurrences) {
		this.occurrences = occurrences;
	}

	public HashMap<String, String> getOccurrenceMapOriginalSeq() {
		return occurrenceMapOriginalSeq;
	}

	public void setOccurrenceMapOriginalSeq(HashMap<String, String> occurrenceMapOriginalSeq) {
		this.occurrenceMapOriginalSeq = occurrenceMapOriginalSeq;
	}

	public int getViolationLength() {
		return violationLength;
	}

	public void setViolationLength(int violationLength) {
		this.violationLength = violationLength;
	}

	@Override
	public int compareTo(Object o) {
		if (((ViolationOccurrences) o).getViolationLength() > this.violationLength)
			return 1;
		else if (((ViolationOccurrences) o).getViolationLength() < this.violationLength)
			return -1;
		else
			return 0;
	}

	public static void main(String[] args) {
		String[] occurrences = new String[2];
		ViolationOccurrences vo = new ViolationOccurrences("1,2,3", occurrences, 3);
		ViolationOccurrences vo2 = new ViolationOccurrences("1,2", occurrences, 2);
		ArrayList<ViolationOccurrences> voList = new ArrayList<ViolationOccurrences>();
		voList.add(vo);
		voList.add(vo2);
		Collections.sort(voList);
		System.out.println(voList.get(0).getViolationLength());
	}
}
