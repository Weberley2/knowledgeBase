package thierfelder.alexander;

import com.sun.net.httpserver.*;

import javax.net.ssl.*;
import java.io.*;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyStore;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class HTTPSServer {
    private static int port;
    private static HttpsServer server;

    static void start() throws Exception {
        port = 31286;

        if(Utils.specifiedServerPort > 0){
            port = Utils.specifiedServerPort;
        }

        if(server != null)
            throw new IOException("Server is already running. Cant start it twice.");
        char[] storepass = "".toCharArray();
        char[] keypass = "".toCharArray();
        FileInputStream fIn = new FileInputStream(Utils.keystorePath);
        KeyStore keystore = KeyStore.getInstance("JKS");
        keystore.load(fIn, storepass);
        KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
        kmf.init(keystore, keypass);
        TrustManagerFactory tmf = TrustManagerFactory.getInstance("SunX509");
        tmf.init(keystore);
        server = HttpsServer.create(new InetSocketAddress(port), 0);
        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);
        server.setHttpsConfigurator(new HttpsConfigurator(sslContext) {
            public void configure(HttpsParameters params) {
                try {
                    // initialise the SSL context
                    SSLContext c = SSLContext.getDefault();
                    SSLEngine engine = c.createSSLEngine();
                    params.setNeedClientAuth(false);
                    params.setCipherSuites(engine.getEnabledCipherSuites());
                    params.setProtocols(engine.getEnabledProtocols());
                    // get the default parameters
                    SSLParameters defaultSSLParameters = c.getDefaultSSLParameters();
                    params.setSSLParameters(defaultSSLParameters);
                } catch (Exception ex) {
                    System.out.println("Failed to create HTTPS server");
                }
            }
        });
        server.createContext( "/", new KB_Handler() );
        server.setExecutor(null);
        server.start();
        System.out.println("Server started at https://127.0.0.1:" + port);
    }

    static void stop() {
        server.stop(0);
    }

    static class KB_Handler implements HttpHandler {
        @Override
        public void handle(HttpExchange t) {
            //System.out.println(t.getRequestMethod() + ", " + t.getRequestURI().toString());
            Map<String, String> params = null;
            try {
                if (t.getRequestMethod().equals("POST")) {
                    params = readPostParams(t);
                    handlePOST(params);
                }

                byte[] responseBytes;
                String uri = t.getRequestURI().toString();
                boolean authenticated = isAuthenticated(t);

                if (uri.endsWith(".js")) {
                    File fi = new File(Paths.get(Utils.homeDir, "src" + uri).toString());
                    if (!fi.exists()) {
                        return;
                    }
                    responseBytes = Files.readAllBytes(fi.toPath());
                } else if (uri.equals("/mini-default.min.css")) {
                    responseBytes = Utils.getFileContent("cssFile").getBytes();
                } else if (!authenticated) {
                    if (params != null && params.get("password") != null && checkPassword(params.get("password"))) {
                        setCookie(t);
                        responseBytes = Database.buildMainPage().getBytes();
                    }
                    else {
                        responseBytes = Utils.getFileContent("loginHtmlDoc").getBytes();
                    }
                } else if (uri.equals("/new")) {
                    String sourceDest = getSourceDestination(params);
                    responseBytes = Database.buildNewDocumentPage(sourceDest).getBytes();
                } else if (uri.startsWith("/results")) {
                    String query = "";
                    String shortQuery = "";
                    String[] querySplit = uri.split("\\?", 2);
                    if (querySplit.length > 1) {
                        query = URLDecoder.decode(querySplit[1], "UTF-8");
                        if (query.length() > 1) {
                            shortQuery = query.substring(1, query.length() - 1);
                        }
                    }
                    int numResults = 10;
                    List<Document> results;
                    if (query.startsWith("\"") && query.endsWith("\"")) {
                        results = Database.searchTitle(shortQuery, numResults);
                    } else if (query.startsWith("'") && query.endsWith("'")) {
                        results = Database.searchContent(shortQuery, numResults);
                    } else {
                        List<String> tags = new LinkedList<>(Arrays.asList(query.split(" ")));
                        results = Database.searchByTags(tags, numResults);
                    }
                    responseBytes = Database.buildResultsPage(results, query).getBytes();
                } else if (uri.startsWith("/edit/")) {
                    String sourceDest = getSourceDestination(params);
                    String[] split = uri.split("/");
                    int id = Integer.valueOf(split[split.length - 1]);
                    Document document = Database.getDocument(id);

                    if (document == null) {
                        return;
                    }

                    if(document.isN() && Utils.hideN){
                        return;
                    }

                    responseBytes = document.renderEdit(sourceDest).getBytes();
                } else if (uri.equals("/nextdocumentid")) {
                    responseBytes = ("<id>" + String.valueOf(Document.getNextID()) + "</id>").getBytes();
                } else if (uri.endsWith(".png") || uri.endsWith(".jpg") || uri.endsWith(".ico")) {
                    File fi = new File(Paths.get(Utils.homeDir, "resources" + uri).toString());
                    if (!fi.exists()) {
                        return;
                    }
                    responseBytes = Files.readAllBytes(fi.toPath());
                } else if (Utils.isInteger(uri.substring(1))) {
                    int id = Integer.parseInt(uri.substring(1));
                    Document document = Database.getDocument(id);

                    if (document == null) {
                        return;
                    }

                    if(document.isN() && Utils.hideN){
                        return;
                    }

                    Database.documentVisited(document);
                    responseBytes = document.render().getBytes();
                } else if (uri.equals("/stop")) {
                    server.stop(0);
                    responseBytes = new byte[0];
                } else {
                    responseBytes = Database.buildMainPage().getBytes();
                }
                t.sendResponseHeaders(200, 0);
                BufferedOutputStream out = new BufferedOutputStream(t.getResponseBody());
                ByteArrayInputStream bis = new ByteArrayInputStream(responseBytes);

                byte[] buffer = new byte[20000];
                int count;
                while ((count = bis.read(buffer)) != -1) {
                    out.write(buffer, 0, count);
                }
                bis.close();
                out.close();
            } catch (Exception e) {
                System.out.println("Exception:" + e.getMessage());
            }
        }

        private Map<String, String> readPostParams(HttpExchange exchange) throws IOException {
            InputStream in = exchange.getRequestBody();
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte buf[] = new byte[4096];
            for (int n = in.read(buf); n > 0; n = in.read(buf)) {
                out.write(buf, 0, n);
            }
            String qry = new String(out.toByteArray());
            in.close();

            String[] querySplit = qry.split("&");
            Map<String, String> result = new HashMap<>();
            for (String param : querySplit) {
                String[] paramSplit = param.split("=");
                String key = URLDecoder.decode(paramSplit[0], "UTF-8");
                String value = "";
                if (paramSplit.length > 1) {
                    value = URLDecoder.decode(paramSplit[1], "UTF-8");
                }
                result.put(key, value);
            }
            return result;
        }

        private void handlePOST(Map<String, String> params) throws IOException {
            String action = params.get("action");
            if (action == null) {
                return;
            }
            if (action.equals("createDocument")) {
                String title = params.get("title");
                String content = params.get("content");
                String tagString = params.get("tags");
                List<String> tags = extractTags(tagString);
                Document document = new Document(title, content, tags);
                Database.addDocument(document);
                Database.save();
            } else if (action.equals("editDocument")) {
                String title = params.get("title");
                String content = params.get("content");
                String tagString = params.get("tags");
                List<String> tags = extractTags(tagString);
                int id = Integer.valueOf(params.get("id"));
                Document document = Database.getDocument(id);
                if (document == null) {
                    return;
                }
                document.setTitle(title);
                document.setTags(tags);
                document.setContent(content);
                Database.save();
            } else if (action.equals("deleteDocument")) {
                int id = Integer.valueOf(params.get("id"));
                Database.removeDocument(id);
            }
        }

        private String getSourceDestination(Map<String, String> params) throws IOException {
            String sourceDest = "/";
            if (params != null) {
                sourceDest = params.get("source");
                if (sourceDest != null && !sourceDest.isEmpty()) {
                    sourceDest = sourceDest.split(String.valueOf(port))[1];
                }
            }
            return sourceDest;
        }

        private List<String> extractTags(String tagString) {
            String magicString = "___}____12___palim_palim____";
            tagString = tagString.replaceAll(",", " ");
            int nextQuote = tagString.indexOf("\"");
            while (nextQuote > 0) {
                int endQuote = tagString.indexOf("\"", nextQuote + 1);
                if (endQuote < 0) {
                    break;
                }
                String quoted = tagString.substring(nextQuote + 1, endQuote).replaceAll(" ", magicString);
                tagString = tagString.substring(0, nextQuote) + quoted + tagString.substring(endQuote + 1);
                nextQuote = tagString.indexOf("\"", nextQuote);
            }
            List<String> result = new LinkedList<>();
            String[] tagSplit = tagString.split(" ");
            for (String tag : tagSplit) {
                result.add(tag.replaceAll(magicString, " "));
            }
            return result;
        }

        private void setCookie(HttpExchange t) {
            Headers respHeaders = t.getResponseHeaders();
            List<String> values = new ArrayList<>();
            OffsetDateTime quiteAWhile = OffsetDateTime.now().plus(Duration.ofDays(365 * 5));
            String cookieExpires = "expires=" + DateTimeFormatter.RFC_1123_DATE_TIME.format(quiteAWhile) + ";";
            values.add("auth=" + Utils.authCookie + "; version=1; Path=/; " + cookieExpires);
            respHeaders.put("Set-Cookie", values);
        }

        private boolean isAuthenticated(HttpExchange t) {
            List<String> cookies = t.getRequestHeaders().get("Cookie");
            if (cookies == null || cookies.size() != 1) {
                return false;
            }
            String cookie = cookies.get(0);
            return cookie.equals("auth=" + Utils.authCookie);
        }
        private boolean checkPassword(String password){
            return password.equals("knowledgebase");
        }
    }

}

