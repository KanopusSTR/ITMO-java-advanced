package info.kgeorgiy.ja.rynk.implementor;

import info.kgeorgiy.java.advanced.implementor.Impler;
import info.kgeorgiy.java.advanced.implementor.ImplerException;
import info.kgeorgiy.java.advanced.implementor.JarImpler;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import java.io.*;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.jar.Attributes;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;

/**
 * Class implements {@link JarImpler}. Create interfaces implementations.
 *
 * @author Artur Rynk
 * @see Impler#implement(Class, Path)
 * @see JarImpler#implementJar(Class, Path)
 */
public class Implementor implements JarImpler {

    /**
     * Const value equals 4 spaces
     */
    private static final String TAB = "    ";

    /**
     * Const value equals 1 space
     */
    private static final String SPACE = " ";

    /**
     * example of class {@link BufferedWriter} for writing implementation
     */
    private static BufferedWriter writer;

    /**
     * Launches method: implement or implementJar (that you need). Help to process Implement arguments
     *
     * @param args "interface_name" or "-jar interface_name jarFile.jar"
     */
    static public void main(String[] args) {
        if (args.length == 0 || args[0] == null) {
            System.err.println("There must be 1 or 3 arguments");
            return;
        }
        if (args[0].equals("-jar") && (args.length == 1)) {
            System.err.println("you should write class-name after \"-jar\"");
            return;
        }
        try {
            Path path = Path.of(".");
            if (args[0].equals("-jar")) {
                new Implementor().implementJar(Class.forName(args[1]), path);
            } else {
                new Implementor().implement(Class.forName(args[0]), path);
            }
        } catch (ClassNotFoundException e) {
            System.err.println("Class not found");
        } catch (ImplerException e) {
            System.err.println("Implement error");
        }
    }

    /**
     * Implement interface ({@code token}) to class. Create file with {@code token} + "Impl" name (Name -> NameImpl).
     *
     * @param token instances of interface that you want to implement
     * @param root path to root directory
     * @throws ImplerException if you write token that not a public or protected interface,
     * or if you write incorrect path. Also, if {@link IOException} occurs.
     */
    @Override
    public void implement(Class<?> token, Path root) throws ImplerException {

        if (!token.isInterface()) {
            throw new ImplerException("You must write interface name");
        }

        if (Modifier.isPrivate(token.getModifiers())) {
            throw new ImplerException("You must write public interface name");
        }

        try {
            Path path = getFullPath(root, token);
            Path parent = path.getParent();

            if (parent != null && Files.notExists(parent)) {
                try {
                    Files.createDirectories(parent);
                } catch (IOException e) {
                    throw new ImplerException("can't make directory: " + parent, e);
                }
            }

            try (BufferedWriter writer1 = Files.newBufferedWriter(path)) {
                writer = writer1;
                final String packageName = token.getPackageName();
                if (!packageName.isEmpty()) {
                    writeMany("package ", packageName, ";");
                    writer.newLine();
                }
                writeMany("public class ", token.getSimpleName(), "Impl implements ", token.getCanonicalName(), " {");
                writer.newLine();
                for (Method method : token.getMethods()) {
                    if (!Modifier.isStatic(method.getModifiers()) && !method.isDefault()) {
                        writeMethod(method);
                    }
                }
                writer.write("}");
            } catch (IOException e) {
                throw new ImplerException("Error while writing in file", e);
            }
        } catch (InvalidPathException e) {
            throw new ImplerException("Invalid path", e);
        }
    }

    /**
     * Implement interface ({@code token}) to jar. New jar file will be located in {@code jarFile}.
     *
     * @param token instances of interface that you want to implement
     * @param jarFile path to jar file you must create
     * @throws ImplerException if error occurs while {@link Implementor#implement(Class, Path)}. Also, if {@link IOException} occurs.
     */
    @Override
    public void implementJar(Class<?> token, Path jarFile) throws ImplerException {
        Path root;
        try {
            root = Files.createTempDirectory(jarFile.getParent(), "temp_");
        } catch (IOException e) {
            throw new ImplerException("Error while creating directory", e);
        }
        new Implementor().implement(token, root);
        compile(token, root);
        Path classPath = Path.of("", token.getPackageName().split("\\."))
                .resolve(token.getSimpleName() + "Impl.class");
        createJar(root, classPath, jarFile);
    }

