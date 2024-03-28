-- TODO rename once ready for review

-- TODO remove once ready for review
alter table daycare
    drop if exists mealtime_breakfast,
    drop if exists mealtime_lunch,
    drop if exists mealtime_snack,
    drop if exists mealtime_supper,
    drop if exists mealtime_evening_snack;

alter table daycare
    add mealtime_breakfast time without time zone,
    add mealtime_lunch time without time zone,
    add mealtime_snack time without time zone,
    add mealtime_supper time without time zone,
    add mealtime_evening_snack time without time zone;