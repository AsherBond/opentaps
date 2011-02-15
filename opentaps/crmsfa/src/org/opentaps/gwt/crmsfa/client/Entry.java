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

package org.opentaps.gwt.crmsfa.client;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.http.client.Request;
import com.google.gwt.http.client.RequestBuilder;
import com.google.gwt.http.client.RequestCallback;
import com.google.gwt.http.client.RequestException;
import com.google.gwt.http.client.Response;
import com.google.gwt.i18n.client.Dictionary;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Hyperlink;
import com.google.gwt.user.client.ui.RootPanel;
import com.gwtext.client.util.Format;
import com.gwtext.client.widgets.Panel;

import org.opentaps.gwt.common.client.BaseEntry;
import org.opentaps.gwt.common.client.UtilUi;
import org.opentaps.gwt.common.client.form.FindAccountsForm;
import org.opentaps.gwt.common.client.form.FindContactsForm;
import org.opentaps.gwt.common.client.form.FormNotificationInterface;
import org.opentaps.gwt.common.client.form.LookupAccountsWindow;
import org.opentaps.gwt.common.client.form.LookupContactsWindow;
import org.opentaps.gwt.common.client.form.MultiSearchForm;
import org.opentaps.gwt.common.client.form.OrderItemsEditable;
import org.opentaps.gwt.common.client.form.ServiceErrorReader;
import org.opentaps.gwt.common.client.listviews.AccountSearchListView;
import org.opentaps.gwt.common.client.listviews.CaseSearchListView;
import org.opentaps.gwt.common.client.listviews.ContactSearchListView;
import org.opentaps.gwt.common.client.listviews.LeadSearchListView;
import org.opentaps.gwt.common.client.listviews.OrderItemsEditableListView.OrderType;
import org.opentaps.gwt.common.client.listviews.SalesOpportunitySearchListView;
import org.opentaps.gwt.common.client.listviews.SalesOrderSearchListView;
import org.opentaps.gwt.common.client.lookup.configuration.PartyLookupConfiguration;
import org.opentaps.gwt.common.client.security.Permission;
import org.opentaps.gwt.crmsfa.client.accounts.form.AccountsSublistView;
import org.opentaps.gwt.crmsfa.client.accounts.form.QuickNewAccountForm;
import org.opentaps.gwt.crmsfa.client.cases.form.CaseSublistView;
import org.opentaps.gwt.crmsfa.client.cases.form.FindCasesForm;
import org.opentaps.gwt.crmsfa.client.cases.form.QuickNewCaseForm;
import org.opentaps.gwt.crmsfa.client.contacts.form.ContactsSublistView;
import org.opentaps.gwt.crmsfa.client.contacts.form.QuickNewContactForm;
import org.opentaps.gwt.crmsfa.client.leads.form.FindLeadsForm;
import org.opentaps.gwt.crmsfa.client.leads.form.QuickNewLeadForm;
import org.opentaps.gwt.crmsfa.client.opportunities.form.FindOpportunitiesForm;
import org.opentaps.gwt.crmsfa.client.opportunities.form.OpportunitiesSublistView;
import org.opentaps.gwt.crmsfa.client.opportunities.form.QuickNewOpportunityForm;
import org.opentaps.gwt.crmsfa.client.orders.form.FindOrdersForm;
import org.opentaps.gwt.crmsfa.client.orders.form.ProductReReservationForm;
import org.opentaps.gwt.crmsfa.client.orders.form.SalesOrdersSublistView;
import org.opentaps.gwt.crmsfa.client.partners.form.FindPartnersForm;

/**
 * Entry point classes define <code>onModuleLoad()</code>.
 */
public class Entry extends BaseEntry {

    private static final String MODULE = Entry.class.getName();

    private FindContactsForm findContactsForm;
    private FindContactsForm myContactsForm;
    private QuickNewContactForm quickNewContactForm;

    private FindLeadsForm findLeadsForm;
    private FindLeadsForm myLeadsForm;
    private QuickNewLeadForm quickNewLeadForm;

    private QuickNewOpportunityForm quickNewOpportunityForm;
    private FindOpportunitiesForm findOpportunitiesForm;
    private FindOpportunitiesForm myOpportunitiesForm;

    private FindOrdersForm findOrdersForm;
    private FindOrdersForm myOrdersForm;

    private FindPartnersForm findPartnersForm;

    private FindAccountsForm findAccountsForm;
    private FindAccountsForm myAccountsForm;
    private QuickNewAccountForm quickNewAccountForm;

    private QuickNewCaseForm quickNewCaseForm;
    private FindCasesForm findCasesForm;
    private FindCasesForm myCasesForm;

    private MultiSearchForm multiCrmsfaSearch;

