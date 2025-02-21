package com.rntgroup;

import javassist.ClassPool;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@Execution(ExecutionMode.CONCURRENT)
public class ExceptionsTest {
    private static final int CHAR_SIZE = 1_000_000_000;
    private static final String JAVA_HEAP_SPACE = "Java heap space";
    private static LargeObject largeObject;
    private Path tempDir;

    @BeforeEach
    public void init() throws IOException {
        tempDir = Files.createTempDirectory("oom_metaspace_classes");
    }

    @AfterEach
    public void end() throws IOException {
        Files.deleteIfExists(tempDir);
    }

    @Test
    public void checkOutOfMemoryErrorInHeapWithCollections() {
        OutOfMemoryError error = assertThrows(OutOfMemoryError.class, () -> {
            List<String> list = new ArrayList<>();
            while (true){
                list.add(new String(new char[CHAR_SIZE]));
            }
        });
        assertEquals(JAVA_HEAP_SPACE, error.getMessage());
    }

    @Test
    public void checkOutOfMemoryErrorInHeapWithoutCollections() {
        OutOfMemoryError error = assertThrows(OutOfMemoryError.class, () -> {
            while (true){
                largeObject = new LargeObject(largeObject);
            }
        });
        assertEquals(JAVA_HEAP_SPACE, error.getMessage());
    }

    @Test
    // Запускать с VM флагом -XX:MaxMetaspaceSize=32m, чтобы быстрее завершился
    public void checkOutOfMemoryErrorInMetaspace() {
        OutOfMemoryError error = assertThrows(OutOfMemoryError.class, () -> {
            ClassPool pool = ClassPool.getDefault();
            CustomClassLoader classLoader = new CustomClassLoader();

            while (true) {
                String className = tempDir + "\\GeneratedClass" + UUID.randomUUID();
                byte[] bytecode = pool
                    .makeClass(className)
                    .toBytecode();
                classLoader.defineClass(className, bytecode);
            }
        });

        assertEquals("Metaspace", error.getMessage());
    }

    @Test
    public void checkStackOverflowErrorWithRecursive(){
        assertThrows(StackOverflowError.class, this::recursiveMethod);
    }

    @Test
    public void checkStackOverflowErrorWithoutRecursive(){
        assertThrows(StackOverflowError.class, StackOverflowErrorExample::new);
    }

    private void recursiveMethod(){
        recursiveMethod();
    }

}
