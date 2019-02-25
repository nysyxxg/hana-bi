package com.anur.core.protocol.operation;

/**
 * Created by Anur IjuoKaruKas on 2/25/2019
 */
public class OperationAndOffset {

    private Operation operation;

    private long offset;

    public OperationAndOffset(Operation operation, long offset) {
        this.operation = operation;
        this.offset = offset;
    }

    public long nextOffset() {
        return offset + 1;
    }
}
