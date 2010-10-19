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
package com.opensourcestrategies.crmsfa.marketing;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javolution.util.FastMap;

import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.entity.Delegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.util.EntityUtil;
import org.ofbiz.entity.condition.EntityCondition;
import org.ofbiz.entity.condition.EntityOperator;
import org.opentaps.common.util.UtilConfig;

/**
 * Marketing Helper methods which are designed to provide a consistent set of APIs that can be reused by
 * higher level services.
 */
public final class MarketingHelper {

    private MarketingHelper() { }

    /** Counts the number of active, ACCEPTED members in a contact list with the given condition. */
    public static long countContactListMembers(String contactListId, EntityCondition condition, Delegator delegator) throws GenericEntityException {
        List<EntityCondition> conditionList = UtilMisc.toList(
                    EntityCondition.makeCondition("contactListId", EntityOperator.EQUALS, contactListId),
                    EntityCondition.makeCondition("statusId", EntityOperator.EQUALS, "CLPT_ACCEPTED"),
                    EntityUtil.getFilterByDateExpr(),
                    EntityUtil.getFilterByDateExpr("contactMechFromDate", "contactMechThruDate")
                    );
        if (condition != null) {
            conditionList.add(condition);
        }
        EntityCondition conditions = EntityCondition.makeCondition(conditionList, EntityOperator.AND);
        GenericValue value = EntityUtil.getFirst(delegator.findByCondition("ContactListPartyCount", conditions, UtilMisc.toList("partyId"), null));
        if (value == null || value.get("partyId") == null) {
            return 0;
        }
        return value.getLong("partyId").intValue();
    }

    /** Count all active, ACCEPTED members of a contact list. */
    public static long countContactListMembers(String contactListId, Delegator delegator) throws GenericEntityException {
        return countContactListMembers(contactListId, null, delegator);
    }

    /** Count all active, ACCEPTED members of a contact list in a given countryGeoId. */
    public static long countContactListMembersByCountry(String contactListId, String countryGeoId, Delegator delegator) throws GenericEntityException {
        EntityCondition condition = EntityCondition.makeCondition("countryGeoId", EntityOperator.EQUALS, countryGeoId);
        return countContactListMembers(contactListId, condition, delegator);
    }

    /** Cound all active, ACCEPTED members of a contact list in the configured defaultCountryGeoId */
    public static long countContactListMembersDomestic(String contactListId, Delegator delegator) throws GenericEntityException {
        String defaultCountryGeoId = UtilConfig.getPropertyValue("crmsfa", "defaultCountryGeoId");
        return countContactListMembersByCountry(contactListId, defaultCountryGeoId, delegator);
    }

    /** Builds a map of zip3 -> bmcCode based on values in UspsZipToBmcCode */
    public static Map<String, String> getZipToBMCCode(Delegator delegator) throws GenericEntityException {
        Map<String, String> results = FastMap.newInstance();
        List<GenericValue> values = delegator.findAllCache("UspsZipToBmcCode");
        for (Iterator<GenericValue> iter = values.iterator(); iter.hasNext();) {
            GenericValue value = iter.next();
            String bmcCode = value.getString("bmcCode");
            if (bmcCode == null || bmcCode.trim().length() == 0) {
                continue;
            }
            results.put(value.getString("zip3"), bmcCode);
        }
        return results;
    }

