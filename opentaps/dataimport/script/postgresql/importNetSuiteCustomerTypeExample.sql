-- Use the postgresql COPY command to load a CSV into the net_suite_customer_type entity.  The fields should appear in this order for it to work.

BEGIN;

COPY net_suite_customer_type

(customer_type_id, description)

FROM '/path/to/net-suite-customer-types.csv' DELIMITERS ',' CSV;

COMMIT;
