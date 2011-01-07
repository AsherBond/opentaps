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

package org.opentaps.gwt.common.server.lookup;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

import javolution.util.FastList;
import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.entity.condition.EntityCondition;
import org.ofbiz.entity.condition.EntityFunction;
import org.ofbiz.entity.condition.EntityOperator;
import org.ofbiz.entity.transaction.GenericTransactionException;
import org.ofbiz.entity.transaction.TransactionUtil;
import org.opentaps.common.util.ConvertMapToString;
import org.opentaps.common.util.ICompositeValue;
import org.opentaps.domain.DomainsDirectory;
import org.opentaps.foundation.entity.EntityFieldInterface;
import org.opentaps.foundation.entity.EntityInterface;
import org.opentaps.foundation.entity.util.EntityListIterator;
import org.opentaps.foundation.exception.FoundationException;
import org.opentaps.foundation.repository.RepositoryException;
import org.opentaps.foundation.repository.RepositoryInterface;
import org.opentaps.foundation.repository.ofbiz.Repository;
import org.opentaps.gwt.common.client.lookup.Permissions;
import org.opentaps.gwt.common.client.lookup.UtilLookup;
import org.opentaps.gwt.common.server.InputProviderInterface;

/**
 * The base service to perform entity lookups.
 * This apply to both list and autocompleters.
 *
 * The main use of this class is to handle parameters related to pagination and excel export.
 *
 * - <code>UtilLookup.PARAM_NO_PAGER</code>: if set to "Y", pagination will not be performed
 * - <code>UtilLookup.PARAM_NO_BLANK</code>: if set to "Y", the final response should no include an empty record
 * - <code>UtilLookup.PARAM_EXPORT_EXCEL</code>: if set to "Y", no pagination will be performed and indicate that the response format should be an Excel spreadsheet.
 *
 * Parameters for pagination are:
 * - <code>UtilLookup.PARAM_PAGER_START</code>: the index of the first record in the response
 * - <code>UtilLookup.PARAM_PAGER_LIMIT</code>: the max number of records in the response
 * - <code>UtilLookup.PARAM_SORT_DIRECTION</code>: the sort direction either "ASC" or "DESC"
 * - <code>UtilLookup.PARAM_SORT_FIELD</code>: the name of the field on which to sort
 *
 * The main methods are:
 * - {@link #paginateResults}: returns a paginated list of entities from a list of entities according to the pagination parameters as well as
 *  set the internal state for {@link #getResults} and {@link #getResultTotalCount}.
 *
 * Usage:
 * <code>
 *  EntityLookupService service = new EntityLookupService(provider, UtilMisc.toList("field1", "field2"));
 *  SomeDomainRepository repository = service.getDomainsDirectory().getSomeDomain().getSomeDomainRepository();
 *  service.paginateResults(repository.findList(Product.class, conditions, service.getOrderBy()));
 * </code>
 *
 * It is also possible to use the find methods provided by this class which automatically sort and paginate the results and store any Exception to be included as a proper error response:
 * <code>
 *  EntityLookupService service = new EntityLookupService(provider, UtilMisc.toList("field1", "field2"));
 *  SomeDomainRepository repository = service.getDomainsDirectory().getSomeDomain().getSomeDomainRepository();
 *  service.setRepository(repository);
 *  service.findList(Product.class, conditions);
 * </code>
 *
 * @see org.opentaps.gwt.common.client.listviews.EntityListView
 */
public abstract class EntityLookupService {

    private static final String MODULE = EntityLookupService.class.getName();

    private InputProviderInterface provider;

    // fields to retreive from the database
    // the fields will also be returned in the JSON
    private List<String> fields;
    private List<Map<String, ConvertMapToString>> calculatedFields = FastList.newInstance();

    private Boolean isExportToExcel = false;
    private Boolean noPager = false;
    private Boolean noBlank = false;

    private List<? extends EntityInterface> results;
    private Integer resultTotalCount;
    private Pager pager;

    private RepositoryInterface repository;
    private Exception lastException;

    // global permissions.
    // they are indications for the consumer widget that those actions are available or not
    // but they are really enforced by the service.
    private final Permissions globalPermissions = new Permissions(false);