    // CRMSFA/Contacts identifiers
    private static final String MY_CONTACTS_ID = "myContacts";
    private static final String FIND_CONTACTS_ID = "findContacts";
    private static final String LOOKUP_CONTACTS_ID = "lookupContacts";
    private static final String QUICK_CREATE_CONTACT_ID = "quickNewContact";
    /** List of related opportunities on view contact page. */
    private static final String CONTACT_OPPORTUNITIES = "contactOpportunitiesSubListView";
    /** List of related accounts on view contact page. */
    private static final String CONTACT_ACCOUNTS = "contactAccountsSubListView";
    /** An ID where CONTACT_ACCOUNTS may place "Assign Account" button. */
    private static final String ASSIGN_ACCOUNT_TO_CONTACT = "assignAccountToContact";
    /** List of open orders for a contact. */
    private static final String CONTACT_ORDERS = "contactOpenOrdersSubsection";
    /** List of cases for a contact. */
    private static final String CONTACT_CASES = "contactCasesSubsection";

    // CRMSFA/Leads widget identifiers
    private static final String FIND_LEADS_ID = "findLeads";
    private static final String MY_LEADS_ID = "myLeads";
    private static final String LOOKUP_LEADS_ID = "lookupLeads";
    private static final String QUICK_CREATE_LEAD_ID = "quickNewLead";
    private static final String LEAD_OPPORTUNITIES = "leadOpportunitiesSubListView";

    // CRMSFA/Opportunities widget identifiers
    private static final String QUICK_CREATE_OPPORTUNITY_ID = "quickNewOpportunity";
    private static final String MY_OPPORTUNITIES_ID = "myOpportunities";
    private static final String FIND_OPPORTUNITIES_ID = "findOpportunities";
    /** List of assigned contacts on view opportunity page. */
    private static final String OPPORTUNITY_CONTACTS = "opportunityContactsSubListView";
    /** An ID where OPPORTUNITY_CONTACTS may place "Assign Contact" button. */
    private static final String ASSIGN_CONTACT_TO_OPPORTUNITY = "assignContactToOpportunity";

    // CRMSFA/Orders widget identifiers
    private static final String MY_ORDERS_ID = "myOrders";
    private static final String FIND_ORDERS_ID = "findOrders";
    private static final String RE_RESERVE_DIALOG = "reReserveItemDialog";
    private static final String ORDER_ITEMS_ID = "orderItemsEntryGrid";

    // CRMSFA/Partners widget identifiers
    private static final String FIND_PARTNERS_ID = "findPartners";

    // CRMSFA/Accounts widget identifiers
    private static final String MY_ACCOUNTS_ID = "myAccounts";
    private static final String FIND_ACCOUNTS_ID = "findAccounts";
    private static final String LOOKUP_ACCOUNTS_ID = "lookupAccounts";
    private static final String QUICK_CREATE_ACCOUNT_ID = "quickNewAccount";
    /** List of assigned contacts on view account page. */
    private static final String ACCOUNT_CONTACTS = "contactsSubListView";
    /** An ID where CONTACTS_SUB_LISTVIEW may place "Assign Contact" button. */
    private static final String ASSIGN_CONTACT_WIDGET = "assignContactToAccount";
    /** List of related opportunities on view account page. */
    private static final String ACCOUNT_OPPORTUNITIES = "accountOpportunitiesSubListView";
    /** List of open orders for an account. */
    private static final String ACCOUNT_ORDERS = "accountOpenOrdersSubsection";
    /** List of cases for an account. */
    private static final String ACCOUNT_CASES = "accountCasesSubsection";

    // CRMSFA/Cases widget identifiers
    private static final String QUICK_CREATE_CASE_ID = "quickNewCase";
    private static final String MY_CASES_ID = "myCases";
    private static final String FIND_CASES_ID = "findCases";

    private static final String CRMSFA_SEARCH_ID = "gwtSearch";

    private static final int PARTIES_PAGE_SIZE = 20;

    /**
     * This is the entry point method.
     * It is loaded for page where the meta tag is found.
     */
    public void onModuleLoad() {

        loadAccountsWidgets();
        loadContactsWidgets();
        loadLeadsWidgets();
        loadOpportunitiesWidgets();
        loadOrdersWidgets();
        loadPartnersWidgets();
        loadCasesWidgets();

        if (RootPanel.get(CRMSFA_SEARCH_ID) != null) {

            // Add search list views according to user security.

            multiCrmsfaSearch = new MultiSearchForm();

            if (Permission.hasPermission(Permission.CRMSFA_ACCOUNT_VIEW)) {
                multiCrmsfaSearch.addResultsGrid(new AccountSearchListView());
            }
            if (Permission.hasPermission(Permission.CRMSFA_CONTACTS_VIEW)) {
                multiCrmsfaSearch.addResultsGrid(new ContactSearchListView());
            }
            if (Permission.hasPermission(Permission.CRMSFA_LEADS_VIEW)) {
                multiCrmsfaSearch.addResultsGrid(new LeadSearchListView());
            }
            if (Permission.hasPermission(Permission.CRMSFA_CASES_VIEW)) {
                multiCrmsfaSearch.addResultsGrid(new CaseSearchListView());
            }
            if (Permission.hasPermission(Permission.CRMSFA_OPPS_VIEW)) {
               multiCrmsfaSearch.addResultsGrid(new SalesOpportunitySearchListView());
            }
            if (Permission.hasPermission(Permission.CRMSFA_ORDERS_VIEW)) {
                multiCrmsfaSearch.addResultsGrid(new SalesOrderSearchListView());
            }

            RootPanel.get(CRMSFA_SEARCH_ID).add(multiCrmsfaSearch);
        }
    }

