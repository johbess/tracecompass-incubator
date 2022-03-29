package org.eclipse.tracecompass.incubator.internal.jpftrace.core.event;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.tmf.core.event.ITmfEvent;
import org.eclipse.tracecompass.tmf.core.event.aspect.ITmfEventAspect;

public interface IJpfTraceAspect<T> extends ITmfEventAspect<T> {

    @Override
    default @Nullable T resolve(@NonNull ITmfEvent event) {
        if (event instanceof JpfTraceEvent) {
            return resolveJpfTraceLogs((JpfTraceEvent) event);
        }
        return null;
    }

    T resolveJpfTraceLogs(@NonNull JpfTraceEvent event);

}
