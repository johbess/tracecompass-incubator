package org.eclipse.tracecompass.incubator.internal.jpftrace.core.trace;

import org.eclipse.core.resources.IResource;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.tmf.core.event.ITmfEvent;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;
import org.eclipse.tracecompass.tmf.core.trace.experiment.TmfExperiment;

public class JpfTraceExperiment extends TmfExperiment {

    /**
     * Constructor
     */
    @Deprecated
    public JpfTraceExperiment() {
        super();
    }

    /**
     * Constructor of an open tracing experiment
     *
     * @param type
     *            The event type
     * @param path
     *            The experiment path
     * @param traces
     *            The experiment set of traces
     * @param indexPageSize
     *            The experiment index page size. You can use
     *            {@link TmfExperiment#DEFAULT_INDEX_PAGE_SIZE} for a default
     *            value.
     * @param resource
     *            The resource associated to the experiment. You can use 'null'
     *            for no resources (tests, etc.)
     */
    public JpfTraceExperiment(final Class<? extends ITmfEvent> type,
            final String path,
            final ITmfTrace[] traces,
            final int indexPageSize,
            final @Nullable IResource resource) {
        super(type, path, traces, indexPageSize, resource);
    }
}
