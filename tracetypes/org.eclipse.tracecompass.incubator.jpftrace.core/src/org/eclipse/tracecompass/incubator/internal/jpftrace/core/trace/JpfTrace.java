package org.eclipse.tracecompass.incubator.internal.jpftrace.core.trace;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.analysis.os.linux.core.trace.IKernelAnalysisEventLayout;
import org.eclipse.tracecompass.analysis.os.linux.core.trace.IKernelTrace;
import org.eclipse.tracecompass.incubator.internal.jpftrace.core.Activator;
import org.eclipse.tracecompass.incubator.internal.jpftrace.core.event.IJpfTraceConstants;
import org.eclipse.tracecompass.incubator.internal.jpftrace.core.event.JpfTraceAspects;
import org.eclipse.tracecompass.incubator.internal.jpftrace.core.event.JpfTraceEvent;
import org.eclipse.tracecompass.incubator.internal.jpftrace.core.event.JpfTraceField;
import org.eclipse.tracecompass.incubator.internal.jpftrace.core.event.JpfTraceEventTime;
import org.eclipse.tracecompass.incubator.internal.jpftrace.core.event.JpfTraceEventLayout;
import org.eclipse.tracecompass.internal.provisional.jsontrace.core.trace.JsonTrace;
import org.eclipse.tracecompass.tmf.core.event.ITmfEvent;
import org.eclipse.tracecompass.tmf.core.event.ITmfLostEvent;
import org.eclipse.tracecompass.tmf.core.event.aspect.ITmfEventAspect;
import org.eclipse.tracecompass.tmf.core.exceptions.TmfTraceException;
import org.eclipse.tracecompass.tmf.core.io.BufferedRandomAccessFile;
import org.eclipse.tracecompass.tmf.core.timestamp.ITmfTimestamp;
import org.eclipse.tracecompass.tmf.core.timestamp.TmfTimestamp;
import org.eclipse.tracecompass.tmf.core.trace.ITmfContext;
import org.eclipse.tracecompass.tmf.core.trace.TmfContext;
import org.eclipse.tracecompass.tmf.core.trace.TmfTraceManager;
import org.eclipse.tracecompass.tmf.core.trace.TmfTraceUtils;
import org.eclipse.tracecompass.tmf.core.trace.TraceValidationStatus;
import org.eclipse.tracecompass.tmf.core.trace.location.ITmfLocation;
import org.eclipse.tracecompass.tmf.core.trace.location.TmfLongLocation;

import com.google.common.collect.Lists;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.stream.JsonReader;

public class JpfTrace extends JsonTrace implements IKernelTrace {

    private final @NonNull Iterable<@NonNull ITmfEventAspect<?>> fEventAspects;
    private static Integer fReaderScope;

    /**
     * Constructor
     */
    public JpfTrace() {
        fEventAspects = Lists.newArrayList(JpfTraceAspects.getAspects());
        fReaderScope = -1;
    }

    private static final void Log(String s){
        System.out.println(s);
    }

    @Override
    public void initTrace(IResource resource, String path, Class<? extends ITmfEvent> type) throws TmfTraceException {
        super.initTrace(resource, path, type);

        fProperties.put("Type", "JpfTrace");

        String dir = TmfTraceManager.getSupplementaryFileDir(this);

        // Log("JpfTrace::initTrace: opening " +  dir + " " + path);
        fFile = new File(dir + new File(path).getName());
        if (!fFile.exists()) {
            Job sortJob = new JpfTraceSortingJob(this, path);
            sortJob.schedule();
            while (sortJob.getResult() == null) {
                try {
                    sortJob.join();
                } catch (InterruptedException e) {
                    throw new TmfTraceException(e.getMessage(), e);
                }
            }

            IStatus result = sortJob.getResult();
            if (!result.isOK()) {
                throw new TmfTraceException("Job failed " + result.getMessage());
            }
            // Log("JpfTrace::initTrace: Sorting Job Complete");
        }
        // Log("JpfTrace::initTrace: try getting timestamp");
        try {
            fFileInput = new BufferedRandomAccessFile(fFile, "r");

            registerBaseTimestamp(path);
            // Log("JpfTrace::initTrace: baseTimestamp:" + String.valueOf(JpfTraceField.getPseudoTime()));

            goToCorrectStart(fFileInput);
            // Log("JpfTrace::initTrace complete");
        } catch (IOException e) {
            throw new TmfTraceException(e.getMessage(), e);
        }
    }

    private static void registerBaseTimestamp(String path) {
        try (FileReader fileReader = new FileReader(path)) {
            try (JsonReader reader = new JsonReader(fileReader);) {
                Gson gson = new Gson();

                JsonObject root = gson.fromJson(reader, JsonObject.class);
                JsonElement jsonElement = root.get("time");
                long baseTime = (jsonElement != null) ? jsonElement.getAsLong() : 0L;
                JpfTraceEventTime.setPseudoBaseTime(baseTime);
            }

        } catch (IOException e) {
            Log("JpfTrace::getBaseTimestamp: IOException");
        }

    }

