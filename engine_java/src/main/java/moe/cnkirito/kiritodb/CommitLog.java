package moe.cnkirito.kiritodb;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author kirito.moe@foxmail.com
 * Date 2018-10-28
 */
public class CommitLog {

    final String path;
    private FileChannel fileChannel;
    private AtomicLong wrotePosition;

    static ThreadLocal<ByteBuffer> bufferThreadLocal = ThreadLocal.withInitial(()-> ByteBuffer.allocate(4*1024));

    private MemoryIndex memoryIndex;


    public CommitLog(String path) {
        this.path = path;
        File file = new File(path);
        if(!file.exists()){
            try {
                file.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        try {
            RandomAccessFile randomAccessFile = new RandomAccessFile(file, "rw");
            this.fileChannel = randomAccessFile.getChannel();
            this.wrotePosition = new AtomicLong(randomAccessFile.length());
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        this.memoryIndex = new MemoryIndex(path+"_index");
    }

    public void write(byte[] key,byte[] value){
        long position = wrotePosition.getAndAdd(value.length);
        try {
            ByteBuffer buffer = ByteBuffer.wrap(value);
            while (buffer.hasRemaining()) {
                this.fileChannel.write(buffer, position + (value.length - buffer.remaining()));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        this.memoryIndex.recordPosition(key, position);
    }

    public byte[] read(byte[] key){
        Long position = this.memoryIndex.getPosition(key);
        if(position==null) return null;
        ByteBuffer readBuffer = bufferThreadLocal.get();
        readBuffer.clear();
        try {
            fileChannel.read(readBuffer, position);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
        byte[] bytes = new byte[4 * 1024];
        readBuffer.flip();
        readBuffer.get(bytes);
        return bytes;
    }


    public void close() {
        memoryIndex.close();
    }
}
