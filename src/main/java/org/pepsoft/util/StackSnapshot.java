/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pepsoft.util;

/**
 *
 * @author pepijn
 */
public class StackSnapshot {
    /**
     * Create a new StackSnapshot of the current stack, ommitting
     * <code>omitFrames</code> frames off the top (in addition to the
     * constructor itself, which is always omitted).
     * 
     * @param omitFrames The number of frames to omit off the top of the stack,
     *     in addition to the constructor.
     */
    private StackSnapshot(int omitFrames) {
        StackTraceElement[] myStacktrace = new Throwable().getStackTrace();
        stackTrace = new StackTraceElement[myStacktrace.length - omitFrames - 1];
        System.arraycopy(myStacktrace, myStacktrace.length - stackTrace.length, stackTrace, 0, stackTrace.length);
    }
    
    /**
     * Determine whether the bottom potion of this stack snapshot is the same as
     * the entirety of the specified stack snapshot. In other words, determines
     * whether this stack snapshot is deeper than, or a continuation of, the
     * specified stack snapshot.
     * 
     * @param stackSnapshot The stack snapshot to compare with the start of this
     *     one.
     * @return <code>true</code> if this stack snapshot starts with the
     *     specified one.
     */
    public boolean startsWith(StackSnapshot stackSnapshot) {
        int d = stackTrace.length - stackSnapshot.stackTrace.length;
        if (d < 0) {
            System.out.println("StackSnapshot.startsWith");
            System.out.println("This snapshot: ");
            for (StackTraceElement element: stackTrace) {
                System.out.println("    " + element);
            }
            System.out.println("Other snapshot: ");
            for (StackTraceElement element: stackSnapshot.stackTrace) {
                System.out.println("    " + element);
            }
            return false;
        }
        for (int i = 0; i < stackSnapshot.stackTrace.length; i++) {
            if (! stackTrace[i + d].equals(stackSnapshot.stackTrace[i])) {
                System.out.println("StackSnapshot.startsWith");
                System.out.println("This snapshot: ");
                for (StackTraceElement element: stackTrace) {
                    System.out.println("    " + element);
                }
                System.out.println("Other snapshot: ");
                for (StackTraceElement element: stackSnapshot.stackTrace) {
                    System.out.println("    " + element);
                }
                return false;
            }
        }
        return true;
    }
    
    public static StackSnapshot get() {
        return get(1);
    }
    
    public static StackSnapshot get(int omitFrames) {
        return new StackSnapshot(omitFrames + 1);
    }
    
    private final StackTraceElement[] stackTrace;
}