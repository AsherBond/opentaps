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

package com.opensourcestrategies.financials.payroll;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.ofbiz.accounting.AccountingException;
import org.ofbiz.accounting.util.UtilAccounting;
import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilNumber;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.entity.Delegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;

import com.opensourcestrategies.financials.util.UtilFinancial;

public class PaycheckReader {

    public static final String module = PaycheckReader.class.getName();
    public static final String resource = "FinancialsUiLabels";

    public static int decimals = UtilNumber.getBigDecimalScale("fin_arithmetic.properties", "payroll.paycheck.decimals");
    public static int rounding = UtilNumber.getBigDecimalRoundingMode("fin_arithmetic.properties", "payroll.paycheck.rounding");
    public static final BigDecimal ZERO = BigDecimal.ZERO.setScale(decimals, rounding);

    public static String SALARY_EXPENSE_GL_ACCOUNT_TYPE = "SALARY_EXPENSES";

    protected GenericValue paycheck = null;
    protected Delegator delegator = null;
    protected Map paycheckItemsByClass = new HashMap();
    protected Map glAccountsByItemType = new HashMap();
    protected String employeePartyId = null;
    protected String organizationPartyId = null;
    protected String currencyUomId = null;
    protected BigDecimal grossAmount = ZERO;
    protected String salaryExpenseGlAccountId = null;
    protected GenericValue billingAddress = null;


    /**
     * This is the main constructor and will do all the entity engine database access up here so the rest of this object just analyzes the available data
     * @param paycheck
     */
    public PaycheckReader(GenericValue paycheck) {
        // TODO verify it is a paycheck
        this.paycheck = paycheck;
        this.organizationPartyId = paycheck.getString("partyIdFrom");
        this.employeePartyId = paycheck.getString("partyIdTo");
        this.currencyUomId = paycheck.getString("currencyUomId");
        this.grossAmount = paycheck.getBigDecimal("amount");
        this.delegator = paycheck.getDelegator();
        
        // load up the item by classes
        try {
            List paycheckItemClasses = delegator.findAllCache("PaycheckItemClass");
            for (Iterator cit = paycheckItemClasses.iterator(); cit.hasNext(); ) {
                GenericValue paycheckItemClass = (GenericValue) cit.next();
                List paycheckItems = delegator.findByAnd("PaycheckItemAndType", UtilMisc.toMap("paycheckItemClassId", paycheckItemClass.getString("paycheckItemClassId"),
                		"paymentId", paycheck.getString("paymentId")),
                        UtilMisc.toList("paycheckItemSeqId"));

                // create the Map of gl account configuration by paycheckItemType
                for (Iterator pit = paycheckItems.iterator(); pit.hasNext(); ) {
                    GenericValue paycheckItem = (GenericValue) pit.next();
                    GenericValue glAccountByItemType = delegator.findByPrimaryKeyCache("PaycheckItemTypeGlAccount",
                            UtilMisc.toMap("organizationPartyId", this.organizationPartyId, "paycheckItemTypeId", paycheckItem.get("paycheckItemTypeId")));
                    this.glAccountsByItemType.put(paycheckItem.get("paycheckItemTypeId"), glAccountByItemType);
                }
                this.paycheckItemsByClass.put(paycheckItemClass.getString("paycheckItemClassId"), paycheckItems);
            }

            // set the salary expense gl account
            this.salaryExpenseGlAccountId = UtilAccounting.getDefaultAccountId(SALARY_EXPENSE_GL_ACCOUNT_TYPE, this.organizationPartyId, this.delegator);

            this.billingAddress = UtilFinancial.getBillingAddress(this.employeePartyId, delegator);

        } catch (GenericEntityException ex) {
            Debug.logError("Entity exception when trying to retrieve paycheck items for paycheck id [" + paycheck.getString("paymentId") + "]: " + ex.getMessage(), module);
        } catch (AccountingException e) {
            Debug.logError(e.getMessage(), module);  //To change body of catch statement use File | Settings | File Templates.
        }
    }

    public PaycheckReader(String paymentId, Delegator delegator) throws GenericEntityException {
        this(delegator.findByPrimaryKey("Payment", UtilMisc.toMap("paymentId", paymentId)));
    }

    public BigDecimal getPaycheckItemSumByClass(String paycheckItemClassId) {
        BigDecimal sum = ZERO;
        List paycheckItems = (List) paycheckItemsByClass.get(paycheckItemClassId);
        for (Iterator piit = paycheckItems.iterator(); piit.hasNext(); ) {
            GenericValue paycheckItem = (GenericValue) piit.next();
            BigDecimal paycheckItemAmount = ((paycheckItem.getBigDecimal("amount") == null)? ZERO : paycheckItem.getBigDecimal("amount").setScale(decimals+1, rounding));
            sum = sum.add(paycheckItemAmount).setScale(decimals+1, rounding);                
        }
        return sum;
    }
    
