import java.lang.classfile.ClassFile;
import java.lang.classfile.ClassModel;
import java.lang.classfile.constantpool.ClassEntry;
import java.lang.classfile.constantpool.FieldRefEntry;
import java.lang.classfile.constantpool.InterfaceMethodRefEntry;
import java.lang.classfile.constantpool.MemberRefEntry;
import java.lang.classfile.constantpool.PoolEntry;
import java.io.File;
import java.lang.reflect.Executable;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * Checks that the plugin jar links against the oldest supported Bukkit API.
 * <p>
 * The plugin is compiled against the newest Paper API, so compiling against old Spigot (-Pspigot) only proves the
 * source is fine: the jar itself may still reference a field or method the old server doesn't have, or call a type
 * as an interface that used to be an enum (Attribute and Sound before 1.21.3). This resolves every org.bukkit class,
 * field and method the jar uses against the given classpath, without running or initializing anything.
 * <p>
 * Usage: java ci/LinkCheck.java plugin/target/MyDog-x.jar &lt;classpath with the old spigot-api&gt;
 */
public class LinkCheck {
    public static void main(String[] args) throws Exception {
        List<URL> urls = new ArrayList<>();
        for (String path : args[1].split(File.pathSeparator)) {
            urls.add(Path.of(path).toUri().toURL());
        }
        ClassLoader api = new URLClassLoader(urls.toArray(URL[]::new), ClassLoader.getPlatformClassLoader());

        Set<String> problems = new TreeSet<>();
        try (JarFile jar = new JarFile(args[0])) {
            for (JarEntry entry : Collections.list(jar.entries())) {
                String name = entry.getName();
                // Our own classes only; the shaded libraries under libs/ don't use Bukkit
                if (!name.startsWith("dk/fido2603/mydog/") || name.startsWith("dk/fido2603/mydog/libs/")
                        || !name.endsWith(".class")) {
                    continue;
                }
                ClassModel model = ClassFile.of().parse(jar.getInputStream(entry).readAllBytes());
                check(api, model, problems);
            }
        }
        problems.forEach(System.out::println);
        if (!problems.isEmpty()) {
            System.out.println(problems.size() + " reference(s) the oldest supported API doesn't have");
            System.exit(1);
        }
        System.out.println("All Bukkit references resolve");
    }

    private static void check(ClassLoader api, ClassModel model, Set<String> problems) {
        String in = "  (in " + model.thisClass().asInternalName() + ")";
        for (PoolEntry poolEntry : model.constantPool()) {
            if (poolEntry instanceof ClassEntry classEntry) {
                String name = classEntry.asInternalName().replaceAll("^\\[+L?", "").replace(";", "");
                if (isBukkit(name) && load(api, name) == null) {
                    problems.add("missing class " + name + in);
                }
            } else if (poolEntry instanceof MemberRefEntry ref) {
                String owner = ref.owner().asInternalName();
                Class<?> type = isBukkit(owner) ? load(api, owner) : null;
                if (type == null) {
                    continue; // not Bukkit, or already reported as a missing class
                }
                String member = owner + "." + ref.name().stringValue();
                String descriptor = ref.type().stringValue();
                if (ref instanceof FieldRefEntry) {
                    if (!hasField(type, ref.name().stringValue())) {
                        problems.add("missing field " + member + in);
                    }
                } else if (!hasMethod(type, ref.name().stringValue(), descriptor)) {
                    problems.add("missing method " + member + descriptor + in);
                } else if (!ref.name().equalsString("<init>")
                        && (ref instanceof InterfaceMethodRefEntry) != type.isInterface()) {
                    problems.add("called as " + (type.isInterface() ? "a class" : "an interface") + ", but is "
                            + (type.isInterface() ? "an interface" : "a class") + ": " + member + in);
                }
            }
        }
    }

    private static boolean isBukkit(String internalName) {
        return internalName.startsWith("org/bukkit/") || internalName.startsWith("net/md_5/");
    }

    private static Class<?> load(ClassLoader api, String internalName) {
        try {
            return Class.forName(internalName.replace('/', '.'), false, api);
        } catch (ClassNotFoundException | LinkageError e) {
            return null;
        }
    }

    private static boolean hasField(Class<?> type, String name) {
        for (Class<?> candidate : hierarchy(type)) {
            for (Field field : candidate.getDeclaredFields()) {
                if (field.getName().equals(name)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean hasMethod(Class<?> type, String name, String descriptor) {
        boolean constructor = name.equals("<init>");
        for (Class<?> candidate : constructor ? List.<Class<?>>of(type) : hierarchy(type)) {
            Executable[] executables = constructor ? candidate.getDeclaredConstructors() : candidate.getDeclaredMethods();
            for (Executable executable : executables) {
                if ((constructor || executable.getName().equals(name)) && descriptor(executable).equals(descriptor)) {
                    return true;
                }
            }
        }
        return false;
    }

    /** The type, its superclasses and all their interfaces (and Object, which interfaces inherit methods from). */
    private static Set<Class<?>> hierarchy(Class<?> type) {
        Set<Class<?>> all = new LinkedHashSet<>();
        Deque<Class<?>> queue = new ArrayDeque<>(List.of(type, Object.class));
        while (!queue.isEmpty()) {
            Class<?> next = queue.poll();
            if (all.add(next)) {
                if (next.getSuperclass() != null) {
                    queue.add(next.getSuperclass());
                }
                queue.addAll(List.of(next.getInterfaces()));
            }
        }
        return all;
    }

    private static String descriptor(Executable executable) {
        StringBuilder descriptor = new StringBuilder("(");
        for (Class<?> parameter : executable.getParameterTypes()) {
            descriptor.append(parameter.descriptorString());
        }
        descriptor.append(')');
        descriptor.append(executable instanceof Method method ? method.getReturnType().descriptorString() : "V");
        return descriptor.toString();
    }
}
