package org.eclipse.visualvm.launcher.java;

import java.io.IOException;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.jdt.junit.launcher.JUnitLaunchConfigurationDelegate;
import org.eclipse.jdt.launching.IVMRunner;
import org.eclipse.visualvm.launcher.api.VisualVMHelper;

public class VisualVMJUnitDelegate extends JUnitLaunchConfigurationDelegate {
	volatile private long usedId = -1;
	
	@Override
	public void launch(ILaunchConfiguration configuration, String mode,
			ILaunch launch, IProgressMonitor monitor) throws CoreException {
		usedId = VisualVMHelper.getNextID();
		super.launch(configuration, mode, launch, monitor);
	}

	@Override
	public String getVMArguments(ILaunchConfiguration configuration)
			throws CoreException {
		StringBuilder args = new StringBuilder(super.getVMArguments(configuration));
		for(String arg : VisualVMHelper.getJvmArgs(usedId)) {
			args.append(" ").append(arg);
		}
		return args.toString();
	}

	@Override
	public IVMRunner getVMRunner(ILaunchConfiguration configuration, String mode)
			throws CoreException {
		try {
			VisualVMHelper.openInVisualVM(usedId);
		} catch (IOException e) {
			VisualVMHelper.logException(e);
		}
		return super.getVMRunner(configuration, mode);
	}
}
