package org.eclipse.tracecompass.incubator.internal.jpftrace.core.analysis.threadstatus;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NavigableSet;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Predicate;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.analysis.os.linux.core.model.ProcessStatus;
import org.eclipse.tracecompass.internal.analysis.os.linux.core.Activator;
import org.eclipse.tracecompass.internal.analysis.os.linux.core.kernel.StateValues;
import org.eclipse.tracecompass.internal.analysis.os.linux.core.threadstatus.ThreadEntryModel;
import org.eclipse.tracecompass.internal.tmf.core.analysis.callsite.CallsiteAnalysis;
import org.eclipse.tracecompass.internal.analysis.os.linux.core.registry.LinuxStyle;
import org.eclipse.tracecompass.internal.tmf.core.model.AbstractTmfTraceDataProvider;
import org.eclipse.tracecompass.internal.tmf.core.model.filters.FetchParametersUtils;
import org.eclipse.tracecompass.internal.provisional.tmf.core.model.annotations.Annotation;
import org.eclipse.tracecompass.internal.provisional.tmf.core.model.annotations.AnnotationModel;
import org.eclipse.tracecompass.internal.provisional.tmf.core.model.annotations.AnnotationCategoriesModel;
import org.eclipse.tracecompass.internal.provisional.tmf.core.model.annotations.IOutputAnnotationProvider;
import org.eclipse.tracecompass.statesystem.core.ITmfStateSystem;
import org.eclipse.tracecompass.statesystem.core.StateSystemUtils.QuarkIterator;
import org.eclipse.tracecompass.statesystem.core.exceptions.AttributeNotFoundException;
import org.eclipse.tracecompass.statesystem.core.exceptions.StateSystemDisposedException;
import org.eclipse.tracecompass.statesystem.core.exceptions.TimeRangeException;
import org.eclipse.tracecompass.statesystem.core.interval.ITmfStateInterval;
import org.eclipse.tracecompass.statesystem.core.interval.TmfStateInterval;
import org.eclipse.tracecompass.tmf.core.TmfStrings;
import org.eclipse.tracecompass.tmf.core.analysis.callsite.ITmfCallsiteResolver;
import org.eclipse.tracecompass.tmf.core.dataprovider.DataProviderParameterUtils;
import org.eclipse.tracecompass.tmf.core.event.aspect.ITmfEventAspect;
import org.eclipse.tracecompass.tmf.core.event.aspect.TmfCpuAspect;
import org.eclipse.tracecompass.tmf.core.event.lookup.ITmfCallsite;
import org.eclipse.tracecompass.tmf.core.model.CommonStatusMessage;
import org.eclipse.tracecompass.tmf.core.model.IOutputStyleProvider;
import org.eclipse.tracecompass.tmf.core.model.OutputElementStyle;
import org.eclipse.tracecompass.tmf.core.model.OutputStyleModel;
import org.eclipse.tracecompass.tmf.core.model.filters.SelectionTimeQueryFilter;
import org.eclipse.tracecompass.tmf.core.model.filters.TimeQueryFilter;
import org.eclipse.tracecompass.tmf.core.model.timegraph.ITimeGraphArrow;
import org.eclipse.tracecompass.tmf.core.model.timegraph.ITimeGraphDataProvider;
import org.eclipse.tracecompass.tmf.core.model.timegraph.ITimeGraphRowModel;
import org.eclipse.tracecompass.tmf.core.model.timegraph.ITimeGraphState;
import org.eclipse.tracecompass.tmf.core.model.timegraph.ITimeGraphStateFilter;
import org.eclipse.tracecompass.tmf.core.model.timegraph.TimeGraphArrow;
import org.eclipse.tracecompass.tmf.core.model.timegraph.TimeGraphEntryModel;
import org.eclipse.tracecompass.tmf.core.model.timegraph.TimeGraphModel;
import org.eclipse.tracecompass.tmf.core.model.timegraph.TimeGraphRowModel;
import org.eclipse.tracecompass.tmf.core.model.timegraph.TimeGraphState;
import org.eclipse.tracecompass.tmf.core.model.tree.TmfTreeModel;
import org.eclipse.tracecompass.tmf.core.response.ITmfResponse;
import org.eclipse.tracecompass.tmf.core.response.TmfModelResponse;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;
import org.eclipse.tracecompass.tmf.core.trace.TmfTraceUtils;
import org.eclipse.tracecompass.tmf.core.util.Pair;

import org.eclipse.tracecompass.incubator.internal.jpftrace.core.event.JpfTraceEventTime;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Iterators;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import com.google.common.collect.TreeMultimap;

