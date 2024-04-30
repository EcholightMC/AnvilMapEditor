package net.minestom.server.network;

import net.minestom.server.utils.validate.Check;
import org.jetbrains.annotations.NotNull;
import org.jglrxavpok.hephaistos.nbt.*;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

import static net.minestom.server.network.NetworkBuffer.*;

interface NetworkBufferTypeImpl<T> extends Type<T> {
	int SEGMENT_BITS = 0x7F;
	int CONTINUE_BIT = 0x80;

	record BooleanType() implements NetworkBufferTypeImpl<Boolean> {
		@Override
		public void write(@NotNull NetworkBuffer buffer, Boolean value) {
			buffer.ensureSize(1);
			buffer.nioBuffer.put(buffer.writeIndex(), value ? (byte) 1 : (byte) 0);
			buffer.writeIndex += 1;
		}

		@Override
		public Boolean read(@NotNull NetworkBuffer buffer) {
			final byte value = buffer.nioBuffer.get(buffer.readIndex());
			buffer.readIndex += 1;
			return value == 1;
		}
	}

	record ByteType() implements NetworkBufferTypeImpl<Byte> {
		@Override
		public void write(@NotNull NetworkBuffer buffer, Byte value) {
			buffer.ensureSize(1);
			buffer.nioBuffer.put(buffer.writeIndex(), value);
			buffer.writeIndex += 1;
		}

		@Override
		public Byte read(@NotNull NetworkBuffer buffer) {
			final byte value = buffer.nioBuffer.get(buffer.readIndex());
			buffer.readIndex += 1;
			return value;
		}
	}

	record ShortType() implements NetworkBufferTypeImpl<Short> {
		@Override
		public void write(@NotNull NetworkBuffer buffer, Short value) {
			buffer.ensureSize(2);
			buffer.nioBuffer.putShort(buffer.writeIndex(), value);
			buffer.writeIndex += 2;
		}

		@Override
		public Short read(@NotNull NetworkBuffer buffer) {
			final short value = buffer.nioBuffer.getShort(buffer.readIndex());
			buffer.readIndex += 2;
			return value;
		}
	}

	record UnsignedShortType() implements NetworkBufferTypeImpl<Integer> {
		@Override
		public void write(@NotNull NetworkBuffer buffer, Integer value) {
			buffer.ensureSize(2);
			buffer.nioBuffer.putShort(buffer.writeIndex(), (short) (value & 0xFFFF));
			buffer.writeIndex += 2;
		}

		@Override
		public Integer read(@NotNull NetworkBuffer buffer) {
			final short value = buffer.nioBuffer.getShort(buffer.readIndex());
			buffer.readIndex += 2;
			return value & 0xFFFF;
		}
	}

	record IntType() implements NetworkBufferTypeImpl<Integer> {
		@Override
		public void write(@NotNull NetworkBuffer buffer, Integer value) {
			buffer.ensureSize(4);
			buffer.nioBuffer.putInt(buffer.writeIndex(), value);
			buffer.writeIndex += 4;
		}

		@Override
		public Integer read(@NotNull NetworkBuffer buffer) {
			final int value = buffer.nioBuffer.getInt(buffer.readIndex());
			buffer.readIndex += 4;
			return value;
		}
	}

	record LongType() implements NetworkBufferTypeImpl<Long> {
		@Override
		public void write(@NotNull NetworkBuffer buffer, Long value) {
			buffer.ensureSize(8);
			buffer.nioBuffer.putLong(buffer.writeIndex(), value);
			buffer.writeIndex += 8;
		}

		@Override
		public Long read(@NotNull NetworkBuffer buffer) {
			final long value = buffer.nioBuffer.getLong(buffer.readIndex());
			buffer.readIndex += 8;
			return value;
		}
	}

	record FloatType() implements NetworkBufferTypeImpl<Float> {
		@Override
		public void write(@NotNull NetworkBuffer buffer, Float value) {
			buffer.ensureSize(4);
			buffer.nioBuffer.putFloat(buffer.writeIndex(), value);
			buffer.writeIndex += 4;
		}

