/*
 * Copyright (C) 2014 CLARIN
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package eu.clarin.cmdi.vlo.service.impl;

import com.google.common.base.Splitter;
import com.google.common.collect.Maps;
import static eu.clarin.cmdi.vlo.VloWebAppParameters.*;
import eu.clarin.cmdi.vlo.pojo.FacetSelection;
import eu.clarin.cmdi.vlo.pojo.FacetSelectionType;
import eu.clarin.cmdi.vlo.pojo.QueryFacetsSelection;
import eu.clarin.cmdi.vlo.service.PageParametersConverter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.util.string.StringValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Page parameter conversion service for {@link QueryFacetsSelection}
 *
 * @author twagoo
 */
public class QueryFacetsSelectionParametersConverter implements PageParametersConverter<QueryFacetsSelection> {

    public final static Logger logger = LoggerFactory.getLogger(QueryFacetsSelectionParametersConverter.class);
    /**
     * Splitter for facet query strings like "language:Dutch". Because it is
     * limited to two tokens, will also work for strings with a colon in their
     * value such as "collection:TLA: DoBeS archive".
     */
    public final static Splitter FILTER_SPLITTER = Splitter.on(":").limit(2);

    @Override
    public QueryFacetsSelection fromParameters(PageParameters params) {
        // Get query string from params
        final String query = params.get(QUERY).toOptionalString();

        final List<StringValue> facetSelectionTypes = params.getValues(FILTER_QUERY_TYPE);
        final List<StringValue> facetValues = params.getValues(FILTER_QUERY);
        final HashMap<String, FacetSelection> selection = Maps.newHashMapWithExpectedSize(facetValues.size());

        for (StringValue selectionType : facetSelectionTypes) {
            if (!selectionType.isEmpty()) {
                final List<String> fqType = FILTER_SPLITTER.splitToList(selectionType.toString());
                if (fqType.size() == 2) {
                    final String facet = fqType.get(0);
                    final String type = fqType.get(1).toUpperCase();

                    try {
                        final FacetSelectionType facetSelectionType = FacetSelectionType.valueOf(type);
                        selection.put(facet, new FacetSelection(facetSelectionType));
                    } catch (IllegalArgumentException ex) {
                        logger.warn("Unknown selection type passed into query parameter {}: {}", FILTER_QUERY_TYPE, type);
                    }
                } else {
                    logger.info("Illegal query parameter value for {}: {}", FILTER_QUERY_TYPE, selectionType);
                }
            }
        }

        // Get facet selections from params
        for (StringValue facetValue : facetValues) {
            if (!facetValue.isEmpty()) {
                final List<String> fq = FILTER_SPLITTER.splitToList(facetValue.toString());
                if (fq.size() == 2) {
                    // we have a facet - value pair
                    final String facet = fq.get(0);
                    final String value = fq.get(1);
                    if (selection.containsKey(facet)) {
                        selection.get(facet).getValues().add(value);
                    } else {
                        selection.put(facet, new FacetSelection(Arrays.asList(value)));
                    }
                } else {
                    logger.info("Illegal query parameter value for {}: {}", FILTER_QUERY, facetValue);
                }
            }
        }

        return new QueryFacetsSelection(query, selection);
    }

    @Override
    public PageParameters toParameters(QueryFacetsSelection selection) {
        final PageParameters params = new PageParameters();

        // put the query in the 'q' parameter
        final String query = selection.getQuery();
        if (query != null) {
            params.add(QUERY, query);
        }

        // put all selections in 'fq' parameters
        for (Entry<String, FacetSelection> facetSelectionEntry : selection.getSelection().entrySet()) {
            final String facet = facetSelectionEntry.getKey();
            final FacetSelection facetSelection = facetSelectionEntry.getValue();
            // put a parameter for the selection type (unless it is AND which is default)
            if (facetSelection.getSelectionType() != FacetSelectionType.AND) {
                params.add(FILTER_QUERY_TYPE, String.format("%s:%s", facet, facetSelection.getSelectionType().toString().toLowerCase()));
            }
            for (String value : facetSelection.getValues()) {
                //TODO: Encode?
                params.add(FILTER_QUERY, String.format("%s:%s", facet, value));
            }
        }

        return params;
    }
}
