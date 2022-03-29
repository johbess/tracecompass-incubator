/*******************************************************************************
 * Copyright (c) 2021 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.internal.kernel.ui.views.swslatency;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.osgi.util.NLS;

/**
 * @author Abdellah Rahmani
 */
@NonNullByDefault({})
public class Messages extends NLS {
    private static final String BUNDLE_NAME = "org.eclipse.tracecompass.incubator.internal.kernel.ui.views.swslatency.messages"; //$NON-NLS-1$

    /**
     * Time vs Duration
     */
    public static String SWSLatencyScatterView_title;

    /**
     * Time axis
     */
    public static String SWSLatencyScatterView_xAxis;

    /**
     * Duration axis
     */
    public static String SWSLatencyScatterView_yAxis;

    static {
        // initialize resource bundle
        NLS.initializeMessages(BUNDLE_NAME, Messages.class);
    }

    private Messages() {
    }
}
