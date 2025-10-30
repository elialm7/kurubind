package com.roelias.kurubind.registry;

import com.roelias.kurubind.core.Dialect;
import com.roelias.kurubind.core.Handler;
import com.roelias.kurubind.metadata.FieldMetadata;

import java.lang.annotation.Annotation;
import java.util.*;

public class HandlerRegistry {
    private final Map<Class<? extends Annotation>, Handler> genericHandlers;
    private final Map<HandlerKey, Handler> dialectHandlers;

    public HandlerRegistry() {
        this.genericHandlers = new HashMap<>();
        this.dialectHandlers = new HashMap<>();
    }

    public void register(Class<? extends Annotation> annotationType, Handler handler) {
        genericHandlers.put(annotationType, handler);
    }

    public void register(Class<? extends Annotation> annotationType, Dialect dialect, Handler handler) {
        dialectHandlers.put(new HandlerKey(annotationType, dialect), handler);
    }

    public Handler getHandler(Class<? extends Annotation> annotationType, Dialect dialect) {
        if (dialect != null) {
            Handler dialectHandler = dialectHandlers.get(new HandlerKey(annotationType, dialect));
            if (dialectHandler != null) {
                return dialectHandler;
            }
        }
        return genericHandlers.get(annotationType);
    }

    public List<Handler> getHandlersForField(FieldMetadata field, Dialect dialect) {
        List<Handler> handlers = new ArrayList<>();
        for (Annotation annotation : field.getAllAnnotations()) {
            Handler handler = getHandler(annotation.annotationType(), dialect);
            if (handler != null) {
                handlers.add(handler);
            }
        }
        return handlers;
    }

    private static class HandlerKey {
        private final Class<? extends Annotation> annotationType;
        private final Dialect dialect;

        HandlerKey(Class<? extends Annotation> annotationType, Dialect dialect) {
            this.annotationType = annotationType;
            this.dialect = dialect;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            HandlerKey that = (HandlerKey) o;
            return annotationType.equals(that.annotationType) && dialect.equals(that.dialect);
        }

        @Override
        public int hashCode() {
            return Objects.hash(annotationType, dialect);
        }
    }
}
