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

import java.util.ArrayList;

public class ChoiceInfoHandler extends KernelEventHandler {

    public ChoiceInfoHandler(IKernelAnalysisEventLayout layout) {
        super(layout);
    }

    @Override
    public void handleEvent(ITmfStateSystemBuilder ss, ITmfEvent event) throws AttributeNotFoundException {
        Integer cpu = KernelEventHandlerUtils.getCpu(event);
        ITmfEventField content = event.getContent();
        Integer tid = content.getFieldValue(Integer.class, IJpfTraceConstants.THREAD_ID);

        if (tid == null) {
            System.out.println("ChoiceInfoHandler::handleEvent: event TID not found"); //$NON-NLS-1$
            return;
        }

        String choiceId = content.getFieldValue(String.class, IJpfTraceConstants.CHOICE_ID);
        String currentChoice = content.getFieldValue(String.class, IJpfTraceConstants.CURRENT_CHOICE);

        ArrayList<String> choiceList = new ArrayList<>();
        if (event instanceof JpfTraceEvent) {
            choiceList = ((JpfTraceEvent) event).getField().getChoices();
        }

        if (choiceId == null || currentChoice == null || choiceList.size() == 0) {
            System.out.println("ChoiceInfoHandler::handleEvent: missing choice information"); //$NON-NLS-1$
            return;
        }

        int pos = choiceList.indexOf(currentChoice);
        if (pos < 0 || pos >= choiceList.size()) {
            System.out.println("ChoiceInfoHandler::handleEvent: no matching choice"); //$NON-NLS-1$
            return;
        }

        String choiceMadeString = String.valueOf(pos + 1) + "/" + String.valueOf(choiceList.size()); //$NON-NLS-1$

        long timestamp = KernelEventHandlerUtils.getTimestamp(event);

        String threadAttributeName = Attributes.buildThreadAttributeName(tid, cpu);
        final int threadNode = ss.getQuarkRelativeAndAdd(KernelEventHandlerUtils.getNodeThreads(ss), threadAttributeName);
        // System.out.println("Choiceinfo Handler: tid: " +
        // String.valueOf(tid));
        final int choiceNode = ss.getQuarkRelativeAndAdd(threadNode, Attributes.CHOICE);
        final int choiceIdNode = ss.getQuarkRelativeAndAdd(threadNode, Attributes.CHOICE_ID);
        final int choideMadeNode = ss.getQuarkRelativeAndAdd(threadNode, Attributes.CHOICE_MADE);
        final int specNode = ss.getQuarkRelativeAndAdd(threadNode, Attributes.SPEC);
        final int detailNode = ss.getQuarkRelativeAndAdd(threadNode, Attributes.DETAIL);

        ss.modifyAttribute(timestamp, currentChoice, choiceNode);
        ss.modifyAttribute(timestamp, choiceId, choiceIdNode);
        ss.modifyAttribute(timestamp, choiceMadeString, choideMadeNode);

        // set the state of special instructions to null
        ss.modifyAttribute(timestamp, "", specNode); //$NON-NLS-1$
        ss.modifyAttribute(timestamp, "", detailNode); //$NON-NLS-1$

        // ITmfStateValue value = ss.queryOngoingState(choideMadeNode);
        // System.out.println("ChoiceInfoHandler::value " +
        // String.valueOf(value.unboxValue()));
    }
}
