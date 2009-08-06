/*
 * Copyright (c) 2007 - 2009 Open Source Strategies, Inc.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the Honest Public License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * Honest Public License for more details.
 *
 * You should have received a copy of the Honest Public License
 * along with this program; if not, write to Funambol,
 * 643 Bair Island Road, Suite 305 - Redwood City, CA 94063, USA
 */
package org.opentaps.foundation.entity.hibernate;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.hibernate.Query;
import org.hibernate.Transaction;
import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.entity.EntityCryptoException;
import org.ofbiz.entity.GenericDelegator;
import org.ofbiz.entity.GenericEntity;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.model.ModelEntity;
import org.ofbiz.entity.model.ModelField;
import org.ofbiz.entity.util.EntityCrypto;
import org.opentaps.domain.base.entities.SequenceValueItem;
import org.opentaps.foundation.entity.Entity;


/**
 * Hibernate Util Class.
 *
 */
public class HibernateUtil {
    private static final String MODULE = HibernateUtil.class.getName();
    // define delimiter, for split words in hql
    public static String DELIMITERS = " \t\n\r";
    // define word pattern
    public static String WORD_PATTERN = "[\\w\\.]+";
    // define logic sign pattern
    public static String SIGN_PATTERN = "(like)|[>=<]+";

    /**
     * Get the next guaranteed unique seq id from the sequence with the given sequence name;
     * if the named sequence doesn't exist, it will be created.
     *@param session a <code>org.hibernate.Session</code> value
     *@param seqName The name of the sequence to get the next seq id from
     *@return next seq id for the given sequence name
     */
    public static synchronized Long getNextSeqId(org.hibernate.Session session, String seqName) {
        //retrieve relate SequenceValueItem
        Transaction tx = session.beginTransaction();
        String hql = "from SequenceValueItem eo where seqName='" + seqName + "'";
        Query query = session.createQuery(hql);
        java.util.List<SequenceValueItem> sequenceValueItems = query.list();
        SequenceValueItem sequenceValueItem;
        if (sequenceValueItems.size() == 0) {
            //create a new SequenceValueItem
            sequenceValueItem = new SequenceValueItem();
            sequenceValueItem.setSeqId((long) OpentapsIdentifierGenerator.SEQUENCE_INIT_VALUE);
            sequenceValueItem.setSeqName(seqName);
        } else {
            sequenceValueItem = sequenceValueItems.get(0);
            sequenceValueItem.setSeqId(sequenceValueItem.getSeqId() + 1);
        }
        session.save(sequenceValueItem);
        session.flush();
        tx.commit();
        session.close();
        return sequenceValueItem.getSeqId();
    }

    /**
     * generate GenericValue from Entity.
     *
     * @param entity
     *            a <code>Entity</code> object
     * @param delegator
     *            a <code>GenericDelegator</code> object
     * @throws GenericEntityException
     *             if an error occurs
     * @return a <code>GenericValue</code> value
     */
    public static GenericValue entityToGenericValue(Entity entity, GenericDelegator delegator)
            throws GenericEntityException {
        GenericValue value = GenericValue.create(delegator.getModelReader().getModelEntity(getEntityClassName(entity)), entity.toMap());
        return value;
    }

    /**
     * get class name of entity.
     *
     * @param entity
     *            a <code>Entity</code> object
     * @return class name of entity value
     */
    public static String getEntityClassName(Entity entity) {
        String className = entity.getClass().getSimpleName();
        if (className.indexOf("_") > 0) {
            className = className.substring(0, className.indexOf("_"));
        }
        return className;
    }

