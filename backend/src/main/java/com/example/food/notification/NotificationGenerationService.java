package com.example.food.notification;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.food.admin.error.AdminErrorLogService;
import com.example.food.pantry.UserPantryItem;
import com.example.food.pantry.UserPantryItemMapper;
import com.example.food.recipe.RecipeRecord;
import com.example.food.recipe.RecipeRecordMapper;
import com.example.food.weekly.WeeklyMenuItem;
import com.example.food.weekly.WeeklyMenuItemMapper;
import com.example.food.weekly.WeeklyMenuPlan;
import com.example.food.weekly.WeeklyMenuPlanMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.time.DayOfWeek;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.Locale;
import java.util.stream.Collectors;

@Service
public class NotificationGenerationService {

    public static final String SUCCESS = "SUCCESS";
    public static final String PARTIAL_FAILURE = "PARTIAL_FAILURE";
    public static final String FAILURE = "FAILURE";
    private static final Logger log = LoggerFactory.getLogger(NotificationGenerationService.class);

    private final NotificationScanMapper scanMapper;
    private final UserNotificationMapper notificationMapper;
    private final UserNotificationPreferenceMapper preferenceMapper;
    private final NotificationScanRunMapper scanRunMapper;
    private final UserPantryItemMapper pantryMapper;
    private final WeeklyMenuPlanMapper weeklyMenuPlanMapper;
    private final WeeklyMenuItemMapper weeklyMenuItemMapper;
    private final RecipeRecordMapper recipeRecordMapper;
    private final AdminErrorLogService errorLogService;
    private final Clock clock;

    @Autowired
    public NotificationGenerationService(
            NotificationScanMapper scanMapper,
            UserNotificationMapper notificationMapper,
            UserNotificationPreferenceMapper preferenceMapper,
            NotificationScanRunMapper scanRunMapper,
            UserPantryItemMapper pantryMapper,
            WeeklyMenuPlanMapper weeklyMenuPlanMapper,
            WeeklyMenuItemMapper weeklyMenuItemMapper,
            RecipeRecordMapper recipeRecordMapper,
            AdminErrorLogService errorLogService
    ) {
        this(
                scanMapper,
                notificationMapper,
                preferenceMapper,
                scanRunMapper,
                pantryMapper,
                weeklyMenuPlanMapper,
                weeklyMenuItemMapper,
                recipeRecordMapper,
                errorLogService,
                Clock.system(NotificationService.ZONE)
        );
    }

    NotificationGenerationService(
            NotificationScanMapper scanMapper,
            UserNotificationMapper notificationMapper,
            UserNotificationPreferenceMapper preferenceMapper,
            NotificationScanRunMapper scanRunMapper,
            UserPantryItemMapper pantryMapper,
            WeeklyMenuPlanMapper weeklyMenuPlanMapper,
            WeeklyMenuItemMapper weeklyMenuItemMapper,
            RecipeRecordMapper recipeRecordMapper,
            AdminErrorLogService errorLogService,
            Clock clock
    ) {
        this.scanMapper = scanMapper;
        this.notificationMapper = notificationMapper;
        this.preferenceMapper = preferenceMapper;
        this.scanRunMapper = scanRunMapper;
        this.pantryMapper = pantryMapper;
        this.weeklyMenuPlanMapper = weeklyMenuPlanMapper;
        this.weeklyMenuItemMapper = weeklyMenuItemMapper;
        this.recipeRecordMapper = recipeRecordMapper;
        this.errorLogService = errorLogService;
        this.clock = clock;
    }