    private void loadAccountsWidgets() {
        if (RootPanel.get(FIND_ACCOUNTS_ID) != null) {
            loadFindAccounts();
        }

        if (RootPanel.get(MY_ACCOUNTS_ID) != null) {
            loadMyAccounts();
        }

        if (RootPanel.get(QUICK_CREATE_ACCOUNT_ID) != null) {
            loadQuickNewAccount();
            if (myAccountsForm != null) {
                quickNewAccountForm.register(myAccountsForm.getListView());
            }
            // for handling refresh of lists on contact creation
            if (findAccountsForm != null) {
                quickNewAccountForm.register(findAccountsForm.getListView());
            }
        }

        if (RootPanel.get(LOOKUP_ACCOUNTS_ID) != null) {
            loadLookupAccounts();
        }

        if (RootPanel.get(ACCOUNT_CONTACTS) != null) {
            loadAccountContacts();
        }

        if (RootPanel.get(ACCOUNT_OPPORTUNITIES) != null) {
            loadAccountOpportunities();
        }

        if (RootPanel.get(ACCOUNT_ORDERS) != null) {
            loadAccountOrders();
        }

        if (RootPanel.get(ACCOUNT_CASES) != null) {
            loadAccountCases();
        }
    }

    private void loadContactsWidgets() {
        if (RootPanel.get(FIND_CONTACTS_ID) != null) {
            loadFindContacts();
        }

        if (RootPanel.get(MY_CONTACTS_ID) != null) {
            loadMyContacts();
        }

        if (RootPanel.get(QUICK_CREATE_CONTACT_ID) != null) {
            loadQuickNewContact();
            // for handling refresh of lists on contact creation
            if (myContactsForm != null) {
                quickNewContactForm.register(myContactsForm.getListView());
            }
            if (findContactsForm != null) {
                quickNewContactForm.register(findContactsForm.getListView());
            }
        }

        if (RootPanel.get(LOOKUP_CONTACTS_ID) != null) {
            loadLookupContacts();
        }

        if (RootPanel.get(CONTACT_OPPORTUNITIES) != null) {
            loadContactOpportunities();
        }

        if (RootPanel.get(CONTACT_ACCOUNTS) != null) {
            loadContactAccounts();
        }

        if (RootPanel.get(CONTACT_ORDERS) != null) {
            loadContactOrders();
        }

        if (RootPanel.get(CONTACT_CASES) != null) {
            loadContactCases();
        }
    }

    private void loadLeadsWidgets() {
        if (RootPanel.get(MY_LEADS_ID) != null) {
            loadMyLeads();
        }

        if (RootPanel.get(FIND_LEADS_ID) != null) {
            loadFindLeads();
        }

        if (RootPanel.get(QUICK_CREATE_LEAD_ID) != null) {
            loadQuickNewLead();
            // for handling refresh of lists on contact creation
            if (findLeadsForm != null) {
                quickNewLeadForm.register(findLeadsForm.getListView());
            }
            if (myLeadsForm != null) {
                quickNewLeadForm.register(myLeadsForm.getListView());
            }
        }

        if (RootPanel.get(LOOKUP_LEADS_ID) != null) {
            loadLookupLeads();
        }

        if (RootPanel.get(LEAD_OPPORTUNITIES) != null) {
            loadLeadOpportunities();
        }
    }

    private void loadOpportunitiesWidgets() {
        if (RootPanel.get(MY_OPPORTUNITIES_ID) != null) {
            loadMyOpportunities();
        }

        if (RootPanel.get(FIND_OPPORTUNITIES_ID) != null) {
            loadFindOpportunities();
        }
        if (RootPanel.get(QUICK_CREATE_OPPORTUNITY_ID) != null) {
            loadQuickNewOpportunity();
            if (findOpportunitiesForm != null) {
                quickNewOpportunityForm.register(findOpportunitiesForm.getListView());
            }
            if (myOpportunitiesForm != null) {
                quickNewOpportunityForm.register(myOpportunitiesForm.getListView());
            }
        }

        if (RootPanel.get(OPPORTUNITY_CONTACTS) != null) {
            loadOpportunityContacts();
        }

    }

