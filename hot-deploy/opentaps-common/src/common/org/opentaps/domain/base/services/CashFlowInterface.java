package org.opentaps.domain.base.services;

/*
 * Copyright (c) 2009 - 2009 Open Source Strategies, Inc.
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

// DO NOT EDIT THIS FILE!  THIS IS AUTO GENERATED AND WILL GET WRITTEN OVER PERIODICALLY WHEN THE SERVICE MODEL CHANGES
// EXTEND THIS CLASS INSTEAD.

import java.util.Map;
import java.util.Set;
import javolution.util.FastSet;
import javolution.util.FastMap;

import org.opentaps.foundation.service.ServiceWrapper;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import org.ofbiz.entity.GenericValue;

/**
 * Auto generated base service entity cashFlowInterface.
 */
public class CashFlowInterface extends ServiceWrapper {

    /** The service name as used by the service engine. */
    public static final String NAME = "cashFlowInterface";

    /** The enumeration of input parameters. */
    public static enum In {
        glFiscalTypeId("glFiscalTypeId"),
        locale("locale"),
        organizationPartyId("organizationPartyId"),
        timeZone("timeZone"),
        userLogin("userLogin");
        private final String _fieldName;
        private In(String name) { this._fieldName = name; }
        public String getName() { return this._fieldName; }
    }

    public static enum Out {
        beginningCashAccountBalances("beginningCashAccountBalances"),
        beginningCashAmount("beginningCashAmount"),
        endingCashAccountBalances("endingCashAccountBalances"),
        endingCashAmount("endingCashAmount"),
        errorMessage("errorMessage"),
        errorMessageList("errorMessageList"),
        financingCashFlow("financingCashFlow"),
        financingCashFlowAccountBalances("financingCashFlowAccountBalances"),
        investingCashFlow("investingCashFlow"),
        investingCashFlowAccountBalances("investingCashFlowAccountBalances"),
        locale("locale"),
        netCashFlow("netCashFlow"),
        netIncome("netIncome"),
        operatingCashFlow("operatingCashFlow"),
        operatingCashFlowAccountBalances("operatingCashFlowAccountBalances"),
        responseMessage("responseMessage"),
        successMessage("successMessage"),
        successMessageList("successMessageList"),
        timeZone("timeZone"),
        userLogin("userLogin");
        private final String _fieldName;
        private Out(String name) { this._fieldName = name; }
        public String getName() { return this._fieldName; }
    }

    private String inGlFiscalTypeId;
    private Locale inLocale;
    private String inOrganizationPartyId;
    private TimeZone inTimeZone;
    private GenericValue inUserLogin;
    private Map outBeginningCashAccountBalances;
    private BigDecimal outBeginningCashAmount;
    private Map outEndingCashAccountBalances;
    private BigDecimal outEndingCashAmount;
    private String outErrorMessage;
    private List outErrorMessageList;
    private BigDecimal outFinancingCashFlow;
    private Map outFinancingCashFlowAccountBalances;
    private BigDecimal outInvestingCashFlow;
    private Map outInvestingCashFlowAccountBalances;
    private Locale outLocale;
    private BigDecimal outNetCashFlow;
    private BigDecimal outNetIncome;
    private BigDecimal outOperatingCashFlow;
    private Map outOperatingCashFlowAccountBalances;
    private String outResponseMessage;
    private String outSuccessMessage;
    private List outSuccessMessageList;
    private TimeZone outTimeZone;
    private GenericValue outUserLogin;

    private Set<String> inParameters = FastSet.newInstance();
    private Set<String> outParameters = FastSet.newInstance();

