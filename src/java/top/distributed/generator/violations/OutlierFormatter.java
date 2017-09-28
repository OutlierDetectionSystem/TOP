package top.distributed.generator.violations;


import top.utils.StdRandom;

public class OutlierFormatter {
	public enum FormatterType {
		ADD(0), REMOVE(1), EXCHANGE(2);

		public final int value;

		FormatterType(final int value) {
			this.value = value;
		}

		public static FormatterType getValue(int value) {
			for (FormatterType e : FormatterType.values()) {
				if (e.value == value) {
					return e;
				}
			}
			return null;// not found
		}
	}

	public static String addElements(String inputStr, String extraElement) {
//		System.out.println("Call Add element");
		String[] subs = inputStr.split(",");
		int pos = StdRandom.random.nextInt(subs.length + 1);
		String returnStr = "";
		for (int i = 0; i < pos; i++) {
			returnStr += subs[i] + ",";
		}
		returnStr += extraElement + ",";
		for (int i = pos; i < subs.length; i++) {
			returnStr += subs[i] + ",";
		}
		if (returnStr.length() > 0)
			returnStr = returnStr.substring(0, returnStr.length() - 1);
		return returnStr;
	}

	public static String removeElements(String inputStr) {
//		System.out.println("Call Remove element");
		String[] subs = inputStr.split(",");
		int pos = StdRandom.random.nextInt(subs.length);
		String returnStr = "";
		for (int i = 0; i < pos; i++) {
			returnStr += subs[i] + ",";
		}
		for (int i = pos + 1; i < subs.length; i++) {
			returnStr += subs[i] + ",";
		}
		if (returnStr.length() > 0)
			returnStr = returnStr.substring(0, returnStr.length() - 1);
		return returnStr;
	}

	public static String exchangeElements(String inputStr) {
//		System.out.println("Call Exchange element");
		String[] subs = inputStr.split(",");
		int pos = StdRandom.random.nextInt(subs.length);
		int pos2;
		do{
			pos2 = StdRandom.random.nextInt(subs.length);
		} while(pos2 == pos);
		String temp = subs[pos];
		subs[pos] = subs[pos2];
		subs[pos2] = temp;
		
		String returnStr = "";
		for (int i = 0; i < subs.length; i++) {
			returnStr += subs[i] + ",";
		}
		if (returnStr.length() > 0)
			returnStr = returnStr.substring(0, returnStr.length() - 1);
		return returnStr;
	}

	public static String changeFSToOutliers(String inputStr, String extraElement) {
		int formatterCode = StdRandom.random.nextInt(3);
		if(StdRandom.bernoulli(0.5))
			formatterCode = 1;
//		formatterCode = (formatterCode + 1) % 3;
		switch (FormatterType.getValue(formatterCode)) {
		case ADD:
			return addElements(inputStr, extraElement);
		case REMOVE:
			return removeElements(inputStr);
		case EXCHANGE:
			return exchangeElements(inputStr);
		}
		return "";
	}

	public static String changeFSToViolations(String inputStr){
		int formatterCode = StdRandom.random.nextInt(2) + 1;
		if(StdRandom.bernoulli(0.5))
			formatterCode = 1;
		switch (FormatterType.getValue(formatterCode)) {
			case REMOVE:
				return removeElements(inputStr);
			case EXCHANGE:
				return exchangeElements(inputStr);
		}
		return "";
	}

	public static void main(String[] args) {
		String original = "1,2,3,4";
		String extraElement  = "7";
		System.out.println(OutlierFormatter.changeFSToOutliers(original, extraElement));
		System.out.println(OutlierFormatter.changeFSToOutliers(original, extraElement));
		System.out.println(OutlierFormatter.changeFSToOutliers(original, extraElement));
		System.out.println(OutlierFormatter.changeFSToOutliers(original, extraElement));
		System.out.println(OutlierFormatter.changeFSToOutliers(original, extraElement));
		System.out.println(OutlierFormatter.changeFSToOutliers(original, extraElement));
		System.out.println(OutlierFormatter.changeFSToOutliers(original, extraElement));
	}
}
