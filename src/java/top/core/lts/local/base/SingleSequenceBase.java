package top.core.lts.local.base;

import top.core.datastructure.ItemPair;
import top.core.lts.local.datamodel.HashMapSet;
import top.inputs.InputSequence;
import top.inputs.InputSequenceWithTS;
import top.parameterspace.LocalParameterSpace;
import top.parameterspace.LocalParameterSpaceWithTS;
import top.utils.generateCombine;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

/**
 * Created by yizhouyan on 7/8/17.
 */
public class SingleSequenceBase implements Comparable {
    // a sequence is an array of itempairs
    protected ArrayList<ItemPair> itemPairList;
    protected HashMap<String, ArrayList<Integer>> subSeqs;
    public SingleSequenceBase(ArrayList<ItemPair> itemPairList) {
        this.itemPairList = itemPairList;
    }
    public SingleSequenceBase() {
        itemPairList = new ArrayList<ItemPair>();
    }

    public String containStringAsSub(String targetString, ArrayList<Integer> originalIndexes) {
        if (subSeqs.containsKey(targetString)) {
            String returnObsoluteIndexes = "";
            ArrayList<Integer> relativeIndexes = subSeqs.get(targetString);
            for(Integer i: relativeIndexes)
                returnObsoluteIndexes += originalIndexes.get(i) + ",";
            if(returnObsoluteIndexes.length() > 0)
                returnObsoluteIndexes = returnObsoluteIndexes.substring(0, returnObsoluteIndexes.length()-1);
            return returnObsoluteIndexes;
        }else
            return null;
    }

