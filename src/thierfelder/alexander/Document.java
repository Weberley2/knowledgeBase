package thierfelder.alexander;


import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;

import org.commonmark.Extension;
import org.commonmark.node.*;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.Renderer;
import org.commonmark.renderer.html.HtmlRenderer;
import org.commonmark.ext.gfm.tables.TablesExtension;

public class Document {
    private List<String> tags;
    private boolean n;
    private String title;
    private String content;
    int ID;
    private boolean changed;
    private static int nextID = 0;
    private static Parser markdownParser;
    private static Renderer htmlRenderer;
    static {
        List<Extension> extensions = Arrays.asList(TablesExtension.create());
        markdownParser = Parser.builder().extensions(extensions).build();
        htmlRenderer = HtmlRenderer.builder().extensions(extensions).build();
    }
    static int createID(){
        return nextID++;
    }

    static void setNextID(int nextID){
        Document.nextID = nextID;
    }
    static int getNextID(){
        return nextID;
    }

    Document(String title, String content, List<String> tags){
        this(title, content, tags, createID());
        // Does currently not work
        //this.replaceImages();
        this.changed = true;
    }
    Document(String title, String content, List<String> tags, int id){
        this.title = title;
        this.content = content;
        this.tags = tags;
        this.ID = id;
        this.n = calculateN();
        this.changed = false;
    }
    float getTagCoverage(List<String> tags){
        if(this.tags.isEmpty()){
            return 0f;
        }
        int hits = 0;
        for(String tag : tags){
            if(this.tags.contains(tag.toLowerCase())){
                hits++;
            }
        }
        return (float)hits/(float)tags.size();
    }

    private boolean calculateN(){
        boolean n = false;

        for(String tag : tags){
            if(tag.toLowerCase().equals("n")){
                n = true;
            }
        }

        return n;
    }

    boolean changed(){
        return this.changed;
    }
    String getTitle(){
        return title;
    }
    void setTitle(String title){
        this.title = title;
        this.changed = true;
    }
    List<String> getTags(){
        return tags;
    }
    void setTags(List<String> tags){
        this.tags = tags;
        this.n = calculateN();
        this.changed = true;
    }
    String getContent(){
        return content;
    }
    void setContent(String content){
        this.content = content;
        this.changed = true;
    }

    boolean isN(){
        return this.n;
    }