    /**
     * Auto generated value accessor.
     * This parameter is optional.
     * @return <code>String</code>
     */
    public String getInGlFiscalTypeId() {
        return this.inGlFiscalTypeId;
    }
    /**
     * Auto generated value accessor.
     * This parameter is optional.
     * @return <code>Locale</code>
     */
    public Locale getInLocale() {
        return this.inLocale;
    }
    /**
     * Auto generated value accessor.
     * This parameter is required.
     * @return <code>String</code>
     */
    public String getInOrganizationPartyId() {
        return this.inOrganizationPartyId;
    }
    /**
     * Auto generated value accessor.
     * This parameter is optional.
     * @return <code>TimeZone</code>
     */
    public TimeZone getInTimeZone() {
        return this.inTimeZone;
    }
    /**
     * Auto generated value accessor.
     * This parameter is optional.
     * @return <code>GenericValue</code>
     */
    public GenericValue getInUserLogin() {
        return this.inUserLogin;
    }
    /**
     * Auto generated value accessor.
     * This parameter is required.
     * @return <code>Map</code>
     */
    public Map getOutBeginningCashAccountBalances() {
        return this.outBeginningCashAccountBalances;
    }
    /**
     * Auto generated value accessor.
     * This parameter is required.
     * @return <code>BigDecimal</code>
     */
    public BigDecimal getOutBeginningCashAmount() {
        return this.outBeginningCashAmount;
    }
    /**
     * Auto generated value accessor.
     * This parameter is required.
     * @return <code>Map</code>
     */
    public Map getOutEndingCashAccountBalances() {
        return this.outEndingCashAccountBalances;
    }
    /**
     * Auto generated value accessor.
     * This parameter is required.
     * @return <code>BigDecimal</code>
     */
    public BigDecimal getOutEndingCashAmount() {
        return this.outEndingCashAmount;
    }
    /**
     * Auto generated value accessor.
     * This parameter is optional.
     * @return <code>String</code>
     */
    public String getOutErrorMessage() {
        return this.outErrorMessage;
    }
    /**
     * Auto generated value accessor.
     * This parameter is optional.
     * @return <code>List</code>
     */
    public List getOutErrorMessageList() {
        return this.outErrorMessageList;
    }
    /**
     * Auto generated value accessor.
     * This parameter is required.
     * @return <code>BigDecimal</code>
     */
    public BigDecimal getOutFinancingCashFlow() {
        return this.outFinancingCashFlow;
    }
    /**
     * Auto generated value accessor.
     * This parameter is required.
     * @return <code>Map</code>
     */
    public Map getOutFinancingCashFlowAccountBalances() {
        return this.outFinancingCashFlowAccountBalances;
    }
    /**
     * Auto generated value accessor.
     * This parameter is required.
     * @return <code>BigDecimal</code>
     */
    public BigDecimal getOutInvestingCashFlow() {
        return this.outInvestingCashFlow;
    }
    /**
     * Auto generated value accessor.
     * This parameter is required.
     * @return <code>Map</code>
     */
    public Map getOutInvestingCashFlowAccountBalances() {
        return this.outInvestingCashFlowAccountBalances;
    }
    /**
     * Auto generated value accessor.
     * This parameter is optional.
     * @return <code>Locale</code>
     */
    public Locale getOutLocale() {
        return this.outLocale;
    }
    /**
     * Auto generated value accessor.
     * This parameter is required.
     * @return <code>BigDecimal</code>
     */
    public BigDecimal getOutNetCashFlow() {
        return this.outNetCashFlow;
    }
    /**
     * Auto generated value accessor.
     * This parameter is required.
     * @return <code>BigDecimal</code>
     */
    public BigDecimal getOutNetIncome() {
        return this.outNetIncome;
    }
    /**
     * Auto generated value accessor.
     * This parameter is required.
     * @return <code>BigDecimal</code>
     */
    public BigDecimal getOutOperatingCashFlow() {
        return this.outOperatingCashFlow;
    }
    /**
     * Auto generated value accessor.
     * This parameter is required.
     * @return <code>Map</code>
     */
    public Map getOutOperatingCashFlowAccountBalances() {
        return this.outOperatingCashFlowAccountBalances;
    }
    /**
     * Auto generated value accessor.
     * This parameter is optional.
     * @return <code>String</code>
     */
    public String getOutResponseMessage() {
        return this.outResponseMessage;
    }
    /**
     * Auto generated value accessor.
     * This parameter is optional.
     * @return <code>String</code>
     */
    public String getOutSuccessMessage() {
        return this.outSuccessMessage;
    }
    /**
     * Auto generated value accessor.
     * This parameter is optional.
     * @return <code>List</code>
     */
    public List getOutSuccessMessageList() {
        return this.outSuccessMessageList;
    }
    /**
     * Auto generated value accessor.
     * This parameter is optional.
     * @return <code>TimeZone</code>
     */
    public TimeZone getOutTimeZone() {
        return this.outTimeZone;
    }
    /**
     * Auto generated value accessor.
     * This parameter is optional.
     * @return <code>GenericValue</code>
     */
    public GenericValue getOutUserLogin() {
        return this.outUserLogin;
    }

