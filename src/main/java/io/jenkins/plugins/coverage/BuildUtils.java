package io.jenkins.plugins.coverage;

import hudson.model.Run;

public class BuildUtils {
	public static Run<?, ?> getPreviousNotFailedCompletedBuild(Run<?, ?> b) {
		while (true) {
			b = b.getPreviousNotFailedBuild();
			if (b == null) {
				return null;
			}
			if (!b.isBuilding()) {
				return b;
			}
		}
	}
}
