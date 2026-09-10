package com.example.food.suggestion;

import com.example.food.notification.NotificationService;
import com.example.food.notification.NotificationType;
import com.example.food.suggestion.dto.FeatureSuggestionAdditionResponse;
import com.example.food.suggestion.dto.FeatureSuggestionAdminPageResponse;
import com.example.food.suggestion.dto.FeatureSuggestionAdminResponse;
import com.example.food.suggestion.dto.FeatureSuggestionAdminUpdateRequest;
import com.example.food.suggestion.dto.FeatureSuggestionAppendRequest;
import com.example.food.suggestion.dto.FeatureSuggestionCreateRequest;
import com.example.food.suggestion.dto.FeatureSuggestionPageResponse;
import com.example.food.suggestion.dto.FeatureSuggestionResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

@Service
public class FeatureSuggestionService {
    private static final int DEFAULT_PAGE_SIZE = 10;
    private static final int MAX_PAGE_SIZE = 50;
    private static final int MAX_KEYWORD_LENGTH = 80;
    private static final String USER_OPERATOR = "USER";
    private static final String ADMIN_OPERATOR = "ADMIN";

    private final FeatureSuggestionMapper suggestionMapper;
    private final FeatureSuggestionAdditionMapper additionMapper;
    private final FeatureSuggestionAuditLogMapper auditLogMapper;
    private final FeatureSuggestionFileStorage fileStorage;
    private final NotificationService notificationService;

    public FeatureSuggestionService(
            FeatureSuggestionMapper suggestionMapper,
            FeatureSuggestionAdditionMapper additionMapper,
            FeatureSuggestionAuditLogMapper auditLogMapper,
            FeatureSuggestionFileStorage fileStorage,
            NotificationService notificationService
    ) {
        this.suggestionMapper = suggestionMapper;
        this.additionMapper = additionMapper;
        this.auditLogMapper = auditLogMapper;
        this.fileStorage = fileStorage;
        this.notificationService = notificationService;
    }

