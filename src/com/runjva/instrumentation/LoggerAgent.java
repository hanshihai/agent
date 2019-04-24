package com.runjva.instrumentation;

import java.lang.instrument.*;
import java.util.*;
import javassist.*;

public class LoggerAgent implements ClassFileTransformer {

  public static void premain(String agentArgument,
      Instrumentation instrumentation) {
    if (agentArgument != null) {
      String[] args = agentArgument.split(",");
      Set argSet = new HashSet(Arrays.asList(args));

      if (argSet.contains("time")) {
        System.out.println("Start at " + new Date());
        Runtime.getRuntime().addShutdownHook(new Thread() {
          public void run() {
            System.out.println("Stop at " + new Date());
          }
        });
      }
      // ... more agent option handling here
    }
    instrumentation.addTransformer(new LoggerAgent());
  }

  String def = "private static java.util.logging.Logger _log;";
  String ifLog = "if (_log.isLoggable(java.util.logging.Level.INFO))";

  String[] ignore = new String[] { "sun/", "java/", "javax/" };

  //
  // The transform(...) method calls doClass(...) if the class name does not
  // start with any of the prefixes it has been told to ignore (note that the
  // separators are slashes, not dots).

  public byte[] transform(ClassLoader loader, String className,
      Class clazz, java.security.ProtectionDomain domain,
      byte[] bytes) {

    for (int i = 0; i < ignore.length; i++) {
      if (className.startsWith(ignore[i])) {
        return bytes;
      }
    }
    return doClass(className, clazz, bytes);
  }

  //
  // The doClass(...) method uses javassist to analyze the byte stream. If it
  // is a real class, a logger field is added and initialized to the name of
  // the class. Each non-empty method is then processed with doMethod(...).
  // The finally-clause ensures that the class definition is removed again
  // from the javassist pools to keep memory usage down.
  //
  // Note: The logger variable has been named _log. In a production version an
  // unused variable name should be found and used.

  private byte[] doClass(String name, Class clazz, byte[] b) {
    ClassPool pool = ClassPool.getDefault();
    CtClass cl = null;
    try {
      cl = pool.makeClass(new java.io.ByteArrayInputStream(b));
      if (cl.isInterface() == false) {

        CtField field = CtField.make(def, cl);
        String getLogger = "java.util.logging.Logger.getLogger("
            + name.replace('/', '.') + ".class.getName());";
        cl.addField(field, getLogger);

        CtBehavior[] methods = cl.getDeclaredBehaviors();
        for (int i = 0; i < methods.length; i++) {
          if (methods[i].isEmpty() == false) {
            doMethod(methods[i]);
          }
        }
        b = cl.toBytecode();
      }
    } catch (Exception e) {
      System.err.println("Could not instrument  " + name
          + ",  exception : " + e.getMessage());
    } finally {
      if (cl != null) {
        cl.detach();
      }
    }
    return b;
  }

  // The doMethod(...) class creates "_log.info(...)" snippets to insert at
  // the beginning and end of each method. Both contain the parameters (as
  // they may have changed), and the end method statement contain the return
  // value for non-void methods (which is available as $_ in the javassist
  // compiler).

  private void doMethod(CtBehavior method)
      throws NotFoundException, CannotCompileException {

    String signature = JavassistHelper.getSignature(method);
    String returnValue = JavassistHelper.returnValue(method);

    method.insertBefore(ifLog + "_log.info(\">> " + signature
        + ");");

    method.insertAfter(ifLog + "_log.info(\"<< " + signature
        + returnValue + ");");
  }
}