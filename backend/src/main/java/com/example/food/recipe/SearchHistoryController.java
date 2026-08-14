package com.example.food.recipe;

import com.example.food.common.ApiResponse;
import com.example.food.recipe.dto.RecentSearchResponse;
import com.example.food.security.AppRole;
import com.example.food.security.AuthPrincipal;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/search-history")
public class SearchHistoryController {

    private final SearchHistoryService searchHistoryService;

    public SearchHistoryController(SearchHistoryService searchHistoryService) {
        this.searchHistoryService = searchHistoryService;
    }

    @GetMapping("/recent")
    public ApiResponse<List<RecentSearchResponse>> recent(
            @AuthenticationPrincipal AuthPrincipal principal
    ) {
        return ApiResponse.ok(searchHistoryService.recent(requireUserId(principal)));
    }

    private Long requireUserId(AuthPrincipal principal) {
        if (principal == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "请先登录");
        }
        if (principal.role() != AppRole.USER) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "仅普通用户可以查看搜索历史");
        }
        return principal.id();
    }
}
