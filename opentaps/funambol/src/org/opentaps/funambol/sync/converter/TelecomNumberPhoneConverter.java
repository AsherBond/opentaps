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

package org.opentaps.funambol.sync.converter;

import java.util.Map;
import java.util.Properties;

import mz.co.dbl.siga.framework.entity.EntityBeanConverter;
import mz.co.dbl.siga.framework.entity.EntityPreparer;

import org.apache.commons.lang.StringUtils;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;

/**
 * Performs custom conversion of data fields between a TelecomNumber entity and a Funambol PIM Phone bean.
 *
 * @author Cameron Smith - Database, Lda - www.database.co.mz
 */
public class TelecomNumberPhoneConverter extends EntityBeanConverter
{
    //=== constructors and initialization ===
    
    public TelecomNumberPhoneConverter(Properties mappings)
    {
        super(mappings, null, null);
    }
        
    //=== override inherited behaviour ===
    
    /**
     * Split the field in the target map into separate parts based on format
     */
    public void toMap(Object source, Map target)
    {
        //1. let the superclass do basic conversion based on its config
        super.toMap(source, target);
        
        //2. now parse out the contactNumber into different bits
        //Format: +countryCode (areaCode) contactNumber x extension
        String contactNumber = (String)target.get("contactNumber");
        
        if(contactNumber != null)
        {
            //2.1 split into tokens
            String[] contactNumberTokens = StringUtils.split(contactNumber);
            
            //2.2 parse the tokens until there is nothing more
            for(int i = 0; i < contactNumberTokens.length; i++)
            {
                i = decodeToken(contactNumberTokens, i, target);
            }            
        }
    }
    
    //=== private behaviour ===
    
    /**
     * Convert the given token to a String, based on format and position
     * 
     * @return position of last token parse
     */
    private int decodeToken(String[] tokens, int currToken, Map target)
    {
        String token = tokens[currToken];
        if(currToken == 0 && token.startsWith("+"))  //country: can only be first
        {
          target.put("countryCode", token.substring(1)); return currToken;
        }
        else if(currToken < 2 && token.startsWith("(")) //area: can only be first or second
        {
            target.put("areaCode", token.substring(1, token.length() - 1)); return currToken;
        }
        else  //number - we must now look for extension which logically is two positions after us
        { 
            if(tokens.length - currToken == 3)
            { 
                currToken += 2;  //advance the parser to skip the "x"
                String extension = tokens[currToken];

                //we must now set the extension in PartyContactMech, bizarrely, which is a related entity
                //see for explanation: https://issues.apache.org/jira/browse/OFBIZ-1332
                if(target instanceof GenericValue) //set directly
                {
                    try
                    {
                        EntityPreparer targetP = new EntityPreparer((GenericValue)target);
                        GenericValue partyContactMech = targetP.getRelated("PartyContactMech");
                        partyContactMech.set("extension", tokens[currToken]);
                        partyContactMech.store();
                    }
                    catch(GenericEntityException extensionX)  //we can't throw a checked exception here, so...
                    {
                        throw new RuntimeException("Could not set extension for " + target, extensionX);
                    }
                }
                else //set the extension into map to be picked up later
                {
                    target.put("extension", extension);
                }
            }
            target.put("contactNumber", token); 
            return currToken;
        } 
    }
}
