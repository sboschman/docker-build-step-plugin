package org.jenkinsci.plugins.dockerbuildstep.log;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.io.ByteStreams.copy;
import static com.google.common.io.ByteStreams.nullOutputStream;

import java.io.Closeable;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;

import com.google.common.io.ByteStreams;

/**
 * https://github.com/spotify/docker-client/blob/master/src/main/java/com/
 * spotify/docker/client/LogReader.java
 */
public class DockerLogStreamReader implements Closeable {
	private final InputStream stream;
	public static final int HEADER_SIZE = 8;
	public static final int FRAME_SIZE_OFFSET = 4;

	private volatile boolean closed;

	public DockerLogStreamReader(InputStream is) {
		this.stream = is;
	}

	public DockerLogMessage nextMessage() throws IOException {
		// Read header
		final byte[] headerBytes = new byte[HEADER_SIZE];
		final int n = ByteStreams.read(stream, headerBytes, 0, HEADER_SIZE);
		if (n == 0) {
			return null;
		}
		if (n != HEADER_SIZE) {
			throw new EOFException();
		}
		final ByteBuffer header = ByteBuffer.wrap(headerBytes);
		final int streamId = header.get();
		header.position(FRAME_SIZE_OFFSET);
		final int frameSize = header.getInt();
		// Read frame
		final byte[] frame = new byte[frameSize];
		ByteStreams.readFully(stream, frame);
		return new DockerLogMessage(streamId, ByteBuffer.wrap(frame));
	}

	@Override
	protected void finalize() throws Throwable {
		super.finalize();
		if (!closed) {
			close();
		}
	}

	public void close() throws IOException {
		closed = true;
		// Jersey will close the stream and release the connection after we read
		// all the data.
		// We cannot call the stream's close method because it an instance of
		// UncloseableInputStream,
		// where close is a no-op.
		copy(stream, new OutputStream() {
			/** Discards the specified byte. */
			@Override
			public void write(int b) {
			}

			/** Discards the specified byte array. */
			@Override
			public void write(byte[] b) {
				checkNotNull(b);
			}

			/** Discards the specified byte array. */
			@Override
			public void write(byte[] b, int off, int len) {
				checkNotNull(b);
			}

			@Override
			public String toString() {
				return "ByteStreams.nullOutputStream()";
			};
		});
	}

}