    public void generateSubSequences(InputSequence inputSequence, HashMap<String, Integer> possibleFSWithFreq,
                                     int curLength, LocalParameterSpace localParameterSpace,
                                     HashMapSet<String,Integer> usedIndexBySubs) {
        this.subSeqs = new HashMap<String, ArrayList<Integer>>();
        HashSet<String> possibleFS = new HashSet<String>();
        if (curLength == itemPairList.size()) {
            // if the whole list of items are required
            ArrayList<Integer> newIndexes = new ArrayList<Integer>();
            String newStr = "";
            for (ItemPair ip : this.itemPairList) {
                newIndexes.add(ip.getIndex());
                newStr += ip.getItem() + ",";
            }
            if (newStr.length() > 0)
                newStr = newStr.substring(0, newStr.length() - 1);
            if(!usedByPreviousSeqs(newStr, newIndexes, usedIndexBySubs)) {
                subSeqs.put(newStr, newIndexes);
                possibleFS.add(newStr);
            }
        } else if (curLength == 1) {
            // if only the first prefix is required
            if (inputSequence.getAvailable().get(itemPairList.get(0).getIndex())) {
                ArrayList<Integer> newIndexes = new ArrayList<Integer>();
                newIndexes.add(itemPairList.get(0).getIndex());
                if(!usedByPreviousSeqs(itemPairList.get(0).getItem(), newIndexes, usedIndexBySubs)) {
                    subSeqs.put(itemPairList.get(0).getItem(), newIndexes);
                    possibleFS.add(itemPairList.get(0).getItem());
                }
            }
        } else if(localParameterSpace.getClass().getName().toString().contains("TS")){
            ArrayList<ItemPair> truncatedItemPairList = this.itemPairList;
            if (this.itemPairList.size() >= curLength) {
                ArrayList<ArrayList<Integer>> finalRes = generateSubsHelper(this.itemPairList.size() - 1,
                        curLength - 1, localParameterSpace.getItemGap(), this.itemPairList);
                // traverse each possible combination, check those constraints
                for (ArrayList<Integer> finalTemp : finalRes) {
                    ArrayList<Integer> newIndexes = new ArrayList<Integer>();
                    newIndexes.add(this.itemPairList.get(0).getIndex());
                    String newStr = this.itemPairList.get(0).getItem();
                    // if there is anything inside available, set to true;
                    boolean available = inputSequence.getAvailable().get(this.itemPairList.get(0).getIndex());
                    int previousIndex = inputSequence.getOriginalIndexes().get(this.itemPairList.get(0).getIndex());
                    long previousTS = ((InputSequenceWithTS) inputSequence).getOriginalTimeStamps().get(this.itemPairList.get(0).getIndex());
                    for (Integer curPos : finalTemp) {
                        int tempIndex = inputSequence.getOriginalIndexes().get(this.itemPairList.get(curPos).getIndex());
                        long tempTS = ((InputSequenceWithTS) inputSequence).getOriginalTimeStamps().get(this.itemPairList.get(curPos).getIndex());
                        if (tempIndex - previousIndex > localParameterSpace.getItemGap() + 1 ||
                                tempTS-previousTS > ((LocalParameterSpaceWithTS) localParameterSpace).getItemTimeInterval() + 1) {
                            available = false;
                            break;
                        }
                        previousIndex = tempIndex;
                        previousTS = tempTS;
                        newIndexes.add(this.itemPairList.get(curPos).getIndex());
                        newStr += "," + this.itemPairList.get(curPos).getItem();
                        if (inputSequence.getAvailable().get(this.itemPairList.get(curPos).getIndex()))
                            available = true;
                        // System.out.print(curPos + ",");
                    }
                    if (available & (!usedByPreviousSeqs(newStr, newIndexes, usedIndexBySubs))) {
                        this.addToCurrentSubSequences(newStr, newIndexes);
                        possibleFS.add(newStr);
                    }
                } // end for
            } // end if (truncatedItemPairList.size() >= curLength)
        }else {
            if (this.itemPairList.size() >= curLength) {
                ArrayList<ArrayList<Integer>> finalRes = generateSubsHelper(this.itemPairList.size() - 1,
                        curLength - 1, localParameterSpace.getItemGap(), this.itemPairList);
                // traverse each possible combination, check those constraints
                for (ArrayList<Integer> finalTemp : finalRes) {
                    ArrayList<Integer> newIndexes = new ArrayList<Integer>();
                    newIndexes.add(this.itemPairList.get(0).getIndex());
                    String newStr = this.itemPairList.get(0).getItem();
                    // if there is anything inside available, set to true;
                    boolean available = inputSequence.getAvailable().get(this.itemPairList.get(0).getIndex());
                    int previousIndex = inputSequence.getOriginalIndexes().get(this.itemPairList.get(0).getIndex());
                    for (Integer curPos : finalTemp) {
                        int tempIndex = inputSequence.getOriginalIndexes().get(this.itemPairList.get(curPos).getIndex());
                        if (tempIndex - previousIndex > localParameterSpace.getItemGap() + 1) {
                            available = false;
                            break;
                        }
                        previousIndex = tempIndex;
                        newIndexes.add(this.itemPairList.get(curPos).getIndex());
                        newStr += "," + this.itemPairList.get(curPos).getItem();
                        if (inputSequence.getAvailable().get(this.itemPairList.get(curPos).getIndex()))
                            available = true;
                        // System.out.print(curPos + ",");
                    }
                    if (available & (!usedByPreviousSeqs(newStr, newIndexes, usedIndexBySubs))) {
                        this.addToCurrentSubSequences(newStr, newIndexes);
                        possibleFS.add(newStr);
                    }
                } // end for
            } // end if (truncatedItemPairList.size() >= curLength)
        }
        addToPossibleFSWithFreq(possibleFSWithFreq, possibleFS);
    } // end
    // function


    public boolean usedByPreviousSeqs(String newStr, ArrayList<Integer> newIndexes,
                                      HashMapSet<String,Integer> usedIndexBySubs){
        boolean usedByPrevious = false;
        if(usedIndexBySubs.getHashMapSetResult().containsKey(newStr)){
            HashSet<Integer> usedIndexes = usedIndexBySubs.getHashMapSetResult().get(newStr);
            for(int ni: newIndexes){
                if(usedIndexes.contains(ni)){
                    usedByPrevious =  true;
                }
            }
        }
        if(!usedByPrevious)
            usedIndexBySubs.addSets(newStr, newIndexes);
        return usedByPrevious;
    }

