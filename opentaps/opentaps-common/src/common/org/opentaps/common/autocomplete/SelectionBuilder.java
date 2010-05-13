package org.opentaps.common.autocomplete;

import org.ofbiz.entity.GenericValue;

import java.util.Map;

/**
 * Interface for building a selection element in a list of autocomplete results.
 */
public interface SelectionBuilder {

    public Map<String,Object> buildRow(Object element);

}
