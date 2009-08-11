-- Example MySQL script for importing product inventory
-- productId must exist in the Product entity
-- availableToPromise, onHand, and inventoryValue may be positive or negative numbers


load data local infile
''
ignore
into table data_import_inventory
fields optionally enclosed by '"'
lines terminated by '\r'
ignore 1 lines

(
    @productId,
    @availableToPromise,
    @onHand,
    @inventoryValue
)

set
product_id = nullif(trim(@productId),''),
available_to_promise = nullif(trim(@availableToPromise),''),
on_hand = nullif(trim(@onHand),''),
inventory_value = nullif(trim(@inventoryValue),'')
