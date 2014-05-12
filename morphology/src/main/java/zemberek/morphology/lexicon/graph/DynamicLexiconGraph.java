package zemberek.morphology.lexicon.graph;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import zemberek.core.turkish.PhoneticAttribute;
import zemberek.core.turkish.PhoneticExpectation;
import zemberek.morphology.lexicon.DictionaryItem;
import zemberek.morphology.lexicon.SuffixForm;
import zemberek.morphology.lexicon.SuffixProvider;
import zemberek.morphology.lexicon.SuffixSurfaceNodeGenerator;
import zemberek.morphology.lexicon.tr.StemNodeGenerator;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class DynamicLexiconGraph {

    private Map<SuffixSurfaceNode, SuffixSurfaceNode> rootSuffixNodeMap = Maps.newHashMap();
    Set<StemNode> stemNodes = Sets.newHashSet();

    StemNodeGenerator stemNodeGenerator;
    SuffixSurfaceNodeGenerator suffixSurfaceNodeGenerator = new SuffixSurfaceNodeGenerator();

    final SuffixProvider suffixProvider;

    private Map<SuffixForm, Set<SuffixSurfaceNode>> suffixFormMap = Maps.newConcurrentMap();

    public DynamicLexiconGraph(SuffixProvider suffixProvider) {
        this.suffixProvider = suffixProvider;
        this.stemNodeGenerator = new StemNodeGenerator(suffixProvider);
    }

    public int totalSuffixNodeCount() {
        int total = 0;
        for (SuffixForm suffixFormSet : suffixFormMap.keySet()) {
            total += suffixFormMap.get(suffixFormSet).size();
        }
        return total;
    }

    public int totalStemNodeCount() {
        return stemNodes.size();
    }

    public void removeStemNodes(StemNode... sNodes) {
        for (StemNode stemNode : sNodes) {
            stemNodes.remove(stemNode);
        }
    }

    public StemNode[] addDictionaryItem(DictionaryItem item) {

        StemNode[] stems = stemNodeGenerator.generate(item);

        for (StemNode stem : stems) {

            if (!stemNodes.contains(stem)) {

                SuffixSurfaceNode rootSuffixSurfaceNode = getRootSuffixNode(stem);
                if (!rootSuffixNodeMap.containsKey(rootSuffixSurfaceNode)) {
                    generateNodeConnections(rootSuffixSurfaceNode);
                }
                // check if it already exist. If it exists, use the existing one or add the new one.
                rootSuffixSurfaceNode = addOrRetrieveExisting(rootSuffixSurfaceNode);
                // connect stem to suffix root node.
                stem.suffixRootSurfaceNode = rootSuffixSurfaceNode;
                stemNodes.add(stem);
            } else {
                // duplicate stem!
                System.out.println("Stem Node:" + stem + " already exist.");
            }
        }
        return stems;
    }

    public void addDictionaryItems(DictionaryItem... items) {
        for (DictionaryItem item : items) {
            addDictionaryItem(item);
        }
    }

    public void addDictionaryItems(Iterable<DictionaryItem> items) {
        for (DictionaryItem item : items) {
            addDictionaryItem(item);
        }
    }

    public Set<StemNode> getStemNodes() {
        return stemNodes;
    }

    private SuffixSurfaceNode addOrRetrieveExisting(SuffixSurfaceNode surfaceNodeTocheck) {
        if (!rootSuffixNodeMap.containsKey(surfaceNodeTocheck)) {
            rootSuffixNodeMap.put(surfaceNodeTocheck, surfaceNodeTocheck);
            return surfaceNodeTocheck;
        } else return rootSuffixNodeMap.get(surfaceNodeTocheck);
    }

    public SuffixSurfaceNode getRootSuffixNode(StemNode node) {
        SuffixForm set = suffixProvider.getRootSet(node.dictionaryItem, node.exclusiveSuffixData);
        // construct a new suffix node.
        return new SuffixSurfaceNode(
                set,
                "",
                node.attributes,
                node.expectations,
                node.exclusiveSuffixData,
                node.termination);
    }

    /**
     * This method generates connections of a SuffixSurfaceNode.
     * A SuffixSurfaceNode is surfaceForm of a SuffixForm. (Suffix form ->A1pl_lAr, SuffixSurfaceNode is lar)
     * We already know the morphotactics of SuffixForms. So we get the specific SuffixNodes that can be connected to a particular SuffixSurfaceNode.
     * Such as, SuffixForm P1sg_Im can follow A1pl_lAr. Therefore, the SuffixSurfaceNode lar can only connect to "ım" surfaceNode of the P1sg_Im suffixForm.
     * Here this connection is generated, as the surfaceNode reference in the successor form is added to this surfaceNode.
     * However, if surfaceNode to be connected does not exist, it is generated as well. And once it is generated and connection is provided
     * Recursively connections to that surfaceNode are also generated.
     *
     * @param surfaceNode Node that connections to successive nodes will be generated.
     */
    private void generateNodeConnections(SuffixSurfaceNode surfaceNode) {
        // get the successive form sets for this surfaceNode.
        SuffixData successors = surfaceNode.suffixForm.connections;
        // iterate over form sets.
        for (SuffixForm successiveForm : successors) {

            // get the nodes for the  suffix form.
            List<SuffixSurfaceNode> nodesInSuccessor = suffixSurfaceNodeGenerator.generate(
                    surfaceNode.attributes,
                    surfaceNode.expectations,
                    surfaceNode.exclusiveSuffixData,
                    successiveForm);
            for (SuffixSurfaceNode surfaceNodeInSuccessor : nodesInSuccessor) {
                // if there are expectations for the surfaceNode, check if it matches with the attributes of the surfaceNode in successor.
                if (!surfaceNode.expectations.isEmpty()) {
                    if (!expectationsMatches(surfaceNode, surfaceNodeInSuccessor))
                        continue;
                }
                boolean recurse = false;
                if (!nodeExists(successiveForm, surfaceNodeInSuccessor)) {
                    recurse = true;
                }
                surfaceNodeInSuccessor = addOrReturnExisting(successiveForm, surfaceNodeInSuccessor);
                surfaceNode.addSuccNode(surfaceNodeInSuccessor);
                if (recurse) {
                    generateNodeConnections(surfaceNodeInSuccessor);
                }
            }
        }
    }

    private boolean expectationsMatches(SuffixSurfaceNode surfaceNode, SuffixSurfaceNode surfaceNodeInSuccessor) {
        if (surfaceNodeInSuccessor.isNullMorpheme())
            return true;
        if ((surfaceNode.expectations.contains(PhoneticExpectation.ConsonantStart) && surfaceNodeInSuccessor.attributes.contains(PhoneticAttribute.FirstLetterConsonant)) ||
                (surfaceNode.expectations.contains(PhoneticExpectation.VowelStart) && surfaceNodeInSuccessor.attributes.contains(PhoneticAttribute.FirstLetterVowel)))
            return true;
        else return false;

    }

    private boolean nodeExists(SuffixForm set, SuffixSurfaceNode newSurfaceNode) {
        Set<SuffixSurfaceNode> surfaceNodes = suffixFormMap.get(set);
        return surfaceNodes != null && surfaceNodes.contains(newSurfaceNode);
    }

    public void stats() {
        Set<StemNode> stemNodes = getStemNodes();
        System.out.println("Stem Node Count:" + stemNodes.size());
        Set<SuffixSurfaceNode> rootSuffixSurfaceNodes = new HashSet<SuffixSurfaceNode>();
        for (StemNode stemNode : stemNodes) {
            rootSuffixSurfaceNodes.add(stemNode.getSuffixRootSurfaceNode());
        }
        System.out.println("Root SuffixSurfaceNode count:" + rootSuffixSurfaceNodes.size());
        int nodeCount = 0;
        for (SuffixForm form : suffixFormMap.keySet()) {
            System.out.println(form.toString());
            Set<SuffixSurfaceNode> surfaceNodes = suffixFormMap.get(form);
            nodeCount += surfaceNodes.size();
        }
        System.out.println("SuffixSurfaceNode count:" + nodeCount);
    }

    public SuffixSurfaceNode addOrReturnExisting(SuffixForm set, SuffixSurfaceNode newSurfaceNode) {

        if (!suffixFormMap.containsKey(set)) {
            suffixFormMap.put(set, new HashSet<SuffixSurfaceNode>());
        }
        Set<SuffixSurfaceNode> surfaceNodes = suffixFormMap.get(set);
        if (!surfaceNodes.contains(newSurfaceNode)) {
            surfaceNodes.add(newSurfaceNode);
            return newSurfaceNode;
        }
        for (SuffixSurfaceNode surfaceNode : surfaceNodes) {
            if (surfaceNode.equals(newSurfaceNode))
                return surfaceNode;
        }
        throw new IllegalStateException("Cannot be here. Set: " + set.id + " Node:" + newSurfaceNode.dump());
    }
}
