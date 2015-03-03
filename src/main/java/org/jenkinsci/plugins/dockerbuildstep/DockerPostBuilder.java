package org.jenkinsci.plugins.dockerbuildstep;

import hudson.Extension;
import hudson.model.AbstractProject;
import hudson.model.FreeStyleProject;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Publisher;

@Extension
public class DockerPostBuilder extends BuildStepDescriptor<Publisher> {

	public DockerPostBuilder() {
		super(DockerPostBuildStep.class);
	}
	
	@Override
	public boolean isApplicable(Class<? extends AbstractProject> jobType) {
		return FreeStyleProject.class.equals(jobType);
	}

	@Override
	public String getDisplayName() {
		return "Stop Docker container";
	}
	
}
