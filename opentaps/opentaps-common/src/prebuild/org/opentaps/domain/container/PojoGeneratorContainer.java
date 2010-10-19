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
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.apache.commons.io.FileUtils;
import org.jvnet.inflector.Noun;
import org.ofbiz.base.container.Container;
import org.ofbiz.base.container.ContainerConfig;
import org.ofbiz.base.container.ContainerException;
import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.ObjectType;
import org.ofbiz.base.util.UtilProperties;
import org.ofbiz.base.util.UtilURL;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.base.util.template.FreeMarkerWorker;
import org.ofbiz.entity.Delegator;
import org.ofbiz.entity.DelegatorFactory;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.model.ModelEntity;
import org.ofbiz.entity.model.ModelField;
import org.ofbiz.entity.model.ModelFieldType;
import org.ofbiz.entity.model.ModelKeyMap;
import org.ofbiz.entity.model.ModelReader;
import org.ofbiz.entity.model.ModelRelation;
import org.ofbiz.entity.model.ModelUtil;
import org.ofbiz.entity.model.ModelViewEntity;
import org.ofbiz.entity.model.ModelViewEntity.ModelAlias;
import org.ofbiz.entity.model.ModelViewEntity.ModelMemberEntity;
import org.ofbiz.entity.model.ModelViewEntity.ModelViewLink;
import org.ofbiz.service.DispatchContext;
import org.ofbiz.service.GenericDispatcher;
import org.ofbiz.service.GenericServiceException;
import org.ofbiz.service.LocalDispatcher;
import org.ofbiz.service.ModelParam;
import org.ofbiz.service.ModelService;
import org.ofbiz.service.ServiceDispatcher;

import freemarker.template.TemplateException;

/**
 * Some utility routines for loading seed data.
 */
public class PojoGeneratorContainer implements Container {

    private static final String MODULE = PojoGeneratorContainer.class.getName();
    private static final String containerName = "pojo-generator-container";
    private static final String fileExtension = ".java";
    private static final List<String> extendClassNames = new ArrayList<String>();
    static {
        extendClassNames.add("Person");
        extendClassNames.add("PartyGroup");
    }

    /** Config file. */
    private String configFile;

    /** The path of output. */
    private String entityOutputPath;
    private String baseEntityOutputPath;
    private String serviceOutputPath;

    private Set<String> baseEntities;

    /** Java service class FTL Template. */
    private String serviceTemplate;

    /** Java entity class FTL Template. */
    private String entityTemplate;

    /** Hibernate IdClass FTL Template. */
    private String pkTemplate;

    /** Hibernate Search PkBridge FTL Template. */
    private String pkBridgeTemplate;

    /** Hibernate configuration FTL Template. */
    private String hibernateCfgTemplate;
    private String hibernateCfgPath;

    private Properties entitySearchProperties;
    private String delegatorNameToUse;
    private Delegator delegator;


    /**
     * Creates a new <code>PojoGeneratorContainer</code> instance.
     */
    public PojoGeneratorContainer() {
        super();
    }

    /** {@inheritDoc} */
    public void init(String[] args, String configFile) throws ContainerException {
        this.configFile = configFile;
        // disable job scheduler, JMS listener and startup services
        ServiceDispatcher.enableJM(false);
        ServiceDispatcher.enableJMS(false);
        ServiceDispatcher.enableSvcs(false);

        // parse arguments here if needed
    }

    /** {@inheritDoc} */
    public boolean start() throws ContainerException {
        ContainerConfig.Container cfg = ContainerConfig.getContainer(containerName, configFile);
        ContainerConfig.Container.Property delegatorNameProp = cfg.getProperty("delegator-name");
        ContainerConfig.Container.Property baseEntitiesProp = cfg.getProperty("baseEntities");
        ContainerConfig.Container.Property entityOutputPathProp = cfg.getProperty("entityOutputPath");
        ContainerConfig.Container.Property baseEntityOutputPathProp = cfg.getProperty("baseEntityOutputPath");
        ContainerConfig.Container.Property serviceOutputPathProp = cfg.getProperty("serviceOutputPath");
        ContainerConfig.Container.Property entityTemplateProp = cfg.getProperty("entityTemplate");
        ContainerConfig.Container.Property serviceTemplateProp = cfg.getProperty("serviceTemplate");
        ContainerConfig.Container.Property pkTemplateProp = cfg.getProperty("pkTemplate");
        ContainerConfig.Container.Property pkBridgeTemplateProp = cfg.getProperty("pkBridgeTemplate");
        ContainerConfig.Container.Property hibernateCfgTemplateProp = cfg.getProperty("hibernateCfgTemplate");
        ContainerConfig.Container.Property hibernateCfgProp = cfg.getProperty("hibernateCfgPath");
        entitySearchProperties = UtilProperties.getProperties("entitysearch.properties");

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

        // get the list of entities to use as base entities
        if (baseEntitiesProp != null && baseEntitiesProp.value != null && baseEntitiesProp.value.length() > 0) {
            String[] fes = baseEntitiesProp.value.split(",");
            baseEntities = new TreeSet<String>();
            for (int i = 0; i < fes.length; i++) {
                baseEntities.add(fes[i]);
            }
            Debug.logInfo("Entities configured to be generated as base entities: " + baseEntities, MODULE);
        }

        // get output path to use from the container configuration
        if (baseEntityOutputPathProp == null || baseEntityOutputPathProp.value == null || baseEntityOutputPathProp.value.length() == 0) {
            throw new ContainerException("Invalid baseEntityOutputPath defined in container configuration");
        } else {
            baseEntityOutputPath = baseEntityOutputPathProp.value;
        }

        // check the output path
        File outputDir = new File(baseEntityOutputPath);
        if (!outputDir.canWrite()) {
            throw new ContainerException("Unable to use base entity output path: [" + baseEntityOutputPath + "], it is not writable");
        }

        // get output path to use from the container configuration
        if (entityOutputPathProp == null || entityOutputPathProp.value == null || entityOutputPathProp.value.length() == 0) {
            throw new ContainerException("Invalid entityOutputPath defined in container configuration");
        } else {
            entityOutputPath = entityOutputPathProp.value;
        }

        // check the output path
        outputDir = new File(entityOutputPath);
        if (!outputDir.canWrite()) {
            throw new ContainerException("Unable to use entity output path: [" + entityOutputPath + "], it is not writable");
        }

        // get output path to use from the container configuration
        if (serviceOutputPathProp == null || serviceOutputPathProp.value == null || serviceOutputPathProp.value.length() == 0) {
            throw new ContainerException("Invalid serviceOutputPath defined in container configuration");
        } else {
            serviceOutputPath = serviceOutputPathProp.value;
        }

        // check the output path
        outputDir = new File(serviceOutputPath);
        if (!outputDir.canWrite()) {
            throw new ContainerException("Unable to use service output path: [" + serviceOutputPath + "], it is not writable");
        }

        // get the template file to use from the container configuration
        if (entityTemplateProp == null || entityTemplateProp.value == null || entityTemplateProp.value.length() == 0) {
            throw new ContainerException("Invalid entity template defined in container configuration");
        } else {
            entityTemplate = entityTemplateProp.value;
        }
        if (serviceTemplateProp == null || serviceTemplateProp.value == null || serviceTemplateProp.value.length() == 0) {
            throw new ContainerException("Invalid service template defined in container configuration");
        } else {
            serviceTemplate = serviceTemplateProp.value;
        }

        // get the primay key template file to use from the container configuration
        if (pkTemplateProp == null || pkTemplateProp.value == null || pkTemplateProp.value.length() == 0) {
            throw new ContainerException("Invalid pk template defined in container configuration");
        } else {
            pkTemplate = pkTemplateProp.value;
        }

        // get the primay key template file to use from the container configuration
        if (hibernateCfgTemplateProp == null || hibernateCfgTemplateProp.value == null || hibernateCfgTemplateProp.value.length() == 0) {
            throw new ContainerException("Invalid hibernate cfg template defined in container configuration");
        } else {
            hibernateCfgTemplate = hibernateCfgTemplateProp.value;
        }

        // get the primay key bridge template file to use from the container configuration
        if (pkBridgeTemplateProp == null || pkBridgeTemplateProp.value == null || pkBridgeTemplateProp.value.length() == 0) {
            throw new ContainerException("Invalid pk bridge template defined in container configuration");
        } else {
            pkBridgeTemplate = pkBridgeTemplateProp.value;
        }

        // get hibernate.cfg.xml output path to use from the container configuration
        if (hibernateCfgProp == null || hibernateCfgProp.value == null || hibernateCfgProp.value.length() == 0) {
            throw new ContainerException("Invalid hibernate cfg path defined in container configuration");
        } else {
            hibernateCfgPath = hibernateCfgProp.value;
        }

        return generateEntities() && generateServices();
    }