    // entity permissions, maps to the entity
    private final Map<EntityInterface, Permissions> entityPermissions = new HashMap<EntityInterface, Permissions>();

    /**
     * Creates a new <code>EntityLookupService</code> instance.
     *
     * @param provider an <code>InputProviderInterface</code>, used to read the parameters
     * @param fields the list of fields that will be in the response
     */
    protected EntityLookupService(InputProviderInterface provider, List<String> fields) {
        this.provider = provider;
        this.fields = new ArrayList<String>(fields);

        // special parameter to force disabling the pager and return all records
        if ("Y".equalsIgnoreCase(provider.getParameter(UtilLookup.PARAM_NO_PAGER))) {
            noPager = true;
        }

        // special parameter to not return a blank field as the first possible value
        if ("Y".equalsIgnoreCase(provider.getParameter(UtilLookup.PARAM_NO_BLANK))) {
            noBlank = true;
        }

        // get exportToExcel
        if ("Y".equalsIgnoreCase(provider.getParameter(UtilLookup.PARAM_EXPORT_EXCEL))) {
            isExportToExcel = true;
            noPager = true;
        }

        pager = new Pager(provider);
    }

    /**
     * Gets the <code>InputProviderInterface</code>.
     * @return an <code>InputProviderInterface</code> value
     */
    public InputProviderInterface getProvider() {
        return this.provider;
    }

    /**
     * Gets the <code>DomainsDirectory</code>.
     * @return an <code>DomainsDirectory</code> value
     */
    public DomainsDirectory getDomainsDirectory() {
        return this.provider.getDomainsDirectory();
    }

    /**
     * Gets the global permissions, defaults to deny all.
     * @return a <code>Permissions</code> value
     */
    public Permissions getGlobalPermissions() {
        return globalPermissions;
    }

    /**
     * Gets the permission set for the given entity.
     * Note that to get the effective permission for this entity you should use {@link #getEffectivePermissions(EntityInterface)} instead.
     * @param entity an <code>EntityInterface</code> value
     * @return a <code>Permissions</code> value
     */
    public Permissions getEntityPermissions(EntityInterface entity) {
        return entityPermissions.get(entity);
    }

    /**
     * Sets permissions for a given entity.
     * @param entity an <code>EntityInterface</code> value
     * @param permissions a <code>Permissions</code> value
     */
    public void setEntityPermissions(EntityInterface entity, Permissions permissions) {
        entityPermissions.put(entity, permissions);
    }

    /**
     * Gets the effective permission for the given entity, which is obtained by merging the base permissions with this entity specific permissions.
     * @param entity an <code>EntityInterface</code> value
     * @return a <code>Permissions</code> value
     */
    public Permissions getEffectivePermissions(EntityInterface entity) {
        return globalPermissions.merge(entityPermissions.get(entity));
    }

    /**
     * Gets the last stored <code>Exception</code>.
     * @return an <code>Exception</code> value
     */
    public Exception getLastException() {
        return lastException;
    }

    /**
     * Stores an <code>Exception</code> so that it can be included in the response.
     * @param e an <code>Exception</code> value
     */
    public void storeException(Exception e) {
        Debug.logError(e, MODULE);
        lastException = e;
    }

    /**
     * Sets a Repository for this service.
     * This is optional, but is there for consistency with {@link #getRepository}.
     * @param repository a <code>RepositoryInterface</code> value
     */
    public void setRepository(RepositoryInterface repository) {
        this.repository = repository;
    }

    /**
     * Gets the Repository associated to this service, if none then returns a default <code>Repository</code>.
     * @return a <code>RepositoryInterface</code> value
     * @exception RepositoryException if an error occurs
     */
    public RepositoryInterface getRepository() throws RepositoryException {
        if (repository == null) {
            repository = new Repository(provider.getInfrastructure());
        }
        return repository;
    }

    /**
     * Gets the <code>Pager</code>.
     * @return a <code>Pager</code> value
     */
    public Pager getPager() {
        return this.pager;
    }

