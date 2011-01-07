package org.opentaps.dataimport;

import javolution.util.FastList;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.entity.Delegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.util.EntityUtil;
import org.ofbiz.service.DispatchContext;
import org.ofbiz.service.LocalDispatcher;
import org.ofbiz.service.ServiceUtil;
import org.opentaps.common.util.UtilMessage;

import java.sql.Timestamp;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Services for importing tax rates.  These rely on the entities defined in entitymode_tax.xml
 */
public class TaxImportServices {

    public static final String module = TaxImportServices.class.getName();

    /**
     * This import service requires two entities:  DataImportUSCountyTaxes for the rates by county
     * and DataImportUSZipCodes for the map of zip codes and county to state.  The import is done
     * in two phases.  First, it reads the rates by county from DataImportUSCountyTaxes and makes
     * the required tax entities.  Then, it reads the DataImportUSZipCodes and creates associations
     * between the counties and the zip codes so that the order system can look up the correct tax.
     */
    public static Map importUSTaxRates(DispatchContext dctx, Map context) {
        String productStoreId = (String) context.get("productStoreId");
        int imported = 0;

        try {
            // make sure product store exists
            Map findMap = UtilMisc.toMap("productStoreId", productStoreId);
            GenericValue productStore = dctx.getDelegator().findByPrimaryKey("ProductStore", findMap);
            if (productStore == null) return UtilMessage.createServiceError("OpentapsError_ProductStoreNotFound", (Locale) context.get("Locale"), findMap);

            // import the zip codes and counties first
            OpentapsImporter zipCodeImporter = new OpentapsImporter("DataImportUSZipCodes", dctx, new ZipCodeDecoder());
            imported += zipCodeImporter.runImport();

            // then import the tax rates for the counties
            OpentapsImporter taxImporter = new OpentapsImporter("DataImportUSCountyTax", dctx, new USTaxDecoder(productStoreId));
            imported += taxImporter.runImport();
        } catch (GenericEntityException e) {
            return UtilMessage.createAndLogServiceError(e, module);
        }
        Map result = ServiceUtil.returnSuccess();
        result.put("importedRecords", imported);
        return result;
    }
}

// maps zip codes to counties in states using the ofbiz geo model (uses DataImportUSZipCode)
class ZipCodeDecoder implements ImportDecoder {
    public static final String module = ZipCodeDecoder.class.getName();

    public List<GenericValue> decode(GenericValue entry, Timestamp importTimestamp, Delegator delegator, LocalDispatcher dispatcher, Object... args) throws Exception {
        List<GenericValue> toBeStored = FastList.newInstance();

        // do some basic validation
        String zipCodeString = entry.getString("zipCode").trim();
        if (zipCodeString.length() != 5 || ! zipCodeString.matches("\\d\\d\\d\\d\\d")) {
            throw new IllegalArgumentException("Zip code ["+zipCodeString+"] is not 5 digits.  Not importing this entry.");
        }
        GenericValue state = delegator.findByPrimaryKeyCache("Geo", UtilMisc.toMap("geoId", entry.getString("stateCode")));
        if (state == null) {
            throw new IllegalArgumentException("State ["+entry.get("stateCode")+"] not found, not importing zip code ["+zipCodeString+"].  Please create this state in the Geo entity first.");
        }

        // create a Geo with geoId = "US-${zipCode}
        String zipCodeId = "USA-" + zipCodeString;
        GenericValue zipCode = delegator.makeValue("Geo");
        zipCode.put("geoId", zipCodeId);
        zipCode.put("geoTypeId", "POSTAL_CODE");
        zipCode.put("geoName", zipCodeString);
        zipCode.put("geoCode", zipCodeString);
        zipCode.put("abbreviation", zipCodeString);
        toBeStored.add(zipCode);

        // find the county if it was already created, otherwise make a new one with a sequential ID
        String countyName = entry.getString("county").toUpperCase();
        GenericValue county = EntityUtil.getFirst( delegator.findByAndCache("Geo", UtilMisc.toMap("geoName", countyName, "geoTypeId", "COUNTY")) );
        if (county == null) {
            // TODO more values after <Geo abbreviation="LA CO." geoCode="LA" geoId="10010" geoName="Los Angeles County" geoTypeId="COUNTY"/>
            String countyId = delegator.getNextSeqId("Geo");
            county = delegator.makeValue("Geo");
            county.put("geoId", countyId);
            county.put("geoName", countyName);
            county.put("geoTypeId", "COUNTY");
            toBeStored.add(county);

            // make assoc between county and state
            GenericValue countyStateAssoc = delegator.makeValue("GeoAssoc");
            countyStateAssoc.put("geoId", state.get("geoId"));
            countyStateAssoc.put("geoIdTo", countyId);
            countyStateAssoc.put("geoAssocTypeId", "REGIONS");
            toBeStored.add(countyStateAssoc);
        }

        // make assoc between zip and county
        GenericValue zipCountyAssoc = delegator.makeValue("GeoAssoc");
        zipCountyAssoc.put("geoId", county.get("geoId"));
        zipCountyAssoc.put("geoIdTo", zipCodeId);
        zipCountyAssoc.put("geoAssocTypeId", "REGIONS");
        toBeStored.add(zipCountyAssoc);

        return toBeStored;
    }
}

