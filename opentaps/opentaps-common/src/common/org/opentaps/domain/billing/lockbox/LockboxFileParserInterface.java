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

package org.opentaps.domain.billing.lockbox;

import java.util.List;

/**
 * Lockbox file parser.
 */
public interface LockboxFileParserInterface {

    /**
     * Parse the data.
     * @param data a <code>String</code> containing the text file to import
     * @throws LockboxFileParserException if an error occurs
     */
    public void parse(String data) throws LockboxFileParserException;

    /**
     * Parse the data.
     * @param lines an array of data strings representing the lockbox file
     * @throws LockboxFileParserException if an error occurs
     */
    public void parse(String[] lines) throws LockboxFileParserException;

    /**
     * Gets the parsed list of <code>LockboxBatch</code>.
     * Note that the primary key has to be completed before storage.
     * @return the parsed list of <code>LockboxBatch</code>
     */
    public List<LockboxBatch> getLockboxBatches();

    /**
     * Gets the parsed list of <code>LockboxBatchItem</code>.
     * Note that the primary key has to be completed before storage.
     * @return the parsed list of <code>LockboxBatchItem</code>
     */
    public List<LockboxBatchItem> getLockboxBatchItems();

    /**
     * Gets the parsed list of <code>LockboxBatchItemDetail</code>.
     * Note that the primary key has to be completed before storage.
     * @return the parsed list of <code>LockboxBatchItemDetail</code>
     */
    public List<LockboxBatchItemDetail> getLockboxBatchItemDetails();
}
