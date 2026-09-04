package com.example.food.notification;

import com.example.food.admin.error.AdminErrorLogService;
import com.example.food.pantry.UserPantryItem;
import com.example.food.pantry.UserPantryItemMapper;
import com.example.food.recipe.RecipeRecord;
import com.example.food.recipe.RecipeRecordMapper;
import com.example.food.weekly.WeeklyMenuItem;
import com.example.food.weekly.WeeklyMenuItemMapper;
import com.example.food.weekly.WeeklyMenuPlan;
import com.example.food.weekly.WeeklyMenuPlanMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationGenerationServiceTest {

    @Mock
    private NotificationScanMapper scanMapper;

    @Mock
    private UserNotificationMapper notificationMapper;

    @Mock
    private UserNotificationPreferenceMapper preferenceMapper;

    @Mock
    private NotificationScanRunMapper scanRunMapper;

    @Mock
    private UserPantryItemMapper pantryMapper;

    @Mock
    private WeeklyMenuPlanMapper weeklyMenuPlanMapper;

    @Mock
    private WeeklyMenuItemMapper weeklyMenuItemMapper;

    @Mock
    private RecipeRecordMapper recipeRecordMapper;

    @Mock
    private AdminErrorLogService errorLogService;

    private NotificationGenerationService service;
    private final LocalDate today = LocalDate.of(2026, 9, 4);

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(
                Instant.parse("2026-09-04T06:00:00Z"),
                ZoneId.of("Asia/Shanghai")
        );
        service = new NotificationGenerationService(
                scanMapper,
                notificationMapper,
                preferenceMapper,
                scanRunMapper,
                pantryMapper,
                weeklyMenuPlanMapper,
                weeklyMenuItemMapper,
                recipeRecordMapper,
                errorLogService,
                clock
        );
    }

    @Test
    void createsDistinctExpiredAndExpiringPantryMessagesInShanghaiTime() {
        UserPantryItem expired = pantry(1L, "番茄", today.minusDays(1));
        UserPantryItem soon = pantry(2L, "牛奶", today.plusDays(2));
        when(preferenceMapper.findByUserId(7L)).thenReturn(null);
        when(pantryMapper.findExpiryAlertsByUserId(7L, today.plusDays(7)))
                .thenReturn(List.of(expired, soon));
        when(notificationMapper.findByDedupeKey(eq(7L), any())).thenReturn(null);
        when(weeklyMenuPlanMapper.findByUserIdAndWeekStart(eq(7L), any())).thenReturn(null);

        int created = service.scanUser(7L, today);

        assertThat(created).isEqualTo(2);
        ArgumentCaptor<UserNotification> captor = ArgumentCaptor.forClass(UserNotification.class);
        verify(notificationMapper, org.mockito.Mockito.times(2)).insert(captor.capture());
        assertThat(captor.getAllValues()).extracting(UserNotification::getNotificationType)
                .containsExactlyInAnyOrder(
                        NotificationType.PANTRY_EXPIRED.name(),
                        NotificationType.PANTRY_EXPIRING.name()
                );
        verify(pantryMapper).findExpiryAlertsByUserId(7L, today.plusDays(7));
    }

    @Test
    void preferenceSwitchesPreventOnlyFutureGeneration() {
        UserNotificationPreference preference = new UserNotificationPreference();
        preference.setPantryExpiringEnabled(false);
        preference.setPantryExpiredEnabled(true);
        preference.setWeeklyMenuPreparationEnabled(false);
        UserPantryItem expired = pantry(1L, "番茄", today.minusDays(1));
        UserPantryItem soon = pantry(2L, "牛奶", today.plusDays(2));
        when(preferenceMapper.findByUserId(7L)).thenReturn(preference);
        when(pantryMapper.findExpiryAlertsByUserId(7L, today.plusDays(7)))
                .thenReturn(List.of(expired, soon));
        when(notificationMapper.findByDedupeKey(eq(7L), any())).thenReturn(null);

        assertThat(service.scanUser(7L, today)).isEqualTo(1);
        verify(notificationMapper).insert(any(UserNotification.class));
        verify(weeklyMenuPlanMapper, never()).findByUserIdAndWeekStart(any(), any());
    }

    @Test
    void weeklyMenuReminderContainsRecipeNamesAndIsIdempotent() {
        LocalDate menuDate = today.plusDays(1);
        LocalDate weekStart = menuDate.with(java.time.temporal.TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY));
        WeeklyMenuPlan plan = new WeeklyMenuPlan();
        plan.setId(31L);
        plan.setUserId(7L);
        plan.setWeekStartDate(weekStart);
        WeeklyMenuItem item = new WeeklyMenuItem();
        item.setId(41L);
        item.setPlanId(31L);
        item.setMenuDate(menuDate);
        item.setRecipeId(51L);
        RecipeRecord recipe = new RecipeRecord();
        recipe.setId(51L);
        recipe.setTitle("番茄炒蛋");
        when(preferenceMapper.findByUserId(7L)).thenReturn(null);
        when(pantryMapper.findExpiryAlertsByUserId(7L, today.plusDays(7))).thenReturn(List.of());
        when(weeklyMenuPlanMapper.findByUserIdAndWeekStart(7L, weekStart)).thenReturn(plan);
        when(weeklyMenuItemMapper.findByPlanId(31L)).thenReturn(List.of(item));
        when(recipeRecordMapper.selectList(any())).thenReturn(List.of(recipe));
        when(notificationMapper.findByDedupeKey(eq(7L), any())).thenReturn(null);

        assertThat(service.scanUser(7L, today)).isEqualTo(1);

        UserNotification existing = new UserNotification();
        existing.setId(99L);
        when(notificationMapper.findByDedupeKey(eq(7L), any())).thenReturn(existing);
        assertThat(service.scanUser(7L, today)).isZero();

        ArgumentCaptor<UserNotification> captor = ArgumentCaptor.forClass(UserNotification.class);
        verify(notificationMapper).insert(captor.capture());
        assertThat(captor.getValue().getNotificationType()).isEqualTo(NotificationType.WEEKLY_MENU_PREP.name());
        assertThat(captor.getValue().getContent()).contains("番茄炒蛋");
        assertThat(captor.getValue().getDedupeKey()).contains("WEEKLY_MENU_PREP|31|" + menuDate);
    }

    @Test
    void oneUserFailureDoesNotStopTheBatchAndIsRecorded() {
        when(scanRunMapper.insert(any(NotificationScanRun.class))).thenAnswer(invocation -> {
            NotificationScanRun run = invocation.getArgument(0);
            run.setId(1L);
            return 1;
        });
        when(scanMapper.findActiveUserIds()).thenReturn(List.of(7L, 8L));
        when(preferenceMapper.findByUserId(7L)).thenThrow(new IllegalStateException("preference database unavailable"));
        when(preferenceMapper.findByUserId(8L)).thenReturn(null);
        when(pantryMapper.findExpiryAlertsByUserId(8L, today.plusDays(7))).thenReturn(List.of());
        when(weeklyMenuPlanMapper.findByUserIdAndWeekStart(eq(8L), any())).thenReturn(null);

        var result = service.scanAll();

        assertThat(result.status()).isEqualTo(NotificationGenerationService.PARTIAL_FAILURE);
        assertThat(result.errorCount()).isEqualTo(1);
        verify(errorLogService).recordFailure(
                eq(AdminErrorLogService.BACKEND),
                eq("NotificationGenerationService#scanUser"),
                any(),
                any(Throwable.class),
                eq(null),
                eq("SCHEDULED"),
                eq("/internal/notifications/scan"),
                eq(500),
                eq(null)
        );
        ArgumentCaptor<NotificationScanRun> runCaptor = ArgumentCaptor.forClass(NotificationScanRun.class);
        verify(scanRunMapper).updateById(runCaptor.capture());
        assertThat(runCaptor.getValue().getErrorCount()).isEqualTo(1);
        assertThat(runCaptor.getValue().getErrorSummary()).contains("用户 7");
    }

    private UserPantryItem pantry(Long id, String name, LocalDate expireDate) {
        UserPantryItem item = new UserPantryItem();
        item.setId(id);
        item.setUserId(7L);
        item.setIngredientName(name);
        item.setQuantity(new BigDecimal("2"));
        item.setUnit("个");
        item.setExpireDate(expireDate);
        return item;
    }
}
