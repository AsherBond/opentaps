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

package org.opentaps.common.util;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.net.MalformedURLException;
import java.net.URL;
import java.sql.Timestamp;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.TreeSet;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import javolution.util.FastList;
import javolution.util.FastMap;
import javolution.util.FastSet;
import org.apache.commons.lang.StringUtils;
import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFCellStyle;
import org.apache.poi.hssf.usermodel.HSSFDataFormat;
import org.apache.poi.hssf.usermodel.HSSFFont;
import org.apache.poi.hssf.usermodel.HSSFRichTextString;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.hssf.util.HSSFColor;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;
import org.ofbiz.base.component.ComponentConfig;
import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.GeneralException;
import org.ofbiz.base.util.StringUtil;
import org.ofbiz.base.util.UtilDateTime;
import org.ofbiz.base.util.UtilHttp;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.base.util.string.FlexibleStringExpander;
import org.ofbiz.content.data.DataResourceWorker;
import org.ofbiz.entity.Delegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.condition.EntityCondition;
import org.ofbiz.entity.condition.EntityExpr;
import org.ofbiz.entity.condition.EntityOperator;
import org.ofbiz.entity.model.ModelEntity;
import org.ofbiz.entity.util.ByteWrapper;
import org.ofbiz.entity.util.EntityFindOptions;
import org.ofbiz.entity.util.EntityUtil;
import org.ofbiz.service.DispatchContext;
import org.ofbiz.service.ModelService;
import org.opentaps.foundation.entity.EntityInterface;

/**
 * UtilCommon - A place for common opentaps helper methods.
 *
 * @author     <a href="mailto:leon@opensourcestrategies.com">Leon Torres</a>
 * @version    $Rev: 488 $
 */
public abstract class UtilCommon {

    private static final String MODULE = UtilCommon.class.getName();
    public static String SESSION_ID_PARTTER = ";jsessionid=[^\\.]*\\.jvm1";
    public static String PARAMETER_SEPARATE_CHAR_PARTTER = "[\\?\\&]";

    // Utility class should not be instantiated.
    private UtilCommon() { }

    /** Frequently used EntityFindOption for distinct read-only select. */
    public static final EntityFindOptions DISTINCT_READ_OPTIONS = new EntityFindOptions(true, EntityFindOptions.TYPE_SCROLL_INSENSITIVE, EntityFindOptions.CONCUR_READ_ONLY, true);

    /** Read only without distinct. */
    public static final EntityFindOptions READ_ONLY_OPTIONS = new EntityFindOptions(true, EntityFindOptions.TYPE_SCROLL_INSENSITIVE, EntityFindOptions.CONCUR_READ_ONLY, false);

    // Following int constants are mainly used to make the values non ambiguous (some methods may take MS, other Seconds ...) in the code and increase readability.

    /** Number of milliseconds in one second. For use in before / after methods for example. */
    public static final int MSEC_IN_1_SEC = 1000;
    /** Number of milliseconds in one minute. For use in before / after methods for example. */
    public static final int MSEC_IN_1_MIN = 60000;
    /** Number of seconds in one hour. For use in overriding transactions timeout for example. */
    public static final int SEC_IN_1_HOUR = 3600;
    /** Number of seconds in one hour. For use in overriding transactions timeout for example. */
    public static final int SEC_IN_2_HOURS = 7200;

    /**
     * Checks if the two lists are equivalent.
     * Returns true if:
     * <ul>
     *  <li>l1 and l2 are both null</li>
     *  <li>l1 and l2 are the same size, and every element in l1 is contained in l2, and every element in l2 is contained in l2</li>
     * </ul>
     * @param l1 first <code>List</code>
     * @param l2 second <code>List</code>
     * @return are the two list equivalent
     */
    @SuppressWarnings("unchecked")
    public static boolean isEquivalent(List l1, List l2) {
        // initial checks
        if ((l1 == null) && (l2 == null)) {
            return true;  // if they are both null, they are equivalent
        }
        if ((l1 == null) || (l2 == null)) {
            return false;  // otherwise, if one is null and the other is not, they are not equivalent
        }
        if (l1.size() != l2.size()) {
            return false; // sizes unequal - not equivalent
        }
        if ((l1.size() == 0) && (l2.size() == 0)) {
            return true; // both no elements - equivalent
        }

        // now loop through both lists and make sure they all contain the same elements
        for (Object element : l1) {
            if ((!l2.contains(element))) {
                return false;
            }
        }

        // since both list have the same number of elements and l2 contains all the elements of l1, then l2 contains all the elements of l1 too
        return true;
    }

    /**
     * Returns the "system" userLogin generic value.
     * @param delegator a <code>Delegator</code>
     * @return the <code>GenericValue</code> representing the "system" UserLogin entity.
     * @throws GenericEntityException if an error occurs
     */
    public static GenericValue getSystemUserLogin(Delegator delegator) throws GenericEntityException {
        return delegator.findByPrimaryKeyCache("UserLogin", UtilMisc.toMap("userLoginId", "system"));
    }

    /**
     * Returns the earliest of the two given <code>Timestamp</code>.
     * @param ts1 a <code>Timestamp</code>
     * @param ts2 another <code>Timestamp</code>
     * @return ts1 if it is before ts2, ts2 otherwise
     */
    public static Timestamp earlierOf(Timestamp ts1, Timestamp ts2) {
        if (ts1.before(ts2)) {
            return ts1;
        } else {
            return ts2;
        }
    }

    /**
     * Returns the latest of the two given <code>Timestamp</code>.
     * @param ts1 a <code>Timestamp</code>
     * @param ts2 another <code>Timestamp</code>
     * @return ts1 if it is after ts2, ts2 otherwise
     */
    public static Timestamp laterOf(Timestamp ts1, Timestamp ts2) {
        if (ts1.after(ts2)) {
            return ts1;
        } else {
            return ts2;
        }
    }

    /**
     * Gets the duration between two timestamps in localized format.
     * @param start a <code>Timestamp</code> value
     * @param end a <code>Timestamp</code> value
     * @param timeZone a <code>TimeZone</code> value
     * @param locale a <code>Locale</code> value
     * @return a <code>String</code> value
     */
    public static String getDuration(Timestamp start, Timestamp end, TimeZone timeZone, Locale locale) {
        Calendar cal = Calendar.getInstance(timeZone, locale);

        // set the time to the beginning of the day
        cal.setTime(UtilDateTime.getDayStart(start, timeZone, locale));

        // the duration in milliseconds
        long duration = end.getTime() - start.getTime();

        // compute number of full hours
        int hours = (int) (duration / 1000 / 60 / 60);

        // compute number of minutes
        int minutes = (int) ((duration / 1000 / 60) % 60);

        //TODO: oandreyev. Currently this way to make formated time is suitable but
        // algorithm should be refactored and based on applicable time format.
        return hours + ":" + (minutes < 10 ? "0" : "") + minutes;
    }

    /**
     * This method takes the date/time/duration form input and transforms it into an end timestamp.
     * It uses Java Date formatting capabilities to transform the duration input into an interval.
     *
     * @param start Full date, hour, minute and second of the starting time
     * @param duration The user input for hour such as "1:00"
     * @param timeZone a <code>TimeZone</code> value
     * @param locale a <code>Locale</code> value
     * @return the end <code>Timestamp</code>
     * @throws IllegalArgumentException If the duration input is unparseable or negative
     */
    public static Timestamp getEndTimestamp(Timestamp start, String duration, Locale locale, TimeZone timeZone) throws IllegalArgumentException {

        // return the start timestamp if no duration specified (i.e., duration = 0)
        if (duration == null || duration.length() == 0) {
            return start;
        }

        Calendar cal = Calendar.getInstance(timeZone, locale);

        // Turn the duraiton into a date and time with the hour being the duration (note this is input from user, which we require to be in HH:mm form)
        SimpleDateFormat df = new SimpleDateFormat("HH:mm");
        try {
            cal.setTime(df.parse(duration));
        } catch (ParseException e) {
            throw new IllegalArgumentException(String.format("Duration input must be in %1$s format.", UtilDateTime.getTimeFormat(locale)));
        }

        // extract the days, hours and minutes
        int days = cal.get(Calendar.DAY_OF_YEAR) - 1;
        int hours = cal.get(Calendar.HOUR_OF_DAY);
        int minutes = cal.get(Calendar.MINUTE);

        // set to the start time and add the hours and minutes
        cal.setTime(start);
        cal.set(Calendar.DAY_OF_YEAR, days + cal.get(Calendar.DAY_OF_YEAR));
        cal.set(Calendar.HOUR_OF_DAY, hours + cal.get(Calendar.HOUR_OF_DAY));
        cal.set(Calendar.MINUTE, minutes + cal.get(Calendar.MINUTE));

        // create the end timestamp
        Timestamp end = new Timestamp(cal.getTimeInMillis());

        // make sure it's after the start timestamp
        if (end.before(start)) {
            throw new IllegalArgumentException("Cannot set a negative duration.");
        }

        // return our result as a Timestamp
        return end;
    }

    /**
     * Gets the <code>Timestamp</code> which time is the given milliseconds before the given <code>Timestamp</code>.
     * @param ts the reference <code>Timestamp</code>
     * @param milliseconds number of milliseconds before ts for which to return
     * @return a <code>Timestamp</code>, <code>null</code> if ts is <code>null</code>
     */
    public static Timestamp beforeMillisecs(Timestamp ts, long milliseconds) {
        if (ts != null) {
            return new Timestamp(ts.getTime() - milliseconds);
        } else {
            return null;
        }
    }

    /**
     * Gets the <code>Timestamp</code> which time is the given milliseconds before the given <code>Timestamp</code>.
     * @param ts the reference <code>Timestamp</code>
     * @param milliseconds number of milliseconds before ts for which to return
     * @return a <code>Timestamp</code>, <code>null</code> if ts is <code>null</code>
     */
    public static Timestamp beforeMillisecs(Timestamp ts, BigDecimal milliseconds) {
        if (milliseconds == null) {
            milliseconds = BigDecimal.ZERO;
        }
        return beforeMillisecs(ts, milliseconds.longValue());
    }

