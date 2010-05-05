-- Use the postgresql COPY command to load a CSV into the net_suite_customer entity.  The fields should appear in this order for it to work.

BEGIN;

COPY net_suite_customer

(
accountnumber,
altemail,
altphone,
amount_complete,
category_0,
comments,
company_name,
cost_estimate,
country,
creditlimit,
currency_id,
customer_id,
customer_type_id,
email,
expected_close,
fax,
first_name,
full_name,
home_phone,
isemailhtml,
isemailpdf,
isinactive,
istaxable,
is_job,
is_person,
last_name,
loginaccess,
middlename,
mobile_phone,
multiple_price_id,
customer_name,
openbalance,
parent_id,
partner_id,
payment_terms_id,
phone,
probability,
projected_end,
reminderdays,
renewal,
resalenumber,
sales_rep_id,
sales_territory_id,
salutation,
state,
status_id,
status_probability,
subsidiary_id,
tax_item_id,
dealer_agreement_received,
credit_application_received,
internet_approved,
dealer_approvedopen,
customer_warehouse_location_id
)

FROM '/path/to/net-suite-customers.csv' DELIMITERS ',' CSV;

COMMIT;
