package com.linminitools.myrsync;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Splits an rsync options string the way a shell splits words, so values pasted from
 * the rsync manual ({@code --exclude='*.pdf'}) become the argument rsync expects.
 *
 * <p>Backslashes are kept as themselves. Patterns such as {@code C:\tmp} must not be
 * eaten as escapes. A quote that is never closed leaves the original whitespace
 * tokens in place, quotes included, rather than inventing a different pattern.
 */
final class ShellWords {

    static final class Word {
        /** Argument text with quoting removed. */
        final String text;
        /** Original slice, including the quote characters the user typed. */
        final String raw;

        Word(String text, String raw) {
            this.text = text;
            this.raw = raw;
        }
    }

    private ShellWords() {}

    static List<String> arguments(String input) {
        ArrayList<String> args = new ArrayList<>();
        for (Word word : split(input)) {
            args.add(word.text);
        }
        return args;
    }

    static List<Word> split(String input) {
        if (input == null || input.isEmpty()) {
            return Collections.emptyList();
        }
        List<Word> parsed = trySplit(input);
        if (parsed != null) {
            return parsed;
        }
        return whitespaceSplit(input);
    }

    /** @return words, or null when a quote is never closed */
    private static List<Word> trySplit(String input) {
        ArrayList<Word> words = new ArrayList<>();
        int n = input.length();
        int i = 0;
        while (i < n) {
            while (i < n && isWhitespace(input.charAt(i))) {
                i++;
            }
            if (i >= n) {
                break;
            }
            int start = i;
            StringBuilder text = new StringBuilder();
            boolean inSingle = false;
            boolean inDouble = false;
            while (i < n) {
                char c = input.charAt(i);
                if (inSingle) {
                    if (c == '\'') {
                        inSingle = false;
                    } else {
                        text.append(c);
                    }
                    i++;
                } else if (inDouble) {
                    if (c == '"') {
                        inDouble = false;
                    } else {
                        text.append(c);
                    }
                    i++;
                } else if (isWhitespace(c)) {
                    break;
                } else if (c == '\'') {
                    inSingle = true;
                    i++;
                } else if (c == '"') {
                    inDouble = true;
                    i++;
                } else {
                    text.append(c);
                    i++;
                }
            }
            if (inSingle || inDouble) {
                return null;
            }
            words.add(new Word(text.toString(), input.substring(start, i)));
        }
        return words;
    }

    private static List<Word> whitespaceSplit(String input) {
        String trimmed = input.trim();
        if (trimmed.isEmpty()) {
            return Collections.emptyList();
        }
        ArrayList<Word> words = new ArrayList<>();
        for (String token : trimmed.split("\\s+")) {
            if (!token.isEmpty()) {
                words.add(new Word(token, token));
            }
        }
        return words;
    }

    /** Same set as Java's {@code \s} without the Unicode flag. */
    private static boolean isWhitespace(char c) {
        return c == ' ' || c == '\t' || c == '\n' || c == '\u000B' || c == '\f' || c == '\r';
    }
}
