#!/bin/sh
# Simple script that runs all the net suite SQL scripts

PSQL="psql opentaps"

$PSQL < importNetSuiteCustomerTypeExample.sql 
$PSQL < importNetSuiteOrderTypeExample.sql 
$PSQL < importNetSuiteCustomerExample.sql 
$PSQL < importNetSuitePaymentTermsExample.sql 
$PSQL < importNetSuiteAddressesExample.sql
$PSQL < importNetSuiteItemExample.sql
$PSQL < importNetSuitePricesExample.sql
