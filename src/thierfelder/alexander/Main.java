package thierfelder.alexander;

public class Main {
    static String homeDir;
    public static void main(String[] args) throws Exception {
        homeDir = getHomeDir(args);
        parseArgs(args);
        Database.load();
        HTTPSServer.start();
    }

    private static String getHomeDir(String[] args){
        String result = System.getProperty("user.dir");
        for(int i = 0; i < args.length; i++){
            if(args[i].equals("--dir")){
                result = args[i + 1];
            }
        }
        return result;
    }
    private static void parseArgs(String[] args){
        for(int i = 0; i < args.length; i++){
            if(args[i].equals("--debug")){
                Utils.setDebug();
            }
            if(args[i].equals("--port")){
                try {
                    Utils.specifiedServerPort = Integer.parseInt(args[i + 1]);
                }
                catch (Exception e){
                    System.out.println("Could not parse port.");
                }
            }
            if(args[i].equals("--n")){
                try {
                    Utils.hideN = !Boolean.parseBoolean(args[i + 1]);
                }
                catch (Exception e){
                    System.out.println("Could not parse boolean.");
                }
            }
            if(args[i].equals("--keystore")){
                Utils.keystorePath = args[i + 1];
            }
        }
    }
}
