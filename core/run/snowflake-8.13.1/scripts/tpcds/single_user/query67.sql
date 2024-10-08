SELECT
    *
FROM
    (
        SELECT
            i_category,
            i_class,
            i_brand,
            i_product_name,
            d_year,
            d_qoy,
            d_moy,
            s_store_id,
            sumsales,
            RANK() OVER(
                PARTITION BY i_category
            ORDER BY
                sumsales DESC
            ) rk
        FROM
            (
                SELECT
                    i_category,
                    i_class,
                    i_brand,
                    i_product_name,
                    d_year,
                    d_qoy,
                    d_moy,
                    s_store_id,
                    SUM( COALESCE( ss_sales_price*ss_quantity, 0 )) sumsales
                FROM
                    ${catalog}.${database}.store_sales ${asof_sf},
                    ${catalog}.${database}.date_dim,
                    ${catalog}.${database}.store,
                    ${catalog}.${database}.item
                WHERE
                    ss_sold_date_sk = d_date_sk
                    AND ss_item_sk = i_item_sk
                    AND ss_store_sk = s_store_sk
                    AND d_month_seq BETWEEN 1194 AND 1194 + 11
                GROUP BY
                    ROLLUP(
                        i_category,
                        i_class,
                        i_brand,
                        i_product_name,
                        d_year,
                        d_qoy,
                        d_moy,
                        s_store_id
                    )
            ) dw1
    ) dw2
WHERE
    rk <= 100
ORDER BY
    i_category,
    i_class,
    i_brand,
    i_product_name,
    d_year,
    d_qoy,
    d_moy,
    s_store_id,
    sumsales,
    rk LIMIT 100;
