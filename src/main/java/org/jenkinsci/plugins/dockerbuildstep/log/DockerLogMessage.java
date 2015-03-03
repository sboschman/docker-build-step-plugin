package org.jenkinsci.plugins.dockerbuildstep.log;

import static com.google.common.base.Preconditions.checkNotNull;

import java.nio.ByteBuffer;

/**
 * https://github.com/spotify/docker-client/blob/master/src/main/java/com/spotify/docker/client/LogMessage.java
 *
 */
public class DockerLogMessage {
	final DockerLogStream stream;
	final ByteBuffer content;

	public DockerLogMessage(final int streamId, final ByteBuffer content) {
		this(DockerLogStream.of(streamId), content);
	}

	public DockerLogMessage(final DockerLogStream stream, final ByteBuffer content) {
		this.stream = checkNotNull(stream, "stream");
		this.content = checkNotNull(content, "content");
	}

	public DockerLogStream stream() {
		return stream;
	}

	public ByteBuffer content() {
		return content.asReadOnlyBuffer();
	}
}
