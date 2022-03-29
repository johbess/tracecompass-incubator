/**********************************************************************
 * Copyright (c) 2021 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 **********************************************************************/

package org.eclipse.tracecompass.incubator.internal.trace.server.jersey.rest.core.model;

import java.util.Collection;
import java.util.Map;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Contributes to the model used for TSP swagger-core annotations.
 */
@Schema(description = "Model returned by outputs that contains annotations per category")
public interface IAnnotationModel {

    /**
     * @return The annotations per category.
     */
    @Schema(description = "Map of annotations where the keys are categories")
    Map<String, Collection<IAnnotation>> getAnnotations();
}
