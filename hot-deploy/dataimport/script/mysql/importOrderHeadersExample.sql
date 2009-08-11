-- Example MySQL script for importing order headers
-- orderDate must be in SQL yyyy-mm-dd hh:mm::ss.fff timestamp format
-- customerId must exist as partyId in the Party entity
-- grandTotal, shippingTotal, taxTotal, adjustmentsTotal may be positive or negative numbers
-- currencyUomId must exist in the Uom entity
-- orderTypeId must exist in the OrderType entity
-- orderClosed must be Y or N

load data local infile
''
ignore
into table data_import_order_header
fields optionally enclosed by '"'
lines terminated by '\r'
ignore 1 lines

(
    @orderId,
    @orderDate,
    @customerId,
    @ignore,
    @grandTotal,
    @shippingTotal,
    @taxTotal,
    @adjustmentsTotal,
    @currencyUomId,
    @comments,
    @orderClosed,
    @orderTypeId,
)

set
order_id = nullif(trim(@orderId),''),
order_type_id = case trim(@orderTypeId) when 'DI' then 'SALES_ORDER' when 'CI' then 'PURCHASE_ORDER' else null end,
customer_party_id = nullif(trim(@customerId),''),
order_date = nullif(trim(@orderDate),''),
currency_uom_id = nullif(trim(ucase(@currencyUomId)),''),
shipping_total = nullif(trim(@shippingTotal),''),
tax_total = nullif(trim(@taxTotal),''),
adjustments_total = nullif(trim(@adjustmentsTotal),''),
grand_total = nullif(trim(@grandTotal),''),
order_closed = nullif(trim(@orderClosed),''),
comments = nullif(trim(@comments),'');

