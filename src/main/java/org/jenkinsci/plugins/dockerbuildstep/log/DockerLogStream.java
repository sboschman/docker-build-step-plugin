package org.jenkinsci.plugins.dockerbuildstep.log;

public enum DockerLogStream {
	STDIN(0),
	STDOUT(1),
	STDERR(2);
	
	private final int id;

	DockerLogStream(int id) {
		this.id = id;
	}

	public int id() {
		return id;
	}

	public static DockerLogStream of(final int id) {
		switch (id) {
		case 0:
			return STDIN;
		case 1:
			return STDOUT;
		case 2:
			return STDERR;
		default:
			throw new IllegalArgumentException();
		}
	}
}
