load data local infile
''
ignore
into table DATA_IMPORT_PRODUCT
fields terminated by ','
optionally enclosed by "'"
lines terminated by '\n'
(
    @productId,
    @customId1,
    @customId2,
    @description,
    @weight,
    @productLength,
    @width,
    @height,
    @price,
    @productFeature1,
    @productTypeId
)

set
PRODUCT_ID = nullif(trim(@productId),''),
CUSTOM_ID1 = nullif(trim(@customId1),''),
CUSTOM_ID2 = nullif(trim(@customId2),''),
DESCRIPTION = nullif(trim(concat(@description, ' ', @customId1)),''),
WEIGHT = nullif(trim(@weight),''),
WEIGHT_UOM_ID = 'WT_lb',
PRODUCT_LENGTH = nullif(trim(@productLength),''),
PRODUCT_LENGTH_UOM_ID = 'LEN_mm',
WIDTH = nullif(trim(@width),''),
WIDTH_UOM_ID = 'LEN_mm',
HEIGHT = nullif(trim(@height),''),
HEIGHT_UOM_ID = 'LEN_mm',
PRICE = nullif(trim(@price),''),
PRICE_CURRENCY_UOM_ID = 'USD',
PRODUCT_FEATURE1 = nullif(trim(@productFeature1),''),
PRODUCT_TYPE_ID = nullif(trim(@productTypeId),'')