    private void loadOrdersWidgets() {

        /*
         * Order view page may contains number of <div/> with id in form reReserveItemDialog_0_1
         * where first digit is order item index and second one is inventory index for that order
         * item index.
         *
         *  Try to get elements with all reasonable identifiers and install link widgets into them.
         */
        RootPanel currentPanel;
        Integer orderItemIndex = 0;
        Integer inventoryItemIndex = 0;

        while (true) {
            String indexedId = RE_RESERVE_DIALOG + "_" + orderItemIndex.toString() + "_" + inventoryItemIndex.toString();

            // try to find the indexed div in the page
            currentPanel = RootPanel.get(indexedId);

            if (currentPanel != null) {
                // insert the button to re-reserve inventory
                loadReReserveDialog(currentPanel, orderItemIndex, inventoryItemIndex);
                // go to next inventory item
                inventoryItemIndex++;
            } else {
                // if the inventory item index is zero, it means we have no more div to find
                if (inventoryItemIndex == 0) {
                    break;
                } else {
                    // else go to next order item
                    inventoryItemIndex = 0;
                    orderItemIndex++;
                }
            }
        }

        if (RootPanel.get(ORDER_ITEMS_ID) != null) {
            loadOrderItems();
        }
        if (RootPanel.get(MY_ORDERS_ID) != null) {
            loadMyOrders();
        }
        if (RootPanel.get(FIND_ORDERS_ID) != null) {
            loadFindOrders();
        }
    }

    private void loadPartnersWidgets() {
        if (RootPanel.get(FIND_PARTNERS_ID) != null) {
            loadFindPartners();
        }
    }

    private void loadCasesWidgets() {
        if (RootPanel.get(FIND_CASES_ID) != null) {
            loadFindCases();
        }
        if (RootPanel.get(MY_CASES_ID) != null) {
            loadMyCases();
        }
        if (RootPanel.get(QUICK_CREATE_CASE_ID) != null) {
            loadQuickNewCase();
            if (findCasesForm != null) {
                quickNewCaseForm.register(findCasesForm.getListView());
            }
            if (myCasesForm != null) {
                quickNewCaseForm.register(myCasesForm.getListView());
            }
        }
    }

    private void loadQuickNewCase() {
        quickNewCaseForm = new QuickNewCaseForm();
        RootPanel.get(QUICK_CREATE_CASE_ID).add(quickNewCaseForm);
    }

    private void loadFindCases() {
        findCasesForm = new FindCasesForm(true);
        RootPanel.get(FIND_CASES_ID).add(findCasesForm.getMainPanel());
    }


    private void loadMyCases() {
        myCasesForm = new FindCasesForm(false);
        myCasesForm.hideFilters();
        myCasesForm.getListView().filterMyOrTeamParties(getViewPref());
        myCasesForm.getListView().applyFilters();
        RootPanel.get(MY_CASES_ID).add(myCasesForm.getMainPanel());
    }

    private void loadFindLeads() {
        findLeadsForm = new FindLeadsForm();
        findLeadsForm.getListView().setPageSize(PARTIES_PAGE_SIZE);
        findLeadsForm.getListView().applyFilters();
        RootPanel.get(FIND_LEADS_ID).add(findLeadsForm.getMainPanel());
    }

    private void loadLookupLeads() {
        findLeadsForm = new FindLeadsForm();
        findLeadsForm.getListView().setLookupMode();
        findLeadsForm.getListView().filterOutDisabledParties(true);
        findLeadsForm.getListView().applyFilters();
        RootPanel.get(LOOKUP_LEADS_ID).add(findLeadsForm.getMainPanel());
    }

    private void loadQuickNewLead() {
        quickNewLeadForm = new QuickNewLeadForm();
        RootPanel.get(QUICK_CREATE_LEAD_ID).add(quickNewLeadForm);
    }

    private void loadMyLeads() {
        myLeadsForm = new FindLeadsForm();
        myLeadsForm.hideFilters();
        myLeadsForm.getListView().setPageSize(PARTIES_PAGE_SIZE);
        myLeadsForm.getListView().filterMyOrTeamParties(PartyLookupConfiguration.MY_VALUES);
        myLeadsForm.getListView().applyFilters();

        RootPanel.get(MY_LEADS_ID).add(myLeadsForm.getMainPanel());
    }

    /**
     * Load list of opportunities for a qualified lead.<br>
     * Designed to use on view lead page.
     */
    private void loadLeadOpportunities() {
        // setup opportunity list view as subsection
        OpportunitiesSublistView opportunities = new OpportunitiesSublistView();
        // limit displayed opportunities for this lead
        opportunities.filterByLead(getPartyId());
        opportunities.applyFilters();

        // add widget to page
        RootPanel.get(LEAD_OPPORTUNITIES).add(opportunities);
    }

    private void loadFindContacts() {
        findContactsForm = new FindContactsForm();
        findContactsForm.getListView().setPageSize(PARTIES_PAGE_SIZE);
        findContactsForm.getListView().applyFilters();
        RootPanel.get(FIND_CONTACTS_ID).add(findContactsForm.getMainPanel());
    }

    private void loadLookupContacts() {
        findContactsForm = new FindContactsForm();
        findContactsForm.getListView().setLookupMode();
        findContactsForm.getListView().filterOutDisabledParties(true);
        findContactsForm.getListView().applyFilters();
        RootPanel.get(LOOKUP_CONTACTS_ID).add(findContactsForm.getMainPanel());
    }

