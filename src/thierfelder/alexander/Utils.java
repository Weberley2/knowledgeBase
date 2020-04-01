package thierfelder.alexander;

import java.io.*;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Utils {
    static boolean debug = false;
    static final String homeDir = Main.homeDir;
    static int specifiedServerPort = -1;
    static boolean hideN = false;
    static String keystorePath = Paths.get("/", "home", "pi", "TelegramServer", "Keys", "kb_keystore.p12").toString();

    static final private String loginHtmlDoc = readFromFile(Paths.get(homeDir, "login.html").toString());
    static final private String cssFile = readFromFile(Paths.get(homeDir, "mini-default.min.css").toString());
    static final private String baseHtmlDoc = readFromFile(Paths.get(homeDir, "documentBase.html").toString());
    static final private String baseDocumentPreviewDiv = readFromFile(Paths.get(homeDir, "documentPreviewDivBase").toString());
    static final private String mainHtmlDoc = readFromFile(Paths.get(homeDir, "knowledgeMain.html").toString());
    static final private String newDocumentHtmlDoc = readFromFile(Paths.get(homeDir, "newDocument.html").toString());
    static final private String resultsHtmlDoc = readFromFile(Paths.get(homeDir, "results.html").toString());

    static void setDebug(){
        debug = true;
        keystorePath = "/home/alexander/keys/kb_keystore.p12";
    }

    static String getFileContent(String file){
        String result;
        switch (file){
            case "loginHtmlDoc":
                if(debug)
                    result = readFromFile(Paths.get(homeDir, "login.html").toString());
                else
                    result = loginHtmlDoc;
                break;
            case "cssFile":
                if(debug)
                    result = readFromFile(Paths.get(homeDir, "mini-default.min.css").toString());
                else
                    result = cssFile;
                break;
            case "baseHtmlDoc":
                if(debug)
                    result = readFromFile(Paths.get(homeDir, "documentBase.html").toString());
                else
                    result = baseHtmlDoc;
                break;
            case "baseDocumentPreviewDiv":
                if(debug)
                    result = readFromFile(Paths.get(homeDir, "documentPreviewDivBase").toString());
                else
                    result = baseDocumentPreviewDiv;
                break;
            case "mainHtmlDoc":
                if(debug)
                    result = readFromFile(Paths.get(homeDir, "knowledgeMain.html").toString());
                else
                    result = mainHtmlDoc;
                break;
            case "newDocumentHtmlDoc":
                if(debug)
                    result = readFromFile(Paths.get(homeDir, "newDocument.html").toString());
                else
                    result = newDocumentHtmlDoc;
                break;
            case "resultsHtmlDoc":
                if(debug)
                    result = readFromFile(Paths.get(homeDir, "results.html").toString());
                else
                    result = resultsHtmlDoc;
                break;
            default:
                throw new IllegalArgumentException("\"" + file + "\" does not exist.");
        }
        return result;
    }

    static final String escapeStart = "[[";
    static final String escapeEnd = "]]";
    static final String authCookie = "5069551";
    static final Map<String, String> colors = new HashMap<String, String>() {{
        put("blue", "#0066cc");
        put("red", "#EE2C2C");
        put("green", "#008B00");
    }};
    static String stripEscapes(String toStrip){
        if(toStrip.startsWith(escapeStart)){
            toStrip = toStrip.substring(escapeStart.length());
        }
        if(toStrip.endsWith(escapeEnd)){
            toStrip = toStrip.substring(0, toStrip.length() - escapeEnd.length());
        }
        return toStrip;
    }
    static void writeToFile(String path, String content) throws IOException
    {
        BufferedWriter writer = new BufferedWriter(new FileWriter(path));
        writer.write(content);
        writer.close();
    }
    static String readFromFile(String path){
        File file = new File(path);
        StringBuilder builder = new StringBuilder();
        try {
            BufferedReader br = new BufferedReader(new FileReader(file));
            String st;
            while ((st = br.readLine()) != null ) {
                builder.append(st);
                builder.append(System.lineSeparator());
            }
        }
        catch (IOException e){
            System.out.println(e.getMessage());
        }
        return builder.toString();
    }
    static String getFileName(File file){
        String[] split = file.getAbsolutePath().split("/");
        return split[split.length - 1];
    }
    static <K, V extends Comparable<? super V>> Map<K, V> sortMapByValue(Map<K, V> map) {
        List<Map.Entry<K, V>> list = new LinkedList<>(map.entrySet());
        list.sort(Map.Entry.comparingByValue());
        Collections.reverse(list);
        Map<K, V> result = new LinkedHashMap<>();
        for (Map.Entry<K, V> entry : list) {
            result.put(entry.getKey(), entry.getValue());
        }
        return result;
    }
    static int count(String text, String find) {
        int index = 0, count = 0, length = find.length();
        while( (index = text.indexOf(find, index)) != -1 ) {
            index += length; count++;
        }
        return count;
    }
    static boolean deleteDirectory(File directory) throws IOException{
        String path = directory.getAbsolutePath();
        if(!path.startsWith(Utils.homeDir)){
            throw new IOException("Tried to delete files outside the project directory: " + path);
        }
        if(directory.exists()){
            File[] files = directory.listFiles();
            if(null!=files){
                for(int i=0; i<files.length; i++) {
                    if(files[i].isDirectory()) {
                        deleteDirectory(files[i]);
                    }
                    else {
                        files[i].delete();
                    }
                }
            }
        }
        return(directory.delete());
    }
    static boolean isInteger(String s) {
        try {
            Integer.parseInt(s);
        } catch(Exception e) {
            return false;
        }
        return true;
    }

    static String encodeHTML(String string) {
        string = string.replaceAll(Pattern.quote("&"), Matcher.quoteReplacement("&amp;"));
        string = string.replaceAll(Pattern.quote("<"), Matcher.quoteReplacement("&lt;"));
        string = string.replaceAll(Pattern.quote(">"), Matcher.quoteReplacement("&gt;"));
        string = string.replaceAll(Pattern.quote("'"), Matcher.quoteReplacement("&#39;"));
        string = string.replaceAll(Pattern.quote("\""), Matcher.quoteReplacement("&quot;"));
        return string;
    }
}