    public void generateSubSequences(InputSequence inputSequence, HashMap<String, Integer> possibleFSWithFreq,
                                     int curLength, LocalParameterSpace localParameterSpace,
                                     HashSet<String> frequentTokens, HashMapSet<String,Integer> usedIndexBySubs) {
        this.subSeqs = new HashMap<String, ArrayList<Integer>>();
        HashSet<String> possibleFS = new HashSet<String>();
        if (curLength == itemPairList.size()) {
            // if the whole list of items are required
            ArrayList<Integer> newIndexes = new ArrayList<Integer>();
            String newStr = "";
            boolean isPossibleFS = true;
            for (ItemPair ip : this.itemPairList) {
                newIndexes.add(ip.getIndex());
                newStr += ip.getItem() + ",";
                if (!frequentTokens.contains(ip.getItem())) {
                    isPossibleFS = false;
                    break;
                }
            }
            if (isPossibleFS) {
                if (newStr.length() > 0)
                    newStr = newStr.substring(0, newStr.length() - 1);
                if(!usedByPreviousSeqs(newStr, newIndexes, usedIndexBySubs)) {
                    subSeqs.put(newStr, newIndexes);
                    possibleFS.add(newStr);
                }
            }
        } else if (curLength == 1) {
            // if only the first prefix is required
            if (inputSequence.getAvailable().get(itemPairList.get(0).getIndex())) {
                ArrayList<Integer> newIndexes = new ArrayList<Integer>();
                newIndexes.add(itemPairList.get(0).getIndex());
                if(!usedByPreviousSeqs(itemPairList.get(0).getItem(), newIndexes, usedIndexBySubs)) {
                    subSeqs.put(itemPairList.get(0).getItem(), newIndexes);
                    possibleFS.add(itemPairList.get(0).getItem());
                }
            }
        } else if(localParameterSpace.getClass().getName().toString().contains("TS")){
            ArrayList<ItemPair> truncatedItemPairList;
            truncatedItemPairList = truncateItemPairListWithTokenFrequency(frequentTokens,
                    inputSequence, localParameterSpace.getItemGap(),
                    ((LocalParameterSpaceWithTS) localParameterSpace).getItemTimeInterval());

            if (truncatedItemPairList.size() >= curLength) {
                ArrayList<ArrayList<Integer>> finalRes = generateSubsHelper(truncatedItemPairList.size() - 1,
                        curLength - 1, localParameterSpace.getItemGap(), truncatedItemPairList);
                // traverse each possible combination, check those constraints
                for (ArrayList<Integer> finalTemp : finalRes) {
                    ArrayList<Integer> newIndexes = new ArrayList<Integer>();
                    newIndexes.add(truncatedItemPairList.get(0).getIndex());
                    String newStr = truncatedItemPairList.get(0).getItem();
                    // if there is anything inside available, set to true;
                    boolean available = inputSequence.getAvailable().get(truncatedItemPairList.get(0).getIndex());
                    int previousIndex = inputSequence.getOriginalIndexes().get(truncatedItemPairList.get(0).getIndex());
                    long previousTS = ((InputSequenceWithTS) inputSequence).getOriginalTimeStamps().get(truncatedItemPairList.get(0).getIndex());
                    for (Integer curPos : finalTemp) {
                        int tempIndex = inputSequence.getOriginalIndexes().get(truncatedItemPairList.get(curPos).getIndex());
                        long tempTS = ((InputSequenceWithTS) inputSequence).getOriginalTimeStamps().get(truncatedItemPairList.get(curPos).getIndex());
                        if (tempIndex - previousIndex > localParameterSpace.getItemGap() + 1 || tempTS-previousTS >
                                ((LocalParameterSpaceWithTS) localParameterSpace).getItemTimeInterval() + 1) {
                            available = false;
                            break;
                        }
                        previousIndex = tempIndex;
                        previousTS = tempTS;
                        newIndexes.add(truncatedItemPairList.get(curPos).getIndex());
                        newStr += "," + truncatedItemPairList.get(curPos).getItem();
                        if (inputSequence.getAvailable().get(truncatedItemPairList.get(curPos).getIndex()))
                            available = true;
                        // System.out.print(curPos + ",");
                    }
                    if (available & (!usedByPreviousSeqs(newStr, newIndexes, usedIndexBySubs))) {
                        this.addToCurrentSubSequences(newStr, newIndexes);
                        possibleFS.add(newStr);
                    }
                } // end for
            } // end if (truncatedItemPairList.size() >= curLength)
        }else {
            ArrayList<ItemPair> truncatedItemPairList;
            truncatedItemPairList = truncateItemPairListWithTokenFrequency(frequentTokens,
                    inputSequence, localParameterSpace.getItemGap());
            if (truncatedItemPairList.size() >= curLength) {
                ArrayList<ArrayList<Integer>> finalRes = generateSubsHelper(truncatedItemPairList.size() - 1,
                        curLength - 1, localParameterSpace.getItemGap(), truncatedItemPairList);
                // traverse each possible combination, check those constraints
                for (ArrayList<Integer> finalTemp : finalRes) {
                    ArrayList<Integer> newIndexes = new ArrayList<Integer>();
                    newIndexes.add(truncatedItemPairList.get(0).getIndex());
                    String newStr = truncatedItemPairList.get(0).getItem();
                    // if there is anything inside available, set to true;
                    boolean available = inputSequence.getAvailable().get(truncatedItemPairList.get(0).getIndex());
                    int previousIndex = inputSequence.getOriginalIndexes().get(truncatedItemPairList.get(0).getIndex());
                    for (Integer curPos : finalTemp) {
                        int tempIndex = inputSequence.getOriginalIndexes().get(truncatedItemPairList.get(curPos).getIndex());
                        if (tempIndex - previousIndex > localParameterSpace.getItemGap() + 1) {
                            available = false;
                            break;
                        }
                        previousIndex = tempIndex;
                        newIndexes.add(truncatedItemPairList.get(curPos).getIndex());
                        newStr += "," + truncatedItemPairList.get(curPos).getItem();
                        if (inputSequence.getAvailable().get(truncatedItemPairList.get(curPos).getIndex()))
                            available = true;
                        // System.out.print(curPos + ",");
                    }
                    if (available & (!usedByPreviousSeqs(newStr, newIndexes, usedIndexBySubs))) {
                        this.addToCurrentSubSequences(newStr, newIndexes);
                        possibleFS.add(newStr);
                    }
                } // end for
            } // end if (truncatedItemPairList.size() >= curLength)
        } // end else
        addToPossibleFSWithFreq(possibleFSWithFreq, possibleFS);
    } // end
    // function

