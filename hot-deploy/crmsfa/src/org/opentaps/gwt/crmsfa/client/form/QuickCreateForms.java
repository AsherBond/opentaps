/*
 * Copyright (c) 2009 Open Source Strategies, Inc.
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
package org.opentaps.gwt.crmsfa.client.form;

import com.gwtext.client.widgets.Panel;
import com.gwtext.client.widgets.layout.AccordionLayout;
import org.opentaps.gwt.common.client.UtilUi;
import org.opentaps.gwt.common.client.form.FormNotificationInterface;
import org.opentaps.gwt.common.client.security.Permission;
import org.opentaps.gwt.crmsfa.client.accounts.form.QuickNewAccountForm;
import org.opentaps.gwt.crmsfa.client.cases.form.QuickNewCaseForm;
import org.opentaps.gwt.crmsfa.client.contacts.form.QuickNewContactForm;
import org.opentaps.gwt.crmsfa.client.leads.form.QuickNewLeadForm;
import org.opentaps.gwt.crmsfa.client.opportunities.form.QuickNewOpportunityForm;


public class QuickCreateForms extends Panel {

    private QuickNewAccountForm quickNewAccountForm;
    private QuickNewContactForm quickNewContactForm;
    private QuickNewLeadForm quickNewLeadForm;
    private QuickNewOpportunityForm quickNewOpportunityForm;
    private QuickNewCaseForm quickNewCaseForm;

    private static final Integer INPUT_LENGTH = 120;

    public QuickCreateForms() {
        super();
        setTitle(UtilUi.MSG.opentapsQuickCreate());
        setBorder(false);
        setPaddings(0);
        setBaseCls(UtilUi.SCREENLET_STYLE);
        setTabCls(UtilUi.SCREENLET_HEADER_STYLE);
        setBodyStyle(UtilUi.SCREENLET_BODY_STYLE);
        AccordionLayout layout = new AccordionLayout(true);
        layout.setFill(false); // let the panel expand automatically according to the content
        setLayout(layout);

        if (Permission.hasPermission(Permission.CRMSFA_ACCOUNT_CREATE)) {
            quickNewAccountForm = new QuickNewAccountForm(INPUT_LENGTH, UtilUi.MSG.crmAccount());
            quickNewAccountForm.collapse();
            add(quickNewAccountForm);
        }

        if (Permission.hasPermission(Permission.CRMSFA_CONTACT_CREATE)) {
            quickNewContactForm = new QuickNewContactForm(INPUT_LENGTH, UtilUi.MSG.crmContact());
            quickNewContactForm.collapse();
            add(quickNewContactForm);
        }

        if (Permission.hasPermission(Permission.CRMSFA_LEAD_CREATE)) {
            quickNewLeadForm = new QuickNewLeadForm(INPUT_LENGTH, UtilUi.MSG.crmLead());
            quickNewLeadForm.collapse();
            add(quickNewLeadForm);
        }

        if (Permission.hasPermission(Permission.CRMSFA_OPP_CREATE)) {
            quickNewOpportunityForm = new QuickNewOpportunityForm(INPUT_LENGTH, UtilUi.MSG.crmOpportunity());
            quickNewOpportunityForm.collapse();
            add(quickNewOpportunityForm);
        }

        if (Permission.hasPermission(Permission.CRMSFA_CASE_CREATE)) {
            quickNewCaseForm = new QuickNewCaseForm(INPUT_LENGTH, UtilUi.MSG.crmCase());
            quickNewCaseForm.collapse();
            add(quickNewCaseForm);
        }
    }

    public void registerAccountList(FormNotificationInterface list) {
        quickNewAccountForm.register(list);
    }

    public void registerContactList(FormNotificationInterface list) {
        quickNewContactForm.register(list);
    }

    public void registerLeadList(FormNotificationInterface list) {
        quickNewLeadForm.register(list);
    }

    public void registerOpportunityList(FormNotificationInterface list) {
        quickNewOpportunityForm.register(list);
    }

    public void registerCaseList(FormNotificationInterface list) {
        quickNewCaseForm.register(list);
    }
}
