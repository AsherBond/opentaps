
-- create the partyClassificationGroup records for wholesale and retail, if not already present
insert into party_classification_group(PARTY_CLASSIFICATION_GROUP_ID, PARTY_CLASSIFICATION_TYPE_ID, PARENT_GROUP_ID, DESCRIPTION, LAST_UPDATED_STAMP, LAST_UPDATED_TX_STAMP, CREATED_STAMP, CREATED_TX_STAMP) VALUES('PCGRP_RETAIL', 'TRADE_RETAIL_CLASSIF', null, 'Retail Group', now(), now(), now(), now()) on duplicate key update LAST_UPDATED_STAMP = now() ;
insert into party_classification_group(PARTY_CLASSIFICATION_GROUP_ID, PARTY_CLASSIFICATION_TYPE_ID, PARENT_GROUP_ID, DESCRIPTION, LAST_UPDATED_STAMP, LAST_UPDATED_TX_STAMP, CREATED_STAMP, CREATED_TX_STAMP) VALUES('PCGRP_WHOLE', 'TRADE_WHOLE_CLASSIFI', null, 'Wholesale Group', now(), now(), now(), now()) on duplicate key update LAST_UPDATED_STAMP = now() ;

load data local infile
''
ignore
into table data_import_customer
fields optionally enclosed by '"'
lines terminated by '\r'
ignore 1 lines

(
    @customerId,
    @companyName,
    @attnName,
    @address1,
    @address2,
    @city,
    @stateProvinceGeoId,
    @mainPostalCodeA,
    @mainPostalCodeB,
    @stateProvinceGeoName,
    @countryName,
    @countryGeoId,
    @shipToAttnName,
    @shipToAddress1,
    @shipToAddress2,
    @shipToCity,
    @shipToStateProvinceGeoId,
    @shipToPostalCodeA,
    @shipToPostalCodeB,
    @shipToCountryName,
    @shipToCountryGeoId,
    @note,
    @primaryPhoneCountryCode,
    @primaryPhoneAreaCode,
    @primaryPhoneNumber,
    @primaryPhoneExtension,
    @faxCountryCode,
    @faxAreaCode,
    @faxNumber,
    @didCountryCode,
    @didAreaCode,
    @didNumber,
    @didExtension,
    @emailAddress,
    @webAddress,
    @partyClassificationTypeId,
    @discount
)

set
customer_id = nullif(trim(@customerId),''),
company_name = nullif(trim(@companyName),''),
attn_name = nullif(trim(@attnName),''),
address1 = nullif(trim(@address1),''),
address2 = nullif(trim(@address2),''),
city = nullif(trim(@city),''),
state_province_geo_id = nullif(trim(ucase(@stateProvinceGeoId)),''),
postal_code = nullif(if( trim(@mainPostalCodeB)='',trim(@mainPostalCodeA),concat(trim(@mainPostalCodeA),'-',trim(@mainPostalCodeB)) ),''),
state_province_geo_name = nullif(trim(@stateProvinceGeoName),''),
country_geo_id = nullif(trim(ucase(@countryGeoId)),''),
ship_to_attn_name = nullif(trim(@shipToAttnName),''),
ship_to_address1 = nullif(trim(@shipToAddress1),''),
ship_to_address2 = nullif(trim(@shipToAddress2),''),
ship_to_city = nullif(trim(@shipToCity),''),
ship_to_state_province_geo_id = nullif(trim(ucase(@shipToStateProvinceGeoId)),''),
ship_to_postal_code = nullif(if( trim(@shipToPostalCodeB)='',trim(@shipToPostalCodeA),concat(trim(@shipToPostalCodeA),'-',trim(@shipToPostalCodeB)) ),''),
ship_to_country_geo_id = nullif(trim(ucase(@shipToCountryGeoId)),''),
note = nullif(trim(@note),''),
primary_phone_country_code = nullif(trim(@primaryPhoneCountryCode),''),
primary_phone_area_code = nullif(trim(@primaryPhoneAreaCode),''),
primary_phone_number = nullif(trim(@primaryPhoneNumber),''),
primary_phone_extension = nullif(trim(@primaryPhoneExtension),''),
fax_country_code = nullif(trim(@faxCountryCode),''),
fax_area_code = nullif(trim(@faxAreaCode),''),
fax_number = nullif(trim(@faxNumber),''),
did_country_code = nullif(trim(@didCountryCode),''),
did_area_code = nullif(trim(@didAreaCode),''),
did_number = nullif(trim(@didNumber),''),
did_extension = nullif(trim(@didExtension),''),
email_address = nullif(trim(@emailAddress),''),
web_address = nullif(trim(@webAddress),''),
party_classification_type_id = case trim(@partyClassificationTypeId) when 'Whole' then 'TRADE_WHOLE_CLASSIFI' when 'Retail' then 'TRADE_RETAIL_CLASSIF' else null end,
discount = nullif(trim(@discount),'')

