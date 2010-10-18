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

import java.util.List;
import org.opentaps.base.entities.DataImportGlAccount;
import org.opentaps.base.entities.EncumbranceDetail;
import org.opentaps.base.entities.EncumbranceSnapshot;
import org.opentaps.foundation.entity.hibernate.Session;
import org.opentaps.foundation.repository.RepositoryException;
import org.opentaps.foundation.repository.RepositoryInterface;

/**
 * Interface for accounting data import related entities.
 */
public interface AccountingDataImportRepositoryInterface extends RepositoryInterface{
    
    /**
     * Finds not imported General Ledger accounts.
     * @throws org.opentaps.foundation.repository.RepositoryException
     */
    public List<DataImportGlAccount> findNotProcessesDataImportGlAccountEntries() throws RepositoryException;

}
