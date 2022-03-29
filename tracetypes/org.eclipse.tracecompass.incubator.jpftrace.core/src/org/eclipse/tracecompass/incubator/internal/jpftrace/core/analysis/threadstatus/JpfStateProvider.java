package org.eclipse.tracecompass.incubator.internal.jpftrace.core.analysis.threadstatus;

import java.util.Map;

import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.analysis.os.linux.core.trace.IKernelAnalysisEventLayout;
import org.eclipse.tracecompass.common.core.NonNullUtils;
import org.eclipse.tracecompass.internal.analysis.os.linux.core.Activator;
import org.eclipse.tracecompass.internal.analysis.os.linux.core.kernel.handlers.CpuFrequencyHandler;
import org.eclipse.tracecompass.internal.analysis.os.linux.core.kernel.handlers.IPIEntryHandler;
import org.eclipse.tracecompass.internal.analysis.os.linux.core.kernel.handlers.IPIExitHandler;
import org.eclipse.tracecompass.internal.analysis.os.linux.core.kernel.handlers.IrqEntryHandler;
import org.eclipse.tracecompass.internal.analysis.os.linux.core.kernel.handlers.IrqExitHandler;
import org.eclipse.tracecompass.internal.analysis.os.linux.core.kernel.handlers.KernelEventHandler;
import org.eclipse.tracecompass.internal.analysis.os.linux.core.kernel.handlers.PiSetprioHandler;
import org.eclipse.tracecompass.internal.analysis.os.linux.core.kernel.handlers.ProcessExitHandler;
import org.eclipse.tracecompass.internal.analysis.os.linux.core.kernel.handlers.ProcessForkHandler;
import org.eclipse.tracecompass.internal.analysis.os.linux.core.kernel.handlers.ProcessFreeHandler;
import org.eclipse.tracecompass.internal.analysis.os.linux.core.kernel.handlers.SchedMigrateTaskHandler;
import org.eclipse.tracecompass.internal.analysis.os.linux.core.kernel.handlers.SchedSwitchHandler;
import org.eclipse.tracecompass.internal.analysis.os.linux.core.kernel.handlers.SchedWakeupHandler;
import org.eclipse.tracecompass.internal.analysis.os.linux.core.kernel.handlers.SoftIrqEntryHandler;
import org.eclipse.tracecompass.internal.analysis.os.linux.core.kernel.handlers.SoftIrqExitHandler;
import org.eclipse.tracecompass.internal.analysis.os.linux.core.kernel.handlers.SoftIrqRaiseHandler;
import org.eclipse.tracecompass.internal.analysis.os.linux.core.kernel.handlers.StateDumpHandler;
import org.eclipse.tracecompass.internal.analysis.os.linux.core.kernel.handlers.SysEntryHandler;
import org.eclipse.tracecompass.internal.analysis.os.linux.core.kernel.handlers.SysExitHandler;
import org.eclipse.tracecompass.statesystem.core.ITmfStateSystemBuilder;
import org.eclipse.tracecompass.statesystem.core.exceptions.AttributeNotFoundException;
import org.eclipse.tracecompass.statesystem.core.exceptions.StateValueTypeException;
import org.eclipse.tracecompass.statesystem.core.exceptions.TimeRangeException;
import org.eclipse.tracecompass.tmf.core.event.ITmfEvent;
import org.eclipse.tracecompass.tmf.core.statesystem.AbstractTmfStateProvider;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;

import com.google.common.collect.ImmutableMap;

import org.eclipse.tracecompass.incubator.internal.jpftrace.core.event.JpfTraceEventLayout;
import org.eclipse.tracecompass.incubator.internal.jpftrace.core.handlers.ChoiceInfoHandler;
import org.eclipse.tracecompass.incubator.internal.jpftrace.core.handlers.InstructionHandler;
import org.eclipse.tracecompass.incubator.internal.jpftrace.core.handlers.ThreadInfoHandler;

