package org.jenkinsci.plugins.dockerbuildstep;

import hudson.console.AnnotatedLargeText;
import hudson.model.Item;
import hudson.model.TaskListener;
import hudson.model.TaskThread;
import hudson.model.AbstractBuild;
import hudson.model.TaskAction;
import hudson.plugins.ansicolor.AnsiColorMap;
import hudson.plugins.ansicolor.AnsiColorBuildWrapper;
import hudson.security.ACL;
import hudson.security.Permission;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Serializable;

import jenkins.model.Jenkins;

import org.apache.commons.io.IOUtils;
import org.apache.commons.jelly.XMLOutput;
import org.jenkinsci.plugins.dockerbuildstep.log.DockerLogMessage;
import org.jenkinsci.plugins.dockerbuildstep.log.DockerLogStreamReader;

import com.github.dockerjava.api.DockerClient;
import com.google.common.base.Charsets;
import com.jcraft.jzlib.GZIPInputStream;

public class DockerLogAction extends TaskAction implements Serializable {
	private static final long serialVersionUID = 1L;

	public final AbstractBuild<?, ?> build;

	private final String containerId;

	public DockerLogAction(AbstractBuild<?, ?> build, String containerId) {
		super();
		this.build = build;
		this.containerId = containerId;
	}

	public DockerLogAction start() throws IOException {
		workerThread = new DockerLogWorkerThread(getLogFile());
		workerThread.start();

		return this;
	}

	public void stop() {
		workerThread.interrupt();
		workerThread = null;
	}

	public String getIconFileName() {
		return "terminal.png";
	}

	public String getDisplayName() {
		return "Docker Container Log";
	}

	public String getFullDisplayName() {
		return build.getFullDisplayName() + ' ' + getDisplayName();
	}

	public String getUrlName() {
		return "dockerlog";
	}

	public AbstractBuild<?, ?> getOwner() {
		return this.build;
	}

	@Override
	protected Permission getPermission() {
		return Item.READ;
	}

	@Override
	protected ACL getACL() {
		return build.getACL();
	}

	public String getBuildStatusUrl() {
		return build.getIconColor().getImage();
	}

	private File getLogFile() {
		return new File(build.getRootDir(), "docker.log");
	}

	@Override
	public AnnotatedLargeText obtainLog() {
		return new AnnotatedLargeText(getLogFile(), Charsets.UTF_8,
				!isLogUpdated(), this);

		// TODO Auto-generated method stub
		// return super.obtainLog();
	}

	public boolean isLogUpdated() {
		return workerThread != null;
	}

	public InputStream getLogInputStream() throws IOException {
		File logFile = getLogFile();

		if (logFile != null && logFile.exists()) {
			// Checking if a ".gz" file was return
			FileInputStream fis = new FileInputStream(logFile);
			if (logFile.getName().endsWith(".gz")) {
				return new GZIPInputStream(fis);
			} else {
				return fis;
			}
		}

		String message = "No such file: " + logFile;
		return new ByteArrayInputStream(message.getBytes(Charsets.UTF_8));
	}

	public void writeLogTo(long offset, XMLOutput out) throws IOException {
		try {
			obtainLog().writeHtmlTo(offset, out.asWriter());
		} catch (IOException e) {
			// try to fall back to the old getLogInputStream()
			// mainly to support .gz compressed files
			// In this case, console annotation handling will be turned off.
			InputStream input = getLogInputStream();
			try {
				IOUtils.copy(input, out.asWriter());
			} finally {
				IOUtils.closeQuietly(input);
			}
		}
	}

	public final class DockerLogWorkerThread extends TaskThread {

		protected DockerLogWorkerThread(File logFile) throws IOException {
			super(DockerLogAction.this, ListenerAndText.forFile(logFile,
					DockerLogAction.this));
		}

		@Override
		protected void perform(final TaskListener listener) throws Exception {
			DockerClient client = ((DockerBuilder.DescriptorImpl) Jenkins
					.getInstance().getDescriptor(DockerBuilder.class))
					.getDockerClient();
			InputStream is = client.attachContainerCmd(containerId)
					.withFollowStream().withStdOut().withStdErr().exec();
			DockerLogStreamReader reader = new DockerLogStreamReader(is);

			AnsiColorBuildWrapper ansiWrapper = new AnsiColorBuildWrapper(
					AnsiColorMap.GnomeTerminal.getName());
			OutputStream out = ansiWrapper.decorateLogger(build,
					listener.getLogger());
			OutputStreamWriter writer = new OutputStreamWriter(out,
					Charsets.UTF_8);

			try {
				while (!isInterrupted()) {
					process(reader, writer);
					Thread.sleep(2000);
				}
				process(reader, writer);
			} finally {
				if (writer != null) {
					writer.close();
				}
				if (reader != null) {
					reader.close();
				}
			}
		}

		private void process(DockerLogStreamReader ls, OutputStreamWriter w)
				throws IOException {
			DockerLogMessage m;
			while ((m = ls.nextMessage()) != null) {
				w.append(Charsets.UTF_8.decode(m.content()));
				w.flush();
			}
		}
	}
}
