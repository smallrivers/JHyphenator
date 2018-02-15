package de.mfietz.jhyphenator;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Hyphenator.java is an highly optimized adaptation of parts from Mathew
 * Kurian's TextJustify-Android Library:
 * https://github.com/bluejamesbond/TextJustify-Android/
 */

public class Hyphenator implements Serializable {

    private static final long serialVersionUID = 1L;

    //private static HashMap<String, Hyphenator> cached;
//
//    static {
//        cached = new HashMap<>();
//    }

    private final TrieNode trie;
    private final int leftMin;
    private final int rightMin;
    private final String patternChars;
    private final String splitRegEx;
    private final HashMap<String, int[]> mExceptions;

    private Hyphenator(HyphenPattern pattern) {
        this.trie = createTrie(pattern.patterns);
        this.leftMin = pattern.leftMin;
        this.rightMin = pattern.rightMin;
        String pc = pattern.patternChars.replace("-", "\\-");
        String s = pc.charAt(0) == '_' ? pc.substring(1) : pc;
        if (s.startsWith("\\-"))
            s = s.substring(2);
        patternChars = pc + s.toUpperCase();
        splitRegEx = "((?<=[^" + patternChars + "])|(?=[^_" + patternChars + "]))";
        mExceptions = pattern.mExceptions;
    }

    /**
     * Returns a hyphenator instance for a given hypenation pattern
     *
     * @param lang hyphenation language
     * @return newly created or cached hyphenator instance
     */
    public static Hyphenator getInstance(String lang) {
        HyphenPattern pattern = HyphenPattern.create(lang);
        if (pattern == null)
            return null;
        return new Hyphenator(pattern);

//        synchronized (cached) {
//            if (!cached.containsKey(lang)) {
//                HyphenPattern pattern = HyphenPattern.create(lang);
//                if (pattern == null)
//                    return null;
//                cached.put(lang, new Hyphenator(pattern));
//            }
//            return cached.get(lang);
//        }
    }

    public int getMinLen() {
        return leftMin + rightMin;
    }

    public String getPatternCharsLU() {
        String s = patternChars.charAt(0) == '_' ? patternChars.substring(1) : patternChars;
        return patternChars + s.toUpperCase();
    }

//    public static void clearCache() {
//        cached = new HashMap<>();
//    }

    private static TrieNode createTrie(Map<Integer, String> patternObject) {
        TrieNode t, tree = new TrieNode();

        for (Map.Entry<Integer, String> entry : patternObject.entrySet()) {
            int key = entry.getKey();
            String value = entry.getValue();
            String[] patterns = new String[value.length() / key];
            for (int i = 0; i + key <= value.length(); i = i + key) {
                patterns[i / key] = value.substring(i, i + key);
            }
            for (int i = 0; i < patterns.length; i++) {
                String pattern = patterns[i];
                t = tree;

                for (int c = 0; c < pattern.length(); c++) {
                    char chr = pattern.charAt(c);
                    if (Character.isDigit(chr)) {
                        continue;
                    }
                    int codePoint = pattern.codePointAt(c);
                    if (t.codePoint.get(codePoint) == null) {
                        t.codePoint.put(codePoint, new TrieNode());
                    }
                    t = t.codePoint.get(codePoint);
                }

                IntArrayList list = new IntArrayList();
                int digitStart = -1;
                for (int p = 0; p < pattern.length(); p++) {
                    if (Character.isDigit(pattern.charAt(p))) {
                        if (digitStart < 0) {
                            digitStart = p;
                        }
                        if(p == pattern.length()-1) {
                            // last number in the pattern
                            String number = pattern.substring(digitStart, pattern.length());
                            list.add(Integer.valueOf(number));
                        }
                    } else if (digitStart >= 0) {
                        String number = pattern.substring(digitStart, p);
                        list.add(Integer.valueOf(number));
                        digitStart = -1;
                    } else {
                        list.add(0);
                    }
                }
                t.points = list.toArray();
            }
        }
        return tree;
    }

    /**
     * Returns a list of syllables that indicates at which points the word can
     * be broken with a hyphen
     *
     * @param word Word to hyphenate
     * @return list of syllables
     */
    public List<String> hyphenate(String word) {
        String wl = word.toLowerCase();
        List<String> result = new ArrayList<String>();

        if (mExceptions != null) {
            int[] hypPos = mExceptions.get(wl);
            if (hypPos != null) {
                // declination :
                // dec +3=3
                // li  +2=5
                // na  +2=7
                // tion
                for (int i = 0; i < hypPos.length; i++) {
                    result.add(word.substring((i == 0 ? 0 : hypPos[i-1]), hypPos[i]));
                }
                return result;
            }
        }
        word = "_" + word + "_";
        String lowercase = "_" + wl + "_";

        int wordLength = lowercase.length();
        int[] points = new int[wordLength];
        int[] characterPoints = new int[wordLength];
        for (int i = 0; i < wordLength; i++) {
            points[i] = 0;
            characterPoints[i] = lowercase.codePointAt(i);
        }

        TrieNode node, trie = this.trie;
        int[] nodePoints;
        for (int i = 0; i < wordLength; i++) {
            node = trie;
            for (int j = i; j < wordLength; j++) {
                node = node.codePoint.get(characterPoints[j]);
                if (node != null) {
                    nodePoints = node.points;
                    if (nodePoints != null) {
                        for (int k = 0, nodePointsLength = nodePoints.length;
                             k < nodePointsLength; k++) {
                            points[i + k] = Math.max(points[i + k], nodePoints[k]);
                        }
                    }
                } else {
                    break;
                }
            }
        }

        int start = 1;
        for (int i = 1; i < wordLength - 1; i++) {
            if (i > this.leftMin && i < (wordLength - this.rightMin) && points[i] % 2 > 0) {
                result.add(word.substring(start, i));
                start = i;
            }
        }
        if (start < word.length() - 1) {
            result.add(word.substring(start, word.length() - 1));
        }
        return result;
    }

    public String hyphenateHtml(String snt) {
        int minLen = leftMin + rightMin;
        String[] as = snt.split(splitRegEx);
        StringBuilder bld = new StringBuilder();
        boolean inTag = false;
        for (int i = 0; i < as.length; i++) {
            String s = as[i];
            if (inTag && s.equals(">"))
                inTag = false;
            else if (!inTag) {
                if (s.equals("<"))
                    inTag = true;
                else {
                    // not in tag
                    if (s.length() >= minLen) {
                        List<String> list = hyphenate(s);
                        StringBuilder result = new StringBuilder(list.get(0));
                        for (int j = 1; j < list.size(); j++) {
                            result.append("\u00ad").append(list.get(j));
                        }
                        s = result.toString();
                    }
                }
            }
            bld.append(s);
        }
        return bld.toString();
    }

    public String hyphenateText(String snt) {
        int minLen = leftMin + rightMin;
        String[] as = snt.split(splitRegEx);
        StringBuilder bld = new StringBuilder();
        for (int i = 0; i < as.length; i++) {
            String s = as[i];
            if (s.length() >= minLen) {
                List<String> list = hyphenate(s);
                StringBuilder result = new StringBuilder(list.get(0));
                for (int j = 1; j < list.size(); j++) {
                    result.append("\u00ad").append(list.get(j));
                }
                s = result.toString();
            }
            bld.append(s);
        }
        return bld.toString();
    }
}
