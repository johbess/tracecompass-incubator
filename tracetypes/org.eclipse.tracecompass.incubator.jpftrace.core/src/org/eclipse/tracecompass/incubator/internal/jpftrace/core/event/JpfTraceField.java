package org.eclipse.tracecompass.incubator.internal.jpftrace.core.event;

import java.util.HashMap;
import java.util.Map;
import java.util.ArrayList;

import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.tmf.core.event.ITmfEventField;
import org.eclipse.tracecompass.tmf.core.event.TmfEventField;

import org.eclipse.tracecompass.analysis.os.linux.core.kernel.LinuxValues;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/**
 * JPF Trace fields.
 */
public class JpfTraceField {

    private static Integer sThreadId = -1;
    private static String sThreadName = "";
    // private static Long pseudoTime = 0L;
    public static final Long sDuration = 10L;

    private final String fType;
    private final Integer fThreadId;
    private final String fThreadName;
    private final long fTimestamp;

    private final ArrayList<String> fChoices;
    private ITmfEventField fContent;

    private static final Gson G_SON = new Gson();

    private JpfTraceField(Map<String, Object> fields) {
        fType = (String) fields.get(IJpfTraceConstants.TYPE);
        fThreadId = (Integer) fields.get(IJpfTraceConstants.THREAD_ID);
        fThreadName = (String) fields.get(IJpfTraceConstants.THREAD_NAME);

        Integer id = (Integer) fields.get(IJpfTraceConstants.ID);
        if (id != null) {
            // getEventTime() automatically assign the duration to event
            // if want to manually set the duration, try overloading this method
            fTimestamp = JpfTraceEventTime.getEventTime(id);
        } else {
            System.out.println("JpfTraceField, warning: event doesn't have an ID");
            fTimestamp = JpfTraceEventTime.getPseudoBaseTime();
        }

        fChoices = new ArrayList<>();

        ITmfEventField[] array = fields.entrySet().stream()
                .map(entry -> new TmfEventField(entry.getKey(), entry.getValue(), null))
                .toArray(ITmfEventField[]::new);

        fContent = new TmfEventField(ITmfEventField.ROOT_FIELD_ID, fields, array);
    }

    private JpfTraceField(Map<String, Object> fields, ArrayList<String> choices) {
        fType = (String) fields.get(IJpfTraceConstants.TYPE);
        fThreadId = (Integer) fields.get(IJpfTraceConstants.THREAD_ID);
        fThreadName = (String) fields.get(IJpfTraceConstants.THREAD_NAME);

        Integer id = (Integer) fields.get(IJpfTraceConstants.ID);
        if (id != null) {
            // getEventTime() automatically assign the duration to event
            // if want to manually set the duration, try overloading this method
            fTimestamp = JpfTraceEventTime.getEventTime(id);
        } else {
            System.out.println("JpfTraceField, warning: event doesn't have an ID");
            fTimestamp = JpfTraceEventTime.getPseudoBaseTime();
        }

        fChoices = choices;

        ITmfEventField[] array = fields.entrySet().stream()
                .map(entry -> new TmfEventField(entry.getKey(), entry.getValue(), null))
                .toArray(ITmfEventField[]::new);

        fContent = new TmfEventField(ITmfEventField.ROOT_FIELD_ID, fields, array);
    }

    // private static final void Log(String s){
    //     System.out.println(s);
    // }

    // public static void setPseudoTime(long value) {
    //     pseudoTime = value;
    // }

    // public static long getPseudoTime() {
    //     return pseudoTime;
    // }

