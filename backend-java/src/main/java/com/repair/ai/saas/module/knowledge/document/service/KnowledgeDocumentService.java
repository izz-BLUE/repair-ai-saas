package com.repair.ai.saas.module.knowledge.document.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.repair.ai.saas.common.BusinessException;
import com.repair.ai.saas.common.ResultCode;
import com.repair.ai.saas.module.ai.service.AiClient;
import com.repair.ai.saas.module.knowledge.document.entity.KnowledgeDocument;
import com.repair.ai.saas.module.knowledge.document.enums.DocumentParseStatus;
import com.repair.ai.saas.module.knowledge.document.mapper.KnowledgeDocumentMapper;
import com.repair.ai.saas.module.knowledge.entity.KnowledgeBase;
import com.repair.ai.saas.module.knowledge.entity.KnowledgeItem;
import com.repair.ai.saas.module.knowledge.enums.KnowledgeStatus;
import com.repair.ai.saas.module.knowledge.mapper.KnowledgeBaseMapper;
import com.repair.ai.saas.module.knowledge.mapper.KnowledgeItemMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class KnowledgeDocumentService {

    private static final List<String> ALLOWED_EXTENSIONS = List.of("txt", "md");
    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB

    private final KnowledgeDocumentMapper documentMapper;
    private final KnowledgeItemMapper knowledgeItemMapper;
    private final KnowledgeBaseMapper knowledgeBaseMapper;
    private final DocumentParseService parseService;
    private final AiClient aiClient;

    @Value("${app.upload.base-dir:data/uploads}")
    private String uploadBaseDir;

    // ==================== 上传 ====================

    /**
     * 上传并解析文档，生成知识条目。
     * 文件保存和 Qdrant 同步不在事务内，失败时记录状态。
     */
    public KnowledgeDocument upload(Long tenantId, Long knowledgeBaseId,
                                    MultipartFile file, Long userId) {
        // 1. 校验知识库属于当前租户
        validateKnowledgeBase(tenantId, knowledgeBaseId);

        // 2. 校验文件
        validateFile(file);

        String originalFilename = file.getOriginalFilename();
        String ext = DocumentParseService.getExtension(originalFilename);

        // 3. 生成安全存储文件名和路径
        String storedFilename = UUID.randomUUID() + "." + ext;
        String relativePath = tenantId + "/" + storedFilename;
        Path basePath = Paths.get(uploadBaseDir).toAbsolutePath().normalize();
        Path targetPath = basePath.resolve(relativePath).normalize();

        // 路径穿越检查
        if (!targetPath.startsWith(basePath)) {
            throw new BusinessException(ResultCode.VALIDATION_ERROR, "非法文件路径");
        }

        // 4. 保存文件
        try {
            Files.createDirectories(targetPath.getParent());
            file.transferTo(targetPath.toFile());
        } catch (IOException e) {
            log.error("Failed to save uploaded file: {}", e.getMessage());
            throw new BusinessException(ResultCode.INTERNAL_ERROR, "文件保存失败");
        }

        // 5. 创建 document 记录
        KnowledgeDocument doc = new KnowledgeDocument();
        doc.setTenantId(tenantId);
        doc.setKnowledgeBaseId(knowledgeBaseId);
        doc.setOriginalFilename(originalFilename);
        doc.setStoredFilename(storedFilename);
        doc.setContentType(file.getContentType());
        doc.setFileSize(file.getSize());
        doc.setStoragePath(relativePath);
        doc.setParseStatus(DocumentParseStatus.PENDING.name());
        doc.setItemCount(0);
        doc.setCreatedBy(userId);
        documentMapper.insert(doc);

        // 6. 解析 + 生成条目（不在事务内，失败记录状态）
        try {
            List<String> paragraphs = parseService.parse(targetPath, file.getContentType());
            int itemCount = createKnowledgeItems(tenantId, knowledgeBaseId, doc, originalFilename, paragraphs);

            doc.setParseStatus(DocumentParseStatus.SUCCESS.name());
            doc.setItemCount(itemCount);
            documentMapper.updateById(doc);
            log.info("Document {} parsed successfully, generated {} items", doc.getId(), itemCount);
        } catch (Exception e) {
            log.error("Failed to parse document {}: {}", doc.getId(), e.getMessage());
            doc.setParseStatus(DocumentParseStatus.FAILED.name());
            doc.setErrorMessage(e.getMessage());
            documentMapper.updateById(doc);
        }

        return doc;
    }

    // ==================== 列表 ====================

    public Page<KnowledgeDocument> list(Long tenantId, int page, int size,
                                         Long knowledgeBaseId, String parseStatus) {
        Page<KnowledgeDocument> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<KnowledgeDocument> wrapper = new LambdaQueryWrapper<KnowledgeDocument>()
                .eq(KnowledgeDocument::getTenantId, tenantId);
        if (knowledgeBaseId != null) {
            wrapper.eq(KnowledgeDocument::getKnowledgeBaseId, knowledgeBaseId);
        }
        if (parseStatus != null && !parseStatus.isBlank()) {
            wrapper.eq(KnowledgeDocument::getParseStatus, parseStatus.trim().toUpperCase());
        }
        wrapper.orderByDesc(KnowledgeDocument::getCreatedAt);
        return documentMapper.selectPage(pageParam, wrapper);
    }

    // ==================== 详情 ====================

    public KnowledgeDocument getById(Long tenantId, Long id) {
        KnowledgeDocument doc = documentMapper.selectOne(
                new LambdaQueryWrapper<KnowledgeDocument>()
                        .eq(KnowledgeDocument::getTenantId, tenantId)
                        .eq(KnowledgeDocument::getId, id)
        );
        if (doc == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "文档不存在");
        }
        return doc;
    }

    // ==================== 删除 ====================

    /**
     * 逻辑删除文档，同时将关联的 knowledge_item 标记 INACTIVE 并同步 Qdrant。
     * Qdrant 同步失败不影响数据库操作，仅记录 warning。
     */
    @Transactional
    public void delete(Long tenantId, Long id) {
        KnowledgeDocument doc = getById(tenantId, id);

        // 逻辑删除文档
        documentMapper.deleteById(id);

        // 将关联条目标记 INACTIVE
        List<KnowledgeItem> items = knowledgeItemMapper.selectList(
                new LambdaQueryWrapper<KnowledgeItem>()
                        .eq(KnowledgeItem::getTenantId, tenantId)
                        .eq(KnowledgeItem::getDocumentId, id)
        );
        for (KnowledgeItem item : items) {
            item.setStatus(KnowledgeStatus.INACTIVE.name());
            knowledgeItemMapper.updateById(item);

            // 同步 Qdrant（fire-and-forget）
            try {
                aiClient.syncKnowledgeItem(
                        tenantId, item.getId(), item.getKnowledgeBaseId(),
                        item.getTitle(), item.getQuestion(), item.getAnswer(),
                        item.getProductType(), item.getFaultType(), item.getStatus());
            } catch (Exception e) {
                log.warn("Failed to sync item {} status to Qdrant after document delete: {}",
                        item.getId(), e.getMessage());
            }
        }

        log.info("Deleted document {} and deactivated {} items", id, items.size());
    }

    // ==================== 重解析 ====================

    /**
     * 重新解析文档：先删除旧条目（逻辑删除 + Qdrant 删除），再重新解析生成新条目。
     */
    public KnowledgeDocument reparse(Long tenantId, Long id) {
        KnowledgeDocument doc = getById(tenantId, id);

        // 删除旧条目
        deactivateOldItems(tenantId, id);

        // 重新解析
        Path basePath = Paths.get(uploadBaseDir).toAbsolutePath().normalize();
        Path filePath = basePath.resolve(doc.getStoragePath()).normalize();

        if (!Files.exists(filePath)) {
            doc.setParseStatus(DocumentParseStatus.FAILED.name());
            doc.setErrorMessage("源文件不存在: " + doc.getStoragePath());
            documentMapper.updateById(doc);
            return doc;
        }

        try {
            List<String> paragraphs = parseService.parse(filePath, doc.getContentType());
            int itemCount = createKnowledgeItems(tenantId, doc.getKnowledgeBaseId(),
                    doc, doc.getOriginalFilename(), paragraphs);

            doc.setParseStatus(DocumentParseStatus.SUCCESS.name());
            doc.setItemCount(itemCount);
            doc.setErrorMessage(null);
            documentMapper.updateById(doc);
            log.info("Document {} reparsed successfully, generated {} items", doc.getId(), itemCount);
        } catch (Exception e) {
            log.error("Failed to reparse document {}: {}", doc.getId(), e.getMessage());
            doc.setParseStatus(DocumentParseStatus.FAILED.name());
            doc.setErrorMessage(e.getMessage());
            documentMapper.updateById(doc);
        }

        return doc;
    }

    // ==================== 内部方法 ====================

    private void validateKnowledgeBase(Long tenantId, Long knowledgeBaseId) {
        KnowledgeBase kb = knowledgeBaseMapper.selectOne(
                new LambdaQueryWrapper<KnowledgeBase>()
                        .eq(KnowledgeBase::getTenantId, tenantId)
                        .eq(KnowledgeBase::getId, knowledgeBaseId)
        );
        if (kb == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "知识库不存在");
        }
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ResultCode.VALIDATION_ERROR, "文件不能为空");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new BusinessException(ResultCode.VALIDATION_ERROR, "文件大小超过 10MB 限制");
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.isBlank()) {
            throw new BusinessException(ResultCode.VALIDATION_ERROR, "文件名不能为空");
        }

        String ext = DocumentParseService.getExtension(originalFilename);
        if (!ALLOWED_EXTENSIONS.contains(ext)) {
            throw new BusinessException(ResultCode.VALIDATION_ERROR,
                    "不支持的文件类型: ." + ext + "，当前仅支持: " + ALLOWED_EXTENSIONS);
        }
    }

    /**
     * 解析段落并生成 knowledge_item，每个 item 写 MySQL 后 fire-and-forget 同步 Qdrant。
     * 返回成功生成的条目数。
     */
    private int createKnowledgeItems(Long tenantId, Long knowledgeBaseId,
                                      KnowledgeDocument doc, String originalFilename,
                                      List<String> paragraphs) {
        int count = 0;
        String baseName = stripExtension(originalFilename);

        for (int i = 0; i < paragraphs.size(); i++) {
            String paragraph = paragraphs.get(i);
            if (paragraph.isBlank()) {
                continue;
            }

            String title = baseName + " - 第" + (i + 1) + "段";

            KnowledgeItem item = new KnowledgeItem();
            item.setTenantId(tenantId);
            item.setKnowledgeBaseId(knowledgeBaseId);
            item.setTitle(title);
            item.setQuestion(title);
            item.setAnswer(paragraph);
            item.setDocumentId(doc.getId());
            item.setSortOrder(i);
            item.setStatus(KnowledgeStatus.ACTIVE.name());
            knowledgeItemMapper.insert(item);

            // 同步 Qdrant（fire-and-forget）
            try {
                aiClient.syncKnowledgeItem(
                        tenantId, item.getId(), knowledgeBaseId,
                        item.getTitle(), item.getQuestion(), item.getAnswer(),
                        null, null, KnowledgeStatus.ACTIVE.name());
            } catch (Exception e) {
                log.warn("Failed to sync item {} to Qdrant: {}", item.getId(), e.getMessage());
            }

            count++;
        }

        return count;
    }

    /**
     * 将文档关联的所有条目标记 INACTIVE 并同步 Qdrant。
     */
    private void deactivateOldItems(Long tenantId, Long documentId) {
        List<KnowledgeItem> items = knowledgeItemMapper.selectList(
                new LambdaQueryWrapper<KnowledgeItem>()
                        .eq(KnowledgeItem::getTenantId, tenantId)
                        .eq(KnowledgeItem::getDocumentId, documentId)
        );
        for (KnowledgeItem item : items) {
            item.setStatus(KnowledgeStatus.INACTIVE.name());
            knowledgeItemMapper.updateById(item);

            try {
                aiClient.syncKnowledgeItem(
                        tenantId, item.getId(), item.getKnowledgeBaseId(),
                        item.getTitle(), item.getQuestion(), item.getAnswer(),
                        item.getProductType(), item.getFaultType(), item.getStatus());
            } catch (Exception e) {
                log.warn("Failed to sync item {} to Qdrant during reparse: {}", item.getId(), e.getMessage());
            }
        }
        log.info("Deactivated {} old items for document reparse {}", items.size(), documentId);
    }

    private String stripExtension(String filename) {
        int dot = filename.lastIndexOf('.');
        return dot > 0 ? filename.substring(0, dot) : filename;
    }
}
