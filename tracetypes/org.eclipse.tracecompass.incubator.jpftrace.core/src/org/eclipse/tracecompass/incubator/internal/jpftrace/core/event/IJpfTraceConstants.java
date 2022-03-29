package org.eclipse.tracecompass.incubator.internal.jpftrace.core.event;

/**
 * @author Johan Besseling
 *
 */
public interface IJpfTraceConstants {

    String ID = "id"; //$NON-NLS-1$

    String BASE_TIME = "time"; //$NON-NLS-1$

    String THREAD_INFO = "threadInfo"; //$NON-NLS-1$

    String THREAD_NAME = "threadName"; //$NON-NLS-1$

    String THREAD_ID = "threadId"; //$NON-NLS-1$

    String THREAD_ENTRY_METHOD = "threadEntryMethod"; //$NON-NLS-1$

    String THREAD_STATE = "threadState"; //$NON-NLS-1$

    String TRANSITIONS = "transitions"; //$NON-NLS-1$

    String CHOICE_INFO = "choiceInfo"; //$NON-NLS-1$

    String CHOICE_ID = "choiceId"; //$NON-NLS-1$

    String NUM_CHOICES = "numChoices"; //$NON-NLS-1$

    String CURRENT_CHOICE = "currentChoice"; //$NON-NLS-1$

    String CHOICES = "choices"; //$NON-NLS-1$

    String STEPS = "steps"; //$NON-NLS-1$

    String NUM_STEPS = "numSteps"; //$NON-NLS-1$

    String INSTRUCTIONS = "instructions"; //$NON-NLS-1$

    String STEP_LOCATION = "stepLocation"; //$NON-NLS-1$

    String FILE_LOCATION = "fileLocation"; //$NON-NLS-1$

    String IS_VIRTUAL_INV = "isVirtualInv"; //$NON-NLS-1$

    String IS_FIELD_INST = "isFieldInst"; //$NON-NLS-1$

    String IS_LOCK_INST = "isLockInst"; //$NON-NLS-1$

    String LOCK_FIELD_NAME = "lockFieldName"; //$NON-NLS-1$

    String IS_JVM_INVOK = "isJVMInvok"; //$NON-NLS-1$

    String IS_JVM_RETURN = "isJVMReturn"; //$NON-NLS-1$

    String IS_SYNCHRONIZED = "isSynchronized"; //$NON-NLS-1$

    String SYNC_METHOD_NAME = "syncMethodName"; //$NON-NLS-1$

    String IS_METHOD_RETURN = "isMethodReturn"; //$NON-NLS-1$

    String RETURNED_METHOD_NAME = "returndMethodName"; //$NON-NLS-1$

    String IS_METHOD_CALL = "isMethodCall"; //$NON-NLS-1$

    String CALLED_METHOD_NAME = "calledMethodName"; //$NON-NLS-1$

    String IS_THREAD_RELATED_METHOD = "isThreadRelatedMethod"; //$NON-NLS-1$

    String THREAD_RELATED_METHOD = "threadRelatedMethod"; //$NON-NLS-1$

    String IS_FIELD_ACCESS = "isFieldAccess"; //$NON-NLS-1$

    String ACCESSED_FIELD = "accessedField"; //$NON-NLS-1$

    String TYPE = "type"; //$NON-NLS-1$

    String PREV_PID = "prevTid"; //$NON-NLS-1$

    String PREV_COMM = "prevThreadName"; //$NON-NLS-1$

    String PREV_PRIO = "prev_prio"; //$NON-NLS-1$

    String PREV_STATE = "prevState"; //$NON-NLS-1$

    String NEXT_PID = "nextTid"; //$NON-NLS-1$

    String NEXT_COMM = "nextThreadName"; //$NON-NLS-1$

    String NEXT_PRIO = "next_prio"; //$NON-NLS-1$

    String COMM = "currentThreadName"; //$NON-NLS-1$

    String TID = "tid"; //$NON-NLS-1$

    String PRIO = "prio"; //$NON-NLS-1$

    String SUCCESS = "success"; //$NON-NLS-1$

    String TARGET_CPU = "target_cpu"; //$NON-NLS-1$

    String DURATION = "duration"; //$NON-NLS-1$

    String THREAD_SWITCH = "threadSwitch"; //$NON-NLS-1$

    String THREAD_AWAKE = "threadAwake"; //$NON-NLS-1$

    String SRC = "src"; //$NON-NLS-1$

    String NO_SRC = "noSrc"; //$NON-NLS-1$
}
