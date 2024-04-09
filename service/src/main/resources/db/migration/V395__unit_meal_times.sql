alter table daycare
    add mealtime_breakfast timerange_non_nullable_range,
    add mealtime_lunch timerange_non_nullable_range,
    add mealtime_snack timerange_non_nullable_range,
    add mealtime_supper timerange_non_nullable_range,
    add mealtime_evening_snack timerange_non_nullable_range;