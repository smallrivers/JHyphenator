package de.mfietz.jhyphenator;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
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
    final HashMap<String, int[]> mExceptions;

    private HyphenPattern(String lang, int leftMin, int rightMin, Map<Integer, String> patterns, String patternChars, HashMap<String, int[]> exceptions) {
        this.lang = lang;
        this.leftMin = leftMin;
        this.rightMin = rightMin;
        this.patterns = patterns;
        this.patternChars = patternChars;
        this.mExceptions = exceptions;
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
            HashMap<Integer, String> pattern = new HashMap<>();
            HashMap<String, int[]> exceptions = null;
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
                }
                else if (line.matches("\\s*patternChars\\s*:\\s*unescape\\(\\s*\\\".*\\\"\\).*")) {
                    // patternChars: unescape("ଆଅଇଈଉଊଋଏଐଔକଗଖଘଙଚଛଜଝଞଟଠଡଢଣତଥଦଧନପଫବଭମଯରଲଵଶଷସହଳିୀାୁୂୃୋୋୈୌୗ୍ଃଂ%u200D"),
                    String[] ss = line.split("\\\"");
                    patChars = ss[1];
                    // convert %uXXXX to \\uXXXX, then decode to actual character
                    Properties p = new Properties();
                    p.load(new StringReader("ue="+patChars.replace("%u", "\\u")));
                    patChars = p.getProperty("ue");
                }
                else if (line.matches("\\s*var\\s+exceptions\\s*=\\s*`\\s*") && !patChars.equals("")) {
                    String pc = patChars.replace("-", "\\-");
                    String allChars = pc.charAt(0) == '_' ? pc.substring(1) : pc;
                    if (allChars.startsWith("\\-"))
                        allChars = allChars.substring(2);
                    allChars = "\\-" + pc + allChars.toUpperCase();
                    exceptions = new HashMap<>();
                    while ((line = reader.readLine()) != null) {
                        line = line.trim();
                        if (line.matches("[" + allChars + "]{3,}")) {
                            String[] a = line.split("\\-");
                            // declination :
                            // dec +3=3
                            // li  +2=5
                            // na  +2=7
                            // tion +4=11
                            String[] syl = line.split("\\-");
                            int[] hypPos = new int[syl.length];
                            for (int i = 0; i < syl.length; i++) {
                                hypPos[i] = (i > 0 ? hypPos[i-1] : 0) + syl[i].length();
                            }
                            line = line.replace("-", "");
                            exceptions.put(line, hypPos);
                        }
                        else
                            break;
                    }
                    if (exceptions.size() == 0)
                        exceptions = null;
                }
            }
            return new HyphenPattern(lang, leftMin, rightMin, pattern, patChars, exceptions);
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
