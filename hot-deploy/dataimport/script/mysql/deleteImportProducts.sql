-- Use this SQL script to erase the imported data and the DataImportProduct content.  
-- Change the timestamp to the time of the import.
delete from GOOD_IDENTIFICATION where CREATED_STAMP >= '2007-01-09 14:58:47.0';
delete from PRODUCT_PRICE where CREATED_DATE >= '2007-01-09 14:58:47.0';
delete from PRODUCT_FEATURE_APPL where CREATED_STAMP >= '2007-01-09 14:58:47.0';
delete from PRODUCT_FEATURE where CREATED_STAMP >= '2007-01-09 14:58:47.0';
delete from PRODUCT_KEYWORD where CREATED_STAMP >= '2007-01-09 14:58:47.0';
delete from PRODUCT where CREATED_STAMP >= '2007-01-09 14:58:47.0';
delete from DATA_IMPORT_PRODUCT;