/**
 * This is the state change input plugin for the state system which handles the
 * kernel traces.
 *
 * Attribute tree:
 *
 * <pre>
 * |- CPUs
 * |  |- <CPU number> -> CPU Status
 * |  |  |- CURRENT_THREAD
 * |  |  |- SOFT_IRQS
 * |  |  |  |- <Soft IRQ number> -> Soft IRQ Status
 * |  |  |- IRQS
 * |  |  |  |- <IRQ number> -> IRQ Status
 * |- IRQs / SOFT_IRQs
 * |  |- <IRQ number> -> Aggregate Status
 * |- THREADS
 * |  |- <Thread number> -> Thread Status
 * |  |  |- PPID -> The thread ID of the parent, can be a process or a thread
 * |  |  |- EXEC_NAME
 * |  |  |- PRIO
 * |  |  |- SYSTEM_CALL
 * |  |  |- CURRENT_CPU_RQ
 * |  |  |- PID -> The process ID. If absent, the thread is a process
 * |  |  |- ENTRY_METHOD -> the caller method of current thread
 * |  |  |- CHOICE -> the current choice
 * |  |  |- CHOICE_ID -> the choice name (id)
 * |  |  |- CHOICE_MADE -> name of the choice  (in the format : "[i]/[n]")
 * |  |  |- SOURCE -> source code of instruction on current thread
 * |  |  |- SPEC -> type of the current instruction, is it interested? (lock/sync/fieldaccess)
 * |  |  |- DETAIL -> detail of the special instruction
 * </pre>
 *
 * @author Alexandre Montplaisir
 */
public class JpfStateProvider extends AbstractTmfStateProvider {

    // ------------------------------------------------------------------------
    // Static fields
    // ------------------------------------------------------------------------

    /**
     * Version number of this state provider. Please bump this if you modify the
     * contents of the generated state history in some way.
     */
    private static final int VERSION = 30;

    // ------------------------------------------------------------------------
    // Fields
    // ------------------------------------------------------------------------

    private final Map<String, KernelEventHandler> fEventNames;
    private final IKernelAnalysisEventLayout fLayout;

    private final KernelEventHandler fSysEntryHandler;
    private final KernelEventHandler fSysExitHandler;

    // private final JpfDefaultEventHandler fJpfDefaultEventHandler;

    // ------------------------------------------------------------------------
    // Constructor
    // ------------------------------------------------------------------------

    /**
     * Instantiate a new state provider plugin.
     *
     * @param trace
     *            The LTTng 2.0 kernel trace directory
     * @param layout
     *            The event layout to use for this state provider. Usually
     *            depending on the tracer implementation.
     */
    public JpfStateProvider(ITmfTrace trace, IKernelAnalysisEventLayout layout) {
        super(trace, "Kernel"); //$NON-NLS-1$
        fLayout = layout;
        fEventNames = buildEventNames(layout);

        fSysEntryHandler = new SysEntryHandler(fLayout);
        fSysExitHandler = new SysExitHandler(fLayout);
    }

    // ------------------------------------------------------------------------
    // Event names management
    // ------------------------------------------------------------------------

