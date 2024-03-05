#!/bin/bash
javac -d jar -cp ../../java-advanced-2023/artifacts/info.kgeorgiy.java.advanced.implementor.jar ../java-solutions/info/kgeorgiy/ja/rynk/implementor/Implementor.java
jar cfm Implementor.jar MANIFEST.MF -C jar info/kgeorgiy/ja/rynk/implementor/Implementor.class
rm -r jar