    /**
     * Adds calculated field definition.
     * @param rule a <code>Map</code> of <code>String</code> to <code>ConvertMapToString</code>
     */
    public void makeCalculatedField(Map<String, ConvertMapToString> rule) {
        calculatedFields.add(rule);
    }

    /**
     * Gets the calculated field definitions.
     * @return a <code>List</code> of <code>Map</code> of <code>String</code> to <code>ConvertMapToString</code>
     */
    public List<Map<String, ConvertMapToString>> getCalculatedFields() {
        return calculatedFields;
    }

    /**
     * Checks if the response format should be an Excel spreadsheet, (defaults to false).
     * This is set to true if the parameter <code>UtilLookup.PARAM_EXPORT_EXCEL</code> of the request is set to <code>Y</code>.
     * @return a <code>Boolean</code> value
     */
    public Boolean isExportToExcel() {
        return isExportToExcel;
    }

    /**
     * Checks if the response should ignore pagination parameters and return the complete list of results.
     * @return a <code>Boolean</code> value
     */
    public Boolean ignorePager() {
        return noPager;
    }

    /**
     * Checks if the response should include a blank record.
     * @return a <code>Boolean</code> value
     */
    public Boolean allowBlank() {
        return !noBlank;
    }

    /**
     * Builds and returns ORDER BY clause.
     *
     * @return
     *     List of <code>String</code> where each element is entity field name plus optional sort direction.
     */
    protected List<String> getOrderBy() {
        List<String> orderBy = null;
        if (pager.hasSortParameters()) {
            // some fields may represent calculated field, in other words they may be
            // concatenation of a few real fields. Only way to get information about
            // these fields ask a class responsible for concatenation
            if (UtilValidate.isNotEmpty(calculatedFields)) {
                for (Map<String, ConvertMapToString> fieldDef : calculatedFields) {
                    String fieldName = fieldDef.keySet().iterator().next();
                    if (pager.getSortFieldName().equals(fieldName)) {
                        // field name that is candidate to be in ORDER BY is calculated and can not be
                        // directly used in expression. Ask converter for real names.
                        Object converter = fieldDef.get(fieldName);
                        if (converter != null && converter instanceof ICompositeValue) {
                            // retrieve collection of field names of given composite (calculated) value
                            LinkedHashSet<String> compositeField = ((ICompositeValue) converter).getFields();
                            for (String simpleField : compositeField) {
                                if (orderBy == null) {
                                    orderBy = FastList.newInstance();
                                }
                                orderBy.add(String.format("%1$s %2$s", simpleField, pager.getSortDirection()));
                            }
                        }
                    }
                }
            }

            if (orderBy == null) {
                // sort is real field, we can use it
                orderBy = pager.getSortList();
            }
        }

        return orderBy;
    }

    /**
     * Gets the list of fields to be retrieved from the database for the given entity.
     * @return the list of field names
     * @see #getFieldsOrdered
     */
    public List<String> getFields() {
        return fields;
    }

    /**
     * Adds a field to be retrieved from the database for the given entity.
     * @param field a <code>String</code> value
     */
    public void addField(String field) {
        fields.add(field);
    }

    /**
     * Gets the ordered list of fields, according to the given indexes.
     * Indexes are given in the parameters <code>_fieldName_idx</code>.
     * @return the ordered list of field names
     * @see #getFields
     */
    public List<String> getFieldsOrdered() {
        Map<Integer, String> fieldsAndIndexes = new HashMap<Integer, String>();
        List<String> extraFields = new ArrayList<String>();
        List<String> fieldsOrdered = new ArrayList<String>();
        // the variant using for store the max index
        int maxFieldIndex = 0;
        for (String fieldName : fields) {
            String fieldIdxStr = provider.getParameter("_" + fieldName + "_idx");
            if (fieldIdxStr != null) {
                Integer fieldIdx = Integer.valueOf(fieldIdxStr);
                fieldsAndIndexes.put(fieldIdx, fieldName);
                if (fieldIdx.intValue() > maxFieldIndex) {
                    maxFieldIndex = fieldIdx.intValue();
                }
            } else {
                extraFields.add(fieldName);
            }
        }

        // add fields of entity that don't appear in client grid and
        // were not included to fieldsToExport on previous step
        for (int n = 0; n < extraFields.size(); n++) {
            // add the extra fields after the indexed fields
            fieldsAndIndexes.put(new Integer(maxFieldIndex + n + 1), extraFields.get(n));
        }

        if (!fieldsAndIndexes.isEmpty()) {
            // build the ordered list of fields
            List<Integer> tmp = new ArrayList<Integer>();
            tmp.addAll(fieldsAndIndexes.keySet());
            Collections.sort(tmp);
            for (Integer idx : tmp) {
                fieldsOrdered.add(fieldsAndIndexes.get(idx));
            }
        } else {
            // if no parameters specifying fields were given, use the query fields
            fieldsOrdered.addAll(fields);
        }

        return fieldsOrdered;
    }

