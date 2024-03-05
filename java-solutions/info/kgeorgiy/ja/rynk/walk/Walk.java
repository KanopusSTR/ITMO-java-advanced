package info.kgeorgiy.ja.rynk.walk;

import java.io.*;
import java.nio.file.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;


public class Walk {

    public static final String ZEROS = "0".repeat(64) + " ";

    public static void main(String[] args) {
        if (args == null || args.length != 2) {
            System.out.println("You must write 2 arguments: inputFile and outputFile");
            return;
        }
        Path inputFile = getPath(args[0], "first argument");
        Path outputFile = getPath(args[1], "second argument");

        if (inputFile == null || outputFile == null) {
            return;
        }
        Path parent = outputFile.getParent();
        try {
            //:note: don't  create file
            if (parent != null && Files.notExists(parent)) {
                try {
                    Files.createDirectory(parent);
                    //:note:  useless
                } catch (IOException e) {
                    System.out.println("Can't create directory");
                    return;
                }
            }
        } catch (SecurityException e) {
            System.out.println("Error: security violation while checking outputFile existence");
        }

        try (BufferedWriter output = Files.newBufferedWriter(outputFile)) {
            try (BufferedReader pathBuf = Files.newBufferedReader(inputFile)) {
                //:note: don't close reade
                //:note: newBufferedReader UTF_8
                String path;
                try {
                    MessageDigest digest = MessageDigest.getInstance("SHA-256");
                    byte[] buffer = new byte[4096];
                    while (pathBuf.ready()) {
                        path = pathBuf.readLine();
                        try {
                            Path inPath = getPath(path, args[0]);
                            if (inPath == null) {
                                writeHash(output, ZEROS, path);
                                continue;
                            }
                            BufferedInputStream is = new BufferedInputStream(Files.newInputStream(inPath));
                            try {
                                int a;
                                while ((a = is.read(buffer)) != -1) {
                                    digest.update(buffer, 0, a);
                                }
                                byte[] hash = digest.digest();
                                StringBuilder hexString = new StringBuilder(2 * hash.length);
                                for (byte b : hash) {
                                    String hex = Integer.toHexString(0xff & b);
                                    if (hex.length() == 1) {
                                        hexString.append('0');
                                    }
                                    hexString.append(hex);
                                }
                                hexString.append(" ");
                                //:note: memory exceed
                                writeHash(output, hexString.toString(), path);
                            } catch (IOException e) {
                                System.out.println("Error while reading from: " + path);
                                writeHash(output, ZEROS, path);
                            }
                            //:note: Unchecked exception
                        } catch (IOException e) {
                            writeHash(output, ZEROS, path);
                        }
                    }
                } catch (NoSuchAlgorithmException e) {
                    System.out.println("Can't find SHA-256");
                }
            } catch (IOException e) {
                System.out.println("Can't read from inputFile");
            } catch (SecurityException e) {
                System.out.println("Problem with security in inputFile");
            }
        } catch (IOException e) {
            System.out.println("Can't open outputFile");
        }
    }

    private static Path getPath(String arg, String number) {
        if (arg == null) {
            System.out.println("You must write correct path in " + number);
            return null;
        }
        Path path;
        try {
            path = Paths.get(arg);
        } catch (InvalidPathException e) {
            System.out.println(number + " must be correct path: " + e.getMessage());
            return null;
        }
        return path;
    }

    private static void writeHash(BufferedWriter output, String strHash, String path) {
        //:note: copy-paste
        try {
            output.write(strHash);
            output.write(path);
            output.write(System.lineSeparator());
        } catch (IOException e) {
            System.out.println("Have some problem while writing in outputFile");
        } catch (SecurityException e) {
            System.out.println("Error: security violation while writing in outputFile");
        }
    }
}