    @Override
    public IStatus validate(IProject project, String path) {
        // Log("JPF::validate");

        File file = new File(path);
        if (!file.exists()) {
            return new Status(IStatus.ERROR, Activator.PLUGIN_ID, "File not found: " + path);
        }
        if (!file.isFile()) {
            return new Status(IStatus.ERROR, Activator.PLUGIN_ID, "Not a file. It's a directory: " + path);
        }
        int confidence = 0;

        try {
            if (!TmfTraceUtils.isText(file)) {
                return new TraceValidationStatus(confidence, Activator.PLUGIN_ID);
            }
        } catch (IOException e) {
            Activator.getInstance().logError("Error validating file: " + path, e);
            return new Status(IStatus.ERROR, Activator.PLUGIN_ID, "IOException validating file: " + path, e);
        }
        try (BufferedRandomAccessFile rafile = new BufferedRandomAccessFile(path, "r")) {
            goToCorrectStart(rafile);
            int lineCount = 0;
            int matches = 0;
            String line = readNextEventString(() -> rafile.read());
            // Log("JPF::validate: " + line);

            while ((line != null) && (lineCount++ < MAX_LINES)) {
                try {
                    JpfTraceField field = JpfTraceField.parseJson(line);
                    if (field!= null) {
                        matches++;
                    }
                } catch (RuntimeException e) {
                    confidence = Integer.MIN_VALUE;
                }

                confidence = MAX_CONFIDENCE * matches / lineCount;
                line = readNextEventString(() -> rafile.read());
            }
            if (matches == 0) {
                return new Status(IStatus.ERROR, Activator.PLUGIN_ID, "Most assuredly NOT a JPF trace");
            }
        } catch (IOException e) {
            Activator.getInstance().logError("Error validating file: " + path, e);
            return new Status(IStatus.ERROR, Activator.PLUGIN_ID, "IOException validating file: " + path, e);
        }
        return new TraceValidationStatus(confidence, Activator.PLUGIN_ID);
    }

    private static void goToCorrectStart(RandomAccessFile rafile) throws IOException {

        String startKey = "\"transitions\""; //$NON-NLS-1$

        StringBuilder sb = new StringBuilder();
        int val = rafile.read();

        // Skip list contains all the odd control characters
        Set<Integer> skipList = new HashSet<>();
        skipList.add((int) ':');
        skipList.add((int) '\t');
        skipList.add((int) '\n');
        skipList.add((int) '\r');
        skipList.add((int) ' ');
        skipList.add((int) '\b');
        skipList.add((int) '\f');

        // feed file input to \"transitions\"
        while (val != -1 && val != ':' && sb.length() < 30) {
            if (!skipList.contains(val)) {
                sb.append((char) val);
            }
            val = rafile.read();
        }

        // For JPF traces, the first colon should have the KEY "transitions" on its left
        if (!(sb.toString().startsWith('{' + startKey) && rafile.length() > 30)) {
            rafile.seek(0);
        }

        // reset the scope counter to -1
        resetGlobalReaderScope();
    }

    private static void resetGlobalReaderScope() {
        fReaderScope = -1;
    }

    @Override
    public Iterable<@NonNull ITmfEventAspect<?>> getEventAspects() {
        return fEventAspects;
    }

    // override function for finding next JPF event
    public static @Nullable String readNextEventString(IReaderWrapper parser) throws IOException {

        // there are three cases,
        // 1. the next event locates right after the braces {} of this one,
        // 2. the next event locates in the same transition, but another brace {}
        // 3. the next event locates in the next transition

        // simply: only strings inside braces with scope = 1 will be recorded as event

        // Log("readNextEventString: " + String.valueOf(fReaderScope));

        StringBuilder sb = new StringBuilder();

        int elem = parser.read();
        boolean inQuotes = false;

        while (elem != -1) {
            if (elem == '"') {
                inQuotes = !inQuotes;
            } else {
                if (inQuotes) {
                    // do nothing, continue to read
                } else if (elem == '{') {
                    fReaderScope++;
                } else if (elem == '}') {
                    fReaderScope--;
                    if (fReaderScope == 0) {
                        sb.append((char) elem);
                        // Log("readNextEventString::" + sb.toString());
                        return sb.toString();
                    }
                }
            }
            if (fReaderScope >= 1) {
                sb.append((char) elem);
            }
            elem = parser.read();
        }
        return null;
    }