    /**
     * Gets the total count of records available, it is used by the client pager to calculate the total number of pages.
     * @return an <code>int</code> value
     * @see #setResultTotalCount
     */
    public int getResultTotalCount() {
        return resultTotalCount;
    }

    /**
     * Sets the total count of records available, it is used by the client pager to calculate the total number of pages.
     * @param count an <code>int</code> value
     * @see #getResultTotalCount
     */
    public void setResultTotalCount(int count) {
        resultTotalCount = count;
    }

    /**
     * Extracts results from an entity list iterator according to the pagination settings.
     * Sets the result total count according to the number of records in the list.
     * Sets the results to be returned to the sub list of records which index in the list match the pagination parameters.
     * For Excel exportation or if specifically said to, pagination is ignored and the whole data set is returned.
     *
     * This is the main method that service implementations must call.
     *
     * NOTE: This will close the iterator.
     *
     * @param <T> the entity class to return
     * @param iterator an <code>EntityListIterator</code>
     * @return the paginated list of entities
     * @exception FoundationException if an error occurs
     */
    public <T extends EntityInterface> List<T> paginateResults(EntityListIterator<T> iterator) throws FoundationException {
        if (ignorePager()) {
            setResults(iterator.getCompleteList());
            setResultTotalCount(getResults().size());
        } else {
            // note +1 because the iterator starts at 1
            setResults(iterator.getPartialList(pager.getPageStart() + 1, pager.getPageSize()));
            // do not use getResultsSizeAfterPartialList, instead go to last and take the index
            // getResultsSizeAfterPartialList tries to do a select count by incorrectly uses distinct on the first field
            // of the entity, instead of the PK, leading to incorrect count on some entities
            iterator.last();
            setResultTotalCount(iterator.currentIndex());
        }
        iterator.close();
        return getResults();
    }

    /**
     * Extracts results from a list of entities according to the pagination settings.
     * Sets the result total count according to the number of records in the list.
     * Sets the results to be returned to the sub list of records which index in the list match the pagination parameters.
     * For Excel exportation or if specifically said to, pagination is ignored and the whole data set is returned.
     *
     * This is the main method that service implementations must call.
     *
     * @param <T> the entity class to return
     * @param list a list of entities
     * @return the paginated list of entities
     */
    public <T extends EntityInterface> List<T> paginateResults(List<T> list) {
        setResultTotalCount(list.size());
        results = new ArrayList<T>();
        if (ignorePager()) {
            setResults(list);
        } else {
            if (pager.getPageStart() < list.size()) {
                setResults(list.subList(pager.getPageStart(), pager.getPageEnd(list.size())));
            }
        }
        return getResults();
    }

    /**
     * Gets the list of entities returned by the query.
     * @param <T> the entity class to return
     * @return list of entities
     */
    public <T extends EntityInterface> List<T> getResults() {
        return (List<T>) results;
    }

    /**
     * Sets the list of entities returned by the query.
     * @param list list of records to set
     */
    public void setResults(List<? extends EntityInterface> list) {
        results = list;
    }

