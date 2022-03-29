package org.eclipse.tracecompass.incubator.internal.jpftrace.core.event;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.tmf.core.event.ITmfEventType;
import org.eclipse.tracecompass.tmf.core.event.TmfEventType;

public class JpfTraceEventTypeFactory {

    private static final Map<String, TmfEventType> TYPES = new HashMap<>();

    public static @Nullable ITmfEventType get(@Nullable String eventName) {
        if (eventName == null || eventName.isEmpty()) {
            return null;
        }

        TmfEventType event = null;
        if (TYPES.containsKey(eventName)) {
            event = TYPES.get(eventName);
        } else {
            event = new TmfEventType(eventName, null);
            TYPES.put(eventName, event);
        }
        return event;
    }
}
