package org.openstreetmap.josm.plugins.scripting.js;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.reflect.InvocationTargetException;
import java.util.logging.Logger;

import javax.swing.SwingUtilities;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;
import org.openstreetmap.josm.plugins.scripting.util.Assert;
import org.openstreetmap.josm.plugins.scripting.util.ExceptionUtil;
import org.openstreetmap.josm.plugins.scripting.util.IOUtil;

/**
 * A facade to the embedded rhino scripting engine.
 * <p>
 * Provides methods to prepare the a script context on the Swing EDT and to evaluate script
 * in this context.
 */
public class RhinoEngine {
	static private final Logger logger = Logger.getLogger(RhinoEngine.class.getName());
	
	private Scriptable swingThreadScope;
		
	protected void runOnSwingEDT(Runnable r){
		if (SwingUtilities.isEventDispatchThread()) {
			r.run();
		} else { 
			try {
				SwingUtilities.invokeAndWait(r);
			} catch(InvocationTargetException e){
				Throwable throwable = e.getCause(); 
		        if (throwable instanceof Error) { 
		            throw (Error) throwable; 
		        } else if(throwable instanceof RuntimeException) { 
		            throw (RuntimeException) throwable; 
		        }
		        // no other checked exceptions expected - log a warning 
		        logger.warning("Unexpected exception wrapped in InvocationTargetException: " + throwable.toString());
		        logger.warning(ExceptionUtil.stackTraceAsString(throwable));
			} catch(InterruptedException e){
				Thread.currentThread().interrupt();
			}
		}
	}
	
	/**
	 * Enter a scripting context on the Swing EDT. This method has to be invoked only once. 
	 * The context is maintained by Rhino as thread local variable.
	 * 
	 * @see #exitSwingThreadContext()
	 */
	public void enterSwingThreadContext() {
		Runnable r = new Runnable() {
			public void run() {
				if (Context.getCurrentContext() != null) return;
				Context ctx = Context.enter();				
				swingThreadScope = ctx.initStandardObjects();
				InputStreamReader reader = null;
				try {
					reader = new InputStreamReader(RhinoEngine.class.getResourceAsStream("/JOSM.js"));
					ctx.evaluateReader(swingThreadScope, reader, "JOSM.js", 0, null);
				} catch(IOException e){
					System.out.println(e);
				} finally {
					IOUtil.close(reader);
				}
			}
		};
		runOnSwingEDT(r);
	}
	
	/**
	 * Exit and discard the scripting context on the Swing EDT (if any).
	 */
	public void exitSwingThreadContext() {
		Runnable r = new Runnable() {
			public void run() {
				if (Context.getCurrentContext() == null) return;
				Context.exit();
			}
		};
		runOnSwingEDT(r);
	}
	
	/**
	 * Evaluate a script on the Swing EDT
	 * 
	 * @param script the script. Ignored if null.
	 */
	public void evaluateOnSwingThread(final String script) {
		if (script == null) return;
		Runnable r = new Runnable() {
			public void run() {
				Context ctx = Context.getCurrentContext();
				if (ctx == null){
					ctx = Context.enter();
				}
				ctx.evaluateString(swingThreadScope, script, "inlineScript", 0, null /* no security domain */);
			}
		};
		runOnSwingEDT(r);		
	}

	/**
	 * Reads and evaluates the script in the file <code>file</code> on the current Swing thread.
	 * 
	 * @param file the script file. Ignored if null. Must be a readable file
	 * @throws IllegalArgumentException thrown if file is a directory
	 * @throws IllegalArgumentException thrown if file isn't readable
	 * @throws FileNotFoundException thrown if file isn't found 
	 */
	public void evaluateOnSwingThread(final File file) throws FileNotFoundException, IOException {
		if (file == null) return;
		Assert.assertArg(!file.isDirectory(), "Can''t read script from a directory ''{0}''", file);
		Assert.assertArg(file.canRead(), "Can''t read script from file, because file isn''t readable. Got file ''{0}''", file);
		Reader reader = null;
		try {
			reader = new FileReader(file);
			final Reader fr = reader;
			Runnable r = new Runnable() {
				public void run() {
					Context ctx = Context.getCurrentContext();
					if (ctx == null){
						ctx = Context.enter();
					}
					try {
						ctx.evaluateReader(swingThreadScope, fr, file.toString(), 0, null /* no security domain */);
					} catch(IOException e){
						throw new RuntimeException(e);
					}
				}
			};
			try {
				runOnSwingEDT(r);
			} catch(RuntimeException e) {
				// unwrapping IO exception thrown from the runnable
				if (e.getCause() != null && e.getCause() instanceof IOException) {
					throw (IOException)e.getCause();
				} 
				throw e;
			}
		} finally {
			if (reader != null) IOUtil.close(reader);
		}
	}
}