    @Override
    public ITmfEvent parseEvent(ITmfContext context) {
        // Log("JpfTrace::parseEvent: called");
        @Nullable
        ITmfLocation location = context.getLocation();
        if (location instanceof TmfLongLocation) {
            TmfLongLocation tmfLongLocation = (TmfLongLocation) location;
            Long locationInfo = tmfLongLocation.getLocationInfo();
            if (location.equals(NULL_LOCATION)) {
                locationInfo = 0L;
            }
            try {
                if (!locationInfo.equals(fFileInput.getFilePointer())) {
                    fFileInput.seek(locationInfo);
                }
                if (locationInfo < 10) {
                    // Log("parseEvent: reset location: before: " + String.valueOf(locationInfo));
                    goToCorrectStart(fFileInput);
                }
                String nextJson = readNextEventString(() -> fFileInput.read());
                if (nextJson != null) {
                    // Log("current location: " + String.valueOf(getCurrentLocation()));
                    JpfTraceField field = JpfTraceField.parseJson(nextJson);
                    if (field == null) {
                        return null;
                    }
                    // Log("context rank: " + String.valueOf(context.getRank()));
                    return new JpfTraceEvent(this, context.getRank(), field);
                }
            } catch (IOException e) {
                Activator.getInstance().logError("Error parsing event", e);
            }
        }
        return null;
    }

    @Override
    protected synchronized void updateAttributes(final ITmfContext context, final @NonNull ITmfEvent event) {
        ITmfTimestamp timestamp = event.getTimestamp();
        Long duration = event.getContent().getFieldValue(Long.class, IJpfTraceConstants.DURATION);
        ITmfTimestamp endTime = duration != null ? TmfTimestamp.fromNanos(timestamp.toNanos() + duration) : timestamp;
        if (event instanceof ITmfLostEvent) {
            endTime = ((ITmfLostEvent) event).getTimeRange().getEndTime();
        }
        if (getStartTime().equals(TmfTimestamp.BIG_BANG) || (getStartTime().compareTo(timestamp) > 0)) {
            setStartTime(timestamp);
        }
        if (getEndTime().equals(TmfTimestamp.BIG_CRUNCH) || (getEndTime().compareTo(endTime) < 0)) {
            setEndTime(endTime);
        }
        if (context.hasValidRank()) {
            long rank = context.getRank();
            // Log("updateAttributes: has valid rank: " + String.valueOf(rank));
            if (getNbEvents() <= rank) {
                setNbEvents(rank + 1);
            }
            if (getIndexer() != null) {
                // Log("updateAttributes: has valid indexer");
                getIndexer().updateIndex(context, timestamp);
            }
        }
    }


    @Override
    public synchronized ITmfContext seekEvent(final ITmfTimestamp timestamp) {
        // A null timestamp indicates to seek the first event
        if (timestamp == null) {
            Log("seekEvent: timestamp is null");
            ITmfContext context = seekEvent((ITmfLocation) null);
            context.setRank(0);
            return context;
        }

        // Position the trace at the checkpoint
        // Log("Timestamp: " + String.valueOf(timestamp.getValue()));
        // ITmfContext context = fIndexer.seekIndex(timestamp);
        ITmfContext context = new TmfContext(new TmfLongLocation(0L), 0L);

        // And locate the requested event context
        ITmfLocation previousLocation = context.getLocation();
        long previousRank = context.getRank();
        ITmfEvent event = getNext(context);
        // int counter = 0;
        // Log("Queried Timestamp: " + String.valueOf(timestamp.getValue()));
        // Log("This Timestamp: " + String.valueOf(event.getTimestamp().getValue()));
        while (event != null && event.getTimestamp().compareTo(timestamp) < 0) {
            previousLocation = context.getLocation();
            previousRank = context.getRank();
            event = getNext(context);
            // if (event == null) {
                // Log("Retrieved event is null");
            // } else {
                // Log("Query Timestamp: " + String.valueOf(timestamp.getValue()));
                // Log("This Timestamp: " + String.valueOf(event.getTimestamp().getValue()));
            // }
            // Log("Counter: " + String.valueOf(counter++));

        }
        if (event == null) {
            context.setLocation(null);
            context.setRank(ITmfContext.UNKNOWN_RANK);
        } else {
            context.dispose();
            context = seekEvent(previousLocation);
            context.setRank(previousRank);
        }
        return context;
    }

    @Override
    public synchronized ITmfEvent getNext(final ITmfContext context) {
        // parseEvent() does not update the context
        final ITmfEvent event = parseEvent(context);
        if (event != null) {
            updateAttributes(context, event);
            context.setLocation(getCurrentLocation());
            context.increaseRank();
            // Log("getNext: " + String.valueOf(context.getRank()));
        }
        return event;
    }

    @Override
    public IKernelAnalysisEventLayout getKernelEventLayout() {
        return JpfTraceEventLayout.getInstance();
    }
}