    /**
     * Auto generated value setter.
     * This parameter is optional.
     * @param inGlFiscalTypeId the inGlFiscalTypeId to set
    */
    public void setInGlFiscalTypeId(String inGlFiscalTypeId) {
        this.inParameters.add("glFiscalTypeId");
        this.inGlFiscalTypeId = inGlFiscalTypeId;
    }
    /**
     * Auto generated value setter.
     * This parameter is optional.
     * @param inLocale the inLocale to set
    */
    public void setInLocale(Locale inLocale) {
        this.inParameters.add("locale");
        this.inLocale = inLocale;
    }
    /**
     * Auto generated value setter.
     * This parameter is required.
     * @param inOrganizationPartyId the inOrganizationPartyId to set
    */
    public void setInOrganizationPartyId(String inOrganizationPartyId) {
        this.inParameters.add("organizationPartyId");
        this.inOrganizationPartyId = inOrganizationPartyId;
    }
    /**
     * Auto generated value setter.
     * This parameter is optional.
     * @param inTimeZone the inTimeZone to set
    */
    public void setInTimeZone(TimeZone inTimeZone) {
        this.inParameters.add("timeZone");
        this.inTimeZone = inTimeZone;
    }
    /**
     * Auto generated value setter.
     * This parameter is optional.
     * @param inUserLogin the inUserLogin to set
    */
    public void setInUserLogin(GenericValue inUserLogin) {
        this.inParameters.add("userLogin");
        this.inUserLogin = inUserLogin;
    }
    /**
     * Auto generated value setter.
     * This parameter is required.
     * @param outBeginningCashAccountBalances the outBeginningCashAccountBalances to set
    */
    public void setOutBeginningCashAccountBalances(Map outBeginningCashAccountBalances) {
        this.outParameters.add("beginningCashAccountBalances");
        this.outBeginningCashAccountBalances = outBeginningCashAccountBalances;
    }
    /**
     * Auto generated value setter.
     * This parameter is required.
     * @param outBeginningCashAmount the outBeginningCashAmount to set
    */
    public void setOutBeginningCashAmount(BigDecimal outBeginningCashAmount) {
        this.outParameters.add("beginningCashAmount");
        this.outBeginningCashAmount = outBeginningCashAmount;
    }
    /**
     * Auto generated value setter.
     * This parameter is required.
     * @param outEndingCashAccountBalances the outEndingCashAccountBalances to set
    */
    public void setOutEndingCashAccountBalances(Map outEndingCashAccountBalances) {
        this.outParameters.add("endingCashAccountBalances");
        this.outEndingCashAccountBalances = outEndingCashAccountBalances;
    }
    /**
     * Auto generated value setter.
     * This parameter is required.
     * @param outEndingCashAmount the outEndingCashAmount to set
    */
    public void setOutEndingCashAmount(BigDecimal outEndingCashAmount) {
        this.outParameters.add("endingCashAmount");
        this.outEndingCashAmount = outEndingCashAmount;
    }
    /**
     * Auto generated value setter.
     * This parameter is optional.
     * @param outErrorMessage the outErrorMessage to set
    */
    public void setOutErrorMessage(String outErrorMessage) {
        this.outParameters.add("errorMessage");
        this.outErrorMessage = outErrorMessage;
    }
    /**
     * Auto generated value setter.
     * This parameter is optional.
     * @param outErrorMessageList the outErrorMessageList to set
    */
    public void setOutErrorMessageList(List outErrorMessageList) {
        this.outParameters.add("errorMessageList");
        this.outErrorMessageList = outErrorMessageList;
    }
    /**
     * Auto generated value setter.
     * This parameter is required.
     * @param outFinancingCashFlow the outFinancingCashFlow to set
    */
    public void setOutFinancingCashFlow(BigDecimal outFinancingCashFlow) {
        this.outParameters.add("financingCashFlow");
        this.outFinancingCashFlow = outFinancingCashFlow;
    }
    /**
     * Auto generated value setter.
     * This parameter is required.
     * @param outFinancingCashFlowAccountBalances the outFinancingCashFlowAccountBalances to set
    */
    public void setOutFinancingCashFlowAccountBalances(Map outFinancingCashFlowAccountBalances) {
        this.outParameters.add("financingCashFlowAccountBalances");
        this.outFinancingCashFlowAccountBalances = outFinancingCashFlowAccountBalances;
    }
    /**
     * Auto generated value setter.
     * This parameter is required.
     * @param outInvestingCashFlow the outInvestingCashFlow to set
    */
    public void setOutInvestingCashFlow(BigDecimal outInvestingCashFlow) {
        this.outParameters.add("investingCashFlow");
        this.outInvestingCashFlow = outInvestingCashFlow;
    }
    /**
     * Auto generated value setter.
     * This parameter is required.
     * @param outInvestingCashFlowAccountBalances the outInvestingCashFlowAccountBalances to set
    */
    public void setOutInvestingCashFlowAccountBalances(Map outInvestingCashFlowAccountBalances) {
        this.outParameters.add("investingCashFlowAccountBalances");
        this.outInvestingCashFlowAccountBalances = outInvestingCashFlowAccountBalances;
    }
    /**
     * Auto generated value setter.
     * This parameter is optional.
     * @param outLocale the outLocale to set
    */
    public void setOutLocale(Locale outLocale) {
        this.outParameters.add("locale");
        this.outLocale = outLocale;
    }
    /**
     * Auto generated value setter.
     * This parameter is required.
     * @param outNetCashFlow the outNetCashFlow to set
    */
    public void setOutNetCashFlow(BigDecimal outNetCashFlow) {
        this.outParameters.add("netCashFlow");
        this.outNetCashFlow = outNetCashFlow;
    }
    /**
     * Auto generated value setter.
     * This parameter is required.
     * @param outNetIncome the outNetIncome to set
    */
    public void setOutNetIncome(BigDecimal outNetIncome) {
        this.outParameters.add("netIncome");
        this.outNetIncome = outNetIncome;
    }
    /**
     * Auto generated value setter.
     * This parameter is required.
     * @param outOperatingCashFlow the outOperatingCashFlow to set
    */
    public void setOutOperatingCashFlow(BigDecimal outOperatingCashFlow) {
        this.outParameters.add("operatingCashFlow");
        this.outOperatingCashFlow = outOperatingCashFlow;
    }
    /**
     * Auto generated value setter.
     * This parameter is required.
     * @param outOperatingCashFlowAccountBalances the outOperatingCashFlowAccountBalances to set
    */
    public void setOutOperatingCashFlowAccountBalances(Map outOperatingCashFlowAccountBalances) {
        this.outParameters.add("operatingCashFlowAccountBalances");
        this.outOperatingCashFlowAccountBalances = outOperatingCashFlowAccountBalances;
    }
    /**
     * Auto generated value setter.
     * This parameter is optional.
     * @param outResponseMessage the outResponseMessage to set
    */
    public void setOutResponseMessage(String outResponseMessage) {
        this.outParameters.add("responseMessage");
        this.outResponseMessage = outResponseMessage;
    }
    /**
     * Auto generated value setter.
     * This parameter is optional.
     * @param outSuccessMessage the outSuccessMessage to set
    */
    public void setOutSuccessMessage(String outSuccessMessage) {
        this.outParameters.add("successMessage");
        this.outSuccessMessage = outSuccessMessage;
    }
    /**
     * Auto generated value setter.
     * This parameter is optional.
     * @param outSuccessMessageList the outSuccessMessageList to set
    */
    public void setOutSuccessMessageList(List outSuccessMessageList) {
        this.outParameters.add("successMessageList");
        this.outSuccessMessageList = outSuccessMessageList;
    }
    /**
     * Auto generated value setter.
     * This parameter is optional.
     * @param outTimeZone the outTimeZone to set
    */
    public void setOutTimeZone(TimeZone outTimeZone) {
        this.outParameters.add("timeZone");
        this.outTimeZone = outTimeZone;
    }
    /**
     * Auto generated value setter.
     * This parameter is optional.
     * @param outUserLogin the outUserLogin to set
    */
    public void setOutUserLogin(GenericValue outUserLogin) {
        this.outParameters.add("userLogin");
        this.outUserLogin = outUserLogin;
    }