		@Override
		public Float read(@NotNull NetworkBuffer buffer) {
			final float value = buffer.nioBuffer.getFloat(buffer.readIndex());
			buffer.readIndex += 4;
			return value;
		}
	}

	record DoubleType() implements NetworkBufferTypeImpl<Double> {
		@Override
		public void write(@NotNull NetworkBuffer buffer, Double value) {
			buffer.ensureSize(8);
			buffer.nioBuffer.putDouble(buffer.writeIndex(), value);
			buffer.writeIndex += 8;
		}

		@Override
		public Double read(@NotNull NetworkBuffer buffer) {
			final double value = buffer.nioBuffer.getDouble(buffer.readIndex());
			buffer.readIndex += 8;
			return value;
		}
	}

	record VarIntType() implements NetworkBufferTypeImpl<Integer> {
		@Override
		public void write(@NotNull NetworkBuffer buffer, Integer boxed) {
			final int value = boxed;
			final int index = buffer.writeIndex();
			if ((value & (0xFFFFFFFF << 7)) == 0) {
				buffer.ensureSize(1);
				buffer.nioBuffer.put(index, (byte) value);
				buffer.writeIndex += 1;
			} else if ((value & (0xFFFFFFFF << 14)) == 0) {
				buffer.ensureSize(2);
				buffer.nioBuffer.putShort(index, (short) ((value & 0x7F | 0x80) << 8 | (value >>> 7)));
				buffer.writeIndex += 2;
			} else if ((value & (0xFFFFFFFF << 21)) == 0) {
				buffer.ensureSize(3);
				var nio = buffer.nioBuffer;
				nio.put(index, (byte) (value & 0x7F | 0x80));
				nio.put(index + 1, (byte) ((value >>> 7) & 0x7F | 0x80));
				nio.put(index + 2, (byte) (value >>> 14));
				buffer.writeIndex += 3;
			} else if ((value & (0xFFFFFFFF << 28)) == 0) {
				buffer.ensureSize(4);
				var nio = buffer.nioBuffer;
				nio.putInt(index, (value & 0x7F | 0x80) << 24 | (((value >>> 7) & 0x7F | 0x80) << 16)
										  | ((value >>> 14) & 0x7F | 0x80) << 8 | (value >>> 21));
				buffer.writeIndex += 4;
			} else {
				buffer.ensureSize(5);
				var nio = buffer.nioBuffer;
				nio.putInt(index, (value & 0x7F | 0x80) << 24 | ((value >>> 7) & 0x7F | 0x80) << 16
										  | ((value >>> 14) & 0x7F | 0x80) << 8 | ((value >>> 21) & 0x7F | 0x80));
				nio.put(index + 4, (byte) (value >>> 28));
				buffer.writeIndex += 5;
			}
		}

		@Override
		public Integer read(@NotNull NetworkBuffer buffer) {
			int index = buffer.readIndex();
			// https://github.com/jvm-profiling-tools/async-profiler/blob/a38a375dc62b31a8109f3af97366a307abb0fe6f/src/converter/one/jfr/JfrReader.java#L393
			int result = 0;
			for (int shift = 0; ; shift += 7) {
				byte b = buffer.nioBuffer.get(index++);
				result |= (b & 0x7f) << shift;
				if (b >= 0) {
					buffer.readIndex += index - buffer.readIndex();
					return result;
				}
			}
		}
	}

	record VarLongType() implements NetworkBufferTypeImpl<Long> {
		@Override
		public void write(@NotNull NetworkBuffer buffer, Long value) {
			buffer.ensureSize(10);
			int size = 0;
			while (true) {
				if ((value & ~((long) SEGMENT_BITS)) == 0) {
					buffer.nioBuffer.put(buffer.writeIndex() + size, (byte) value.intValue());
					buffer.writeIndex += size + 1;
					return;
				}
				buffer.nioBuffer.put(buffer.writeIndex() + size, (byte) (value & SEGMENT_BITS | CONTINUE_BIT));
				size++;
				// Note: >>> means that the sign bit is shifted with the rest of the number rather than being left alone
				value >>>= 7;
			}
		}

