/*
 * Copyright (c) 2009 Open Source Strategies, Inc.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the Honest Public License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * Honest Public License for more details.
 *
 * You should have received a copy of the Honest Public License
 * along with this program; if not, write to Funambol,
 * 643 Bair Island Road, Suite 305 - Redwood City, CA 94063, USA
 */
package org.opentaps.domain.billing.payment;

import org.opentaps.domain.party.Party;
import org.opentaps.foundation.entity.EntityNotFoundException;
import org.opentaps.foundation.repository.RepositoryException;
import org.opentaps.foundation.repository.RepositoryInterface;

/**
 * Interface for payment repository.
 * TODO: uncertain whether the isPaymentType() should be in specification or repository. note use of delegator
 */
public interface PaymentRepositoryInterface extends RepositoryInterface {

    public PaymentSpecificationInterface getPaymentSpecification();

    public Payment getPaymentById(String paymentId) throws RepositoryException, EntityNotFoundException;

    public Boolean isPaymentType(Payment payment, PaymentTypeInterface paymentType) throws RepositoryException;

    public Boolean isDisbursement(Payment payment) throws RepositoryException;

    public Boolean isReceipt(Payment payment) throws RepositoryException;

    public Boolean isCustomerRefund(Payment payment) throws RepositoryException;

    public Boolean isTaxPayment(Payment payment) throws RepositoryException;

    public Boolean isPayCheck(Payment payment) throws RepositoryException;

    /**
     * Finds a <code>Party</code> by ID from the database.
     * @param partyId the party ID
     * @return the <code>Party</code> found
     * @throws RepositoryException if an error occurs
     * @throws EntityNotFoundException no <code>Party</code> is found for the given id
     */
    public Party getPartyById(String partyId) throws RepositoryException, EntityNotFoundException;

}
