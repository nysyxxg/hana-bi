package com.anur.util;

import java.nio.ByteBuffer;
import com.anur.core.struct.base.Operation;

/**
 * Created by Anur IjuoKaruKas on 2019/2/27
 *
 * 一些和 Operation 相关的 ByteBuffer 操作
 */
public class ByteBufferUtil {

    /**
     * 将一个 operation，写入 buffer 中，并为其分配 offset
     */
    public static void writeMessage(ByteBuffer buffer, Operation operation, long offset) {
        buffer.putLong(offset);
        buffer.putInt(operation.size());
        buffer.put(operation.getByteBuffer());
        operation.getByteBuffer()
                 .rewind();
    }

    /**
     * Compute the CRC32 of the byte array
     *
     * @param bytes The array to compute the checksum for
     *
     * @return The CRC32
     */
    public static long crc32(byte[] bytes) {
        return crc32(bytes, 0, bytes.length);
    }

    /**
     * @param offset 从哪一位开始计算offset
     * @param size 从上面开始，计算xx位
     */
    public static long crc32(byte[] bytes, int offset, int size) {
        Crc32 crc = new Crc32();
        crc.update(bytes, offset, size);
        return crc.getValue();
    }

    /**
     * Write the given long value as a 4 byte unsigned integer. Overflow is ignored.
     *
     * @param buffer The buffer to write to
     * @param index The position in the buffer at which to begin writing
     * @param value The value to write
     */
    public static void writeUnsignedInt(ByteBuffer buffer, int index, long value) {
        buffer.putInt(index, (int) (value & 0xffffffffL));
    }

    /**
     * Read an unsigned integer from the given position without modifying the buffers position
     *
     * @param buffer the buffer to read from
     * @param index the index from which to read the integer
     *
     * @return The integer read, as a long to avoid signedness
     */
    public static long readUnsignedInt(ByteBuffer buffer, int index) {
        return buffer.getInt(index) & 0xffffffffL;
    }
}
