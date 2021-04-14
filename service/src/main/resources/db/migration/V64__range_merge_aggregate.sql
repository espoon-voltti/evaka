CREATE AGGREGATE range_merge(anyrange) (sfunc = range_merge, stype = anyrange);
