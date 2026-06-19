package com.repair.ai.saas.module.knowledge.document.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * 文档解析服务：将文件内容切分为段落。
 * MVP 阶段仅支持 .txt 和 .md。
 */
@Slf4j
@Service
public class DocumentParseService {

    /** 单段最大长度，超过则二次切分 */
    private static final int MAX_PARAGRAPH_LENGTH = 1000;

    /**
     * 解析文件，返回段落列表。
     *
     * @param filePath    文件路径
     * @param contentType MIME 类型
     * @return 段落内容列表
     */
    public List<String> parse(Path filePath, String contentType) throws IOException {
        String ext = getExtension(filePath.getFileName().toString());

        if ("txt".equals(ext) || "md".equals(ext)) {
            return parsePlainText(filePath);
        }

        // MVP 不支持 PDF/DOCX，由上层校验拦截。此处兜底。
        throw new UnsupportedOperationException("不支持的文件类型: " + ext);
    }

    private List<String> parsePlainText(Path filePath) throws IOException {
        String content = Files.readString(filePath, StandardCharsets.UTF_8);
        return splitParagraphs(content);
    }

    /**
     * 按空行切分段落，超长段落二次切分。
     */
    List<String> splitParagraphs(String content) {
        List<String> paragraphs = new ArrayList<>();
        // 按两个以上连续换行切分
        String[] blocks = content.split("(\\r?\\n){2,}");

        for (String block : blocks) {
            String trimmed = block.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            if (trimmed.length() <= MAX_PARAGRAPH_LENGTH) {
                paragraphs.add(trimmed);
            } else {
                // 二次切分：按单个换行切分
                paragraphs.addAll(splitLongParagraph(trimmed));
            }
        }
        return paragraphs;
    }

    private List<String> splitLongParagraph(String text) {
        List<String> parts = new ArrayList<>();
        String[] lines = text.split("\\r?\\n");
        StringBuilder current = new StringBuilder();

        for (String line : lines) {
            if (current.length() + line.length() + 1 > MAX_PARAGRAPH_LENGTH && current.length() > 0) {
                parts.add(current.toString().trim());
                current.setLength(0);
            }
            if (current.length() > 0) {
                current.append("\n");
            }
            current.append(line);
        }
        if (current.length() > 0) {
            parts.add(current.toString().trim());
        }
        return parts;
    }

    /**
     * 获取文件扩展名（小写，不含点号）。
     */
    public static String getExtension(String filename) {
        int dot = filename.lastIndexOf('.');
        if (dot < 0 || dot == filename.length() - 1) {
            return "";
        }
        return filename.substring(dot + 1).toLowerCase();
    }
}