    public NotificationScanResult scanAll() {
        LocalDateTime startedAt = now();
        NotificationScanRun run = new NotificationScanRun();
        run.setStartedAt(startedAt);
        run.setStatus(FAILURE);
        run.setProcessedCount(0);
        run.setErrorCount(0);
        scanRunMapper.insert(run);

        int processedCount = 0;
        int errorCount = 0;
        List<String> errors = new ArrayList<>();
        try {
            List<Long> userIds = safeList(scanMapper.findActiveUserIds());
            for (Long userId : userIds) {
                if (userId == null) {
                    continue;
                }
                try {
                    processedCount += scanUser(userId, startedAt.toLocalDate());
                } catch (RuntimeException exception) {
                    errorCount++;
                    String summary = "用户 " + userId + "：" + safeMessage(exception);
                    errors.add(summary);
                    recordFailure(exception, "NotificationGenerationService#scanUser", summary);
                    log.warn("Notification scan failed for user {}", userId, exception);
                }
            }
            run.setStatus(errorCount == 0 ? SUCCESS : PARTIAL_FAILURE);
        } catch (RuntimeException exception) {
            errorCount++;
            errors.add(safeMessage(exception));
            recordFailure(exception, "NotificationGenerationService#scanAll", safeMessage(exception));
            log.error("Notification scan failed", exception);
            run.setStatus(FAILURE);
        }

        run.setFinishedAt(now());
        run.setProcessedCount(processedCount);
        run.setErrorCount(errorCount);
        run.setErrorSummary(limitText(String.join("; ", errors), 1000));
        scanRunMapper.updateById(run);
        return new NotificationScanResult(processedCount, errorCount, run.getStatus());
    }

    int scanUser(Long userId, LocalDate today) {
        UserNotificationPreference preference = preferenceMapper.findByUserId(userId);
        int processed = 0;
        if (preference == null || enabled(preference.getPantryExpiringEnabled()) || enabled(preference.getPantryExpiredEnabled())) {
            processed += scanPantry(userId, today, preference);
        }
        if (preference == null || enabled(preference.getWeeklyMenuPreparationEnabled())) {
            processed += scanWeeklyMenu(userId, today.plusDays(1));
        }
        return processed;
    }

    private int scanPantry(
            Long userId,
            LocalDate today,
            UserNotificationPreference preference
    ) {
        List<UserPantryItem> items = safeList(pantryMapper.findExpiryAlertsByUserId(userId, today.plusDays(7)));
        int processed = 0;
        for (UserPantryItem item : items) {
            if (!isUsable(item) || item.getExpireDate() == null) {
                continue;
            }
            boolean expired = item.getExpireDate().isBefore(today);
            if (expired && preference != null && !enabled(preference.getPantryExpiredEnabled())) {
                continue;
            }
            if (!expired && preference != null && !enabled(preference.getPantryExpiringEnabled())) {
                continue;
            }
            NotificationType type = expired ? NotificationType.PANTRY_EXPIRED : NotificationType.PANTRY_EXPIRING;
            String dedupeKey = "PANTRY|" + type.name() + "|" + normalizedIngredientKey(item.getIngredientName())
                    + "|" + item.getExpireDate();
            if (createNotification(pantryNotification(userId, item, type, dedupeKey))) {
                processed++;
            }
        }
        return processed;
    }

