package zemberek.spelling;

import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import zemberek.core.DoubleValueSet;
import zemberek.lm.compression.SmoothLm;

import java.util.ArrayList;
import java.util.List;

//TODO: Not yet finished.
public class LmBasedSpellChecker {
    SmoothLm lm;
    SingleWordSpellChecker singleWordSpellChecker;

    public LmBasedSpellChecker(SmoothLm lm, SingleWordSpellChecker singleWordSpellChecker) {
        this.lm = lm;
        this.singleWordSpellChecker = singleWordSpellChecker;
    }

    public String getBest(String input) {
        List<String> sequence = Lists.newArrayList(Splitter.on(" ").omitEmptyStrings().trimResults().split(input));
        List<DoubleValueSet<String>> results = new ArrayList<>();
        for (String s : sequence) {
            DoubleValueSet<String> suggestions = singleWordSpellChecker.decode(s);
            if (suggestions.size() == 0)
                suggestions.set(s, 0.0);
        }
        return "";

    }

}
