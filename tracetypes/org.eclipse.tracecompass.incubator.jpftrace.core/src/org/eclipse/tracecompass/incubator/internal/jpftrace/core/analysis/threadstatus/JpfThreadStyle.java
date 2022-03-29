package org.eclipse.tracecompass.incubator.internal.jpftrace.core.analysis.threadstatus;

import java.util.Map;

import org.eclipse.tracecompass.tmf.core.dataprovider.X11ColorUtils;
import org.eclipse.tracecompass.tmf.core.model.StyleProperties;

import com.google.common.collect.ImmutableMap;

public enum JpfThreadStyle {

    SYNC(String.valueOf(Messages.JpfThreadStyle_sync), 0, 100, 200, 255, 0.5f, StyleProperties.SymbolType.DIAMOND),

    METHOD_CALL(String.valueOf(Messages.JpfThreadStyle_method_call), 200, 40, 100, 255, 0.6f, StyleProperties.SymbolType.INVERTED_TRIANGLE),

    LOCK_UNLOCK(String.valueOf(Messages.JpfThreadStyle_lock_unlock), 255, 200, 100, 255, 0.5f, StyleProperties.SymbolType.CROSS),

    FIELD_ACCESS(String.valueOf(Messages.JpfThreadStyle_field_access), 100, 255, 200, 255, 0.7f, StyleProperties.SymbolType.CIRCLE);

    private final Map<String, Object> fMap;

    private JpfThreadStyle(String label, int red, int green, int blue, int alpha, float heightFactor, String symbolType) {
        if (red > 255 || red < 0) {
            throw new IllegalArgumentException("Red needs to be between 0 and 255"); //$NON-NLS-1$
        }
        if (green > 255 || green < 0) {
            throw new IllegalArgumentException("Green needs to be between 0 and 255"); //$NON-NLS-1$
        }
        if (blue > 255 || blue < 0) {
            throw new IllegalArgumentException("Blue needs to be between 0 and 255"); //$NON-NLS-1$
        }
        if (alpha > 255 || alpha < 0) {
            throw new IllegalArgumentException("alpha needs to be between 0 and 255"); //$NON-NLS-1$
        }
        if (heightFactor > 1.0 || heightFactor < 0) {
            throw new IllegalArgumentException("Height factor needs to be between 0 and 1.0, given hint : " + heightFactor); //$NON-NLS-1$
        }

        ImmutableMap.Builder<String, Object> builder = new ImmutableMap.Builder<>();
        builder.put(StyleProperties.STYLE_NAME, label);
        builder.put(StyleProperties.COLOR, X11ColorUtils.toHexColor(red, green, blue));
        builder.put(StyleProperties.BACKGROUND_COLOR, X11ColorUtils.toHexColor(red, green, blue));
        builder.put(StyleProperties.HEIGHT, heightFactor);
        builder.put(StyleProperties.OPACITY, (float) alpha / 255);
        builder.put(StyleProperties.SYMBOL_TYPE, symbolType);

        fMap = builder.build();
    }

    public String getLabel() {
        return (String) toMap().getOrDefault(StyleProperties.STYLE_NAME, ""); //$NON-NLS-1$
    }

    public Map<String, Object> toMap() {
        return fMap;
    }
}
