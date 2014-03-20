/**
 * 
 */
package queue;

/* Copy from Java Concurrency In Practice */

/**
 * StaticUtilities
 *
 * @author Brian Goetz and Tim Peierls
 */

public class LaunderThrowable {
	
	public static RuntimeException launderThrowable(Throwable t) {
        if (t instanceof RuntimeException)
            return (RuntimeException) t;
        else if (t instanceof Error)
            throw (Error) t;
        else
            throw new IllegalStateException("Not unchecked", t);
    }
}
