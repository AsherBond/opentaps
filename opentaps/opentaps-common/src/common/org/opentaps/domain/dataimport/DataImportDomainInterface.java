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
package org.opentaps.domain.dataimport;

import org.opentaps.foundation.domain.DomainInterface;
import org.opentaps.foundation.repository.RepositoryException;
import org.opentaps.foundation.service.ServiceException;

/**
 * Interface for Data Import module.
 */
public interface DataImportDomainInterface extends DomainInterface {
    
    /**
     * Returns the accounting data import repository instance.
     * @return a <code>AccountingDataImportRepositoryInterface</code> value
     * @throws RepositoryException if an error occurs
     */
    public AccountingDataImportRepositoryInterface getAccountingDataImportRepository() throws RepositoryException;
    
    /**
     * Returns the importing General Ledger accounts service instance.
     * @return an <code>GlAccountImportServiceInterface</code> value
     * @throws ServiceException if an error occurs
     */
    public GlAccountImportServiceInterface getGlAccountImportService() throws ServiceException;
    
    /**
     * Returns the product data import repository instance.
     * @return a <code>ProductDataImportRepositoryInterface</code> value
     * @throws RepositoryException if an error occurs
     */
    public ProductDataImportRepositoryInterface getProductDataImportRepository() throws RepositoryException;
    
    /**
     * Returns the party data import repository instance.
     * @return a <code>PartyDataImportRepositoryInterface</code> value
     * @throws RepositoryException if an error occurs
     */
    public PartyDataImportRepositoryInterface getPartyDataImportRepository() throws RepositoryException;

}
