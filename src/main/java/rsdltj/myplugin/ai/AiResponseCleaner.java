package rsdltj.myplugin.ai;

public class AiResponseCleaner {

    /*
        With this class we can process our AI's answers to clean the format
        Handles trimming, Markdown fence removal, and Unicode escape decoding.
     */
    public static String clean(String rawResponse) {
        if (rawResponse == null || rawResponse.isBlank()) {
            return "";
        }

        String cleaned = rawResponse.trim();

        // 1. Remove Markdown code fences (common LLM output artifact)
        cleaned = cleaned.replace("```java", "")
                .replace("```", "");

        // 2. Decode unicode escapes (e.g., \u003c -> <, \u003e -> >)
        cleaned = decodeUnicodeEscapes(cleaned);

        return cleaned.trim();
    }

    /*
        Safely decodes unicode escape sequences like \u003c into their character equivalents
        This preserves original text if sequence is malformed.
     */
    private static String decodeUnicodeEscapes(String text) {
        StringBuilder result = new StringBuilder(text.length());
        int i = 0;
        while (i < text.length()) {
            if (text.charAt(i) == '\\' && i + 5 < text.length()
                    && Character.toLowerCase(text.charAt(i + 1)) == 'u') {
                try {
                    String hex = text.substring(i + 2, i + 6);
                    char unicodeChar = (char) Integer.parseInt(hex, 16);
                    result.append(unicodeChar);
                    i += 6;
                    continue;
                } catch (NumberFormatException e) {
                    // If parsing fails, keep the backslash as literal
                    result.append('\\');
                    i++;
                }
            } else {
                result.append(text.charAt(i));
                i++;
            }
        }
        return result.toString();
    }
}