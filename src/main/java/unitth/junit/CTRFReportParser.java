/*
 * MIT License - see LICENSE.txt
 */
package unitth.junit;

import java.io.File;
import java.io.FileReader;
import java.io.BufferedReader;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;

import unitth.core.ReportParser;
import unitth.core.UnitTH;
import unitth.core.UnitTHException;

public class CTRFReportParser extends ReportParser {

    private TestHistory history = null;
    private int currentRunIdx = 0;

    public CTRFReportParser() {
        history = new TestHistory();
    }

    @Override
    public TestHistory parseRuns(ArrayList<ArrayList<File>> testRunFiles,
            ArrayList<String> dirs) {
        currentRunIdx = 1;
        for (ArrayList<File> files : testRunFiles) {
            TestRun currentTestRun = new TestRun();
            HashMap<String, TestPackage> parsedTestPackages = new HashMap<>();
            for (File f : files) {
                parseFile(f, parsedTestPackages);
            }
            currentTestRun.setRunPath(dirs.get(currentRunIdx - 1));
            currentTestRun.addTestPackages(parsedTestPackages);
            try {
                history.addTestRun(currentTestRun);
            } catch (UnitTHException e) {
                System.err.println("Error adding test run: " + e.getMessage());
            }
            currentRunIdx++;
        }
        return history;
    }

    private void parseFile(File file, HashMap<String, TestPackage> packages) {
        if (UnitTH.c_DBG) {
            System.out.println("Parsing CTRF file: " + file.getName());
        }
        try {
            String json = readFile(file);

            String testsArray = extractTestsArray(json);
            if (testsArray == null) {
                System.err.println("No tests array found in: " + file.getName());
                return;
            }

            String folderDate = parseFolderDate(file.getParentFile().getName());

            java.util.LinkedHashMap<String, ArrayList<String>> suiteMap = new java.util.LinkedHashMap<>();
            ArrayList<String> testObjects = splitTestObjects(testsArray);
            for (String testJson : testObjects) {
                String suite = extractString(testJson, "\"suite\"", null);
                if (suite == null) suite = "SQLValidation";
                suiteMap.computeIfAbsent(suite, k -> new ArrayList<>()).add(testJson);
            }

            for (java.util.Map.Entry<String, ArrayList<String>> entry : suiteMap.entrySet()) {
                String suiteName = entry.getKey();
                ArrayList<String> tests = entry.getValue();

                TestModule module = new TestModule();
                int lastUnderscore = suiteName.lastIndexOf('_');
                String moduleName = lastUnderscore > 0
                        ? suiteName.substring(0, lastUnderscore) + "." + suiteName.substring(lastUnderscore + 1)
                        : "default." + suiteName;
                module.setName(moduleName);

                int totalTests = 0;
                int failCount = 0;
                int skipCount = 0;
                long totalDurationMs = 0;

                for (String testJson : tests) {
                    TestCase tc = new TestCase();
                    String name = extractString(testJson, "\"name\"", null);
                    String status = extractString(testJson, "\"status\"", null);
                    long duration = extractLong(testJson, "\"duration\"");

                    tc.setName(name != null ? name : "Unnamed");
                    tc.setClassName(suiteName);
                    tc.setModuleName(moduleName);
                    tc.setExecutionTime(String.valueOf(duration / 1000.0));
                    totalDurationMs += duration;
                    totalTests++;

                    if ("failed".equals(status)) {
                        tc.setVerdict(TestCaseVerdict.e_FAIL);
                        failCount++;
                    } else if ("skipped".equals(status)) {
                        tc.setVerdict(TestCaseVerdict.e_IGNORED);
                        skipCount++;
                    } else {
                        tc.setVerdict(TestCaseVerdict.e_PASS);
                    }
                    module.addTestCase(tc);
                }

                module.setNoTestCases(String.valueOf(totalTests));
                module.setNoFailures(String.valueOf(failCount));
                module.setNoErrors("0");
                module.setNoIgnored(String.valueOf(skipCount));
                module.setExecutionTime(String.valueOf(totalDurationMs / 1000.0));

                if (folderDate != null) {
                    module.setDate(folderDate);
                } else {
                    module.setDate(file.lastModified());
                }

                module.calcStats();

                String packageName = module.getPackageName();
                TestPackage tp = packages.get(packageName);
                if (tp == null) {
                    tp = new TestPackage();
                    tp.setName(packageName);
                    packages.put(packageName, tp);
                }
                tp.addTestModule(module);
            }

        } catch (Exception e) {
            System.err.println("Error parsing CTRF file '" + file.getAbsolutePath() + "': " + e.getMessage());
        }
    }