    /**
     * Parse a JSON string
     *
     * @param fieldsString
     *            the string
     * @param processField
     *            the process name and tags
     * @return an event field
     */
    public static @Nullable JpfTraceField parseJson(String fieldsString) {
        // Log("JpfTraceField::parseJson " + fieldsString);

        @Nullable
        JsonObject root = G_SON.fromJson(fieldsString, JsonObject.class);

        Map<String, Object> fieldsMap = new HashMap<>();
        fieldsMap.put(IJpfTraceConstants.DURATION, sDuration);

        Integer id = optInt(root, IJpfTraceConstants.ID);
        if (id >= 0) {
            fieldsMap.put(IJpfTraceConstants.ID, id);
        }

        // if the event is thread info
        if (optString(root, IJpfTraceConstants.THREAD_NAME) != null) {
            // get common k-v pairs of threadInfo
            Integer threadId = optInt(root, IJpfTraceConstants.THREAD_ID);
            String threadName = optString(root, IJpfTraceConstants.THREAD_NAME);
            String threadState = optString(root, IJpfTraceConstants.THREAD_STATE);
            String threadEntryMethod = optString(root, IJpfTraceConstants.THREAD_ENTRY_METHOD);

            // update static fields: thread id and name
            sThreadId = threadId;

            if (threadName != null) {
                sThreadName = threadName;
            }

            fieldsMap.put(IJpfTraceConstants.THREAD_ID, threadId);
            fieldsMap.put(IJpfTraceConstants.THREAD_NAME, threadName);
            fieldsMap.put(IJpfTraceConstants.THREAD_STATE, threadState);
            fieldsMap.put(IJpfTraceConstants.THREAD_ENTRY_METHOD, threadEntryMethod);

            // threadinfo + threadswitch
            if (optBoolean(root, IJpfTraceConstants.THREAD_SWITCH) != null) {
                String prevComm = optString(root, IJpfTraceConstants.PREV_COMM);
                Integer prevPid = optInt(root, IJpfTraceConstants.PREV_PID);
                String nextComm = optString(root, IJpfTraceConstants.NEXT_COMM);
                Integer nextPid = optInt(root, IJpfTraceConstants.NEXT_PID);

                fieldsMap.put(IJpfTraceConstants.TYPE, "ThreadSwitch");
                fieldsMap.put(IJpfTraceConstants.THREAD_ID, sThreadId);
                fieldsMap.put(IJpfTraceConstants.THREAD_NAME, sThreadName);

                fieldsMap.put(IJpfTraceConstants.PREV_COMM, prevComm);
                fieldsMap.put(IJpfTraceConstants.PREV_PID, prevPid);
                fieldsMap.put(IJpfTraceConstants.NEXT_COMM, nextComm);
                fieldsMap.put(IJpfTraceConstants.NEXT_PID, nextPid);

                fieldsMap.put(IJpfTraceConstants.PREV_PRIO, 100);
                fieldsMap.put(IJpfTraceConstants.NEXT_PRIO, 100);
                fieldsMap.put(IJpfTraceConstants.PREV_STATE, (long) LinuxValues.TASK_STATE_RUNNING);

                // TODO
                return new JpfTraceField(fieldsMap);
            }

            // threadinfo + threadawake
            if (optBoolean(root, IJpfTraceConstants.THREAD_AWAKE) != null) {

                String comm = optString(root, IJpfTraceConstants.COMM);
                Integer tid = optInt(root, IJpfTraceConstants.TID);

                fieldsMap.put(IJpfTraceConstants.TYPE, "ThreadAwake");
                fieldsMap.put(IJpfTraceConstants.THREAD_ID, sThreadId);
                fieldsMap.put(IJpfTraceConstants.THREAD_NAME, sThreadName);

                fieldsMap.put(IJpfTraceConstants.COMM, comm);
                fieldsMap.put(IJpfTraceConstants.TID, tid);
                fieldsMap.put(IJpfTraceConstants.PRIO, 100);
                fieldsMap.put(IJpfTraceConstants.SUCCESS, 1);
                fieldsMap.put(IJpfTraceConstants.TARGET_CPU, 0);

                // TODO
                return new JpfTraceField(fieldsMap);
            }

            fieldsMap.put(IJpfTraceConstants.TYPE, "ThreadInfo");
            return new JpfTraceField(fieldsMap);
        }

        // choice info event
        if (optString(root, IJpfTraceConstants.CHOICE_ID) != null) {
            String choiceId = optString(root, IJpfTraceConstants.CHOICE_ID);
            String currentChoice = optString(root, IJpfTraceConstants.CURRENT_CHOICE);

            ArrayList<String> choices = new ArrayList<>();
            JsonArray choiceArray = optJSONArray(root, IJpfTraceConstants.CHOICES);
            if (choiceArray != null) {
                for (int i = 0; i < choiceArray.size(); i++ ) {
                    String choiceName = choiceArray.get(i).getAsString();
                    choices.add(choiceName);
                }
            }

            // TODO: return JpfTraceField
            fieldsMap.put(IJpfTraceConstants.TYPE, "ChoiceInfo");
            fieldsMap.put(IJpfTraceConstants.THREAD_ID, sThreadId);
            fieldsMap.put(IJpfTraceConstants.THREAD_NAME, sThreadName);

            fieldsMap.put(IJpfTraceConstants.CHOICE_ID, choiceId);
            fieldsMap.put(IJpfTraceConstants.CURRENT_CHOICE, currentChoice);

            return new JpfTraceField(fieldsMap, choices);
        }


        // instruction event
        if (optString(root, IJpfTraceConstants.SRC) != null) {
            String src = optString(root, IJpfTraceConstants.SRC);

            Integer stepLocation = optInt(root, IJpfTraceConstants.STEP_LOCATION);
            String fileLocation = optString(root, IJpfTraceConstants.FILE_LOCATION);

            fieldsMap.put(IJpfTraceConstants.SRC, src);
            fieldsMap.put(IJpfTraceConstants.STEP_LOCATION, stepLocation);
            fieldsMap.put(IJpfTraceConstants.FILE_LOCATION, fileLocation);

            Boolean isSync = optBoolean(root, IJpfTraceConstants.IS_SYNCHRONIZED);
            if (isSync != null ) {
                String syncMethodName = optString(root, IJpfTraceConstants.SYNC_METHOD_NAME );

                fieldsMap.put(IJpfTraceConstants.IS_SYNCHRONIZED, isSync);
                fieldsMap.put(IJpfTraceConstants.SYNC_METHOD_NAME, syncMethodName);
            }

            Boolean isMethodReturn = optBoolean(root, IJpfTraceConstants.IS_METHOD_RETURN);
            if (isMethodReturn != null) {
                String returnedMethodName = optString(root, IJpfTraceConstants.RETURNED_METHOD_NAME );

                fieldsMap.put(IJpfTraceConstants.IS_METHOD_RETURN, isMethodReturn);
                fieldsMap.put(IJpfTraceConstants.RETURNED_METHOD_NAME, returnedMethodName);
            }

            Boolean isMethodCall = optBoolean(root, IJpfTraceConstants.IS_METHOD_CALL);
            if (isMethodCall != null) {
                String calledMethodName = optString(root, IJpfTraceConstants.CALLED_METHOD_NAME );

                fieldsMap.put(IJpfTraceConstants.IS_METHOD_CALL, isMethodCall);
                fieldsMap.put(IJpfTraceConstants.CALLED_METHOD_NAME, calledMethodName);
            }

            Boolean isThreadRelatedMethod = optBoolean(root, IJpfTraceConstants.IS_THREAD_RELATED_METHOD);
            if (isThreadRelatedMethod != null) {
                String threadRelatedMethod = optString(root, IJpfTraceConstants.THREAD_RELATED_METHOD );

                fieldsMap.put(IJpfTraceConstants.IS_THREAD_RELATED_METHOD, isThreadRelatedMethod);
                fieldsMap.put(IJpfTraceConstants.THREAD_RELATED_METHOD, threadRelatedMethod);
            }

            Boolean isFieldAccess = optBoolean(root, IJpfTraceConstants.IS_FIELD_ACCESS);
            if (isFieldAccess != null) {
                String accessedField = optString(root, IJpfTraceConstants.ACCESSED_FIELD );

                fieldsMap.put(IJpfTraceConstants.IS_FIELD_ACCESS, isFieldAccess);
                fieldsMap.put(IJpfTraceConstants.ACCESSED_FIELD, accessedField);
            }

            fieldsMap.put(IJpfTraceConstants.TYPE, "Instruction");
            fieldsMap.put(IJpfTraceConstants.THREAD_ID, sThreadId);
            fieldsMap.put(IJpfTraceConstants.THREAD_NAME, sThreadName);

            // TODO
            return new JpfTraceField(fieldsMap);
        }

        System.out.println("Not recognized as a JPF Trace Event: " + fieldsString);
        return null;
    }