    /**
     * Given a 3 digit zip code, determine what the BMC code is.
     * This is based on Zip 3 to USPS BMC table as of 2006-12-13.
     */
    public static String findBMCCode(String zip3String, Map<String, String> overrideMap) {
        String bmcOverride = overrideMap.get(zip3String);
        if (bmcOverride != null) {
            return bmcOverride;
        }

        int zip3 = Integer.parseInt(zip3String);

        // check zip codes that are in a range

        if (zip3 >= 68 && zip3 <= 79 || zip3 >= 85 && zip3 <= 119 || zip3 >= 124 && zip3 <= 127) {
            return "BMC_NJ_00102";
        }

        // TODO: if mailing from 6-9, destination is SCF_PR_006 (see footnote 1 in pdf)
        if (zip3 >= 6 && zip3 <= 9 || zip3 >= 313 && zip3 <= 316 || zip3 >= 320 && zip3 <= 342) {
            return "BMC_FL_32099";
        }

        if (zip3 >= 10 && zip3 <= 67 || zip3 >= 120 && zip3 <= 123) {
            return "BMC_MA_05500";
        }

        if (zip3 >= 80 && zip3 <= 84 || zip3 >= 137 && zip3 <= 139 || zip3 >= 169 && zip3 <= 199) {
            return "BMC_PA_19205";
        }

        if (zip3 >= 130 && zip3 <= 136 || zip3 >= 140 && zip3 <= 168 || zip3 >= 260 && zip3 <= 266 || zip3 >= 439 && zip3 <= 447) {
            return "BMC_PA_15195";
        }

        if (zip3 >= 200 && zip3 <= 212 || zip3 >= 214 && zip3 <= 239) {
            return "BMC_DC_20499";
        }

        if (zip3 >= 240 && zip3 <= 243 || zip3 >= 245 && zip3 <= 249 || zip3 >= 270 && zip3 <= 297) {
            return "BMC_NC_27075";
        }

        if (zip3 >= 250 && zip3 <= 253 || zip3 >= 255 && zip3 <= 259 || zip3 >= 400 && zip3 <= 418 ||
            zip3 >= 425 && zip3 <= 427 || zip3 >= 403 && zip3 <= 433 || zip3 >= 448 && zip3 <= 462 || zip3 >= 469 && zip3 <= 474) {
            return "BMC_OH_45900";
        }

        if (zip3 >= 300 && zip3 <= 312 || zip3 >= 317 && zip3 <= 319 || zip3 >= 350 && zip3 <= 352 || zip3 >= 354 && zip3 <= 368 || zip3 >= 377 && zip3 <= 379) {
            return "BMC_GA_31195";
        }

        if (zip3 >= 369 && zip3 <= 372 || zip3 >= 380 && zip3 <= 397 || zip3 >= 703 && zip3 <= 705 || zip3 >= 719 && zip3 <= 729) {
            return "BMC_TN_38999";
        }

        if (zip3 >= 475 && zip3 <= 479 || zip3 >= 614 && zip3 <= 620 || zip3 >= 622 && zip3 <= 631 || zip3 >= 633 && zip3 <= 639) {
            return "BMC_MO_63299";
        }

        if (zip3 >= 434 && zip3 <= 436 || zip3 >= 465 && zip3 <= 468 || zip3 >= 480 && zip3 <= 497) {
            return "BMC_MI_48399";
        }

        if (zip3 >= 530 && zip3 <= 532 || zip3 >= 537 && zip3 <= 539 || zip3 >= 600 && zip3 <= 611) {
            return "BMC_IL_60808";
        }

        if (zip3 >= 540 && zip3 <= 551 || zip3 >= 553 && zip3 <= 567 || zip3 >= 580 && zip3 <= 588) {
            return "BMC_MN_55202";
        }

        if (zip3 >= 500 && zip3 <= 516 || zip3 >= 520 && zip3 <= 528 || zip3 >= 570 && zip3 <= 577 || zip3 >= 683 && zip3 <= 689) {
            return "BMC_IA_50999";
        }

        if (zip3 >= 590 && zip3 <= 599 || zip3 >= 690 && zip3 <= 693 || zip3 >= 800 && zip3 <= 816 ||
            zip3 >= 820 && zip3 <= 834 || zip3 >= 840 && zip3 <= 847 || zip3 >= 870 && zip3 <= 875 || zip3 >= 877 && zip3 <= 884) {
            return "BMC_CO_80088";
        }

        if (zip3 >= 641 && zip3 <= 658 || zip3 >= 660 && zip3 <= 662 || zip3 >= 664 && zip3 <= 679) {
            return "BMC_KS_64399";
        }

        if (zip3 >= 710 && zip3 <= 712 || zip3 >= 733 && zip3 <= 738 || zip3 >= 743 && zip3 <= 799) {
            return "BMC_TX_75199";
        }

        if (zip3 >= 970 && zip3 <= 978 || zip3 >= 980 && zip3 <= 986 || zip3 >= 988 && zip3 <= 999) {
            return "BMC_WA_98000";
        }

        if (zip3 >= 889 && zip3 <= 891 || zip3 >= 900 && zip3 <= 908 || zip3 >= 910 && zip3 <= 928 || zip3 >= 930 && zip3 <= 935) {
            return "BMC_CA_90901";
        }

        if (zip3 >= 936 && zip3 <= 969) {
            return "BMC_CA_94850";
        }

        // check isolated ZIP codes that aren't in a range
        switch (zip3) {
            case 5:
                return "BMC_NJ_00102";
            case 299: case 344: case 346: case 347: case 349:
                return "BMC_FL_32099";
            case 128: case 129:
                return "BMC_MA_05500";
            case 244: case 254: case 267: case 268:
                return "BMC_DC_20499";
            case 376:
                return "BMC_NC_27075";
            case 421: case 422: case 437: case 438:
                return "BMC_OH_45900";
            case 298: case 373: case 374: case 398: case 399:
                return "BMC_GA_31195";
            case 375: case 700: case 701: case 707: case 708: case 713: case 714: case 716: case 717:
                return "BMC_TN_38999";
            case 420: case 423: case 424:
                return "BMC_MO_63299";
            case 463: case 464: case 534: case 535: case 613:
                return "BMC_IL_60808";
            case 498: case 499:
                return "BMC_MN_55202";
            case 612: case 680: case 681:
                return "BMC_IA_50999";
            case 836: case 837: case 856: case 857: case 865: case 979:
            case 850: case 852: case 853: case 855: // TODO: if the origin zip is in a certain set, these actually go to BMC_CA_90901 (see footnote 2 in pdf)
                return "BMC_CO_80088";
            case 640: case 641: case 739:
                return "BMC_KS_64399";
            case 706: case 718: case 730: case 731: case 741: case 885:
                return "BMC_TX_75199";
            case 835: case 838:
                return "BMC_WA_98000";
            case 893:
                return "BMC_CA_90901";
            case 894: case 895: case 897:
                return "BMC_CA_94850";
        }

        // nothing found, so return null
        return null;
    }

}