    /** {@inheritDoc} */
    public String name() {
        return NAME;
    }

    /** {@inheritDoc} */
    public Map<String, Object> inputMap() {
        Map<String, Object> mapValue = new FastMap<String, Object>();
        if (inParameters.contains("glFiscalTypeId")) mapValue.put("glFiscalTypeId", getInGlFiscalTypeId());
        if (inParameters.contains("locale")) mapValue.put("locale", getInLocale());
        if (inParameters.contains("organizationPartyId")) mapValue.put("organizationPartyId", getInOrganizationPartyId());
        if (inParameters.contains("timeZone")) mapValue.put("timeZone", getInTimeZone());
        if (inParameters.contains("userLogin")) mapValue.put("userLogin", getInUserLogin());
        return mapValue;
    }

    /** {@inheritDoc} */
    public Map<String, Object> outputMap() {
        Map<String, Object> mapValue = new FastMap<String, Object>();
        if (outParameters.contains("beginningCashAccountBalances")) mapValue.put("beginningCashAccountBalances", getOutBeginningCashAccountBalances());
        if (outParameters.contains("beginningCashAmount")) mapValue.put("beginningCashAmount", getOutBeginningCashAmount());
        if (outParameters.contains("endingCashAccountBalances")) mapValue.put("endingCashAccountBalances", getOutEndingCashAccountBalances());
        if (outParameters.contains("endingCashAmount")) mapValue.put("endingCashAmount", getOutEndingCashAmount());
        if (outParameters.contains("errorMessage")) mapValue.put("errorMessage", getOutErrorMessage());
        if (outParameters.contains("errorMessageList")) mapValue.put("errorMessageList", getOutErrorMessageList());
        if (outParameters.contains("financingCashFlow")) mapValue.put("financingCashFlow", getOutFinancingCashFlow());
        if (outParameters.contains("financingCashFlowAccountBalances")) mapValue.put("financingCashFlowAccountBalances", getOutFinancingCashFlowAccountBalances());
        if (outParameters.contains("investingCashFlow")) mapValue.put("investingCashFlow", getOutInvestingCashFlow());
        if (outParameters.contains("investingCashFlowAccountBalances")) mapValue.put("investingCashFlowAccountBalances", getOutInvestingCashFlowAccountBalances());
        if (outParameters.contains("locale")) mapValue.put("locale", getOutLocale());
        if (outParameters.contains("netCashFlow")) mapValue.put("netCashFlow", getOutNetCashFlow());
        if (outParameters.contains("netIncome")) mapValue.put("netIncome", getOutNetIncome());
        if (outParameters.contains("operatingCashFlow")) mapValue.put("operatingCashFlow", getOutOperatingCashFlow());
        if (outParameters.contains("operatingCashFlowAccountBalances")) mapValue.put("operatingCashFlowAccountBalances", getOutOperatingCashFlowAccountBalances());
        if (outParameters.contains("responseMessage")) mapValue.put("responseMessage", getOutResponseMessage());
        if (outParameters.contains("successMessage")) mapValue.put("successMessage", getOutSuccessMessage());
        if (outParameters.contains("successMessageList")) mapValue.put("successMessageList", getOutSuccessMessageList());
        if (outParameters.contains("timeZone")) mapValue.put("timeZone", getOutTimeZone());
        if (outParameters.contains("userLogin")) mapValue.put("userLogin", getOutUserLogin());
        return mapValue;
    }

