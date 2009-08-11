-- Use the postgresql COPY command to load a CSV into the net_suite_item_price entity.  The fields should appear in this order for it to work.

BEGIN;

COPY net_suite_item_price
(
-- this field has to be generated, for example use cat -n file, then remove the whitespace around the number and insert a , separator
price_id,
isinactive,
item_price_name,
isonline,
currency_id,
discount_percentage,
item_id,
item_price_extid,
item_price_id,
item_quantity_id,
item_unit_price
)
FROM '/path/to/net-suite-prices.csv' DELIMITERS ',' CSV;

COMMIT;