		@Override
		public Long read(@NotNull NetworkBuffer buffer) {
			int length = 0;
			long value = 0;
			int position = 0;
			byte currentByte;
			while (true) {
				currentByte = buffer.nioBuffer.get(buffer.readIndex() + length);
				length++;
				value |= (long) (currentByte & SEGMENT_BITS) << position;
				if ((currentByte & CONTINUE_BIT) == 0) break;
				position += 7;
				if (position >= 64) throw new RuntimeException("VarLong is too big");
			}
			buffer.readIndex += length;
			return value;
		}
	}

	record RawBytesType() implements NetworkBufferTypeImpl<byte[]> {
		@Override
		public void write(@NotNull NetworkBuffer buffer, byte[] value) {
			buffer.ensureSize(value.length);
			buffer.nioBuffer.put(buffer.writeIndex(), value);
			buffer.writeIndex += value.length;
		}

		@Override
		public byte[] read(@NotNull NetworkBuffer buffer) {
			final int limit = buffer.nioBuffer.limit();
			final int length = limit - buffer.readIndex();
			assert length > 0 : "Invalid remaining: " + length;
			final byte[] bytes = new byte[length];
			buffer.nioBuffer.get(buffer.readIndex(), bytes);
			buffer.readIndex += length;
			return bytes;
		}
	}

	record StringType() implements NetworkBufferTypeImpl<String> {
		@Override
		public void write(@NotNull NetworkBuffer buffer, String value) {
			final byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
			buffer.write(VAR_INT, bytes.length);
			buffer.write(RAW_BYTES, bytes);
		}

		@Override
		public String read(@NotNull NetworkBuffer buffer) {
			final int length = buffer.read(VAR_INT);
			final int remaining = buffer.nioBuffer.limit() - buffer.readIndex();
			Check.argCondition(length > remaining, "String is too long (length: {0}, readable: {1})", length, remaining);
			byte[] bytes = new byte[length];
			buffer.nioBuffer.get(buffer.readIndex(), bytes);
			buffer.readIndex += length;
			return new String(bytes, StandardCharsets.UTF_8);
		}
	}

	record NbtType() implements NetworkBufferTypeImpl<org.jglrxavpok.hephaistos.nbt.NBT> {
		@Override
		public void write(@NotNull NetworkBuffer buffer, org.jglrxavpok.hephaistos.nbt.NBT value) {
			NBTWriter nbtWriter = buffer.nbtWriter;
			if (nbtWriter == null) {
				nbtWriter = new NBTWriter(new OutputStream() {
					@Override
					public void write(int b) {
						buffer.write(BYTE, (byte) b);
					}
				}, CompressedProcesser.NONE);
				buffer.nbtWriter = nbtWriter;
			}
			try {
				if (value == NBTEnd.INSTANCE) {
					// Kotlin - https://discord.com/channels/706185253441634317/706186227493109860/1163703658341478462
					buffer.write(BYTE, (byte) NBTType.TAG_End.getOrdinal());
				} else {
					buffer.write(BYTE, (byte) value.getID().getOrdinal());
					nbtWriter.writeRaw(value);
				}
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}

		@Override
		public org.jglrxavpok.hephaistos.nbt.NBT read(@NotNull NetworkBuffer buffer) {
			NBTReader nbtReader = buffer.nbtReader;
			if (nbtReader == null) {
				nbtReader = new NBTReader(new InputStream() {
					@Override
					public int read() {
						return buffer.read(BYTE) & 0xFF;
					}

					@Override
					public int available() {
						return buffer.readableBytes();
					}
				}, CompressedProcesser.NONE);
				buffer.nbtReader = nbtReader;
			}
			try {
				byte tagId = buffer.read(BYTE);
				if (tagId == NBTType.TAG_End.getOrdinal())
					return NBTEnd.INSTANCE;
				return nbtReader.readRaw(tagId);
			} catch (IOException | NBTException e) {
				throw new RuntimeException(e);
			}
		}
	}

