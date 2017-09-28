package top.inputs;

import top.core.lts.local.datamodel.HashMapCount;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Map;

/**
 * Created by yizhouyan on 7/8/17.
 */
public class InputSequenceWithTS extends InputSequence {
    private ArrayList<Long> originalTimeStamps;
    public InputSequenceWithTS(String inputString){
        super(inputString);
        this.originalTimeStamps = new ArrayList<Long>();
    }

    @Override
    public void removeInitSeqsItemIncludeGlobalInFreq(HashSet<String> globalFrequentElements, int minSupport) {
        HashMapCount<String> itemMap = new HashMapCount<String>();
        String[] subItems = inputString.split(",");
        for (String temp : subItems) {
            String tempItem = temp.split("\\|")[0];
            itemMap.addObject(tempItem);
        }

        for (Map.Entry entry : itemMap.getHashMapForCountResult().entrySet()) {
            String key = (String) entry.getKey();
            int count = (int) entry.getValue();
            if (count >= minSupport  && globalFrequentElements.contains(key)) {
                singleItems.add(key);
            }
        }

        for (int i = 0; i < subItems.length; i++) {
            String[] splitItems = subItems[i].split("\\|");
            if (globalFrequentElements.contains(splitItems[0]) && singleItems.contains(splitItems[0])) {
                this.originalString.add(splitItems[0]);
                this.originalIndexes.add(i);
//                this.originalTimeStamps.add(Toolbox.dateTypeToLong(splitItems[1]));
                this.originalTimeStamps.add(Long.parseLong(splitItems[1]));
                this.available.add(true);
            }
        }
    }

    public ArrayList<Long> getOriginalTimeStamps() {
        return originalTimeStamps;
    }

    public void setOriginalTimeStamps(ArrayList<Long> originalTimeStamps) {
        this.originalTimeStamps = originalTimeStamps;
    }
}
