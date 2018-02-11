package de.mfietz.jhyphenator;

import static junit.framework.Assert.assertNotNull;
import static org.junit.Assert.assertEquals;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.FileSystems;
import java.util.List;
import java.util.Locale;

import junitparams.JUnitParamsRunner;
import junitparams.Parameters;

import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(JUnitParamsRunner.class) public class HyphenatorTest {

    public HyphenatorTest() {
        HyphenPattern.setStreamReaderProvider(new HyphenPattern.StreamReaderProvider() {
            /**
             * @param lang - 2 letter language code, e.g. "en", "pl", or "en_gb" for British Engish
             *             patterns
             * @return InputStreamReader for the hyphenation pattern file or resource
             * @throws IOException
             */
            @Override
            public InputStreamReader getPatStreamForLang(String lang) throws IOException {
                // this provider finds android src/main/res/raw directory with hyphenation patterns,
                // reads pattern files from there.
                String path = Hyphenator.class.getProtectionDomain().getCodeSource().getLocation().getPath();
                // Example of path returned above on Windows:
                //     /C:/android/atVoiceLibs/JHyphenator/build/intermediates/classes/debug/
                path = path.substring(0, path.indexOf("/build/")) + "/src/main/res/raw/";
                InputStreamReader patIsr = new InputStreamReader(new FileInputStream(
                        path + lang + "_js"), "UTF-8");
                return patIsr;
            }
        });
    }

    /**
     * Example of setting StreamReaderProvider on Android within a class extending ContextWrapper,
     * e.g. Application, Activity, Service...
     */
//    HyphenPattern.setStreamReaderProvider(new HyphenPattern.StreamReaderProvider() {
//        @Override
//        public InputStreamReader getPatStreamForLang(String lang) throws IOException {
//            int resId = getResources().getIdentifier(lang+"_js", "raw", getPackageName());
//            if (resId == 0)
//                return null;
//            InputStreamReader patIsr = new InputStreamReader(
//                    getResources().openRawResource(resId), "UTF-8");
//            return patIsr;
//        }
//    });



    public static String join(List<String> list, String delimiter) {
        if (list == null || list.size() == 0) {
            return "";
        }
        StringBuilder result = new StringBuilder(list.get(0));
        for (int i = 1; i < list.size(); i++) {
            result.append(delimiter).append(list.get(i));
        }
        return result.toString();
    }

    @Test
    @Parameters({
            "conselheiros, con-se-lheiros",
    })
    public void testPt(String input, String expected) {
        Hyphenator h = Hyphenator.getInstance("pt");
        assertNotNull(h);
        String actual = join(h.hyphenate(input), "-");
        assertEquals(expected, actual);
        String hs = h.hyphenateText("Decerto, o montante das penas será acrescido de algumas dezenas, ou mesmo centenas, de anos.");
        assertEquals(hs, "De\u00ADcerto, o mon\u00ADtante das penas será acres\u00ADcido de al\u00ADgumas de\u00ADzenas, ou mesmo cen\u00ADtenas, de anos.");
        System.out.println(hs);
    }

    @Test
    @Parameters({
            "бесконечными,  бес-ко-неч-ны-ми",
    })
    public void testRu(String input, String expected) {
        Hyphenator h = Hyphenator.getInstance("ru");
        assertNotNull(h);
        String actual = join(h.hyphenate(input), "-");
        assertEquals(expected, actual);
        String hs = h.hyphenateText("Сияет бесконечными огнями колоссальный мегаполис, давно поглотивший округ Ориндж. Город поглощает и людей, маня бесконечными соблазнами.");
        assertEquals(hs, "Си\u00ADя\u00ADет бес\u00ADко\u00ADнеч\u00ADны\u00ADми ог\u00ADня\u00ADми ко\u00ADлос\u00ADсаль\u00ADный ме\u00ADга\u00ADпо\u00ADлис, дав\u00ADно по\u00ADгло\u00ADтив\u00ADший округ Ориндж. Го\u00ADрод по\u00ADгло\u00ADща\u00ADет и лю\u00ADдей, ма\u00ADня бес\u00ADко\u00ADнеч\u00ADны\u00ADми со\u00ADблаз\u00ADна\u00ADми.");
        System.out.println(hs);
    }


    @Test
    @Parameters({
      "ich, ich",
      "Kochschule, Koch-schu-le", 
      "Seewetterdienst, See-wet-ter-dienst",
      "Hochverrat, Hoch-ver-rat", 
      "Musterbeispiel, Mus-ter-bei-spiel",
      "Bundespräsident, Bun-des-prä-si-dent", 
      "Schmetterling, Schmet-ter-ling",
      "Christian, Chris-ti-an"
    }) 
    public void testDe(String input, String expected) {
        Hyphenator h = Hyphenator.getInstance("de");
        assertNotNull(h);
        String actual = join(h.hyphenate(input), "-");
        assertEquals(expected, actual);
    }

    @Test
    @Parameters({
            "Konstantynopol, Kon-stan-ty-no-pol",
            "uwięzienie, uwię-zie-nie",
            "niepodległość, nie-pod-le-głość",
            "gość, gość",
            "kat, kat",
            "kawa, ka-wa",
            "kawał, ka-wał",
            "małostkowaty, ma-łost-ko-wa-ty"
    })
    public void testPl(String input, String expected) {
        Hyphenator h = Hyphenator.getInstance("pl");
        assertNotNull(h);
        String actual = join(h.hyphenate(input), "-");
        assertEquals(expected, actual);
    }

    @Test
    @Parameters({
      "segítség, se-gít-ség"
    })
    public void testHu(String input, String expected) {
        Hyphenator h = Hyphenator.getInstance("hu");
        assertNotNull(h);
        String actual = join(h.hyphenate(input), "-");
        assertEquals(expected, actual);
  }


    @Test
    @Parameters({
      "crocodile, croc-o-dile",
      "activity, ac-tiv-ity",
      "potato, potato",
      "hyphenation, hy-phen-ation",
      "podcast, pod-cast",
      "message, mes-sage"
    })
    public void testEnUs(String input, String expected) {
        Hyphenator h = Hyphenator.getInstance("en");
        assertNotNull(h);
        String actual = join(h.hyphenate(input), "-");
        assertEquals(expected, actual);
    }

}