    /**
     * refresh pojo properties by GenericValue.
     *
     * @param entity
     *            a <code>Entity</code> object
     * @param value
     *            a <code>GenericValue</code> object
     * @throws GenericEntityException
     *             if an error occurs
     */
    public static void refreshPojoByGenericValue(Entity entity, GenericEntity value)
            throws GenericEntityException {
        Map fields = value.getAllFields();
        Iterator it = fields.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry entry = (Entry) it.next();
            if (!entity.getPrimaryKeyNames().contains(entry.getKey())) {
                entity.set((String) entry.getKey(), entry.getValue());
            }
        }
    }

    /**
     * Get if exist specific filed in object.
     *
     * @param object a <code>Object</code> value
     * @param fieldName field name
     * @param classType a <code>Class</code> value
     * @return if exist this field
     * @throws NoSuchMethodException if an error occurs
     * @throws IllegalArgumentException if an error occurs
     * @throws IllegalAccessException if an error occurs
     * @throws InvocationTargetException if an error occurs
     */
    public static  boolean isExistField(Object object, String fieldName, Class classType) throws NoSuchMethodException, IllegalArgumentException, IllegalAccessException, InvocationTargetException {
        String getter = "get" + Character.toUpperCase(fieldName.charAt(0)) + fieldName.substring(1);
        String setter = "set" + Character.toUpperCase(fieldName.charAt(0)) + fieldName.substring(1);
        Method getMethod = object.getClass().getMethod(getter);
        Method setMethod = object.getClass().getMethod(setter, new Class[] {classType});
        return getMethod != null && setMethod != null;
    }

    /**
     * Get filed value by field name.
     *
     * @param object a <code>Object</code> value
     * @param fieldName field name
     * @return field value
     * @throws IllegalArgumentException if an error occurs
     * @throws IllegalAccessException if an error occurs
     * @throws InvocationTargetException if an error occurs
     */
    public static Object getFieldValue(Object object, String fieldName) throws IllegalArgumentException, IllegalAccessException, InvocationTargetException {
        String getter = "get" + Character.toUpperCase(fieldName.charAt(0)) + fieldName.substring(1);
        try {
            Method getMethod = object.getClass().getMethod(getter, new Class[]{});
            Object value = getMethod.invoke(object, new Object[]{});
            return value;
        } catch (NoSuchMethodException ex) {
            return null;
        }
    }

    /**
     * Set filed value by field name.
     *
     * @param object a <code>Object</code> value
     * @param fieldName field name
     * @param fieldValue field value
     * @throws NoSuchMethodException if an error occurs
     * @throws IllegalArgumentException if an error occurs
     * @throws IllegalAccessException if an error occurs
     * @throws InvocationTargetException if an error occurs
     */
    public static void setFieldValue(Object object, String fieldName, Object fieldValue) throws  NoSuchMethodException, IllegalArgumentException, IllegalAccessException, InvocationTargetException {
        if (fieldValue != null) {
            String setter = "set" + Character.toUpperCase(fieldName.charAt(0)) + fieldName.substring(1);
            Method setMethod = object.getClass().getMethod(setter, new Class[]{fieldValue.getClass()});
            setMethod.invoke(object, fieldValue);
        }
    }

    /**
     * Get new Entity Instance by QueryString.
     *
     * @param queryString a <code>String</code> value
     * @return a <code>Entity</code> value
     * @throws InstantiationException if an error occurs
     * @throws IllegalAccessException if an error occurs
     * @throws ClassNotFoundException if an error occurs
     */
    public static Entity getEntityInstanceByQueryString(String queryString) throws InstantiationException, IllegalAccessException, ClassNotFoundException {
        String className = retrieveClassName(queryString);
        return getEntityInstanceByClassName(className);
    }

    /**
     * Get new Entity Instance by ClassName.
     *
     * @param className a <code>String</code> value
     * @return a <code>Entity</code> value
     * @throws InstantiationException if an error occurs
     * @throws IllegalAccessException if an error occurs
     * @throws ClassNotFoundException if an error occurs
     */
    public static Entity getEntityInstanceByClassName(String className) throws InstantiationException, IllegalAccessException, ClassNotFoundException {
        // add default entity package name if it doesn't already have one
        if (className.indexOf(".") < 0) {
            className = "org.opentaps.domain.base.entities." + className;
        }
        Entity entity = (Entity) Class.forName(className).newInstance();
        return entity;
    }

    /**
     * Get encrypt parameters by QueryString.
     *
     * @param queryString a <code>String</code> value
     * @param delegator a <code>GenericDelegator</code> value
     * @return a <code>List<String></code> value
     */
    public static List<String> getEncryptParametersByQueryString(String queryString, GenericDelegator delegator) {
        List<String> encryptFields = new ArrayList<String>();
        try {
            Entity entity =  getEntityInstanceByQueryString(queryString);
            GenericValue genericValue = entityToGenericValue(entity, delegator);
            ModelEntity model = genericValue.getModelEntity();
            Iterator i = model.getFieldsIterator();
            while (i.hasNext()) {
                ModelField modelField = (ModelField) i.next();
                if (modelField.getEncrypt()) {
                    //write encrypt field value
                    encryptFields.add(modelField.getName());
                }
            }
            return getEncryptParametersByQueryString(queryString, retrieveClassAlias(queryString), encryptFields);
        } catch (ClassNotFoundException ex) {
            Debug.logError("Casuse ClassNotFoundException when call getEncryptFieldsByQueryString, " + ex, MODULE);
        } catch (IllegalAccessException ex) {
            Debug.logError("Casuse IllegalAccessException when call getEncryptFieldsByQueryString, " + ex, MODULE);
        } catch (InstantiationException ex) {
            Debug.logError("Casuse InstantiationException when call getEncryptFieldsByQueryString, " + ex, MODULE);
        } catch (GenericEntityException ex) {
            Debug.logError("Casuse GenericEntityException when call getEncryptFieldsByQueryString, " + ex, MODULE);
        }
        return encryptFields;
     }

    /**
     * Get encrypt field by QueryString.
     *
     * @param className a <code>String</code> value
     * @param delegator a <code>GenericDelegator</code> value
     * @return a <code>List<String></code> value
     */
    public static List<String> getEncryptFieldsByClassName(String className, GenericDelegator delegator) {
        List<String> encryptFields = new ArrayList<String>();
        try {
            Entity entity =  getEntityInstanceByClassName(className);
            GenericValue genericValue = entityToGenericValue(entity, delegator);
            ModelEntity model = genericValue.getModelEntity();
            Map fields = genericValue.getAllFields();
            Iterator i = model.getFieldsIterator();
            while (i.hasNext()) {
                ModelField modelField = (ModelField) i.next();
                if (modelField.getEncrypt()) {
                    //write encrypt field value
                    encryptFields.add(modelField.getName());
                }
            }
        } catch (ClassNotFoundException ex) {
            Debug.logError("Casuse ClassNotFoundException when call getEncryptFieldsByQueryString, " + ex, MODULE);
        } catch (IllegalAccessException ex) {
            Debug.logError("Casuse IllegalAccessException when call getEncryptFieldsByQueryString, " + ex, MODULE);
        } catch (InstantiationException ex) {
            Debug.logError("Casuse InstantiationException when call getEncryptFieldsByQueryString, " + ex, MODULE);
        } catch (GenericEntityException ex) {
            Debug.logError("Casuse GenericEntityException when call getEncryptFieldsByQueryString, " + ex, MODULE);
        }
        return encryptFields;
     }

    /**
     * retrieve class name from hql, like "from TestEntityAndItem eo" should return "TestEntityAndItem".
     *
     * @param hql
     *            hql query string
     * @return the class name
     */
    public static String retrieveClassName(String hql) {
        StringTokenizer stringTokenizer = new StringTokenizer(hql, DELIMITERS);
        while (stringTokenizer.hasMoreTokens()) {
            // iterate each token
            String token = stringTokenizer.nextToken();
            if (token.equalsIgnoreCase("from")
                    && stringTokenizer.hasMoreTokens()) {
                // class name should be the token that after "from" keyword
                String fromClass = stringTokenizer.nextToken();
                return fromClass;
            }
        }
        return null;
    }

    /**
     * retrieve simple class name from hql, like "from TestEntityAndItem eo" should
     * return "TestEntityAndItem".
     *
     * @param hql query string
     * @return the class name
     */
    public static String retrieveSimpleClassName(String hql) {
        StringTokenizer stringTokenizer = new StringTokenizer(hql, DELIMITERS);
        while (stringTokenizer.hasMoreTokens()) {
            // iterate each token
            String token = stringTokenizer.nextToken();
            if (token.equalsIgnoreCase("from")
                    && stringTokenizer.hasMoreTokens()) {
                // class name should be the token that after "from" keyword
                String fromClass = stringTokenizer.nextToken();
                if (fromClass.indexOf(".") > 0) {
                    fromClass = fromClass.substring(fromClass.lastIndexOf(".") + 1);
                }
                return fromClass;
            }
        }
        return null;
    }

    /**
     * retrieve class alias from hql, like "from TestEntityAndItem eo" should
     * return "eo".
     *
     * @param hql
     *            hql query string
     * @return the class alias or null
     */
    public static String retrieveClassAlias(String hql) {
        StringTokenizer stringTokenizer = new StringTokenizer(hql, HibernateUtil.DELIMITERS);
        while (stringTokenizer.hasMoreTokens()) {
            // iterate each token
            String token = stringTokenizer.nextToken();
            if (token.equalsIgnoreCase("from")
                    && stringTokenizer.hasMoreTokens()) {
                String fromClass = stringTokenizer.nextToken();
                if (stringTokenizer.hasMoreTokens()) {
                    String nextToken = stringTokenizer.nextToken();
                    // if exist "as", class alias should be next token
                    if (nextToken.equalsIgnoreCase("as")
                            && stringTokenizer.hasMoreTokens()) {
                        String classAlias = stringTokenizer.nextToken();
                        return classAlias;
                    } else if (!nextToken.equalsIgnoreCase("where")
                            && !nextToken.equalsIgnoreCase("order")) {
                        // if not "where"/"order", class alias should be this
                        // token
                        return nextToken;
                    }
                }
            }
        }
        // can not find alias, return null
        return null;
    }

    /**
     * transform hql to sql, like
     * "from TestEntityAndItem eo where eo.testEntityId = '10000'" should return
     * "where TEST_ENTITY_ID = '10000'".
     *
     * @param hql
     *            query string
     * @param className
     *            class name
     * @param classAlias
     *            class alias
     * @param fieldMapColumns
     *            the mapping of field-column
     * @return the sql query string
     */
    public static String hqlToSql(String hql, String className,
            String classAlias, Map<String, String> fieldMapColumns) {
        String sql = "";
        boolean shouldChange = false;
        StringTokenizer stringTokenizer = new StringTokenizer(hql, HibernateUtil.DELIMITERS);
        while (stringTokenizer.hasMoreTokens()) {
            // iterate each token
            String token = stringTokenizer.nextToken();
            if (shouldChange) {
                // if it is token that should change
                String newToken = token;
                // split it as word group
                Pattern p = Pattern.compile(HibernateUtil.WORD_PATTERN);
                Matcher m = p.matcher(token);
                while (m.find()) {
                    String word = m.group();
                    if (classAlias != null
                            && word.indexOf(classAlias + ".") == 0) {
                        // remove alias, such as "eo.partyId" => "partyId"
                        word = word.substring((classAlias + ".").length());
                    }
                    if (fieldMapColumns.containsKey(word)) {
                        // if exist this field, then replace it by column name
                        newToken = newToken.replace(m.group(), fieldMapColumns
                                .get(word));
                        Debug.logInfo("hqlToSql [" + word + "]" + m.group()
                                + " -> " + newToken, MODULE);
                    }
                }
                if (!sql.equals("")) {
                    sql += " ";
                }
                sql += newToken;
            } else if (classAlias != null) {
                // if class alias not null, we will transform the sentence that
                // after class alias token
                shouldChange = token.equals(classAlias);
            } else {
                // if class alias is null, we will transform the sentence that
                // after class name token
                shouldChange = token.equals(className);
            }
        }
        return sql;
    }

    /**
     * crypt the encrypt field.
     *
     * @param entityName a <code>String</code> value
     * @param encryptParameters a <code>List<String></code> value
     * @param crypto a <code>EntityCrypto</code> object
     * @param paramter a <code>String</code> value
     * @param value a <code>Object</code> value
     * @return Object
     */
    public static Object getCryptoParameter(String entityName, List<String> encryptParameters, EntityCrypto crypto, String paramter, Object value) {
        if (value != null && value instanceof String) {
            return getCryptoParameter(entityName, encryptParameters, crypto, paramter, (String) value);
        } else {
            return value;
        }
    }

    /**
     * crypt the encrypt field.
     *
     * @param entityName a <code>String</code> value
     * @param encryptParameters a <code>List<String></code> value
     * @param crypto a <code>EntityCrypto</code> object
     * @param paramter a <code>String</code> value
     * @param value a <code>String</code> value
     * @return String
     */
    public static String getCryptoParameter(String entityName, List<String> encryptParameters, EntityCrypto crypto, String paramter, String value) {
        String returnValue = value;
        if (entityName != null && value != null && encryptParameters.contains(paramter)) {
            try {
                returnValue = crypto.encrypt(entityName, value);
            } catch (EntityCryptoException e) {
                Debug.logError(e.getMessage(), MODULE);
            }
        }
        return returnValue;
    }

    /**
     * decrypt the encrypt field to initial value.
     * @param entityName a <code>String</code> value
     * @param encryptFields a <code>List<String></code> value
     * @param crypto a <code>EntityCrypto</code> object
     * @param entity a <code>Entity</code> object
     */
    public static void decryptField(String entityName, List<String> encryptFields, EntityCrypto crypto, Entity entity) {
        if (entityName != null) {
            for (String encryptField : encryptFields) {
                String encHex = (String) entity.get(encryptField);
                if (UtilValidate.isNotEmpty(encHex)) {
                    try {
                        HibernateUtil.setFieldValue(entity, encryptField, crypto.decrypt(entityName, encHex));
                    } catch (Exception e) {
                        Debug.logInfo(e, MODULE);
                    }
                }
            }
        }
    }

    /**
     * get encrypt parameter name from queryString.
     *
     * @param hql
     *            query string
     * @param classAlias
     *            class alias
     * @param encryptFields
     *            the encrypt field list
     * @return the encrypt parameter list
     */
    public static List<String> getEncryptParametersByQueryString(String hql, String classAlias, List<String> encryptFields) {
        //create return list
        List<String> encryptParameters = new ArrayList<String>();
        if (encryptFields.size() == 0) {
            // no encrypt fields in this entity, so no need to search encrypt parameters
            return encryptParameters;
        }
        //create a StringTokenizer to split hql
        StringTokenizer stringTokenizer = new StringTokenizer(hql, HibernateUtil.DELIMITERS);
        // create a list to store tokens
        List<String> tokens = new ArrayList<String>();
        while (stringTokenizer.hasMoreTokens()) {
            // iterate each token
            String token = stringTokenizer.nextToken();
            tokens.add(token);
        }
        // index of parameter, if you use the locate parameter ? in HQL
        int parameterIndex = 0;
        for (int i = 0; i < tokens.size(); i++) {
            // iterate each token to find if include any encrypt field
            String token = tokens.get(i);
            if (token.indexOf(":") == 0 || token.indexOf("?") == 0) {
                //if find a parameter
                String searchField = null;
                if (i > 1 && tokens.get(i - 1).matches(SIGN_PATTERN)) {
                    //if front token is SIGH, then searchFile is front token of the front token.
                    searchField = tokens.get(i - 2);
                } else if (i + 2 < tokens.size() && tokens.get(i + 1).matches(SIGN_PATTERN)) {
                  //if next token is SIGH, then searchFile is next token of the next token.
                    searchField = tokens.get(i + 2);
                }
                // split words from searchField
                Pattern p = Pattern.compile(HibernateUtil.WORD_PATTERN);
                Matcher m = p.matcher(searchField);
                while (m.find()) {
                    String word = m.group();
                    if (classAlias != null && word.indexOf(classAlias + ".") == 0) {
                        // remove alias, such as "eo.partyId" => "partyId"
                        word = word.substring((classAlias + ".").length());
                    }
                    // if it is a encrypt field
                    if (encryptFields.contains(word)) {
                        String parameter = token.length() > 1 ? token.substring(1) : String.valueOf(parameterIndex++);
                        // add this parameter
                        if (!encryptParameters.contains(parameter)) {
                            encryptParameters.add(parameter);
                            Debug.logInfo("find encrypt parameter [" + parameter + "] in [" + hql + "]", MODULE);
                        }
                    }
                }
            }
        }
        return encryptParameters;
    }


}