    /**
     * Builds the query from the given list of <code>EntityCondition</code> and a list of filters that should
     *  be taken from the request parameters.
     * @param <T> the entity class
     * @param entityName class to find and return
     * @param conds initial list of conditions
     * @param filters list of parameter names to retrieve
     * @return the list of entities found, or <code>null</code> if an error occurred
     */
    protected <T extends EntityInterface> List<T> findListWithFilters(Class<T> entityName, List<EntityCondition> conds, List<String> filters) {
        for (String filter : filters) {
            if (provider.parameterIsPresent(filter)) {
                conds.add(EntityCondition.makeCondition(EntityFunction.UPPER_FIELD(filter), EntityOperator.LIKE, EntityFunction.UPPER("%" + provider.getParameter(filter) + "%")));
            }
        }

        return findList(entityName, EntityCondition.makeCondition(conds, EntityOperator.AND));
    }

    // some common find methods

    /* find all */

    /**
     * Find all entities.
     * @param <T> the entity class
     * @param entityName class to find and return
     * @return the list of entities found, <code>null</code> if an error occurs
     */
    public <T extends EntityInterface> List<T> findAll(Class<T> entityName) {
        return findList(entityName, (EntityCondition) null);
    }

    /* findList by Map of Field: value */

    /**
     * Find entities by conditions.
     * @param <T> the entity class
     * @param entityName class to find and return
     * @param conditions a Map of fields -> value that the entities must all match
     * @param paginate if the results should be paginated
     * @return the list of entities found, <code>null</code> if an error occurs
     */
    public <T extends EntityInterface> List<T> findList(Class<T> entityName, Map<? extends EntityFieldInterface<? super T>, Object> conditions, boolean paginate) {
        try {
            if (paginate) {
                boolean t = TransactionUtil.begin();
                paginateResults(getRepository().findIterator(entityName, conditions, getFields(), getOrderBy()));
                TransactionUtil.commit(t);
            } else {
                boolean p = noPager;
                noPager = true;
                setResults(getRepository().findList(entityName, conditions, getFields(), getOrderBy()));
                setResultTotalCount(getResults().size());
                noPager = p;
            }
            return getResults();
        } catch (RepositoryException e) {
            storeException(e);
            return null;
        } catch (GenericTransactionException e) {
            storeException(e);
            return null;
        } catch (FoundationException e) {
            storeException(e);
            return null;
        }
    }

    /**
     * Find entities by conditions.
     * @param <T> the entity class
     * @param entityName class to find and return
     * @param conditions a Map of fields -> value that the entities must all match
     * @return the list of entities found, <code>null</code> if an error occurs
     */
    public <T extends EntityInterface> List<T> findList(Class<T> entityName, Map<? extends EntityFieldInterface<? super T>, Object> conditions) {
        return findList(entityName, conditions, true);
    }

    /* findList by Conditions */

    /**
     * Find entities by conditions.
     * @param <T> the entity class
     * @param entityName class to find and return
     * @param condition the EntityCondition used to find the entities
     * @param paginate if the results should be paginated
     * @return the list of entities found, <code>null</code> if an error occurs
     */
    public <T extends EntityInterface> List<T> findList(Class<T> entityName, EntityCondition condition, boolean paginate) {
        try {
            if (paginate) {
                boolean t = TransactionUtil.begin();
                paginateResults(getRepository().findIterator(entityName, condition, getFields(), getOrderBy()));
                TransactionUtil.commit(t);
            } else {
                boolean p = noPager;
                noPager = true;
                setResults(getRepository().findList(entityName, condition, getFields(), getOrderBy()));
                setResultTotalCount(getResults().size());
                noPager = p;
            }
            return getResults();
        } catch (RepositoryException e) {
            storeException(e);
            return null;
        } catch (GenericTransactionException e) {
            storeException(e);
            return null;
        } catch (FoundationException e) {
            storeException(e);
            return null;
        }

    }

    /**
     * Find entities by conditions and paginate.
     * @param <T> the entity class
     * @param entityName class to find and return
     * @param condition the EntityCondition used to find the entities
     * @return the list of entities found, <code>null</code> if an error occurs
     */
    public <T extends EntityInterface> List<T> findList(Class<T> entityName, EntityCondition condition) {
        return findList(entityName, condition, true);
    }
}
