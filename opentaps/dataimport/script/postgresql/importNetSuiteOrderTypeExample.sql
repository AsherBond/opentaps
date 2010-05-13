-- Use the postgresql COPY command to load a CSV into the net_suite_sales_order_type entity.  The fields should appear in this order for it to work.

BEGIN;

COPY net_suite_sales_order_type

(list_id, description)

FROM '/path/to/net-suite-sales-order-types.csv' DELIMITERS ',' CSV;

COMMIT;
