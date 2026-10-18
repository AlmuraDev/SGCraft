/*
 * This file is part of SGCraft, licensed under the MIT License (MIT).
 *
 * Copyright (c) AlmuraDev <https://github.com/AlmuraDev>
 * Copyright (c) contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package com.almuradev.sgcraft.mod.address;

import java.nio.ByteBuffer;
import java.security.GeneralSecurityException;
import java.util.Arrays;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * Puts a world's cells in an order only a secret key can follow. Each cell still gets exactly one code and each code
 * names at most one cell, but without the key no one can tell from a code which cell it names, nor from one cell's
 * code what any other cell's is.
 *
 * <p>The order is a Feistel network keyed with HMAC-SHA256, over the smallest even number of bits that holds every
 * code; a value that lands past the last code is shuffled again until it doesn't, so the order covers the codes
 * exactly.</p>
 */
public final class SecretOrder {

    /** How many bytes a key has. */
    public static final int KEY_BYTES = 32;

    private static final String ALGORITHM = "HmacSHA256";
    private static final int ROUNDS = 8;

    private final byte[] key;
    private final ThreadLocal<Mac> mac;

    /** @param key the server's secret key, {@value #KEY_BYTES} bytes */
    public SecretOrder(final byte[] key) {
        if (key.length != SecretOrder.KEY_BYTES) {
            throw new IllegalArgumentException("A key has " + SecretOrder.KEY_BYTES + " bytes, not " + key.length);
        }
        this.key = key.clone();
        final SecretKeySpec spec = new SecretKeySpec(this.key, SecretOrder.ALGORITHM);
        this.mac = ThreadLocal.withInitial(() -> {
            try {
                final Mac made = Mac.getInstance(SecretOrder.ALGORITHM);
                made.init(spec);
                return made;
            } catch (final GeneralSecurityException e) {
                throw new IllegalStateException("HmacSHA256 is missing", e);
            }
        });
    }

    /** @return the code a cell gets, from 0 to {@code codes - 1}; {@code cell} is below {@code codes} */
    public long scramble(final long cell, final long codes) {
        long value = cell;
        do {
            value = this.shuffle(value, codes, true);
        } while (value >= codes);
        return value;
    }

    /** @return the cell a code names: the reverse of {@link #scramble}; {@code code} is below {@code codes} */
    public long unscramble(final long code, final long codes) {
        long value = code;
        do {
            value = this.shuffle(value, codes, false);
        } while (value >= codes);
        return value;
    }

    // One pass of the network over both halves, forward or back
    private long shuffle(final long value, final long codes, final boolean forward) {
        final int bits = Math.max(2, 64 - Long.numberOfLeadingZeros(codes - 1));
        final int half = (bits + 1) / 2;
        final long mask = (1L << half) - 1;
        long left = value >>> half & mask;
        long right = value & mask;
        for (int i = 0; i < SecretOrder.ROUNDS; i++) {
            if (forward) {
                final long next = left ^ this.round(codes, i, right) & mask;
                left = right;
                right = next;
            } else {
                final long previous = right ^ this.round(codes, SecretOrder.ROUNDS - 1 - i, left) & mask;
                right = left;
                left = previous;
            }
        }
        return left << half | right;
    }

    // The round function: the key's hash of the number of codes, the round and one half
    private long round(final long codes, final int round, final long half) {
        final Mac hash = this.mac.get();
        final byte[] input = ByteBuffer.allocate(Long.BYTES * 2 + 1).putLong(codes).put((byte) round).putLong(half)
                .array();
        return ByteBuffer.wrap(hash.doFinal(input)).getLong();
    }

    @Override
    public boolean equals(final Object other) {
        return other instanceof final SecretOrder order && Arrays.equals(this.key, order.key);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(this.key);
    }
}