    public ArrayList<ItemPair> truncateItemPairListWithTokenFrequency(HashSet<String> frequentTokens,
                                                                      InputSequence inputSequence, int itemGapConstraint, long itemTimeInterval) {
        ArrayList<ItemPair> truncatedItemPairList = new ArrayList<ItemPair>();
        truncatedItemPairList.add(this.itemPairList.get(0));
        boolean available = inputSequence.getAvailable().get(itemPairList.get(0).getIndex());
        int previousIndex = inputSequence.getOriginalIndexes().get(itemPairList.get(0).getIndex());
        long previousTS = ((InputSequenceWithTS) inputSequence).getOriginalTimeStamps().get(itemPairList.get(0).getIndex());
        for (int i = 1; i < this.itemPairList.size(); i++) {
            ItemPair curItemPair = this.itemPairList.get(i);
            if (!frequentTokens.contains(curItemPair.getItem()))
                continue;
            int tempIndex = inputSequence.getOriginalIndexes().get(curItemPair.getIndex());
            long tempTS = ((InputSequenceWithTS) inputSequence).getOriginalTimeStamps().get(curItemPair.getIndex());
            if (tempIndex - previousIndex > itemGapConstraint + 1 || tempTS - previousTS > itemTimeInterval + 1) {
                break;
            }
            previousIndex = tempIndex;
            previousTS = tempTS;
            truncatedItemPairList.add(curItemPair);
            if (inputSequence.getAvailable().get(curItemPair.getIndex()))
                available = true;
        }
        if (!available)
            return new ArrayList<ItemPair>();
        return truncatedItemPairList;
    }



    public void addToPossibleFSWithFreq(HashMap<String, Integer> possibleFSWithFreq, HashSet<String> possibleFS ){
        for (String tempCurFS : possibleFS) {
            if (possibleFSWithFreq.containsKey(tempCurFS)) {
                possibleFSWithFreq.put(tempCurFS, possibleFSWithFreq.get(tempCurFS) + 1);
            } else
                possibleFSWithFreq.put(tempCurFS, 1);
        }
    }

