package top.inputs;

import top.core.lts.local.datamodel.HashMapCount;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Map;

/**
 * Created by yizhouyan on 7/8/17.
 */
public class InputSequence {
    protected String inputString;
    protected ArrayList<String> singleItems;
    protected ArrayList<String> originalString;
    protected ArrayList<Integer> originalIndexes;
    protected ArrayList<Boolean> available;

    public InputSequence(String inputString){
        this.inputString = inputString;
        this.singleItems = new ArrayList<String>();
        this.originalIndexes = new ArrayList<Integer>();
        this.originalString = new ArrayList<String>();
        this.available = new ArrayList<Boolean>();
    }

    /**
     * Remove single item that does not satisfy minSupport
     */
    public void removeInitSeqsItemIncludeGlobalInFreq(HashSet<String> globalFrequentElements, int minSupport) {
        HashMapCount<String> itemMap = new HashMapCount<String>();
        String[] subItems = inputString.split(",");
        for (String temp : subItems) {
            itemMap.addObject(temp);
        }
        for (Map.Entry entry : itemMap.getHashMapForCountResult().entrySet()) {
            String key = (String) entry.getKey();
            int count = (int) entry.getValue();

            if (count >= minSupport  && globalFrequentElements.contains(key)) {
                singleItems.add(key);
            }
        }
        for (int i = 0; i < subItems.length; i++) {
            if (globalFrequentElements.contains(subItems[i]) && singleItems.contains(subItems[i])) {
                this.originalString.add(subItems[i]);
                this.originalIndexes.add(i);
                this.available.add(true);
            }
        }
    }

    public String getInputString() {
        return inputString;
    }

    public void setInputString(String inputString) {
        this.inputString = inputString;
    }

    public ArrayList<String> getSingleItems() {
        return singleItems;
    }

    public void setSingleItems(ArrayList<String> singleItems) {
        this.singleItems = singleItems;
    }

    public ArrayList<String> getOriginalString() {
        return originalString;
    }

    public void setOriginalString(ArrayList<String> originalString) {
        this.originalString = originalString;
    }

    public ArrayList<Integer> getOriginalIndexes() {
        return originalIndexes;
    }

    public void setOriginalIndexes(ArrayList<Integer> originalIndexes) {
        this.originalIndexes = originalIndexes;
    }

    public ArrayList<Boolean> getAvailable() {
        return available;
    }

    public void setAvailable(ArrayList<Boolean> available) {
        this.available = available;
    }
}
