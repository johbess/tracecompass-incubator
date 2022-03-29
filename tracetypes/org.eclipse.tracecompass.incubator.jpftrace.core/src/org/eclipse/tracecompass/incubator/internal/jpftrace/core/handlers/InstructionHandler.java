package org.eclipse.tracecompass.incubator.internal.jpftrace.core.handlers;

import org.eclipse.tracecompass.analysis.os.linux.core.trace.IKernelAnalysisEventLayout;
import org.eclipse.tracecompass.internal.analysis.os.linux.core.kernel.handlers.KernelEventHandler;
import org.eclipse.tracecompass.internal.analysis.os.linux.core.kernel.handlers.KernelEventHandlerUtils;
import org.eclipse.tracecompass.statesystem.core.ITmfStateSystemBuilder;
import org.eclipse.tracecompass.statesystem.core.exceptions.AttributeNotFoundException;
// import org.eclipse.tracecompass.statesystem.core.statevalue.ITmfStateValue;
import org.eclipse.tracecompass.tmf.core.event.ITmfEvent;
import org.eclipse.tracecompass.tmf.core.event.ITmfEventField;
import org.eclipse.tracecompass.incubator.internal.jpftrace.core.analysis.threadstatus.Attributes;
import org.eclipse.tracecompass.incubator.internal.jpftrace.core.event.IJpfTraceConstants;
import org.eclipse.tracecompass.incubator.internal.jpftrace.core.event.JpfTraceEvent;

public class InstructionHandler extends KernelEventHandler {

    public InstructionHandler(IKernelAnalysisEventLayout layout) {
        super(layout);
    }

    @Override
    public void handleEvent(ITmfStateSystemBuilder ss, ITmfEvent event) throws AttributeNotFoundException {
        Integer cpu = KernelEventHandlerUtils.getCpu(event);
        ITmfEventField content = event.getContent();
        Integer tid = content.getFieldValue(Integer.class, IJpfTraceConstants.THREAD_ID);

        if (tid == null) {
            System.out.println("InstructionHandler::handleEvent: event TID not found"); //$NON-NLS-1$
            return;
        }

        String[] specAndDetail = ((JpfTraceEvent) event).getField().getInstructionSpecAndDetail();
        String threadAttributeName = Attributes.buildThreadAttributeName(tid, cpu);
        final int threadNode = ss.getQuarkRelativeAndAdd(KernelEventHandlerUtils.getNodeThreads(ss), threadAttributeName);
        final int specNode = ss.getQuarkRelativeAndAdd(threadNode, Attributes.SPEC);
        final int detailNode = ss.getQuarkRelativeAndAdd(threadNode, Attributes.DETAIL);

        long timestamp = KernelEventHandlerUtils.getTimestamp(event);

        ss.modifyAttribute(timestamp, specAndDetail[0], specNode);
        ss.modifyAttribute(timestamp, specAndDetail[1], detailNode);

        // ITmfStateValue value = ss.queryOngoingState(specNode);
        // System.out.println("InstructionHandler::value " +
        // String.valueOf(value.unboxValue()));
    }
}
