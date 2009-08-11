-- Use the postgresql COPY command to load a CSV into the net_suite_payment_term entity.  The fields should appear in this order for it to work.

BEGIN;

COPY net_suite_payment_term
(
date_driven,
days_until_due,
discount_days,
is_inactive,
minimum_days,
term_name,
payment_terms_extid,
payment_terms_id,
percentage_discount
)

FROM '/path/to/net-suite-payment-terms.csv' DELIMITERS ',' CSV;

COMMIT;
