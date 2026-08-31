package com.example.food.weekly.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;

public record WeeklyMenuSaveRequest(
        @NotNull
        LocalDate weekStart,

        @NotNull
        @Size(max = 21)
        List<@Valid WeeklyMenuItemRequest> items
) {
}