    private static int optInt(JsonObject root, String key) {
        JsonElement jsonElement = root.get(key);
        return jsonElement != null ? jsonElement.getAsInt() : Integer.MIN_VALUE;
    }

    private static @Nullable JsonArray optJSONArray(JsonObject root, String key) {
        JsonElement jsonElement = root.get(key);
        return jsonElement != null ? jsonElement.getAsJsonArray() : null;
    }

    private static @Nullable String optString(JsonObject root, String key) {
        JsonElement jsonElement = root.get(key);
        return jsonElement != null ? jsonElement.getAsString() : null;
    }

    private static @Nullable Boolean optBoolean(JsonObject root, String key) {
        JsonElement jsonElement = root.get(key);
        if (jsonElement != null) {
            return jsonElement.getAsBoolean();
        }
        return null;
    }

    public String getThreadName(){
        return fThreadName;
    }
    /**
     * Get the event content
     *
     * @return the event content
     */
    public ITmfEventField getContent() {
        return fContent;
    }

    public long getTimestamp() {
        return fTimestamp;
    }

    public String getType() {
        return fType;
    }

    public int getThreadId() {
        return fThreadId;
    }

    public String getThreadState() {
        String threadState = fContent.getFieldValue(String.class, IJpfTraceConstants.THREAD_STATE);
        return (threadState != null) ? threadState : "" ;
    }

