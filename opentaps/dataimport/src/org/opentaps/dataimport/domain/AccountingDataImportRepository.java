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
package org.opentaps.dataimport.domain;

import java.util.List;
import org.opentaps.base.entities.DataImportGlAccount;
import org.opentaps.domain.DomainRepository;
import org.opentaps.domain.dataimport.AccountingDataImportRepositoryInterface;
import org.opentaps.foundation.infrastructure.Infrastructure;
import org.opentaps.foundation.repository.RepositoryException;
import org.ofbiz.entity.condition.EntityCondition;
import org.ofbiz.entity.condition.EntityOperator;
import org.opentaps.base.constants.StatusItemConstants;
import org.opentaps.foundation.infrastructure.User;

/** {@inheritDoc}. */
public class AccountingDataImportRepository extends DomainRepository implements AccountingDataImportRepositoryInterface{
    
    /**
     * Default constructor.
     */
    public AccountingDataImportRepository() {
        super();
    }

     /**
     * If you want the full infrastructure including the dispatcher, then you must have the User.
     * @param infrastructure the domain infrastructure
     * @param user the domain user
     * @throws RepositoryException if an error occurs
     */
    public AccountingDataImportRepository(Infrastructure infrastructure, User user) throws RepositoryException {
        super(infrastructure, user);
    }

    /** {@inheritDoc}. */
    public List<DataImportGlAccount> findNotProcessesDataImportGlAccountEntries() throws RepositoryException {
        EntityCondition statusCond = EntityCondition.makeCondition(EntityOperator.OR,
        EntityCondition.makeCondition("importStatusId", EntityOperator.EQUALS, StatusItemConstants.Dataimport.DATAIMP_NOT_PROC),
        EntityCondition.makeCondition("importStatusId", EntityOperator.EQUALS, StatusItemConstants.Dataimport.DATAIMP_FAILED),
        EntityCondition.makeCondition("importStatusId", EntityOperator.EQUALS, null));
        return this.findList(DataImportGlAccount.class, statusCond);
    }   
}
