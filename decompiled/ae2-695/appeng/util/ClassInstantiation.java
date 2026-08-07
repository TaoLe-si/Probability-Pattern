/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.base.Optional
 */
package appeng.util;

import appeng.core.AELog;
import com.google.common.base.Optional;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

public class ClassInstantiation<T> {
    private final Class<? extends T> template;
    private final Object[] args;

    public ClassInstantiation(Class<? extends T> template, Object ... args) {
        this.template = template;
        this.args = args;
    }

    public Optional<T> get() {
        Constructor<?>[] constructors;
        for (Constructor<?> constructor : constructors = this.template.getConstructors()) {
            Class<?>[] paramTypes = constructor.getParameterTypes();
            if (paramTypes.length != this.args.length) continue;
            boolean valid = true;
            for (int idx = 0; idx < paramTypes.length; ++idx) {
                Class<?> cz = this.args[idx].getClass();
                if (this.isClassMatch(paramTypes[idx], cz, this.args[idx])) continue;
                valid = false;
            }
            if (!valid) continue;
            try {
                return Optional.of(constructor.newInstance(this.args));
            }
            catch (IllegalAccessException | InstantiationException | InvocationTargetException e) {
                e.printStackTrace();
                break;
            }
        }
        return Optional.absent();
    }

    private boolean isClassMatch(Class<?> expected, Class<?> got, Object value) {
        if (value == null && !expected.isPrimitive()) {
            return true;
        }
        expected = this.condense(expected, Boolean.class, Character.class, Byte.class, Short.class, Integer.class, Long.class, Float.class, Double.class);
        return expected == (got = this.condense(got, Boolean.class, Character.class, Byte.class, Short.class, Integer.class, Long.class, Float.class, Double.class)) || expected.isAssignableFrom(got);
    }

    private Class<?> condense(Class<?> expected, Class<?> ... wrappers) {
        if (expected.isPrimitive()) {
            for (Class<?> clz : wrappers) {
                try {
                    if (expected == clz.getField("TYPE").get(null)) {
                        return clz;
                    }
                }
                catch (Throwable t) {
                    AELog.debug(t);
                }
            }
        }
        return expected;
    }
}

