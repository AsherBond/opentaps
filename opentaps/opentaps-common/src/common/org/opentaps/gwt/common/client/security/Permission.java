/*
 * Copyright (c) Open Source Strategies, Inc.
 *
 * Opentaps is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Opentaps is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Opentaps.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.opentaps.gwt.common.client.security;

/**
 * Available permissions.
 */
public enum Permission {

    /** ALL operations in the Accounting Manager. */
    ACCOUNTING_ADMIN("ACCOUNTING_ADMIN"),
    /** Create general ledger accounting transaction and entries. */
    ACCOUNTING_ATX_CREATE("ACCOUNTING_ATX_CREATE"),
    /** Delete general ledger accounting transaction and entries. */
    ACCOUNTING_ATX_DELETE("ACCOUNTING_ATX_DELETE"),
    /** Post general ledger accounting transactions. */
    ACCOUNTING_ATX_POST("ACCOUNTING_ATX_POST"),
    /** Update general ledger accounting transaction and entries. */
    ACCOUNTING_ATX_UPDATE("ACCOUNTING_ATX_UPDATE"),
    /** View commission rates. */
    ACCOUNTING_COMM_VIEW("ACCOUNTING_COMM_VIEW"),
    /** Create operations in the Accounting Manager. */
    ACCOUNTING_CREATE("ACCOUNTING_CREATE"),
    /** Delete operations in the Accounting Manager. */
    ACCOUNTING_DELETE("ACCOUNTING_DELETE"),
    /** Print checks. */
    ACCOUNTING_PRINT_CHECKS("ACCOUNTING_PRINT_CHECKS"),
    /** Update operations in the Accounting Manager. */
    ACCOUNTING_UPDATE("ACCOUNTING_UPDATE"),
    /** View operations in the Accounting Manager. */
    ACCOUNTING_VIEW("ACCOUNTING_VIEW"),
    /** Set foreign exchange rates. */
    ACCTG_FX_ENTRY("ACCTG_FX_ENTRY"),
    /** Set organization accounting preferences. */
    ACCTG_PREF_CREATE("ACCTG_PREF_CREATE"),
    /** ALL operations in the Catalog Manager. */
    CATALOG_ADMIN("CATALOG_ADMIN"),
    /** Create operations in the Catalog Manager. */
    CATALOG_CREATE("CATALOG_CREATE"),
    /** Delete operations in the Catalog Manager. */
    CATALOG_DELETE("CATALOG_DELETE"),
    /** Permission required, in addition to other applicable permissions, to maintain product price information including prices, promotions, and price rules. */
    CATALOG_PRICE_MAINT("CATALOG_PRICE_MAINT"),
    /** Allow create/update of 'Purchase Allow' in the Catalog Manager. */
    CATALOG_PURCHASE_ALLOW("CATALOG_PURCHASE_ALLOW"),
    /** Limited Create operations in the Catalog Manager. */
    CATALOG_ROLE_CREATE("CATALOG_ROLE_CREATE"),
    /** Limited Delete operations in the Catalog Manager. */
    CATALOG_ROLE_DELETE("CATALOG_ROLE_DELETE"),
    /** Limited Update operations in the Catalog Manager. */
    CATALOG_ROLE_UPDATE("CATALOG_ROLE_UPDATE"),
    /** Update operations in the Catalog Manager. */
    CATALOG_UPDATE("CATALOG_UPDATE"),
    /** View operations in the Catalog Manager. */
    CATALOG_VIEW("CATALOG_VIEW"),
    /** Allow create/update of 'View Allow' in the Catalog Manager. */
    CATALOG_VIEW_ALLOW("CATALOG_VIEW_ALLOW"),
    /** Create operations in the Common Component. */
    COMMON_CREATE("COMMON_CREATE"),
    /** Delete operations in the Common Component. */
    COMMON_DELETE("COMMON_DELETE"),
    /** Update operations in the Common Component. */
    COMMON_UPDATE("COMMON_UPDATE"),
    /** View operations in the Common Component. */
    COMMON_VIEW("COMMON_VIEW"),
    /** ALL operations in the Content Manager. */
    CONTENTMGR_ADMIN("CONTENTMGR_ADMIN"),
    /** Create operations in the Content Manager. */
    CONTENTMGR_CREATE("CONTENTMGR_CREATE"),
    /** Delete operations in the Content Manager. */
    CONTENTMGR_DELETE("CONTENTMGR_DELETE"),
    /** Limited Create operations in the Content Manager. */
    CONTENTMGR_ROLE_CREATE("CONTENTMGR_ROLE_CREATE"),
    /** Limited Delete operations in the Content Manager. */
    CONTENTMGR_ROLE_DELETE("CONTENTMGR_ROLE_DELETE"),
    /** Limited Update operations in the Content Manager. */
    CONTENTMGR_ROLE_UPDATE("CONTENTMGR_ROLE_UPDATE"),
    /** Limited View operations in the Content Manager. */
    CONTENTMGR_ROLE_VIEW("CONTENTMGR_ROLE_VIEW"),
    /** Update operations in the Content Manager. */
    CONTENTMGR_UPDATE("CONTENTMGR_UPDATE"),
    /** View operations in the Content Manager. */
    CONTENTMGR_VIEW("CONTENTMGR_VIEW"),
    /** Create a forecast. */
    CRMSFA_4C_CREATE("CRMSFA_4C_CREATE"),
    /** Update a forecast. */
    CRMSFA_4C_UPDATE("CRMSFA_4C_UPDATE"),
    /** Access to forecast function of application. */
    CRMSFA_4C_VIEW("CRMSFA_4C_VIEW"),
    /** View all forecasts. */
    CRMSFA_4C_VIEWALL("CRMSFA_4C_VIEWALL"),
    /** Access to the Accounts function of the application. */
    CRMSFA_ACCOUNTS_VIEW("CRMSFA_ACCOUNTS_VIEW"),
    /** Create a new account. */
    CRMSFA_ACCOUNT_CREATE("CRMSFA_ACCOUNT_CREATE"),
    /** Deactivate any existing account. */
    CRMSFA_ACCOUNT_DEACTIVATE("CRMSFA_ACCOUNT_DEACTIVATE"),
    /** Reassign owner of an existing account. */
    CRMSFA_ACCOUNT_REASSIGN("CRMSFA_ACCOUNT_REASSIGN"),
    /** Update any existing account. */
    CRMSFA_ACCOUNT_UPDATE("CRMSFA_ACCOUNT_UPDATE"),
    /** View any Account. */
    CRMSFA_ACCOUNT_VIEW("CRMSFA_ACCOUNT_VIEW"),
    /** Access to the Activities function of the application. */
    CRMSFA_ACTS_VIEW("CRMSFA_ACTS_VIEW"),
    /** View and set scope for public, private and confidential activities. */
    CRMSFA_ACT_ADMIN("CRMSFA_ACT_ADMIN"),
    /** Close an existing Activity: Event or Task. */
    CRMSFA_ACT_CLOSE("CRMSFA_ACT_CLOSE"),
    /** Create a new Activity: Event or Task. */
    CRMSFA_ACT_CREATE("CRMSFA_ACT_CREATE"),
    /** Update an existing Activity: Event or Task. */
    CRMSFA_ACT_UPDATE("CRMSFA_ACT_UPDATE"),
    /** View an Activity: Event or Task. */
    CRMSFA_ACT_VIEW("CRMSFA_ACT_VIEW"),
    /** Create marketing campaigns in CRMSFA. */
    CRMSFA_CAMP_CREATE("CRMSFA_CAMP_CREATE"),
    /** Update marketing campaigns in CRMSFA and add, update or remove contact lists to them. */
    CRMSFA_CAMP_UPDATE("CRMSFA_CAMP_UPDATE"),
    /** Access to the Cases function of the application. */
    CRMSFA_CASES_VIEW("CRMSFA_CASES_VIEW"),
    /** Close an existing Case. */
    CRMSFA_CASE_CLOSE("CRMSFA_CASE_CLOSE"),
    /** Create a new Case. */
    CRMSFA_CASE_CREATE("CRMSFA_CASE_CREATE"),
    /** Update an existing Case. */
    CRMSFA_CASE_UPDATE("CRMSFA_CASE_UPDATE"),
    /** View a Case. */
    CRMSFA_CASE_VIEW("CRMSFA_CASE_VIEW"),
    /** Access to the Contacts function of the application. */
    CRMSFA_CONTACTS_VIEW("CRMSFA_CONTACTS_VIEW"),
    /** Create a new Contact. */
    CRMSFA_CONTACT_CREATE("CRMSFA_CONTACT_CREATE"),
    /** Deactivate any existing Contact. */
    CRMSFA_CONTACT_DEACTIVATE("CRMSFA_CONTACT_DEACTIVATE"),
    /** Reassign owner of an existing contact. */
    CRMSFA_CONTACT_REASSIGN("CRMSFA_CONTACT_REASSIGN"),
    /** Update any existing Contact. */
    CRMSFA_CONTACT_UPDATE("CRMSFA_CONTACT_UPDATE"),
    /** View any Contact. */
    CRMSFA_CONTACT_VIEW("CRMSFA_CONTACT_VIEW"),
    /** Access to the dashboard Application. */
    CRMSFA_DASH_VIEW("CRMSFA_DASH_VIEW"),
    /** View an invoice. */
    CRMSFA_INVOICE_VIEW("CRMSFA_INVOICE_VIEW"),
    /** Access to the Leads function of the application. */
    CRMSFA_LEADS_VIEW("CRMSFA_LEADS_VIEW"),
    /** Create a new Lead. */
    CRMSFA_LEAD_CREATE("CRMSFA_LEAD_CREATE"),
    /** Deactivate any existing Lead. */
    CRMSFA_LEAD_DEACTIVATE("CRMSFA_LEAD_DEACTIVATE"),
    /** Delete a lead that hasn't been converted. */
    CRMSFA_LEAD_DELETE("CRMSFA_LEAD_DELETE"),
    /** Reassign owner of an existing lead. */
    CRMSFA_LEAD_REASSIGN("CRMSFA_LEAD_REASSIGN"),
    /** Update any existing Lead. */
    CRMSFA_LEAD_UPDATE("CRMSFA_LEAD_UPDATE"),
    /** View any Lead. */
    CRMSFA_LEAD_VIEW("CRMSFA_LEAD_VIEW"),
    /** Access to the Marketing function of the application. */
    CRMSFA_MKTG_VIEW("CRMSFA_MKTG_VIEW"),
    /** Access to the Opportunities function of the application. */
    CRMSFA_OPPS_VIEW("CRMSFA_OPPS_VIEW"),
    /** Create a new Opportunity. */
    CRMSFA_OPP_CREATE("CRMSFA_OPP_CREATE"),
    /** Deactivate any existing Opportunity. */
    CRMSFA_OPP_DEACTIVATE("CRMSFA_OPP_DEACTIVATE"),
    /** Update any existing Opportunity. */
    CRMSFA_OPP_UPDATE("CRMSFA_OPP_UPDATE"),
    /** View any Opportunity. */
    CRMSFA_OPP_VIEW("CRMSFA_OPP_VIEW"),
    /** Access to the Orders function of the application. */
    CRMSFA_ORDERS_VIEW("CRMSFA_ORDERS_VIEW"),
    /** Create a new Order. */
    CRMSFA_ORDER_CREATE("CRMSFA_ORDER_CREATE"),
    /** View any Order. */
    CRMSFA_ORDER_VIEW("CRMSFA_ORDER_VIEW"),
    /** Create a new Partner. */
    CRMSFA_PARTNER_CREATE("CRMSFA_PARTNER_CREATE"),
    /** Update any existing Partner. */
    CRMSFA_PARTNER_UPDATE("CRMSFA_PARTNER_UPDATE"),
    /** Access to the Partners function of the application. */
    CRMSFA_PARTNER_VIEW("CRMSFA_PARTNER_VIEW"),
    /** Update passwords for accounts/leads/contacts. */
    CRMSFA_PASS_UPDATE("CRMSFA_PASS_UPDATE"),
    /** Update payment methods of a party. */
    CRMSFA_PAY_UPDATE("CRMSFA_PAY_UPDATE"),
    /** View payment methods of a party. */
    CRMSFA_PAY_VIEW("CRMSFA_PAY_VIEW"),
    /** Access to the Quotes function of the application. */
    CRMSFA_QUOTES_VIEW("CRMSFA_QUOTES_VIEW"),
    /** Create a new Quote. */
    CRMSFA_QUOTE_CREATE("CRMSFA_QUOTE_CREATE"),
    /** Update any existing Quote. */
    CRMSFA_QUOTE_UPDATE("CRMSFA_QUOTE_UPDATE"),
    /** View any Quote. */
    CRMSFA_QUOTE_VIEW("CRMSFA_QUOTE_VIEW"),
    /** Accept returns. */
    CRMSFA_RETURN_ACCEPT("CRMSFA_RETURN_ACCEPT"),
    /** Cancel returns. */
    CRMSFA_RETURN_CANCEL("CRMSFA_RETURN_CANCEL"),
    /** Force complete returns. */
    CRMSFA_RETURN_COMP("CRMSFA_RETURN_COMP"),
    /** Create returns. */
    CRMSFA_RETURN_CREATE("CRMSFA_RETURN_CREATE"),
    /** Update shopping lists. */
    CRMSFA_SLT_UPDATE("CRMSFA_SLT_UPDATE"),
    /** View shopping lists. */
    CRMSFA_SLT_VIEW("CRMSFA_SLT_VIEW"),
    /** View survey results from CRMSFA. */
    CRMSFA_SURVEY_VIEW("CRMSFA_SURVEY_VIEW"),
    /** Assign new members to an account or team. */
    CRMSFA_TEAM_ASSIGN("CRMSFA_TEAM_ASSIGN"),
    /** See team's calendar events. */
    CRMSFA_TEAM_CALVIEW("CRMSFA_TEAM_CALVIEW"),
    /** Create a new sales team. */
    CRMSFA_TEAM_CREATE("CRMSFA_TEAM_CREATE"),
    /** Deactivate a sales team. */
    CRMSFA_TEAM_DEACTIVATE("CRMSFA_TEAM_DEACTIVATE"),
    /** Remove account or team member. */
    CRMSFA_TEAM_REMOVE("CRMSFA_TEAM_REMOVE"),
    /** Update roles of account or team member. */
    CRMSFA_TEAM_UPDATE("CRMSFA_TEAM_UPDATE"),
    /** Access to the Team Management function of the application. */
    CRMSFA_TEAM_VIEW("CRMSFA_TEAM_VIEW"),
    /** Access to the CRM/SFA Application. */
    CRMSFA_VIEW("CRMSFA_VIEW"),
    /** Use the Data File Maintenance pages. */
    DATAFILE_MAINT("DATAFILE_MAINT"),
    /** Basic permission to execute import in Dataimport application. */
    DATAIMPORT_ADMIN("DATAIMPORT_ADMIN"),
    /** Basic permission to use the Dataimport application. */
    DATAIMPORT_VIEW("DATAIMPORT_VIEW"),
    /** View operations in the eBay application. */
    EBAY_VIEW("EBAY_VIEW"),
    /** ALL with the Entity Data Maintenance pages. */
    ENTITY_DATA_ADMIN("ENTITY_DATA_ADMIN"),
    /** Create with the Entity Data Maintenance pages. */
    ENTITY_DATA_CREATE("ENTITY_DATA_CREATE"),
    /** Delete with the Entity Data Maintenance pages. */
    ENTITY_DATA_DELETE("ENTITY_DATA_DELETE"),
    /** Update with the Entity Data Maintenance pages. */
    ENTITY_DATA_UPDATE("ENTITY_DATA_UPDATE"),
    /** View with the Entity Data Maintenance pages. */
    ENTITY_DATA_VIEW("ENTITY_DATA_VIEW"),
    /** Use the Entity Maintenance pages. */
    ENTITY_MAINT("ENTITY_MAINT"),
    /** Use the Entity Sync Admin pages. */
    ENTITY_SYNC_ADMIN("ENTITY_SYNC_ADMIN"),
    /** Use the Enum and Status Maintenance pages. */
    ENUM_STATUS_MAINT("ENUM_STATUS_MAINT"),
    /** ALL operations in the Facility Manager. */
    FACILITY_ADMIN("FACILITY_ADMIN"),
    /** Create operations in the Facility Manager. */
    FACILITY_CREATE("FACILITY_CREATE"),
    /** Delete operations in the Facility Manager. */
    FACILITY_DELETE("FACILITY_DELETE"),
    /** Limited update operations in the Facility Manager. */
    FACILITY_ROLE_UPDATE("FACILITY_ROLE_UPDATE"),
    /** Limited view operations in the Facility Manager. */
    FACILITY_ROLE_VIEW("FACILITY_ROLE_VIEW"),
    /** Update operations in the Facility Manager. */
    FACILITY_UPDATE("FACILITY_UPDATE"),
    /** View operations in the Facility Manager. */
    FACILITY_VIEW("FACILITY_VIEW"),
    /** All operations in Financials application. */
    FINANCIALS_ADMIN("FINANCIALS_ADMIN"),
    /** Create invoices in the [Payables] tab and screens. */
    FINANCIALS_AP_INCRTE("FINANCIALS_AP_INCRTE"),
    /** Update invoices in the [Payables] tab and screens. */
    FINANCIALS_AP_INUPDT("FINANCIALS_AP_INUPDT"),
    /** View invoices in the [Payables] tab and screens. */
    FINANCIALS_AP_INVIEW("FINANCIALS_AP_INVIEW"),
    /** Write off invoices in the [Payables] tab and screens. */
    FINANCIALS_AP_INWRTOF("FINANCIALS_AP_INWRTOF"),
    /** Apply payments in the [Payables] tab and screens. */
    FINANCIALS_AP_PAPPL("FINANCIALS_AP_PAPPL"),
    /** Create payments in the [Payables] tab and screens. */
    FINANCIALS_AP_PCRTE("FINANCIALS_AP_PCRTE"),
    /** Update payments in the [Payables] tab and screens. */
    FINANCIALS_AP_PUPDT("FINANCIALS_AP_PUPDT"),
    /** View payments in the [Payables] tab and screens. */
    FINANCIALS_AP_PVIEW("FINANCIALS_AP_PVIEW"),
    /** View operations in the [Payables] tab and all screens inside it. */
    FINANCIALS_AP_VIEW("FINANCIALS_AP_VIEW"),
    /** Create invoices in the [Receivables] tab and screens. */
    FINANCIALS_AR_INCRTE("FINANCIALS_AR_INCRTE"),
    /** Update invoices in the [Receivables] tab and screens. */
    FINANCIALS_AR_INUPDT("FINANCIALS_AR_INUPDT"),
    /** View invoices in the [Receivables] tab and screens. */
    FINANCIALS_AR_INVIEW("FINANCIALS_AR_INVIEW"),
    /** Write off invoices in the [Receivables] tab and screens. */
    FINANCIALS_AR_INWRTOF("FINANCIALS_AR_INWRTOF"),
    /** Apply payments in the [Receivables] tab and screens. */
    FINANCIALS_AR_PAPPL("FINANCIALS_AR_PAPPL"),
    /** Create payments in the [Receivables] tab and screens. */
    FINANCIALS_AR_PCRTE("FINANCIALS_AR_PCRTE"),
    /** Update payments in the [Receivables] tab and screens. */
    FINANCIALS_AR_PUPDT("FINANCIALS_AR_PUPDT"),
    /** View payments in the [Receivables] tab and screens. */
    FINANCIALS_AR_PVIEW("FINANCIALS_AR_PVIEW"),
    /** View operations in the [Receivables] tab and all screens inside it. */
    FINANCIALS_AR_VIEW("FINANCIALS_AR_VIEW"),
    /** Create and update commission agreements. */
    FINANCIALS_COMM_UPDT("FINANCIALS_COMM_UPDT"),
    /** View commission agreements. */
    FINANCIALS_COMM_VIEW("FINANCIALS_COMM_VIEW"),
    /** Configuration operations in Financials. */
    FINANCIALS_CONFIG("FINANCIALS_CONFIG"),
    /** Create employee paychecks. */
    FINANCIALS_EMP_PCCRTE("FINANCIALS_EMP_PCCRTE"),
    /** Update employee paychecks. */
    FINANCIALS_EMP_PCUPDT("FINANCIALS_EMP_PCUPDT"),
    /** View [Employee] tab and screens. */
    FINANCIALS_EMP_VIEW("FINANCIALS_EMP_VIEW"),
    /** Create partner agreements. */
    FINANCIALS_PARTNER_AGREEMENT_CREATE("FINANCIALS_PARTNER_AGREEMENT_CREATE"),
    /** Update partner agreements. */
    FINANCIALS_PARTNER_AGREEMENT_UPDATE("FINANCIALS_PARTNER_AGREEMENT_UPDATE"),
    /** View partner agreements. */
    FINANCIALS_PARTNER_AGREEMENT_VIEW("FINANCIALS_PARTNER_AGREEMENT_VIEW"),
    /** Create partner invioces. */
    FINANCIALS_PARTNER_INVOICE_CREATE("FINANCIALS_PARTNER_INVOICE_CREATE"),
    /** Update partner invoices. */
    FINANCIALS_PARTNER_INVOICE_UPDATE("FINANCIALS_PARTNER_INVOICE_UPDATE"),
    /** View partner invioces. */
    FINANCIALS_PARTNER_INVOICE_VIEW("FINANCIALS_PARTNER_INVOICE_VIEW"),
    /** View [Partners] tab and screens. */
    FINANCIALS_PARTNER_VIEW("FINANCIALS_PARTNER_VIEW"),
    /** Control invoice recurrence. */
    FINANCIALS_RECUR_INV("FINANCIALS_RECUR_INV"),
    /** Reverse transactions. */
    FINANCIALS_REVERSE("FINANCIALS_REVERSE"),
    /** View operations in the  [Reports] tab and all of the reports inside it. */
    FINANCIALS_RPT_VIEW("FINANCIALS_RPT_VIEW"),
    /** Create and update sales agreements. */
    FINANCIALS_SAGR_UPDT("FINANCIALS_SAGR_UPDT"),
    /** View sales agreements. */
    FINANCIALS_SAGR_VIEW("FINANCIALS_SAGR_VIEW"),
    /** View operations in the [Transactions] tab and all of the screens inside it. */
    FINANCIALS_TX_VIEW("FINANCIALS_TX_VIEW"),
    /** View operations in the Financials Manager. */
    FINANCIALS_VIEW("FINANCIALS_VIEW"),
    /** View operations in the Google Base application. */
    GOOGLEBASE_VIEW("GOOGLEBASE_VIEW"),
    /** Manual Payment Transaction. */
    MANUAL_PAYMENT("MANUAL_PAYMENT"),
    /** ALL operations in the Manufacturing Manager. */
    MANUFACTURING_ADMIN("MANUFACTURING_ADMIN"),
    /** Create operations in the Manufacturing Manager. */
    MANUFACTURING_CREATE("MANUFACTURING_CREATE"),
    /** Delete operations in the Manufacturing Manager. */
    MANUFACTURING_DELETE("MANUFACTURING_DELETE"),
    /** Update operations in the Manufacturing Manager. */
    MANUFACTURING_UPDATE("MANUFACTURING_UPDATE"),
    /** View operations in the Manufacturing Manager. */
    MANUFACTURING_VIEW("MANUFACTURING_VIEW"),
    /** ALL operations in the Marketing Manager. */
    MARKETING_ADMIN("MARKETING_ADMIN"),
    /** Create operations in the Marketing Manager. */
    MARKETING_CREATE("MARKETING_CREATE"),
    /** Delete operations in the Marketing Manager. */
    MARKETING_DELETE("MARKETING_DELETE"),
    /** Limited update operations in the Marketing Manager. */
    MARKETING_ROLE_UPDATE("MARKETING_ROLE_UPDATE"),
    /** Limited view operations in the Marketing Manager. */
    MARKETING_ROLE_VIEW("MARKETING_ROLE_VIEW"),
    /** Update operations in the Marketing Manager. */
    MARKETING_UPDATE("MARKETING_UPDATE"),
    /** View operations in the Marketing Manager. */
    MARKETING_VIEW("MARKETING_VIEW"),
    /** Permission to access the Stock OFBiz Manager Applications. */
    OFBTOOLS_VIEW("OFBTOOLS_VIEW"),
    /** Create sales forecasts in the Order Manager. */
    ORDERMGR_4C_CREATE("ORDERMGR_4C_CREATE"),
    /** Update sales forecasts in the Order Manager. */
    ORDERMGR_4C_UPDATE("ORDERMGR_4C_UPDATE"),
    /** ALL operations in the Order Manager. */
    ORDERMGR_ADMIN("ORDERMGR_ADMIN"),
    /** Create operations in the Order Manager. */
    ORDERMGR_CREATE("ORDERMGR_CREATE"),
    /** Create Customer Requests in the Order Manager. */
    ORDERMGR_CRQ_CREATE("ORDERMGR_CRQ_CREATE"),
    /** Delete operations in the Order Manager. */
    ORDERMGR_DELETE("ORDERMGR_DELETE"),
    /** Create notes in the Order Manager. */
    ORDERMGR_NOTE("ORDERMGR_NOTE"),
    /** Create purchase orders in the Order Manager. */
    ORDERMGR_PURCHASE_CREATE("ORDERMGR_PURCHASE_CREATE"),
    /** Purchase Order Entry in the Order Manager. */
    ORDERMGR_PURCHASE_ENTRY("ORDERMGR_PURCHASE_ENTRY"),
    /** View purchase orders in the Order Manager. */
    ORDERMGR_PURCHASE_VIEW("ORDERMGR_PURCHASE_VIEW"),
    /** Quote price manager in the Order Manager. */
    ORDERMGR_QUOTE_PRICE("ORDERMGR_QUOTE_PRICE"),
    /** Process returns in the Order Manager. */
    ORDERMGR_RETURN("ORDERMGR_RETURN"),
    /** Limited Create operations in the Order Manager. */
    ORDERMGR_ROLE_CREATE("ORDERMGR_ROLE_CREATE"),
    /** Limited Delete operations in the Order Manager. */
    ORDERMGR_ROLE_DELETE("ORDERMGR_ROLE_DELETE"),
    /** Limited Update operations in the Order Manager. */
    ORDERMGR_ROLE_UPDATE("ORDERMGR_ROLE_UPDATE"),
    /** Limited view operations in the Order Manager. */
    ORDERMGR_ROLE_VIEW("ORDERMGR_ROLE_VIEW"),
    /** Create sales orders for all stores in the Order Manager. */
    ORDERMGR_SALES_CREATE("ORDERMGR_SALES_CREATE"),
    /** Sales Order Entry in the Order Manager. */
    ORDERMGR_SALES_ENTRY("ORDERMGR_SALES_ENTRY"),
    /** Price change permission for sales orders. */
    ORDERMGR_SALES_PRICEMOD("ORDERMGR_SALES_PRICEMOD"),
    /** Send order confirmation notification. */
    ORDERMGR_SEND_CONFIRMATION("ORDERMGR_SEND_CONFIRMATION"),
    /** Update operations in the Order Manager. */
    ORDERMGR_UPDATE("ORDERMGR_UPDATE"),
    /** View operations in the Order Manager. */
    ORDERMGR_VIEW("ORDERMGR_VIEW"),
    /** ALL operations in the Party Manager. */
    PARTYMGR_ADMIN("PARTYMGR_ADMIN"),
    /** Create communication event. */
    PARTYMGR_CME_CREATE("PARTYMGR_CME_CREATE"),
    /** Delete communication event. */
    PARTYMGR_CME_DELETE("PARTYMGR_CME_DELETE"),
    /** Update communication event. */
    PARTYMGR_CME_UPDATE("PARTYMGR_CME_UPDATE"),
    /** Create operations in the Party Manager. */
    PARTYMGR_CREATE("PARTYMGR_CREATE"),
    /** Delete operations in the Party Manager. */
    PARTYMGR_DELETE("PARTYMGR_DELETE"),
    /** Update PartyGroup or Person detail information. */
    PARTYMGR_GRP_UPDATE("PARTYMGR_GRP_UPDATE"),
    /** Create notes in the Party Manager. */
    PARTYMGR_NOTE("PARTYMGR_NOTE"),
    /** Create party contact mechs in the Party Manager. */
    PARTYMGR_PCM_CREATE("PARTYMGR_PCM_CREATE"),
    /** Delete party contact mechs in the Party Manager. */
    PARTYMGR_PCM_DELETE("PARTYMGR_PCM_DELETE"),
    /** Update party contact mechs in the Party Manager. */
    PARTYMGR_PCM_UPDATE("PARTYMGR_PCM_UPDATE"),
    /** Create party quals in the Party Manager. */
    PARTYMGR_QAL_CREATE("PARTYMGR_QAL_CREATE"),
    /** Delete party quals in the Party Manager. */
    PARTYMGR_QAL_DELETE("PARTYMGR_QAL_DELETE"),
    /** Update party quals in the Party Manager. */
    PARTYMGR_QAL_UPDATE("PARTYMGR_QAL_UPDATE"),
    /** Create party relationships in the Party Manager. */
    PARTYMGR_REL_CREATE("PARTYMGR_REL_CREATE"),
    /** Delete party relationships in the Party Manager. */
    PARTYMGR_REL_DELETE("PARTYMGR_REL_DELETE"),
    /** Update party relationships in the Party Manager. */
    PARTYMGR_REL_UPDATE("PARTYMGR_REL_UPDATE"),
    /** Create party roles in the Party Manager. */
    PARTYMGR_ROLE_CREATE("PARTYMGR_ROLE_CREATE"),
    /** Delete party roles in the Party Manager. */
    PARTYMGR_ROLE_DELETE("PARTYMGR_ROLE_DELETE"),
    /** Create party to data source relations. */
    PARTYMGR_SRC_CREATE("PARTYMGR_SRC_CREATE"),
    /** Update party status in the Party Manager. */
    PARTYMGR_STS_UPDATE("PARTYMGR_STS_UPDATE"),
    /** Update operations in the Party Manager. */
    PARTYMGR_UPDATE("PARTYMGR_UPDATE"),
    /** View operations in the Party Manager. */
    PARTYMGR_VIEW("PARTYMGR_VIEW"),
    /** ALL operations in the Payment Processors Setup. */
    PAYPROC_ADMIN("PAYPROC_ADMIN"),
    /** Create operations in the Payment Processors Setup. */
    PAYPROC_CREATE("PAYPROC_CREATE"),
    /** Delete operations in the Payment Processors Setup. */
    PAYPROC_DELETE("PAYPROC_DELETE"),
    /** View operations in the Payment Processors Setup. */
    PAYPROC_VIEW("PAYPROC_VIEW"),
    /** ALL Payment Information Operations. */
    PAY_INFO_ADMIN("PAY_INFO_ADMIN"),
    /** Create Payment Information. */
    PAY_INFO_CREATE("PAY_INFO_CREATE"),
    /** Delete Payment Information. */
    PAY_INFO_DELETE("PAY_INFO_DELETE"),
    /** Update Payment Information. */
    PAY_INFO_UPDATE("PAY_INFO_UPDATE"),
    /** View Payment Information. */
    PAY_INFO_VIEW("PAY_INFO_VIEW"),
    /** Use the Period Maintenance pages. */
    PERIOD_MAINT("PERIOD_MAINT"),
    /** Basic permission to create and update Manufacturing. */
    PRCH_MFG_ADMIN("PRCH_MFG_ADMIN"),
    /** Basic permission to view the Manufacturing. */
    PRCH_MFG_VIEW("PRCH_MFG_VIEW"),
    /** Permission to approve requirements. */
    PRCH_PLAN_APRV("PRCH_PLAN_APRV"),
    /** Basic permission to use the Planning tab. */
    PRCH_PLAN_VIEW("PRCH_PLAN_VIEW"),
    /** Permission to create Purchase Orders. */
    PRCH_PO_CREATE("PRCH_PO_CREATE"),
    /** Basic permission to use the Purchase Order tab. */
    PRCH_PO_VIEW("PRCH_PO_VIEW"),
    /** View operations in the  [Reports] tab and all of the reports inside it. */
    PRCH_RPT_VIEW("PRCH_RPT_VIEW"),
    /** Permission to create suppliers. */
    PRCH_SPLR_CREATE("PRCH_SPLR_CREATE"),
    /** Permission to modify suppliers. */
    PRCH_SPLR_UPDATE("PRCH_SPLR_UPDATE"),
    /** Basic permission to use the Supplier tab. */
    PRCH_SPLR_VIEW("PRCH_SPLR_VIEW"),
    /** Basic permission to use the Purchasing application. */
    PRCH_VIEW("PRCH_VIEW"),
    /** Permission to configure backup warehouses. */
    PRCH_WRHS_CONFIG("PRCH_WRHS_CONFIG"),
    /** ALL operations in the Security Management Screens. */
    SECURITY_ADMIN("SECURITY_ADMIN"),
    /** Create operations in the Security Management Screens. */
    SECURITY_CREATE("SECURITY_CREATE"),
    /** Delete operations in the Security Management Screens. */
    SECURITY_DELETE("SECURITY_DELETE"),
    /** Update operations in the Security Management Screens. */
    SECURITY_UPDATE("SECURITY_UPDATE"),
    /** View operations in the Security Management Screens. */
    SECURITY_VIEW("SECURITY_VIEW"),
    /** Send to the Control Applet. */
    SEND_CONTROL_APPLET("SEND_CONTROL_APPLET"),
    /** View the Server Statistics pages. */
    SERVER_STATS_VIEW("SERVER_STATS_VIEW"),
    /** Permission to invoke any service remotely. */
    SERVICE_INVOKE_ANY("SERVICE_INVOKE_ANY"),
    /** Use the Service Maintenance pages. */
    SERVICE_MAINT("SERVICE_MAINT"),
    /** ALL operations in the Shipping Rate Editor. */
    SHIPRATE_ADMIN("SHIPRATE_ADMIN"),
    /** Create operations in the Shipping Rate Editor. */
    SHIPRATE_CREATE("SHIPRATE_CREATE"),
    /** Delete operations in the Shipping Rate Editor. */
    SHIPRATE_DELETE("SHIPRATE_DELETE"),
    /** View operations in the Shipping Rate Editor. */
    SHIPRATE_VIEW("SHIPRATE_VIEW"),
    /** ALL operations in the Tax Rate Editor. */
    TAXRATE_ADMIN("TAXRATE_ADMIN"),
    /** Create operations in the Tax Rate Editor. */
    TAXRATE_CREATE("TAXRATE_CREATE"),
    /** Delete operations in the Tax Rate Editor. */
    TAXRATE_DELETE("TAXRATE_DELETE"),
    /** View operations in the Tax Rate Editor. */
    TAXRATE_VIEW("TAXRATE_VIEW"),
    /** Edit a UtilCache instance. */
    UTIL_CACHE_EDIT("UTIL_CACHE_EDIT"),
    /** View a UtilCache instance. */
    UTIL_CACHE_VIEW("UTIL_CACHE_VIEW"),
    /** Edit a UtilDebug instance. */
    UTIL_DEBUG_EDIT("UTIL_DEBUG_EDIT"),
    /** View a UtilDebug instance. */
    UTIL_DEBUG_VIEW("UTIL_DEBUG_VIEW"),
    /** Permission to access the WebTools Menu. */
    WEBTOOLS_VIEW("WEBTOOLS_VIEW"),
    /** ALL operations in the Work Effort Manager. */
    WORKEFFORTMGR_ADMIN("WORKEFFORTMGR_ADMIN"),
    /** Create operations in the Work Effort Manager. */
    WORKEFFORTMGR_CREATE("WORKEFFORTMGR_CREATE"),
    /** Delete operations in the Work Effort Manager. */
    WORKEFFORTMGR_DELETE("WORKEFFORTMGR_DELETE"),
    /** Create work effort roles in the Work Effort Manager. */
    WORKEFFORTMGR_ROLE_CREATE("WORKEFFORTMGR_ROLE_CREATE"),
    /** Update work effort roles in the Work Effort Manager. */
    WORKEFFORTMGR_ROLE_UPDATE("WORKEFFORTMGR_ROLE_UPDATE"),
    /** View work effort roles in the Work Effort Manager. */
    WORKEFFORTMGR_ROLE_VIEW("WORKEFFORTMGR_ROLE_VIEW"),
    /** Update operations in the Work Effort Manager. */
    WORKEFFORTMGR_UPDATE("WORKEFFORTMGR_UPDATE"),
    /** View operations in the Work Effort Manager. */
    WORKEFFORTMGR_VIEW("WORKEFFORTMGR_VIEW"),
    /** Use the Workflow Maintenance pages. */
    WORKFLOW_MAINT("WORKFLOW_MAINT"),
    /** Admin permission for any operation in any facility. */
    WRHS_ADMIN("WRHS_ADMIN"),
    /** Permission to create and configure a warehouse. */
    WRHS_CONFIG("WRHS_CONFIG"),
    /** Permission to use the config tab. */
    WRHS_CONFIG_VIEW("WRHS_CONFIG_VIEW"),
    /** Permission to view invoices of packed orders. */
    WRHS_INVOICE_VIEW("WRHS_INVOICE_VIEW"),
    /** Permission to create new lot. */
    WRHS_INV_LOT_CREATE("WRHS_INV_LOT_CREATE"),
    /** Permission to update a lot. */
    WRHS_INV_LOT_UPDATE("WRHS_INV_LOT_UPDATE"),
    /** Permission to view lot level screens. */
    WRHS_INV_LOT_VIEW("WRHS_INV_LOT_VIEW"),
    /** Permission to modify physical inventory. */
    WRHS_INV_PHINV("WRHS_INV_PHINV"),
    /** Permission to receive Purchase Orders. */
    WRHS_INV_RCPO("WRHS_INV_RCPO"),
    /** Permission to override calculated unit cost of received inventory. */
    WRHS_INV_SETCOST("WRHS_INV_SETCOST"),
    /** Permission to perform stock moves. */
    WRHS_INV_STKMV("WRHS_INV_STKMV"),
    /** Basic permission to use the Inventory tab. */
    WRHS_INV_VIEW("WRHS_INV_VIEW"),
    /** Permission to perform inventory transfers. */
    WRHS_INV_XFER("WRHS_INV_XFER"),
    /** Permission to create and update production runs. */
    WRHS_MFG_CREATE("WRHS_MFG_CREATE"),
    /** Basic permission to use the Manufacturing tab. */
    WRHS_MFG_VIEW("WRHS_MFG_VIEW"),
    /** Permission to pack orders. */
    WRHS_SHIP_PACK("WRHS_SHIP_PACK"),
    /** Permission to create picklists. */
    WRHS_SHIP_PICK_CREATE("WRHS_SHIP_PICK_CREATE"),
    /** Permission to view picklists. */
    WRHS_SHIP_PICK_VIEW("WRHS_SHIP_PICK_VIEW"),
    /** Permission to schedule shipments and print labels. */
    WRHS_SHIP_SCHED("WRHS_SHIP_SCHED"),
    /** Basic permission to use the Shipment tab. */
    WRHS_SHIP_VIEW("WRHS_SHIP_VIEW"),
    /** Basic permission to use the Warehouse application. */
    WRHS_VIEW("WRHS_VIEW");

    private final String permissionId;
    private Permission(String permissionId) {
        this.permissionId = permissionId;
    }

    /**
     * Gets the corresponding permission id.
     * @return the permission
     */
    public String getPermissionId() {
        return permissionId;
    }

    /**
     * Checks against a permission id.
     * @param permissionId the permission to check for
     * @return a <code>boolean</code>
     */
    public boolean equals(String permissionId) {
        return this.permissionId.equals(permissionId);
    }

    /**
     * Checks if the user has the given permission.
     * @param permission a <code>Permission</code> value
     * @return a <code>boolean</code> value
     */
    public static boolean hasPermission(Permission permission) {
        return hasPermission(permission.getPermissionId());
    }

    private static native boolean hasPermission(String permission)/*-{
        if ($wnd.securityUser) {
          return $wnd.securityUser[permission];
        } else {
          return false;
        }
    }-*/;

}
