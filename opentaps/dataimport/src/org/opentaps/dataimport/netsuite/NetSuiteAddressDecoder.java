package org.opentaps.dataimport.netsuite;

import javolution.util.FastList;
import javolution.util.FastMap;
import org.ofbiz.base.util.GeneralException;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.entity.Delegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.util.EntityUtil;
import org.ofbiz.service.LocalDispatcher;
import org.ofbiz.service.ServiceUtil;
import org.opentaps.dataimport.ImportDecoder;

import java.sql.Timestamp;
import java.util.List;
import java.util.Map;

/**
 * Import net suite addresses.
 */
class NetSuiteAddressDecoder implements ImportDecoder {

    // three minute timeout for servcies
    public static final int serviceTimeout = 180;

    protected GenericValue userLogin;

    public NetSuiteAddressDecoder(GenericValue userLogin) {
        this.userLogin = userLogin;
    }
    
    public List<GenericValue> decode(GenericValue entry, Timestamp importTimestamp, Delegator delegator, LocalDispatcher dispatcher, Object... args) throws Exception {
        List<GenericValue> toBeStored = new FastList<GenericValue>();
        String addressBookId = entry.getString("addressBookId");

        // ensure that the party is imported
        String partyId = NetSuiteCustomerDecoder.getPartyIdFromNetSuiteCustomerId(entry.getString("entityId"));
        GenericValue party = delegator.findByPrimaryKeyCache("Party", UtilMisc.toMap("partyId", partyId));
        if (party == null) {
            throw new IllegalArgumentException("Can't import address [" + addressBookId + "] because its customer [" + partyId + "] has not been imported yet.");
        }

        // create the party contact mech with initial purpose shipping address
        Map<String, Object> input = new FastMap<String, Object>();
        input.put("userLogin", userLogin);
        input.put("partyId", partyId);
        input.put("contactMechPurposeTypeId", "SHIPPING_LOCATION");

        // map the geoIds
        String countryGeoId = mapCountry(entry.getString("country"), delegator);
        String stateProvinceGeoId = mapStateProvince(countryGeoId, entry.getString("stateProvinceName"), delegator);

        // TODO: validate existance of address1, city and postalCode.  throw exception if missing

        // fill in the address (this might need to change depending on your data)
        input.put("toName", entry.get("company"));
        input.put("address1", entry.get("addressLine1"));
        input.put("address2", entry.get("addressLine2"));
        input.put("attnName", entry.get("attention"));
        input.put("city", entry.get("city"));
        input.put("postalCode", entry.get("zip"));
        input.put("countryGeoId", countryGeoId);
        input.put("stateProvinceGeoId", stateProvinceGeoId);
        Map results = dispatcher.runSync("createPartyPostalAddress", input, serviceTimeout, false);
        if (ServiceUtil.isError(results)) {
            throw new GeneralException("Failed to create party address: " + ServiceUtil.getErrorMessage(results));
        }
        String contactMechId = (String) results.get("contactMechId");

        // if it's also a billing address, make it the general correspondence, billing, and primary address
        if ("Yes".equals(entry.get("isDefaultBillAddress"))) {
            Map<String, Object> purpose = UtilMisc.toMap("partyId", partyId, "contactMechId", contactMechId, "fromDate", importTimestamp);
            for (String purposeId : new String[] {"BILLING_LOCATION", "GENERAL_LOCATION", "PRIMARY_LOCATION"}) {
                purpose.put("contactMechPurposeTypeId", purposeId);
                toBeStored.add( delegator.makeValue("PartyContactMechPurpose", purpose) );
            }
        }

        entry.put("contactMechId", contactMechId);
        toBeStored.add(entry);
        
        return toBeStored;
    }

    /**
     * Maps the country to the opentaps geoId.  As long as the country is a two letter ISO country code, it
     * should map cleanly into the opentaps Geo model.  However, some countries might be missing from the data,
     * in which case this method will throw an exception.
     */
    public static String mapCountry(String country, Delegator delegator) throws GenericEntityException {
        if (country == null) throw new IllegalArgumentException("Address has no country.  Cannot import.");
        GenericValue geo = EntityUtil.getFirst( delegator.findByAndCache("Geo", UtilMisc.toMap("geoTypeId", "COUNTRY", "geoCode", country) ) );
        if (geo == null) throw new IllegalArgumentException("Cannot find a country corresponding to ["+country+"] for this address.");
        return geo.getString("geoId");
    }

    /**
     * Maps the state/province to the opentaps geoId.  As long as the state or province is a standard abbreviated
     * code (NSW for Australia's New South Wales, etc.) then the mapping should be good.  If not, it tries the full
     * name of the province.  See the GeoAssocAndGeoTo view entity for details.
     */
    public static String mapStateProvince(String countryGeoId, String stateProvince, Delegator delegator) throws GenericEntityException {
        if (stateProvince == null) throw new IllegalArgumentException("Address has no state or province.  Cannot import.");
        GenericValue geo = EntityUtil.getFirst( delegator.findByAndCache("GeoAssocAndGeoTo", UtilMisc.toMap("geoIdFrom", countryGeoId, "geoCode", stateProvince)) );
        if (geo == null) {
            // try using geoName
            geo = EntityUtil.getFirst( delegator.findByAndCache("GeoAssocAndGeoTo", UtilMisc.toMap("geoIdFrom", countryGeoId, "geoName", stateProvince)) );
            if (geo == null) {
                throw new IllegalArgumentException("Cannot find a state or province corresponding to ["+stateProvince+"] in the country ["+countryGeoId+"] for this address.");
            }
        }
        return geo.getString("geoId");
    }
}