    private void loadMyContacts() {
        myContactsForm = new FindContactsForm(false);
        myContactsForm.hideFilters();
        myContactsForm.getListView().setPageSize(PARTIES_PAGE_SIZE);
        myContactsForm.getListView().filterMyOrTeamParties(PartyLookupConfiguration.TEAM_VALUES);
        myContactsForm.getListView().applyFilters();
        RootPanel.get(MY_CONTACTS_ID).add(myContactsForm.getMainPanel());
    }

    private void loadQuickNewContact() {
        quickNewContactForm = new QuickNewContactForm();
        RootPanel.get(QUICK_CREATE_CONTACT_ID).add(quickNewContactForm);
    }

    /**
     * Load list of contact opportunities.<br>
     * Designed to use on view contact page.
     */
    private void loadContactOpportunities() {
        // setup opportunity list view as subsection
        OpportunitiesSublistView opportunities = new OpportunitiesSublistView();
        // limit displayed opportunities for this contact
        opportunities.filterByContact(getPartyId());
        opportunities.applyFilters();

        // add widget to page
        RootPanel.get(CONTACT_OPPORTUNITIES).add(opportunities);
    }

    /**
     * Load list of parent accounts for a contact.
     * Designed to use on view contact page.
     */
    private void loadContactAccounts() {
        // setup account list view as subsection
        AccountsSublistView accounts = new AccountsSublistView(getPartyId(), false);
        // limit accounts to the contact
        accounts.filterByContact(getPartyId());
        accounts.applyFilters();

        // add widget to page
        RootPanel.get(CONTACT_ACCOUNTS).add(accounts);

        // if required ID is found setup button [Assign Account] in subsection title
        RootPanel assignAccountButton = null;
        if ((assignAccountButton = RootPanel.get(ASSIGN_ACCOUNT_TO_CONTACT)) != null) {
            loadAssignAccountToContactWidget(assignAccountButton,  accounts);
        }
    }


    /**
     * This method setup a button into subsection title at right-hand position
     * and allow to assign a new account using lookup accounts window.
     * @param container <code>RootPanel</code> that is root container for button
     * @param contactAccounts parent accounts list view.
     */
    private void loadAssignAccountToContactWidget(RootPanel container, final AccountsSublistView contactAccounts) {

        // create lookup window
        final LookupAccountsWindow window = new LookupAccountsWindow(true, true);
        window.create();

        // register listener to be notified when user selects an account
        window.register(new FormNotificationInterface<String>() {

            /** {@inheritDoc} */
            public void notifySuccess(String obj) {
                if (obj == null) {
                    return;
                }

                final String actionUrl = "/crmsfa/control/assignAccountToContactAJX";

                // keep user's selection
                String accountPartyId = obj;

                // nothing special, just send HTTP POST request
                // and run crmsfa.assignContactToAccount service
                RequestBuilder request = new RequestBuilder(RequestBuilder.POST, actionUrl);
                request.setHeader("Content-type", "application/x-www-form-urlencoded");
                request.setRequestData(Format.format("partyId={0}&contactPartyId={0}&accountPartyId={1}", getPartyId(), accountPartyId));
                request.setCallback(new RequestCallback() {
                    public void onError(Request request, Throwable exception) {
                        // display error message
                        UtilUi.errorMessage(exception.toString());
                    }
                    public void onResponseReceived(Request request, Response response) {
                        // we don't expect anything from server, just reload the list of contacts
                        UtilUi.logInfo("onResponseReceived, response = " + response, "", "loadAssignContactToAccountWidget");
                        if (!ServiceErrorReader.showErrorMessageIfAny(response, actionUrl)) {
                            contactAccounts.getStore().reload();
                            contactAccounts.loadFirstPage();
                        }
                        contactAccounts.markGridNotBusy();
                    }
                });

                try {
                    contactAccounts.markGridBusy();
                    UtilUi.logInfo("Run service crmsfa.assignContactToAccount", MODULE, "loadAssignContactToAccountWidget");
                    request.send();
                } catch (RequestException re) {
                    // display error message
                    UtilUi.errorMessage(re.toString(), MODULE, "loadAssignContactToAccountWidget");
                }
            }

        });

        // create hyperlink as submenu button
        Button embedLink = new Button(UtilUi.MSG.crmAssignAccount(), new ClickHandler() {

            public void onClick(ClickEvent event) {
                window.show();
            }

        });
        embedLink.setStyleName("subMenuButton");

        // place [Assign Contact] button on page
        container.add(embedLink);
    }

    /**
     * Load list of open orders as view contact page subsection.
     */
    private void loadContactOrders() {
        // setup order list view subsection
        SalesOrdersSublistView orders = new SalesOrdersSublistView(getPartyId());
        // limit orders to particular account
        orders.filterForParty();
        orders.applyFilters();

        // add widget to page
        RootPanel.get(CONTACT_ORDERS).add(orders);
    }