    private static Map<String, KernelEventHandler> buildEventNames(IKernelAnalysisEventLayout layout) {
        ImmutableMap.Builder<String, KernelEventHandler> builder = ImmutableMap.builder();

        JpfTraceEventLayout jpfLayout;
        if (layout instanceof JpfTraceEventLayout){
            jpfLayout = (JpfTraceEventLayout) layout;

            builder.put(jpfLayout.eventIrqHandlerEntry(), new IrqEntryHandler(layout));
            builder.put(jpfLayout.eventIrqHandlerExit(), new IrqExitHandler(layout));
            builder.put(jpfLayout.eventSoftIrqEntry(), new SoftIrqEntryHandler(layout));
            builder.put(jpfLayout.eventSoftIrqExit(), new SoftIrqExitHandler(layout));
            builder.put(jpfLayout.eventSoftIrqRaise(), new SoftIrqRaiseHandler(layout));
            builder.put(jpfLayout.eventSchedSwitch(), new SchedSwitchHandler(layout));
            builder.put(jpfLayout.eventSchedPiSetprio(), new PiSetprioHandler(layout));
            builder.put(jpfLayout.eventSchedProcessFork(), new ProcessForkHandler(layout));
            builder.put(jpfLayout.eventSchedProcessExit(), new ProcessExitHandler(layout));
            builder.put(jpfLayout.eventSchedProcessFree(), new ProcessFreeHandler(layout));
            builder.put(jpfLayout.eventSchedProcessWaking(), new SchedWakeupHandler(layout));
            builder.put(jpfLayout.eventSchedMigrateTask(), new SchedMigrateTaskHandler(layout));
            builder.put(jpfLayout.eventCpuFrequency(), new CpuFrequencyHandler(layout));
            builder.put(jpfLayout.eventThreadInfo(), new ThreadInfoHandler(layout));
            builder.put(jpfLayout.eventChoiceInfo(), new ChoiceInfoHandler(layout));
            builder.put(jpfLayout.eventInstruction(), new InstructionHandler(layout));
            // builder.put(jpfLayout.eventThreadLock(), new ThreadLockHandler(layout));
            // builder.put(jpfLayout.eventThreadExpose(), new ThreadExposeHandler(layout));
        }

        for (String s : layout.getIPIIrqVectorsEntries()) {
            builder.put(s, new IPIEntryHandler(layout));
        }
        for (String s : layout.getIPIIrqVectorsExits()) {
            builder.put(s, new IPIExitHandler(layout));
        }

        final String eventStatedumpProcessState = layout.eventStatedumpProcessState();
        if (eventStatedumpProcessState != null) {
            builder.put(eventStatedumpProcessState, new StateDumpHandler(layout));
        }

        for (String eventSchedWakeup : layout.eventsSchedWakeup()) {
            builder.put(eventSchedWakeup, new SchedWakeupHandler(layout));
        }

        return builder.build();
    }

    // ------------------------------------------------------------------------
    // IStateChangeInput
    // ------------------------------------------------------------------------

    @Override
    public int getVersion() {
        return VERSION;
    }

    @Override
    public JpfStateProvider getNewInstance() {
        return new JpfStateProvider(this.getTrace(), fLayout);
    }

    @Override
    protected void eventHandle(@Nullable ITmfEvent event) {
        if (event == null) {
            return;
        }

        final String eventName = event.getName();

        try {
            final ITmfStateSystemBuilder ss = NonNullUtils.checkNotNull(getStateSystemBuilder());
            /*
             * Feed event to the history system if it's known to cause a state
             * transition.
             */
            KernelEventHandler handler = fEventNames.get(eventName);
            if (handler == null) {
                if (isSyscallExit(eventName)) {
                    handler = fSysExitHandler;
                } else if (isSyscallEntry(eventName)) {
                    handler = fSysEntryHandler;
                }
            }
            if (handler != null) {
                handler.handleEvent(ss, event);

                // if (handler instanceof SchedSwitchHandler || handler instanceof SchedWakeupHandler) {
                //     System.out.println("JpfStateProvider::Recognized as thread related event");
                // }
            }

        } catch (AttributeNotFoundException ae) {
            /*
             * This would indicate a problem with the logic of the manager here,
             * so it shouldn't happen.
             */
            Activator.getDefault().logError("Attribute not found: " + ae.getMessage(), ae); //$NON-NLS-1$

        } catch (TimeRangeException tre) {
            /*
             * This would happen if the events in the trace aren't ordered
             * chronologically, which should never be the case ...
             */
            Activator.getDefault().logError("TimeRangeExcpetion caught in the state system's event manager.\n" + //$NON-NLS-1$
                    "Are the events in the trace correctly ordered?\n" + tre.getMessage(), tre); //$NON-NLS-1$

        } catch (StateValueTypeException sve) {
            /*
             * This would happen if we were trying to push/pop attributes not of
             * type integer. Which, once again, should never happen.
             */
            Activator.getDefault().logError("State value error: " + sve.getMessage(), sve); //$NON-NLS-1$
        }
    }

    private boolean isSyscallEntry(String eventName) {
        return (eventName.startsWith(fLayout.eventSyscallEntryPrefix())
                || eventName.startsWith(fLayout.eventCompatSyscallEntryPrefix()));
    }

    private boolean isSyscallExit(String eventName) {
        return (eventName.startsWith(fLayout.eventSyscallExitPrefix()) ||
                eventName.startsWith(fLayout.eventCompatSyscallExitPrefix()));
    }

}
