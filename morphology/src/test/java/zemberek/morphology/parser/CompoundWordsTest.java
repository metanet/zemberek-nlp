package zemberek.morphology.parser;

import org.junit.Assert;
import org.junit.Test;
import zemberek.morphology.lexicon.graph.DynamicLexiconGraph;
import zemberek.morphology.lexicon.tr.TurkishDictionaryLoader;
import zemberek.morphology.lexicon.tr.TurkishSuffixes;

import java.util.List;

public class CompoundWordsTest {

    TurkishSuffixes suffixProvider = new TurkishSuffixes();

    @Test
    public void parseTest1() {
        String[] lines = {"yağ", "zeytinyağı [A:CompoundP3sg; Roots:zeytin-yağ]"};
        String[] testSet = {"zeytinyağlı", "zeytinyağıdır", "zeytinyağım"};
        applyTest(lines, testSet);
    }

    private void applyTest(String[] lines, String[] testSet) {
        SimpleParser parser = getParser(lines);
        for (String s : testSet) {
            List<MorphParse> parseResults = parser.parse(s);
            if (parseResults.size() == 0)
                parser.dump(s);
            Assert.assertTrue(s, parseResults.size() > 0);
        }
    }

    private void applyFalseTest(String[] lines, String[] testSet) {
        SimpleParser parser = getParser(lines);
        for (String s : testSet) {
            List<MorphParse> parseResults = parser.parse(s);
            Assert.assertTrue(s, parseResults.size() == 0);
        }
    }

    @Test
    public void parseTest2() {
        String[] lines = {"yazı", "alınyazısı [A:CompoundP3sg; Roots:alın-yazı]"};
        String[] trueSet = {
                "alınyazısı", "alınyazısıdır",
                "alınyazım", "alınyazıma", "alınyazımda", "alınyazımdan", "alınyazımdır", "alınyazımsa"
        };
        applyTest(lines, trueSet);
    }

    @Test
    public void parseTest3() {
        String[] lines = {"kuyruk", "atkuyruğu [A:CompoundP3sg; Roots:at-kuyruk]"};
        String[] trueSet = {"atkuyruğum"};
        String[] falseSet = {"atkuyruk", "atkuyrukum"};
        applyTest(lines, trueSet);
        applyFalseTest(lines, falseSet);
    }

    private SimpleParser getParser(String... lines) {
        DynamicLexiconGraph graph = new DynamicLexiconGraph(suffixProvider);
        graph.addDictionaryItems(new TurkishDictionaryLoader(suffixProvider).load(lines));
        return new SimpleParser(graph);
    }
}
