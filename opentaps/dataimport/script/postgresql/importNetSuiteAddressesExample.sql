-- Use the postgresql COPY command to load a CSV into the net_suite_address_book entity.  The fields should appear in this order for it to work.

BEGIN;

COPY net_suite_address_book
(
address_book_id,
address_line1,
address_line2,
attention,
city,
company,
country,
entity_id,
is_default_bill_address,
is_default_ship_address,
address_name,
phone,
state_province_name,
zip
)
FROM '/path/to/net-suite-addresses.csv' DELIMITERS ',' CSV;

COMMIT;