    void replaceImages() throws IOException{
        int nextReplacement = content.indexOf(Utils.escapeStart);
        while (nextReplacement != -1){
            int endOfReplacement = content.indexOf(Utils.escapeEnd, nextReplacement);
            if(endOfReplacement < nextReplacement){
                break;
            }
            String subString = content.substring(nextReplacement + Utils.escapeStart.length(), endOfReplacement);
            if(Regex.isPath(subString)){
                File originalFile = new File(subString);
                String fileName = subString.substring(subString.lastIndexOf(File.separator) + 1);
                File directory = new File(Paths.get(Utils.homeDir, "resources", Integer.toString(ID)).toString());
                File newFile = new File(Paths.get(directory.getAbsolutePath(), fileName).toString());
                if(!directory.exists() && !directory.mkdir()){
                    throw new IOException("Could not create " + directory.getAbsolutePath());
                }
                if(originalFile.exists() && !newFile.exists()){
                    Files.copy(originalFile.toPath(), newFile.toPath());
                }
                content = content.substring(0, nextReplacement + Utils.escapeStart.length()) +
                        newFile.getAbsolutePath() +
                        content.substring(endOfReplacement);
                changed = true;
                int lengthDifference = subString.length() - newFile.getAbsolutePath().length();
                endOfReplacement -= lengthDifference;
            }
            nextReplacement = content.indexOf(Utils.escapeStart, endOfReplacement);
        }
    }
    private String renderContent(boolean renderPreview){
        String renderedContent = Regex.replaceHeadlines(this.content, renderPreview);
        renderedContent = Regex.replaceColors(renderedContent);
        if(renderPreview) {
            while (renderedContent.startsWith("<br>")){
                renderedContent = renderedContent.substring("<br>".length());
            }
            while (renderedContent.startsWith("\r") || renderedContent.startsWith("\n")){
                renderedContent = renderedContent.substring("\n".length());
            }
            renderedContent = renderedContent.replaceAll("(" + System.lineSeparator() + "|\r)+", "<br>");
            renderedContent = renderedContent.replaceAll("<br>(<br>|" + System.lineSeparator() + "|\r)+", "<br>");
        }
        else {
            Node document = markdownParser.parse(renderedContent);
            renderedContent = htmlRenderer.render(document);

        }


        return renderedContent;
    }
    String renderEdit(String sourceDestination){
        String result = Utils.getFileContent("newDocumentHtmlDoc");
        result = result.replaceFirst("\\[source destination\\]", sourceDestination);
        result = result.replaceFirst("createDocument", "editDocument");
        result = result.replaceFirst("New Document", "Edit Document");
        result = result.replaceFirst("placeholder=\"Content\">", Matcher.quoteReplacement("placeholder=\"Content\">\n" + Utils.encodeHTML(content)));
        String tagString = "";
        for(String tag : tags){
            tagString += tag + " ";
        }
        tagString = Utils.encodeHTML(tagString.trim());
        result = result.replaceFirst("placeholder=\"Tags\"", "placeholder=\"Tags\" value=\"" + tagString + "\"");
        result = result.replaceFirst("placeholder=\"Title\"", Matcher.quoteReplacement("placeholder=\"Title\" value=\"" + Utils.encodeHTML(title) + "\""));
        return result;
    }
    String render(){
        String content = renderContent(false);
        String title = "<h1>" + this.title + "</h1>";
        String result = Utils.getFileContent("baseHtmlDoc");
        result = result.replace("[id]", Integer.toString(ID));
        int titleDifference = "[title]".length() - title.length();
        int contentIndex = result.indexOf("[content]") - titleDifference;
        result = result.replace("[title]", title);
        result = result.substring(0, contentIndex) + content + result.substring(contentIndex + "[content]".length());

        return result;
    }

    String renderPreview(){
        String content = renderContent(true);
        while (content.endsWith("<br>")){
            content = content.substring(0, content.length() - "<br>".length());
        }
        content = content.replaceAll("(<br>)+", "<br>");
        content = content.replaceAll("<img src=.*?>", "");
        int virtualIndex = 0, realIndex = 0;
        char currentChar;
        boolean addBr = false;
        int brLength = 500/3;
        boolean count = true;
        while(virtualIndex < 500 && realIndex < content.length()){
            currentChar = content.charAt(realIndex);
            if(currentChar == '<'){
                if(content.substring(realIndex).startsWith("<br>")){
                    addBr = true;
                }
                count = false;
            }
            else if(currentChar =='>'){
                if(addBr){
                    virtualIndex += brLength;
                    addBr = false;
                }
                count = true;
            }
            if(count){
                virtualIndex++;
            }
            realIndex++;
        }
        content = content.substring(0, realIndex);

        content = "<p>" + content + "</p>";
        String tags = "";
        for(String tag : this.tags){
            tags += tag + " ";
        }
        tags = tags.substring(0, tags.length() - 1);
        String result = Utils.getFileContent("baseDocumentPreviewDiv");
        result = result.replaceAll("\\[id\\]", String.valueOf(ID));
        result = result.replaceAll("\\[tags\\]", tags);
        result = result.replaceAll("\\[title\\]", title);
        result = result.replaceAll("\\[content\\]", Matcher.quoteReplacement(content));
        return result;
    }
    private double termFrequency(String term, boolean searchContent){
        String toSearch = title;
        if(searchContent){
            toSearch = content;
        }
        return (double) Utils.count(toSearch, term) / (double) toSearch.split(" ").length;
    }
    double tfIdf(String term, boolean searchContent){
        return termFrequency(term, searchContent) * Database.inverseDocumentFrequency(term, searchContent);
    }
}
