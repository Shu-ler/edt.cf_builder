package ru.yanygin.dt.cfbuilder.plugin.ui;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IWorkbenchWindow;

import com._1c.g5.v8.dt.export.ExportException;
import com._1c.g5.v8.dt.export.IExportService;
import com._1c.g5.v8.dt.export.IExportServiceRegistry;
import com._1c.g5.v8.dt.metadata.mdclass.Configuration;
import com._1c.g5.v8.dt.platform.version.Version;

public class BuildJob extends Job {

	private ProcessResult processResult = new ProcessResult(Status.OK_STATUS);
//	private IStatus status = Status.OK_STATUS;
//	private String processOutput;
	
	private TempDirs tempDirs;
	private ProjectContext projectContext;
	private IProgressMonitor buildMonitor;
	private IWorkbenchWindow windowInfo;

	public BuildJob(ProjectContext projectContext, IWorkbenchWindow windowInfo, TempDirs tempDirs) {
		//super(Messages.CfBuild_Build_Project_Name.replace("%projectName%", projectContext.getProjectName()));
		super(MessageFormat.format(Messages.CfBuild_Build_Project_Name, projectContext.getProjectName()));
		
		this.projectContext = projectContext;
		this.windowInfo = windowInfo;
		this.tempDirs = tempDirs;
	}

	@Override
	protected IStatus run(IProgressMonitor progressMonitor) {

		String buildResult;
		String buildMessage;
		
		this.buildMonitor = progressMonitor;

		Actions.createTempBase(projectContext, tempDirs, progressMonitor, processResult);
		Actions.loadConfigFromXml(projectContext, tempDirs, progressMonitor, processResult);
		Actions.dumpConfigToCf(projectContext, tempDirs, progressMonitor, processResult);

		if (processResult.statusIsOK()) {
			buildResult = Messages.CfBuild_Done;
			buildMessage = Messages.CfBuild_File_CF_Save_Is
							.concat(System.lineSeparator())
							.concat(System.lineSeparator())
							.concat(projectContext.getCfFullName());

			//Activator.log(Activator.createInfoStatus(Messages.CfBuild_End_Build.replace("%projectName%", projectContext.getProjectName())));
			Activator.log(Activator.createInfoStatus(MessageFormat.format(Messages.CfBuild_End_Build, projectContext.getProjectName())));
			
		} else if (processResult.statusIsCancel()) {
			//buildResult = Messages.CfBuild_Cancel.replace("%projectName%", projectContext.getProjectName());
			buildResult = MessageFormat.format(Messages.CfBuild_Cancel, projectContext.getProjectName());
			buildMessage = buildResult;
			
			Activator.log(Activator.createInfoStatus(buildResult));
			
		} else {
			buildResult = Messages.CfBuild_Abort;
			buildMessage = Messages.CfBuild_Abort;
			
			Activator.log(Activator.createErrorStatus(buildResult));

			if (!processResult.getOutput().isEmpty()) {
				Activator.log(Activator.createErrorStatus(processResult.getOutput()));
				buildMessage = buildMessage
							.concat(System.lineSeparator())
							.concat(System.lineSeparator())
							.concat(processResult.getOutput());
			}

			String outLog = Actions.readOutLogFile(tempDirs.getLogFilePath());
			if (!outLog.isEmpty()) {
				Activator.log(Activator.createErrorStatus(outLog));
				buildMessage = buildMessage
							.concat(System.lineSeparator())
							.concat(System.lineSeparator())
							.concat(outLog);
			}
		}

		buildMonitor.setTaskName(buildResult);

		Messages.showPostBuildMessage(windowInfo, buildResult, buildMessage);

		tempDirs.deleteDirs(buildMonitor);

		return processResult.getStatus();
	}

//	private boolean checkBuildState(String taskName) {
//		if (processResult.getStatus().isOK() & buildMonitor.isCanceled()) {
//			processResult = new ProcessResult(Status.CANCEL_STATUS);
//			//String infoMessage = Messages.CfBuild_Cancel.replace("%projectName%", projectContext.getProjectName());
//			String infoMessage = MessageFormat.format(Messages.CfBuild_Cancel, projectContext.getProjectName());
//			buildMonitor.setTaskName(infoMessage);
//			//Activator.log(Activator.createInfoStatus(infoMessage));
//			return false;
//		}
//
//		if (processResult.getStatus().isOK()) {
//			buildMonitor.beginTask(taskName, IProgressMonitor.UNKNOWN);
//			Activator.log(Activator.createInfoStatus(taskName));
//			return true;
//		}
//
//		return false;
//	}
//
//	private void createTempBase() {
//
//		if (!checkBuildState(Messages.CfBuild_Create_Base))
//			return;
//
//		Map<String, String> environmentVariables = new HashMap<>();
//		environmentVariables.put("PLATFORM_1C_PATH",	projectContext.getPlatformPath());
//		environmentVariables.put("BASE_1C_PATH",		tempDirs.getOnesBasePath());
//		environmentVariables.put("LOGFILE",				tempDirs.getLogFilePath());
//
//		String command = "%PLATFORM_1C_PATH% CREATEINFOBASE File=%BASE_1C_PATH% /Out %LOGFILE%";
//
//		//runCommand(command, environmentVariables, proces);
//		processResult = Actions.runCommand(command, environmentVariables);
//	}
//
//	private void loadConfig() {
//
//		if (!checkBuildState(Messages.CfBuild_Load_Config))
//			return;
//
//		Map<String, String> environmentVariables = new HashMap<>();
//		environmentVariables.put("PLATFORM_1C_PATH",	projectContext.getPlatformPath());
//		environmentVariables.put("BASE_1C_PATH",		tempDirs.getOnesBasePath());
//		environmentVariables.put("outXmlDir",			tempDirs.getXmlPath());
//		environmentVariables.put("LOGFILE",				tempDirs.getLogFilePath());
//
//		//String command = "%PLATFORM_1C_PATH% DESIGNER /F %BASE_1C_PATH% /LoadConfigFromFiles %outXmlDir% /UpdateDBCfg /Out %LOGFILE%";
//		String command = "%PLATFORM_1C_PATH% DESIGNER /F %BASE_1C_PATH% /LoadConfigFromFiles %outXmlDir% /Out %LOGFILE%";
//
//		//runCommand(command, environmentVariables, proces);
//		processResult = Actions.runCommand(command, environmentVariables);
//
//	}
//
//	private void dumpConfig() {
//
//		if (!checkBuildState(Messages.CfBuild_Dump_Config))
//			return;
//
//		File buildDir = new File(projectContext.getCfLocation());
//		if (!buildDir.exists()) {
//			buildDir.mkdir();
//		}
//
//		Map<String, String> environmentVariables = new HashMap<>();
//		environmentVariables.put("PLATFORM_1C_PATH",	projectContext.getPlatformPath());
//		environmentVariables.put("BASE_1C_PATH",		tempDirs.getOnesBasePath());
//		environmentVariables.put("cfName",				projectContext.getCfFullName());
//		environmentVariables.put("LOGFILE",				tempDirs.getLogFilePath());
//
//		String command = "%PLATFORM_1C_PATH% DESIGNER /F %BASE_1C_PATH% /DumpCfg \"%cfName%\" /Out %LOGFILE%";
//
//		//runCommand(command, environmentVariables, proces);
//		processResult = Actions.runCommand(command, environmentVariables);
//
//	}

