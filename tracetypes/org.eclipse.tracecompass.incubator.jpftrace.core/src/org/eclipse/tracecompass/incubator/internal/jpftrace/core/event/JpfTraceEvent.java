package org.eclipse.tracecompass.incubator.internal.jpftrace.core.event;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.tracecompass.tmf.core.event.ITmfEventField;
import org.eclipse.tracecompass.tmf.core.event.TmfEvent;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;

public class JpfTraceEvent extends TmfEvent {

    private final String fName;
    private final JpfTraceField fField;

    /**
     * Constructor for simple traceEventEvent
     *
     * @param trace
     *            the trace
     * @param rank
     *            the rank
     * @param field
     *            the event field, contains all the needed data
     */
    public JpfTraceEvent(ITmfTrace trace, long rank, JpfTraceField field) {
        super(trace, rank, trace.createTimestamp(field.getTimestamp()), JpfTraceEventTypeFactory.get(field.getType()), field.getContent()); // $NON-NLS-1$
        fField = field;
        fName = field.getType();
    }

    @Override
    public ITmfEventField getContent() {
        return fField.getContent();
    }

    @Override
    public @NonNull String getName() {
        return fName;
    }

    public JpfTraceField getField() {
        return fField;
    }

}
