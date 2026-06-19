package com.repair.ai.saas.module.knowledge.document.service;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DocumentParseServiceTest {

    private final DocumentParseService service = new DocumentParseService();

    // ===== 按空行切分 =====

    @Test
    void splitParagraphs_twoParagraphs() {
        String content = "第一段内容\n\n第二段内容";
        List<String> result = service.splitParagraphs(content);
        assertEquals(2, result.size());
        assertEquals("第一段内容", result.get(0));
        assertEquals("第二段内容", result.get(1));
    }

    @Test
    void splitParagraphs_multipleBlankLines() {
        String content = "段落A\n\n\n\n段落B";
        List<String> result = service.splitParagraphs(content);
        assertEquals(2, result.size());
        assertEquals("段落A", result.get(0));
        assertEquals("段落B", result.get(1));
    }

    @Test
    void splitParagraphs_emptyContent() {
        List<String> result = service.splitParagraphs("");
        assertTrue(result.isEmpty());
    }

    @Test
    void splitParagraphs_blankContent() {
        List<String> result = service.splitParagraphs("   \n\n  ");
        assertTrue(result.isEmpty());
    }

    // ===== 超过 1000 字二次切分 =====

    @Test
    void splitParagraphs_longParagraph_splitByNewlines() {
        // 构造一个超过 1000 字的段落，用换行分隔
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 50; i++) {
            if (i > 0) sb.append("\n");
            sb.append("这是第").append(i + 1).append("行内容，用于测试长段落切分功能。");
        }
        String content = sb.toString();
        assertTrue(content.length() > 1000, "Content should be > 1000 chars");

        List<String> result = service.splitParagraphs(content);
        // 应该被切分为多段
        assertTrue(result.size() > 1, "Long paragraph should be split into multiple parts");
        // 每段长度不应超过 1000
        for (String part : result) {
            assertTrue(part.length() <= 1000, "Each part should be <= 1000 chars, got: " + part.length());
        }
    }

    @Test
    void splitParagraphs_shortParagraph_notSplit() {
        String content = "这是一个短段落，不需要切分。";
        List<String> result = service.splitParagraphs(content);
        assertEquals(1, result.size());
        assertEquals("这是一个短段落，不需要切分。", result.get(0));
    }

    // ===== getExtension =====

    @Test
    void getExtension_txt() {
        assertEquals("txt", DocumentParseService.getExtension("test.txt"));
    }

    @Test
    void getExtension_md() {
        assertEquals("md", DocumentParseService.getExtension("readme.md"));
    }

    @Test
    void getExtension_pdf() {
        assertEquals("pdf", DocumentParseService.getExtension("doc.pdf"));
    }

    @Test
    void getExtension_exe() {
        assertEquals("exe", DocumentParseService.getExtension("virus.exe"));
    }

    @Test
    void getExtension_noExtension() {
        assertEquals("", DocumentParseService.getExtension("Makefile"));
    }

    @Test
    void getExtension_upperCase() {
        assertEquals("txt", DocumentParseService.getExtension("FILE.TXT"));
    }

    @Test
    void getExtension_multipleDots() {
        assertEquals("txt", DocumentParseService.getExtension("my.file.name.txt"));
    }
}
