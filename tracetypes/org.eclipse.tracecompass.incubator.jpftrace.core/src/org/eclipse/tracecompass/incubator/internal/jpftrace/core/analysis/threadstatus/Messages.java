package org.eclipse.tracecompass.incubator.internal.jpftrace.core.analysis.threadstatus;

import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.osgi.util.NLS;

class Messages extends NLS {
    private static final String BUNDLE_NAME = "org.eclipse.tracecompass.incubator.internal.jpftrace.core.analysis.threadstatus.messages"; //$NON-NLS-1$

    public static String LttngKernelAnalysisModule_Help;

    public static String JpfThreadStatusDataProviderFactory_title;

    public static String JpfThreadStatusDataProviderFactory_descriptionText;

    public static String JpfThreadStatusDataProvider_link;

    public static @Nullable String JpfThreadStyle_ProcessGroup = null;

    public static @Nullable String JpfThreadStyle_sync = null;

    public static @Nullable String JpfThreadStyle_method_call = null;

    public static @Nullable String JpfThreadStyle_lock_unlock = null;

    public static @Nullable String JpfThreadStyle_field_access = null;

    static {
        NLS.initializeMessages(BUNDLE_NAME, Messages.class);
    }

    private Messages() {
    }
}
