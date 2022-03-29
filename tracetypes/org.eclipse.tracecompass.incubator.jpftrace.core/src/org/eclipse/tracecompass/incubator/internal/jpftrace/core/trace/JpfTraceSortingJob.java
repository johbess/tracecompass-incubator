package org.eclipse.tracecompass.incubator.internal.jpftrace.core.trace;

import java.io.IOException;

import org.eclipse.tracecompass.internal.jsontrace.core.job.SortingJob;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;

public class JpfTraceSortingJob extends SortingJob {

    /**
     * Constructor
     *
     * @param trace
     *            the trace to be sort
     * @param path
     *            the path to the trace file
     */
    public JpfTraceSortingJob(ITmfTrace trace, String path) {
        super(trace, path, "\"groupId\":", 1);
    }

    @Override
    protected void processMetadata(ITmfTrace trace, String dir) throws IOException {
        // metadata comes after event list should be parsed here.
    }

}
