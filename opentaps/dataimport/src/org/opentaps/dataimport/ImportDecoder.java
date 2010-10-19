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
package org.opentaps.dataimport;

import org.ofbiz.entity.Delegator;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.service.LocalDispatcher;

import java.sql.Timestamp;
import java.util.List;

/**
 * Interface that defines a function to map a flat entity into
 * a set of Opentaps entities.
 *
 * Note that optional arguments to decode() may be supplied via OpentapsImporter constructor.  Alternatively,
 * an implementation of this could store the necessary parameters from construction.
 */
public interface ImportDecoder {

    /**
     * Decode the given flatEntity into a List of opentaps entities for importing.  Supplementary arguments may be provided.
     * Exceptions from the entity or service engines are handled by the import system.  An importTime is provided by
     * the import system if you wish to set a date equal to the time of import.
     *
     * You may also want to add the flatEntity to the return list as well, in case you want to update it.  For instance, if
     * you wish to link the flatEntity.importedProductId to the Product.productId that is created.  Note that the
     * import process will handle updating the importStatusId, exception and processedTimestamp fields.
     *
     * If this method throws *any* kind of exception, the import of this particular entity will be aborted and the import
     * will continue to the next entry.
     */
    public List<GenericValue> decode(GenericValue flatEntity, Timestamp importTime, Delegator delegator, LocalDispatcher dispatcher, Object... args) throws Exception;

}
