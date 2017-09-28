package top.inputs;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by yizhouyan on 7/9/17.
 */
public class InputFile {
    private ArrayList<String> inputStringArray;
    private HashMap<Integer, String> deviceIdMap; // map device id and map
    private boolean containDeviceId; // if contains device id
    private HashMap<String, String> metaDataMapping;

    public InputFile(ArrayList<String> inputStringArray,HashMap<Integer, String> deviceIdMap,
                     HashMap<String, String> metaDataMapping, boolean containDeviceId){
        this.inputStringArray = inputStringArray;
        this.deviceIdMap = deviceIdMap;
        this.metaDataMapping = metaDataMapping;
        this.containDeviceId = containDeviceId;
    }
    public ArrayList<String> getInputStringArray() {
        return inputStringArray;
    }

    public void setInputStringArray(ArrayList<String> inputStringArray) {
        this.inputStringArray = inputStringArray;
    }

    public HashMap<Integer, String> getDeviceIdMap() {
        return deviceIdMap;
    }

    public void setDeviceIdMap(HashMap<Integer, String> deviceIdMap) {
        this.deviceIdMap = deviceIdMap;
    }

    public boolean isContainDeviceId() {
        return containDeviceId;
    }

    public void setContainDeviceId(boolean containDeviceId) {
        this.containDeviceId = containDeviceId;
    }

    public HashMap<String, String> getMetaDataMapping() {
        return metaDataMapping;
    }

    public void setMetaDataMapping(HashMap<String, String> metaDataMapping) {
        this.metaDataMapping = metaDataMapping;
    }
}