    /**
     * Gets the <code>Timestamp</code> which time is the given milliseconds after the given <code>Timestamp</code>.
     * @param ts the reference <code>Timestamp</code>
     * @param milliseconds number of milliseconds after ts for which to return
     * @return a <code>Timestamp</code>, <code>null</code> if ts is <code>null</code>
     */
    public static Timestamp afterMillisecs(Timestamp ts, long milliseconds) {
        if (ts != null) {
            return new Timestamp(ts.getTime() + milliseconds);
        } else {
            return null;
        }
    }

    /**
     * Gets the <code>Timestamp</code> which time is the given milliseconds after the given <code>Timestamp</code>.
     * @param ts the reference <code>Timestamp</code>
     * @param milliseconds number of milliseconds after ts for which to return
     * @return a <code>Timestamp</code>, <code>null</code> if ts is <code>null</code>
     */
    public static Timestamp afterMillisecs(Timestamp ts, Double milliseconds) {
        if (milliseconds == null) {
            milliseconds = new Double(0);
        }
        return afterMillisecs(ts, milliseconds.longValue());
    }

    /**
     * Converts a <code>String</code> into it corresponding <code>Timestamp</code> value.
     * @param timestampString a <code>String</code> representing a <code>Timestamp</code>
     * @return the corresponding <code>Timestamp</code> value
     * @deprecated Use UtilDate.toTimestamp(String timestampString, TimeZone timeZone, Locale locale)
     */
    @Deprecated
    public static Timestamp toTimestamp(String timestampString) {
        return UtilDate.toTimestamp(timestampString, TimeZone.getDefault(), Locale.getDefault());
    }

    /**
     * Gets a list of countries.
     * @param delegator a <code>Delegator</code> value
     * @return a <code>List</code> of countries Geo <code>GenericValue</code>
     * @exception GenericEntityException if an error occurs
     */
    public static List<GenericValue> getCountries(Delegator delegator) throws GenericEntityException {
        return delegator.findByAndCache("Geo", UtilMisc.toMap("geoTypeId", "COUNTRY"), UtilMisc.toList("geoName"));
    }

    /**
     * Gets a list of states in the given country.
     * @param delegator a <code>Delegator</code> value
     * @param countryGeoId the country for which to return the list of states
     * @return a <code>List</code> of states Geo <code>GenericValue</code>
     * @exception GenericEntityException if an error occurs
     */
    public static List<GenericValue> getStates(Delegator delegator, String countryGeoId) throws GenericEntityException {
        return delegator.findByAndCache("GeoAssocAndGeoTo", UtilMisc.toMap("geoIdFrom", countryGeoId, "geoAssocTypeId", "REGIONS"), UtilMisc.toList("geoName"));
    }

    /**
     * Gets a list of currencies.
     * @param delegator a <code>Delegator</code> value
     * @return a <code>List</code> of currencies <code>GenericValue</code>
     * @exception GenericEntityException if an error occurs
     */
    public static List<GenericValue> getCurrencies(Delegator delegator) throws GenericEntityException {
        return delegator.findByAndCache("Uom", UtilMisc.toMap("uomTypeId", "CURRENCY_MEASURE"), UtilMisc.toList("abbreviation"));
    }

    /**
     * Gets the organization party ID.
     * @param request a <code>HttpServletRequest</code> value
     * @return the organizationPartyId from the session, or <code>null</code> if not set
     */
    public static String getOrganizationPartyId(HttpServletRequest request) {
        HttpSession session = request.getSession();
        if (session == null) {
            return null;
        }

        Boolean applicationContextSet = (Boolean) session.getAttribute("applicationContextSet");
        if (applicationContextSet == null) {
            UtilConfig.checkDefaultOrganization(request);
        }

        String org = (String) session.getAttribute("organizationPartyId");
        if (UtilValidate.isEmpty(org)) {
            return null;
        }

        return org;
    }

    /**
     * Gets the given organization corresponding PartyAcctgPreference.
     * @param organizationPartyId the organization identifier
     * @param delegator a <code>Delegator</code> value
     * @return the PartyAcctgPreference <code>GenericValue</code>
     * @exception GenericEntityException if an error occurs
     */
    public static GenericValue getOrgAcctgPref(String organizationPartyId, Delegator delegator) throws GenericEntityException {
        GenericValue orgAcctgPref = delegator.findByPrimaryKeyCache("PartyAcctgPreference", UtilMisc.toMap("partyId", organizationPartyId));
        return orgAcctgPref;
    }

    /**
     * Re-factored method to return a field from organizationPartyId or null if it is not set or there are problems.
     * Please define specific get methods and not use this method -- that's why I made it private (Si)
     * @param organizationPartyId the organization identifier
     * @param fieldId name of the field to return from the organization PartyAcctgPreference
     * @param delegator a <code>Delegator</code> value
     * @return the PartyAcctgPreference field string value or <code>null</code> if there is any problem
     */
    private static String getOrgAcctgPrefField(String organizationPartyId, String fieldId, Delegator delegator) {
        try {
            GenericValue acctgPref = getOrgAcctgPref(organizationPartyId, delegator);
            if (UtilValidate.isNotEmpty(acctgPref)) {
                return acctgPref.getString(fieldId);
            } else {
                Debug.logWarning("Cannot get accounting preference [" + fieldId + "] for [" + organizationPartyId + "], no PartyAcctgPreference found.", MODULE);
                return null;
            }
        } catch (GenericEntityException ex) {
            Debug.logError("Problem getting accounting preference [" + fieldId + "] for [" + organizationPartyId + "]: " + ex.getMessage(), MODULE);
            return null;
        }
    }

    /**
     * Gets the PartyAcctgPreference.cogsMethodId for the given organization corresponding.
     * @param organizationPartyId the organization identifier
     * @param delegator a <code>Delegator</code> value
     * @return the PartyAcctgPreference.cogsMethodId or <code>null</code> if there is any problem
     */
    public static String getOrgCOGSMethodId(String organizationPartyId, Delegator delegator) {
        return getOrgAcctgPrefField(organizationPartyId, "cogsMethodId", delegator);
    }

    /**
     * Gets the PartyAcctgPreference.baseCurrencyUomId for the given organization corresponding.
     * @param organizationPartyId the organization identifier
     * @param delegator a <code>Delegator</code> value
     * @return the PartyAcctgPreference.cogsMethodId or <code>null</code> if there is any problem
     */
    public static String getOrgBaseCurrency(String organizationPartyId, Delegator delegator) {
        return getOrgAcctgPrefField(organizationPartyId, "baseCurrencyUomId", delegator);
    }

    /**
     * Gets the <code>List</code> of currently active contactMechIds for the given facility.
     * @param facilityId the facility identifier
     * @param delegator a <code>Delegator</code> value
     * @return the <code>List</code> of currently active contactMechIds for the given facility
     * @throws GenericEntityException if an error occurs
     */
    @SuppressWarnings("unchecked")
    public static List<String> getFacilityContactMechIds(String facilityId, Delegator delegator) throws GenericEntityException {
        List<GenericValue> facilityContactMechs = delegator.findByAnd("FacilityContactMech", EntityCondition.makeCondition(EntityOperator.AND,
                EntityCondition.makeCondition("facilityId", facilityId),
                EntityUtil.getFilterByDateExpr()));
        return EntityUtil.getFieldListFromEntityList(facilityContactMechs, "contactMechId", true);
    }

    /**
     * Gets the current postal address of for the given facility.
     * @param delegator a <code>Delegator</code> value
     * @param facilityId the facility identifier
     * @return the current postal address of for the given facility, <code>null</code> if none is found
     * @throws GenericEntityException if an error occurs
     */
    public static GenericValue getFacilityPostalAddress(Delegator delegator, String facilityId) throws GenericEntityException {
        List<GenericValue> facilityMechPurps = delegator.findByAndCache("FacilityContactMechPurpose", UtilMisc.toMap("facilityId", facilityId, "contactMechPurposeTypeId", "SHIP_ORIG_LOCATION"));
        facilityMechPurps = EntityUtil.filterByDate(facilityMechPurps);
        if (UtilValidate.isNotEmpty(facilityMechPurps)) {
            return EntityUtil.getFirst(facilityMechPurps).getRelatedOne("ContactMech").getRelatedOne("PostalAddress");
        }
        return null;
    }

    /**
     * Gets the <code>List</code> of facilityId of the facilities which can get inventory for the given organization.
     * @param organizationPartyId the organization identifier
     * @param delegator a <code>Delegator</code> value
     * @return the <code>List</code> of facilityId of the facilities which can get inventory for the given organization
     * @throws GenericEntityException if an error occurs
     */
    public static List<String> getOrgReceivingFacilityIds(String organizationPartyId, Delegator delegator) throws GenericEntityException {
        return EntityUtil.getFieldListFromEntityList(getOrganizationReceivingFacilities(organizationPartyId, delegator), "facilityId", true);
    }

