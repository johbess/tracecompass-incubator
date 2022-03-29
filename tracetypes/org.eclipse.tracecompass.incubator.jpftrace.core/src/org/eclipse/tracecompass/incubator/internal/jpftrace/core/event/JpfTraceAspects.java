/**
 *
 */
package org.eclipse.tracecompass.incubator.internal.jpftrace.core.event;

//import java.text.Format;
//import java.util.Collections;
//import java.util.Map;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.tmf.core.event.ITmfEvent;
//import org.eclipse.tracecompass.tmf.core.event.ITmfEventField;
import org.eclipse.tracecompass.tmf.core.event.aspect.ITmfEventAspect;
import org.eclipse.tracecompass.tmf.core.event.aspect.TmfBaseAspects;
import org.eclipse.tracecompass.tmf.core.event.aspect.TmfCpuAspect;

import com.google.common.collect.ImmutableList;

public class JpfTraceAspects {

 private static @Nullable Iterable<@NonNull ITmfEventAspect<?>> aspects;

 public static Iterable<@NonNull ITmfEventAspect<?>> getAspects() {
     Iterable<@NonNull ITmfEventAspect<?>> aspectSet = aspects;
     if (aspectSet == null) {
         aspectSet = ImmutableList.of(
                 new JpfTraceLabelAspect(),
                 new JpfTraceCpuAspect(),
                 TmfBaseAspects.getTimestampAspect(),
                 TmfBaseAspects.getEventTypeAspect(),
                 new JpfTraceInstructionTypeAspect(),
                 new JpfTraceThreadNameAspect(),
                 new JpfTraceThreadStateAspect(),
                 new JpfTraceChoiceAspect(),
                 new JpfTraceSourceAspect(),
                 TmfBaseAspects.getContentsAspect()
                 );
         aspects = aspectSet;
     }
     return aspectSet;
 }

 private static class JpfTraceLabelAspect implements IJpfTraceAspect<String> {

      @Override
      public @NonNull String getName() {
          return String.valueOf(Messages.JpfTraceAspects_Name);
      }

      @Override
      public @NonNull String getHelpText() {
          return String.valueOf(Messages.JpfTraceAspects_NameD);
      }

      @Override
      public String resolveJpfTraceLogs(@NonNull JpfTraceEvent event) {
          return event.getName();
      }
  }

  private static class JpfTraceThreadStateAspect implements IJpfTraceAspect<String> {

      @Override
      public @NonNull String getName() {
          return String.valueOf(Messages.JpfTraceAspects_ThreadState);
      }

      @Override
      public @NonNull String getHelpText() {
          return String.valueOf(Messages.JpfTraceAspects_ThreadStateD);
      }

      @Override
      public String resolveJpfTraceLogs(@NonNull JpfTraceEvent event) {
          return event.getField().getThreadState();
      }
  }

 private static class JpfTraceThreadNameAspect implements IJpfTraceAspect<String> {

     @Override
     public @NonNull String getName() {
         return String.valueOf(Messages.JpfTraceAspects_ThreadName);
     }

     @Override
     public @NonNull String getHelpText() {
         return String.valueOf(Messages.JpfTraceAspects_ThreadNameD);
     }

     @Override
     public String resolveJpfTraceLogs(@NonNull JpfTraceEvent event) {
         return event.getField().getThreadName();
     }
 }

 private static class JpfTraceInstructionTypeAspect implements IJpfTraceAspect<String> {
     @Override
     public @NonNull String getName() {
         return String.valueOf(Messages.JpfTraceAspects_InstructionType);
     }

     @Override
     public @NonNull String getHelpText() {
         return String.valueOf(Messages.JpfTraceAspects_InstructionTypeD);
     }

     @Override
     public String resolveJpfTraceLogs(@NonNull JpfTraceEvent event) {
         return event.getField().getInstructionSpecAndDetail()[0];
     }
 }

 private static class JpfTraceChoiceAspect implements IJpfTraceAspect<String> {

     @Override
     public @NonNull String getName() {
         return String.valueOf(Messages.JpfTraceAspects_Choice);
     }

     @Override
     public @NonNull String getHelpText() {
         return String.valueOf(Messages.JpfTraceAspects_ChoiceD);
     }

     @Override
     public String resolveJpfTraceLogs(@NonNull JpfTraceEvent event) {
         return event.getField().getChoiceId();
     }
 }

 private static class JpfTraceSourceAspect implements IJpfTraceAspect<String> {
     @Override
     public @NonNull String getName() {
         return String.valueOf(Messages.JpfTraceAspects_Source);
     }

     @Override
     public @NonNull String getHelpText() {
         return String.valueOf(Messages.JpfTraceAspects_SourceD);
     }

     @Override
     public String resolveJpfTraceLogs(@NonNull JpfTraceEvent event) {
         return event.getField().getSource();
     }
 }

 // private static final TmfEventFieldAspect SOURCE_ASPECT = new TmfEventFieldAspect(String.valueOf(Messages.JpfTraceAspects_Source), null, event -> event.getField().getSources()) {
 //     @Override
 //     public String getHelpText() {
 //         return String.valueOf(Messages.JpfTraceAspects_SourceD);
 //     }
 // };

 private static class JpfTraceCpuAspect extends TmfCpuAspect {
     @Override
     public @Nullable Integer resolve(ITmfEvent event) {
         if (!(event instanceof JpfTraceEvent)) {
             return null;
         }
         return 0;
     }
 }
}