    /**
     * Load list of cases as view contact page subsection.
     */
    private void loadContactCases() {
        // setup case list view as subsection
        CaseSublistView cases = new CaseSublistView(getPartyId());
        // limit cases to the contact
        cases.filterForContact();
        cases.applyFilters();

        // add widget to page
        RootPanel.get(CONTACT_CASES).add(cases);
    }

    private void loadQuickNewOpportunity() {
        quickNewOpportunityForm = new QuickNewOpportunityForm();
        RootPanel.get(QUICK_CREATE_OPPORTUNITY_ID).add(quickNewOpportunityForm);
    }

    private void loadFindOpportunities() {
        findOpportunitiesForm = new FindOpportunitiesForm(true);
        RootPanel.get(FIND_OPPORTUNITIES_ID).add(findOpportunitiesForm.getMainPanel());
    }


    private void loadMyOpportunities() {
        myOpportunitiesForm = new FindOpportunitiesForm(false);
        myOpportunitiesForm.hideFilters();
        myOpportunitiesForm.getListView().filterMyOrTeamParties(getViewPref());
        myOpportunitiesForm.getListView().applyFilters();
        RootPanel.get(MY_OPPORTUNITIES_ID).add(myOpportunitiesForm.getMainPanel());
    }

    /**
     * Load list of opportunity (actually from related account) contacts.<br>
     * Designed to use on view opportunity page.
     */
    private void loadOpportunityContacts() {
        // setup contacts list view as subsection
        ContactsSublistView opportunityContacts = new ContactsSublistView(getSalesOpportunityId(), true, false);
        // limit displayed contacts to the account and current opportunity
        opportunityContacts.filterByOpportunity(getSalesOpportunityId());
        opportunityContacts.applyFilters();

        // add widget to page
        RootPanel.get(OPPORTUNITY_CONTACTS).add(opportunityContacts);

        // if required ID is found setup button [Assign Contact] in subsection title
        RootPanel assignContactButton = RootPanel.get(ASSIGN_CONTACT_TO_OPPORTUNITY);
        if (assignContactButton != null) {
            loadAssignContactToOpportunityWidget(assignContactButton,  opportunityContacts);
        }
    }

    /**
     * This method setup a button into subsection title at right-hand position
     * and allow to assign a new contact using lookup contacts window.
     * @param container <code>RootPanel</code> that is root container for button
     * @param opportunityContacts parent contacts list view.
     */
    private void loadAssignContactToOpportunityWidget(RootPanel container, final ContactsSublistView opportunityContacts) {

        // create lookup window
        final LookupContactsWindow window = new LookupContactsWindow(true, true);
        window.create();

        // register listener to be notified when user selects a contact
        window.register(new FormNotificationInterface<String>() {

            /** {@inheritDoc} */
            public void notifySuccess(String obj) {
                if (obj == null) {
                    return;
                }

                final String actionUrl = "/crmsfa/control/addContactToOpportunityAJX";

                // keep user's selection
                String contactPartyId = obj;

                // nothing special, just send HTTP POST request
                // and run crmsfa.assignContactToAccount service
                RequestBuilder request = new RequestBuilder(RequestBuilder.POST, actionUrl);
                request.setHeader("Content-type", "application/x-www-form-urlencoded");
                request.setRequestData(Format.format("salesOpportunityId={0}&contactPartyId={1}", getSalesOpportunityId(), contactPartyId));
                request.setCallback(new RequestCallback() {
                    public void onError(Request request, Throwable exception) {
                        // display error message
                        UtilUi.errorMessage(exception.toString());
                    }
                    public void onResponseReceived(Request request, Response response) {
                        // we don't expect anything from server, just reload the list of contacts
                        UtilUi.logInfo("onResponseReceived, response = " + response, "", "loadAssignContactToAccountWidget");
                        if (!ServiceErrorReader.showErrorMessageIfAny(response, actionUrl)) {
                            opportunityContacts.getStore().reload();
                            opportunityContacts.loadFirstPage();
                        }
                        opportunityContacts.markGridNotBusy();
                    }
                });

                try {
                    opportunityContacts.markGridBusy();
                    UtilUi.logInfo("Run service crmsfa.assignContactToAccount", MODULE, "loadAssignContactToAccountWidget");
                    request.send();
                } catch (RequestException re) {
                    // display error message
                    UtilUi.errorMessage(re.toString(), MODULE, "loadAssignContactToAccountWidget");
                }
            }

        });

        // create hyperlink as submenu button
        Button embedLink = new Button(UtilUi.MSG.crmAssignContact(), new ClickHandler() {

            public void onClick(ClickEvent event) {
                window.show();
            }

        });
        embedLink.setStyleName("subMenuButton");

        // place [Assign Contact] button on page
        container.add(embedLink);
    }

