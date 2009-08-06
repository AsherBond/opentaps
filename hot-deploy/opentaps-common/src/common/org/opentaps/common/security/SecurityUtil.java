package org.opentaps.common.security;

import org.ofbiz.base.util.StringUtil;
import org.ofbiz.base.util.UtilValidate;

/**
 * Utility methods for security and encryption
 *
 * @author     <a href="mailto:cliberty@opensourcestrategies.com">Chris Liberty</a>
 * @version    $Rev$
 */
public class SecurityUtil {

    /**
     * A utility method to convert an MD5 or SHA1 hash such as those produced by PHP's md5() and sha1() functions to the same
     *  double-encoded hash as produced by org.ofbiz.base.crypto.HashCrypt.getDigestHash().
     * 
     * @param hash the hex-encoded hash
     * @param hashType MD5 or SHA
     * @return  the OFBiz-style hash
     */
    public static String convertHash(String hash, String hashType) {
        if (UtilValidate.isEmpty(hash) || UtilValidate.isEmpty(hashType)) return null;
        
        byte[] inBytes = StringUtil.fromHexString(hash);
        
        // Perform the same transformation that happens in HashCrypt.getDigestHash()
        int k = 0;
        char digestChars[] = new char[inBytes.length * 2];
        for (int l = 0; l < inBytes.length; l++) {
            int i1 = inBytes[l];
            if (i1 < 0) i1 = 127 + i1 * -1;
            StringUtil.encodeInt(i1, k, digestChars);
            k += 2;
        }
        
        return new String(digestChars, 0, digestChars.length);
    }    
}
