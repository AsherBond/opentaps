/*
 * Copyright (c) 2009 - 2009 Open Source Strategies, Inc.
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
package org.opentaps.search.domain;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.apache.lucene.analysis.KeywordAnalyzer;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.Query;
import org.hibernate.CacheMode;
import org.hibernate.Transaction;
import org.hibernate.search.FullTextQuery;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.Search;
import org.ofbiz.base.util.Debug;
import org.opentaps.domain.search.CommonSearchService;
import org.opentaps.domain.search.SearchRepositoryInterface;
import org.opentaps.foundation.entity.hibernate.Session;
import org.opentaps.foundation.infrastructure.InfrastructureException;
import org.opentaps.foundation.service.ServiceException;

/**
 * Base class for the Search Service implementations.
 */
public abstract class SearchService extends CommonSearchService {

    private static final String MODULE = SearchService.class.getName();


    private ArrayList<String> projectedFields;


    /**
     * Adds a field to the query projection, unless it was already set ion the projection.
     * Must be called before {@link #search}.
     * @param field a <code>String</code> value
     * @return the index of the field in the results as returned by {@link #getResults}
     */
    protected Integer addQueryProjectedField(String field) {
        if (projectedFields == null) {
            prepareQueryProjectedFields();
        }

        if (!projectedFields.contains(field)) {
            projectedFields.add(field);
        }
        return projectedFields.indexOf(field);
    }

    /**
     * Gets the index of a projected field previously added with {@link #addQueryProjectedField}.
     * @param field a <code>String</code> value
     * @return the index of the field in the results as returned by {@link #getResults}, or <code>-1</code> if the field is not defined in the projection
     */
    public Integer getQueryProjectedFieldIndex(String field) {
        if (projectedFields == null) {
            prepareQueryProjectedFields();
        }

        return projectedFields.indexOf(field);
    }

    /** {@inheritDoc} */
    public Set<String> getQueryProjectedFields() {
        Set<String> fields = new LinkedHashSet<String>();
        fields.addAll(Arrays.asList(FullTextQuery.OBJECT_CLASS, FullTextQuery.ID));
        return fields;
    }

    /**
     * Adds all fields from {@link #getQueryProjectedFields}.
     */
    protected void prepareQueryProjectedFields() {
        if (projectedFields == null) {
            projectedFields = new ArrayList<String>();
        }

        for (String field : getQueryProjectedFields()) {
            addQueryProjectedField(field);
        }
    }

    /**
     * Sets the query projection for the search methods.
     * By default the projection is set to all fields defined by {@link getQueryProjectedFields}, sub classes
     *  can override this method to define a custom projection and should then adapt to the returned
     *  list new format.
     * @param query the <code>FullTextQuery</code> instance
     */
    protected void setQueryProjection(FullTextQuery query) {
        prepareQueryProjectedFields();
        Debug.logInfo("setQueryProjection: with fields [" + projectedFields + "]", MODULE);
        query.setProjection(projectedFields.toArray(new String[projectedFields.size()]));
    }

