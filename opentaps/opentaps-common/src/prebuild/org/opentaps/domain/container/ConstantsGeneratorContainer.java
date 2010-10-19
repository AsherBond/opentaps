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

/*******************************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 *******************************************************************************/

/* This file has been modified by Open Source Strategies, Inc. */

package org.opentaps.domain.container;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;

import freemarker.template.TemplateException;
import org.apache.commons.io.FileUtils;
import org.ofbiz.base.container.Container;
import org.ofbiz.base.container.ContainerConfig;
import org.ofbiz.base.container.ContainerException;
import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilProperties;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.base.util.template.FreeMarkerWorker;
import org.ofbiz.entity.Delegator;
import org.ofbiz.entity.DelegatorFactory;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.condition.EntityCondition;
import org.ofbiz.entity.condition.EntityOperator;
import org.ofbiz.service.ServiceDispatcher;

/**
 * Some utility routines for loading seed data.
 */
public class ConstantsGeneratorContainer implements Container {

    private static final String MODULE = ConstantsGeneratorContainer.class.getName();
    private static final String CONTAINER_NAME = "constants-generation-container";

    /** Container config file. */
    private String containerConfigFile;

    /** The path of output. */
    private String outputPath;
    /** Java class Template. */
    private String template;
    /** The properties file containing the configuration of the constants to generate. */
    private Properties configProperties;

    private String delegatorNameToUse;
    private Delegator delegator;

    /** Entries in <code>configProperties</code> with this value indicates the entities to query. */
    private static final String GENERATE_VALUE = "generate";

    /** Prefix constants that starts with a number to make a valid Java constant name. */
    private static final String NUMERIC_PREFIX = "_";


    /**
     * Creates a new <code>PojoGeneratorContainer</code> instance.
     */
    public ConstantsGeneratorContainer() {
        super();
    }

    /** {@inheritDoc} */
    public void init(String[] args, String configFile) throws ContainerException {
        this.containerConfigFile = configFile;
        // disable job scheduler, JMS listener and startup services
        ServiceDispatcher.enableJM(false);
        ServiceDispatcher.enableJMS(false);
        ServiceDispatcher.enableSvcs(false);
    }

    /** {@inheritDoc} */
    public boolean start() throws ContainerException {
        ContainerConfig.Container cfg = ContainerConfig.getContainer(CONTAINER_NAME, containerConfigFile);
        ContainerConfig.Container.Property delegatorNameProp = cfg.getProperty("delegator-name");
        ContainerConfig.Container.Property outputPathProp = cfg.getProperty("outputPath");
        ContainerConfig.Container.Property templateProp = cfg.getProperty("template");
        ContainerConfig.Container.Property configProp = cfg.getProperty("configProperties");

        // get delegator to use from the container configuration
        if (delegatorNameProp == null || delegatorNameProp.value == null || delegatorNameProp.value.length() == 0) {
            throw new ContainerException("Invalid delegator-name defined in container configuration");
        } else {
            delegatorNameToUse = delegatorNameProp.value;
        }

        // get the delegator
        delegator = DelegatorFactory.getDelegator(delegatorNameToUse);
        if (delegator == null) {
            throw new ContainerException("Invalid delegator name: " + delegatorNameToUse);
        }

        // get output path to use from the container configuration
        if (outputPathProp == null || outputPathProp.value == null || outputPathProp.value.length() == 0) {
            throw new ContainerException("Invalid output-name defined in container configuration");
        } else {
            outputPath = outputPathProp.value;
        }

        // check the output path
        File outputDir = new File(outputPath);
        if (!outputDir.canWrite()) {
            throw new ContainerException("Unable to use output path: [" + outputPath + "], it is not writable");
        }

        // get the template file to use from the container configuration
        if (templateProp == null || templateProp.value == null || templateProp.value.length() == 0) {
            throw new ContainerException("Invalid entity template defined in container configuration");
        } else {
            template = templateProp.value;
        }

        // get the properties file name containing the configuration
        if (configProp == null || configProp.value == null || templateProp.value.length() == 0) {
            throw new ContainerException("Invalid configuration properties defined in container configuration");
        } else {
            configProperties = UtilProperties.getProperties(configProp.value);
        }

        return generateConstants();
    }

