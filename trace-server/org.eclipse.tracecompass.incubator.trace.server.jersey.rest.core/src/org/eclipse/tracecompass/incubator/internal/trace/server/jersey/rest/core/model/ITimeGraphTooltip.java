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

import org.eclipse.jdt.annotation.NonNull;

/**
 * Contributes to the model used for TSP swagger-core annotations.
 */
public interface ITimeGraphTooltip {

    /**
     * @return The key.
     */
    @NonNull
    String getKey();

    /**
     * @return The value.
     */
    @NonNull
    String getValue();
}