    private static String parseFolderDate(String folderName) {
        SimpleDateFormat[] formats = {
            new SimpleDateFormat("yyyy-MM-dd'T'HHmmss'Z'", Locale.US),
            new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US),
            new SimpleDateFormat("yyyy-MM-dd'T'HHmmss", Locale.US),
            new SimpleDateFormat("yyyy-MM-dd_HHmmss", Locale.US)
        };
        for (SimpleDateFormat fmt : formats) {
            fmt.setLenient(false);
            try {
                Date d = fmt.parse(folderName);
                return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(d);
            } catch (Exception e) { /* try next */ }
        }
        return null;
    }

    private String readFile(File file) throws IOException {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }
        }
        return sb.toString();
    }

    private String extractString(String json, String key, String context) {
        int keyIdx = -1;
        if (context != null) {
            int ctxIdx = json.indexOf("\"" + context + "\"");
            if (ctxIdx >= 0) {
                keyIdx = json.indexOf(key, ctxIdx);
            }
        }
        if (keyIdx < 0) {
            keyIdx = json.indexOf(key);
        }
        if (keyIdx < 0) return null;

        int colonIdx = json.indexOf(':', keyIdx + key.length());
        if (colonIdx < 0) return null;

        int startQuote = json.indexOf('"', colonIdx + 1);
        if (startQuote < 0) return null;

        int endQuote = json.indexOf('"', startQuote + 1);
        while (endQuote > 0 && json.charAt(endQuote - 1) == '\\') {
            endQuote = json.indexOf('"', endQuote + 1);
        }
        if (endQuote < 0) return null;

        return json.substring(startQuote + 1, endQuote);
    }

    private int extractInt(String json, String key) {
        int keyIdx = json.indexOf(key);
        if (keyIdx < 0) return 0;
        int colonIdx = json.indexOf(':', keyIdx + key.length());
        if (colonIdx < 0) return 0;

        int start = colonIdx + 1;
        while (start < json.length() && (json.charAt(start) == ' ' || json.charAt(start) == '\t')) {
            start++;
        }
        int end = start;
        while (end < json.length() && (Character.isDigit(json.charAt(end)) || json.charAt(end) == '-')) {
            end++;
        }
        if (start == end) return 0;
        try {
            return Integer.parseInt(json.substring(start, end));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private long extractLong(String json, String key) {
        int keyIdx = json.indexOf(key);
        if (keyIdx < 0) return 0;
        int colonIdx = json.indexOf(':', keyIdx + key.length());
        if (colonIdx < 0) return 0;

        int start = colonIdx + 1;
        while (start < json.length() && (json.charAt(start) == ' ' || json.charAt(start) == '\t')) {
            start++;
        }
        int end = start;
        while (end < json.length() && (Character.isDigit(json.charAt(end)) || json.charAt(end) == '-')) {
            end++;
        }
        if (start == end) return 0;
        try {
            return Long.parseLong(json.substring(start, end));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private String extractTestsArray(String json) {
        int idx = json.indexOf("\"tests\"");
        while (idx >= 0) {
            int colonIdx = json.indexOf(':', idx + 7);
            if (colonIdx < 0) return null;
            int bracketIdx = -1;
            for (int i = colonIdx + 1; i < json.length(); i++) {
                char c = json.charAt(i);
                if (c == ' ' || c == '\t' || c == '\n' || c == '\r') continue;
                if (c == '[') {
                    bracketIdx = i;
                }
                break;
            }
            if (bracketIdx >= 0) {
                int depth = 1;
                int end = bracketIdx + 1;
                while (end < json.length() && depth > 0) {
                    char c = json.charAt(end);
                    if (c == '[') depth++;
                    else if (c == ']') depth--;
                    end++;
                }
                return json.substring(bracketIdx + 1, end - 1);
            }
            idx = json.indexOf("\"tests\"", idx + 7);
        }
        return null;
    }

    private ArrayList<String> splitTestObjects(String arrayContent) {
        ArrayList<String> objects = new ArrayList<>();
        int depth = 0;
        int start = -1;
        for (int i = 0; i < arrayContent.length(); i++) {
            char c = arrayContent.charAt(i);
            if (c == '{') {
                if (depth == 0) start = i;
                depth++;
            } else if (c == '}') {
                depth--;
                if (depth == 0 && start >= 0) {
                    objects.add(arrayContent.substring(start, i + 1));
                    start = -1;
                }
            }
        }
        return objects;
    }

    @Override
    public void startElement(String uri, String localName, String qName, org.xml.sax.Attributes attributes) {}

    @Override
    public void endElement(String uri, String localName, String qName) {}
}
