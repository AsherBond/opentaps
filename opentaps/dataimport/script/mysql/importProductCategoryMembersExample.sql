-- Creates product_category_member records for each existing product to @productCategoryId

set @productCategoryId = 'PARTS';

insert into product_category_member(product_category_id, product_id, from_date, thru_date, comments, sequence_num, quantity, last_updated_stamp, last_updated_tx_stamp, created_stamp, created_tx_stamp) 
select
    @productCategoryId,
    product_id,
    '2007-1-17 4:12:12.0',
    null,
    null,
    null,
    null,
    '2007-1-17 4:12:12.0',
    '2007-1-17 4:12:12.0',
    '2007-1-17 4:12:12.0',
    '2007-1-17 4:12:12.0'
from product