    public List getPaycheckItemsByClass(String paycheckItemClassId) {
        return (List) paycheckItemsByClass.get(paycheckItemClassId);
    }

    public String getSalaryExpenseGlAccountId() {
        return this.salaryExpenseGlAccountId;
    }

    public String getOrganizationPartyId() {
        return this.organizationPartyId;
    }

    public String getEmployeePartyId() {
        return this.employeePartyId;
    }

    public String getCurrencyUomId() {
        return this.currencyUomId;
    }

    public BigDecimal getGrossAmount() {
        return this.grossAmount;
    }

    /**
     * Gets the GL account configuration for this paycheckItem
     * @param paycheckItem
     * @return
     * @throws GenericEntityException
     */
    public GenericValue getGlAccountConfig(GenericValue paycheckItem) throws GenericEntityException {
        return (GenericValue) glAccountsByItemType.get(paycheckItem.getString("paycheckItemTypeId"));
    }

    /**
     * Gets the partyId to post a paycheck item from the paycheckItem itself
     * @param paycheckItem
     * @return
     * @throws GenericEntityException
     */
    public String getPostToPartyId(GenericValue paycheckItem) throws GenericEntityException {
        return paycheckItem.getString("partyId");
    }

    /**
     * Uses getGlAccountConfig above to get the debit GL account
     * @param paycheckItem
     * @return
     * @throws GenericEntityException
     */
    public String getDebitGlAccountId(GenericValue paycheckItem) throws GenericEntityException {
        GenericValue glAccountConfig = getGlAccountConfig(paycheckItem);
        if (UtilValidate.isNotEmpty(glAccountConfig)) {
            return glAccountConfig.getString("debitGlAccountId");
        }
        return null;
    }

    /**
     * Uses getGlAccountConfig above to get the credit GL account
     * @param paycheckItem
     * @return
     * @throws GenericEntityException
     */
    public String getCreditGlAccountId(GenericValue paycheckItem) throws GenericEntityException {
        GenericValue glAccountConfig = getGlAccountConfig(paycheckItem);
        if (UtilValidate.isNotEmpty(glAccountConfig)) {
            return glAccountConfig.getString("creditGlAccountId");
        }
        return null;
    }

    /**
     * Returns amount of paycheck net of all deductions
     * @return
     * @throws GenericEntityException
     */
    public BigDecimal getNetAmount() throws GenericEntityException {
        BigDecimal netAmount = this.paycheck.getBigDecimal("amount");
        netAmount = netAmount.subtract(getPaycheckItemSumByClass("DEDUCTION")).setScale(decimals, rounding);
        return netAmount;
    }

    /**
     * Returns the billing address of the paycheck recepient.
     */
    public GenericValue getBillingAddress() {
        return billingAddress;
    }
    
    /**
     * Returns true if paycheck item amount change doesn't cause negative paycheck net pay
     * @return
     * @param paycheckItemSeqId
     * @param amount 
     */
    public boolean isPaycheckItemAmountChangeValid(String paycheckItemSeqId, Double amount) {
    	if (UtilValidate.isNotEmpty(paycheckItemSeqId)) { 
	        BigDecimal witholdings = ZERO;
	        List paycheckItems = (List) paycheckItemsByClass.get("DEDUCTION");
	        for (Iterator piit = paycheckItems.iterator(); piit.hasNext(); ) {
	            GenericValue paycheckItem = (GenericValue) piit.next();
	        	if (paycheckItem.getString("paycheckItemSeqId").equals(paycheckItemSeqId)) {
	                BigDecimal paycheckItemAmount = (amount == null)? ZERO : BigDecimal.valueOf(amount).setScale(decimals+1, rounding);
	                witholdings = witholdings.add(paycheckItemAmount).setScale(decimals+1, rounding);        			
	        	} else {
	                BigDecimal paycheckItemAmount = ((paycheckItem.getBigDecimal("amount") == null)? ZERO : paycheckItem.getBigDecimal("amount").setScale(decimals+1, rounding));
	                witholdings = witholdings.add(paycheckItemAmount).setScale(decimals+1, rounding);                
	        	}                
	        }
	        BigDecimal grossAmount = this.paycheck.getBigDecimal("amount");
	        BigDecimal netAmount = grossAmount.subtract(witholdings);	
	   		if (netAmount.compareTo(ZERO) >= 0) {
                return true;
    		}  	    
    	}
        return false;    	
    }
    
}
