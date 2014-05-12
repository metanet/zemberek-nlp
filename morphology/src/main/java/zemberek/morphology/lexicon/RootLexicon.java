package zemberek.morphology.lexicon;

import com.google.common.collect.*;
import zemberek.core.turkish.PrimaryPos;

import java.util.*;

/**
 * This is the collection of all Dictionary Items.
 */
public class RootLexicon implements Iterable<DictionaryItem> {
    Multimap<String, DictionaryItem> itemMap = HashMultimap.create();
    Map<String, DictionaryItem> idMap = Maps.newHashMap();
    Set<DictionaryItem> itemSet = Sets.newLinkedHashSet();

    public RootLexicon(List<DictionaryItem> dictionaryItems) {
        itemSet.addAll(dictionaryItems);
    }

    public RootLexicon() {
    }

    public void add(DictionaryItem item) {
        if (itemSet.contains(item)) {
            throw new IllegalArgumentException("Duplicated item:" + item);
        }
        this.itemSet.add(item);
        if (idMap.containsKey(item.id)) {
            throw new IllegalArgumentException("Duplicated item id of:" + item + " with " + idMap.get(item.id));
        }
        idMap.put(item.id, item);
        itemMap.put(item.lemma, item);
    }

    public void addAll(Iterable<DictionaryItem> items) {
        for (DictionaryItem item : items) {
            add(item);
        }
    }

    public List<DictionaryItem> getMatchingItems(String lemma) {
        Collection<DictionaryItem> items = itemMap.get(lemma);
        if (items == null)
            return Collections.emptyList();
        else return Lists.newArrayList(items);
    }

    public DictionaryItem getItemById(String id) {
        return idMap.get(id);
    }

    public List<DictionaryItem> getMatchingItems(String lemma, PrimaryPos pos) {
        Collection<DictionaryItem> items = itemMap.get(lemma);
        if (items == null)
            return Collections.emptyList();
        List<DictionaryItem> matches = Lists.newArrayListWithCapacity(1);
        for (DictionaryItem item : items) {
            if (item.primaryPos == pos)
                matches.add(item);
        }
        return matches;
    }

    public boolean isEmpty() {
        return itemSet.isEmpty();
    }

    public int size() {
        return itemSet.size();
    }

    @Override
    public Iterator<DictionaryItem> iterator() {
        return itemSet.iterator();
    }
}
