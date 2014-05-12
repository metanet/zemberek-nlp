package zemberek.tokenizer;

import com.google.common.collect.Sets;
import zemberek.core.DoubleValueSet;
import zemberek.core.io.ResourceUtil;
import zemberek.core.io.SimpleTextReader;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class PerceptronSentenceBoundaryDetector implements SentenceBoundaryDetector {

    public static final int SKIP_SPACE_FREQUENCY = 200000;
    public static final String BOUNDARY_CHARS = ".!?";
    DoubleValueSet<String> weights = new DoubleValueSet<>();

    static Set<String> TurkishAbbreviationSet = new HashSet<>();
    private static Locale localeTr = new Locale("tr");

    static {
        try {
            for (String line : ResourceUtil.readAllLines("tokenizer/abbreviations.txt", PerceptronSentenceBoundaryDetector.class.getClassLoader())) {
                final int abbrEndIndex = line.indexOf(":");
                if (abbrEndIndex > 0) {
                    final String abbr = line.substring(0, abbrEndIndex);
                    if (abbr.endsWith(".")) {
                        TurkishAbbreviationSet.add(abbr);
                        TurkishAbbreviationSet.add(abbr.toLowerCase(Locale.ENGLISH));
                        TurkishAbbreviationSet.add(abbr.toLowerCase(localeTr));
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public PerceptronSentenceBoundaryDetector(DoubleValueSet<String> weights) {
        this.weights = weights;
    }

    public static class Trainer {
        File trainFile;
        int iterationCount;

        public Trainer(File trainFile, int iterationCount) {
            this.trainFile = trainFile;
            this.iterationCount = iterationCount;
        }

        public PerceptronSentenceBoundaryDetector train() throws IOException {
            DoubleValueSet<String> weights = new DoubleValueSet<>();
            List<String> sentences = SimpleTextReader.trimmingUTF8Reader(trainFile).asStringList();

            for (int i = 0; i < iterationCount; i++) {
                if (i != 0)
                    Collections.shuffle(sentences);

                Set<Integer> indexSet = new LinkedHashSet<>();

                Random rnd = new Random(1);
                StringBuilder sb = new StringBuilder();
                int boundaryIndexCounter;
                int sentenceCounter = 0;
                for (String sentence : sentences) {
                    sb.append(sentence);
                    boundaryIndexCounter = sb.length() - 1;
                    indexSet.add(boundaryIndexCounter);
                    // in approximately every 20 sentences we skip adding a space between sentences.
                    if (rnd.nextInt(SKIP_SPACE_FREQUENCY) != 1 && sentenceCounter < sentences.size() - 1) {
                        sb.append(" ");
                    }
                    sentenceCounter++;
                }

                String joinedSentence = sb.toString();

                for (int j = 0; j < joinedSentence.length(); j++) {
                    // skip if char cannot be a boundary char.
                    char chr = joinedSentence.charAt(j);
                    if (BOUNDARY_CHARS.indexOf(chr) < 0)
                        continue;
                    List<String> features = extractFeatures(joinedSentence, j);
                    double score = 0;
                    for (String feature : features) {
                        score += weights.get(feature);
                    }
                    int update = 0;
                    // if we found no-boundary but it is a boundary
                    if (score <= 0 && indexSet.contains(j)) {
                        update = 1;
                    }
                    // if we found boundary but it is not a boundary
                    else if (score > 0 && !indexSet.contains(j)) {
                        update = -1;
                    }
                    for (String feature : features) {
                        if (update != 0) {
                            double d = weights.incrementByAmount(feature, update);
                            if (d == 0.0)
                                weights.remove(feature);
                        }
                    }
                }
            }
            return new PerceptronSentenceBoundaryDetector(weights);
        }
    }

    @Override
    public List<String> getSentences(String doc) {
        List<String> sentences = new ArrayList<>();
        int begin = 0;
        for (int j = 0; j < doc.length(); j++) {
            // skip if char cannot be a boundary char.
            char chr = doc.charAt(j);
            if (BOUNDARY_CHARS.indexOf(chr) < 0)
                continue;
            List<String> features = extractFeatures(doc, j);
            double score = 0;
            for (String feature : features) {
                score += weights.get(feature);
            }
            if (score > 0) {
                sentences.add(doc.substring(begin, j + 1).trim());
                begin = j + 1;
            }
        }
        return sentences;
    }

    private static List<String> extractFeatures(String input, int pointer) {

        List<String> features = new ArrayList<>();
        // 1 letter before and after
        char previousLetter;
        if (pointer > 0)
            previousLetter = input.charAt(pointer - 1);
        else
            previousLetter = '_';
        char nextLetter;
        if (pointer < input.length() - 1)
            nextLetter = input.charAt(pointer + 1);
        else
            nextLetter = '_';

        String prev2 = "__";
        if (pointer > 2)
            prev2 = input.substring(pointer - 2, pointer);
        String next2 = "__";
        if (pointer < input.length() - 3) {
            next2 = input.substring(pointer + 1, pointer + 3);
        }

        String currentWord;
        int i = pointer - 1;
        StringBuilder sb = new StringBuilder();
        sb.append(input.charAt(pointer));
        while (i > 0) {
            char c = input.charAt(i);
            if (c == ' ') {
                break;
            }
            sb.append(c);
            i--;
        }
        currentWord = sb.reverse().toString();

        if (currentWord.length() > 0) {
            int trimLength = 3;
            if (sb.length() < trimLength) {
                trimLength = sb.length();
            }
            features.add("5:" + currentWord.substring(0, trimLength));
            features.add("5a:" + currentWord.substring(currentWord.length() - trimLength, currentWord.length()));
            features.add("8:" + currentWord);
            features.add("9:" + getMetaChars(currentWord));
/*            features.add("9a:" + getMetaChars(currentWord));
            features.add("9b:" + getMetaChars(currentWord));
            features.add("9c:" + getMetaChars(currentWord));*/
            features.add("9d:" + Character.isUpperCase(currentWord.charAt(0)));
            int dotIndex = currentWord.indexOf('.');
            features.add("9e:" + String.valueOf(dotIndex < currentWord.length() - 1));
        }

        String previousWord;
        while (i >= 0) {
            if (input.charAt(i) != ' ')
                break;
            --i;
        }
        sb = new StringBuilder();
        while (i >= 0) {
            char c = input.charAt(i);
            if (c == ' ') {
                break;
            }
            sb.append(c);
            --i;
        }
        previousWord = sb.reverse().toString();
        if (previousWord.length() > 0)
            features.add("5c:" + getMetaChars(previousWord));

        String nextWord;
        i = pointer + 1;
        sb = new StringBuilder();
        while (i < input.length()) {
            if (input.charAt(i) != ' ')
                break;
            i++;
        }
        while (i < input.length()) {
            char c = input.charAt(i);
            if (c == ' ') {
                break;
            }
            sb.append(c);
            i++;
        }
        nextWord = sb.toString();

        if (nextWord.length() > 0) {
            int trimLength = 3;
            if (sb.length() < trimLength) {
                trimLength = sb.length();
            }
            features.add("6:" + nextWord.substring(0, trimLength));
        }

        features.add("7:" + Character.isUpperCase(previousLetter));
        String currentNoPunct = currentWord.replaceAll("[.]", "");
        if (currentNoPunct.length() > 0)
            features.add("10:" + getMetaChars(currentNoPunct));

        if (currentNoPunct.length() > 0) {
            boolean allUp = true;
            for (char c : currentNoPunct.toCharArray()) {
                if (!Character.isUpperCase(c))
                    allUp = false;
            }
            features.add("11:" + allUp);
        }

        features.add("12:" + String.valueOf(numberOfChars(currentWord, '.')));
        features.add("12a:" + String.valueOf(numberOfChars(currentWord, '.') == 2));
        features.add("12b:" + String.valueOf(numberOfChars(currentWord, '.') == 3));
        features.add("12c:" + String.valueOf(numberOfChars(previousWord, '.')));
        features.add("12d:" + String.valueOf(numberOfChars(nextWord, '.')));
        features.add("12e:" + String.valueOf(numberOfChars(nextWord, '.') == 2));
        features.add("13:" + String.valueOf(TurkishAbbreviationSet.contains(currentWord + ".")));
        features.add("14:" + String.valueOf(TurkishAbbreviationSet.contains(nextWord)));
        features.add("15:" + String.valueOf(potentialWebSite(currentWord)));
        features.add("16:" + String.valueOf(TurkishAbbreviationSet.contains(previousWord)));

        features.add("1:" + previousLetter + nextLetter);
        features.add("2:" + getMetaChar(previousLetter) + getMetaChar(nextLetter));
        features.add("3:" + prev2 + next2);
        features.add("3a:" + getMetaChars(prev2) + getMetaChars(next2));
        //features.add("4:" + getMetaChars(prev2));


        return features;
    }


    public static final Set<String> urlWords = Sets.newHashSet("http", "www", ".tr", ".edu", ".com", ".net", ".gov", ".org");

    private static boolean potentialWebSite(String s) {
        for (String urlWord : urlWords) {
            if (s.contains(urlWord))
                return true;
        }
        return false;
    }

    private static int numberOfChars(String s, char c) {
        int result = 0;
        for (char chr : s.toCharArray()) {
            if (chr == c)
                result++;
        }
        return result;
    }

    private static char getMetaChar(char letter) {
        char c;
        if (Character.isUpperCase(letter))
            c = 'C';
        else if (Character.isLowerCase(letter))
            c = 'c';
        else if (Character.isDigit(letter))
            c = 'd';
        else if (Character.isWhitespace(letter))
            c = ' ';
        else if (BOUNDARY_CHARS.indexOf(letter) >= 0)
            c = 'P';
        else c = '-';
        return c;
    }

    private static String getMetaChars(String str) {
        StringBuilder sb = new StringBuilder(str.length());
        for (int i = 0; i < str.length(); i++) {
            sb.append(getMetaChar(str.charAt(i)));
        }
        return sb.toString();
    }
}