    /**
     * Compiles a {@code token}
     *
     * @param token instances of interface that you want to implement
     * @param root path to root directory
     * @throws ImplerException if can't find java compiler or can't compile {@code token}
     */
    private static void compile(Class<?> token, Path root) throws ImplerException {
        final JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) {
            throw new ImplerException("Could not find java compiler, include tools.jar to classpath");
        }
        try {
            final Path classpath = root.
                    resolve(Path.of("", (Path.of(token.getProtectionDomain().getCodeSource().getLocation().toURI())).toString()));
            final Path path = getFullPath(root, token);
            if (compiler.run(null, null, null, path.toString(),
                    "-cp", classpath.toString(), "-encoding", "UTF8") != 0) {
                throw new ImplerException("Can not compile token");
            }
        } catch (URISyntaxException e) {
            throw new ImplerException("error while getting class path", e);
        }
    }

    /**
     * Creates a jar file for this class file
     *
     * @param root path to root directory
     * @param classPath path to class file
     * @param jarFile path to jar file
     * @throws ImplerException if some {@link IOException} occurs
     */
    private static void createJar(Path root, Path classPath, Path jarFile) throws ImplerException {
        Manifest manifest = new Manifest();
        manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
        StringBuilder jarClassPath = new StringBuilder();
        for (Path p : classPath) {
            jarClassPath.append(p).append("/");
        }
        try (JarOutputStream output = new JarOutputStream(Files.newOutputStream(jarFile), manifest)) {
            output.putNextEntry(new ZipEntry(jarClassPath.substring(0, jarClassPath.length() - 1)));
            Files.copy(root.resolve(classPath), output);
        } catch (IOException e) {
            throw new ImplerException("Error while writing jar", e);
        }
    }

    /**
     * Write down a few lines using {@link BufferedWriter#write(String)}.
     *
     * @param strings strings that you write to {@link Implementor#writer}
     * @throws IOException if error occurs while writing
     */
    private void writeMany(String... strings) throws IOException {
        for (String string : strings) {
            writer.write(string);
        }
    }

    /**
     * Writes the method passed as an argument to the {@link Implementor#writer}
     *
     * @param method method that you need to write to the {@link Implementor#writer}
     * @throws IOException if error occurs while writing
     */
    private void writeMethod(Method method) throws IOException {
        writeMany(TAB, "@Override", " public ", method.getReturnType().getCanonicalName(), SPACE, method.getName(), "(");
        boolean first = true;
        for (int i = 0; i < method.getParameterTypes().length; ++i) {
            Class<?> myClass = method.getParameterTypes()[i];
            if (!first) {
                writer.write(", ");
            } else {
                first = false;
            }
            writeMany(myClass.getCanonicalName(), " var" + i);
        }
        writeMany(")", " {");
        writer.newLine();
        writeMany(TAB, TAB, "return ", getNullElem(method), ";");
        writer.newLine();
        writer.write(TAB);
        writer.write("}");
        writer.newLine();
    }

    /**
     * Return zero element for method.
     * Return empty instance of {@link String} if method return type is "void".
     * Return instance of {@link String} equals "false" if method return type is "boolean".
     * Return instance of {@link String} equals "0" if method return type is primitive.
     * Else return instance of {@link String} equals "null".
     *
     * @param method method to get a zero element
     * @return zero element for method
     */
    private static String getNullElem(Method method) {
        if (method.getReturnType().getSimpleName().equals("void")) {
            return "";
        } else if (method.getReturnType().getSimpleName().equals("boolean")) {
            return "false";
        } else if (method.getReturnType().isPrimitive()) {
            return "0";
        } else {
            return "null";
        }
    }

    /**
     * Give path to interface implementation file (with Impl in name).
     *
     * @param root path to root directory
     * @param token instances of interface that you want to implement
     * @return path to implementation of interface
     */
    private static Path getFullPath (Path root, Class<?> token) {
        return root
                .resolve(Path.of("", token.getPackageName().split("\\.")))
                .resolve(token.getSimpleName() + "Impl.java");
    }
}
