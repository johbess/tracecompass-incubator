package org.eclipse.tracecompass.incubator.internal.jpftrace.core.analysis.threadstatus;

import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.tmf.core.util.Pair;

/**
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
 */

@SuppressWarnings({ "nls", "javadoc" })
public interface Attributes {

    /* First-level attributes */
    String CPUS = "CPUs";
    String THREADS = "Threads";

    /* Sub-attributes of the CPU nodes */
    String CURRENT_THREAD = "Current_thread";
    String SOFT_IRQS = "Soft_IRQs";
    String IRQS = "IRQs";
    String CURRENT_FREQUENCY = "Frequency";
    String MIN_FREQUENCY = "Min frequency";
    String MAX_FREQUENCY = "Max frequency";

    /* Sub-attributes of the Thread nodes */
    String CURRENT_CPU_RQ = "Current_cpu_rq";
    String PPID = "PPID";
    String PID = "PID";
    String EXEC_NAME = "Exec_name";

    String PRIO = "Prio";
    String SYSTEM_CALL = "System_call";

    /* Misc stuff */
    String UNKNOWN = "Unknown";
    String THREAD_0_PREFIX = "0_";
    String THREAD_0_SEPARATOR = "_";

    /* Defined Jpf Thread States */
    String ENTRY_METHOD = "Thread_entry_method";
    String CHOICE = "Choice_name";
    String CHOICE_ID = "Choice_id";
    String CHOICE_MADE = "Choice_made";
    String SOURCE = "Source";
    String SPEC = "Special_instruction";
    String DETAIL = "Instruction_detail";

    public static @Nullable String buildThreadAttributeName(int threadId, @Nullable Integer cpuId) {
        if (threadId == 0) {
            if (cpuId == null) {
                return null;
            }
            return Attributes.THREAD_0_PREFIX + String.valueOf(cpuId);
        }

        return String.valueOf(threadId);
    }

    public static Pair<Integer, Integer> parseThreadAttributeName(String threadAttributeName) {
        Integer threadId = -1;
        Integer cpuId = -1;

        try {
            if (threadAttributeName.startsWith(Attributes.THREAD_0_PREFIX)) {
                threadId = 0;
                String[] tokens = threadAttributeName.split(Attributes.THREAD_0_SEPARATOR);
                cpuId = Integer.parseInt(tokens[1]);
            } else {
                threadId = Integer.parseInt(threadAttributeName);
            }
        } catch (NumberFormatException e1) {
            // pass
        }

        return new Pair<>(threadId, cpuId);
    }
}