    private boolean generateServices() throws ContainerException {

        // check the service template file
        File serviceTemplateFile = new File(serviceTemplate);
        if (!serviceTemplateFile.canRead()) {
            throw new ContainerException("Unable to read service template file: [" + serviceTemplate + "]");
        }

        // get services list
        LocalDispatcher localDispatcher = GenericDispatcher.getLocalDispatcher("webtools", delegator);
        DispatchContext dispatchContext = localDispatcher.getDispatchContext();
        Set<String> serviceNames = dispatchContext.getAllServiceNames();

        // record errors for summary
        List<String> errorEntities = new LinkedList<String>();

        int totalGeneratedClasses = 0;
        if (serviceNames != null && serviceNames.size() > 0) {
            Debug.logImportant("=-=-=-=-=-=-= Generating the POJO services ...", MODULE);
            Map<String, String> generatedServices = new HashMap<String, String>();
            for (String serviceName : serviceNames) {
                ModelService modelService = null;
                try {
                    modelService = dispatchContext.getModelService(serviceName);
                } catch (GenericServiceException e) {
                    Debug.logError(e, MODULE);
                }

                if (modelService == null) {
                    errorEntities.add(serviceName);
                    Debug.logError("Error getting the service model for [" + serviceName + "]", MODULE);
                    continue;
                }

                // service names can include characters which cannot be used in class names
                // and we also need to upper the first char to conform to the Java standard
                String serviceClassName = makeClassName(serviceName) + "Service";

                // check if we have a conflict after the name change
                if (generatedServices.containsKey(serviceClassName)) {
                    Debug.logError("Service [" + serviceName + "] has conflicting class name with service [" + generatedServices.get(serviceClassName) + "], both resolved to [" + serviceClassName + "]", MODULE);
                    errorEntities.add(serviceName);
                    continue;
                }

                Map<String, Object> entityInfo = new HashMap<String, Object>();

                // distinct types for the import section
                Set<String> types = new TreeSet<String>();
                types.add("java.util.Map");
                types.add("java.util.Set");
                types.add("javolution.util.FastMap");
                types.add("javolution.util.FastSet");
                types.add("org.opentaps.foundation.infrastructure.User");
                // a type for each field during declarations
                Map<String, String> fieldTypes = new TreeMap<String, String>();
                Map<String, String> fieldRealTypes = new TreeMap<String, String>();
                // temporary, those are stored at the end of each param so we test duplicates
                Map<String, ModelParam> fieldModels = new TreeMap<String, ModelParam>();
                // map parameter names to valid java field names
                Map<String, String> inFieldNames = new TreeMap<String, String>();
                Map<String, String> outFieldNames = new TreeMap<String, String>();
                // read the service parameters
                Map<String, String> paramNames = new TreeMap<String, String>(); // for the enums
                Map<String, ModelParam> inParams = new TreeMap<String, ModelParam>();
                Map<String, ModelParam> outParams = new TreeMap<String, ModelParam>();
                // names of the get and set methods, we need two sets for In and Out parameters
                Map<String, String> inGetMethodNames = new TreeMap<String, String>();
                Map<String, String> inSetMethodNames = new TreeMap<String, String>();
                Map<String, String> outGetMethodNames = new TreeMap<String, String>();
                Map<String, String> outSetMethodNames = new TreeMap<String, String>();

                boolean hasError = false;
                for (ModelParam p : modelService.getModelParamList()) {
                    // use the class name transform, we will append "in" or "out" to get the field name
                    String fieldName = makeClassName(p.name);
                    String shortFieldName = Character.toLowerCase(fieldName.charAt(0)) + fieldName.substring(1);

                    // check that two different parameter names did not translate into the same java field
                    // eg: some.param and someParam
                    if ((!paramNames.containsKey(p.name) && paramNames.containsValue(shortFieldName))) {
                        errorEntities.add(serviceName);
                        Debug.logError("Service [" + serviceName + "] has parameter [" + p.name + "] which conflicts with another of its parameters that was translated to the same java field [" + fieldName + "]", MODULE);
                        hasError = true;
                        break;
                    }

                    // for the enumeration
                    paramNames.put(p.name, shortFieldName);

                    try {
                        if (p.isIn()) {
                            inParams.put(p.name, p);
                            inGetMethodNames.put(p.name, accessorMethodName("getIn", fieldName));
                            inSetMethodNames.put(p.name, accessorMethodName("setIn", fieldName));
                            if (inFieldNames.containsKey(p.name)) {
                                Debug.logWarning("Service [" + serviceName + "] redefines IN parameter [" + p.name + "] (used attribute instead of override, or redefined an auto attribute)", MODULE);
                            }
                            inFieldNames.put(p.name, "in" + fieldName);
                        }
                        // no else, can be INOUT
                        if (p.isOut()) {
                            outParams.put(p.name, p);
                            outGetMethodNames.put(p.name, accessorMethodName("getOut", fieldName));
                            outSetMethodNames.put(p.name, accessorMethodName("setOut", fieldName));
                            if (outFieldNames.containsKey(p.name)) {
                                Debug.logWarning("Service [" + serviceName + "] redefines OUT parameter [" + p.name + "] (used attribute instead of override, or redefined an auto attribute)", MODULE);
                            }
                            outFieldNames.put(p.name, "out" + fieldName);
                        }
                    } catch (IllegalArgumentException e) {
                        errorEntities.add(serviceName);
                        Debug.logError(e, MODULE);
                        hasError = true;
                        break;
                    }
                    // this is the short type: ie, java.lang.String -> String; java.util.HashMap -> HashMap, etc.
                    String type = p.type;
                    // replace $ with dot in some types that points to inner classes
                    type = type.replaceAll("\\$", ".");
                    String shortType = type;
                    int idx = shortType.lastIndexOf(".");

                    if (idx > 0) {
                        // need to work around some compilation issues for service that are using a type defined in their own
                        // application, fallback to the generic Object
                        if (shortType.startsWith("org.opentaps.") && !shortType.startsWith("org.opentaps.common")) {
                            Debug.logWarning("Service [" + serviceName + "] field [" + p.name + "] has type [" + type + "] which is not available yet, falling back to 'Object'.", MODULE);
                            shortType = "Object";
                            fieldRealTypes.put(p.name, type);
                        } else {
                            shortType = shortType.substring(idx + 1);
                            types.add(type);
                        }
                    } else {
                        // some short types also need to be imported
                        if (Arrays.asList("List", "Map", "Set", "Date").contains(type)) {
                            types.add("java.util." + type);
                        } else if (Arrays.asList("GenericValue", "GenericEntity", "GenericPK").contains(type)) {
                            types.add("org.ofbiz.entity." + type);
                        } else if (Arrays.asList("Timestamp").contains(type)) {
                            types.add("java.sql." + type);
                        } else if ("BigDecimal".equals(type)) {
                            types.add("java.math." + type);
                        }
                    }

                    // check that if a parameter is defined IN and OUT it has the same type
                    if (fieldTypes.containsKey(p.name) && !shortType.equals(fieldTypes.get(p.name))) {
                        // this can happen if the service redefined an internal parameter
                        ModelParam p2 = fieldModels.get(p.name);
                        if (p2 != null && (p2.internal || p.internal)) {
                            Debug.logWarning("Service [" + serviceName + "] has parameter [" + p.name + "] with type [" + shortType + "] which conflicts with an internal service parameter of the same name and with type [" + fieldTypes.get(p.name) + "]", MODULE);
                        } else {
                            errorEntities.add(serviceName);
                            Debug.logError("Service [" + serviceName + "] has parameter [" + p.name + "] with type [" + shortType + "] which conflicts with another definition of this parameter that has type [" + fieldTypes.get(p.name) + "]", MODULE);
                            hasError = true;
                            break;
                        }
                    }

                    fieldTypes.put(p.name, shortType);
                    fieldModels.put(p.name, p);
                }

                if (hasError) {
                    continue;
                }

                entityInfo.put("name", serviceName);
                entityInfo.put("className", serviceClassName);
                entityInfo.put("types", types);
                entityInfo.put("fieldTypes", fieldTypes);
                entityInfo.put("fieldRealTypes", fieldRealTypes);
                entityInfo.put("inFieldNames", inFieldNames);
                entityInfo.put("outFieldNames", outFieldNames);
                entityInfo.put("inGetMethodNames", inGetMethodNames);
                entityInfo.put("inSetMethodNames", inSetMethodNames);
                entityInfo.put("outGetMethodNames", outGetMethodNames);
                entityInfo.put("outSetMethodNames", outSetMethodNames);
                entityInfo.put("paramNames", paramNames);
                entityInfo.put("inParams", inParams);
                entityInfo.put("outParams", outParams);
                entityInfo.put("requiresAuth", modelService.auth);
                entityInfo.put("requiresNewTransaction", modelService.requireNewTransaction);
                entityInfo.put("usesTransaction", modelService.useTransaction);
                entityInfo.put("serviceDescription", modelService.description);
                entityInfo.put("serviceEngine", modelService.engineName);
                entityInfo.put("serviceLocation", modelService.location);
                entityInfo.put("serviceInvoke", modelService.invoke);
                String serviceDefPath = null;
                try {
                    serviceDefPath = UtilURL.getOfbizHomeRelativeLocation(new URL(modelService.definitionLocation));
                } catch (MalformedURLException e) {
                    Debug.logError(e.getMessage(), MODULE);
                }
                entityInfo.put("serviceDefinition", serviceDefPath);

                // render it as FTL
                Writer writer = new StringWriter();
                try {
                    FreeMarkerWorker.renderTemplateAtLocation(serviceTemplate, entityInfo, writer);
                } catch (MalformedURLException e) {
                    Debug.logError(e, MODULE);
                    errorEntities.add(serviceName);
                    break;
                } catch (TemplateException e) {
                    Debug.logError(e, MODULE);
                    errorEntities.add(serviceName);
                    break;
                } catch (IOException e) {
                    Debug.logError(e, MODULE);
                    errorEntities.add(serviceName);
                    break;
                } catch (IllegalArgumentException e) {
                    Debug.logError(e, MODULE);
                    errorEntities.add(serviceName);
                    break;
                }

                // write it as a Java file
                File file = new File(serviceOutputPath + serviceClassName + fileExtension);
                try {
                    FileUtils.writeStringToFile(file, writer.toString(), "UTF-8");
                } catch (IOException e) {
                    Debug.logError(e, "Aborting, error writing file " + serviceOutputPath + serviceClassName + fileExtension, MODULE);
                    errorEntities.add(serviceName);
                    break;
                }
                // increment counter
                generatedServices.put(serviceClassName, serviceName);
                totalGeneratedClasses++;
            }
        } else {
            Debug.logImportant("=-=-=-=-=-=-= No service found.", MODULE);
        }

        // error summary
        if (errorEntities.size() > 0) {
            Debug.logImportant("The following services could not be generated:", MODULE);
            for (String name : errorEntities) {
                Debug.logImportant(name, MODULE);
            }
        }

        Debug.logImportant("=-=-=-=-=-=-= Finished the POJO services generation with " + totalGeneratedClasses + " classes generated.", MODULE);

        if (errorEntities.size() > 0) {
            return false;
        }

        return true;
    }


