-- Example MySQL script for importing order items.
-- orderId must be present in data_import_order_header.orderId
-- productId must exist in the Product entity
-- quantity, price and adjustmentsTotal may be positive or negative numbers

load data local infile
''
ignore
into table data_import_order_item
fields optionally enclosed by '"'
lines terminated by '\r'
ignore 1 lines

(
    @itemOrderId,
    @customerPo,
    @productId,
    @quantity,
    @price,
    @adjustmentsTotal,
    @comments
)

set
order_id = nullif(trim(@itemOrderId),''),
product_id = nullif(trim(@productId),''),
quantity = nullif(trim(ucase(@quantity)),''),
price = nullif(trim(@price),''),
adjustments_total = nullif(trim(@adjustmentsTotal),''),
customer_po = nullif(trim(@customerPo),''),
comments = nullif(trim(@comments),'');


