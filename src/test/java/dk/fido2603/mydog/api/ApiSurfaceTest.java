package dk.fido2603.mydog.api;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.RecordComponent;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * The API may only expose Java, Bukkit and its own types. MyDog's internals change between versions, and Adventure is
 * relocated inside the MyDog jar, so other plugins couldn't use either.
 */
class ApiSurfaceTest {
    private static final List<Class<?>> API = List.of(MyDogApi.class, DogInfo.class);

    @Test
    void onlyExposesJavaBukkitAndApiTypes() {
        List<String> problems = new ArrayList<>();
        for (Class<?> type : API) {
            for (Method method : type.getMethods()) {
                if (method.getDeclaringClass() == Object.class || method.getDeclaringClass() == Record.class) {
                    continue;
                }
                check(problems, type.getSimpleName() + "." + method.getName(), method.getGenericReturnType());
                for (Type parameter : method.getGenericParameterTypes()) {
                    check(problems, type.getSimpleName() + "." + method.getName(), parameter);
                }
            }
            if (type.isRecord()) {
                for (RecordComponent component : type.getRecordComponents()) {
                    check(problems, type.getSimpleName() + "." + component.getName(), component.getGenericType());
                }
            }
        }
        assertTrue(problems.isEmpty(), "Types other plugins can't rely on: " + problems);
    }

    private static void check(List<String> problems, String where, Type type) {
        if (type instanceof ParameterizedType parameterized) {
            check(problems, where, parameterized.getRawType());
            for (Type argument : parameterized.getActualTypeArguments()) {
                check(problems, where, argument);
            }
            return;
        }
        if (!(type instanceof Class<?> c) || c.isPrimitive()) {
            return;
        }
        String name = c.getName();
        boolean allowed = name.startsWith("java.") || name.startsWith("org.bukkit.")
                || name.startsWith(MyDogApi.class.getPackageName() + ".");
        if (!allowed) {
            problems.add(where + " uses " + name);
        }
    }
}