    /**
     * Load list of account contacts.<br>
     * Designed to use on view account page.
     */
    private void loadAccountContacts() {
        // setup contacts list view as subsection
        ContactsSublistView accountContacts = new ContactsSublistView(getPartyId(), false, false);
        // limit displayed contacts to the account
        accountContacts.filterByAccount(getPartyId());
        accountContacts.applyFilters();

        // add widget to page
        RootPanel.get(ACCOUNT_CONTACTS).add(accountContacts);

        // if required ID is found setup button [Assign Contact] in subsection title
        RootPanel assignContactButton = null;
        if ((assignContactButton = RootPanel.get(ASSIGN_CONTACT_WIDGET)) != null) {
            loadAssignContactToAccountWidget(assignContactButton,  accountContacts);
        }
    }

    /**
     * This method setup a button into subsection title at right-hand position
     * and allow to assign a new contact using lookup contacts window.
     * @param container <code>RootPanel</code> that is root container for button
     * @param accountContacts parent contacts list view.
     */
    private void loadAssignContactToAccountWidget(RootPanel container, final ContactsSublistView accountContacts) {

        // create lookup window
        final LookupContactsWindow window = new LookupContactsWindow(true, true);
        window.create();

        // register listener to be notified when user selects a contact
        window.register(new FormNotificationInterface<String>() {

            /** {@inheritDoc} */
            public void notifySuccess(String obj) {
                if (obj == null) {
                    return;
                }

                final String actionUrl = "/crmsfa/control/assignContactToAccountAJX";

                // keep user's selection
                String contactPartyId = obj;

                // nothing special, just send HTTP POST request
                // and run crmsfa.assignContactToAccount service
                RequestBuilder request = new RequestBuilder(RequestBuilder.POST, actionUrl);
                request.setHeader("Content-type", "application/x-www-form-urlencoded");
                request.setRequestData(Format.format("partyId={0}&accountPartyId={0}&contactPartyId={1}", getPartyId(), contactPartyId));
                request.setCallback(new RequestCallback() {
                    public void onError(Request request, Throwable exception) {
                        // display error message
                        UtilUi.errorMessage(exception.toString());
                    }
                    public void onResponseReceived(Request request, Response response) {
                        // we don't expect anything from server, just reload the list of contacts
                        UtilUi.logInfo("onResponseReceived, response = " + response, "", "loadAssignContactToAccountWidget");
                        if (!ServiceErrorReader.showErrorMessageIfAny(response, actionUrl)) {
                            accountContacts.getStore().reload();
                            accountContacts.loadFirstPage();
                        }
                        accountContacts.markGridNotBusy();
                    }
                });

                try {
                    accountContacts.markGridBusy();
                    UtilUi.logInfo("Run service crmsfa.assignContactToAccount", MODULE, "loadAssignContactToAccountWidget");
                    request.send();
                } catch (RequestException re) {
                    // display error message
                    UtilUi.errorMessage(re.toString(), MODULE, "loadAssignContactToAccountWidget");
                }
            }

        });

        // create hyperlink as submenu button
        Button embedLink = new Button(UtilUi.MSG.crmAssignContact(), new ClickHandler() {

            public void onClick(ClickEvent event) {
                window.show();
            }

        });
        embedLink.setStyleName("subMenuButton");

        // place [Assign Contact] button on page
        container.add(embedLink);
    }

    /**
     * Load list of account opportunities.<br>
     * Designed to use on view account page.
     */
    private void loadAccountOpportunities() {
        // setup opportunity list view as subsection
        OpportunitiesSublistView accountOpportunities = new OpportunitiesSublistView();
        // limit displayed opportunities for this account
        accountOpportunities.filterByAccount(getPartyId());
        accountOpportunities.applyFilters();

        // add widget to page
        RootPanel.get(ACCOUNT_OPPORTUNITIES).add(accountOpportunities);
    }

    /**
     * Load list of open orders as view account page subsection.
     */
    private void loadAccountOrders() {
        // setup order list view subsection
        SalesOrdersSublistView orders = new SalesOrdersSublistView(getPartyId());
        // limit orders to particular account
        orders.filterForParty();
        orders.applyFilters();

        // add widget to page
        RootPanel.get(ACCOUNT_ORDERS).add(orders);
    }

    /**
     * Load list of cases as view account page subsection.
     */
    private void loadAccountCases() {
        // setup case list view as subsection
        CaseSublistView cases = new CaseSublistView(getPartyId());
        // limit cases to the account
        cases.filterForAccount();
        cases.applyFilters();

        // add widget to page
        RootPanel.get(ACCOUNT_CASES).add(cases);
    }

    private void loadFindAccounts() {
        findAccountsForm = new FindAccountsForm();
        findAccountsForm.getListView().setPageSize(PARTIES_PAGE_SIZE);
        findAccountsForm.getListView().applyFilters();
        RootPanel.get(FIND_ACCOUNTS_ID).add(findAccountsForm.getMainPanel());
    }

    private void loadLookupAccounts() {
        findAccountsForm = new FindAccountsForm();
        findAccountsForm.getListView().setLookupMode();
        findAccountsForm.getListView().filterOutDisabledParties(true);
        findAccountsForm.getListView().applyFilters();
        RootPanel.get(LOOKUP_ACCOUNTS_ID).add(findAccountsForm.getMainPanel());
    }

