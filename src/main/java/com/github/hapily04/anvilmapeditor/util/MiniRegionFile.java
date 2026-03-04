package com.github.hapily04.anvilmapeditor.util;

import live.minehub.polarpaper.util.CoordConversion;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.util.BitSet;
import java.util.concurrent.locks.ReentrantLock;

public class MiniRegionFile {

    private static final int MAX_ENTRY_COUNT = 1024;
    private static final int SECTOR_SIZE = 4096;
    private static final int HEADER_LENGTH = MAX_ENTRY_COUNT * 2 * 4; // 2 4-byte fields per entry

    private final ReentrantLock lock = new ReentrantLock();
    private final RandomAccessFile file;

    private final int[] locations = new int[MAX_ENTRY_COUNT];
    private final BitSet freeSectors = new BitSet(2);

    // Cache header data to avoid repeated file I/O
    private final ByteBuffer headerBuffer = ByteBuffer.allocate(HEADER_LENGTH);

    public MiniRegionFile(File file) throws IOException {
        this.file = new RandomAccessFile(file, "rw");
        readHeader();
    }

    public boolean hasChunkData(int chunkX, int chunkZ) {
        lock.lock();
        try {
            return locations[getChunkIndex(chunkX, chunkZ)] != 0;
        } finally {
            lock.unlock();
        }
    }

    private void readHeader() throws IOException {
        file.seek(0);
        if (file.length() < HEADER_LENGTH) {
            // new file, fill in data
            file.write(new byte[HEADER_LENGTH]);
        }

        final long totalSectors = ((file.length() - 1) / SECTOR_SIZE) + 1; // Round up, last sector does not need to be full size
        freeSectors.set(0, (int) totalSectors); // Set all sectors as free initially
        freeSectors.clear(0); // First sector is locations
        freeSectors.clear(1); // Second sector is timestamps

        // Read entire header in one operation
        file.seek(0);
        byte[] headerData = new byte[HEADER_LENGTH];
        file.readFully(headerData);
        headerBuffer.clear();
        headerBuffer.put(headerData);
        headerBuffer.flip();

        // Parse locations from buffer
        for (int i = 0; i < MAX_ENTRY_COUNT; i++) {
            int location = locations[i] = headerBuffer.getInt();
            if (location != 0) {
                markLocationInBitSet(location);
            }
        }
    }

    private int getChunkIndex(int chunkX, int chunkZ) {
        return (chunkToRegionLocal(chunkZ) << 5) | chunkToRegionLocal(chunkX);
    }

    private void markLocationInBitSet(int location) {
        int sectorCount = location & 0xFF;
        int sectorStart = location >> 8;
        freeSectors.set(sectorStart, sectorStart + sectorCount, false);
    }

    public static int chunkToRegionLocal(int chunkCoordinate) {
        return chunkCoordinate & 0x1F;
    }

}