    private boolean generateConstants() throws ContainerException {

        // check the template file
        File templateFile = new File(template);
        if (!templateFile.canRead()) {
            throw new ContainerException("Unable to read the template file: [" + template + "]");
        }

        // get the list of entities for which to get constants values
        // throw an error if the same key is used twice
        Map<String, ConstantModel> models = readConstantConfiguration(configProperties);

        // record errors for summary
        List<String> errorEntities = new LinkedList<String>();
        // keep track of generated files
        Map<String, String> generatedConstants = new HashMap<String, String>();

        int totalGenerated = 0;
        int totalGeneratedClass = 0;

        if (models != null && models.size() > 0) {
            Debug.logImportant("=-=-=-=-=-=-= Generating the constants ...", MODULE);
            for (String key : models.keySet()) {
                ConstantModel model = models.get(key);
                if (generatedConstants.containsKey(model.getClassName())) {
                    Debug.logError("Constant file [" + model.getClassName() + "] was already generated and is conflicting with the configuration key [" + key + "]", MODULE);
                    errorEntities.add(key);
                    continue;
                }

                List<Map<String, String>> values;
                try {
                    values = model.getDbValues(delegator);
                } catch (ConstantException e) {
                    errorEntities.add(key);
                    Debug.logError("Error getting the DB values for [" + key + "] : " + e.getMessage(), MODULE);
                    continue;
                }

                totalGenerated += values.size();

                // could be an object, but is just a Map for simplicity
                Map<String, Object> entityInfo = new HashMap<String, Object>();
                entityInfo.put("className", model.getClassName());
                entityInfo.put("description", model.getDescription());
                Boolean useInnerClasses = UtilValidate.isNotEmpty(model.getTypeField());
                entityInfo.put("useInnerClasses", useInnerClasses);
                // group by inner class if needed
                Map<String, List<Map<String, String>>> byInnerClass = new TreeMap<String, List<Map<String, String>>>();
                if (useInnerClasses) {
                    for (Map<String, String> value : values) {
                        String innerClass = value.get("innerClass");
                        List<Map<String, String>> grouped = byInnerClass.get(innerClass);
                        if (grouped == null) {
                            grouped = new ArrayList<Map<String, String>>();
                            byInnerClass.put(innerClass, grouped);
                        }
                        grouped.add(value);
                    }
                } else {
                    byInnerClass.put("", values);
                }
                entityInfo.put("valuesByInnerClass", byInnerClass);

                // render it as FTL
                Writer writer = new StringWriter();
                try {
                    FreeMarkerWorker.renderTemplateAtLocation(template, entityInfo, writer);
                } catch (MalformedURLException e) {
                    Debug.logError(e, MODULE);
                    errorEntities.add(key);
                    break;
                } catch (TemplateException e) {
                    Debug.logError(e, MODULE);
                    errorEntities.add(key);
                    break;
                } catch (IOException e) {
                    Debug.logError(e, MODULE);
                    errorEntities.add(key);
                    break;
                } catch (IllegalArgumentException e) {
                    Debug.logError(e, MODULE);
                    errorEntities.add(key);
                    break;
                }

                // write it as a Java file
                File file = new File(outputPath + model.getClassName() + ".java");
                try {
                    FileUtils.writeStringToFile(file, writer.toString(), "UTF-8");
                } catch (IOException e) {
                    Debug.logError(e, "Aborting, error writing file " + outputPath + model.getClassName() + ".java", MODULE);
                    errorEntities.add(key);
                    break;
                }
                // increment counter
                generatedConstants.put(model.getClassName(), key);
                totalGeneratedClass++;

            }
        } else {
            Debug.logImportant("=-=-=-=-=-=-= No constant to generate.", MODULE);
        }

        // error summary
        if (errorEntities.size() > 0) {
            Debug.logImportant("The following constants could not be generated:", MODULE);
            for (String name : errorEntities) {
                Debug.logImportant(name, MODULE);
            }
        }

        Debug.logImportant("=-=-=-=-=-=-= Finished the constants generation with " + totalGeneratedClass + " classes / " + totalGenerated + " values generated.", MODULE);

        if (errorEntities.size() > 0) {
            return false;
        }

        return true;
    }