public class JpfThreadStatusDataProvider extends AbstractTmfTraceDataProvider implements
    ITimeGraphDataProvider<@NonNull TimeGraphEntryModel>,
    IOutputStyleProvider,
    IOutputAnnotationProvider {

    public static final @NonNull String ID = "org.eclipse.tracecompass.incubator.internal.jpftrace.core.analysis.threadstatus.JpfThreadStatusDataProvider"; //$NON-NLS-1$

    public static final @NonNull String CPU = "cpu"; //$NON-NLS-1$

    public static final @NonNull String ACTIVE_THREAD_FILTER_KEY = "active_thread_filter"; //$NON-NLS-1$

    private static final String WILDCARD = "*"; //$NON-NLS-1$
    private static final Set<Integer> ACTIVE_STATES = ImmutableSet.of(StateValues.PROCESS_STATUS_RUN_USERMODE,
            StateValues.PROCESS_STATUS_RUN_SYSCALL, StateValues.PROCESS_STATUS_INTERRUPTED);

    private static final AtomicLong fAtomicLong = new AtomicLong();

    private static final @NonNull Map<@NonNull String, @NonNull OutputElementStyle> STYLE_MAP;
    private static final int LINK_VALUE = 8;
    private static final int SYNC_VALUE = 9;
    private static final int METHOD_CALL_VALUE = 10;
    private static final int LOCK_UNLOCK_VALUE = 11;
    private static final int FIELD_ACCESS_VALUE = 12;

    // private static final @NonNull Map<@NonNull String, @NonNull OutputElementStyle> STYLE_MAP = Collections.synchronizedMap(new HashMap<>());

    static {
        ImmutableMap.Builder<@NonNull String, @NonNull OutputElementStyle> builder = new ImmutableMap.Builder<>();

        builder.put(LinuxStyle.UNKNOWN.getLabel(), new OutputElementStyle(null, LinuxStyle.UNKNOWN.toMap()));
        builder.put(LinuxStyle.USERMODE.getLabel(), new OutputElementStyle(null, LinuxStyle.USERMODE.toMap()));
        builder.put(LinuxStyle.SYSCALL.getLabel(), new OutputElementStyle(null, LinuxStyle.SYSCALL.toMap()));
        builder.put(LinuxStyle.INTERRUPTED.getLabel(), new OutputElementStyle(null, LinuxStyle.INTERRUPTED.toMap()));
        builder.put(LinuxStyle.WAIT_BLOCKED.getLabel(), new OutputElementStyle(null, LinuxStyle.WAIT_BLOCKED.toMap()));
        builder.put(LinuxStyle.WAIT_FOR_CPU.getLabel(), new OutputElementStyle(null, LinuxStyle.WAIT_FOR_CPU.toMap()));
        builder.put(LinuxStyle.WAIT_UNKNOWN.getLabel(), new OutputElementStyle(null, LinuxStyle.WAIT_UNKNOWN.toMap()));
        builder.put(LinuxStyle.LINK.getLabel(), new OutputElementStyle(null, LinuxStyle.LINK.toMap()));
        builder.put(JpfThreadStyle.SYNC.getLabel(), new OutputElementStyle(null, JpfThreadStyle.SYNC.toMap()));
        builder.put(JpfThreadStyle.METHOD_CALL.getLabel(), new OutputElementStyle(null, JpfThreadStyle.METHOD_CALL.toMap()));
        builder.put(JpfThreadStyle.LOCK_UNLOCK.getLabel(), new OutputElementStyle(null, JpfThreadStyle.LOCK_UNLOCK.toMap()));
        builder.put(JpfThreadStyle.FIELD_ACCESS.getLabel(), new OutputElementStyle(null, JpfThreadStyle.FIELD_ACCESS.toMap()));
        STYLE_MAP = builder.build();
    }

    private final JpfAnalysisModule fModule;
    private final long fTraceId = fAtomicLong.getAndIncrement();

    private final Map<Long, Integer> fQuarkMap = new HashMap<>();

    private final Map<Pair<Integer, Integer>, ThreadEntryModel.Builder> fBuildMap = new HashMap<>();

    private long fLastEnd = Long.MIN_VALUE;

    private @Nullable TimeGraphEntryModel fTraceEntry = null;

    private final TreeMultimap<Integer, ThreadEntryModel.Builder> fTidToEntry = TreeMultimap.create(Comparator.naturalOrder(),
            Comparator.comparing(ThreadEntryModel.Builder::getStartTime));

    private final Map<Long, @NonNull Multimap<@NonNull String, @NonNull Object>> fEntryMetadata = new HashMap<>();

    private static final @NonNull List<String> ANNOTATION_CAT_LIST;

    static {
        ImmutableList.Builder<@NonNull String> builder = new ImmutableList.Builder<>();

        builder.add(JpfThreadStyle.SYNC.getLabel());
        builder.add(JpfThreadStyle.METHOD_CALL.getLabel());
        builder.add(JpfThreadStyle.LOCK_UNLOCK.getLabel());
        builder.add(JpfThreadStyle.FIELD_ACCESS.getLabel());
        ANNOTATION_CAT_LIST = builder.build();
    }

    // private final @NonNull Map<String, Collection<Annotation>> ANNOTATION_MAP = new HashMap<>();

    public JpfThreadStatusDataProvider(@NonNull ITmfTrace trace, JpfAnalysisModule module) {
        super(trace);
        fModule = module;
        // System.out.println("JpfThreadStatusDataProvider::constructor");
    }

    @Override
    public @NonNull TmfModelResponse<@NonNull TmfTreeModel<@NonNull TimeGraphEntryModel>> fetchTree(@NonNull Map<@NonNull String, @NonNull Object> fetchParameters, @Nullable IProgressMonitor monitor) {
        if (fLastEnd == Long.MAX_VALUE) {
            return new TmfModelResponse<>(new TmfTreeModel<>(Collections.emptyList(), filter(Objects.requireNonNull(fTraceEntry), fTidToEntry, fetchParameters)), ITmfResponse.Status.COMPLETED, CommonStatusMessage.COMPLETED);
        }

        fModule.waitForInitialization();
        ITmfStateSystem ss = fModule.getStateSystem();
        if (ss == null) {
            return new TmfModelResponse<>(null, ITmfResponse.Status.FAILED, CommonStatusMessage.ANALYSIS_INITIALIZATION_FAILED);
        }
        synchronized (fBuildMap) {
            boolean complete = ss.waitUntilBuilt(0);
            @NonNull List<@NonNull TimeGraphEntryModel> list = Collections.emptyList();

            if (ss.getNbAttributes() > 0 && ss.getStartTime() != Long.MIN_VALUE) {
                long end = ss.getCurrentEndTime();
                fLastEnd = Long.max(fLastEnd, ss.getStartTime());

                TreeMultimap<Integer, ITmfStateInterval> threadData = TreeMultimap.create(Comparator.naturalOrder(),
                        Comparator.comparing(ITmfStateInterval::getStartTime));


                List<Integer> quarks = new ArrayList<>(ss.getQuarks(Attributes.THREADS, WILDCARD, Attributes.EXEC_NAME));
                quarks.addAll(ss.getQuarks(Attributes.THREADS, WILDCARD, Attributes.PPID));
                quarks.addAll(ss.getQuarks(Attributes.THREADS, WILDCARD, Attributes.PID));
                try {
                    for (ITmfStateInterval interval : ss.query2D(quarks, Long.min(fLastEnd, end), end)) {
                        if (monitor != null && monitor.isCanceled()) {
                            return new TmfModelResponse<>(null, ITmfResponse.Status.CANCELLED, CommonStatusMessage.TASK_CANCELLED);
                        }
                        threadData.put(interval.getAttribute(), interval);
                    }
                } catch (TimeRangeException | StateSystemDisposedException e) {
                    return new TmfModelResponse<>(null, ITmfResponse.Status.FAILED, e.getClass().getName() + ':' + String.valueOf(e.getMessage()));
                }

                // update the trace Entry.
                TimeGraphEntryModel traceEntry = new TimeGraphEntryModel(fTraceId, -1, getTrace().getName(), ss.getStartTime(), end);
                fTraceEntry = traceEntry;

                for (Integer threadQuark : ss.getQuarks(Attributes.THREADS, WILDCARD)) {
                    String threadAttributeName = ss.getAttributeName(threadQuark);
                    Pair<Integer, Integer> entryKey = Attributes.parseThreadAttributeName(threadAttributeName);
                    int threadId = entryKey.getFirst();
                    if (threadId < 0) {
                        // ignore the 'unknown' (-1) thread
                        continue;
                    }

                    int execNameQuark = ss.optQuarkRelative(threadQuark, Attributes.EXEC_NAME);
                    int ppidQuark = ss.optQuarkRelative(threadQuark, Attributes.PPID);
                    int pidQuark = ss.optQuarkRelative(threadQuark, Attributes.PID);
                    NavigableSet<ITmfStateInterval> ppidIntervals = threadData.get(ppidQuark);
                    NavigableSet<ITmfStateInterval> pidIntervals = threadData.get(pidQuark);
                    for (ITmfStateInterval execNameInterval : threadData.get(execNameQuark)) {
                        if (monitor != null && monitor.isCanceled()) {
                            return new TmfModelResponse<>(null, ITmfResponse.Status.CANCELLED, CommonStatusMessage.TASK_CANCELLED);
                        }
                        updateEntry(threadQuark, entryKey, ppidIntervals, execNameInterval, pidIntervals);
                    }
                }

                fLastEnd = end;

                list = filter(traceEntry, fTidToEntry, fetchParameters);
            }

            for (TimeGraphEntryModel model : list) {
                fEntryMetadata.put(model.getId(), model.getMetadata());
            }

            if (complete) {
                fBuildMap.clear();
                fLastEnd = Long.MAX_VALUE;
                return new TmfModelResponse<>(new TmfTreeModel<>(Collections.emptyList(), list), ITmfResponse.Status.COMPLETED, CommonStatusMessage.COMPLETED);
            }

            return new TmfModelResponse<>(new TmfTreeModel<>(Collections.emptyList(), list), ITmfResponse.Status.RUNNING, CommonStatusMessage.RUNNING);
        }
    }

    private void updateEntry(Integer threadQuark, Pair<Integer, Integer> entryKey,
            NavigableSet<ITmfStateInterval> ppidIntervals, ITmfStateInterval execNameInterval,
            NavigableSet<ITmfStateInterval> pidIntervals) {
        Object value = execNameInterval.getValue();
        if (value == null) {
            fBuildMap.remove(entryKey);
            return;
        }

        ThreadEntryModel.Builder entry = fBuildMap.get(entryKey);
        long startTime = execNameInterval.getStartTime();
        long endTime = execNameInterval.getEndTime() + 1;
        String execName = String.valueOf(value);
        int threadId = entryKey.getFirst();
        int ppid = getIntegerFromSet(ppidIntervals, endTime);
        int pid = getIntegerFromSet(pidIntervals, endTime);

        if (entry == null) {
            long id = fAtomicLong.getAndIncrement();
            entry = new ThreadEntryModel.Builder(id, Collections.singletonList(execName), startTime, endTime, threadId, ppid, pid);
            fQuarkMap.put(id, threadQuark);
        } else {

            entry.setEndTime(endTime);
            entry.setPpid(ppid);
            entry.setName(Collections.singletonList(execName));
        }
        fBuildMap.put(entryKey, entry);
        fTidToEntry.put(threadId, entry);
    }

    private static int getIntegerFromSet(NavigableSet<ITmfStateInterval> intervalIterator, long t) {
        ITmfStateInterval interval = intervalIterator.lower(new TmfStateInterval(t, t + 1, 0, 0));
        if (interval != null) {
            Object o = interval.getValue();
            if (o instanceof Integer) {
                return (Integer) o;
            }
        }
        return -1;
    }

    private @NonNull List<@NonNull TimeGraphEntryModel> filter(TimeGraphEntryModel traceEntry, TreeMultimap<Integer, ThreadEntryModel.Builder> tidToEntry, @NonNull Map<@NonNull String, @NonNull Object> parameters) {
        // avoid putting everything as a child of the swapper thread.
        Boolean isActiveFilter = DataProviderParameterUtils.extractBoolean(parameters, ACTIVE_THREAD_FILTER_KEY);
        if (!Boolean.TRUE.equals(isActiveFilter)) {
            ImmutableList.Builder<TimeGraphEntryModel> builder = ImmutableList.builder();
            builder.add(traceEntry);
            for (ThreadEntryModel.Builder entryBuilder : tidToEntry.values()) {
                builder.add(build(entryBuilder));
            }
            return builder.build();
        }
        ITmfStateSystem ss = fModule.getStateSystem();
        if (ss == null) {
            return Collections.emptyList();
        }

        List<@NonNull Long> filter = DataProviderParameterUtils.extractTimeRequested(parameters);
        if (filter == null || filter.isEmpty()) {
            return Collections.emptyList();
        }

        long start = Long.max(filter.get(0), ss.getStartTime());
        long end = Long.min(filter.get(filter.size() - 1), ss.getCurrentEndTime());
        if (start > end) {
            return Collections.emptyList();
        }
        List<@NonNull Long> selectedItems = DataProviderParameterUtils.extractSelectedItems(parameters);
        if (selectedItems != null) {
            Set<Long> cpus = Sets.newHashSet(selectedItems);
            List<@NonNull Integer> quarks = ss.getQuarks(Attributes.THREADS, WILDCARD, Attributes.CURRENT_CPU_RQ);
            Set<TimeGraphEntryModel> models = new HashSet<>();
            models.add(traceEntry);
            Map<Integer, Integer> rqToPidCache = new HashMap<>();
            try {
                for (ITmfStateInterval interval : ss.query2D(quarks, Long.max(ss.getStartTime(), start), end)) {
                    Object o = interval.getValue();
                    if (o instanceof Number && cpus.contains(((Number) o).longValue())) {
                        int attribute = interval.getAttribute();

                        try {
                            // Get the name of the thread
                            int nameQuark = ss.getQuarkRelative(ss.getParentAttributeQuark(attribute), Attributes.EXEC_NAME);
                            Iterable<@NonNull ITmfStateInterval> names2d = ss.query2D(Collections.singleton(nameQuark), interval.getStartTime(), interval.getEndTime());
                            Iterable<@NonNull String> names = Iterables.transform(names2d, intervalName -> String.valueOf(intervalName.getValue()));

                            int tid = rqToPidCache.computeIfAbsent(attribute, a -> Attributes.parseThreadAttributeName(ss.getAttributeName(ss.getParentAttributeQuark(a))).getFirst());
                            //Skip Idle (thread 0)
                            if (tid == 0) {
                                continue;
                            }
                            for (ThreadEntryModel.Builder model : tidToEntry.get(tid)) {
                                if (interval.getStartTime() <= model.getEndTime() &&
                                        model.getStartTime() <= interval.getEndTime()) {
                                    ThreadEntryModel build = build(model);
                                    if (!Iterables.any(names, name -> name.equals(build.getName()))) {
                                        continue;
                                    }
                                    models.add(build);
                                }
                            }
                        } catch (AttributeNotFoundException e) {
                            Activator.getDefault().logWarning("Unable to get the quark for the attribute name", e); //$NON-NLS-1$
                        }
                    }
                }
            } catch (IndexOutOfBoundsException | TimeRangeException e) {
                Activator.getDefault().logError("Invalid query parameters", e); //$NON-NLS-1$
            } catch (StateSystemDisposedException e) {
                return Collections.emptyList();
            }
            return Lists.newArrayList(models);
        }
        ImmutableList.Builder<TimeGraphEntryModel> builder = ImmutableList.builder();
        builder.add(traceEntry);
        for (ThreadEntryModel.Builder thread : tidToEntry.values()) {
            Integer statusQuark = fQuarkMap.get(thread.getId());
            if (statusQuark == null) {
                continue;
            }
            QuarkIterator iterator = new QuarkIterator(ss, statusQuark, start, end);
            Iterator<Object> values = Iterators.transform(iterator, ITmfStateInterval::getValue);
            if (Iterators.any(values, ACTIVE_STATES::contains)) {
                builder.add(build(thread));
            }
        }
        return builder.build();
    }

    private ThreadEntryModel build(ThreadEntryModel.Builder entryBuilder) {
        if (entryBuilder.getId() == fTraceId) {
            return entryBuilder.build(-1);
        }
        long parentId = entryBuilder.getPpid() > 0 ? findEntry(entryBuilder.getPpid(), entryBuilder.getEndTime()) : fTraceId;
        return entryBuilder.build(parentId);
    }

    @Override
    public @NonNull TmfModelResponse<@NonNull TimeGraphModel> fetchRowModel(@NonNull Map<@NonNull String, @NonNull Object> fetchParameters, IProgressMonitor monitor) {
        ITmfStateSystem ss = fModule.getStateSystem();
        if (ss == null) {
            return new TmfModelResponse<>(null, ITmfResponse.Status.FAILED, CommonStatusMessage.ANALYSIS_INITIALIZATION_FAILED);
        }

        TreeMultimap<Integer, ITmfStateInterval> intervals = TreeMultimap.create(Comparator.naturalOrder(),
                Comparator.comparing(ITmfStateInterval::getStartTime));
        SelectionTimeQueryFilter filter = FetchParametersUtils.createSelectionTimeQuery(fetchParameters);
        Map<Long, Integer> selectedIdsToQuarks = getSelectedIdsToQuarks(filter);
        Collection<Integer> stateAndSyscallQuarks = addSyscall(selectedIdsToQuarks.values(), ss);
        Collection<Long> times = getTimes(ss, filter);
        try {
            /* Do the actual query */
            for (ITmfStateInterval interval : ss.query2D(stateAndSyscallQuarks, times)) {
                if (monitor != null && monitor.isCanceled()) {
                    return new TmfModelResponse<>(null, ITmfResponse.Status.CANCELLED, CommonStatusMessage.TASK_CANCELLED);
                }
                intervals.put(interval.getAttribute(), interval);
            }
        } catch (TimeRangeException | StateSystemDisposedException e) {
            return new TmfModelResponse<>(null, ITmfResponse.Status.FAILED, String.valueOf(e.getMessage()));
        }

        Map<@NonNull Integer, @NonNull Predicate< @NonNull Multimap<@NonNull String, @NonNull Object>>> predicates = new HashMap<>();
        Multimap<@NonNull Integer, @NonNull String> regexesMap = DataProviderParameterUtils.extractRegexFilter(fetchParameters);
        if (regexesMap != null) {
            predicates.putAll(computeRegexPredicate(regexesMap));
        }

        @NonNull List<@NonNull ITimeGraphRowModel> rows = new ArrayList<>();
        for (Entry<Long, Integer> entry : selectedIdsToQuarks.entrySet()) {
            int quark = entry.getValue();
            NavigableSet<ITmfStateInterval> states = intervals.get(quark);
            NavigableSet<ITmfStateInterval> syscalls = intervals.get(ss.optQuarkRelative(quark, Attributes.SYSTEM_CALL));

            if (monitor != null && monitor.isCanceled()) {
                return new TmfModelResponse<>(null, ITmfResponse.Status.CANCELLED, CommonStatusMessage.TASK_CANCELLED);
            }
            List<ITimeGraphState> eventList = new ArrayList<>();
            states.forEach(i -> {
                ITimeGraphState timegraphState = createTimeGraphState(i, syscalls);
                Long key = Objects.requireNonNull(entry.getKey());
                applyFilterAndAddState(eventList, timegraphState, key, predicates, monitor);
            });
            rows.add(new TimeGraphRowModel(entry.getKey(), eventList));
        }
        return new TmfModelResponse<>(new TimeGraphModel(rows), ITmfResponse.Status.COMPLETED, CommonStatusMessage.COMPLETED);
    }

    private Map<Long, Integer> getSelectedIdsToQuarks(SelectionTimeQueryFilter filter) {
        Map<Long, Integer> map = new LinkedHashMap<>();
        for (Long id : filter.getSelectedItems()) {
            Integer quark = fQuarkMap.get(id);
            if (quark != null) {
                map.put(id, quark);
            }
        }
        return map;
    }

    private static Collection<Integer> addSyscall(Collection<Integer> quarks, ITmfStateSystem ss) {
        Collection<Integer> copy = new HashSet<>(quarks);
        for (Integer quark : quarks) {
            int syscallQuark = ss.optQuarkRelative(quark, Attributes.SYSTEM_CALL);
            if (syscallQuark != ITmfStateSystem.INVALID_ATTRIBUTE) {
                copy.add(syscallQuark);
            }
        }
        return copy;
    }

    private static @NonNull Collection<@NonNull Long> getTimes(ITmfStateSystem ss, TimeQueryFilter filter) {
        long start = ss.getStartTime();
        // use a HashSet to deduplicate time stamps
        Collection<@NonNull Long> times = new HashSet<>();
        for (long t : filter.getTimesRequested()) {
            if (t >= start) {
                times.add(t);
            }
        }
        return times;
    }

    private static @NonNull Collection<@NonNull Long> getAllTimes(ITmfStateSystem ss) {
        Long start = ss.getStartTime();
        Long end = ss.getCurrentEndTime();

        Long step = JpfTraceEventTime.getTimeStep();

        // System.out.println("Start: " + String.valueOf(start));
        // System.out.println("End: " + String.valueOf(end));

        Collection<@NonNull Long> times = new HashSet<>();
        for (Long t = start; t < end; t += step ) {
            times.add(t);
        }
        return times;
    }

    private static @NonNull ITimeGraphState createTimeGraphState(ITmfStateInterval interval, NavigableSet<ITmfStateInterval> syscalls) {
        long startTime = interval.getStartTime();
        long duration = interval.getEndTime() - startTime + 1;
        Object status = interval.getValue();
        if (status instanceof Integer) {
            int s = (int) status;
            if (s == StateValues.PROCESS_STATUS_RUN_SYSCALL) {
                // intervals are sorted by start time
                ITmfStateInterval syscall = syscalls.floor(new TmfStateInterval(startTime, startTime + 1, 0, 0));

                if (syscall != null) {
                    Object value = syscall.getValue();
                    if (value instanceof String) {
                        return new TimeGraphState(startTime, duration, String.valueOf(value), getElementStyle(s));
                    }
                }
            }
            return new TimeGraphState(startTime, duration, null, getElementStyle(s));
        }
        return new TimeGraphState(startTime, duration, Integer.MIN_VALUE);
    }

    private static @NonNull OutputElementStyle getElementStyle(int stateValue) {
        String styleFor = getStyleFor(stateValue);
        OutputElementStyle style = STYLE_MAP.get(styleFor) ;
        if (style != null) {
            return style;
        }
        return new OutputElementStyle("");
        // return STYLE_MAP.computeIfAbsent(styleFor, style -> new OutputElementStyle(style));
    }

    private static @NonNull String getStyleFor(int stateValue) {
        switch (stateValue) {
        case StateValues.PROCESS_STATUS_UNKNOWN:
            return LinuxStyle.UNKNOWN.getLabel();
        case StateValues.PROCESS_STATUS_RUN_USERMODE:
            return LinuxStyle.USERMODE.getLabel();
        case StateValues.PROCESS_STATUS_RUN_SYSCALL:
            return LinuxStyle.SYSCALL.getLabel();
        case StateValues.PROCESS_STATUS_INTERRUPTED:
            return LinuxStyle.INTERRUPTED.getLabel();
        case StateValues.PROCESS_STATUS_WAIT_BLOCKED:
            return LinuxStyle.WAIT_BLOCKED.getLabel();
        case StateValues.PROCESS_STATUS_WAIT_FOR_CPU:
            return LinuxStyle.WAIT_FOR_CPU.getLabel();
        case StateValues.PROCESS_STATUS_WAIT_UNKNOWN:
            return LinuxStyle.WAIT_UNKNOWN.getLabel();
        case LINK_VALUE:
            return LinuxStyle.LINK.getLabel();
        case SYNC_VALUE:
            return JpfThreadStyle.SYNC.getLabel();
        case METHOD_CALL_VALUE:
            return JpfThreadStyle.METHOD_CALL.getLabel();
        case LOCK_UNLOCK_VALUE:
            return JpfThreadStyle.LOCK_UNLOCK.getLabel();
        case FIELD_ACCESS_VALUE:
            return JpfThreadStyle.FIELD_ACCESS.getLabel();
        default:
            return LinuxStyle.UNKNOWN.getLabel();
        }
    }

    @Override
    public @NonNull TmfModelResponse<@NonNull List<@NonNull ITimeGraphArrow>> fetchArrows(@NonNull Map<@NonNull String, @NonNull Object> fetchParameters, IProgressMonitor monitor) {
        ITmfStateSystem ss = fModule.getStateSystem();
        if (ss == null) {
            return new TmfModelResponse<>(null, ITmfResponse.Status.FAILED, CommonStatusMessage.ANALYSIS_INITIALIZATION_FAILED);
        }
        List<@NonNull ITimeGraphArrow> linkList = new ArrayList<>();
        /**
         * MultiMap of the current thread intervals, grouped by CPU, by increasing start
         * time.
         */
        TreeMultimap<Integer, ITmfStateInterval> currentThreadIntervalsMap = TreeMultimap.create(
                Comparator.naturalOrder(),
                Comparator.comparing(ITmfStateInterval::getStartTime));
        List<Integer> quarks = ss.getQuarks(Attributes.CPUS, WILDCARD, Attributes.CURRENT_THREAD);
        TimeQueryFilter filter = FetchParametersUtils.createTimeQuery(fetchParameters);
        Collection<Long> times = getTimes(ss, filter);
        try {
            /* Do the actual query */
            for (ITmfStateInterval interval : ss.query2D(quarks, times)) {
                if (monitor != null && monitor.isCanceled()) {
                    return new TmfModelResponse<>(null, ITmfResponse.Status.CANCELLED, CommonStatusMessage.TASK_CANCELLED);
                }
                currentThreadIntervalsMap.put(interval.getAttribute(), interval);
            }

            /* Get the arrows. */
            for (Collection<ITmfStateInterval> currentThreadIntervals : currentThreadIntervalsMap.asMap().values()) {
                if (monitor != null && monitor.isCanceled()) {
                    return new TmfModelResponse<>(null, ITmfResponse.Status.CANCELLED, CommonStatusMessage.TASK_CANCELLED);
                }
                linkList.addAll(createCpuArrows(ss, (NavigableSet<ITmfStateInterval>) currentThreadIntervals));
            }
        } catch (TimeRangeException | StateSystemDisposedException e) {
            return new TmfModelResponse<>(null, ITmfResponse.Status.FAILED, String.valueOf(e.getMessage()));
        }
        return new TmfModelResponse<>(linkList, ITmfResponse.Status.COMPLETED, CommonStatusMessage.COMPLETED);
    }

    private List<@NonNull TimeGraphArrow> createCpuArrows(ITmfStateSystem ss, NavigableSet<ITmfStateInterval> intervals)
            throws StateSystemDisposedException {
        if (intervals.isEmpty()) {
            return Collections.emptyList();
        }

        ITmfStateInterval first = intervals.first();
        long start = first.getStartTime() - 1;
        if (start >= ss.getStartTime() && Objects.equals(first.getValue(), 0)) {
            intervals.add(ss.querySingleState(start, first.getAttribute()));
        }
        ITmfStateInterval last = intervals.last();
        long end = last.getEndTime() + 1;
        if (end <= ss.getCurrentEndTime() && Objects.equals(last.getValue(), 0)) {
            intervals.add(ss.querySingleState(end, last.getAttribute()));
        }

        List<@NonNull TimeGraphArrow> linkList = new ArrayList<>();
        long prevEnd = 0;
        long lastEnd = 0;
        long prevEntry = -1;
        for (ITmfStateInterval currentThreadInterval : intervals) {
            long time = currentThreadInterval.getStartTime();
            if (time != lastEnd) {
                /*
                 * Don't create links where there are gaps in intervals due to the resolution
                 */
                prevEntry = -1;
                prevEnd = 0;
            }
            Integer tid = (Integer) currentThreadInterval.getValue();
            lastEnd = currentThreadInterval.getEndTime() + 1;
            long nextEntry = -1;
            if (tid != null && tid > 0) {
                nextEntry = findEntry(tid, time);
                if (prevEntry >= 0 && nextEntry >= 0) {
                    // System.out.println("prevE: " + String.valueOf(prevEntry) + " nextE: " + String.valueOf(nextEntry));
                    TimeGraphArrow arrow = new TimeGraphArrow(prevEntry, nextEntry, prevEnd, time - prevEnd, getElementStyle(LINK_VALUE));
                    linkList.add(arrow);
                }
                prevEntry = nextEntry;
                prevEnd = lastEnd;
            }
        }
        return linkList;
    }

    private long findEntry(int tid, long time) {

        ThreadEntryModel.Builder entry = Iterables.find(fTidToEntry.get(tid),
                cfe -> cfe.getStartTime() <= time && time <= cfe.getEndTime(), null);
        return entry != null ? entry.getId() : fTraceId;
    }

    @Override
    public @NonNull String getId() {
        return ID;
    }

    @Override
    public @NonNull TmfModelResponse<@NonNull Map<@NonNull String, @NonNull String>> fetchTooltip(@NonNull Map<@NonNull String, @NonNull Object> fetchParameters, @Nullable IProgressMonitor monitor) {
        ITmfStateSystem ss = fModule.getStateSystem();
        if (ss == null) {
            return new TmfModelResponse<>(null, ITmfResponse.Status.FAILED, CommonStatusMessage.STATE_SYSTEM_FAILED);
        }

        boolean completed = ss.waitUntilBuilt(0);
        ITmfResponse.Status status = completed ? ITmfResponse.Status.COMPLETED : ITmfResponse.Status.RUNNING;
        String statusMessage = completed ? CommonStatusMessage.COMPLETED : CommonStatusMessage.RUNNING;

        SelectionTimeQueryFilter filter = FetchParametersUtils.createSelectionTimeQuery(fetchParameters);
        if (filter == null) {
            return new TmfModelResponse<>(null, ITmfResponse.Status.FAILED, CommonStatusMessage.INCORRECT_QUERY_PARAMETERS);
        }
        Integer quark = fQuarkMap.get(filter.getSelectedItems().iterator().next());
        if (quark == null) {
            return new TmfModelResponse<>(null, status, statusMessage);
        }
        long start = filter.getStart();
        try {
            List<@NonNull ITmfStateInterval> states = ss.queryFullState(start);
            int currentCpuRqQuark = ss.optQuarkRelative(quark, Attributes.CURRENT_CPU_RQ);
            if (currentCpuRqQuark == ITmfStateSystem.INVALID_ATTRIBUTE || start < ss.getStartTime() || start > ss.getCurrentEndTime()) {
                return new TmfModelResponse<>(null, status, statusMessage);
            }
            ITmfCallsiteResolver csAnalysis = TmfTraceUtils.getAnalysisModuleOfClass(getTrace(), CallsiteAnalysis.class, CallsiteAnalysis.ID);
            Object value = states.get(currentCpuRqQuark).getValue();

            if (value instanceof Integer) {
                String cpuId = String.valueOf(value);
                Map<String, String> returnValue = new LinkedHashMap<>();
                returnValue.put(TmfStrings.cpu(), cpuId);
                if (csAnalysis != null) {
                    Object cpuThreadObj = states.get(quark).getValue();
                    if (cpuThreadObj instanceof Integer && Objects.equals(ProcessStatus.RUN_SYTEMCALL.getStateValue().unboxInt(), cpuThreadObj)) {
                        ITmfTrace trace = getTrace();
                        for (ITmfEventAspect<?> aspect : trace.getEventAspects()) {
                            if (aspect instanceof TmfCpuAspect) {
                                TmfCpuAspect deviceAspect = (TmfCpuAspect) aspect;
                                List<@NonNull ITmfCallsite> callsites = csAnalysis.getCallsites(String.valueOf(trace.getUUID()), deviceAspect.getDeviceType(), cpuId, start);
                                if (!callsites.isEmpty()) {
                                    returnValue.put(TmfStrings.source(), callsites.get(0).toString());
                                }
                            }
                        }
                    }
                }
                return new TmfModelResponse<>(returnValue, status, statusMessage);
            }
        } catch (StateSystemDisposedException e) {
            /* Ignored */
        }
        return new TmfModelResponse<>(null, status, statusMessage);
    }

    @Override
    public @NonNull Multimap<@NonNull String, @NonNull Object> getFilterData(long entryId, long time, @Nullable IProgressMonitor monitor) {
        Multimap<@NonNull String, @NonNull Object> data = ITimeGraphStateFilter.mergeMultimaps(ITimeGraphDataProvider.super.getFilterData(entryId, time, monitor),
                fEntryMetadata.getOrDefault(entryId, ImmutableMultimap.of()));
        SelectionTimeQueryFilter filter = new SelectionTimeQueryFilter(Collections.singletonList(time), Collections.singleton(Objects.requireNonNull(entryId)));
        Map<@NonNull String, @NonNull Object> parameters = FetchParametersUtils.selectionTimeQueryToMap(filter);
        TmfModelResponse<Map<String, String>> response = fetchTooltip(parameters, monitor);
        Map<@NonNull String, @NonNull String> model = response.getModel();
        if (model != null) {
            for (Entry<String, String> entry : model.entrySet()) {
                data.put(entry.getKey(), entry.getValue());
            }
        }
        return data;
    }

    @Override
    public TmfModelResponse<OutputStyleModel> fetchStyle(Map<String, Object> fetchParameters, @Nullable IProgressMonitor monitor) {
        return new TmfModelResponse<>(new OutputStyleModel(STYLE_MAP), ITmfResponse.Status.COMPLETED, CommonStatusMessage.COMPLETED);
    }

    // ANNOTATION_CAT_LIST: List<String>
    @Override
    public TmfModelResponse<AnnotationCategoriesModel> fetchAnnotationCategories(Map<String, Object> fetchParameters, @Nullable IProgressMonitor monitor) {
        // System.out.println("JpfThreadStatusDataProvider::fetchAnnotationCategories");
        // System.out.println("Category size: " + String.valueOf(ANNOTATION_CAT_LIST.size()));
        return new TmfModelResponse<>(new AnnotationCategoriesModel(ANNOTATION_CAT_LIST), ITmfResponse.Status.COMPLETED, CommonStatusMessage.COMPLETED);
    }

    // ANNOTATION_MAP: Map<String, Collection<Annotation>>
    @Override
    public TmfModelResponse<AnnotationModel> fetchAnnotations(Map<String, Object> fetchParameters, @Nullable IProgressMonitor monitor) {
        // System.out.println("JpfThreadStatusDataProvider::fetchAnnotations");
        synchronized (fBuildMap) {

            ITmfStateSystem ss = fModule.getStateSystem();
            if (ss == null) {
                return new TmfModelResponse<>(null, ITmfResponse.Status.FAILED, CommonStatusMessage.ANALYSIS_INITIALIZATION_FAILED);
            }

            List<Integer> tidQuarks = ss.getQuarks(Attributes.CPUS, WILDCARD, Attributes.CURRENT_THREAD);
            Integer tidQuark = -1;
            if (tidQuarks.size() > 0){
                tidQuark = tidQuarks.get(0);
            } else {
                return new TmfModelResponse<>(null, ITmfResponse.Status.FAILED, CommonStatusMessage.STATE_SYSTEM_FAILED);
            }

            Collection<Long> times = getAllTimes(ss);

            Map<String, Collection<Annotation>> ANNOTATION_MAP = new HashMap<>();
            Collection<Annotation> syncCollection = new ArrayList<>();
            Collection<Annotation> methodCallCollection = new ArrayList<>();
            Collection<Annotation> lockUnlockCollection = new ArrayList<>();
            Collection<Annotation> fieldAccessCollection = new ArrayList<>();

            for (Long t : times) {

                Integer tid = -1;
                try {
                    ITmfStateInterval currentThreadInterval = ss.querySingleState(t, tidQuark);
                    tid = (Integer)currentThreadInterval.getValue();
                    // System.out.println("Tid Value: " + String.valueOf(ss.querySingleState(t, tidQuark).getValue()));
                    if (tid == null) {
                        continue;
                    }
                } catch(TimeRangeException | StateSystemDisposedException e) {
                    return new TmfModelResponse<>(null, ITmfResponse.Status.FAILED, String.valueOf(e.getMessage()));
                }

                // Long formerT = t - JpfTraceField.sDuration;

                // Integer formerTid = -1;
                // try {
                //     if (formerT < ss.getStartTime()) {
                //         formerTid = -1;
                //         continue;
                //     }

                //     ITmfStateInterval formerThreadInterval = ss.querySingleState(formerT, tidQuark);
                //     formerTid = (Integer) formerThreadInterval.getValue();
                //     if (formerTid == null) {
                //         formerTid = -1;
                //     }
                // } catch(TimeRangeException | StateSystemDisposedException e) {
                //     return new TmfModelResponse<>(null, ITmfResponse.Status.FAILED, String.valueOf(e.getMessage()));
                // }

                int cpu = 0;
                List<Integer> specQuarks = ss.getQuarks(Attributes.THREADS, Attributes.buildThreadAttributeName(tid, cpu), Attributes.SPEC);

                for (Integer quark : specQuarks) {

                    try {

                        if (monitor != null && monitor.isCanceled()) {
                            System.out.println("Task cancelled");
                            return new TmfModelResponse<>(null, ITmfResponse.Status.CANCELLED, CommonStatusMessage.TASK_CANCELLED);
                        }

                        ITmfStateInterval specInterval = ss.querySingleState(t, quark);
                        // ITmfStateInterval formerSpecInterval = ss.querySingleState(formerT, quark);
                        String content = (String) specInterval.getValue();
                        // String formerContent = (String) formerSpecInterval.getValue();

                        if (content != null) {
                            if (content.contains("Sync")) {
                                // if ((!formerTid.equals(tid)) || ((formerContent != null) && (!formerContent.contains("isSync")))) {
                                    Annotation anno = new Annotation(t, 0, findEntry(tid, t), JpfThreadStyle.SYNC.getLabel(), getElementStyle(SYNC_VALUE));
                                    syncCollection.add(anno);
                                // }
                            }

                            if (content.contains("MethodCall")) {
                                // if ((!formerTid.equals(tid)) || ((formerContent != null) && (!formerContent.contains("isMethodCall")))) {
                                    Annotation anno = new Annotation(t, 0, findEntry(tid, t), JpfThreadStyle.METHOD_CALL.getLabel(), getElementStyle(METHOD_CALL_VALUE));
                                    methodCallCollection.add(anno);
                                // }
                            }

                            if (content.contains("Lock/Unlock")) {
                                // if ((!formerTid.equals(tid)) || ((formerContent != null) && (!formerContent.contains("isThreadRelatedMethod")))) {
                                    Annotation anno = new Annotation(t, 0, findEntry(tid, t), JpfThreadStyle.LOCK_UNLOCK.getLabel(), getElementStyle(LOCK_UNLOCK_VALUE));
                                    lockUnlockCollection.add(anno);
                                // }
                            }

                            if (content.contains("FieldAccess")) {
                                // if ((!formerTid.equals(tid)) || ((formerContent != null) && (!formerContent.contains("isFieldAccess")))) {
                                    Annotation anno = new Annotation(t, 0, findEntry(tid, t), JpfThreadStyle.FIELD_ACCESS.getLabel(), getElementStyle(FIELD_ACCESS_VALUE));
                                    fieldAccessCollection.add(anno);
                                // }

                            }
                        }
                    } catch (TimeRangeException | StateSystemDisposedException e) {
                        // System.out.println("Exception");
                        e.printStackTrace();
                        return new TmfModelResponse<>(null, ITmfResponse.Status.FAILED, String.valueOf(e.getMessage()));
                    }
                }
            }

            ANNOTATION_MAP.put(JpfThreadStyle.SYNC.getLabel(), syncCollection);
            ANNOTATION_MAP.put(JpfThreadStyle.METHOD_CALL.getLabel(), methodCallCollection);
            ANNOTATION_MAP.put(JpfThreadStyle.LOCK_UNLOCK.getLabel(), lockUnlockCollection);
            ANNOTATION_MAP.put(JpfThreadStyle.FIELD_ACCESS.getLabel(), fieldAccessCollection);
            return new TmfModelResponse<>(new AnnotationModel(ANNOTATION_MAP), ITmfResponse.Status.COMPLETED, CommonStatusMessage.COMPLETED);
        }
    }

}
