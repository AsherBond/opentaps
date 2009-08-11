BEGIN;

delete from data_import_u_s_zip_codes;
delete from data_import_u_s_county_tax;
delete from tax_authority_assoc where created_stamp > '2008-06-10 10:45:00';
delete from tax_authority_rate_product where created_stamp > '2008-06-10 10:45:00';
delete from tax_authority where created_stamp > '2008-06-10 10:45:00';
delete from geo_assoc where created_stamp > '2008-06-10 10:45:00';
delete from geo where created_stamp > '2008-06-10 10:45:00';

-- import data from a file called zipcodes.csv which has 5 digit zip code, the 2 letter state, and the county name
-- the county level taxes may also be imported via csv or using the data file USCountyTaxRate.xml in dataimport/data/
copy data_import_u_s_zip_codes (zip_code, state_code, county)
from '/tmp/zipcodes.csv'
with CSV
;

-- inject some failures for testing
insert into data_import_u_s_zip_codes (zip_code, state_code, county) values ('910', 'CA', 'Solano');
insert into data_import_u_s_zip_codes (zip_code, state_code, county) values ('9x005', 'CA', 'Solano');
insert into data_import_u_s_zip_codes (zip_code, state_code, county) values ('91107', 'FB', 'Los Angeles');

COMMIT;