	/*
	private void runCommand(String command, Map<String, String> environmentVariables) {

		processOutput = "";
		Process process;
		ProcessBuilder processBuilder = new ProcessBuilder();

		Map<String, String> env = processBuilder.environment();
		environmentVariables.forEach((k, v) -> env.put(k, v));

		processBuilder.command("cmd.exe", "/c", command);
		try {
			process = processBuilder.start();

			BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), "windows-1251"));// cp866
																														// UTF-8

			String line;
			while ((line = reader.readLine()) != null) {
				processOutput = processOutput.concat(System.lineSeparator()).concat(line);
			}

			int exitCode = process.waitFor();
			if (exitCode != 0) {
				status = new Status(Status.ERROR, Activator.PLUGIN_ID, Messages.CfBuild_Abort);
				Activator.log(Activator.createErrorStatus(Messages.CfBuild_Abort));
			}

		} catch (IOException | InterruptedException e) {
			status = new Status(Status.ERROR, Activator.PLUGIN_ID, Messages.CfBuild_Unknown_Error);
			Activator.log(Activator.createErrorStatus(Messages.CfBuild_Unknown_Error.concat(processOutput), e));
		}

	}

	private String readOutLogFile(String fileName) {
		String contents = "";

		if (new File(fileName).exists()) {
			try {
				contents = new String(Files.readAllBytes(Paths.get(fileName)), Charset.forName("Windows-1251"));
			} catch (IOException e) {
				status = new Status(Status.ERROR, Activator.PLUGIN_ID, Messages.CfBuild_Unknown_Error);
				Activator.log(Activator.createErrorStatus(e.getLocalizedMessage(), e));
			}
		}

		return contents;
	}
	*/

}