// maps DataImportUSCountyTaxes into a set of opentaps entities that describes the tax for each county
class USTaxDecoder implements ImportDecoder {
    public static final String module = USTaxDecoder.class.getName();
    protected String productStoreId;

    public USTaxDecoder(String productStoreId) {
        this.productStoreId = productStoreId;
    }

    // first of the custom arguments is the productStoreId
    public List<GenericValue> decode(GenericValue entry, Timestamp importTimestamp, Delegator delegator, LocalDispatcher dispatcher, Object... args) throws Exception {
        List<GenericValue> toBeStored = FastList.newInstance();

        GenericValue county = EntityUtil.getFirst( delegator.findByAndCache("Geo", UtilMisc.toMap("geoName", entry.getString("county").toUpperCase(), "geoTypeId", "COUNTY")) );
        if (county == null) {
            throw new IllegalArgumentException("Cannot import tax rate for county ["+entry.getString("county")+"] because the county hasn't been imported properly.");
        }

        // for tax party, just grab the first one defined for the state that the county is in
        String stateId = entry.getString("stateCode");
        GenericValue taxAuthority = EntityUtil.getFirst( delegator.findByAndCache("TaxAuthority", UtilMisc.toMap("taxAuthGeoId", stateId)) );
        if (taxAuthority == null) {
            throw new IllegalArgumentException("Cannot import tax rate for county ["+entry.getString("county")+"]:  Cannot find a tax authority party for state ["+stateId+"].  Please define one first.");
        }
        String taxAuthPartyId = taxAuthority.getString("taxAuthPartyId");

        // map this tax party to our county
        taxAuthority = delegator.makeValue("TaxAuthority");
        taxAuthority.put("taxAuthGeoId", county.get("geoId"));
        taxAuthority.put("taxAuthPartyId", taxAuthPartyId);
        taxAuthority.put("includeTaxInPrice", "N"); // TODO is this always N?
        toBeStored.add(taxAuthority);

        // store the tax rate for the county
        GenericValue taxRate = delegator.makeValue("TaxAuthorityRateProduct");
        taxRate.put("taxAuthorityRateSeqId", delegator.getNextSeqId("TaxAuthorityRateProduct"));
        taxRate.put("taxAuthGeoId", county.get("geoId"));
        taxRate.put("taxAuthPartyId", taxAuthPartyId);
        taxRate.put("taxAuthorityRateTypeId", "SALES_TAX");
        taxRate.put("productStoreId", productStoreId);
        taxRate.put("taxPercentage", entry.get("taxRate"));
        taxRate.put("fromDate", importTimestamp);
        taxRate.put("description", county.get("geoName") + " County Sales Tax");
        toBeStored.add(taxRate);

        // if we're inheriting exemptions, create the required association to trigger this (by default we always assume exemptions unless field is N)
        if (! "N".equals(entry.get("inheritExemptions"))) {
            GenericValue assoc = delegator.makeValue("TaxAuthorityAssoc");
            assoc.put("taxAuthGeoId", stateId);
            assoc.put("taxAuthPartyId", taxAuthPartyId);
            assoc.put("toTaxAuthGeoId", county.get("geoId"));
            assoc.put("toTaxAuthPartyId", taxAuthPartyId);
            assoc.put("taxAuthorityAssocTypeId", "EXEMPT_INHER");
            assoc.put("fromDate", importTimestamp);
            toBeStored.add(assoc);
        }

        return toBeStored;
    }
}