    public String getChoiceId() {
        String choice = fContent.getFieldValue(String.class, IJpfTraceConstants.CHOICE_ID);
        return (choice != null) ? choice : "" ;
    }

    public String getSource() {
        String src = fContent.getFieldValue(String.class, IJpfTraceConstants.SRC);
        return (src != null) ? src : "" ;
    }

    public ArrayList<String> getChoices() {
        return fChoices;
    }

    public String[] getInstructionSpecAndDetail() {

        String src = fContent.getFieldValue(String.class, IJpfTraceConstants.SRC);
        if (src == null) {
            // System.out.println("JpfTraceField::getInstructionSpecAndDetail: source code not found");
            return new String[] {"", ""};
        }

        StringBuilder specBuilder = new StringBuilder();
        StringBuilder detailBuilder = new StringBuilder();
        boolean isSpec = false;

        Boolean isSync = fContent.getFieldValue(Boolean.class, IJpfTraceConstants.IS_SYNCHRONIZED);
        if (isSync != null) {
            specBuilder.append("Sync");
            String detail = fContent.getFieldValue(String.class, IJpfTraceConstants.SYNC_METHOD_NAME);
            if (detail != null) {
                detailBuilder.append(detail);
            } else {
                System.out.println("JpfTraceField::getInstructionSpecAndDetail: Warning, the insn is SYNC but has no detail");
            }
            isSpec = true;
        }

        Boolean isMethodCall = fContent.getFieldValue(Boolean.class, IJpfTraceConstants.IS_METHOD_CALL);
        if (isMethodCall != null) {
            if (isSpec) {
                specBuilder.append(" | ");
                detailBuilder.append(" | ");
            }

            specBuilder.append("MethodCall");
            String detail = fContent.getFieldValue(String.class, IJpfTraceConstants.CALLED_METHOD_NAME);
            if (detail != null) {
                detailBuilder.append(detail);
            } else {
                System.out.println("JpfTraceField::getInstructionSpecAndDetail: Warning, the insn is METHOD CALL but has no detail");
            }
            isSpec = true;
        }

        Boolean isThreadRelatedMethod = fContent.getFieldValue(Boolean.class, IJpfTraceConstants.IS_THREAD_RELATED_METHOD);
        if (isThreadRelatedMethod != null) {
            if (isSpec) {
                specBuilder.append(" | ");
                detailBuilder.append(" | ");
            }

            specBuilder.append("Lock/Unlock");
            String detail = fContent.getFieldValue(String.class, IJpfTraceConstants.THREAD_RELATED_METHOD);
            if (detail != null) {
                detailBuilder.append(detail);
            } else {
                System.out.println("JpfTraceField::getInstructionSpecAndDetail: Warning, the insn is a LOCK/UNLOCK but has no detail");
            }
            isSpec = true;
        }

        Boolean isFieldAccess = fContent.getFieldValue(Boolean.class, IJpfTraceConstants.IS_FIELD_ACCESS);
        if (isFieldAccess != null) {
            if (isSpec) {
                specBuilder.append(" | ");
                detailBuilder.append(" | ");
            }

            specBuilder.append("FieldAccess");
            String detail = fContent.getFieldValue(String.class, IJpfTraceConstants.ACCESSED_FIELD);
            if (detail != null) {
                detailBuilder.append(detail);
            } else {
                System.out.println("JpfTraceField::handleEvent: Warning, the insn is a FIELD ACCESS but has no detail");
            }
            isSpec = true;
        }

        return new String[] {specBuilder.toString(), detailBuilder.toString()};
    }
}
