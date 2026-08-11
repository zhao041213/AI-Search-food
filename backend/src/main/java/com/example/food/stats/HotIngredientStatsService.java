package com.example.food.stats;

import com.example.food.stats.dto.HotIngredientItemResponse;
import com.example.food.stats.dto.HotIngredientStatsResponse;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class HotIngredientStatsService {

    private final SearchLogIngredientMapper ingredientMapper;
    private final Clock clock;

    public HotIngredientStatsService(SearchLogIngredientMapper ingredientMapper, Clock clock) {
        this.ingredientMapper = ingredientMapper;
        this.clock = clock;
    }

    public HotIngredientStatsResponse get(String periodValue, int limit) {
        validateLimit(limit);
        HotIngredientPeriod period = HotIngredientPeriod.parse(periodValue);
        LocalDateTime now = LocalDateTime.now(clock);
        LocalDateTime cutoff = period.cutoff(now);
        HotIngredientTotals totals = ingredientMapper.summarize(cutoff);
        if (totals == null) {
            totals = new HotIngredientTotals(0, 0);
        }
        List<HotIngredientAggregateRow> rows = ingredientMapper.findHotIngredients(cutoff, limit);
        return new HotIngredientStatsResponse(
                period.value(),
                totals.getTotalSearches(),
                totals.getTotalIngredientOccurrences(),
                now,
                toItems(rows, totals.getTotalIngredientOccurrences())
        );
    }

    private List<HotIngredientItemResponse> toItems(
            List<HotIngredientAggregateRow> rows,
            long totalIngredientOccurrences
    ) {
        if (rows == null || rows.isEmpty()) {
            return List.of();
        }
        List<HotIngredientItemResponse> items = new ArrayList<>(rows.size());
        for (int index = 0; index < rows.size(); index++) {
            HotIngredientAggregateRow row = rows.get(index);
            items.add(new HotIngredientItemResponse(
                    index + 1,
                    row.getIngredientName(),
                    row.getSearchCount(),
                    share(row.getSearchCount(), totalIngredientOccurrences),
                    row.getLatestSearchAt()
            ));
        }
        return items;
    }

    private double share(long count, long total) {
        if (total == 0) {
            return 0;
        }
        return BigDecimal.valueOf(count)
                .divide(BigDecimal.valueOf(total), 4, RoundingMode.HALF_UP)
                .doubleValue();
    }

    private void validateLimit(int limit) {
        if (limit < 1 || limit > 50) {
            throw new IllegalArgumentException("排行榜数量必须在 1 到 50 之间");
        }
    }
}
