package thierfelder.alexander;


import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.*;

public class Database {
    private static List<Document> documents = new LinkedList<>();
    private static LinkedList<Document> documentUsage = new LinkedList<>();
    private static Set<String> tags = new HashSet<>();

    static void load() throws IOException{
        File documentDir = new File(Paths.get(Utils.homeDir, "documents").toString());
        if(!documentDir.exists() && !documentDir.mkdir()){
            throw new IOException("Cannot create " + documentDir.getAbsolutePath());
        }
        int highestID = 0;
        File[] documents = documentDir.listFiles();
        for(File documentFile : documents){
            String[] lines = Utils.readFromFile(documentFile.getAbsolutePath()).split(System.lineSeparator());
            String title = lines[0];
            int documentID = Integer.valueOf(Utils.getFileName(documentFile));
            if(documentID > highestID){
                highestID = documentID;
            }
            List<String> tags = new LinkedList<>(Arrays.asList(lines[1].split(",")));
            Database.tags.addAll(tags);
            StringBuilder builder = new StringBuilder();
            for(int i = 2; i < lines.length; i++){
                builder.append(lines[i]);
                builder.append(System.lineSeparator());
            }
            String content = builder.toString();
            Document document = new Document(title, content, tags, documentID);
            Database.documents.add(document);
        }
        Document.setNextID(highestID + 1);
    }
    static void save() throws IOException{
        for(Document document : documents){
            if(document.changed()){
                File file = new File(Paths.get(Utils.homeDir, "documents", String.valueOf(document.ID)).toString());
                if(file.exists() && !file.delete()){
                    throw new IOException("Could not replace " + file.getAbsolutePath());
                }
                StringBuilder builder = new StringBuilder();
                builder.append(document.getTitle());
                builder.append(System.lineSeparator());
                List<String> tags = document.getTags();
                for(int i = 0; i + 1 < tags.size(); i++){
                    builder.append(tags.get(i));
                    builder.append(",");
                }
                builder.append(tags.get(tags.size() - 1));
                builder.append(System.lineSeparator());
                builder.append(document.getContent());
                Utils.writeToFile(file.getAbsolutePath(), builder.toString());
            }
        }
    }
    static void addDocument(Document document){
        if(!documents.contains(document)){
            documents.add(document);
        }
    }
    static void documentVisited(Document document){
        if(documentUsage.contains(document)){
            documentUsage.remove(document);
        }

        if(documentUsage.size() > 100){
            for(int i = documentUsage.size() - 1; i >= 100; i--){
                documentUsage.remove(i);
            }
        }
        documentUsage.add(0, document);
    }
    static Document getDocument(int id) {
        for(Document d : documents){
            if(d.ID == id){
                return d;
            }
        }
        return null;
    }
    static void removeDocument(int id) throws IOException{
        Document document = getDocument(id);
        if(document != null){
            documents.remove(document);
            documentUsage.remove(document);
            File docResources = new File(Paths.get(Utils.homeDir, "resources", Integer.toString(id)).toString());
            if(docResources.exists() && !Utils.deleteDirectory(docResources)){
                throw new IOException("Unable to delete " + docResources.getAbsolutePath());
            }
            File documentFile = new File(Paths.get(Utils.homeDir, "documents", Integer.toString(id)).toString());
            if(documentFile.exists() && !documentFile.delete()){
                throw new IOException("Unable to delete " + documentFile.getAbsolutePath());
            }
        }
    }

    static List<Document> searchByTags(List<String> tags, int results){
        Map<Integer, Float> resultMap = new HashMap<>();
        for(Document doc : documents){
            if(doc.isN() && Utils.hideN){
                continue;
            }
            resultMap.put(doc.ID, doc.getTagCoverage(tags));
        }
        resultMap = Utils.sortMapByValue(resultMap);
       return mapToShortList(resultMap, results);
    }
    static List<Document> searchContent(String query, int results){
        return searchDocuments(query.toLowerCase(), results, true);
    }
    static List<Document> searchTitle(String query, int results){
        return searchDocuments(query.toLowerCase(), results, false);
    }
    private static List<Document> searchDocuments(String query, int results, boolean searchContent){
        String[] terms = query.split(" ");
        Map<Integer, Double> tfIdfValues = new HashMap<>();
        for(Document d : documents){
            if(d.isN() && Utils.hideN){
                continue;
            }
            tfIdfValues.put(d.ID, 0.0);
        }
        for(String term : terms){
            for(Document d : documents){
                if(d.isN() && Utils.hideN){
                    continue;
                }
                tfIdfValues.put(d.ID, tfIdfValues.get(d.ID) + d.tfIdf(term, searchContent));
            }
        }
        tfIdfValues = Utils.sortMapByValue(tfIdfValues);
        return mapToShortList(tfIdfValues, results);
    }
    private static <V> List<Document> mapToShortList(Map<Integer,V> map, int results){
        List<Document> result = new LinkedList<>();
        for (Map.Entry<Integer, V> entry : map.entrySet()) {
            result.add(getDocument(entry.getKey()));
            if(result.size() == results){
                break;
            }
        }
        return result;
    }
    static double inverseDocumentFrequency(String term, boolean searchContent){
        int documentFrequency = 0;
        for(Document d : documents){
            String toSearch = d.getTitle();
            if(searchContent){
                toSearch = d.getContent();
            }
            if(toSearch.contains(term)){
                documentFrequency++;
            }
        }
        if(documentFrequency == 0){
            return 0;
        }
        return Math.log((double) documents.size()/(double) documentFrequency);
    }
    static String buildResultsPage(List<Document> searchResults, String query){
        String result = Utils.getFileContent("resultsHtmlDoc");
        StringBuilder searchResultsBuilder = new StringBuilder();
        for(Document document : searchResults){
            searchResultsBuilder.append(document.renderPreview());
            searchResultsBuilder.append(System.lineSeparator());
            searchResultsBuilder.append("<br>");
            searchResultsBuilder.append(System.lineSeparator());
        }
        result = result.replace("[searchResults]", searchResultsBuilder.toString());
        result = result.replaceFirst("\\[query\\]", Utils.encodeHTML(query));
        return result;
    }

    static String buildMainPage(){
        List<Document> recentDocuments = getRecentDocuments(7);
        String result = Utils.getFileContent("mainHtmlDoc");
        StringBuilder recommendationsBuilder = new StringBuilder();
        for(Document d : recentDocuments){
            recommendationsBuilder.append(d.renderPreview());
            recommendationsBuilder.append(System.lineSeparator());
            recommendationsBuilder.append("<br>");
        }
        result = result.replace("[Document recommendations]", recommendationsBuilder.toString());
        return result;
    }
    static String buildNewDocumentPage(String sourceDestination){
        String result = Utils.getFileContent("newDocumentHtmlDoc");
        return result.replaceAll("\\[source destination\\]", sourceDestination);
    }

    private static List<Document> getRecentDocuments(int count){

        LinkedList<Integer> ids = new LinkedList<>();

        for(Document d : documentUsage){

            if(d.isN() && Utils.hideN){
                continue;
            }

            if(ids.size() >= count){
                break;
            }

            ids.add(d.ID);
        }

        for(int i = Document.getNextID(); i >= 0; i--){

            if(ids.size() >= count){
                break;
            }

            Document d = Database.getDocument(i);

            if(d == null || ids.contains(d.ID) || (d.isN() && Utils.hideN)){
                continue;
            }

            ids.add(d.ID);

        }

        LinkedList<Document> result = new LinkedList<>();

        for(int id : ids){
            result.add(Database.getDocument(id));
        }

        return result;
    }
}
