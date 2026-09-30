package com.trainticket;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * Lightweight test runner that discovers and executes JUnit 5 @Test annotated methods
 * directly without requiring external Surefire plugin downloads in sandboxed environments.
 */
public class TestRunner {

    public static void main(String[] args) {
        List<Class<?>> testClasses = new ArrayList<>();
        if (args.length > 0) {
            for (String className : args) {
                try {
                    testClasses.add(Class.forName(className));
                } catch (ClassNotFoundException e) {
                    System.err.println("Could not find test class: " + className);
                }
            }
        } else {
            testClasses.add(com.trainticket.util.PasswordUtilsTest.class);
            testClasses.add(com.trainticket.service.AuthServiceTest.class);
        }

        int totalPassed = 0;
        int totalFailed = 0;

        System.out.println("=================================================");
        System.out.println("  RailFlow Lightweight Unit Test Runner");
        System.out.println("=================================================");

        for (Class<?> testClass : testClasses) {
            System.out.println("\nRunning: " + testClass.getSimpleName());
            Object instance;
            try {
                instance = testClass.getDeclaredConstructor().newInstance();
            } catch (Exception e) {
                System.err.println("Failed to instantiate " + testClass.getName() + ": " + e.getMessage());
                totalFailed++;
                continue;
            }

            for (Method method : testClass.getDeclaredMethods()) {
                if (method.isAnnotationPresent(Test.class)) {
                    DisplayName dn = method.getAnnotation(DisplayName.class);
                    String testName = (dn != null) ? dn.value() : method.getName();
                    try {
                        // Invoke @BeforeEach lifecycle methods if present
                        for (Method bm : testClass.getDeclaredMethods()) {
                            if (bm.isAnnotationPresent(org.junit.jupiter.api.BeforeEach.class)) {
                                bm.setAccessible(true);
                                bm.invoke(instance);
                            }
                        }

                        method.setAccessible(true);
                        method.invoke(instance);
                        System.out.println("  ✓ PASS: " + testName);
                        totalPassed++;
                    } catch (Exception ex) {
                        Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
                        System.err.println("  ✗ FAIL: " + testName + " -> " + cause.getMessage());
                        cause.printStackTrace(System.err);
                        totalFailed++;
                    }
                }
            }
        }

        System.out.println("\n-------------------------------------------------");
        System.out.println(String.format("Results: %d passed, %d failed.", totalPassed, totalFailed));
        System.out.println("-------------------------------------------------");

        if (totalFailed > 0) {
            System.exit(1);
        }
    }
}
