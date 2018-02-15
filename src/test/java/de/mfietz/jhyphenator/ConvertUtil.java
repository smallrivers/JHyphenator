package de.mfietz.jhyphenator;

import org.junit.Test;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;

import static junit.framework.Assert.assertTrue;

/**
 * This is not a test, but rather my utility program to convert hyphenation patters
 * from https://github.com/ytiurin/hyphen to Hyphenopoly format. They have more patterns
 * (e.g. Bulgarian) than Hyphenator and Hyphenopoly, hence the need for conversion.
 *
 * Why test and not new Java project? I'm writing this in Android Studio, too lazy
 * to use different environment and create special project for this little onversion.
 */

public class ConvertUtil {

    @Test
    public void test() {
        String lang = "bg";
        boolean ret = convert(new File("/github/hyphen/patterns/" + lang + ".js"),
                new File("/android/atVoiceLibs/JHyphenator/src/main/res/raw/" + lang + "_js"));
        assertTrue(ret);
    }

    private boolean convert(File ytFile, File hpFile) {
        BufferedReader reader = null;
        BufferedWriter writer = null;
        IntHashMap <StringBuilder> patterns = new IntHashMap<>(16, null, StringBuilder.class);
        try {
            reader = new BufferedReader(new InputStreamReader(new FileInputStream(ytFile)));
            writer = new BufferedWriter(new FileWriter(hpFile));
            String line;
            boolean inPats = false;
            HashSet<Character> patternChars = new HashSet<>(64);
            while ((line = reader.readLine()) != null) {
                if (!inPats && line.matches("\\s*var\\s+patterns\\s*=\\s*`\\s*")) {
                    inPats = true;
                    continue;
                }
                if (!inPats) {
                    if (line.matches("\\s*// Hyphenation patterns for .+")) {
                        writer.write(line);
                        writer.write("\n");
                    }
                    continue;
                }
                line = line.trim();
                if (line.matches("`\\s*;")) {
                    inPats = false;
                    continue;
                }
                if (line.length() < 2 || line.startsWith("%"))
                    continue;
                line = line.replace(".", "_");
                String[] p = line.split("\\s+");
                for (String s : p) {
                    int n = s.length();
                    StringBuilder sb = patterns.get(n);
                    if (sb == null) {
                        sb = new StringBuilder(1024);
                        patterns.put(n, sb);

                    }
                    sb.append(s);
                    for (int j = 0; j < s.length(); j++) {
                        char c = s.charAt(j);
                        if (c > '9')
                            patternChars.add(c);
                    }
                }
            }
            writer.write("// Converted from https://github.com/ytiurin/hyphen/blob/master/patterns/" + ytFile.getName() + "\n");
            writer.write("// with ConvertUtil.java test class.\n");
            writer.write("leftmin: 2,\n");
            writer.write("rightmin: 2,\n");
            IntHashMap.Iterator it = patterns.iterator();
            while (it.hasNext()) {
                it.advance();
                int k = it.key();
                StringBuilder sb = (StringBuilder) it.value();
                writer.write(k + ": \"" + sb.toString() + "\",\n");
            }
            ArrayList<Character> ptChList = new ArrayList(patternChars);
            Collections.sort(ptChList);
            StringBuilder pcsb = new StringBuilder(ptChList.size());
            for (Character c : ptChList)
                pcsb.append(c);
            writer.write("patternChars: \"" + pcsb.toString() + "\",\n");
            return true;
        }
        catch (IOException iox) {
            iox.printStackTrace();
            return false;
        }
        finally {
            try {
                if (writer != null)
                    writer.close();
                if (reader != null)
                    reader.close();
            }
            catch (IOException ign) {}
        }
    }
}