    private void loadQuickNewAccount() {
        quickNewAccountForm = new QuickNewAccountForm();
        RootPanel.get(QUICK_CREATE_ACCOUNT_ID).add(quickNewAccountForm);
    }

    private void loadMyAccounts() {
        myAccountsForm = new FindAccountsForm();
        myAccountsForm.hideFilters();
        myAccountsForm.getListView().setPageSize(PARTIES_PAGE_SIZE);
        myAccountsForm.getListView().filterMyOrTeamParties(getViewPref());
        myAccountsForm.getListView().applyFilters();
        RootPanel.get(MY_ACCOUNTS_ID).add(myAccountsForm.getMainPanel());
    }

    private void loadMyOrders() {
        myOrdersForm = new FindOrdersForm(false);
        myOrdersForm.hideFilters();
        String pageSize = getWidgetParameter(MY_ORDERS_ID, "pageSize");
        if (pageSize != null) {
            Integer size = Integer.parseInt(pageSize);
            myOrdersForm.getListView().setPageSize(size);
            myOrdersForm.getListView().setDefaultPageSize(size);
        }
        myOrdersForm.getListView().filterMyOrTeamParties(getViewPref());
        myOrdersForm.getListView().applyFilters();
        RootPanel.get(MY_ORDERS_ID).add(myOrdersForm.getMainPanel());
    }

    private void loadFindOrders() {
        findOrdersForm = new FindOrdersForm(false);
        RootPanel.get(FIND_ORDERS_ID).add(findOrdersForm.getMainPanel());
    }


    private void loadOrderItems() {
        OrderItemsEditable orderItemsEditable = new OrderItemsEditable(OrderType.SALES);
        RootPanel.get(ORDER_ITEMS_ID).add(orderItemsEditable.getMainPanel());
    }

    /**
     * Add link beside inventory item and open form for re-reservation on click.
     * @param container the container <code>RootPanel</code>
     * @param orderItemIndex an <code>Integer</code> value
     * @param oisgrIndex an <code>Integer</code> value
     */
    private void loadReReserveDialog(RootPanel container, Integer orderItemIndex, Integer oisgrIndex) {
        Panel panel = new Panel();
        panel.setBorder(false);

        Dictionary facilities = Dictionary.getDictionary("facilityList");
        Dictionary widgetParameters = Dictionary.getDictionary("reReservationWidgetParameters");

        final ProductReReservationForm window = new ProductReReservationForm(
                UtilUi.MSG.opentapsReReserveProduct(),
                facilities,
                getOrderId(),
                widgetParameters.get("orderItemSeqId_" + orderItemIndex.toString()),
                widgetParameters.get("inventoryItemId_" + orderItemIndex.toString() + "_" + oisgrIndex.toString()),
                widgetParameters.get("shipGroupSeqId_" + orderItemIndex.toString() + "_" + oisgrIndex.toString()),
                widgetParameters.get("quantity_" + orderItemIndex.toString() + "_" + oisgrIndex.toString())
        );
        window.create();

        window.register(new FormNotificationInterface<Void>() {
            public void notifySuccess(Void v) {
                Window.Location.replace(Window.Location.getHref());
            }
        });

        Hyperlink embedLink = new Hyperlink(UtilUi.MSG.opentapsReReserve(), null);
        embedLink.setStyleName("buttontext");
        embedLink.addClickHandler(new ClickHandler() {

            public void onClick(ClickEvent event) {
                window.show();
            }

        });

        panel.add(embedLink);
        container.add(panel);
    }

    RootPanel getNextRootPanel(int lastOrderItemIndex, int lastInventoryItemIndex) {
        return null;
    }

    private void loadFindPartners() {
        findPartnersForm = new FindPartnersForm();
        findPartnersForm.getListView().setPageSize(PARTIES_PAGE_SIZE);
        findPartnersForm.getListView().applyFilters();
        RootPanel.get(FIND_PARTNERS_ID).add(findPartnersForm.getMainPanel());
    }

    /**
     * Retrieve JS variable <code>partyId</code>.
     * @return the <code>partyId</code>
     */
    private static native String getPartyId()/*-{
        return $wnd.partyId;
    }-*/;

    /**
     * Retrieve JS variable <code>salesopportunityId</code>.
     * @return an opportunity identifier
     */
    private static native String getSalesOpportunityId()/*-{
        return $wnd.salesOpportunityId;
    }-*/;

    /**
     * Retrieve JS variable <code>orderId</code>.
     * @return the <code>orderId</code>
     */
    private static native String getOrderId() /*-{
        return $wnd.orderId;
    }-*/;

    /**
     * Retrieve GWT parameter viewPref. Parameter is optional.
     * @return
     *     Possible values are <code>MY_VALUES</code> (or <code>OrderLookupConfiguration.MY_VALUES</code>) and
     *     <code>TEAM_VALUES</code> (or <code>OrderLookupConfiguration.TEAM_VALUES</code>)
     */
    private static native String getViewPref()/*-{
        return $wnd.viewPref;
    }-*/;

}
