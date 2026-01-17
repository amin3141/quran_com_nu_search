package com.quran.omni.search;

import java.util.regex.Pattern;

public final class TextCleaner {
    private static final Pattern HTML_TAGS = Pattern.compile("<[^>]*>");
    private static final Pattern MARKDOWN_LINK = Pattern.compile("\\[([^\\]]+)]\\([^\\)]+\\)");
    private static final Pattern MARKDOWN_HEADERS = Pattern.compile("(?m)^#{1,6}\\s+");
    private static final Pattern MULTI_SPACE = Pattern.compile("\\s+");

    private TextCleaner() {
    }

    public static String cleanSnippet(String text, int maxChars) {
        if (text == null) {
            return "";
        }
        String cleaned = text;
        cleaned = MARKDOWN_HEADERS.matcher(cleaned).replaceAll("");
        cleaned = MARKDOWN_LINK.matcher(cleaned).replaceAll("$1");
        cleaned = HTML_TAGS.matcher(cleaned).replaceAll(" ");
        cleaned = cleaned.replace("`", "");
        cleaned = unescapeHtml(cleaned);
        cleaned = MULTI_SPACE.matcher(cleaned).replaceAll(" ").trim();
        if (maxChars > 0 && cleaned.length() > maxChars) {
            return cleaned.substring(0, maxChars).trim() + "...";
        }
        return cleaned;
    }

    private static String unescapeHtml(String text) {
        return text
            .replace("&amp;", "&")
            .replace("&quot;", "\"")
            .replace("&#39;", "'")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&nbsp;", " ");
    }
}