    private boolean generateEntities() throws ContainerException {

        // check the entity template file
        File entityTemplateFile = new File(entityTemplate);
        if (!entityTemplateFile.canRead()) {
            throw new ContainerException("Unable to read entity template file: [" + entityTemplate + "]");
        }

        // check the pk template file
        File pkTemplateFile = new File(pkTemplate);
        if (!pkTemplateFile.canRead()) {
            throw new ContainerException("Unable to read pk template file: [" + pkTemplate + "]");
        }

        // check the pk template file
        File cfgTemplateFile = new File(hibernateCfgTemplate);
        if (!cfgTemplateFile.canRead()) {
            throw new ContainerException("Unable to read hibernate cfg template file: [" + hibernateCfgTemplate + "]");
        }

        // check the pk template file
        File pkBridgeTemplateFile = new File(pkBridgeTemplate);
        if (!pkBridgeTemplateFile.canRead()) {
            throw new ContainerException("Unable to read pk bridge template file: [" + pkBridgeTemplate + "]");
        }

        // get entities list
        ModelReader modelReader = delegator.getModelReader();
        Collection<String> entities = null;
        try {
            entities = modelReader.getEntityNames();
        } catch (GenericEntityException e) {
            Debug.logError(e, "Error getting the entities list from delegator " + delegatorNameToUse, MODULE);
        }

        // record errors for summary
        List<String> errorEntities = new LinkedList<String>();
        // record view entities
        List<String> viewEntities = new LinkedList<String>();

        int totalGeneratedClasses = 0;
        if (entities != null && entities.size() > 0) {
            Debug.logImportant("=-=-=-=-=-=-= Generating the POJO entities ...", MODULE);
            for (String entityName : entities) {
                ModelEntity modelEntity = null;
                try {
                    modelEntity = modelReader.getModelEntity(entityName);
                    if (modelEntity != null && modelEntity instanceof ModelViewEntity) {
                        viewEntities.add(entityName);
                    }
                } catch (GenericEntityException e) {
                    Debug.logError(e, MODULE);
                }
            }
            List<String> needIndexEntities = new LinkedList<String>();
            for (String entityName : entities) {
                //retrieve entityName
                if (entitySearchProperties != null && entitySearchProperties.containsKey(entityName)) {
                    if (entitySearchProperties.getProperty(entityName).equals("index")) {
                        needIndexEntities.add(entityName);
                    }
                }
            }
            for (String entityName : entities) {
                ModelEntity modelEntity = null;
                try {
                    modelEntity = modelReader.getModelEntity(entityName);
                } catch (GenericEntityException e) {
                    Debug.logError(e, MODULE);
                }

                if (modelEntity == null) {
                    errorEntities.add(entityName);
                    Debug.logError("Error getting the entity model from delegator " + delegatorNameToUse + " and entity " + entityName, MODULE);
                    continue;
                }
                // could be an object, but is just a Map for simplicity
                Map<String, Object> entityInfo = new HashMap<String, Object>();
                // entity columns what used in entity field
                List<String> entityColumns = new ArrayList<String>();
                boolean isView = false;
                // justify the entity whether need index or not
                boolean isNeedIndex = needIndexEntities.contains(entityName);

                if ((modelEntity instanceof ModelViewEntity)) {
                    isView = true;
                    // the view entity cannot index
                    isNeedIndex = false;
                }
                // get name of the entity
                entityName = modelEntity.getEntityName();
                entityInfo.put("name", entityName);
                entityInfo.put("tableName", modelEntity.getPlainTableName());
                entityInfo.put("isView", isView);
                entityInfo.put("isNeedIndex", isNeedIndex);
                entityInfo.put("needIndexEntities", needIndexEntities);
                entityInfo.put("resourceName", modelEntity.getDefaultResourceName());
                entityInfo.put("primaryKeys", modelEntity.getPkFieldNames());
                entityInfo.put("viewEntities" , viewEntities);
                // get all the fields of the entity which become members of the class
                List<String> fieldNames = modelEntity.getAllFieldNames();
                entityInfo.put("fields", fieldNames);

                //get all the columns of the entity
                Map<String, String> columnNames = new TreeMap<String, String>();
                for (String fieldName : fieldNames) {
                    String columnName = modelEntity.getColNameOrAlias(fieldName);
                    columnNames.put(fieldName, columnName);
                    entityColumns.add(columnName);
                }
                entityInfo.put("columnNames", columnNames);
                // distinct types for the import section
                Set<String> types = new TreeSet<String>();

                // a type for each field during declarations
                Map<String, String> fieldTypes = new TreeMap<String, String>();
                // indicate  declarations
                Map<String, String> indexWeights = new TreeMap<String, String>();
                Map<String, String> tokenTypes = new TreeMap<String, String>();
                // names of the get and set methods
                Map<String, String> getMethodNames = new TreeMap<String, String>();
                Map<String, String> setMethodNames = new TreeMap<String, String>();
                Map<String, List<String>> validatorMaps = new TreeMap<String, List<String>>();

                // now go through all the fields
                boolean hasError = false;
                for (String fieldName : fieldNames) {
                    // use the model field to figure out the Java type
                    ModelField modelField = modelEntity.getField(fieldName);
                    String type = null;
                    try {
                        // this converts String to java.lang.String, Timestamp to java.sql.Timestamp, etc.
                        ModelFieldType fieldType = delegator.getEntityFieldType(modelEntity, modelField.getType());
                        if (fieldType == null) {
                            throw new GenericEntityException("No field type defined for field " + fieldName);
                        }
                        type = ObjectType.loadClass(fieldType.getJavaType()).getName();
                    } catch (Exception e) {
                        Debug.logError(e, MODULE);
                    }

                    if (type == null) {
                        errorEntities.add(entityName);
                        Debug.logError("Error getting the type of the field " + fieldName + " of entity " + entityName + " for delegator " + delegatorNameToUse, MODULE);
                        hasError = true;
                        break;
                    }

                    // make all Doubles BigDecimals -- in the entity model fieldtype XML files, all floating points are defined as Doubles
                    // and there is a GenericEntity.getBigDecimal method which converts them to BigDecimal.  We will make them all BigDecimals
                    // and then use the Entity.convertToBigDecimal method
                    if ("java.lang.Double".equals(type)) {
                        type = "java.math.BigDecimal";
                    }

                    // make all Object field to byte[]
                    if ("java.lang.Object".equals(type) || "java.sql.Blob".equals(type)) {
                        type = "byte[]";
                    } else {
                        types.add(type);
                    }
                    // this is the short type: ie, java.lang.String -> String; java.util.HashMap -> HashMap, etc.
                    String shortType = type;
                    int idx = type.lastIndexOf(".");
                    if (idx > 0) {
                        shortType = type.substring(idx + 1);
                    }
                    fieldTypes.put(fieldName, shortType);
                    // if entitysearch.properties contain the field, then add boost annotation to set index weight
                    String indexFieldKey = entityName + "." + fieldName;
                    if (entitySearchProperties != null && entitySearchProperties.containsKey(indexFieldKey)) {
                        String[] indexValue = entitySearchProperties.getProperty(indexFieldKey).split(",");
                        String weight = indexValue[0];
                        String tokenType = indexValue.length > 1 ? indexValue[1] : "TOKENIZED";
                        indexWeights.put(fieldName, weight);
                        tokenTypes.put(fieldName, tokenType);
                    }
                    // accessor method names
                    try {
                        getMethodNames.put(fieldName, getterName(fieldName));
                        setMethodNames.put(fieldName, setterName(fieldName));
                    } catch (IllegalArgumentException e) {
                        errorEntities.add(entityName);
                        Debug.logError(e, MODULE);
                        hasError = true;
                        break;
                    }
                    if (modelField.getValidatorsSize() > 0) {
                        List<String> validators = new ArrayList<String>();
                        for (int i = 0; i < modelField.getValidatorsSize(); i++) {
                            String validator = modelField.getValidator(i);
                            validators.add(validator);
                        }
                        validatorMaps.put(fieldName, validators);
                    }
                }

                if (hasError) {
                    continue;
                }
                // get the relations
                List<Map<String, String>> relations = new ArrayList<Map<String, String>>();
                Iterator<ModelRelation> relationsIter = modelEntity.getRelationsIterator();
                while (relationsIter.hasNext()) {
                    ModelRelation modelRelation = relationsIter.next();
                    Map<String, String> relationInfo = new TreeMap<String, String>();
                    relationInfo.put("entityName", modelRelation.getRelEntityName());
                    // the string to put in the getRelated() method, title is "" if null
                    relationInfo.put("relationName", modelRelation.getTitle() + modelRelation.getRelEntityName());
                    // the names are pluralized for relation of type many
                    String accessorName = modelRelation.getTitle() + modelRelation.getRelEntityName();
                    // title may have white spaces, remove them to use as an attribute in the java class
                    accessorName = accessorName.replaceAll(" ", "");
                    // justify if need create hibernate mapping annotation
                    String isNeedMapping = "Y";
                    if ("many".equals(modelRelation.getType())) {
                        relationInfo.put("type", "many");
                        try {
                            accessorName = Noun.pluralOf(accessorName, Locale.ENGLISH);
                        } catch (Exception e) {
                            Debug.logWarning("For entity " + entityName + ", could not get the plural of " + accessorName + ", falling back to " + accessorName + "s.", MODULE);
                            accessorName = accessorName + "s";
                        }
                        // if the relation class is extendClass, such as PartyGroup and not oneToOne relation, then don't mapping this relation for hibernate
                        if (extendClassNames.contains(entityName)) {
                            isNeedMapping = "N";
                        }
                    } else {
                        // we do not care about the difference between one and one-nofk types, because we can use one-to-one to cascade
                        relationInfo.put("type", "one");
                        // mark isOneToOne field for Entity ftl, this is useful for hibernate search feature
                        try {
                            String joinField = modelRelation.getKeyMap(0).getFieldName();
                            ModelEntity refEntity = modelReader.getModelEntity(modelRelation.getRelEntityName());
                            if (modelRelation.isAutoRelation() && refEntity.getPkFieldNames().size() == 1 && modelEntity.getPkFieldNames().size() == 1
                                    && (refEntity.getPkFieldNames().contains(modelEntity.getPkFieldNames()) || modelEntity.getPkFieldNames().contains(joinField))) {
                                relationInfo.put("isOneToOne", "Y");
                            } else {
                                relationInfo.put("isOneToOne", "N");
                                // if the relation class is extendClass, such as PartyGroup and not oneToOne relation, then don't mapping this relation for hibernate
                                if (extendClassNames.contains(modelRelation.getRelEntityName())) {
                                    isNeedMapping = "N";
                                }
                            }

                        } catch (Exception e) {
                            Debug.logWarning("For entity " + entityName + ", could not get the relative Entity " + modelRelation.getRelEntityName() + ".", MODULE);
                            accessorName = accessorName + "s";
                        }
                    }

                    // check if the accessor conflicts with an already defined field
                    String fieldName = accessorName.substring(0, 1).toLowerCase() + accessorName.substring(1);
                    if (fieldNames.contains(fieldName)) {
                        String oldFieldName = fieldName;
                        accessorName = "Related" + accessorName;
                        fieldName = accessorName.substring(0, 1).toLowerCase() + accessorName.substring(1);
                        Debug.logWarning("For entity " + entityName + ", field " + oldFieldName + " already defined, using " + fieldName + " instead.", MODULE);
                    }
                    String fkName = modelRelation.getFkName();
                    relationInfo.put("accessorName", accessorName);
                    relationInfo.put("fieldName", fieldName);
                    relationInfo.put("fkName", fkName);
                    relationInfo.put("isNeedMapping", isNeedMapping);
                    if (modelRelation.getKeyMapsSize() == 1) {
                        //relation child Entity field
                        String joinField = "";
                        //relation parent Entity field
                        String mappedByFieldId = "";
                        String columnName = "";
                        if ("many".equals(modelRelation.getType())) {
                            joinField = modelRelation.getKeyMap(0).getRelFieldName();
                            mappedByFieldId  = modelRelation.getKeyMap(0).getFieldName();
                            try {
                                // get relative column name
                                ModelEntity refEntity = modelReader.getModelEntity(modelRelation.getRelEntityName());
                                columnName = refEntity.getColNameOrAlias(joinField);
                            } catch (GenericEntityException e) {
                                Debug.logError(e, MODULE);
                            }
                        } else {
                            joinField = modelRelation.getKeyMap(0).getFieldName();
                            mappedByFieldId  = modelRelation.getKeyMap(0).getRelFieldName();
                            // get joinField column
                            columnName = modelEntity.getColNameOrAlias(joinField);
                        }
                        //adjust this column if use for other relation, if used we should mapped with insert="false" update="false"
                        if (!entityColumns.contains(columnName)) {
                            entityColumns.add(columnName);
                            relationInfo.put("isRepeated", "N");
                        } else {
                            relationInfo.put("isRepeated", "Y");
                        }
                        relationInfo.put("joinColumn", columnName);
                        //if this property is collection, and have only one primary key, and find it in child property
                        // this is useful for hibernate's cascading feature
                        if ("many".equals(modelRelation.getType()) && modelEntity.getPkFieldNames().size() == 1) {
                            try {
                                ModelEntity refEntity = modelReader.getModelEntity(modelRelation.getRelEntityName());
                                // manyToOne field
                                String refField = "";
                                Iterator<ModelRelation> it = refEntity.getRelationsIterator();
                                while (it.hasNext()) {
                                    ModelRelation relation = it.next();
                                    //if relation map modelRelation
                                    if (relation.getRelEntityName().equals(entityName) && relation.getKeyMapsSize() == 1
                                            && relation.getKeyMap(0).getFieldName().equals(joinField)) {
                                        //get access name
                                        String aName = relation.getTitle() + relation.getRelEntityName();
                                        //get ref field name
                                        String relateField = aName.substring(0, 1).toLowerCase() + aName.substring(1);
                                        // replace all space characters
                                        relateField = relateField.replaceAll("\\s*", "");
                                        // if relate entity contain relate field
                                        if (refEntity.getAllFieldNames().contains(relateField + "Id") || refEntity.getAllFieldNames().containsAll(modelEntity.getPkFieldNames())) {
                                            refField = relateField;
                                            break;
                                        }                                        
                                    }
                                }
                                if (!refField.equals("") && (refEntity.getPkFieldNames().size() == 1 || refEntity.getPkFieldNames().contains(joinField))) {
                                    relationInfo.put("itemName", itemName(fieldName));
                                    relationInfo.put("refField", refField);
                                    // if this collection contains current pk, it should be a cascade collection property
                                    if (refEntity.getPkFieldNames().contains(joinField)) {
                                        //put add item method of collection
                                        relationInfo.put("addMethodName", addName(fieldName));
                                        //put remove item method of collection
                                        relationInfo.put("removeMethodName", removeName(fieldName));
                                        //put clear item method of collection
                                        relationInfo.put("clearMethodName", clearName(fieldName));
                                    }
                                }
                            } catch (GenericEntityException e) {
                                Debug.logError(e, MODULE);
                            }
                        }
                    }
                    relations.add(relationInfo);
                }
                entityInfo.put("relations", relations);
                entityInfo.put("types", types);
                entityInfo.put("fieldTypes", fieldTypes);
                entityInfo.put("getMethodNames", getMethodNames);
                entityInfo.put("setMethodNames", setMethodNames);
                entityInfo.put("validatorMaps", validatorMaps);
                entityInfo.put("indexWeights", indexWeights);
                entityInfo.put("tokenTypes", tokenTypes);

                // map view-entity to @NamedNativeQuery
                if (isView) {
                    //pk fields of the first entity as view-entity pk
                    List<String> viewEntityPks = new LinkedList<String>();
                    StringBuffer query = new StringBuffer();
                    query.append("SELECT ");
                    // field <-> column alias mapping
                    Map<String, String> fieldMapAlias = new TreeMap<String, String>();
                    // field <-> column name mapping
                    Map<String, String> fieldMapColumns = new TreeMap<String, String>();
                    List<String> relationAlias = new LinkedList<String>();
                    ModelViewEntity modelViewEntity = (ModelViewEntity) modelEntity;
                    Iterator<ModelAlias> aliasIter = modelViewEntity.getAliasesIterator();
                    // iterate all fields, construct select sentence
                    while (aliasIter.hasNext()) {
                        ModelAlias alias = aliasIter.next();
                        String columnName = ModelUtil.javaNameToDbName(alias.getField());
                        if (fieldMapAlias.size() > 0) {
                            query.append(",");
                        }
                        //iterator field, such as "P.PARTY_ID \"partyId\"
                        query.append(alias.getEntityAlias() + "." + columnName + " AS \\\"" + alias.getField() + "\\\"");
                        String colAlias = alias.getColAlias();
                        String field = ModelUtil.dbNameToVarName(alias.getColAlias());
                        // put field-colAlias mapping
                        fieldMapAlias.put(field, colAlias);
                        // put field-column mapping
                        fieldMapColumns.put(field, alias.getEntityAlias() + "." + columnName);
                    }
                    // iterate all entities, construct from sentence
                    for (int i = 0; i < modelViewEntity.getAllModelMemberEntities().size(); i++) {
                        ModelMemberEntity modelMemberEntity = (ModelMemberEntity) modelViewEntity.getAllModelMemberEntities().get(i);
                        String tableName = ModelUtil.javaNameToDbName(modelMemberEntity.getEntityName());
                        String tableAlias = modelMemberEntity.getEntityAlias();
                        relationAlias.add(tableAlias);
                        if (i == 0) {
                            // main table
                            query.append(" FROM " + tableName + " " + tableAlias);
                            // get all the pk fields of the first entity which become members of the class
                            try {
                                ModelEntity firstEntity = modelReader.getModelEntity(modelMemberEntity.getEntityName());
                                Iterator<ModelField> it = firstEntity.getPksIterator();
                                while (it.hasNext()) {
                                    ModelField field = it.next();
                                    //just need one pk, else secondary pk would be null
                                    if (fieldMapAlias.containsKey(field.getName()) && viewEntityPks.size()==0) {
                                        viewEntityPks.add(field.getName());
                                    }
                                }
                            } catch (GenericEntityException e) {
                                Debug.logError(e, MODULE);
                            }
                        } else {
                            // join table
                            Iterator<ModelViewLink> viewLinkIter = modelViewEntity.getViewLinksIterator();
                            while (viewLinkIter.hasNext()) {
                                ModelViewLink modelViewLink = viewLinkIter.next();
                                if (relationAlias.contains(modelViewLink.getEntityAlias())
                                        && relationAlias.contains(modelViewLink.getRelEntityAlias())
                                        && (tableAlias.equals(modelViewLink.getEntityAlias()) || tableAlias.equals(modelViewLink.getRelEntityAlias()))
                                ) {
                                    // adjust if left join or inner join
                                    String joinType = modelViewLink.isRelOptional() ? " LEFT JOIN " : " INNER JOIN ";
                                    query.append(joinType + tableName + " " + tableAlias);
                                    for (int k = 0; k < modelViewLink.getKeyMapsSize(); k++) {
                                        ModelKeyMap modelKeyMap = modelViewLink.getKeyMap(k);
                                        //get join conditions
                                        String joinCondition = modelViewLink.getEntityAlias() + "." + ModelUtil.javaNameToDbName(modelKeyMap.getFieldName())
                                        + " = " + modelViewLink.getRelEntityAlias() + "." + ModelUtil.javaNameToDbName(modelKeyMap.getRelFieldName());
                                        if (k == 0) {
                                            // if it is first join condition
                                            query.append(" ON " + joinCondition);
                                        } else {
                                            // if it isn't first join condition
                                            query.append(" AND " + joinCondition);
                                        }
                                    }
                                }
                            }
                        }
                    }
                    // if no pk for this viewEntity, then give a random field, because hibernate annotation need at least one pk filed.
                    if (viewEntityPks.size() == 0) {
                        viewEntityPks.add(fieldNames.get(0));
                    }
                    entityInfo.put("query", query.toString());
                    entityInfo.put("fieldMapAlias", fieldMapAlias);
                    entityInfo.put("fieldMapColumns", fieldMapColumns);
                    entityInfo.put("viewEntityPks", viewEntityPks);
                }
                // render it as FTL
                Writer writer = new StringWriter();
                try {
                    FreeMarkerWorker.renderTemplateAtLocation(entityTemplate, entityInfo, writer);
                } catch (MalformedURLException e) {
                    Debug.logError(e, MODULE);
                    errorEntities.add(entityName);
                    break;
                } catch (TemplateException e) {
                    Debug.logError(e, MODULE);
                    errorEntities.add(entityName);
                    break;
                } catch (IOException e) {
                    Debug.logError(e, MODULE);
                    errorEntities.add(entityName);
                    break;
                } catch (IllegalArgumentException e) {
                    Debug.logError(e, MODULE);
                    errorEntities.add(entityName);
                    break;
                }

                // write it as a Java file
                String path = entityOutputPath;
                if (baseEntities.contains(entityName)) {
                    Debug.logInfo("Entity [" + entityName + "] is configured as a base entity.", MODULE);
                    path = baseEntityOutputPath;
                }
                String fileName = path + entityName + fileExtension;

                File file = new File(fileName);
                try {
                    FileUtils.writeStringToFile(file, writer.toString(), "UTF-8");
                } catch (IOException e) {
                    Debug.logError(e, "Aborting, error writing file " + fileName, MODULE);
                    errorEntities.add(entityName);
                    break;
                }
                // pk class info
                Map<String, Object> pkInfo = null;
                if (!isView && modelEntity.getPkFieldNames().size() > 1) {
                    //if entity has more than one pk
                    pkInfo = new TreeMap<String, Object>();
                    // get all the pk fields of the entity which become members of the class
                    List<String> primaryKeys = modelEntity.getPkFieldNames();
                    // a type for each pk during declarations
                    Map<String, String> pkTypes = new TreeMap<String, String>();
                    // names of the get and set methods
                    Map<String, String> getPkMethodNames = new TreeMap<String, String>();
                    Map<String, String> setPkMethodNames = new TreeMap<String, String>();
                    // distinct types for the import section
                    Set<String> pkFieldTypes = new TreeSet<String>();
                    Iterator<ModelField> pksIter = modelEntity.getPksIterator();
                    while (pksIter.hasNext()) {
                        String type = null;
                        ModelField modelField = pksIter.next();
                        getPkMethodNames.put(modelField.getName(), getterName(modelField.getName()));
                        setPkMethodNames.put(modelField.getName(), setterName(modelField.getName()));
                        try {
                            // this converts String to java.lang.String, Timestamp to java.sql.Timestamp, etc.
                            type = ObjectType.loadClass(delegator.getEntityFieldType(modelEntity, modelField.getType()).getJavaType()).getName();
                        } catch (Exception e) {
                            Debug.logError(e, MODULE);
                        }

                        if (type == null) {
                            errorEntities.add(entityName);
                            Debug.logError("Error getting the type of the field " + modelField.getName() + " of entity " + entityName + " for delegator " + delegatorNameToUse, MODULE);
                            hasError = true;
                            break;
                        }

                        // make all Doubles BigDecimals -- in the entity model fieldtype XML files, all floating points are defined as Doubles
                        // and there is a GenericEntity.getBigDecimal method which converts them to BigDecimal.  We will make them all BigDecimals
                        // and then use the Entity.convertToBigDecimal method
                        if ("java.lang.Double".equals(type)) {
                            type = "java.math.BigDecimal";
                        }
                        pkFieldTypes.add(type);
                        // this is the short type: ie, java.lang.String -> String; java.util.HashMap -> HashMap, etc.
                        String shortType = type;
                        int idx = type.lastIndexOf(".");
                        if (idx > 0) {
                            shortType = type.substring(idx + 1);
                        }
                        pkTypes.put(modelField.getName(), shortType);
                    }
                    pkInfo.put("pkName", modelEntity.getEntityName() + "Pk");
                    pkInfo.put("entityName", modelEntity.getEntityName());
                    pkInfo.put("primaryKeys", primaryKeys);
                    pkInfo.put("pkTypes", pkTypes);
                    pkInfo.put("getPkMethodNames", getPkMethodNames);
                    pkInfo.put("setPkMethodNames", setPkMethodNames);
                    pkInfo.put("pkFieldTypes", pkFieldTypes);
                    pkInfo.put("columnNames", columnNames);
                }
                if (pkInfo != null) {
                    // render pk class as FTL
                    Writer pkWriter = new StringWriter();
                    try {
                        FreeMarkerWorker.renderTemplateAtLocation(pkTemplate, pkInfo, pkWriter);
                    } catch (MalformedURLException e) {
                        Debug.logError(e, MODULE);
                        errorEntities.add(entityName);
                        break;
                    } catch (TemplateException e) {
                        Debug.logError(e, MODULE);
                        errorEntities.add(entityName);
                        break;
                    } catch (IOException e) {
                        Debug.logError(e, MODULE);
                        errorEntities.add(entityName);
                        break;
                    } catch (IllegalArgumentException e) {
                        Debug.logError(e, MODULE);
                        errorEntities.add(entityName);
                        break;
                    }

                    // write it as a Java file (PK)
                    File pkFile = new File(entityOutputPath + entityName + "Pk" + fileExtension);
                    try {
                        FileUtils.writeStringToFile(pkFile, pkWriter.toString(), "UTF-8");
                    } catch (IOException e) {
                        Debug.logError(e, "Aborting, error writing file " + entityOutputPath + entityName + "Pk" + fileExtension, MODULE);
                        errorEntities.add(entityName);
                        break;
                    }

                    // render pk bridge class as FTL
                    Writer pkBridgeWriter = new StringWriter();
                    try {
                        FreeMarkerWorker.renderTemplateAtLocation(pkBridgeTemplate, pkInfo, pkBridgeWriter);
                    } catch (MalformedURLException e) {
                        Debug.logError(e, MODULE);
                        errorEntities.add(entityName);
                        break;
                    } catch (TemplateException e) {
                        Debug.logError(e, MODULE);
                        errorEntities.add(entityName);
                        break;
                    } catch (IOException e) {
                        Debug.logError(e, MODULE);
                        errorEntities.add(entityName);
                        break;
                    } catch (IllegalArgumentException e) {
                        Debug.logError(e, MODULE);
                        errorEntities.add(entityName);
                        break;
                    }

                    // write it as a Java file (PK)
                    File pkBridgeFile = new File(entityOutputPath + "/bridge/" + entityName + "PkBridge" + fileExtension);
                    try {
                        FileUtils.writeStringToFile(pkBridgeFile, pkBridgeWriter.toString(), "UTF-8");
                    } catch (IOException e) {
                        Debug.logError(e, "Aborting, error writing file " + entityOutputPath + "/bridge/" + entityName + "PkBridge" + fileExtension, MODULE);
                        errorEntities.add(entityName);
                        break;
                    }
                }
                // increment counter
                totalGeneratedClasses++;

            }

            // render hibernate.cfg.xml by FTL
            Writer cfgWriter = new StringWriter();
            Map<String, Object> cfgInfo = new HashMap<String, Object>();
            cfgInfo.put("entities", entities);
            try {
                FreeMarkerWorker.renderTemplateAtLocation(hibernateCfgTemplate, cfgInfo, cfgWriter);
            } catch (MalformedURLException e1) {
                Debug.logError(e1, MODULE);
            } catch (TemplateException e1) {
                Debug.logError(e1, MODULE);
            } catch (IOException e1) {
                Debug.logError(e1, MODULE);
            }
            // write it as a hibernate.cfg.xml
            File hibernateFile = new File(hibernateCfgPath);
            try {
                FileUtils.writeStringToFile(hibernateFile, cfgWriter.toString(), "UTF-8");
            } catch (IOException e) {
                Debug.logError(e, "Aborting, error writing file " + hibernateCfgPath, MODULE);
            }
        } else {
            Debug.logImportant("=-=-=-=-=-=-= No entity found.", MODULE);
        }

        // error summary
        if (errorEntities.size() > 0) {
            Debug.logImportant("The following entities could not be generated:", MODULE);
            for (String name : errorEntities) {
                Debug.logImportant(name, MODULE);
            }
        }

        Debug.logImportant("=-=-=-=-=-=-= Finished the POJO entities generation with " + totalGeneratedClasses + " classes generated.", MODULE);

        if (errorEntities.size() > 0) {
            return false;
        }

        return true;
    }




