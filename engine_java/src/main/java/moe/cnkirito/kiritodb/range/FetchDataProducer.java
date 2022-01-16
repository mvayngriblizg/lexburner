package moe.cnkirito.kiritodb.range;

import moe.cnkirito.kiritodb.KiritoDB;
import moe.cnkirito.kiritodb.common.Constant;
import moe.cnkirito.kiritodb.common.LoopQuerySemaphore;
import moe.cnkirito.kiritodb.data.CommitLog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sun.nio.ch.DirectBuffer;

import java.io.IOException;
import java.nio.ByteBuffer;

public class FetchDataProducer {

    public final static Logger logger = LoggerFactory.getLogger(FetchDataProducer.class);

    private int windowsNum;
    private ByteBuffer[] buffers;
    private LoopQuerySemaphore[] readSemaphores;
    private LoopQuerySemaphore[] writeSemaphores;
    private CommitLog[] commitLogs;

    public FetchDataProducer(KiritoDB kiritoDB) {
        int expectedNumPerPartition = kiritoDB.commitLogs[0].getFileLength();
        for (int i = 1; i < Constant.partitionNum; i++) {
            expectedNumPerPartition = Math.max(kiritoDB.commitLogs[i].getFileLength(), expectedNumPerPartition);
        }
        if (expectedNumPerPartition < 64000) {
            windowsNum = 4;
        } else {
            windowsNum = 1;
        }
        buffers = new ByteBuffer[windowsNum];
        readSemaphores = new LoopQuerySemaphore[windowsNum];
        writeSemaphores = new LoopQuerySemaphore[windowsNum];
        for (int i = 0; i < windowsNum; i++) {
            writeSemaphores[i] = new LoopQuerySemaphore(1);
            readSemaphores[i] = new LoopQuerySemaphore(0);
            if(windowsNum==1){
                buffers[i] = ByteBuffer.allocateDirect(expectedNumPerPartition * Constant.VALUE_LENGTH);
            }else {
                buffers[i] = ByteBuffer.allocateDirect(expectedNumPerPartition * Constant.VALUE_LENGTH);
            }
        }
        this.commitLogs = kiritoDB.commitLogs;
        logger.info("expectedNumPerPartition={}", expectedNumPerPartition);
    }

    public void startFetch() {
        for (int threadNo = 0; threadNo < windowsNum; threadNo++) {
            final int threadPartition = threadNo;
            new Thread(() -> {
                try {
                    for (int i = 0; i < Constant.partitionNum / windowsNum; i++) {
                        writeSemaphores[threadPartition].acquireNoSleep();
                        commitLogs[i * windowsNum + threadPartition].loadAll(buffers[threadPartition]);
                        readSemaphores[threadPartition].release();
                    }
                } catch (InterruptedException | IOException e) {
                    logger.error("threadNo{} load failed", threadPartition, e);
                }
            }).start();
        }
    }


    public ByteBuffer getBuffer(int partition) {
        try {
            readSemaphores[partition % windowsNum].acquireNoSleep();
        } catch (InterruptedException e) {
            logger.error("threadNo{} getBuffer failed", partition, e);
        }
        return buffers[partition % windowsNum];
    }

    public void release(int partition) {
        writeSemaphores[partition % windowsNum].release();
    }

    public void destroy(){
        if(buffers!=null){
            for (ByteBuffer buffer : buffers) {
                if(buffer instanceof DirectBuffer){
                    ((DirectBuffer) buffer).cleaner().clean();
                }
            }
        }
    }

}
