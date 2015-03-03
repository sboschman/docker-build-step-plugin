package org.jenkinsci.plugins.dockerbuildstep;

import hudson.Launcher;
import hudson.model.Action;
import hudson.model.BuildListener;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Recorder;
import hudson.tasks.test.TestResultProjectAction;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;

import org.jenkinsci.plugins.dockerbuildstep.cmd.RemoveCommand;
import org.jenkinsci.plugins.dockerbuildstep.cmd.StopCommand;
import org.jenkinsci.plugins.dockerbuildstep.log.ConsoleLogger;
import org.kohsuke.stapler.DataBoundConstructor;

// logs, container stoppen, ...
public class DockerPostBuildStep extends Recorder {

    private final String containerIds;

    @DataBoundConstructor
    public DockerPostBuildStep(String containerIds) {
        this.containerIds = containerIds;
    }

    public String getContainerIds() {
        return containerIds;
    }
	

	
	public BuildStepMonitor getRequiredMonitorService() {
		return BuildStepMonitor.NONE;
	}

	@Override
	public boolean perform(AbstractBuild build, Launcher launcher,
			BuildListener listener) throws InterruptedException, IOException {
		ConsoleLogger clog = new ConsoleLogger(listener);
		
		DockerLogAction action = build.getAction(DockerLogAction.class);
		action.stop();

		StopCommand stopCommand = new StopCommand(containerIds);
		stopCommand.execute(build, clog);
		
		RemoveCommand removeCommand = new RemoveCommand(containerIds, true);
		removeCommand.execute(build, clog);
		
		return true;
	}

}
