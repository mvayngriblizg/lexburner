package moe.cnkirito.kiritodb;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;

public class ByteBufferTest {

    Logger logger = LoggerFactory.getLogger(ByteBufferTest.class);

    @Test
    public void test0(){
        System.out.println(Integer.MAX_VALUE/1024/1024);
    }

    @Test
    public void test3() {
        try{
            byte[] bytes = new byte[250 * 1024 * 1024];
            logger.error("250M 分配成功");
        }catch (Exception e){
            logger.error("250M 分配失败");
        }
    }

    @Test
    public void test4() {
        try{
            byte[] bytes = new byte[500 * 1024 * 1024];
            logger.error("500M 分配成功");
        }catch (Exception e){
            logger.error("500M 分配失败");
        }
    }


    @Test
    public void test1() {
        try{
            byte[] bytes = new byte[900 * 1024 * 1024];
            logger.error("900M 分配成功");
        }catch (Exception e){
            logger.error("900M 分配失败");
        }
    }

    @Test
    public void test2() {
        try{
            byte[] bytes = new byte[1024 * 1024 * 1024 /2 *3];
            logger.error("1.5G 分配成功");
        }catch (Exception e){
            logger.error("1.5G 分配失败");
        }
    }


}