	record ByteArrayType() implements NetworkBufferTypeImpl<byte[]> {
		@Override
		public void write(@NotNull NetworkBuffer buffer, byte[] value) {
			buffer.write(VAR_INT, value.length);
			buffer.write(RAW_BYTES, value);
		}

		@Override
		public byte[] read(@NotNull NetworkBuffer buffer) {
			final int length = buffer.read(VAR_INT);
			final byte[] bytes = new byte[length];
			buffer.nioBuffer.get(buffer.readIndex(), bytes);
			buffer.readIndex += length;
			return bytes;
		}
	}

	record LongArrayType() implements NetworkBufferTypeImpl<long[]> {
		@Override
		public void write(@NotNull NetworkBuffer buffer, long[] value) {
			buffer.write(VAR_INT, value.length);
			for (long l : value) buffer.write(LONG, l);
		}

		@Override
		public long[] read(@NotNull NetworkBuffer buffer) {
			final int length = buffer.read(VAR_INT);
			final long[] longs = new long[length];
			for (int i = 0; i < length; i++) longs[i] = buffer.read(LONG);
			return longs;
		}
	}

	record VarIntArrayType() implements NetworkBufferTypeImpl<int[]> {
		@Override
		public void write(@NotNull NetworkBuffer buffer, int[] value) {
			buffer.write(VAR_INT, value.length);
			for (int i : value) buffer.write(VAR_INT, i);
		}

		@Override
		public int[] read(@NotNull NetworkBuffer buffer) {
			final int length = buffer.read(VAR_INT);
			final int[] ints = new int[length];
			for (int i = 0; i < length; i++) ints[i] = buffer.read(VAR_INT);
			return ints;
		}
	}

	record VarLongArrayType() implements NetworkBufferTypeImpl<long[]> {
		@Override
		public void write(@NotNull NetworkBuffer buffer, long[] value) {
			buffer.write(VAR_INT, value.length);
			for (long l : value) buffer.write(VAR_LONG, l);
		}

		@Override
		public long[] read(@NotNull NetworkBuffer buffer) {
			final int length = buffer.read(VAR_INT);
			final long[] longs = new long[length];
			for (int i = 0; i < length; i++) longs[i] = buffer.read(VAR_LONG);
			return longs;
		}
	}

	record QuaternionType() implements NetworkBufferTypeImpl<float[]> {
		@Override
		public void write(@NotNull NetworkBuffer buffer, float[] value) {
			buffer.write(FLOAT, value[0]);
			buffer.write(FLOAT, value[1]);
			buffer.write(FLOAT, value[2]);
			buffer.write(FLOAT, value[3]);
		}

		@Override
		public float[] read(@NotNull NetworkBuffer buffer) {
			final float x = buffer.read(FLOAT);
			final float y = buffer.read(FLOAT);
			final float z = buffer.read(FLOAT);
			final float w = buffer.read(FLOAT);
			return new float[]{x, y, z, w};
		}
	}

	static <T extends Enum<?>> NetworkBufferTypeImpl<T> fromEnum(Class<T> enumClass) {
		return new NetworkBufferTypeImpl<>() {
			@Override
			public void write(@NotNull NetworkBuffer buffer, T value) {
				buffer.writeEnum(enumClass, value);
			}

			@Override
			public T read(@NotNull NetworkBuffer buffer) {
				return buffer.readEnum(enumClass);
			}
		};
	}

	static <T> NetworkBufferTypeImpl<T> fromOptional(Type<T> optionalType) {
		return new NetworkBufferTypeImpl<>() {
			@Override
			public void write(@NotNull NetworkBuffer buffer, T value) {
				buffer.writeOptional(optionalType, value);
			}

			@Override
			public T read(@NotNull NetworkBuffer buffer) {
				return buffer.readOptional(optionalType);
			}
		};
	}
}