    /**
     * Searches using the given query string in the given list of entity classes.
     * This uses <code>setQueryProjection()</code> which defines the format of the returned list.
     *
     * This also uses <code>getPageStart()</code> and <code>getPageSize()</code> for paginating the results.
     * Once the query has succeeded, <code>getResultSize()</code> will give the total number of matches (which
     *  because of pagination may not be equal to the resulting list size).
     *
     * The resulting list can also be accessed using <code>getResults()</code>.
     *
     * @param entityClasses the list of entity class to query
     * @param queryString the Lucene query string already formatted
     * @return a <code>List</code>, results of <code>fullTextQuery.list()</code> which is a <code>List<Object[]></code> where the first two fields are <code>{OBJECT_CLASS, ID}</code>
     * @throws ServiceException if an error occurs
     * @see #setQueryProjection
     */
    @SuppressWarnings("unchecked")
    protected List<Object[]> searchInEntities(Set<Class> entityClasses, String queryString) throws ServiceException {

        Debug.logInfo("searchInEntities: [" + entityClasses + "] query [" + queryString + "]", MODULE);

        Session session = null;
        FullTextSession fullTextSession = null;
        Transaction tx = null;

        try {
            session = getInfrastructure().getSession();
            fullTextSession = Search.getFullTextSession(session.getHibernateSession());
            fullTextSession.setCacheMode(CacheMode.IGNORE);
            tx = fullTextSession.beginTransaction();
            // create a native Lucene query
            QueryParser parser = new QueryParser("_hibernate_class", new KeywordAnalyzer());
            Query luceneQuery = parser.parse(queryString);
            // create the full text query
            FullTextQuery fullTextQuery = fullTextSession.createFullTextQuery(luceneQuery, entityClasses.toArray(new Class[entityClasses.size()]));
            setQueryProjection(fullTextQuery);
            // set pagination
            fullTextQuery.setFirstResult(getPageStart());
            fullTextQuery.setMaxResults(getPageSize());
            // perform the query
            setResults(fullTextQuery.list());
            setResultSize(fullTextQuery.getResultSize());
            Debug.logInfo("searchInEntities: found " + getResultSize() + " total results.", MODULE);
            Debug.logInfo("searchInEntities: list contains " + getResultSize() + " results.", MODULE);

            // a Verbose debug of all results (in current pagination range)
            Debug.logInfo("------- results from " + getPageStart() + " to " + getPageEnd() + " out of " + getResultSize() + " -------", MODULE);
            int i = getPageStart() + 1;
            for (Object[] o : getResults()) {
                StringBuilder sb = new StringBuilder(" ").append(i).append(" --> ");
                for (Object detail : o) {
                    sb.append(detail).append(" ");
                }
                Debug.logInfo(sb.toString(), MODULE);
                i++;
            }
            Debug.logInfo("------- end of results from " + getPageStart() + " to " + getPageEnd() + " out of " + getResultSize() + " -------", MODULE);

            tx.commit();
        } catch (ParseException e) {
            Debug.logError(e, "Error parsing lucene query [" + queryString + "].", MODULE);
            // rollback the transaction
            if (tx != null) {
                try {
                    tx.rollback();
                } catch (Exception e2) {
                    Debug.logWarning(e2, "Could not rollback the hibernate transaction on error.", MODULE);
                }
            }
            throw new ServiceException(e);
        } catch (InfrastructureException e) {
            Debug.logError(e, "Error getting the hibernate session.", MODULE);
            // rollback the transaction
            if (tx != null) {
                try {
                    tx.rollback();
                } catch (Exception e2) {
                    Debug.logWarning(e2, "Could not rollback the hibernate transaction on error.", MODULE);
                }
            }
            throw new ServiceException(e);
        } finally {
            // close the sessions
            try {
                if (fullTextSession != null) {
                    fullTextSession.close();
                }
            } catch (Exception e) {
                Debug.logWarning(e, "Could not close the FullTextSession.", MODULE);
            }

            try {
                if (session != null && session.isOpen()) {
                    Debug.logWarning("Session still open, closing.", MODULE);
                    session.close();
                }
            } catch (Exception e) {
                Debug.logWarning(e, "Could not close the Session.", MODULE);
            }
        }
        return getResults();
    }

    /**
     * Inserts user given keywords into a Lucene query.
     * The query is assumed to use <code>DEFAULT_PLACEHOLDER</code> where the keywords should be inserted.
     * The user given keywords are escaped as to not cause any parsing exception.
     * @param queryString a Lucene query with placeholders
     * @param keywords a user given string of the keywords he is looking for
     * @return the Lucene query string
     */
    protected String makeQueryString(String queryString, String keywords) {
        return makeQueryString(queryString, SearchRepositoryInterface.DEFAULT_PLACEHOLDER_REGEX, keywords);
    }

    /**
     * Inserts user given keywords into a Lucene query.
     * The user given keywords are escaped as to not cause any parsing exception.
     * @param queryString a Lucene query with placeholders
     * @param placeholderRegex a regular expression matching the placeholder used
     * @param keywords a user given string of the keywords he is looking for
     * @return the Lucene query string
     */
    protected String makeQueryString(String queryString, String placeholderRegex, String keywords) {
        return queryString.replaceAll(placeholderRegex, "(\"" + QueryParser.escape(keywords.toLowerCase()) + "\")");
    }
}