    /**
     * Gets the <code>List</code> of facilities (warehouses) for which inventory is received on behalf of the given organization.
     * This could be either Facility owned by the organization or where the organization has a role of RECV_INV_FOR.
     * @param organizationPartyId the organization identifier
     * @param delegator a <code>Delegator</code> value
     * @return the <code>List</code> of receiving facilities
     * @throws GenericEntityException if an error occurs
     */
    public static List<GenericValue> getOrganizationReceivingFacilities(String organizationPartyId, Delegator delegator) throws GenericEntityException {
        if (UtilValidate.isEmpty(organizationPartyId)) {
            return null;
        }

        // first find the facilities where this organization is an owner
        List<GenericValue> facilities = delegator.findByAnd("Facility", UtilMisc.toMap("ownerPartyId", organizationPartyId), UtilMisc.toList("facilityName"));

        // now find the facilities where this organization can receive inventory
        // SELECT * FROM FACILITY WHERE FACILITY_ID IN (SELECT * FROM FACILITY_ROLE WHERE ROLE_TYPE_ID = 'RECV_INV_FOR' AND PARTY_ID = organizationPartyId))
        List<GenericValue> additionalFacilities = delegator.findByAnd("FacilityRole", UtilMisc.toMap("roleTypeId", "RECV_INV_FOR", "partyId", organizationPartyId));
        if (UtilValidate.isNotEmpty(additionalFacilities)) {
            facilities.addAll(delegator.findByAnd("Facility", UtilMisc.toList(EntityCondition.makeCondition("facilityId", EntityOperator.IN, EntityUtil.getFieldListFromEntityList(additionalFacilities, "facilityId", true)))));
        }
        return facilities;
    }
    /**
     * Retrieves information required for companyHeader.fo.ftl for the given organization.
     * @param organizationPartyId the organization identifier
     * @param delegator a <code>Delegator</code> value
     * @return a <code>Map</code> containing organization information
     * @exception GenericEntityException if an error occurs
     */
    public static Map<String, Object> getOrganizationHeaderInfo(String organizationPartyId, Delegator delegator) throws GenericEntityException {
        Map<String, Object> results = FastMap.newInstance();

        // the logo image URL
        GenericValue partyGroup = delegator.findByPrimaryKey("PartyGroup", UtilMisc.toMap("partyId", organizationPartyId));
        if (partyGroup != null && UtilValidate.isNotEmpty(partyGroup.getString("logoImageUrl"))) {
            results.put("organizationLogoImageUrl", partyGroup.getString("logoImageUrl"));
        }

        // the company name
        if (partyGroup != null && UtilValidate.isNotEmpty(partyGroup.getString("groupName"))) {
            results.put("organizationCompanyName", partyGroup.getString("groupName"));
        }

        // the address
        List<GenericValue> addresses = delegator.findByAnd("PartyContactMechPurpose", UtilMisc.toMap("partyId", organizationPartyId, "contactMechPurposeTypeId", "GENERAL_LOCATION"));
        GenericValue address = EntityUtil.getFirst(EntityUtil.filterByDate(addresses, UtilDateTime.nowTimestamp(), "fromDate", "thruDate", true));
        if (address != null) {
            GenericValue postalAddress = delegator.findByPrimaryKey("PostalAddress", UtilMisc.toMap("contactMechId", address.getString("contactMechId")));
            if (postalAddress != null) {
                results.put("organizationPostalAddress", postalAddress);

                // get the country name and state/province abbreviation
                GenericValue country = postalAddress.getRelatedOneCache("CountryGeo");
                if (country != null) {
                    results.put("countryName", country.getString("geoName"));
                }
                GenericValue stateProvince = postalAddress.getRelatedOneCache("StateProvinceGeo");
                if (stateProvince != null) {
                    results.put("stateProvinceAbbrv", stateProvince.getString("abbreviation"));
                }
            }
        }

        return results;
    }

    /**
     * Checks if there are any valid status changes from the given statusId.
     * @param statusId the original status
     * @param delegator a <code>Delegator</code> value
     * @return a <code>boolean</code> if any valid status changes from the given statusId is found
     * @exception GenericEntityException if an error occurs
     */
    public static boolean hasValidChange(String statusId, Delegator delegator) throws GenericEntityException {
        return delegator.findCountByAnd("StatusValidChange", UtilMisc.toMap("statusId", statusId)) > 0 ? true : false;
    }

    /**
     * Fetches a <code>List</code> of <code>StatusValidChange</code> that are a valid change from the given status.
     * @param statusId the current status ID
     * @param delegator a <code>Delegator</code> value
     * @return the <code>List</code> of status <code>GenericValue</code>
     * @exception GenericEntityException if an error occurs
     */
    public static List<GenericValue> getValidChanges(String statusId, Delegator delegator) throws GenericEntityException {
        return delegator.findByAndCache("StatusValidChange", UtilMisc.toMap("statusId", statusId));
    }

    /**
     * Checks if there is a <code>StatusValidChange</code> for the given status and status to.
     * @param statusId the current status ID
     * @param statusIdTo the new status ID
     * @param delegator a <code>Delegator</code> value
     * @return <code>true</code> if the change is valid
     * @exception GenericEntityException if an error occurs
     */
    public static boolean isValidChange(String statusId, String statusIdTo, Delegator delegator) throws GenericEntityException {
        return delegator.findCountByAnd("StatusValidChange", UtilMisc.toMap("statusId", statusId, "statusIdTo", statusIdTo)) > 0 ? true : false;
    }

    /**
     * Fetches a <code>List</code> of <code>StatusItem</code> by status type from the cache.
     * @param statusTypeId the type of status to fetch
     * @param delegator a <code>Delegator</code> value
     * @return the <code>List</code> of status <code>GenericValue</code>
     * @exception GenericEntityException if an error occurs
     */
    public static List<GenericValue> getStatuses(String statusTypeId, Delegator delegator) throws GenericEntityException {
        return delegator.findByAndCache("StatusItem", UtilMisc.toMap("statusTypeId", statusTypeId), UtilMisc.toList("sequenceId"));
    }

