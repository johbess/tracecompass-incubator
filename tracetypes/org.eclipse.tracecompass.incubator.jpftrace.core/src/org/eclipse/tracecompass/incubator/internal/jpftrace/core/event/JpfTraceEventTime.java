package org.eclipse.tracecompass.incubator.internal.jpftrace.core.event;

import java.util.List;
import java.util.ArrayList;

public class JpfTraceEventTime {
    private static Long baseTime = 0L;
    private static Long stepTime = 10L;
    private static List<Long> durations = new ArrayList<>();

    private static void Error(String s) {
        System.err.println(s);
    }

    public static void setPseudoBaseTime(Long t) {
        baseTime = t;
    }

    public static Long getPseudoBaseTime() {
        return baseTime;
    }

    public static void calculateEventTime(int id) {
        Long t = stepTime;
        durations.add(t);

        if (durations.size() != id + 1) {
            Error("JpfTraceEventTime::calculateEventTime: List of event doesn't match in length");
        }
    }

    public static Long getEventTime(int id) {
        Long t = baseTime;
        for (int i = 0; i < id; i++ ) {
            if (i < durations.size()){
                t += durations.get(i);
            } else {
                Error("JpfTraceEventTime::getEventTime: index out of range");
            }
        }
        if (id > durations.size()) {
            Error("JpfTraceEventTime::getEventTime: unexpected index ");
        }

        if (id == durations.size()) {
            calculateEventTime(id);
        }

        return t;
    }

    public static Long getTimeStep() {
        return stepTime;
    }
}