    public ArrayList<ItemPair> truncateItemPairListWithTokenFrequency(HashSet<String> frequentTokens,
                                                                      InputSequence inputSequence, int itemGap) {
        ArrayList<ItemPair> truncatedItemPairList = new ArrayList<ItemPair>();
        truncatedItemPairList.add(this.itemPairList.get(0));
        boolean available = inputSequence.getAvailable().get(itemPairList.get(0).getIndex());
        int previousIndex = inputSequence.getOriginalIndexes().get(itemPairList.get(0).getIndex());
        for (int i = 1; i < this.itemPairList.size(); i++) {
            ItemPair curItemPair = this.itemPairList.get(i);
            if (!frequentTokens.contains(curItemPair.getItem()))
                continue;
            int tempIndex = inputSequence.getOriginalIndexes().get(curItemPair.getIndex());
            if (tempIndex - previousIndex > itemGap + 1) {
                break;
            }
            previousIndex = tempIndex;
            truncatedItemPairList.add(curItemPair);
            if (inputSequence.getAvailable().get(curItemPair.getIndex()))
                available = true;
        }
        if (!available)
            return new ArrayList<ItemPair>();
        return truncatedItemPairList;
    }

    public void addToCurrentSubSequences(String newStr, ArrayList<Integer> newIndexes) {
        if (this.subSeqs.containsKey(newStr)) {
            // check which one has smaller indexes, and save the one with
            // smallest indexes
            ArrayList<Integer> oldIndexes = this.subSeqs.get(newStr);
            boolean hasToReplace = false;
            for (int i = 0; i < oldIndexes.size(); i++) {
                if (newIndexes.get(i) < oldIndexes.get(i)) {
                    hasToReplace = true;
                    break;
                }
            } // end for
            if (hasToReplace)
                this.subSeqs.put(newStr, newIndexes);
        } else {
            this.subSeqs.put(newStr, newIndexes);
        }
    }

    public ArrayList<ArrayList<Integer>> generateSubsHelper(int originalSize, int targetSize, int itemGap,
                                                            ArrayList<ItemPair> truncatedItemPairList) {
        ArrayList<Integer> originalList = new ArrayList<Integer>();
        for (int i = 1; i <= originalSize; i++)
            originalList.add(i);
        int[] b = new int[targetSize];
        ArrayList<ArrayList<Integer>> finalRes = new ArrayList<ArrayList<Integer>>();
        generateCombine.comb(originalList.toArray(), b, 0, targetSize, finalRes, itemGap,
                truncatedItemPairList.get(0).getIndex(), truncatedItemPairList);
        return finalRes;
    }
    public ArrayList<ItemPair> getItemPairList() {
        return itemPairList;
    }

    public void setItemPairList(ArrayList<ItemPair> itemPairList) {
        this.itemPairList = (ArrayList<ItemPair>) itemPairList.clone();
    }

    public boolean containStringAsSub(String targetString, HashSet<Integer> possibleEliminates) {
        if (subSeqs.containsKey(targetString)) {
            possibleEliminates.addAll(subSeqs.get(targetString));
            return true;
        }
        return false;
    }

    public String getSequenceInString() {
        String str = "";
        for (ItemPair it : this.itemPairList) {
            str += it.getItem() + ",";
        }
        str = str.substring(0, str.length() - 1);
        return str;
    }

    public String getSequenceInStringWithIndex() {
        String str = "";
        for (ItemPair it : this.itemPairList) {
            str += it.getItem() +  "[" + it.getIndex() + "]" + ",";
        }
        str = str.substring(0, str.length() - 1);
        return str;
    }

    public HashSet<String> getItemSetInString() {
        HashSet<String> returnSet = new HashSet<String>();
        for (ItemPair it : this.itemPairList)
            returnSet.add(it.getItem());
        return returnSet;
    }

    public boolean checkAvailablity(ArrayList<Boolean> availability) {
        for (ItemPair ip : itemPairList) {
            if (availability.get(ip.getIndex()))
                return true;
        }
        return false;
    }

    @Override
    public int compareTo(Object o) {
        SingleSequenceBase other  = (SingleSequenceBase) o;
        if(this.getItemPairList().get(0).getIndex() > other.getItemPairList().get(0).getIndex())
            return 1;
        else if(this.getItemPairList().get(0).getIndex() < other.getItemPairList().get(0).getIndex())
            return -1;
        else
            return 0;
    }
}
