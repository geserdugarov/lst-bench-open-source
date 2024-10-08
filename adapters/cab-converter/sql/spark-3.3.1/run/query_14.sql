select
    100.00 * sum(case
        when p_type like 'PROMO%'
        then l_extendedprice*(1-l_discount)
else 0
    end) / sum(l_extendedprice * (1 - l_discount)) as promo_revenue
from
    ${catalog}.${database}${stream_num}.lineitem,
    ${catalog}.${database}${stream_num}.part
where
    l_partkey = p_partkey
    and l_shipdate >= date '${param1}'
    and l_shipdate < date '${param1}' + interval '1' month;
