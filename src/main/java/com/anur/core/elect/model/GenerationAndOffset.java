package com.anur.core.elect.model;

import java.util.Optional;

/**
 * Created by Anur IjuoKaruKas on 2019/3/12
 */
public class GenerationAndOffset implements Comparable<GenerationAndOffset> {

    public static final GenerationAndOffset INVALID = new GenerationAndOffset(-1, -1);

    private long generation;

    private long offset;

    public GenerationAndOffset(long generation, long offset) {
        this.generation = generation;
        this.offset = offset;
    }

    public long getGeneration() {
        return generation;
    }

    public long getOffset() {
        return offset;
    }

    public GenerationAndOffset next() {
        if (offset == Long.MAX_VALUE) {
            return new GenerationAndOffset(generation + 1, 1);
        } else {
            return new GenerationAndOffset(generation, offset + 1);
        }
    }

    @Override
    public boolean equals(Object obj) {
        return Optional.ofNullable(obj)
                       .filter(o -> o instanceof GenerationAndOffset)
                       .map(o -> (GenerationAndOffset) o)
                       .map(o -> o.generation == this.generation && o.offset == this.offset)
                       .orElse(false);
    }

    @Override
    public int compareTo(GenerationAndOffset o) {
        if (this.generation > o.generation) {
            return 1;
        } else if (this.generation == o.generation) {
            if (this.offset > o.offset) {
                return 1;
            } else if (this.offset == o.offset) {
                return 0;
            } else {
                return -1;
            }
        } else {
            return -1;
        }
    }

    @Override
    public String toString() {
        return "- {" +
            "generation=" + generation +
            ", offset=" + offset +
            "} -";
    }
}
