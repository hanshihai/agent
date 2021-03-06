package com.runjva.instrumentation;

import javassist.CtBehavior;
import javassist.CtClass;
import javassist.CtConstructor;
import javassist.CtMethod;
import javassist.Modifier;
import javassist.NotFoundException;
import javassist.bytecode.CodeAttribute;
import javassist.bytecode.LocalVariableAttribute;

public class JavassistHelper {

  static String returnValue(CtBehavior method)
      throws NotFoundException {
    String returnValue = "";
    if (methodReturnsValue(method)) {
      returnValue = "\" returns: \" + $_ ";
    }
    return returnValue;
  }

  private static boolean methodReturnsValue(CtBehavior method)
      throws NotFoundException {
    CtClass returnType = ((CtMethod) method).getReturnType();
    String returnTypeName = returnType.getName();
    
    boolean isVoidMethod = (method instanceof CtMethod)
        && "void".equals(returnTypeName);
    boolean isConstructor = method instanceof CtConstructor;

    boolean methodReturnsValue = (isVoidMethod || isConstructor) == false;
    return methodReturnsValue;
  }

  static String getSignature(CtBehavior method)
      throws NotFoundException {
    CtClass parameterTypes[] = method.getParameterTypes();

    CodeAttribute codeAttribute = method.getMethodInfo()
        .getCodeAttribute();

    LocalVariableAttribute locals = (LocalVariableAttribute) codeAttribute
        .getAttribute("LocalVariableTable");
    String methodName = method.getName();

    StringBuffer sb = new StringBuffer(methodName + "(\" ");
    for (int i = 0; i < parameterTypes.length; i++) {
      if (i > 0) {
        sb.append(" + \", \" ");
      }

      CtClass parameterType = parameterTypes[i];
      CtClass arrayOf = parameterType.getComponentType();

      sb.append(" + \"");
      sb.append(parameterNameFor(method, locals, i));
      sb.append("\" + \"=");

      // use Arrays.asList() to render array of objects.
      if (arrayOf != null && !arrayOf.isPrimitive()) {
        sb.append("\"+ java.util.Arrays.asList($" + (i + 1)
            + ")");
      } else {
        sb.append("\"+ $" + (i + 1));
      }
    }
    sb.append("+\")\"");

    String signature = sb.toString();
    return signature;
  }

  static String parameterNameFor(CtBehavior method,
      LocalVariableAttribute locals, int i) {
    if (locals == null) {
      return Integer.toString(i + 1);
    }

    if (Modifier.isStatic(method.getModifiers())) {
      return locals.variableName(i);
    }

    // skip #0 which is reference to "this"
    return locals.variableName(i + 1);
  }

}
