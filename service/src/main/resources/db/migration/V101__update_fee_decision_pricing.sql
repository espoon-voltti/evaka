
UPDATE fee_decision SET pricing =
    jsonb_build_object(
        'minIncomeThreshold',
        CASE
            WHEN family_size = 1 THEN 0
            WHEN family_size = 2 THEN (pricing->'minThreshold2')::int
            WHEN family_size = 3 THEN (pricing->'minThreshold3')::int
            WHEN family_size = 4 THEN (pricing->'minThreshold4')::int
            WHEN family_size = 5 THEN (pricing->'minThreshold5')::int
            WHEN family_size = 6 THEN (pricing->'minThreshold6')::int
            WHEN family_size > 6 THEN (pricing->'minThreshold6')::int + (pricing->'thresholdIncrease6Plus')::int * (family_size - 6)
        END,
        'maxIncomeThreshold',
        CASE
            WHEN family_size = 1 THEN 0
            WHEN family_size = 2 THEN (pricing->'minThreshold2')::int
            WHEN family_size = 3 THEN (pricing->'minThreshold3')::int
            WHEN family_size = 4 THEN (pricing->'minThreshold4')::int
            WHEN family_size = 5 THEN (pricing->'minThreshold5')::int
            WHEN family_size = 6 THEN (pricing->'minThreshold6')::int
            WHEN family_size > 6 THEN (pricing->'minThreshold6')::int + (pricing->'thresholdIncrease6Plus')::int * (family_size - 6)
        END + (pricing->'maxThresholdDifference')::int,
        'incomeMultiplier',
        pricing->'multiplier',
        'maxFee',
        ((pricing->'maxThresholdDifference')::int * (pricing->'multiplier')::numeric / 100.0)::int * 100,
        'minFee',
        2700
    );
