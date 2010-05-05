package org.opentaps.funambol.sync.converter;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import junit.framework.TestCase;
import mz.co.dbl.siga.framework.entity.EntityBeanConverter;

import org.opentaps.funambol.common.PimPropertyEditor;
import org.springframework.beans.PropertyEditorRegistry;
import org.springframework.beans.PropertyEditorRegistrySupport;

import com.funambol.common.pim.contact.Contact;
import com.funambol.common.pim.contact.Phone;

public class TestTelecomNumberPhoneConverter extends TestCase
{
    public void testToMap() throws Exception
    {
        //0. build up a Contact with a Phone in it
        Contact contact = new Contact();
        //Format: +countryCode (areaCode) contactNumber x extension
        Phone phone = new Phone();
        phone.setPropertyType("BusinessTelephoneNumber");
        phone.setPropertyValue("+258 (21) 310166 x 2");
        contact.getBusinessDetail().getPhones().clear();
        contact.getBusinessDetail().getPhones().add(phone);
        contact.getName().getFirstName().setPropertyValue("Cameron");
        
        //1. build up a converter
        Properties conversionProps = new Properties();
        conversionProps.setProperty("contactNumber", "businessDetail.phones[propertyType=BusinessTelephoneNumber]");
        conversionProps.setProperty("askForName", "name.firstName");
        PropertyEditorRegistry propReg = new PropertyEditorRegistrySupport();
        propReg.registerCustomEditor(Class.forName("com.funambol.common.pim.common.Property"), new PimPropertyEditor());
        EntityBeanConverter converter = new TelecomNumberPhoneConverter(conversionProps);
        converter.setPropertyEditors(propReg);
        converter.setQualifier("PRIMARY_PHONE");
        
        //2. perform a full conversion and verify that the data comes out as expected
        Map target = new HashMap();
        converter.toMap(contact, target);
        assertEquals("Cameron", target.get("askForName"));
        assertEquals("258", target.get("countryCode"));
        assertEquals("21", target.get("areaCode"));
        assertEquals("310166 x 2", target.get("contactNumber"));
        
        //3. test a number without CC
        target = new HashMap();
        phone.setPropertyValue("(21) 310166 x 2");
        converter.toMap(contact, target);
        assertEquals("Cameron", target.get("askForName"));
        assertNull(target.get("countryCode"));
        assertEquals("21", target.get("areaCode"));
        assertEquals("310166 x 2", target.get("contactNumber"));  
        
        //4. test a number without CC or area code
        target = new HashMap();
        phone.setPropertyValue("310166 x 2");
        converter.toMap(contact, target);
        assertEquals("Cameron", target.get("askForName"));
        assertNull(target.get("countryCode"));
        assertNull(target.get("areaCode"));
        assertEquals("310166 x 2", target.get("contactNumber")); 
        
        //5. test a number without ANY extras
        target = new HashMap();
        phone.setPropertyValue("310166");
        converter.toMap(contact, target);
        assertEquals("Cameron", target.get("askForName"));
        assertNull(target.get("countryCode"));
        assertNull(target.get("areaCode"));
        assertEquals("310166", target.get("contactNumber")); 
    }
}
