package org.eclipse.tracecompass.incubator.internal.jpftrace.core.analysis.threadstatus;

import java.util.Collection;
import java.util.Collections;
import java.util.Objects;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.internal.tmf.core.model.DataProviderDescriptor;
import org.eclipse.tracecompass.tmf.core.dataprovider.IDataProviderDescriptor;
import org.eclipse.tracecompass.tmf.core.dataprovider.IDataProviderDescriptor.ProviderType;
import org.eclipse.tracecompass.tmf.core.dataprovider.IDataProviderFactory;
import org.eclipse.tracecompass.tmf.core.model.tree.ITmfTreeDataModel;
import org.eclipse.tracecompass.tmf.core.model.tree.ITmfTreeDataProvider;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;
import org.eclipse.tracecompass.tmf.core.trace.TmfTraceUtils;

public class JpfThreadStatusDataProviderFactory implements IDataProviderFactory {

    private static final IDataProviderDescriptor DESCRIPTOR = new DataProviderDescriptor.Builder()
            .setId(JpfThreadStatusDataProvider.ID)
            .setName(Objects.requireNonNull(Messages.JpfThreadStatusDataProviderFactory_title))
            .setDescription(Objects.requireNonNull(Messages.JpfThreadStatusDataProviderFactory_descriptionText))
            .setProviderType(ProviderType.TIME_GRAPH)
            .build();

    @Override
    public @Nullable ITmfTreeDataProvider<? extends ITmfTreeDataModel> createProvider(@NonNull ITmfTrace trace) {
        JpfAnalysisModule module = TmfTraceUtils.getAnalysisModuleOfClass(trace, JpfAnalysisModule.class, JpfAnalysisModule.ID);
        // System.out.println("JPFFactory::createProvider: called");
        if (module != null) {
            module.schedule();
            // check this function is called
            // System.out.println("JPFFactory::createProvider: module exists");
            return new JpfThreadStatusDataProvider(trace, module);
        }

        return null;
    }

    @Override
    public Collection<IDataProviderDescriptor> getDescriptors(@NonNull ITmfTrace trace) {
        JpfAnalysisModule module = TmfTraceUtils.getAnalysisModuleOfClass(trace, JpfAnalysisModule.class, JpfAnalysisModule.ID);
        return module != null ? Collections.singletonList(DESCRIPTOR) : Collections.emptyList();
    }

}
