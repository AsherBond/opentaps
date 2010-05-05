--This is a simple example of importing a Bill of Materials.  The products must have already been imported with product imports
--and the BOM file basically has productId, productIdTo, and quantity.  I
--NOTE: after you import a BOM this way, you must run the service "initLowLevelCode" to set the Product.billOfMaterialsLevel.

load data local infile
''
ignore
into table PRODUCT_ASSOC
fields terminated by ','
optionally enclosed by '"'
lines terminated by '\n'
ignore 1 lines
(
    @productId,
    @productIdTo,
    @quantity
)

set
PRODUCT_ID = nullif(trim(@productId),''),
PRODUCT_ID_TO = nullif(trim(@productIdTo),''),
PRODUCT_ASSOC_TYPE_ID = 'MANUF_COMPONENT',
FROM_DATE = '2007-01-01 00:00:00.0',
QUANTITY = @quantity
;

delete from PRODUCT_ASSOC where PRODUCT_ID = PRODUCT_ID_TO and PRODUCT_ASSOC_TYPE_ID = 'MANUF_COMPONENT' and FROM_DATE = '2007-01-01 00:00:00.0';
