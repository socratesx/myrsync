package com.linminitools.myrsync;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import java.util.List;
import java.util.Set;

public class ShellWordsTest {

    @Test
    public void unquotedPatternIsOneArgument() {
        assertEquals(List.of("--exclude=*.pdf"), ShellWords.arguments("--exclude=*.pdf"));
    }

    @Test
    public void singleQuotesAreNotPassedToRsync() {
        List<ShellWords.Word> words = ShellWords.split("--exclude='*.pdf'");
        assertEquals(1, words.size());
        assertEquals("--exclude=*.pdf", words.get(0).text);
        assertEquals("--exclude='*.pdf'", words.get(0).raw);
    }

    @Test
    public void doubleQuotesAreNotPassedToRsync() {
        assertEquals(List.of("--exclude=*.pdf"), ShellWords.arguments("--exclude=\"*.pdf\""));
    }

    @Test
    public void separatePatternArgumentDropsQuotes() {
        assertEquals(List.of("--exclude", "*.pdf"),
                ShellWords.arguments("--exclude '*.pdf'"));
    }

    @Test
    public void spacesInsideQuotesStayOneArgument() {
        assertEquals(List.of("--exclude=my file.pdf"),
                ShellWords.arguments("--exclude='my file.pdf'"));
        assertEquals(List.of("--exclude=it's a file"),
                ShellWords.arguments("--exclude=\"it's a file\""));
        assertEquals(List.of("--exclude=say \"hi\".pdf"),
                ShellWords.arguments("--exclude='say \"hi\".pdf'"));
    }

    @Test
    public void severalOptionsKeepTheirOwnQuotes() {
        assertEquals(List.of("-avz", "--exclude=*.pdf", "--exclude=*.tmp"),
                ShellWords.arguments("-avz --exclude='*.pdf' --exclude='*.tmp'"));
    }

    @Test
    public void backslashesStayLiteral() {
        assertEquals(List.of("--exclude=C:\\tmp\\file"),
                ShellWords.arguments("--exclude=C:\\tmp\\file"));
    }

    @Test
    public void unbalancedQuoteFallsBackToWhitespaceTokens() {
        assertEquals(List.of("-av", "--exclude='*.pdf"),
                ShellWords.arguments("-av --exclude='*.pdf"));
    }

    @Test
    public void extraWhitespaceDoesNotCreateEmptyArguments() {
        assertEquals(List.of("--exclude=*.pdf", "-n"),
                ShellWords.arguments("  --exclude='*.pdf'   -n\t"));
    }

    @Test
    public void emptyInputHasNoArguments() {
        assertEquals(List.of(), ShellWords.arguments(null));
        assertEquals(List.of(), ShellWords.arguments("   "));
    }

    @Test
    public void stripKeepsQuotedPatternAndDropsDuplicateFlag() {
        String kept = addConfig.stripDuplicateAdvancedOptions(
                "--archive --exclude='my file.pdf'", Set.of('a'));
        assertEquals("--exclude='my file.pdf'", kept);
    }

    @Test
    public void stripRewritesCombinedShortFlagsWithoutBreakingLaterQuotes() {
        String kept = addConfig.stripDuplicateAdvancedOptions(
                "-nv --exclude='my file.pdf'", Set.of('n'));
        assertEquals("-v --exclude='my file.pdf'", kept);
    }

    @Test
    public void stripLeavesUnbalancedQuotesUntouched() {
        String raw = "--exclude='*.pdf";
        String kept = addConfig.stripDuplicateAdvancedOptions(raw, Set.of('a'));
        assertEquals(raw, kept);
    }
}