    @Transactional
    public FeatureSuggestionResponse create(Long userId, FeatureSuggestionCreateRequest request, MultipartFile screenshot) {
        if (suggestionMapper.countRecent(userId, LocalDateTime.now().minusHours(1)) >= 5) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "每小时最多提交 5 条功能建议");
        }
        String title = cleanRequired(request.title(), "建议标题不能为空");
        if (suggestionMapper.findRecentTitle(userId, title, LocalDateTime.now().minusDays(1)) != null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "24 小时内已经提交过相同标题的建议");
        }

        FeatureSuggestionFileStorage.StoredFile storedFile = null;
        if (screenshot != null && !screenshot.isEmpty()) {
            storedFile = fileStorage.store(fileStorage.readAndValidate(screenshot));
        }
        LocalDateTime now = LocalDateTime.now();
        FeatureSuggestion suggestion = new FeatureSuggestion();
        suggestion.setUserId(userId);
        suggestion.setType(normalizeFilter(request.type(), FeatureSuggestionType.values(), "建议类型不合法"));
        suggestion.setTitle(title);
        suggestion.setDetail(cleanRequired(request.detail(), "建议详情不能为空"));
        suggestion.setExpectedEffect(cleanOptional(request.expectedEffect()));
        suggestion.setStatus(FeatureSuggestionStatus.PENDING.name());
        if (storedFile != null) {
            suggestion.setScreenshotOriginalName(storedFile.originalName());
            suggestion.setScreenshotStoredName(storedFile.storedName());
            suggestion.setScreenshotContentType(storedFile.contentType());
            suggestion.setScreenshotSize(storedFile.fileSize());
            suggestion.setScreenshotStoragePath(storedFile.storagePath());
        }
        suggestion.setCreatedAt(now);
        suggestion.setUpdatedAt(now);
        try {
            suggestionMapper.insert(suggestion);
            audit(suggestion.getId(), userId, USER_OPERATOR, "USER_SUBMIT", "提交功能建议，类型=" + suggestion.getType());
            return toUserResponse(suggestion);
        } catch (RuntimeException exception) {
            if (storedFile != null) {
                fileStorage.deleteQuietly(storedFile.storedName());
            }
            throw exception;
        }
    }

    public FeatureSuggestionPageResponse listMine(Long userId, int page, int size) {
        PageArgs args = pageArgs(page, size);
        List<FeatureSuggestionResponse> items = suggestionMapper.findUserPage(userId, args.size(), args.offset())
                .stream().map(this::toUserResponse).toList();
        long total = suggestionMapper.countUser(userId);
        return new FeatureSuggestionPageResponse(items, total, args.page(), args.size(), totalPages(total, args.size()));
    }

    public FeatureSuggestionResponse detailMine(Long userId, Long id) {
        return toUserResponse(requireOwned(id, userId));
    }

    @Transactional
    public FeatureSuggestionResponse appendMine(Long userId, Long id, FeatureSuggestionAppendRequest request) {
        FeatureSuggestion suggestion = requireOwned(id, userId);
        FeatureSuggestionAddition addition = new FeatureSuggestionAddition();
        addition.setSuggestionId(id);
        addition.setUserId(userId);
        addition.setContent(cleanRequired(request.content(), "补充内容不能为空"));
        addition.setCreatedAt(LocalDateTime.now());
        additionMapper.insert(addition);
        suggestion.setUpdatedAt(LocalDateTime.now());
        suggestionMapper.updateById(suggestion);
        audit(id, userId, USER_OPERATOR, "USER_APPEND", "用户补充建议内容");
        return toUserResponse(requireOwned(id, userId));
    }

    public FeatureSuggestionAdminPageResponse listAdmin(String keyword, String type, String status, int page, int size) {
        PageArgs args = pageArgs(page, size);
        String normalizedKeyword = normalizeKeyword(keyword);
        String normalizedType = normalizeFilter(type, FeatureSuggestionType.values(), "建议类型不合法");
        String normalizedStatus = normalizeFilter(status, FeatureSuggestionStatus.values(), "建议状态不合法");
        List<FeatureSuggestionAdminResponse> items = suggestionMapper.findAdminPage(
                        normalizedKeyword, normalizedType, normalizedStatus, args.size(), args.offset()
                ).stream().map(this::toAdminResponse).toList();
        long total = suggestionMapper.countAdmin(normalizedKeyword, normalizedType, normalizedStatus);
        return new FeatureSuggestionAdminPageResponse(items, total, args.page(), args.size(), totalPages(total, args.size()));
    }

    public FeatureSuggestionAdminResponse detailAdmin(Long id) {
        return toAdminResponse(requireActive(id));
    }

    @Transactional
    public FeatureSuggestionAdminResponse updateAdmin(
            Long adminId,
            Long id,
            FeatureSuggestionAdminUpdateRequest request
    ) {
        FeatureSuggestion suggestion = requireActive(id);
        String status = request.status() == null ? suggestion.getStatus() : normalizeEnum(request.status(), "建议状态不合法");
        String adminReply = request.adminReply() == null ? suggestion.getAdminReply() : cleanOptional(request.adminReply());
        String internalNote = request.internalNote() == null ? suggestion.getInternalNote() : cleanOptional(request.internalNote());
        Long duplicateOfId = request.duplicateOfId() == null ? suggestion.getDuplicateOfId() : request.duplicateOfId();
        if (duplicateOfId != null && duplicateOfId.equals(id)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "不能将建议标记为自己的重复项");
        }
        if (duplicateOfId != null && suggestionMapper.findActive(duplicateOfId) == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "重复项不存在");
        }
        boolean statusChanged = !status.equals(suggestion.getStatus());
        boolean replyChanged = !same(adminReply, suggestion.getAdminReply());
        suggestion.setStatus(status);
        suggestion.setAdminReply(adminReply);
        suggestion.setInternalNote(internalNote);
        suggestion.setDuplicateOfId(duplicateOfId);
        suggestion.setUpdatedAt(LocalDateTime.now());
        if (statusChanged || replyChanged) {
            suggestion.setLastProcessedAt(suggestion.getUpdatedAt());
        }
        if (suggestionMapper.updateAdmin(suggestion) != 1) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "建议已变化，请刷新后重试");
        }
        String action = statusChanged ? "ADMIN_STATUS_UPDATE" : "ADMIN_REPLY_UPDATE";
        audit(id, adminId, ADMIN_OPERATOR, action, "状态=" + status + (replyChanged ? "，已更新回复" : ""));
        if (statusChanged || replyChanged) {
            notificationService.notifyUser(
                    suggestion.getUserId(),
                    NotificationType.FEATURE_SUGGESTION,
                    "功能建议有新进展",
                    "「" + suggestion.getTitle() + "」的处理状态已更新为“" + statusLabel(status) + "”。",
                    StringUtils.hasText(adminReply) ? adminReply : "感谢你的建议，我们已经更新了处理进度。",
                    "/feature-suggestions",
                    "FEATURE_SUGGESTION|" + id + "|" + suggestion.getUpdatedAt()
            );
        }
        return toAdminResponse(requireActive(id));
    }

    @Transactional
    public void deleteAdmin(Long adminId, Long id) {
        FeatureSuggestion suggestion = requireActive(id);
        if (suggestionMapper.logicalDelete(id, LocalDateTime.now()) != 1) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "建议已变化，请刷新后重试");
        }
        audit(id, adminId, ADMIN_OPERATOR, "ADMIN_DELETE", "管理员隐藏功能建议");
    }

    public FeatureSuggestionFileStorage.StoredFileInfo image(Long id, Long userId, boolean admin) {
        FeatureSuggestion suggestion = admin ? requireActive(id) : requireOwned(id, userId);
        if (!StringUtils.hasText(suggestion.getScreenshotStoredName())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "该建议没有截图");
        }
        return new FeatureSuggestionFileStorage.StoredFileInfo(
                fileStorage.load(suggestion.getScreenshotStoredName()),
                suggestion.getScreenshotContentType(),
                suggestion.getScreenshotOriginalName()
        );
    }

    public byte[] exportCsv(String keyword, String type, String status) {
        String normalizedKeyword = normalizeKeyword(keyword);
        String normalizedType = normalizeFilter(type, FeatureSuggestionType.values(), "建议类型不合法");
        String normalizedStatus = normalizeFilter(status, FeatureSuggestionStatus.values(), "建议状态不合法");
        List<FeatureSuggestion> items = suggestionMapper.findAdminPage(
                normalizedKeyword, normalizedType, normalizedStatus, 10000, 0
        );
        StringBuilder csv = new StringBuilder("ID,用户ID,类型,标题,状态,创建时间,更新时间\n");
        for (FeatureSuggestion item : items) {
            csv.append(item.getId()).append(',')
                    .append(item.getUserId()).append(',')
                    .append(csvCell(item.getType())).append(',')
                    .append(csvCell(item.getTitle())).append(',')
                    .append(csvCell(item.getStatus())).append(',')
                    .append(csvCell(String.valueOf(item.getCreatedAt()))).append(',')
                    .append(csvCell(String.valueOf(item.getUpdatedAt()))).append('\n');
        }
        return ("\uFEFF" + csv).getBytes(StandardCharsets.UTF_8);
    }

    private FeatureSuggestion requireOwned(Long id, Long userId) {
        FeatureSuggestion suggestion = suggestionMapper.findOwned(id, userId);
        if (suggestion == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "功能建议不存在");
        return suggestion;
    }

    private FeatureSuggestion requireActive(Long id) {
        FeatureSuggestion suggestion = suggestionMapper.findActive(id);
        if (suggestion == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "功能建议不存在");
        return suggestion;
    }

    private FeatureSuggestionResponse toUserResponse(FeatureSuggestion item) {
        return new FeatureSuggestionResponse(
                item.getId(), item.getType(), item.getTitle(), item.getDetail(), item.getExpectedEffect(),
                item.getStatus(), item.getScreenshotOriginalName(), item.getScreenshotContentType(), item.getScreenshotSize(),
                screenshotUrl(item, false), item.getDuplicateOfId(), item.getAdminReply(), item.getCreatedAt(), item.getUpdatedAt(),
                item.getLastProcessedAt(), additions(item.getId())
        );
    }

    private FeatureSuggestionAdminResponse toAdminResponse(FeatureSuggestion item) {
        return new FeatureSuggestionAdminResponse(
                item.getId(), item.getUserId(), item.getType(), item.getTitle(), item.getDetail(), item.getExpectedEffect(),
                item.getStatus(), item.getScreenshotOriginalName(), item.getScreenshotContentType(), item.getScreenshotSize(),
                screenshotUrl(item, true), item.getDuplicateOfId(), item.getInternalNote(), item.getAdminReply(), item.getCreatedAt(),
                item.getUpdatedAt(), item.getLastProcessedAt(), additions(item.getId())
        );
    }

    private List<FeatureSuggestionAdditionResponse> additions(Long suggestionId) {
        return additionMapper.findBySuggestionId(suggestionId).stream()
                .map(item -> new FeatureSuggestionAdditionResponse(item.getId(), item.getUserId(), item.getContent(), item.getCreatedAt()))
                .toList();
    }

    private String screenshotUrl(FeatureSuggestion item, boolean admin) {
        if (!StringUtils.hasText(item.getScreenshotStoredName())) return null;
        return (admin ? "/api/admin/feature-suggestions/" : "/api/feature-suggestions/")
                + item.getId() + "/screenshot";
    }

    private void audit(Long suggestionId, Long operatorId, String operatorType, String action, String details) {
        FeatureSuggestionAuditLog log = new FeatureSuggestionAuditLog();
        log.setSuggestionId(suggestionId);
        log.setOperatorId(operatorId);
        log.setOperatorType(operatorType);
        log.setAction(action);
        log.setDetails(details);
        log.setCreatedAt(LocalDateTime.now());
        auditLogMapper.insert(log);
    }

    private PageArgs pageArgs(int page, int size) {
        int safePage = page < 1 ? 1 : page;
        int safeSize = size < 1 ? DEFAULT_PAGE_SIZE : Math.min(size, MAX_PAGE_SIZE);
        long offset = (long) (safePage - 1) * safeSize;
        if (offset > Integer.MAX_VALUE) throw new IllegalArgumentException("页码超出范围");
        return new PageArgs(safePage, safeSize, (int) offset);
    }

    private int totalPages(long total, int size) {
        return total == 0 ? 0 : (int) Math.ceil(total / (double) size);
    }

    private String normalizeKeyword(String value) {
        if (!StringUtils.hasText(value)) return null;
        return trimToCodePoints(value.trim(), MAX_KEYWORD_LENGTH);
    }

    private <E extends Enum<E>> String normalizeFilter(String value, E[] values, String message) {
        if (!StringUtils.hasText(value) || "ALL".equalsIgnoreCase(value.trim())) return null;
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        for (E candidate : values) if (candidate.name().equals(normalized)) return normalized;
        throw new IllegalArgumentException(message);
    }

    private String normalizeEnum(String value, String message) {
        if (!StringUtils.hasText(value)) throw new IllegalArgumentException(message);
        return value.trim().toUpperCase(Locale.ROOT);
    }

    private String cleanRequired(String value, String message) {
        if (!StringUtils.hasText(value)) throw new IllegalArgumentException(message);
        return value.trim();
    }

    private String cleanOptional(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private boolean same(String left, String right) {
        return left == null ? right == null : left.equals(right);
    }

    private String trimToCodePoints(String value, int max) {
        int end = value.offsetByCodePoints(0, Math.min(max, value.codePointCount(0, value.length())));
        return value.substring(0, end);
    }

    private String csvCell(String value) {
        String safe = value == null ? "" : value.replace("\"", "\"\"");
        return "\"" + safe + "\"";
    }

    private String statusLabel(String status) {
        return switch (status) {
            case "ACCEPTED" -> "已采纳";
            case "PLANNED" -> "已排期";
            case "IN_DEVELOPMENT" -> "开发中";
            case "COMPLETED" -> "已完成";
            case "DECLINED" -> "暂不处理";
            default -> "待处理";
        };
    }

    private record PageArgs(int page, int size, int offset) { }
}