    /** {@inheritDoc} */
    public void stop() throws ContainerException { }

    private TreeMap<String, ConstantModel> readConstantConfiguration(Properties config) throws ContainerException {
        TreeMap<String, ConstantModel> models = new TreeMap<String, ConstantModel>();
        // first collect the entity names
        Enumeration<?> propertyNames = config.propertyNames();
        while (propertyNames.hasMoreElements()) {
            String key = (String) propertyNames.nextElement();
            if (GENERATE_VALUE.equals(config.getProperty(key))) {
                if (models.containsKey(key)) {
                    throw new ContainerException("Entity: [" + key + "] already defined in the configuration.");
                }

                models.put(key, new ConstantModel(key));
            }
        }
        // then for each entity read the configuration
        for (String key : models.keySet()) {
            ConstantModel model = models.get(key);
            model.setClassName(config.getProperty(key + ".className"));
            model.setDescription(config.getProperty(key + ".description"));
            model.setTypeField(config.getProperty(key + ".typeField"));
            model.setNameField(config.getProperty(key + ".nameField"));
            model.setDescriptionField(config.getProperty(key + ".descriptionField"));
            model.setConstantField(config.getProperty(key + ".constantField"));
            model.setWhere(config.getProperty(key + ".where"));
        }

        return models;
    }

    private static class ConstantComparator implements Comparator<Map<String, String>> {
        public int compare(Map<String, String> o1, Map<String, String> o2) {
            String name1 = o1.get("constantName");
            String name2 = o2.get("constantName");
            if (name1 == null && name2 == null) {
                return 0;
            } else if (name1 == null && name2 != null) {
                return 1;
            } else {
                return name1.compareTo(name2);
            }
        }
    }

    private static class ConstantModel {
        private String key;
        private String entityName;
        private String className;
        private String description;

        private String typeField;
        private String constantField;
        private String nameField;
        private String descriptionField;

        private String where;

        public ConstantModel(String key) {
            this.key = key;
            this.setEntityName(key);
        }

        public void setEntityName(String entityName) {
            int idx = entityName.indexOf("!");
            if (idx >= 0) {
                this.entityName = entityName.substring(0, idx);
            } else {
                this.entityName = entityName;
            }
        }

        public void setClassName(String className) {
            if (className == null) {
                this.className = entityName + "Constants";
            } else {
                this.className = className;
            }
        }

        public void setDescription(String description) {
            if (description == null) {
                this.description = entityName + " constant values";
            } else {
                this.description = description;
            }
        }

        public void setTypeField(String typeField) { this.typeField = typeField; }
        public void setDescriptionField(String descriptionField) { this.descriptionField = descriptionField; }
        public void setNameField(String nameField) { this.nameField = nameField; }
        public void setConstantField(String constantField) { this.constantField = constantField; }
        public void setWhere(String where) { this.where = where; }

        public String getEntityName() { return this.entityName; }
        public String getClassName() { return this.className; }
        public String getDescription() { return this.description; }
        public String getConstantField() { return this.constantField; }
        public String getNameField() { return this.nameField; }
        public String getDescriptionField() { return this.descriptionField; }
        public String getTypeField() { return this.typeField; }

        public EntityCondition getWhereCondition() throws ConstantException {
            if (where == null) {
                return null;
            }
            // else the where string is field1=value1,field2=value2 ...
            String[] couples = where.split(",");
            List<EntityCondition> conds = new ArrayList<EntityCondition>();
            for (int i = 0; i < couples.length; i++) {
                String pair = couples[i];
                String[] values = pair.split("=");
                if (values.length != 2) {
                    throw new ConstantException("The where string must be formatted as 'field1=value1,field2=value2,...', was: " + where);
                }
                conds.add(EntityCondition.makeCondition(values[0], EntityOperator.EQUALS, values[1]));
            }
            return EntityCondition.makeCondition(conds);
        }