    private int scanWeeklyMenu(Long userId, LocalDate menuDate) {
        LocalDate weekStart = menuDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        WeeklyMenuPlan plan = weeklyMenuPlanMapper.findByUserIdAndWeekStart(userId, weekStart);
        if (plan == null) {
            return 0;
        }
        List<WeeklyMenuItem> menuItems = safeList(weeklyMenuItemMapper.findByPlanId(plan.getId())).stream()
                .filter(item -> menuDate.equals(item.getMenuDate()))
                .toList();
        if (menuItems.isEmpty()) {
            return 0;
        }

        Set<Long> recipeIds = menuItems.stream()
                .map(WeeklyMenuItem::getRecipeId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Map<Long, String> recipeTitles = recipeIds.isEmpty()
                ? Map.of()
                : safeList(recipeRecordMapper.selectList(new QueryWrapper<RecipeRecord>()
                                .eq("user_id", userId)
                                .in("id", recipeIds)
                                .orderByAsc("id"))).stream()
                        .collect(Collectors.toMap(
                                RecipeRecord::getId,
                                record -> hasText(record.getTitle()) ? record.getTitle().trim() : "待确认菜谱",
                                (first, ignored) -> first,
                                LinkedHashMap::new
                        ));
        List<String> titles = menuItems.stream()
                .map(WeeklyMenuItem::getRecipeId)
                .map(recipeTitles::get)
                .map(title -> hasText(title) ? title : "待确认菜谱")
                .distinct()
                .toList();
        String recipeSummary = String.join("、", titles);
        String dedupeKey = "WEEKLY_MENU_PREP|" + plan.getId() + "|" + menuDate;
        UserNotification notification = new UserNotification();
        notification.setUserId(userId);
        notification.setNotificationType(NotificationType.WEEKLY_MENU_PREP.name());
        notification.setTitle("明日周菜单准备提醒");
        notification.setSummary("明天（" + menuDate + "）有 " + titles.size() + " 道菜需要准备。");
        notification.setContent("菜单日期：" + menuDate + "；菜谱：" + recipeSummary
                + "。建议提前检查食材和烹饪安排。");
        notification.setTargetPath("/weekly-menu?weekStart=" + weekStart);
        notification.setStatus(NotificationStatus.UNREAD.name());
        notification.setDedupeKey(dedupeKey);
        notification.setCreatedAt(now());
        return createNotification(notification) ? 1 : 0;
    }

    private UserNotification pantryNotification(
            Long userId,
            UserPantryItem item,
            NotificationType type,
            String dedupeKey
    ) {
        String ingredientName = hasText(item.getIngredientName()) ? item.getIngredientName().trim() : "未命名食材";
        String expireDate = item.getExpireDate().toString();
        String quantity = formatQuantity(item.getQuantity(), item.getUnit());
        UserNotification notification = new UserNotification();
        notification.setUserId(userId);
        notification.setNotificationType(type.name());
        if (type == NotificationType.PANTRY_EXPIRED) {
            notification.setTitle("食材已过期：" + ingredientName);
            notification.setSummary(expireDate + " 已过期，请尽快处理。");
            notification.setContent("库存中的「" + ingredientName + "」已于 " + expireDate
                    + " 过期，当前数量约为 " + quantity + "，建议停止食用并从库存中处理。");
        } else {
            notification.setTitle("食材即将到期：" + ingredientName);
            notification.setSummary("将在 " + expireDate + " 到期，建议优先安排使用。");
            notification.setContent("库存中的「" + ingredientName + "」将在 " + expireDate
                    + " 到期，当前数量约为 " + quantity + "，可以优先安排到近期菜谱中。");
        }
        notification.setTargetPath("/pantry");
        notification.setStatus(NotificationStatus.UNREAD.name());
        notification.setDedupeKey(dedupeKey);
        notification.setCreatedAt(now());
        return notification;
    }

    private boolean createNotification(UserNotification notification) {
        if (notificationMapper.findByDedupeKey(notification.getUserId(), notification.getDedupeKey()) != null) {
            return false;
        }
        try {
            notificationMapper.insert(notification);
            return true;
        } catch (DuplicateKeyException exception) {
            return false;
        }
    }

    private void recordFailure(Throwable exception, String component, String message) {
        try {
            errorLogService.recordFailure(
                    AdminErrorLogService.BACKEND,
                    component,
                    message,
                    exception,
                    null,
                    "SCHEDULED",
                    "/internal/notifications/scan",
                    500,
                    null
            );
        } catch (RuntimeException loggingException) {
            log.warn("Unable to record notification scan failure", loggingException);
        }
    }

    private boolean isUsable(UserPantryItem item) {
        return item != null
                && item.getQuantity() != null
                && item.getQuantity().compareTo(BigDecimal.ZERO) > 0;
    }

    private boolean enabled(Boolean value) {
        return !Boolean.FALSE.equals(value);
    }

    private LocalDateTime now() {
        return LocalDateTime.now(clock);
    }

    private String formatQuantity(BigDecimal quantity, String unit) {
        String value = quantity == null ? "未填写" : quantity.stripTrailingZeros().toPlainString();
        return hasText(unit) ? value + unit.trim() : value;
    }

    private String safeMessage(Throwable exception) {
        return hasText(exception.getMessage()) ? limitText(exception.getMessage(), 240) : exception.getClass().getSimpleName();
    }

    private String limitText(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private String normalizedIngredientKey(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private <T> List<T> safeList(List<T> values) {
        return values == null ? List.of() : values;
    }

    public record NotificationScanResult(int processedCount, int errorCount, String status) {
    }
}