    /** {@inheritDoc} */
    public void putAllInput(Map<String, Object> mapValue) {
        if (mapValue.containsKey("glFiscalTypeId")) setInGlFiscalTypeId((String) mapValue.get("glFiscalTypeId"));
        if (mapValue.containsKey("locale")) setInLocale((Locale) mapValue.get("locale"));
        if (mapValue.containsKey("organizationPartyId")) setInOrganizationPartyId((String) mapValue.get("organizationPartyId"));
        if (mapValue.containsKey("timeZone")) setInTimeZone((TimeZone) mapValue.get("timeZone"));
        if (mapValue.containsKey("userLogin")) setInUserLogin((GenericValue) mapValue.get("userLogin"));
    }

    /** {@inheritDoc} */
    public void putAllOutput(Map<String, Object> mapValue) {
        if (mapValue.containsKey("beginningCashAccountBalances")) setOutBeginningCashAccountBalances((Map) mapValue.get("beginningCashAccountBalances"));
        if (mapValue.containsKey("beginningCashAmount")) setOutBeginningCashAmount((BigDecimal) mapValue.get("beginningCashAmount"));
        if (mapValue.containsKey("endingCashAccountBalances")) setOutEndingCashAccountBalances((Map) mapValue.get("endingCashAccountBalances"));
        if (mapValue.containsKey("endingCashAmount")) setOutEndingCashAmount((BigDecimal) mapValue.get("endingCashAmount"));
        if (mapValue.containsKey("errorMessage")) setOutErrorMessage((String) mapValue.get("errorMessage"));
        if (mapValue.containsKey("errorMessageList")) setOutErrorMessageList((List) mapValue.get("errorMessageList"));
        if (mapValue.containsKey("financingCashFlow")) setOutFinancingCashFlow((BigDecimal) mapValue.get("financingCashFlow"));
        if (mapValue.containsKey("financingCashFlowAccountBalances")) setOutFinancingCashFlowAccountBalances((Map) mapValue.get("financingCashFlowAccountBalances"));
        if (mapValue.containsKey("investingCashFlow")) setOutInvestingCashFlow((BigDecimal) mapValue.get("investingCashFlow"));
        if (mapValue.containsKey("investingCashFlowAccountBalances")) setOutInvestingCashFlowAccountBalances((Map) mapValue.get("investingCashFlowAccountBalances"));
        if (mapValue.containsKey("locale")) setOutLocale((Locale) mapValue.get("locale"));
        if (mapValue.containsKey("netCashFlow")) setOutNetCashFlow((BigDecimal) mapValue.get("netCashFlow"));
        if (mapValue.containsKey("netIncome")) setOutNetIncome((BigDecimal) mapValue.get("netIncome"));
        if (mapValue.containsKey("operatingCashFlow")) setOutOperatingCashFlow((BigDecimal) mapValue.get("operatingCashFlow"));
        if (mapValue.containsKey("operatingCashFlowAccountBalances")) setOutOperatingCashFlowAccountBalances((Map) mapValue.get("operatingCashFlowAccountBalances"));
        if (mapValue.containsKey("responseMessage")) setOutResponseMessage((String) mapValue.get("responseMessage"));
        if (mapValue.containsKey("successMessage")) setOutSuccessMessage((String) mapValue.get("successMessage"));
        if (mapValue.containsKey("successMessageList")) setOutSuccessMessageList((List) mapValue.get("successMessageList"));
        if (mapValue.containsKey("timeZone")) setOutTimeZone((TimeZone) mapValue.get("timeZone"));
        if (mapValue.containsKey("userLogin")) setOutUserLogin((GenericValue) mapValue.get("userLogin"));
    }

