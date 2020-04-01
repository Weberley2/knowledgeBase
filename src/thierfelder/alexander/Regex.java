package thierfelder.alexander;

import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class Regex {
    static boolean isUrl(String potentialUrl){
        return potentialUrl.matches("(?:http(s)?:\\/\\/)?[\\w.-]+(?:\\.[\\w\\.-]+)+[\\w\\-\\._~:/?#\\[\\]@!\\$&'\\(\\)\\*\\+,;=.]");
    }
    static boolean isPath(String potentialPath){
        return potentialPath.matches("^(/[^/ ]*)+/?$");
    }
    static String replaceColors(String text){
        List<String> matches = findMatches("\\[[^\\[]*\\]\\[(green|red|blue)\\]", text);
        for (String match : matches){
            String color = match.substring(match.lastIndexOf("[") + 1, match.length() - 1);
            String toReplace = match.substring(0, match.length() - 2);
            toReplace = toReplace.substring(1, toReplace.lastIndexOf("]"));
            String replacement = "<font color=\"" + Utils.colors.get(color) + "\">" + Utils.stripEscapes(toReplace) + "</font>";
            text = text.replace(match, Matcher.quoteReplacement(replacement));
        }
        return text;
    }

    static String replaceHeadlines(String text, boolean removeHeadlines){
        List<String> matches = findMatches("#+ ", text);
        for(String match : matches){
            text = text.replace(match, "replace_" + match.length());
        }
        for(String match : matches){
            if(removeHeadlines){
                text = text.replace("replace_" + match.length(), " ");
            }
            else {
                text = text.replace("replace_" + match.length(), "##" + match);
            }
        }
        return text;
    }

    private static List<String> findMatches(String pattern, String text){
        Matcher matcher = Pattern.compile(pattern).matcher(text);
        List<String> matches = new LinkedList<>();
        while (matcher.find()){
            matches.add(matcher.group());
        }
        return matches;
    }
}