        public List<Map<String, String>> getDbValues(Delegator delegator) throws ConstantException {
            // this should have been set
            if (UtilValidate.isEmpty(getConstantField())) {
                throw new ConstantException("No constantField defined in the configuration for key [" + key + "]");
            }

            Set<String> fields = UtilMisc.toSet(getConstantField());
            if (UtilValidate.isNotEmpty(getNameField())) { fields.add(getNameField()); }
            if (UtilValidate.isNotEmpty(getDescriptionField())) { fields.add(getDescriptionField()); }
            if (UtilValidate.isNotEmpty(getTypeField())) { fields.add(getTypeField()); }

            List<String> orderBy = new ArrayList<String>();
            if (UtilValidate.isNotEmpty(getTypeField())) { orderBy.add(getTypeField()); }
            orderBy.add(getConstantField());

            List<GenericValue> entities;
            try {
                entities = delegator.findList(getEntityName(), getWhereCondition(), fields, orderBy, null, false);
            } catch (GenericEntityException e) {
                throw new ConstantException("GenericEntityException: " + e.getMessage());
            }

            List<Map<String, String>> values = new ArrayList<Map<String, String>>();
            Set<String> constantNames = new HashSet<String>();
            for (GenericValue entity : entities) {
                Map<String, String> map = new HashMap<String, String>();
                String val = entity.getString(getConstantField());
                map.put("constantValue", val);
                String type = "";
                if (UtilValidate.isNotEmpty(getTypeField())) {
                    type = entity.getString(getTypeField());
                    if (UtilValidate.isEmpty(type)) {
                        Debug.logWarning("The type field is empty in value: " + entity + " using its value as the type.", MODULE);
                        type = makeClassName(val);
                        map.put("innerClassIsSelf", "Y");
                    } else {
                        type = makeClassName(type);
                    }
                    map.put("innerClass", type);
                }
                String name = null;
                if (UtilValidate.isNotEmpty(getNameField())) {
                    name = entity.getString(getNameField());
                } else {
                    name = val;
                }
                name = makeConstantName(name);
                if (constantNames.contains(type + "$" + name)) {
                    throw new ConstantException("Duplicate constant found with name [" + name + "] type [" + type + "], in key [" + key + "] with value: " + entity);
                }
                constantNames.add(type + "$" + name);
                map.put("constantName", name);

                if (UtilValidate.isNotEmpty(getDescriptionField())) {
                    map.put("constantDescription", entity.getString(getDescriptionField()));
                } else {
                    map.put("constantDescription", val);
                }
                values.add(map);
            }
            Collections.sort(values, new ConstantComparator());
            return values;
        }

    }

    @SuppressWarnings("serial")
    private static class ConstantException extends Exception {
        public ConstantException(String message) { super(message); }
    }

    /**
     * Transform a name String (eg: Some String) into a java static field name (eg: SOME_STRING).
     * @param name the name to transform
     * @return a class name
     */
    public static String makeConstantName(String name) {
        if (UtilValidate.isEmpty(name)) {
            throw new IllegalArgumentException("name called for null or empty string");
        } else {
            String fieldName = name.toUpperCase();
            fieldName = fieldName.replace(' ', '_');
            fieldName = fieldName.replace('.', '_');
            fieldName = fieldName.replace('-', '_');
            fieldName = fieldName.replace(':', '_');
            if (fieldName.substring(0, 1).matches("[0-9]")) {
                fieldName = NUMERIC_PREFIX + fieldName;
            }
            return fieldName;
        }
    }

    /**
     * Transform a constant String (eg: SOME_VALUE) into a class name (eg: SomeValue).
     * @param name the name to transform
     * @return a class name
     */
    public static String makeClassName(String name) {
        if (UtilValidate.isEmpty(name)) {
            throw new IllegalArgumentException("className called for null or empty string");
        } else {
            String className = name.toLowerCase();
            className = Character.toUpperCase(className.charAt(0)) + className.substring(1);
            className = filterChar(className, ".");
            className = filterChar(className, "_");
            className = filterChar(className, "-");
            className = filterChar(className, ":");
            return className;
        }
    }

    private static String filterChar(String name, String filter) {
        int idx = -1;
        String newName = name;
        while ((idx = newName.indexOf(filter)) >= 0) {
            String temp = newName.substring(0, idx);
            if (newName.length() > (idx + 1)) {
                temp += Character.toUpperCase(newName.charAt(idx + 1));
            }
            if (newName.length() > (idx + 2)) {
                temp += newName.substring(idx + 2);
            }
            newName = temp;
        }
        return newName;
    }

}