    /**
     * Construct a <code>CashFlowInterface</code> from the input values of the given <code>CashFlowInterface</code>.
     * @param input a <code>CashFlowInterface</code>
     */
    public static CashFlowInterface fromInput(CashFlowInterface input) {
        CashFlowInterface service = new CashFlowInterface();
        service.putAllInput(input.inputMap());
        return service;
    }

    /**
     * Construct a <code>CashFlowInterface</code> from the output values of the given <code>CashFlowInterface</code>.
     * @param output a <code>CashFlowInterface</code>
     */
    public static CashFlowInterface fromOutput(CashFlowInterface output) {
        CashFlowInterface service = new CashFlowInterface();
        service.putAllOutput(output.outputMap());
        return service;
    }

    /**
     * Construct a <code>CashFlowInterface</code> from the given input <code>Map</code>.
     * @param mapValue the service input <code>Map</code>
     */
    public static CashFlowInterface fromInput(Map<String, Object> mapValue) {
        CashFlowInterface service = new CashFlowInterface();
        service.putAllInput(mapValue);
        return service;
    }

    /**
     * Construct a <code>CashFlowInterface</code> from the given output <code>Map</code>.
     * @param mapValue the service output <code>Map</code>
     */
    public static CashFlowInterface fromOutput(Map<String, Object> mapValue) {
        CashFlowInterface service = new CashFlowInterface();
        service.putAllOutput(mapValue);
        return service;
    }
}