    /**
     * Checks if a request has an error set.
     * @param request a <code>HttpServletRequest</code> value
     * @return a <code>Boolean</code> value
     */
    @SuppressWarnings("unchecked")
    public static Boolean hasError(HttpServletRequest request) {
        Enumeration<String> attrs = request.getAttributeNames();
        while (attrs.hasMoreElements()) {
            String a = attrs.nextElement();
            if ("_ERROR_MESSAGE_LIST_".equals(a) || "_ERROR_MESSAGE_".equals(a) || "opentapsErrors".equals(a)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Gets a request parameter, or <code>null</code> if the string was empty.  This simplifies the trinary logic
     * of empty strings vs <code>null</code> strings into a simple boolean, either the string exists and has content
     * or it is null.
     * @param request a <code>HttpServletRequest</code> value
     * @param parameterName the parameter name to get
     * @return the parameter <code>String</code> value or <code>null</code> if the value was <code>null</code> or empty
     * @see #getParameter(Map, String)
     * @see #getUTF8Parameter(HttpServletRequest, String)
     */
    public static String getParameter(HttpServletRequest request, String parameterName) {
        String result = request.getParameter(parameterName);
        if (result == null) {
            return null;
        }
        result = result.trim();
        if (result.length() == 0) {
            return null;
        }
        return result;
    }

    /**
     * As above, but search a downstream context.  This allows searching fields set by the screen widget,
     * by the request parameters, by the session, by the global context, and so on.
     * @param context the context <code>Map</code>
     * @param parameterName the parameter name to get
     * @return the parameter <code>String</code> value or <code>null</code> if the value was <code>null</code> or empty
     * @see #getParameter(HttpServletRequest, String)
     */
    @SuppressWarnings("unchecked")
    public static String getParameter(Map<String, ?> context, String parameterName) {
        String result = (String) context.get(parameterName); // search the context map
        if (result == null) {
            // search the request and session using the special "parameters" map
            Map<String, String> parameters = (Map<String, String>) context.get("parameters");
            if (parameters != null) {
                result = parameters.get(parameterName);
            }
            if (result == null) {
                // search the global context
                Map<String, String> global = (Map<String, String>) context.get("globalContext");
                if (global != null) {
                    result = global.get(parameterName);
                }
            }
        }
        if (result == null) {
            return null;
        }
        result = result.trim();
        if (result.length() == 0) {
            return null;
        }
        return result;
    }

    /**
     * Gets an UTF8 encoded string from request parameters.
     * @param request a <code>HttpServletRequest</code> value
     * @param parameterName the parameter name to get
     * @return a <code>String</code> value
     * @see #getParameter(HttpServletRequest, String)
     */
    public static String getUTF8Parameter(HttpServletRequest request, String parameterName) {
        try {
            String result = request.getParameter(parameterName);
            if (result == null) {
                return null;
            }
            result = new String(result.getBytes("iso-8859-1"), "UTF-8");
            if (result == null) {
                return null;
            }
            result = result.trim();
            if (result.length() == 0) {
                return null;
            }
            return result;
        } catch (UnsupportedEncodingException e) {
            return null;
        }
    }

    /**
     * Gets the <code>List</code> of children for the given parent.  The entity must have a Parent/Child relationship.
     * This function relies on the cache and is intended for data that rarely changes, such as type entities.
     * @param delegator a <code>Delegator</code> value
     * @param parent the parent <code>GenericValue</code>
     * @return the <code>List</code> of children <code>GenericValue</code> for the given parent
     * @exception GenericEntityException if an error occurs
     */
    public static List<GenericValue> getEntityChildren(Delegator delegator, GenericValue parent) throws GenericEntityException {
        List<GenericValue> combined = FastList.newInstance();
        if (parent == null) {
            return combined;
        }
        combined.add(parent);
        List<GenericValue> children = parent.getRelatedCache("Child" + parent.getEntityName());
        for (Iterator<GenericValue> iter = children.iterator(); iter.hasNext();) {
            GenericValue child = iter.next();
            combined.addAll(getEntityChildren(delegator, child));
        }
        return combined;
    }

    /**
     * Given a parent entity, returns an entity expression that constrains to the members of the parent's family.
     * This function relies on the cache and is intended for data that rarely changes, such as type entities.
     * It also assumes that the entity has one primary key, whose field name must be specified by pkFieldName.
     * @param delegator a <code>Delegator</code> value
     * @param entityName the entity name
     * @param pkFieldName the field name of the primary key
     * @param parentId the value of the primary key for the parent (root) entity
     * @return an <code>EntityExpr</code> value
     * @exception GenericEntityException if an error occurs
     */
    public static EntityExpr getEntityChildrenExpr(Delegator delegator, String entityName, String pkFieldName, String parentId) throws GenericEntityException {
        return getEntityChildrenExpr(delegator, entityName, pkFieldName, parentId, false);
    }

    /**
     * Given a parent entity, returns an entity expression that constrains to the members NOT in the parent's family.
     * This function relies on the cache and is intended for data that rarely changes, such as type entities.
     * It also assumes that the entity has one primary key, whose field name must be specified by pkFieldName.
     * @param delegator a <code>Delegator</code> value
     * @param entityName the entity name
     * @param pkFieldName the field name of the primary key
     * @param parentId the value of the primary key for the parent (root) entity
     * @return an <code>EntityExpr</code> value
     * @exception GenericEntityException if an error occurs
     */
    public static EntityExpr getEntityChildrenComplementExpr(Delegator delegator, String entityName, String pkFieldName, String parentId) throws GenericEntityException {
        return getEntityChildrenExpr(delegator, entityName, pkFieldName, parentId, true);
    }

    /**
     * Builds an <code>EntityExpr</code> for getting parent / child related entities.
     * See above.
     * @param delegator a <code>Delegator</code> value
     * @param entityName the entity name
     * @param pkFieldName the field name of the primary key
     * @param parentId the value of the primary key for the parent (root) entity
     * @param isComplement if <code>true</code>, gets the child not related to the parent, else get the child related to the parent
     * @return an <code>EntityExpr</code> value
     * @exception GenericEntityException if an error occurs
     */
    private static EntityExpr getEntityChildrenExpr(Delegator delegator, String entityName, String pkFieldName, String parentId, boolean isComplement) throws GenericEntityException {

        // first get the root value and if it doesn't exist, return a condition that always evaluates to true
        GenericValue parent = delegator.findByPrimaryKeyCache(entityName, UtilMisc.toMap(pkFieldName, parentId));
        if (parent == null) {
            Debug.logWarning("Cannot find " + entityName + " [" + parentId + "]", MODULE);
            return EntityCondition.makeCondition("1", EntityOperator.EQUALS, "1");
        }

        // recursively build the list of ids that are of this type
        Set<Object> ids = FastSet.newInstance();
        recurseGetEntityChildrenSet(parent, ids, pkFieldName);

        // make a WHERE paymentTypeId IN (list of ids) expression or NOT_IN for complement search
        return EntityCondition.makeCondition(pkFieldName, (isComplement ? EntityOperator.NOT_IN : EntityOperator.IN), ids);
    }

    /**
     * Builds a set of id values for a parent/child tree using pkFieldName.
     * @param parent the parent <code>GenericValue</code>
     * @param ids the <code>Set</code> of identifiers to populate
     * @param pkFieldName the name of the primary key field in the entity
     * @exception GenericEntityException if an error occurs
     */
    private static void recurseGetEntityChildrenSet(GenericValue parent, Set<Object> ids, String pkFieldName) throws GenericEntityException {
        ids.add(parent.get(pkFieldName));
        List<GenericValue> children = parent.getRelatedCache("Child" + parent.getEntityName());
        for (Iterator<GenericValue> iter = children.iterator(); iter.hasNext();) {
            GenericValue child = iter.next();
            recurseGetEntityChildrenSet(child, ids, pkFieldName);
        }
    }

    /**
     * Checks if an application component is loaded.
     * @param componentName the application component name
     * @return <code>true</code> if the application component is loaded
     */
    public static boolean isLoaded(String componentName) {
        if (UtilValidate.isEmpty(componentName)) {
            return false;
        }
        Collection<ComponentConfig> componentConfigs = ComponentConfig.getAllComponents();
        if (componentConfigs == null) {
            return false;
        }
        for (ComponentConfig componentConfig : componentConfigs) {
            if (componentName.equalsIgnoreCase(componentConfig.getComponentName())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Makes a query string from the parameters map. Used for navigation history.
     * @param parameters the parameters <code>Map</code>
     * @param override a <code>List</code> of parameter names to include, if <code>null</code> or empty then all the parameters will be included except a few common
     * @return a query <code>String</code> such as <code>paramName1=value1&amp;paramName2=value2 ...</code>
     */
    public static String makeHistoryQueryString(Map<String, ? extends Object> parameters, List<String> override) {
        Set<String> keySet = parameters.keySet();
        Set<String> excludes = FastSet.<String>newInstance();
        String queryString = "";

        if (override != null && override.size() > 0) {
            // Include to query string parameters with names that is listed in override
            String[] tokens = UtilHttp.urlEncodeArgs(parameters, false).split("[?&]");
            for (String key : override) {
                for (String token : tokens) {
                    if (token.startsWith(key)) {
                        queryString += (token + "&");
                    }
                }
            }
        } else {
            // Exclude from query string well known and unnecessary in our case parameters
            excludes.add("VIEW_INDEX");
            excludes.add("VIEW_SIZE");
            excludes.add("viewIndex");
            excludes.add("viewSize");
            excludes.add("dispatcher");
            excludes.add("thisRequestUri");
            excludes.add("servletContext");
            excludes.add("delegator");
            excludes.add("autoUserLogin");
            excludes.add("userLogin");
            excludes.add("person");
            excludes.add("delegatorName");
            excludes.add("visit");
            excludes.add("entityDelegatorName");
            excludes.add("localDispatcherName");
            excludes.add("componentName");
            excludes.add("autoName");
            excludes.add("applicationContextSet");
            excludes.add("multiPartMap");
            excludes.add("serviceReaderUrls");
            excludes.add("mainApplicationDecoratorLocation");
            excludes.add("mainDecoratorLocation");
            excludes.add("security");
            excludes.add("targetRequestUri");
            excludes.add("jpublishWrapper");
            excludes.add("externalLoginKey");
            excludes.add("sessionId");
            excludes.add("timeZone");
            excludes.add("ofbizServerName");

            for (String key : keySet) {

                //Exclude from query string all parameters started with "_"
                if (key.startsWith("_")) {
                    excludes.add(key);
                    continue;
                }

                //Exclude from query string keys if they are classes
                if (key.startsWith("org.") || key.startsWith("java.") || key.startsWith("javax.") || key.startsWith("javolution.") || key.startsWith("paginator.")) {
                    excludes.add(key);
                    continue;
                }

                // Exclude permission variables as well
                if (key.startsWith("has") && key.endsWith("Permission")) {
                    excludes.add(key);
                    continue;
                }
            }

            queryString = UtilHttp.stripNamedParamsFromQueryString(UtilHttp.urlEncodeArgs(parameters, false), excludes);
        }

        int lastIndex = queryString.length() - 1;
        if (UtilValidate.isNotEmpty(queryString) && '&' == queryString.charAt(lastIndex)) {
            queryString = queryString.substring(0, lastIndex);
        }

        return queryString;
    }

    /**
     * Prepares an History entry for later processing.
     * @param text the text that should be displayed as the label of the entry
     * @param view the view name, if not <code>null</code> the history entry will only get stored if this matches current request URI
     * @param override a <code>List</code> of parameter names to include, if <code>null</code> or empty then all the parameters will be included except a few common
     * @return the history entry <code>Map</code>
     */
    public static Map<String, ?> makeHistoryEntry(String text, String view, List<String> override) {
        Map<String, Object> entry = FastMap.<String, Object>newInstance();
        if (text == null) {
            throw new IllegalArgumentException("Argument \"text\" can't be null");
        }
        entry.put("text", text);
        if (view != null) {
            entry.put("view", view);
        }
        if (override != null) {
            entry.put("override", override);
        }
        return entry;
    }

    /**
     * Prepares an History entry for later processing.
     * @param text the text that should be displayed as the label of the entry
     * @param view the view name
     * @return the history entry <code>Map</code>
     */
    public static Map<String, ?> makeHistoryEntry(String text, String view) {
        return makeHistoryEntry(text, view, null);
    }

    /**
     * Prepares an History entry for later processing.
     * @param text the text that should be displayed as the label of the entry
     * @return the history entry <code>Map</code>
     */
    public static Map<String, ?> makeHistoryEntry(String text) {
        return makeHistoryEntry(text, null, null);
    }

     /**
      * Given a set of values, calculates the correspondent % of total.
      *
      * @param values a <code>Map</code> of values, such as customer/vendor balance values
      * @param minPercentage the minimum percentage to consider for calculation purposes
      * @param locale the <code>Locale</code> used to build the label strings
      * @return returns the weight (percentage) of each balance
      */
    public static List<Map<String, Number>> getPercentageValues(Map<String, BigDecimal> values, BigDecimal minPercentage, Locale locale) {

         Collection<BigDecimal> inValues = values.values();
         Set<String> keys = values.keySet();
         List<Map<String, Number>> list = new LinkedList<Map<String, Number>>();
         BigDecimal total = BigDecimal.ZERO;
         BigDecimal othersTotal = BigDecimal.ZERO;
         final int decimals = 2;  // precision for the percentage values

         // total up all the values
         for (BigDecimal value : inValues) {
             total = total.add(value);
         }

         if (total.signum() > 0) { //prevent division by zero
             for (String key : keys) {
                 BigDecimal value = values.get(key);
                 value = value.divide(total, 10, RoundingMode.HALF_UP);
                 if (value.compareTo(minPercentage) == 1) { //greater than minPercentage?
                     Map<String, Number> map = FastMap.newInstance();
                     value = value.multiply(new BigDecimal(100)).setScale(decimals, RoundingMode.HALF_UP); //display only 2 decimal places
                     map.put(key, value);
                     list.add(map);
                 } else {
                     othersTotal = othersTotal.add(value).setScale(decimals + 3, RoundingMode.HALF_UP);
                 }
             }

             // normalize to % - ie 0.577 to 57.7
             othersTotal = othersTotal.multiply(new BigDecimal(100)).setScale(decimals, RoundingMode.HALF_UP);
             if (othersTotal.signum() > 0) {
                 list.add(UtilMisc.<String, Number>toMap(UtilMessage.expandLabel("CommonOther", locale) + String.format(" (%1$s%%)", othersTotal.toString()), othersTotal));
             }
         }

         return list;
     }

    /**
     * Reads a simply formatted, single sheet Excel document into a list of <code>Map</code> skipping the first row considered to be the header.
     * @param stream an <code>InputStream</code> of the excel document
     * @param columnNames a List containing the keys to use when mapping the columns into the Map (column 1 goes in the Map key columnNames 1, etc ...)
     * @return the List of Map representing the rows
     * @throws IOException if an error occurs
     */
    public static List<Map<String, String>> readExcelFile(InputStream stream, List<String> columnNames) throws IOException {
        return readExcelFile(stream, columnNames, 1);
    }

    /**
     * Reads a simply formatted, single sheet Excel document into a list of <code>Map</code>.
     * @param stream an <code>InputStream</code> of the excel document
     * @param columnNames a List containing the keys to use when mapping the columns into the Map (column 1 goes in the Map key columnNames 1, etc ...)
     * @param skipRows number of rows to skip, typically 1 to skip the header row
     * @return the List of Map representing the rows
     * @throws IOException if an error occurs
     */
    public static List<Map<String, String>> readExcelFile(InputStream stream, List<String> columnNames, int skipRows) throws IOException {
        POIFSFileSystem fs = new POIFSFileSystem(stream);
        HSSFWorkbook wb = new HSSFWorkbook(fs);
        HSSFSheet sheet = wb.getSheetAt(0);
        int sheetLastRowNumber = sheet.getLastRowNum();
        List<Map<String, String>> rows = new ArrayList<Map<String, String>>();
        for (int j = skipRows; j <= sheetLastRowNumber; j++) {
            HSSFRow erow = sheet.getRow(j);
            Map<String, String> row = new HashMap<String, String>();
            for (int i = 0; i < columnNames.size(); i++) {
                String columnName = columnNames.get(i);
                HSSFCell cell = erow.getCell(i);
                String s = "";
                if (cell != null) {

                    // check if cell contains a number
                    BigDecimal bd = null;
                    try {
                        double d = cell.getNumericCellValue();
                        bd = BigDecimal.valueOf(d);
                    } catch (Exception e) {
                        // do nothing
                    }
                    if (bd == null) {
                        s = cell.toString().trim();
                    } else {
                        // if cell contains number trim the tailing zeros so that for example postal code string
                        // does not appear as a floating point number
                        s = bd.toPlainString();
                        // convert XX.XX000 into XX.XX
                        s = s.replaceFirst("^(-?\\d+\\.0*[^0]+)0*\\s*$", "$1");
                        // convert XX.000 into XX
                        s = s.replaceFirst("^(-?\\d+)\\.0*$", "$1");
                    }
                }
                Debug.logInfo("readExcelFile cell (" + j + ", " + i + ") as (" + columnName + ") == " + s, MODULE);
                row.put(columnName, s);
            }
            rows.add(row);
        }
        return rows;
    }

    /**
     * Creates an Excel document with a given column name list, and column data list.
     * The String objects in the column name list are used as Map keys to look up the corresponding
     * column header and data. The column data to be exported is a List of Map objects where
     * the first Map element contains column headers, and the rest has all the column data.
     * @param workBookName a String object as Excel file name
     * @param workSheetName a String object as the name of the Excel sheet
     * @param columnNameList a List of String objects as column names, they usually correspond to entity field names
     * @param data a List of Map objects to be exported where the first Map element contains column headers,
     *        and the rest has all the column data.
     * @throws IOException if an error occurs
     */
    public static void saveToExcel(
            final String workBookName,
            final String workSheetName,
            final List<String> columnNameList,
            final List<Map<String, Object>> data
    ) throws IOException {
        if (StringUtils.isEmpty(workBookName)) {
            throw new IllegalArgumentException("Argument workBookName can't be empty");
        }

        if (StringUtils.isEmpty(workSheetName)) {
            throw new IllegalArgumentException("Argument workSheetName can't be empty");
        }

        if (columnNameList == null || columnNameList.isEmpty()) {
            throw new IllegalArgumentException("Argument columnNameList can't be empty");
        }

        // the data list should have at least one element for the column headers
        if (data == null || data.isEmpty()) {
            throw new IllegalArgumentException("Argument data can't be empty");
        }

        FileOutputStream fileOut = new FileOutputStream(new File(workBookName));
        assert fileOut != null;

        HSSFWorkbook workBook = new HSSFWorkbook();
        assert workBook != null;

        HSSFSheet workSheet = workBook.createSheet(workSheetName);
        assert workSheet != null;

        // create the header row

        HSSFRow headerRow = workSheet.createRow(0);
        assert workSheet != null;

        HSSFFont headerFont = workBook.createFont();
        assert headerFont != null;

        headerFont.setBoldweight(HSSFFont.BOLDWEIGHT_BOLD);
        headerFont.setColor(HSSFColor.BLACK.index);

        HSSFCellStyle headerCellStyle = workBook.createCellStyle();
        assert headerCellStyle != null;

        headerCellStyle.setFont(headerFont);

        // the first data list element should always be the column header map
        Map<String, Object> columnHeaderMap = data.get(0);

        if (columnHeaderMap != null) {
            for (short i = 0; i < columnNameList.size(); i++) {
                HSSFCell cell = headerRow.createCell(i);
                assert cell != null;

                cell.setCellStyle(headerCellStyle);

                Object columnHeaderTitle = columnHeaderMap.get(columnNameList.get(i));
                if (columnHeaderTitle != null) {
                    cell.setCellValue(new HSSFRichTextString(columnHeaderTitle.toString()));
                }
            }
        }

        // create data rows

        // column data starts from the second element
        if (data.size() > 1) {

            // Create the style used for dates.
            HSSFCellStyle dateCellStyle = workBook.createCellStyle();
            String dateFormat = "mm/dd/yyyy hh:mm:ss";
            HSSFDataFormat hsfDateFormat = workBook.createDataFormat();
            short dateFormatIdx = hsfDateFormat.getFormat(dateFormat);
            if (dateFormatIdx == -1) {
                Debug.logWarning("Date format [" + dateFormat + "] could be found or created, try one of the pre-built instead:" + HSSFDataFormat.getBuiltinFormats(), MODULE);
            }
            dateCellStyle.setDataFormat(dateFormatIdx);

            for (int dataRowIndex = 1; dataRowIndex < data.size(); dataRowIndex++) {
                Map<String, Object> rowDataMap = data.get(dataRowIndex);
                if (rowDataMap == null) {
                    continue;
                }

                HSSFRow dataRow = workSheet.createRow(dataRowIndex);
                assert dataRow != null;

                for (short i = 0; i < columnNameList.size(); i++) {
                    HSSFCell cell = dataRow.createCell(i);
                    assert cell != null;

                    Object cellData = rowDataMap.get(columnNameList.get(i));
                    if (cellData != null) {
                        // Note: dates are actually numeric values in Excel and so the cell need to have
                        //  a special style set so it actually displays as a date
                        if (cellData instanceof Calendar) {
                            cell.setCellStyle(dateCellStyle);
                            cell.setCellValue((Calendar) cellData);
                        } else if (cellData instanceof Date) {
                            cell.setCellStyle(dateCellStyle);
                            cell.setCellValue((Date) cellData);
                        } else if (cellData instanceof BigDecimal) {
                            cell.setCellValue(((BigDecimal) cellData).doubleValue());
                        } else if (cellData instanceof Double) {
                            cell.setCellValue(((Double) cellData).doubleValue());
                        } else if (cellData instanceof Integer) {
                            cell.setCellValue(((Integer) cellData).doubleValue());
                        } else if (cellData instanceof BigInteger) {
                            cell.setCellValue(((BigInteger) cellData).doubleValue());
                        } else {
                            cell.setCellValue(new HSSFRichTextString(cellData.toString()));
                        }
                    }
                }
            }
        }

        // auto size the column width
        if (columnHeaderMap != null) {
            for (short i = 0; i < columnNameList.size(); i++) {
                workSheet.autoSizeColumn(i);
            }
        }

        // create the Excel file
        workBook.write(fileOut);
        fileOut.close();
    }

    /**
     * Gets the Url context help resource from the opentaps.helpUrlPattern
     * and the ContextHelpResource entity if it exits. Return null if not.
     * @param delegator a <code>Delegator</code> value
     * @param appName the application name (eg: crmsfa, financials ...)
     * @param screenName the name of the screen
     * @param screenState the state of the screen, optional
     * @return an <code>URL</code> value
     */
    @SuppressWarnings("unchecked")
    public static URL getUrlContextHelpResource(Delegator delegator, String appName, String screenName, String screenState) {
        List helps = null;
        URL helpUrl = null;

        try {
            if (UtilValidate.isEmpty(screenState)) {
                helps = delegator.findByAndCache("ContextHelpResource", UtilMisc.toMap("screenName", screenName, "applicationName", appName));
            } else {
                helps = delegator.findByAndCache("ContextHelpResource", UtilMisc.toMap("screenName", screenName, "applicationName", appName, "screenState", screenState));
            }
        } catch (GenericEntityException ex) {
            helps = null;
            Debug.logError("Problem getting context help resource for [" + appName + ", " + screenName + "]: " + ex.getMessage(), MODULE);
        }
        GenericValue help = EntityUtil.getFirst(helps);

        if (help == null) {
            return helpUrl;
        }

        Map args = UtilMisc.toMap("remotePageName", help.getString("remotePageName"), "remotePageVersion", help.getString("remotePageVersion"));
        String helpUrlPattern = help.getString("overrideUrlPattern");

        if (UtilValidate.isEmpty(helpUrlPattern)) {
            helpUrlPattern = UtilConfig.getPropertyValue("opentaps", "opentaps.helpUrlPattern");
        }

        if (UtilValidate.isEmpty(helpUrlPattern)) {
            UtilMessage.logServiceWarning("OpentapsError_PropertyNotConfigured", UtilMisc.toMap("propertyName", "opentaps.helpUrlPattern", "resource", "opentaps.properties"), UtilMisc.ensureLocale(null), MODULE);
            return null;
        }

        try {
            helpUrl = new URL(FlexibleStringExpander.expandString(helpUrlPattern, args));
        } catch (MalformedURLException ex) {
            Debug.logError("Problem formatting context url help resource for [" + appName + ", " + screenName + "]: " + ex.getMessage(), MODULE);
        }
        return helpUrl;
    }

    /**
     * Converts a localized number <code>String</code> to a <code>BigDecimal</code> using the given <code>Locale</code>, defaulting to the system <code>Locale</code>.
     * @param locale the <code>Locale</code> to use for parsing the number, optional, defaults to the system <code>Locale</code>
     * @param numberString a <code>String</code> to convert to a <code>BigDecimal</code>
     * @return the corresponding <code>BigDecimal</code> value
     * @throws ParseException if an occurs during parsing
     */
    public static BigDecimal parseLocalizedNumber(Locale locale, String numberString) throws ParseException {
        locale = UtilMisc.ensureLocale(locale);
        NumberFormat parser = NumberFormat.getNumberInstance(locale);
        Number n = parser.parse(numberString);
        return new BigDecimal(n.toString());
    }

    /**
     * Converts a <code>Number</code> into a <code>BigDecimal</code>.
     * Note that with Java 5, you can pass in a primitive, which will be autoboxed.
     * @param number the <code>Number</code> to convert
     * @return a <code>BigDecimal</code> value, or <code>null</code> if the given number was <code>null</code>
     * @exception NumberFormatException if the <code>Number</code> cannot be parsed, which shouldn't happen
     */
    public static BigDecimal asBigDecimal(Number number) throws NumberFormatException {
        if (number == null) {
            return null;
        }
        if (number instanceof BigDecimal) {
            return (BigDecimal) number;
        }
        return new BigDecimal(number.toString());
    }

    /**
     * Converts a <code>String</code> into a <code>BigDecimal</code>.
     * @param number the <code>String</code> to convert
     * @return a <code>BigDecimal</code> value, or <code>null</code> if the given number was <code>null</code>
     * @throws NumberFormatException if the <code>String</code> cannot be parsed into <code>BigDecimal</code>
     */
    public static BigDecimal asBigDecimal(String number) throws NumberFormatException {
        if (number == null) {
            return null;
        }
        return new BigDecimal(number);
    }

    /**
     * Converts an <code>Object</code> to a <code>BigDecimal</code>, provided it is a <code>String</code> or <code>Number</code>.
     * @param obj the <code>Object</code> to convert, must be a <code>String</code> or <code>Number</code>
     * @return a <code>BigDecimal</code> value, or <code>null</code> if the given number was <code>null</code>
     * @throws IllegalArgumentException if the given <code>Object</code> is not a <code>String</code> or a <code>Number</code>
     */
    public static BigDecimal asBigDecimal(Object obj) throws IllegalArgumentException {
        if (obj == null) {
            return null;
        }
        if (obj instanceof String) {
            return asBigDecimal((String) obj);
        }
        if (obj instanceof Number) {
            return asBigDecimal((Number) obj);
        }
        throw new IllegalArgumentException("Cannot convert object of type [" + obj.getClass().getName() + "] to BigDecimal.");
    }

    /**
     * Sums all numeric values in the given <code>Map</code> and ignores non numbers.
     * @param map a <code>Map</code> value
     * @return the total of all the numeric values contained in the given <code>Map</code>
     */
    @SuppressWarnings("unchecked")
    public static BigDecimal mapSum(Map map) {
        BigDecimal sum = BigDecimal.ZERO;
        for (Object obj : map.values()) {
            if (obj instanceof Number) {
                sum = sum.add(asBigDecimal((Number) obj));
            }
        }
        return sum;
    }

    /**
     * This method can be helpful writing aspects as <code>proceed()</code> requires array of <code>Object</code> in arguments.
     * @param dctx a <code>DispatchContext</code> value
     * @param context a <code>Map</code> value
     * @return an <code>Object[]</code> value as <code>[DispatchContext, context]</code>
     */
    public static Object[] serviceArgsToArray(DispatchContext dctx, Map<String, Object> context) {
        Object[] params = new Object[2];
        params[0] = dctx;
        params[1] = context;
        return params;
    }

    /**
     * Intended to prepare service context because used service results may have internal
     * attributes or may not. Helpful in AspectJ advises when we emulate service ECA.
     *
     * @param dctx a <code>DispatchContext</code> value
     * @param results results from previous service call.
     * @param system set to <code>true</code> to use the system account instead of the context UserLogin
     * @return the service context <code>Map</code>
     * @throws GenericEntityException if an error occurs
     */
    public static Map<String, Object> makeValidSECAContext(DispatchContext dctx, Map<String, Object> results, boolean system) throws GenericEntityException {
        Map<String, Object> context = FastMap.newInstance();
        context.putAll(results);
        if (UtilValidate.isEmpty(context.get("userLogin"))) {
            if (system) {
                Delegator delegator = dctx.getDelegator();
                GenericValue systemUser = delegator.findByPrimaryKeyCache("UserLogin", UtilMisc.toMap("userLoginId", "system"));
                context.put("userLogin", systemUser);
            } else {
                context.put("userLogin", context.get("userLogin"));
            }
        }
        if (UtilValidate.isEmpty(context.get("locale"))) {
            context.put("locale", context.get("locale"));
        }
        if (UtilValidate.isEmpty(context.get("timeZone"))) {
            context.put("timeZone", context.get("timeZone"));
        }
        return context;
    }

    /**
     * Fetches a <code>List</code> of Enumerations by enumTypeId from the cache.
     * @param enumTypeId the type of enumeration to fetch
     * @param delegator a <code>Delegator</code> value
     * @return the <code>List</code> of enumeration <code>GenericValue</code>
     * @exception GenericEntityException if an error occurs
     */
    public static List<GenericValue> getEnumerations(String enumTypeId, Delegator delegator) throws GenericEntityException {
        return delegator.findByAndCache("Enumeration", UtilMisc.toMap("enumTypeId", enumTypeId), UtilMisc.toList("sequenceId"));
    }

    /**
     * Fetches a localized Enumeration description from the cache.
     * @param enumId the Enumeration identifier
     * @param locale the <code>Locale</code> to use for translating the description
     * @param delegator a <code>Delegator</code> value
     * @return the localized Enumeration description, or empty an <code>String</code> if the enumeration was not found
     * @exception GenericEntityException if an error occurs
     */
    public static String getEnumerationDescription(String enumId, Locale locale, Delegator delegator) throws GenericEntityException {
        GenericValue e = delegator.findByPrimaryKeyCache("Enumeration", UtilMisc.toMap("enumId", enumId));
        if (e == null) {
            Debug.logWarning("Cannot find Enumeration with ID [" + enumId + "]", MODULE);
            return "";
        }
        return (String) e.get("description", locale);
    }

    /**
     * Fetches a localized Geo name from the cache.
     * @param geoId the Geo identifier
     * @param locale the <code>Locale</code> to use for translating the description
     * @param delegator a <code>Delegator</code> value
     * @return the localized Geo name, or empty an <code>String</code> if the enumeration was not found
     * @exception GenericEntityException if an error occurs
     */
    public static String getGeoName(String geoId, Locale locale, Delegator delegator) throws GenericEntityException {
        GenericValue e = delegator.findByPrimaryKeyCache("Geo", UtilMisc.toMap("geoId", geoId));
        if (e == null) {
            Debug.logWarning("Cannot find Geo with ID [" + geoId + "]", MODULE);
            return "";
        }
        return (String) e.get("geoName", locale);
    }

    /**
     * Fetches a Geo code from the cache.
     * @param geoId the Geo identifier
     * @param locale the <code>Locale</code>, unused at the moment
     * @param delegator a <code>Delegator</code> value
     * @return the Geo code, or empty an <code>String</code> if the enumeration was not found
     * @exception GenericEntityException if an error occurs
     */
    public static String getGeoCode(String geoId, Locale locale, Delegator delegator) throws GenericEntityException {
        GenericValue e = delegator.findByPrimaryKeyCache("Geo", UtilMisc.toMap("geoId", geoId));
        if (e == null) {
            Debug.logWarning("Cannot find Geo with ID [" + geoId + "]", MODULE);
            return "";
        }
        return (String) e.get("geoCode");
    }

    /**
     * Parse a comma-delimited string of email addresses and validate each.
     * @param emailAddressString comma-delimited string of email addresses
     * @return <code>Set</code> of valid email addresses
     */
    public static Set<String> getValidEmailAddressesFromString(String emailAddressString) {
        return getValidEmailAddressesFromString(emailAddressString, false);
    }

    /**
     * Parse a comma-delimited string of email addresses and validate each.
     * @param emailAddressString comma-delimited string of email addresses
     * @param requireDot if a dot is required in the email address to consider it valid
     * @return <code>Set</code> of valid email addresses
     * @deprecated <code>UtilValidate</code> removed requireDot, so this parameter is now ignored
     */
    @Deprecated public static Set<String> getValidEmailAddressesFromString(String emailAddressString, boolean requireDot) {
        Set<String> emailAddresses = new TreeSet<String>();
        if (UtilValidate.isNotEmpty(emailAddressString)) {
            String[] emails = emailAddressString.split(",");
            for (int x = 0; x < emails.length; x++) {
                if (!UtilValidate.isEmail(emails[x])) {
                    Debug.logInfo("Ignoring invalid email address: " + emails[x], MODULE);
                    continue;
                }
                emailAddresses.add(UtilValidate.stripWhitespace(emails[x]));
            }
        }
        return emailAddresses;
    }

    /**
     * Gets the UserLogin <code>GenericValue</code> from the request.
     * @param request a <code>HttpServletRequest</code> value
     * @return a <code>GenericValue</code> value
     */
    public static GenericValue getUserLogin(HttpServletRequest request) {
        HttpSession session = request.getSession();
        return (GenericValue) session.getAttribute("userLogin");
    }

    /**
     * Gets a <code>UserLoginViewPreference</code> value from the request.
     * @param request a <code>HttpServletRequest</code> value
     * @param applicationName the application to get the preference for
     * @param screenName the screen to get the preference for
     * @param option the option to get the value for
     * @return the option value, or <code>null</code> if not found
     * @throws GenericEntityException if an error occurs
     */
    public static String getUserLoginViewPreference(HttpServletRequest request, String applicationName, String screenName, String option) throws GenericEntityException {
        GenericValue userLogin = getUserLogin(request);
        Delegator delegator = (Delegator) request.getAttribute("delegator");
        GenericValue pref = delegator.findByPrimaryKeyCache("UserLoginViewPreference", UtilMisc.toMap("userLoginId", userLogin.get("userLoginId"), "applicationName", applicationName, "screenName", screenName, "preferenceName", option));
        if (pref == null) {
            return null;
        }
        return pref.getString("preferenceValue");
    }

    /**
     * Sets a <code>UserLoginViewPreference</code> value.
     * @param request a <code>HttpServletRequest</code> value
     * @param applicationName the application to set the preference for
     * @param screenName the screen to set the preference for
     * @param option the option to set the value for
     * @param value the option value to set
     * @throws GenericEntityException if an error occurs
     */
    public static void setUserLoginViewPreference(HttpServletRequest request, String applicationName, String screenName, String option, String value) throws GenericEntityException {
        GenericValue userLogin = getUserLogin(request);
        Delegator delegator = (Delegator) request.getAttribute("delegator");
        GenericValue pref = delegator.findByPrimaryKey("UserLoginViewPreference", UtilMisc.toMap("userLoginId", userLogin.get("userLoginId"), "applicationName", applicationName, "screenName", screenName, "preferenceName", option));
        if (pref == null) {
            pref = delegator.makeValue("UserLoginViewPreference", UtilMisc.toMap("userLoginId", userLogin.get("userLoginId"), "applicationName", applicationName, "screenName", screenName, "preferenceName", option));
        }
        pref.set("preferenceValue", value);
        delegator.createOrStore(pref);
    }

    /**
     * put a GWT Script into context.  scriptLocation here is the location of a GWT script, such as
     * "commongwt/org.opentaps.gwt.common.asterisk.asterisk" for the asterisk script in the commongwt webapp/ in
     * opentaps/opentaps-common.  This is used in lieu of
     * <set field="gwtScripts[]" value="commongwt/org.opentaps.gwt.common.asterisk.asterisk" global="true"/> in
     * a screen XML definition.
     * @param context a <code>Map</code> value
     * @param scriptLocation a <code>String</code> value
     */
    @SuppressWarnings("unchecked")
    public static void addGwtScript(Map context, String scriptLocation) {
        List<String> gwtScripts = (List<String>) context.get("gwtScripts");
        if (gwtScripts == null) {
            gwtScripts = UtilMisc.toList(scriptLocation);
            context.put("gwtScripts", gwtScripts);
        } else {
            gwtScripts.add(scriptLocation);
        }
    }

    /**
     * Get the shortcuts available in the given context.
     * Since multiple shortcuts can be applied to different actions according to the context, only the most specific
     *  actions applicable for the given context are returned. There are no two entities returned with the same "shortcut".
     * @param userLogin the current user
     * @param applicationName the current application name (eg: crmsfa, financials, ...)
     * @param screenName the screen name, which is from the URI
     * @param delegator a <code>Delegator</code> value
     * @return a <code>List</code> of <code>KeyboardShortcut</code> entities that should be activated
     * @exception GenericEntityException if an error occurs
     */
    public static List<GenericValue> getKeyboardShortcuts(GenericValue userLogin, String applicationName, String screenName, Delegator delegator) throws GenericEntityException {
        String userLoginId = null;
        if (userLogin == null) {
            return new ArrayList<GenericValue>();
        }
        userLoginId = userLogin.getString("userLoginId");

        List<GenericValue> shortcuts = delegator.findByAnd("KeyboardShortcutAndHandler",
         UtilMisc.toList(
                EntityCondition.makeCondition(EntityOperator.OR,
                    EntityCondition.makeCondition("userLoginId", EntityOperator.EQUALS, userLoginId) , EntityCondition.makeCondition("userLoginId", EntityOperator.EQUALS, null)),
                EntityCondition.makeCondition(EntityOperator.OR,
                    EntityCondition.makeCondition("applicationName", applicationName), EntityCondition.makeCondition("applicationName", null)),
                EntityCondition.makeCondition(EntityOperator.OR,
                    EntityCondition.makeCondition("screenName", screenName), EntityCondition.makeCondition("screenName", null))
         ), UtilMisc.toList("shortcut", "userLoginId", "screenName", "applicationName")); // nulls are returned last

        // remove global shortcuts masked / overridden by more specific ones
        Iterator<GenericValue> it = shortcuts.iterator();
        String lastShortcut = null;
        while (it.hasNext()) {
            GenericValue s = it.next();
            if (lastShortcut != null && lastShortcut.equalsIgnoreCase(s.getString("shortcut"))) {
                Debug.logWarning("Ignored masked shortcut: [" + lastShortcut + "] with action [" + s.getString("actionTypeId") + " -> " + s.getString("actionTarget") + "] in screen " + applicationName + "/" + screenName + ", for user [" + userLogin.getString("userLoginId") + "]", MODULE);
                it.remove();
            }
            lastShortcut = s.getString("shortcut");
        }

        return shortcuts;
    }

    /**
     * Get the shortcuts available for Login page.
     * @param applicationName the current application name (eg: crmsfa, financials, ...)
     * @param screenName the screen name, which is from the URI
     * @param delegator a <code>Delegator</code> value
     * @return a <code>List</code> of <code>KeyboardShortcut</code> entities that should be activated
     * @exception GenericEntityException if an error occurs
     */
    public static List<GenericValue> getKeyboardShortcutsForLoginPage(String applicationName, String screenName, Delegator delegator) throws GenericEntityException {

        List<GenericValue> shortcuts = delegator.findByAnd("KeyboardShortcutAndHandler", UtilMisc.toList(
                EntityCondition.makeCondition("userLoginId", EntityOperator.EQUALS, null),
                EntityCondition.makeCondition(EntityOperator.OR,
                      EntityCondition.makeCondition("applicationName", EntityOperator.EQUALS, applicationName),
                      EntityCondition.makeCondition("applicationName", EntityOperator.EQUALS, null)),
                EntityCondition.makeCondition(EntityOperator.OR,
                      EntityCondition.makeCondition("screenName", EntityOperator.EQUALS, screenName),
                      EntityCondition.makeCondition("screenName", EntityOperator.EQUALS, null))
        ), UtilMisc.toList("shortcut", "userLoginId", "screenName", "applicationName")); // nulls are returned last

        // remove global shortcuts masked / overridden by more specific ones
        Iterator<GenericValue> it = shortcuts.iterator();
        String lastShortcut = null;
        while (it.hasNext()) {
            GenericValue s = it.next();
            if (lastShortcut != null && lastShortcut.equalsIgnoreCase(s.getString("shortcut"))) {
                Debug.logWarning("Ignored masked shortcut: [" + lastShortcut + "] with action [" + s.getString("actionTypeId") + " -> " + s.getString("actionTarget") + "] in screen " + applicationName + "/" + screenName, MODULE);
                it.remove();
            }
            lastShortcut = s.getString("shortcut");
        }

        return shortcuts;
    }

    /**
     * Assuming theMap not null; if null will throw a NullPointerException.
     * @param <K> the key type
     * @param theMap the <code>Map<K, BigDecimal></code> where to add the value
     * @param mapKey the key in the map where to add the value
     * @param addNumber the value to add in the map (can be null)
     * @return the new value for the given key in the map, after the value is added
     */
    public static <K> BigDecimal addInMapOfBigDecimal(Map<K, BigDecimal> theMap, K mapKey, BigDecimal addNumber) {
        BigDecimal currentNumber = theMap.get(mapKey);
        if (currentNumber == null) {
            currentNumber = BigDecimal.ZERO;
        }

        if (addNumber == null || addNumber.signum() == 0) {
            return currentNumber;
        }
        currentNumber = currentNumber.add(addNumber);
        theMap.put(mapKey, currentNumber);
        return currentNumber;
    }

    /**
     * Gets a <code>ByteWrapper</code> object for the given parameters.
     * @param delegator a <code>Delegator</code> value
     * @param dataResourceId a <code>String</code> value
     * @param https a <code>String</code> value
     * @param webSiteId a <code>String</code> value
     * @param locale a <code>Locale</code> value
     * @param rootDir a <code>String</code> value
     * @return the <code>ByteWrapper</code>
     * @exception IOException if an error occurs
     * @exception GeneralException if an error occurs
     * @deprecated for upgrade ofbiz to new version only, refactor the code later, ofbiz no longer uses ByteWrapper, instead use byte[] directly.
     */
    public static ByteWrapper getContentAsByteWrapper(Delegator delegator, String dataResourceId, String https, String webSiteId, Locale locale, String rootDir) throws IOException, GeneralException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataResourceWorker.streamDataResource(baos, delegator, dataResourceId, https, webSiteId, locale, rootDir);
        ByteWrapper byteWrapper = new ByteWrapper(baos.toByteArray());
        return byteWrapper;
    }

    /**
     * Get a <code>Locale</code> from the <code>HttpServletRequest</code>, or if not set get the default <code>Locale</code>.
     *
     * @param request a <code>HttpServletRequest</code> value
     * @return a <code>Locale</code> value
     */
    public static Locale getLocale(HttpServletRequest request) {
        return UtilHttp.getLocale(request);
    }

    /**
     * Get a <code>Locale</code> from a context <code>Map</code>, or if not set get the default <code>Locale</code>.
     *
     * @param context a context <code>Map</code> value
     * @return a <code>Locale</code> value
     */
    public static Locale getLocale(Map<String, ?> context) {
        return UtilMisc.ensureLocale(context.get("locale"));
    }

    /**
     * Get a <code>TimeZone</code> from the <code>HttpServletRequest</code>, or if not set return and set the default <code>TimeZone</code>.
     *
     * @param request a <code>HttpServletRequest</code> value
     * @return a <code>TimeZone</code> value
     */
    public static TimeZone getTimeZone(HttpServletRequest request) {
        TimeZone tz = UtilHttp.getTimeZone(request);
        if (tz == null) {
            tz = TimeZone.getDefault();
            UtilHttp.setTimeZone(request.getSession(), tz);
        }
        return tz;
    }

    /**
     * Get a <code>TimeZone</code> from a context <code>Map</code>, or if not set return the default <code>TimeZone</code>.
     *
     * @param context a context <code>Map</code> value
     * @return a <code>TimeZone</code> value
     */
    public static TimeZone getTimeZone(Map<String, ?> context) {
        TimeZone tz = (TimeZone) context.get("timeZone");
        if (tz == null) {
            tz = TimeZone.getDefault();
        }
        return tz;
    }

    /**
     * Checks if a service response is a success.
     * @param results a service response <code>Map</code>
     * @return a <code>boolean</code> value
     */
    public static boolean isSuccess(Map<String, Object> results) {
        if (results == null || results.get(ModelService.RESPONSE_MESSAGE) == null) {
            return false;
        }
        return ModelService.RESPOND_SUCCESS.equals(results.get(ModelService.RESPONSE_MESSAGE));
    }

    /**
     * Format string for javascript.
     * @param string a <code>String</code> value
     * @return a <code>String</code> value
     */
    public static String toJsString(String string) {
        if (UtilValidate.isEmail(string)) {
            return string;
        }
        return string.replaceAll("'", "&apos;");
    }

    /**
     * Gets the absolute path to the given file according to the servlet context.
     * Files are in /runtime/output/ and we get there from the servlet context path.
     * @param servletContext a <code>ServletContext</code> value
     * @param filename a <code>String</code> value
     * @return the absolute path
     */
    public static String getAbsoluteFilePath(ServletContext servletContext, final String filename) {
        String rootPath;

        // JBoss is a special case as the directory structure is a bit different than with the embedded tomcat server
        if (servletContext.getServerInfo().toLowerCase().contains("jboss")) {
            rootPath = servletContext.getRealPath("../");
        } else {
            rootPath = servletContext.getRealPath("../../../../");
        }

        String filePath = "/runtime/output/";
        return rootPath + filePath + filename;
    }

    /**
     * Gets the absolute path to the given file according to the servlet context.
     * Files are in /runtime/output/ and we get there from the servlet context path.
     * @param request a <code>HttpServletRequest</code> value
     * @param filename a <code>String</code> value
     * @return the absolute path
     */
    public static String getAbsoluteFilePath(HttpServletRequest request, final String filename) {
        ServletContext servletContext = request.getSession().getServletContext();
        return getAbsoluteFilePath(servletContext, filename);
    }

    /**
     * Split string to Vector.
     * @param ids a <code>String</code> value
     * @return a <code>Vector</code> value
     */
    public static Vector<String> stringToVector(String ids) {
        Vector<String> vector = new Vector<String>();
        List<String> idList = StringUtil.split(ids, ",");
        for (String id : idList) {
            vector.add(id);
        }
        return vector;
    }

    /**
     * Checks if the given field name is a custom field.
     * This is used in some places to identify parameters that should be treated as custom fields, so that the code
     * can adapt automatically instead of having to change the code when new fields are introduced.
     * @param fieldName a <code>String</code> value
     * @return a <code>Boolean</code> value
     */
    public static Boolean isCustomEntityField(String fieldName) {
        return fieldName.startsWith("cust_");
    }

    public static Map<String, Object> getCustomFieldsFromServiceMap(ModelEntity model, Map<String, Object> customFieldsMap, String parameterNameSuffix, Delegator delegator) {
        Map<String, Object> out = new HashMap<String, Object>();
        if (customFieldsMap != null) {
            for (String n : model.getAllFieldNames()) {
                String custKey = n;
                if (UtilValidate.isNotEmpty(parameterNameSuffix)) {
                    custKey = custKey + parameterNameSuffix;
                }
                custKey = custKey.substring(5); // 5 is length of "cust_"
                if (customFieldsMap.containsKey(custKey)) {
                    Object value = customFieldsMap.get(custKey);
                    out.put(n, model.convertFieldValue(n, value, delegator));
                }
            }
        }
        return out;
    }

    public static void setCustomFieldsFromServiceMap(GenericValue entity, Map<String, Object> customFieldsMap, String parameterNameSuffix, Delegator delegator) {
        ModelEntity model = entity.getModelEntity();
        if (customFieldsMap != null) {
            for (String n : model.getAllFieldNames()) {
                String custKey = n;
                if (UtilValidate.isNotEmpty(parameterNameSuffix)) {
                    custKey = custKey + parameterNameSuffix;
                }
                custKey = custKey.substring(5); // 5 is length of "cust_"
                if (customFieldsMap.containsKey(custKey)) {
                    Object value = customFieldsMap.get(custKey);
                    entity.set(n, model.convertFieldValue(n, value, delegator));
                }
            }
        }
    }

    public static void setCustomFieldsFromServiceMap(EntityInterface entity, Map<String, Object> customFieldsMap, String parameterNameSuffix, Delegator delegator) {
        ModelEntity model = delegator.getModelEntity(entity.getBaseEntityName());
        if (customFieldsMap != null) {
            for (String n : model.getAllFieldNames()) {
                String custKey = n;
                if (UtilValidate.isNotEmpty(parameterNameSuffix)) {
                    custKey = custKey + parameterNameSuffix;
                }
                custKey = custKey.substring(5); // 5 is length of "cust_"
                if (customFieldsMap.containsKey(custKey)) {
                    Object value = customFieldsMap.get(custKey);
                    entity.set(n, model.convertFieldValue(n, value, delegator));
                }
            }
        }
    }

    /**
     * Returns full email address containing a domain address and personal name for
     * the specified contact mech identifier.
     * @param contactMechId A contact mechanism identifier.
     * @param delegator An instance of the <code>Delegator</code>.
     * @return
     * @throws GenericEntityException
     */
    public static String emailAndPersonalName(String contactMechId, Delegator delegator) throws GenericEntityException {
        if (UtilValidate.isEmail(contactMechId)) {
            throw new IllegalArgumentException();
        }

        GenericValue contactMech = delegator.findByPrimaryKey("ContactMech", UtilMisc.toMap("contactMechId", contactMechId));
        if (contactMech == null) {
            Debug.logWarning(String.format("There is no ContactMech entity w/ identifier [%1$s]", contactMechId), MODULE);
    	    return null;
        }

        String emailAddr = contactMech.getString("infoString");

        List<GenericValue> validPartyContactMechs =
        delegator.findByCondition("PartyContactMech", 
                    EntityCondition.makeCondition(
                        UtilMisc.toList(EntityUtil.getFilterByDateExpr(), EntityCondition.makeCondition("contactMechId", contactMechId))
                    ),
                    UtilMisc.toList("partyId", "contactMechId"), null);

        String partyId = EntityUtil.getFirst(validPartyContactMechs).getString("partyId");

        // we will be careful and return the address only if multiple relations between
        // party and contact exist.
        if (validPartyContactMechs == null || validPartyContactMechs.size() != 1 || UtilValidate.isEmail(partyId)) {
            return emailAddr;
        }

    	return emailAndPersonalName(emailAddr, partyId, delegator);
    }

    /**
     * Returns full email address containing a domain address and personal name for
     * the specified contact address and party identifier.
     * @param email An email address in format user@host 
     * @param partyId A party identifier
     * @param delegator An instance of the <code>Delegator</code>.
     * @return
     */
    public static String emailAndPersonalName(String email, String partyId, Delegator delegator) {
        return String.format("%1$s <%2$s>", org.ofbiz.party.party.PartyHelper.getPartyName(delegator, partyId, false), email);
    }
    /*
     * It's for fixing URL in view history so that after browser restart special characters such as & are not changed into &#63;
     * and re-arrange parameters in the url 
     */
    public static String getCorrectUrlFromEncodeUrl (String url) {
        String prevPath = url.substring(0, url.lastIndexOf("/") + 1);
        String contextUrl = url.substring(url.lastIndexOf("/") + 1);
        // if we cannot found ? in url, that meaning it was a wrong url 
        if (contextUrl.indexOf("&") >= 0 && contextUrl.indexOf("?") < 0) {
            Pattern p = Pattern.compile(SESSION_ID_PARTTER); 
            Matcher m = p.matcher(contextUrl);
            String sessionId = null;
            if (m.find()) {
                sessionId = m.group();
                contextUrl = contextUrl.replace(sessionId, "");
            }
            // replace session id parttern
            contextUrl = contextUrl.replace("&#63;", "&");
            contextUrl = contextUrl.replace("&#61;", "=");
            String[] parameters = contextUrl.split(PARAMETER_SEPARATE_CHAR_PARTTER);
            contextUrl = parameters[0];
            if (sessionId != null){
                contextUrl += sessionId;
            }
            if (parameters.length > 1) {
                contextUrl += "?" + parameters[1];
            }
            for (int i=2; i < parameters.length; i++) {
                contextUrl += "&" + parameters[i];
            }
            return prevPath + contextUrl;
        } else {
            return url;
        }
        
    }
}
