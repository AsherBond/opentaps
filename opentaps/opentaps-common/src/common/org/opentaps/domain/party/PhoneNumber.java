package org.opentaps.domain.party;

import org.opentaps.base.entities.TelecomNumber;

/**
 * Phone number object.
 * TODO does it make sense to put it in party domain?  Facility can have a phone number too. 
 */
public class PhoneNumber extends TelecomNumber {

    private final static String formatErrorMsg = "Cannot create a Phone Number from [%s].  It must have at least an area code and a 7 digit phone number.";

    /**
     * Create a PhoneNumber from a string containing a US style phone number.  The
     * format can be arbitrary, as long as the area code and 7 digit phone number
     * are present. Extensions are appended to the 7 digit phone number field.
     */
    public PhoneNumber(String number) {
        if (number == null) throw new IllegalArgumentException("Phone Number cannot be null.");

        // first strip out all the non digits
        String n = number.replaceAll("\\D", "");
        if (n.length() == 0) throw new IllegalArgumentException(String.format(formatErrorMsg, number));

        // strip out the country code -- we rely on the fact that no area codes begin with 1
        if (n.charAt(0) == '1') n = n.substring(1, n.length());

        // make sure we have enough digits
        if (n.length() < 10) {
            throw new IllegalArgumentException(String.format(formatErrorMsg, number));
        }

        // the first 3 digits are area code, the next 7 are local phone, and any remaining are extension
        setCountryCode("1");
        setAreaCode(n.substring(0,3));
        String phone = n.substring(3,6) + "-" + n.substring(6,10);
        if (n.length() > 10) {
            phone += " x" + n.substring(10, n.length());
        }
        setContactNumber(phone);
    }

}
