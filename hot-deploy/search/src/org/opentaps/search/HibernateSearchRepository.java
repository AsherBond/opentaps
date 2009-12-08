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
package org.opentaps.search;

import java.util.ArrayList;
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
import org.opentaps.domain.search.SearchRepositoryInterface;
import org.opentaps.domain.search.SearchResult;
import org.opentaps.foundation.entity.hibernate.Session;
import org.opentaps.foundation.infrastructure.InfrastructureException;
import org.opentaps.foundation.repository.RepositoryException;
import org.opentaps.foundation.repository.ofbiz.Repository;

/**
 * Implementation of the search repository using Hibernate search.
 */
public class HibernateSearchRepository extends Repository implements SearchRepositoryInterface {

    /** The default placeholder in Lucene queries. */
    public static final String DEFAULT_PLACEHOLDER = "?";
    /** The regular expression recognizing the default placeholder in Lucene queries. */
    public static final String DEFAULT_PLACEHOLDER_REGEX = "\\?";

    private static final String MODULE = HibernateSearchRepository.class.getName();

    private List<SearchResult> results;
    private int resultSize = 0;

    /** {@inheritDoc} */
    public String makeQueryString(String queryString, String keywords) {
        return makeQueryString(queryString, DEFAULT_PLACEHOLDER_REGEX, keywords);
    }

    /**
     * Inserts user given keywords into a Lucene query.
     * The user given keywords are escaped as to not cause any parsing exception.
     * @param queryString a Lucene query with placeholders
     * @param placeholderRegex a regular expression matching the placeholder used
     * @param keywords a user given string of the keywords he is looking for
     * @return the Lucene query string
     */
    private String makeQueryString(String queryString, String placeholderRegex, String keywords) {
        // note: this supports both case sensitive and case insensitive fields
        //  for example the UN_TOKENIZED fields may use Upper Case values and are indexed case sensitively
        //  so we have a way to match them here
        return
            // to match UN_TOKENIZED fields
            queryString.replaceAll(placeholderRegex, "(\"" + QueryParser.escape(keywords) + "\")")
            + " "
            // to match TOKENIZED fields
            + queryString.replaceAll(placeholderRegex, "(\"" + QueryParser.escape(keywords.toLowerCase()) + "\")");
    }

    /** {@inheritDoc} */
    public void searchInEntities(Set<Class<?>> entityClasses, String queryString, int pageStart, int pageSize) throws RepositoryException {

        Debug.logInfo("searchInEntities: [" + entityClasses + "] query [" + queryString + "]", MODULE);
        int pageEnd = pageStart + pageSize;
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
            fullTextQuery.setProjection(new String[]{FullTextQuery.OBJECT_CLASS, FullTextQuery.ID});
            // set pagination
            fullTextQuery.setFirstResult(pageStart);
            fullTextQuery.setMaxResults(pageSize);
            // perform the query
            List<Object[]> queryResults = fullTextQuery.list();
            resultSize = fullTextQuery.getResultSize();
            Debug.logInfo("searchInEntities: found " + resultSize + " total results.", MODULE);
            Debug.logInfo("searchInEntities: list contains " + queryResults.size() + " results.", MODULE);

            // a Verbose debug of all results (in current pagination range)
            // and map the query results into SearchResult objects
            results = new ArrayList<SearchResult>();
            Debug.logInfo("------- results from " + pageStart + " to " + pageEnd + " out of " + resultSize + " -------", MODULE);
            int i = pageStart + 1;
            for (Object[] o : queryResults) {
                StringBuilder sb = new StringBuilder(" ").append(i).append(" --> ");
                for (Object detail : o) {
                    sb.append(detail).append(" ");
                }
                Debug.logInfo(sb.toString(), MODULE);
                i++;

                results.add(new SearchResult((Class<?>) o[0], (Object) o[1]));

            }
            Debug.logInfo("------- end of results from " + pageStart + " to " + pageEnd + " out of " + resultSize + " -------", MODULE);

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
            throw new RepositoryException(e);
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
            throw new RepositoryException(e);
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
    }

    /** {@inheritDoc} */
    public List<SearchResult> getResults() {
        return results;
    }

    /** {@inheritDoc} */
    public int getResultSize() {
        return resultSize;
    }

}