    /** {@inheritDoc} */
    public void stop() throws ContainerException { }

    /**
     * Transform a string into a class name by capitalizing the first char and filtering invliad chars.
     * @param name the name to transform
     * @return a class name
     */
    public static String makeClassName(String name) {
        if (UtilValidate.isEmpty(name)) {
            throw new IllegalArgumentException("className called for null or empty string");
        } else {
            String className = Character.toUpperCase(name.charAt(0)) + name.substring(1);
            className = filterChar(className, ".");
            className = filterChar(className, "_");
            className = filterChar(className, "-");
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

    /**
     * Standardize accessor method names.  If fieldName is orderId, will return "prefix" + OrderId.
     *
     * @param prefix prefix to the method name, for example "get" or "set"
     * @param fieldName name of the field the accessor method access
     * @return the accessor method name
     */
    public static String accessorMethodName(String prefix, String fieldName) {
        if (UtilValidate.isEmpty(fieldName)) {
            throw new IllegalArgumentException("methodName called for null or empty fieldName");
        } else {
            return prefix + Character.toUpperCase(fieldName.charAt(0)) + fieldName.substring(1);
        }
    }

    /**
     * Standardize accessor collection method names.  If fieldName is items, will return "prefix" + Item.
     *
     * @param prefix prefix to the method name, for example "add" or "remove"
     * @param fieldName name of the field the accessor method access
     * @return the accessor method name
     */
    public static String accessorCollectionMethodName(String prefix, String fieldName) {
        if (UtilValidate.isEmpty(fieldName)) {
            throw new IllegalArgumentException("methodName called for null or empty fieldName");
        } else {
            return prefix + Character.toUpperCase(fieldName.charAt(0)) + fieldName.substring(1, fieldName.length() - 1);
        }
    }

    /**
     * Standardize getter method names.
     *
     * @param fieldName name of the field the accessor method access
     * @return the getter method name
     */
    public static String getterName(String fieldName) {
        return accessorMethodName("get", fieldName);
    }


    /**
     * Standardize setter method names.
     *
     * @param fieldName name of the field the accessor method access
     * @return the setter method name
     */
    public static String setterName(String fieldName) {
        return accessorMethodName("set", fieldName);
    }

    /**
     * Standardize add method of Collection property names.
     *
     * @param fieldName name of the field the add method access
     * @return the method name
     */
    public static String addName(String fieldName) {
        return accessorCollectionMethodName("add", fieldName);
    }

    /**
     * Standardize remove method of Collection property names.
     *
     * @param fieldName name of the field the remove method access
     * @return the method name
     */
    public static String removeName(String fieldName) {
        return accessorCollectionMethodName("remove", fieldName);
    }

    /**
     * Standardize clear method of Collection property names.
     *
     * @param fieldName name of the field the clear method access
     * @return the method name
     */
    public static String clearName(String fieldName) {
        return accessorCollectionMethodName("clear", fieldName);
    }

    /**
     * get item of Collection property names.
     *
     * @param fieldName name of the field the clear method access
     * @return the item field name
     */
    public static String itemName(String fieldName) {
        if (UtilValidate.isEmpty(fieldName)) {
            throw new IllegalArgumentException("methodName called for null or empty fieldName");
        } else {
            return fieldName.substring(0, fieldName.length() - 1);
        }
    }

    /**
     * get Pk name of Entity.
     *
     * @param entityName name of the Entity
     * @return the Pk field name
     */
    public static String getEntityPkName(String entityName) {
        if (entityName == null || entityName.equals("")) {
            return entityName;
        }
        String pkName = Character.toLowerCase(entityName.charAt(0)) + entityName.substring(1, entityName.length()) + "Id";
        return pkName;
    }
}
