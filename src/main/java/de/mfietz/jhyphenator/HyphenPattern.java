package de.mfietz.jhyphenator;

import android.annotation.SuppressLint;
import android.util.SparseArray;

import com.hyperionics.ttssetup.AndyUtil;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

// GKochaniak, 2/6/2018
// One hunge enum is not a good idea for mobile devices (Android)
// Need to re-code these structures as JSON files maybe, load one
// language on demand.

public class HyphenPattern {

    private static StreamReaderProvider mStrmProvider = null;
    final String lang;
    final int leftMin;
    final int rightMin;
    final Map<Integer, String> patterns;
    final String patternChars;

    private HyphenPattern(String lang, int leftMin, int rightMin, Map<Integer, String> patterns, String patternChars) {
        this.lang = lang;
        this.leftMin = leftMin;
        this.rightMin = rightMin;
        this.patterns = patterns;
        this.patternChars = patternChars;
    }

    public static HyphenPattern create(String lang) {
        if (mStrmProvider == null)
            return null;
        BufferedReader reader = null;
        try {
            InputStreamReader langPatternsStream = mStrmProvider.getPatStreamForLang(lang);
            if (langPatternsStream == null)
                return null;
            reader = new BufferedReader(langPatternsStream); //new InputStreamReader(AndyUtil.getApp().getAssets().open("hyphens/" + lang + ".js"), "UTF-8"));
            String line, patChars = "";
            int leftMin = 1, rightMin = 1;
            @SuppressLint("UseSparseArrays")
            HashMap<Integer, String> pattern = new HashMap<>();
            while ((line = reader.readLine()) != null) {
                if (line.matches("\\s*leftmin\\s*:\\s*\\d+.*")) {
                    leftMin = Integer.parseInt(line.split("[^\\d]+")[1]);
                }
                else if (line.matches("\\s*rightmin\\s*:\\s*\\d+.*")) {
                    rightMin = Integer.parseInt(line.split("[^\\d]+")[1]);
                }
                else if (line.matches("\\s*\\\"\\d+\\\"\\s*:\\s*\\\".*\\\".*")) { // like "2": "a1ą1e1ę1i1o1ó1u1y1",
                    String[] ss = line.split("\\\"");
                    int n = Integer.parseInt(ss[1]);
                    pattern.put(n, ss[3]);
                }
                else if (line.matches("\\s*\\d+\\s*:\\s*\\\".*\\\".*")) { // like 13 : "5einstellunge_er8stritten_",
                    String[] ss = line.split(":|\\\"");
                    int n = Integer.parseInt(ss[0].trim());
                    pattern.put(n, ss[2]);
                }
                else if (line.matches("\\s*patternChars\\s*:\\s*\\\".*\\\".*")) {
                    String[] ss = line.split("\\\"");
                    patChars = ss[1];
                } else if (line.matches("\\s*patternChars\\s*:\\s*unescape\\(\\s*\\\".*\\\"\\).*")) {
                    // patternChars: unescape("ଆଅଇଈଉଊଋଏଐଔକଗଖଘଙଚଛଜଝଞଟଠଡଢଣତଥଦଧନପଫବଭମଯରଲଵଶଷସହଳିୀାୁୂୃୋୋୈୌୗ୍ଃଂ%u200D"),
                    String[] ss = line.split("\\\"");
                    patChars = ss[1];
                    // convert %uXXXX to \\uXXXX, then decode to actual character
                    Properties p = new Properties();
                    p.load(new StringReader("ue="+patChars.replace("%u", "\\u")));
                    patChars = p.getProperty("ue");                }
            }
            return new HyphenPattern(lang, leftMin, rightMin, pattern, patChars);
        }
        catch (IOException iox) {
            iox.printStackTrace();
            return null;
        }
        finally {
            if (reader != null) try {
                reader.close();
            } catch (IOException ign) {}
        }
    }

    public static void setStreamReaderProvider(StreamReaderProvider strmProvider) {
        mStrmProvider = strmProvider;
    }

    public interface StreamReaderProvider {
        InputStreamReader getPatStreamForLang(String lang) throws IOException;
    }

}
