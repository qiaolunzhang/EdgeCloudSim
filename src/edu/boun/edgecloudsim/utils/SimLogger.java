/*
 * Title:        EdgeCloudSim - Simulation Logger
 * 
 * Description: 
 * SimLogger is responsible for storing simulation events/results
 * in to the files in a specific format.
 * Format is decided in a way to use results in matlab efficiently.
 * If you need more results or another file format, you should modify
 * this class.
 * 
 * Licence:      GPL - http://www.gnu.org/copyleft/gpl.html
 * Copyright (c) 2017, Bogazici University, Istanbul, Turkey
 */

package edu.boun.edgecloudsim.utils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;

import edu.boun.edgecloudsim.core.SimManager;
import edu.boun.edgecloudsim.core.SimSettings;
import edu.boun.edgecloudsim.core.KernelBasedApplicationStatus;
import edu.boun.edgecloudsim.core.SimSettings.NETWORK_DELAY_TYPES;
import edu.boun.edgecloudsim.utils.SimLogger.NETWORK_ERRORS;

public class SimLogger {
	public static enum KERNEL_STATUS {
		CREATED, UPLOADING, PROCESSING, DOWNLOADING, COMLETED, REJECTED_DUE_TO_VM_CAPACITY, REJECTED_DUE_TO_BANDWIDTH, UNFINISHED_DUE_TO_BANDWIDTH, UNFINISHED_DUE_TO_MOBILITY
	}
	
	public static enum NETWORK_ERRORS {
		LAN_ERROR, MAN_ERROR, WAN_ERROR, NONE
	}

	private static boolean fileLogEnabled;
	private static boolean printLogEnabled;
	private String filePrefix;
	private String outputFolder;
	private Map<Integer, LogItem> kernelMap;
	private LinkedList<VmLoadLogItem> vmLoadList;

	private static SimLogger singleton = new SimLogger();

	/*
	 * A private Constructor prevents any other class from instantiating.
	 */
	private SimLogger() {
		fileLogEnabled = false;
		printLogEnabled = false;
	}

	/* Static 'instance' method */
	public static SimLogger getInstance() {
		return singleton;
	}

	public static void enableFileLog() {
		fileLogEnabled = true;
	}

	public static void enablePrintLog() {
		printLogEnabled = true;
	}

	public static boolean isFileLogEnabled() {
		return fileLogEnabled;
	}

	public static void disablePrintLog() {
		printLogEnabled = false;
	}

	private void appendToFile(BufferedWriter bw, String line) throws IOException {
		bw.write(line);
		bw.newLine();
	}

	public static void printLine(String msg) {
		if (printLogEnabled)
			System.out.println(msg);
	}

	public static void print(String msg) {
		if (printLogEnabled)
			System.out.print(msg);
	}

	public void simStarted(String outFolder, String fileName) {
		filePrefix = fileName;
		outputFolder = outFolder;
		kernelMap = new HashMap<Integer, LogItem>();
		vmLoadList = new LinkedList<VmLoadLogItem>();
	}

	public void addLog(int cloudletId, int kernelType, int kerneLength, int kernelInputType,
			int kernelOutputSize, int kernelId) {
		// printLine(taskId+"->"+taskStartTime);
		kernelMap.put(cloudletId, new LogItem(kernelType, kerneLength, kernelInputType, kernelOutputSize, kernelId));
	}

	public void kernelStarted(int kernelId, double time) {
		kernelMap.get(kernelId).kernelStarted(time);
	}

	public void setUploadDelay(int taskId, double delay, NETWORK_DELAY_TYPES delayType) {
		kernelMap.get(taskId).setUploadDelay(delay, delayType);
	}

	public void setDownloadDelay(int taskId, double delay, NETWORK_DELAY_TYPES delayType) {
		kernelMap.get(taskId).setDownloadDelay(delay, delayType);
	}
	
	public void kernelAssigned(int kernelId, int datacenterId, int hostId, int vmId, int vmType) {
		kernelMap.get(kernelId).kernelAssigned(datacenterId, hostId, vmId, vmType);
	}

	public void kernelExecuted(int kernelId) {
		kernelMap.get(kernelId).kernelExecuted();
	}

	public void kernelEnded(int kernelId, double time) {
		kernelMap.get(kernelId).kernelEnded(time);
	}

	public void rejectedDueToVMCapacity(int taskId, double time, int vmType) {
		kernelMap.get(taskId).kernelRejectedDueToVMCapacity(time, vmType);
	}

	public void rejectedDueToBandwidth(int taskId, double time, int vmType, NETWORK_DELAY_TYPES delayType) {
		kernelMap.get(taskId).kernelRejectedDueToBandwidth(time, vmType, delayType);
	}

	public void failedDueToBandwidth(int taskId, double time, NETWORK_DELAY_TYPES delayType) {
		kernelMap.get(taskId).kernelFailedDueToBandwidth(time, delayType);
	}

	public void failedDueToMobility(int taskId, double time) {
		kernelMap.get(taskId).kernelFailedDueToMobility(time);
	}

	public void addVmUtilizationLog(double time, double loadOnEdge, double loadOnCloud, double loadOnMobile) {
		vmLoadList.add(new VmLoadLogItem(time, loadOnEdge, loadOnCloud, loadOnMobile));
	}

	public void simStopped() throws IOException {
		int numOfAppTypes = SimSettings.getInstance().getApplicationLookUpTable().length;
		int numKernelBasedApplication = 0;

		File successFile = null, failFile = null, vmLoadFile = null, locationFile = null;
		FileWriter successFW = null, failFW = null, vmLoadFW = null, locationFW = null;
		BufferedWriter successBW = null, failBW = null, vmLoadBW = null, locationBW = null;

		// Save generic results to file for each app type. last index is average
		// of all app types
		File[] genericFiles = new File[numOfAppTypes + 1];
		FileWriter[] genericFWs = new FileWriter[numOfAppTypes + 1];
		BufferedWriter[] genericBWs = new BufferedWriter[numOfAppTypes + 1];

		// extract following values for each app type. last index is average of
		// all app types
		int[] uncompletedApplication = new int[numOfAppTypes + 1];
		int[] uncompletedApplicationOnCloud = new int[numOfAppTypes + 1];
		int[] uncompletedApplicationOnEdge = new int[numOfAppTypes + 1];
		int[] uncompletedapplicationOnMobile = new int[numOfAppTypes + 1];
		
		int[] uncompletedKernelInKBApp = new int[numOfAppTypes + 1];
		int[] uncompletedKernelInKBAppOnCloud = new int[numOfAppTypes + 1];
		int[] uncompletedKernelInKBAppOnEdge = new int[numOfAppTypes + 1];
		int[] uncompletedKernelInKBAppOnMobile = new int[numOfAppTypes + 1];

		int[] completedApplication = new int[numOfAppTypes + 1];
		int[] completedAppOnCloud = new int[numOfAppTypes + 1];
		int[] completedAppOnEdge = new int[numOfAppTypes + 1];
		int[] completedAppOnMobile = new int[numOfAppTypes + 1];

		int[] completedKernelInKBApp = new int[numOfAppTypes + 1];
		int[] completedKernelInKBAppOnCloud = new int[numOfAppTypes + 1];
		int[] completedKernelInKBAppOnEdge = new int[numOfAppTypes + 1];
		int[] completedKernelInKBAppOnMobile = new int[numOfAppTypes + 1];

		int[] failedApplication = new int[numOfAppTypes + 1];
		int[] failedAppOnCloud = new int[numOfAppTypes + 1];
		int[] failedAppOnEdge = new int[numOfAppTypes + 1];
		int[] failedAppOnMobile = new int[numOfAppTypes + 1];

		int[] failedKernelInKBApp = new int[numOfAppTypes + 1];
		int[] failedKernelInKBAppOnCloud = new int[numOfAppTypes + 1];
		int[] failedKernelInKBAppOnEdge = new int[numOfAppTypes + 1];
		int[] failedKernelInKBAppOnMobile = new int[numOfAppTypes + 1];

		double[] networkDelay = new double[numOfAppTypes + 1];
		double[] wanDelay = new double[numOfAppTypes + 1];
		double[] manDelay = new double[numOfAppTypes + 1];
		double[] lanDelay = new double[numOfAppTypes + 1];
		
		double[] wanUsage = new double[numOfAppTypes + 1];
		double[] manUsage = new double[numOfAppTypes + 1];
		double[] lanUsage = new double[numOfAppTypes + 1];

		double[] serviceTime = new double[numOfAppTypes + 1];
		double[] serviceTimeOnCloud = new double[numOfAppTypes + 1];
		double[] serviceTimeOnEdge = new double[numOfAppTypes + 1];
		double[] serviceTimeOnMobile = new double[numOfAppTypes + 1];

		double[] processingTime = new double[numOfAppTypes + 1];
		double[] processingTimeOnCloud = new double[numOfAppTypes + 1];
		double[] processingTimeOnEdge = new double[numOfAppTypes + 1];
		double[] processingTimeOnMobile = new double[numOfAppTypes + 1];

		int[] failedAppDueToVmCapacity = new int[numOfAppTypes + 1];
		int[] failedAppDueToVmCapacityOnCloud = new int[numOfAppTypes + 1];
		int[] failedAppDueToVmCapacityOnEdge = new int[numOfAppTypes + 1];
		int[] failedAppDueToVmCapacityOnMobile = new int[numOfAppTypes + 1];

		int[] failedKernelInKBAppDueToVmCapacity = new int[numOfAppTypes + 1];
		int[] failedKernelInKBAppDueToVmCapacityOnCloud = new int[numOfAppTypes + 1];
		int[] failedKernelInKBAppDueToVmCapacityOnEdge = new int[numOfAppTypes + 1];
		int[] failedKernelInKBAppDueToVmCapacityOnMobile = new int[numOfAppTypes + 1];
		
		double[] cost = new double[numOfAppTypes + 1];
		int[] failedAppDuetoBw = new int[numOfAppTypes + 1];
		int[] failedAppDuetoLanBw = new int[numOfAppTypes + 1];
		int[] failedAppDuetoManBw = new int[numOfAppTypes + 1];
		int[] failedAppDuetoWanBw = new int[numOfAppTypes + 1];
		int[] failedAppDuetoMobility = new int[numOfAppTypes + 1];

		int[] failedKernelInKBAppDuetoBw = new int[numOfAppTypes + 1];
		int[] failedKernelInKBAppDuetoLanBw = new int[numOfAppTypes + 1];
		int[] failedKernelInKBAppDuetoManBw = new int[numOfAppTypes + 1];
		int[] failedKernelInKBAppDuetoWanBw = new int[numOfAppTypes + 1];
		int[] failedKernelInKBAppDuetoMobility = new int[numOfAppTypes + 1];


		// open all files and prepare them for write
		if (fileLogEnabled) {
			if (SimSettings.getInstance().getDeepFileLoggingEnabled()) {
				successFile = new File(outputFolder, filePrefix + "_SUCCESS.log");
				successFW = new FileWriter(successFile, true);
				successBW = new BufferedWriter(successFW);

				failFile = new File(outputFolder, filePrefix + "_FAIL.log");
				failFW = new FileWriter(failFile, true);
				failBW = new BufferedWriter(failFW);
			}

			vmLoadFile = new File(outputFolder, filePrefix + "_VM_LOAD.log");
			vmLoadFW = new FileWriter(vmLoadFile, true);
			vmLoadBW = new BufferedWriter(vmLoadFW);

			locationFile = new File(outputFolder, filePrefix + "_LOCATION.log");
			locationFW = new FileWriter(locationFile, true);
			locationBW = new BufferedWriter(locationFW);

			for (int i = 0; i < numOfAppTypes + 1; i++) {
				String fileName = "ALL_APPS_GENERIC.log";

				if (i < numOfAppTypes) {
					// if related app is not used in this simulation, just
					// discard it
					if (SimSettings.getInstance().getApplicationLookUpTable()[i][0] == 0)
						continue;

					fileName = SimSettings.getInstance().getApplicationName(i) + "_GENERIC.log";
				}

				genericFiles[i] = new File(outputFolder, filePrefix + "_" + fileName);
				genericFWs[i] = new FileWriter(genericFiles[i], true);
				genericBWs[i] = new BufferedWriter(genericFWs[i]);
				appendToFile(genericBWs[i], "#auto generated file!");
			}

			if (SimSettings.getInstance().getDeepFileLoggingEnabled()) {
				appendToFile(successBW, "#auto generated file!");
				appendToFile(failBW, "#auto generated file!");
			}

			appendToFile(vmLoadBW, "#auto generated file!");
			appendToFile(locationBW, "#auto generated file!");
		}

		KernelBasedApplicationStatus.getInstance().checkAllSubmittedAndSetStatus();
		//System.out.println("Number of kernel-based app is: " + KernelBasedApplicationStatus.getInstance().getNumKernelBasedApplication());
		//int totalKernelNum = 0;

		// extract the result of each task and write it to the file if required
		for (Map.Entry<Integer, LogItem> entry : kernelMap.entrySet()) {
			Integer key = entry.getKey();
			LogItem value = entry.getValue();
			//totalKernelNum++;
			
			if (value.isInWarmUpPeriod())
				continue;

			int kernelId = value.getKernelId();

			// is kernel in kernel-based application with multiple kernels
			if (KernelBasedApplicationStatus.getInstance().checkKernelInKBApp(kernelId)) {
				int status = KernelBasedApplicationStatus.getInstance().getKernelBasedAppFinalStatus(kernelId);
				if (status == 2) {
					continue;
				}
				
				if (value.getStatus() == SimLogger.KERNEL_STATUS.COMLETED) {
					continue;
				}
				else if(value.getStatus() == SimLogger.KERNEL_STATUS.CREATED ||
						value.getStatus() == SimLogger.KERNEL_STATUS.UPLOADING ||
						value.getStatus() == SimLogger.KERNEL_STATUS.PROCESSING ||
						value.getStatus() == SimLogger.KERNEL_STATUS.DOWNLOADING)
				{
					KernelBasedApplicationStatus.getInstance().setKernelBasedAppFinalStatus(kernelId, 1);
				}
				else {
					KernelBasedApplicationStatus.getInstance().setKernelBasedAppFinalStatus(kernelId, 2);
				}
			} 

			else {

			if (value.getStatus() == SimLogger.KERNEL_STATUS.COMLETED) {
				completedApplication[value.getKernelType()]++;

				if (value.getVmType() == SimSettings.VM_TYPES.CLOUD_VM.ordinal())
					completedAppOnCloud[value.getKernelType()]++;
				else if (value.getVmType() == SimSettings.VM_TYPES.MOBILE_VM.ordinal())
					completedAppOnMobile[value.getKernelType()]++;
				else
					completedAppOnEdge[value.getKernelType()]++;
			}
			else if(value.getStatus() == SimLogger.KERNEL_STATUS.CREATED ||
					value.getStatus() == SimLogger.KERNEL_STATUS.UPLOADING ||
					value.getStatus() == SimLogger.KERNEL_STATUS.PROCESSING ||
					value.getStatus() == SimLogger.KERNEL_STATUS.DOWNLOADING)
			{
				uncompletedApplication[value.getKernelType()]++;
				if (value.getVmType() == SimSettings.VM_TYPES.CLOUD_VM.ordinal())
					uncompletedApplicationOnCloud[value.getKernelType()]++;
				else if (value.getVmType() == SimSettings.VM_TYPES.MOBILE_VM.ordinal())
					uncompletedapplicationOnMobile[value.getKernelType()]++;
				else
					uncompletedApplicationOnEdge[value.getKernelType()]++;
			}
			else {
				failedApplication[value.getKernelType()]++;

				if (value.getVmType() == SimSettings.VM_TYPES.CLOUD_VM.ordinal())
					failedAppOnCloud[value.getKernelType()]++;
				else if (value.getVmType() == SimSettings.VM_TYPES.MOBILE_VM.ordinal())
					failedAppOnMobile[value.getKernelType()]++;
				else
					failedAppOnEdge[value.getKernelType()]++;
			}

			if (value.getStatus() == SimLogger.KERNEL_STATUS.COMLETED) {
				cost[value.getKernelType()] += value.getCost();
				serviceTime[value.getKernelType()] += value.getServiceTime();
				networkDelay[value.getKernelType()] += value.getNetworkDelay();
				processingTime[value.getKernelType()] += (value.getServiceTime() - value.getNetworkDelay());
				
				if(value.getNetworkDelay(NETWORK_DELAY_TYPES.WLAN_DELAY) != 0) {
					lanUsage[value.getKernelType()]++;
					lanDelay[value.getKernelType()] += value.getNetworkDelay(NETWORK_DELAY_TYPES.WLAN_DELAY);
				}
				if(value.getNetworkDelay(NETWORK_DELAY_TYPES.MAN_DELAY) != 0) {
					manUsage[value.getKernelType()]++;
					manDelay[value.getKernelType()] += value.getNetworkDelay(NETWORK_DELAY_TYPES.MAN_DELAY);
				}
				if(value.getNetworkDelay(NETWORK_DELAY_TYPES.WAN_DELAY) != 0) {
					wanUsage[value.getKernelType()]++;
					wanDelay[value.getKernelType()] += value.getNetworkDelay(NETWORK_DELAY_TYPES.WAN_DELAY);
				}

				
				if (value.getVmType() == SimSettings.VM_TYPES.CLOUD_VM.ordinal()) {
					serviceTimeOnCloud[value.getKernelType()] += value.getServiceTime();
					processingTimeOnCloud[value.getKernelType()] += (value.getServiceTime() - value.getNetworkDelay());
				}
				else if (value.getVmType() == SimSettings.VM_TYPES.MOBILE_VM.ordinal()) {
					serviceTimeOnMobile[value.getKernelType()] += value.getServiceTime();
					processingTimeOnMobile[value.getKernelType()] += value.getServiceTime();
				}
				else {
					serviceTimeOnEdge[value.getKernelType()] += value.getServiceTime();
					processingTimeOnEdge[value.getKernelType()] += (value.getServiceTime() - value.getNetworkDelay());
				}

				if (fileLogEnabled && SimSettings.getInstance().getDeepFileLoggingEnabled())
					appendToFile(successBW, value.toString(key));
			} else if (value.getStatus() == SimLogger.KERNEL_STATUS.REJECTED_DUE_TO_VM_CAPACITY) {
				failedAppDueToVmCapacity[value.getKernelType()]++;
				
				if (value.getVmType() == SimSettings.VM_TYPES.CLOUD_VM.ordinal())
					failedAppDueToVmCapacityOnCloud[value.getKernelType()]++;
				else if (value.getVmType() == SimSettings.VM_TYPES.MOBILE_VM.ordinal())
					failedAppDueToVmCapacityOnMobile[value.getKernelType()]++;
				else
					failedAppDueToVmCapacityOnEdge[value.getKernelType()]++;
				
				if (fileLogEnabled && SimSettings.getInstance().getDeepFileLoggingEnabled())
					appendToFile(failBW, value.toString(key));
			} else if (value.getStatus() == SimLogger.KERNEL_STATUS.REJECTED_DUE_TO_BANDWIDTH
					|| value.getStatus() == SimLogger.KERNEL_STATUS.UNFINISHED_DUE_TO_BANDWIDTH) {
				failedAppDuetoBw[value.getKernelType()]++;
				if (value.getNetworkError() == NETWORK_ERRORS.LAN_ERROR)
					failedAppDuetoLanBw[value.getKernelType()]++;
				else if (value.getNetworkError() == NETWORK_ERRORS.MAN_ERROR)
					failedAppDuetoManBw[value.getKernelType()]++;
				else if (value.getNetworkError() == NETWORK_ERRORS.WAN_ERROR)
					failedAppDuetoWanBw[value.getKernelType()]++;

				if (fileLogEnabled && SimSettings.getInstance().getDeepFileLoggingEnabled())
					appendToFile(failBW, value.toString(key));
			} else if (value.getStatus() == SimLogger.KERNEL_STATUS.UNFINISHED_DUE_TO_MOBILITY) {
				failedAppDuetoMobility[value.getKernelType()]++;
				if (fileLogEnabled && SimSettings.getInstance().getDeepFileLoggingEnabled())
					appendToFile(failBW, value.toString(key));
			}
			}
		}
		
		//System.out.println("Total number of kernel logged is " + totalKernelNum);
		

		
		for (Map.Entry<Integer, LogItem> entry : kernelMap.entrySet()) {
			Integer key = entry.getKey();
			LogItem value = entry.getValue();
			
			if (value.isInWarmUpPeriod())
				continue;

			int kernelId = value.getKernelId();

			if (KernelBasedApplicationStatus.getInstance().checkKernelInKBApp(kernelId)) {
					//System.out.println("It's a sub-task");
				int status = KernelBasedApplicationStatus.getInstance().getKernelBasedAppFinalStatus(kernelId);
				
				if (KernelBasedApplicationStatus.getInstance().checkFinalStatusLogged(kernelId)) {
					numKernelBasedApplication++;
					if (status == 0) {
						completedApplication[value.getKernelType()]++;
						KernelBasedApplicationStatus.getInstance().setFinalStatusLogged(kernelId);
					}
					else if (status == 1) {
						uncompletedApplication[value.getKernelType()]++;
						KernelBasedApplicationStatus.getInstance().setFinalStatusLogged(kernelId);
					} else {
						failedApplication[value.getKernelType()]++;
						KernelBasedApplicationStatus.getInstance().setFinalStatusLogged(kernelId);
					}
				}
					
				if (value.getStatus() == SimLogger.KERNEL_STATUS.COMLETED) {
					completedKernelInKBApp[value.getKernelType()]++;

					if (value.getVmType() == SimSettings.VM_TYPES.CLOUD_VM.ordinal())
						completedKernelInKBAppOnCloud[value.getKernelType()]++;
					else if (value.getVmType() == SimSettings.VM_TYPES.MOBILE_VM.ordinal())
						completedKernelInKBAppOnMobile[value.getKernelType()]++;
					else
						completedKernelInKBAppOnEdge[value.getKernelType()]++;
				}
				else if(value.getStatus() == SimLogger.KERNEL_STATUS.CREATED ||
						value.getStatus() == SimLogger.KERNEL_STATUS.UPLOADING ||
						value.getStatus() == SimLogger.KERNEL_STATUS.PROCESSING ||
						value.getStatus() == SimLogger.KERNEL_STATUS.DOWNLOADING)
				{
					uncompletedKernelInKBApp[value.getKernelType()]++;
					if (value.getVmType() == SimSettings.VM_TYPES.CLOUD_VM.ordinal())
						uncompletedKernelInKBAppOnCloud[value.getKernelType()]++;
					else if (value.getVmType() == SimSettings.VM_TYPES.MOBILE_VM.ordinal())
						uncompletedKernelInKBAppOnMobile[value.getKernelType()]++;
					else
						uncompletedKernelInKBAppOnEdge[value.getKernelType()]++;
				}
				else {
					failedKernelInKBApp[value.getKernelType()]++;

					if (value.getVmType() == SimSettings.VM_TYPES.CLOUD_VM.ordinal())
						failedKernelInKBAppOnCloud[value.getKernelType()]++;
					else if (value.getVmType() == SimSettings.VM_TYPES.MOBILE_VM.ordinal())
						failedKernelInKBAppOnMobile[value.getKernelType()]++;
					else
						failedKernelInKBAppOnEdge[value.getKernelType()]++;
				}

				if (status == 0) {
					cost[value.getKernelType()] += value.getCost();
					serviceTime[value.getKernelType()] += value.getServiceTime();
					networkDelay[value.getKernelType()] += value.getNetworkDelay();
					processingTime[value.getKernelType()] += (value.getServiceTime() - value.getNetworkDelay());
					
					if(value.getNetworkDelay(NETWORK_DELAY_TYPES.WLAN_DELAY) != 0) {
						lanUsage[value.getKernelType()]++;
						lanDelay[value.getKernelType()] += value.getNetworkDelay(NETWORK_DELAY_TYPES.WLAN_DELAY);
					}
					if(value.getNetworkDelay(NETWORK_DELAY_TYPES.MAN_DELAY) != 0) {
						manUsage[value.getKernelType()]++;
						manDelay[value.getKernelType()] += value.getNetworkDelay(NETWORK_DELAY_TYPES.MAN_DELAY);
					}
					if(value.getNetworkDelay(NETWORK_DELAY_TYPES.WAN_DELAY) != 0) {
						wanUsage[value.getKernelType()]++;
						wanDelay[value.getKernelType()] += value.getNetworkDelay(NETWORK_DELAY_TYPES.WAN_DELAY);
					}

					
					if (value.getVmType() == SimSettings.VM_TYPES.CLOUD_VM.ordinal()) {
						serviceTimeOnCloud[value.getKernelType()] += value.getServiceTime();
						processingTimeOnCloud[value.getKernelType()] += (value.getServiceTime() - value.getNetworkDelay());
					}
					else if (value.getVmType() == SimSettings.VM_TYPES.MOBILE_VM.ordinal()) {
						serviceTimeOnMobile[value.getKernelType()] += value.getServiceTime();
						processingTimeOnMobile[value.getKernelType()] += value.getServiceTime();
					}
					else {
						serviceTimeOnEdge[value.getKernelType()] += value.getServiceTime();
						processingTimeOnEdge[value.getKernelType()] += (value.getServiceTime() - value.getNetworkDelay());
					}

					if (fileLogEnabled && SimSettings.getInstance().getDeepFileLoggingEnabled())
						appendToFile(successBW, value.toString(key));
				} else if (value.getStatus() == SimLogger.KERNEL_STATUS.REJECTED_DUE_TO_VM_CAPACITY) {
					failedKernelInKBAppDueToVmCapacity[value.getKernelType()]++;
					
					if (value.getVmType() == SimSettings.VM_TYPES.CLOUD_VM.ordinal())
						failedKernelInKBAppDueToVmCapacityOnCloud[value.getKernelType()]++;
					else if (value.getVmType() == SimSettings.VM_TYPES.MOBILE_VM.ordinal())
						failedKernelInKBAppDueToVmCapacityOnMobile[value.getKernelType()]++;
					else
						failedKernelInKBAppDueToVmCapacityOnEdge[value.getKernelType()]++;
					
					if (fileLogEnabled && SimSettings.getInstance().getDeepFileLoggingEnabled())
						appendToFile(failBW, value.toString(key));
				} else if (value.getStatus() == SimLogger.KERNEL_STATUS.REJECTED_DUE_TO_BANDWIDTH
						|| value.getStatus() == SimLogger.KERNEL_STATUS.UNFINISHED_DUE_TO_BANDWIDTH) {
					failedKernelInKBAppDuetoBw[value.getKernelType()]++;
					if (value.getNetworkError() == NETWORK_ERRORS.LAN_ERROR)
						failedKernelInKBAppDuetoLanBw[value.getKernelType()]++;
					else if (value.getNetworkError() == NETWORK_ERRORS.MAN_ERROR)
						failedKernelInKBAppDuetoManBw[value.getKernelType()]++;
					else if (value.getNetworkError() == NETWORK_ERRORS.WAN_ERROR)
						failedKernelInKBAppDuetoWanBw[value.getKernelType()]++;

					if (fileLogEnabled && SimSettings.getInstance().getDeepFileLoggingEnabled())
						appendToFile(failBW, value.toString(key));
				} else if (value.getStatus() == SimLogger.KERNEL_STATUS.UNFINISHED_DUE_TO_MOBILITY) {
					failedKernelInKBAppDuetoMobility[value.getKernelType()]++;
					if (fileLogEnabled && SimSettings.getInstance().getDeepFileLoggingEnabled())
						appendToFile(failBW, value.toString(key));
				}
				}
		}
		

		// calculate total values
		uncompletedApplication[numOfAppTypes] = IntStream.of(uncompletedApplication).sum();
		uncompletedApplicationOnCloud[numOfAppTypes] = IntStream.of(uncompletedApplicationOnCloud).sum();
		uncompletedApplicationOnEdge[numOfAppTypes] = IntStream.of(uncompletedApplicationOnEdge).sum();
		uncompletedapplicationOnMobile[numOfAppTypes] = IntStream.of(uncompletedapplicationOnMobile).sum();

		uncompletedKernelInKBApp[numOfAppTypes] = IntStream.of(uncompletedKernelInKBApp).sum();
		uncompletedKernelInKBAppOnCloud[numOfAppTypes] = IntStream.of(uncompletedKernelInKBAppOnCloud).sum();
		uncompletedKernelInKBAppOnEdge[numOfAppTypes] = IntStream.of(uncompletedKernelInKBAppOnEdge).sum();
		uncompletedKernelInKBAppOnMobile[numOfAppTypes] = IntStream.of(uncompletedKernelInKBAppOnMobile).sum();


		completedApplication[numOfAppTypes] = IntStream.of(completedApplication).sum();
		completedAppOnCloud[numOfAppTypes] = IntStream.of(completedAppOnCloud).sum();
		completedAppOnEdge[numOfAppTypes] = IntStream.of(completedAppOnEdge).sum();
		completedAppOnMobile[numOfAppTypes] = IntStream.of(completedAppOnMobile).sum();

		completedKernelInKBApp[numOfAppTypes] = IntStream.of(completedKernelInKBApp).sum();
		completedKernelInKBAppOnCloud[numOfAppTypes] = IntStream.of(completedKernelInKBAppOnCloud).sum();
		completedKernelInKBAppOnEdge[numOfAppTypes] = IntStream.of(completedKernelInKBAppOnEdge).sum();
		completedKernelInKBAppOnMobile[numOfAppTypes] = IntStream.of(completedKernelInKBAppOnMobile).sum();


		failedApplication[numOfAppTypes] = IntStream.of(failedApplication).sum();
		failedAppOnCloud[numOfAppTypes] = IntStream.of(failedAppOnCloud).sum();
		failedAppOnEdge[numOfAppTypes] = IntStream.of(failedAppOnEdge).sum();
		failedAppOnMobile[numOfAppTypes] = IntStream.of(failedAppOnMobile).sum();

		failedKernelInKBApp[numOfAppTypes] = IntStream.of(failedKernelInKBApp).sum();
		failedKernelInKBAppOnCloud[numOfAppTypes] = IntStream.of(failedKernelInKBAppOnCloud).sum();
		failedKernelInKBAppOnEdge[numOfAppTypes] = IntStream.of(failedKernelInKBAppOnEdge).sum();
		failedKernelInKBAppOnMobile[numOfAppTypes] = IntStream.of(failedKernelInKBAppOnMobile).sum();


		networkDelay[numOfAppTypes] = DoubleStream.of(networkDelay).sum();
		lanDelay[numOfAppTypes] = DoubleStream.of(lanDelay).sum();
		manDelay[numOfAppTypes] = DoubleStream.of(manDelay).sum();
		wanDelay[numOfAppTypes] = DoubleStream.of(wanDelay).sum();
		
		lanUsage[numOfAppTypes] = DoubleStream.of(lanUsage).sum();
		manUsage[numOfAppTypes] = DoubleStream.of(manUsage).sum();
		wanUsage[numOfAppTypes] = DoubleStream.of(wanUsage).sum();

		serviceTime[numOfAppTypes] = DoubleStream.of(serviceTime).sum();
		serviceTimeOnCloud[numOfAppTypes] = DoubleStream.of(serviceTimeOnCloud).sum();
		serviceTimeOnEdge[numOfAppTypes] = DoubleStream.of(serviceTimeOnEdge).sum();
		serviceTimeOnMobile[numOfAppTypes] = DoubleStream.of(serviceTimeOnMobile).sum();

		processingTime[numOfAppTypes] = DoubleStream.of(processingTime).sum();
		processingTimeOnCloud[numOfAppTypes] = DoubleStream.of(processingTimeOnCloud).sum();
		processingTimeOnEdge[numOfAppTypes] = DoubleStream.of(processingTimeOnEdge).sum();
		processingTimeOnMobile[numOfAppTypes] = DoubleStream.of(processingTimeOnMobile).sum();

		failedAppDueToVmCapacity[numOfAppTypes] = IntStream.of(failedAppDueToVmCapacity).sum();
		failedAppDueToVmCapacityOnCloud[numOfAppTypes] = IntStream.of(failedAppDueToVmCapacityOnCloud).sum();
		failedAppDueToVmCapacityOnEdge[numOfAppTypes] = IntStream.of(failedAppDueToVmCapacityOnEdge).sum();
		failedAppDueToVmCapacityOnMobile[numOfAppTypes] = IntStream.of(failedAppDueToVmCapacityOnMobile).sum();
		
		
		failedKernelInKBAppDueToVmCapacity[numOfAppTypes] = IntStream.of(failedKernelInKBAppDueToVmCapacity).sum();
		failedKernelInKBAppDueToVmCapacityOnCloud[numOfAppTypes] = IntStream.of(failedKernelInKBAppDueToVmCapacityOnCloud).sum();
		failedKernelInKBAppDueToVmCapacityOnEdge[numOfAppTypes] = IntStream.of(failedKernelInKBAppDueToVmCapacityOnEdge).sum();
		failedKernelInKBAppDueToVmCapacityOnMobile[numOfAppTypes] = IntStream.of(failedKernelInKBAppDueToVmCapacityOnMobile).sum();
		
	
		cost[numOfAppTypes] = DoubleStream.of(cost).sum();
		failedAppDuetoBw[numOfAppTypes] = IntStream.of(failedAppDuetoBw).sum();
		failedAppDuetoWanBw[numOfAppTypes] = IntStream.of(failedAppDuetoWanBw).sum();
		failedAppDuetoManBw[numOfAppTypes] = IntStream.of(failedAppDuetoManBw).sum();
		failedAppDuetoLanBw[numOfAppTypes] = IntStream.of(failedAppDuetoLanBw).sum();
		failedAppDuetoMobility[numOfAppTypes] = IntStream.of(failedAppDuetoMobility).sum();

		failedKernelInKBAppDuetoBw[numOfAppTypes] = IntStream.of(failedKernelInKBAppDuetoBw).sum();
		failedKernelInKBAppDuetoWanBw[numOfAppTypes] = IntStream.of(failedKernelInKBAppDuetoWanBw).sum();
		failedKernelInKBAppDuetoManBw[numOfAppTypes] = IntStream.of(failedKernelInKBAppDuetoManBw).sum();
		failedKernelInKBAppDuetoLanBw[numOfAppTypes] = IntStream.of(failedKernelInKBAppDuetoLanBw).sum();
		failedKernelInKBAppDuetoMobility[numOfAppTypes] = IntStream.of(failedKernelInKBAppDuetoMobility).sum();


		
		// calculate server load
		double totalVmLoadOnEdge = 0;
		double totalVmLoadOnCloud = 0;
		double totalVmLoadOnMobile = 0;
		for (VmLoadLogItem entry : vmLoadList) {
			totalVmLoadOnEdge += entry.getEdgeLoad();
			totalVmLoadOnCloud += entry.getCloudLoad();
			totalVmLoadOnMobile += entry.getMobileLoad();
			if (fileLogEnabled)
				appendToFile(vmLoadBW, entry.toString());
		}

		if (fileLogEnabled) {
			// write location info to file
			for (int t = 1; t < (SimSettings.getInstance().getSimulationTime()
					/ SimSettings.getInstance().getVmLocationLogInterval()); t++) {
				int[] locationInfo = new int[SimSettings.getInstance().getNumOfPlaceTypes()];
				Double time = t * SimSettings.getInstance().getVmLocationLogInterval();

				if (time < SimSettings.getInstance().getWarmUpPeriod())
					continue;

				for (int i = 0; i < SimManager.getInstance().getNumOfMobileDevice(); i++) {

					Location loc = SimManager.getInstance().getMobilityModel().getLocation(i, time);
					int placeTypeIndex = loc.getPlaceTypeIndex();
					locationInfo[placeTypeIndex]++;
				}

				locationBW.write(time.toString());
				for (int i = 0; i < locationInfo.length; i++)
					locationBW.write(SimSettings.DELIMITER + locationInfo[i]);

				locationBW.newLine();
			}

			for (int i = 0; i < numOfAppTypes + 1; i++) {

				if (i < numOfAppTypes) {
					// if related app is not used in this simulation, just
					// discard it
					if (SimSettings.getInstance().getApplicationLookUpTable()[i][0] == 0)
						continue;
				}

				// check if the divisor is zero in order to avoid division by
				// zero problem
				double _serviceTime = (completedApplication[i] == 0) ? 0.0 : (serviceTime[i] / (double) completedApplication[i]);
				double _networkDelay = (completedApplication[i] == 0) ? 0.0 : (networkDelay[i] / ((double) completedApplication[i] - (double)completedAppOnMobile[i]));
				double _processingTime = (completedApplication[i] == 0) ? 0.0 : (processingTime[i] / (double) completedApplication[i]);
				double _vmLoadOnEdge = (vmLoadList.size() == 0) ? 0.0 : (totalVmLoadOnEdge / (double) vmLoadList.size());
				double _vmLoadOnClould = (vmLoadList.size() == 0) ? 0.0 : (totalVmLoadOnCloud / (double) vmLoadList.size());
				double _vmLoadOnMobile = (vmLoadList.size() == 0) ? 0.0 : (totalVmLoadOnMobile / (double) vmLoadList.size());
				double _cost = (completedApplication[i] == 0) ? 0.0 : (cost[i] / (double) completedApplication[i]);

				double _lanDelay = (lanUsage[i] == 0) ? 0.0
						: (lanDelay[i] / (double) lanUsage[i]);
				double _manDelay = (manUsage[i] == 0) ? 0.0
						: (manDelay[i] / (double) manUsage[i]);
				double _wanDelay = (wanUsage[i] == 0) ? 0.0
						: (wanDelay[i] / (double) wanUsage[i]);

				// write generic results
				String genericResult1 = Integer.toString(completedApplication[i]) + SimSettings.DELIMITER
						+ Integer.toString(failedApplication[i]) + SimSettings.DELIMITER 
						+ Integer.toString(uncompletedApplication[i]) + SimSettings.DELIMITER 
						+ Integer.toString(failedAppDuetoBw[i]) + SimSettings.DELIMITER
						+ Double.toString(_serviceTime) + SimSettings.DELIMITER 
						+ Double.toString(_processingTime) + SimSettings.DELIMITER 
						+ Double.toString(_networkDelay) + SimSettings.DELIMITER
						+ Double.toString(0) + SimSettings.DELIMITER 
						+ Double.toString(_cost) + SimSettings.DELIMITER 
						+ Integer.toString(failedAppDueToVmCapacity[i]) + SimSettings.DELIMITER 
						+ Integer.toString(failedAppDuetoMobility[i]);

				// check if the divisor is zero in order to avoid division by zero problem
				double _serviceTimeOnEdge = (completedAppOnEdge[i] == 0) ? 0.0
						: (serviceTimeOnEdge[i] / (double) completedAppOnEdge[i]);
				double _processingTimeOnEdge = (completedAppOnEdge[i] == 0) ? 0.0
						: (processingTimeOnEdge[i] / (double) completedAppOnEdge[i]);
				String genericResult2 = Integer.toString(completedAppOnEdge[i]) + SimSettings.DELIMITER
						+ Integer.toString(failedAppOnEdge[i]) + SimSettings.DELIMITER
						+ Integer.toString(uncompletedApplicationOnEdge[i]) + SimSettings.DELIMITER
						+ Integer.toString(0) + SimSettings.DELIMITER
						+ Double.toString(_serviceTimeOnEdge) + SimSettings.DELIMITER
						+ Double.toString(_processingTimeOnEdge) + SimSettings.DELIMITER
						+ Double.toString(0.0) + SimSettings.DELIMITER 
						+ Double.toString(_vmLoadOnEdge) + SimSettings.DELIMITER 
						+ Integer.toString(failedAppDueToVmCapacityOnEdge[i]);

				// check if the divisor is zero in order to avoid division by zero problem
				double _serviceTimeOnCloud = (completedAppOnCloud[i] == 0) ? 0.0
						: (serviceTimeOnCloud[i] / (double) completedAppOnCloud[i]);
				double _processingTimeOnCloud = (completedAppOnCloud[i] == 0) ? 0.0
						: (processingTimeOnCloud[i] / (double) completedAppOnCloud[i]);
				String genericResult3 = Integer.toString(completedAppOnCloud[i]) + SimSettings.DELIMITER
						+ Integer.toString(failedAppOnCloud[i]) + SimSettings.DELIMITER
						+ Integer.toString(uncompletedApplicationOnCloud[i]) + SimSettings.DELIMITER
						+ Integer.toString(0) + SimSettings.DELIMITER
						+ Double.toString(_serviceTimeOnCloud) + SimSettings.DELIMITER
						+ Double.toString(_processingTimeOnCloud) + SimSettings.DELIMITER 
						+ Double.toString(0.0) + SimSettings.DELIMITER
						+ Double.toString(_vmLoadOnClould) + SimSettings.DELIMITER 
						+ Integer.toString(failedAppDueToVmCapacityOnCloud[i]);
				
				// check if the divisor is zero in order to avoid division by zero problem
				double _serviceTimeOnMobile = (completedAppOnMobile[i] == 0) ? 0.0
						: (serviceTimeOnMobile[i] / (double) completedAppOnMobile[i]);
				double _processingTimeOnMobile = (completedAppOnMobile[i] == 0) ? 0.0
						: (processingTimeOnMobile[i] / (double) completedAppOnMobile[i]);
				String genericResult4 = Integer.toString(completedAppOnMobile[i]) + SimSettings.DELIMITER
						+ Integer.toString(failedAppOnMobile[i]) + SimSettings.DELIMITER
						+ Integer.toString(uncompletedapplicationOnMobile[i]) + SimSettings.DELIMITER
						+ Integer.toString(0) + SimSettings.DELIMITER
						+ Double.toString(_serviceTimeOnMobile) + SimSettings.DELIMITER
						+ Double.toString(_processingTimeOnMobile) + SimSettings.DELIMITER 
						+ Double.toString(0.0) + SimSettings.DELIMITER
						+ Double.toString(_vmLoadOnMobile) + SimSettings.DELIMITER 
						+ Integer.toString(failedAppDueToVmCapacityOnMobile[i]);
				
				String genericResult5 = Double.toString(_lanDelay) + SimSettings.DELIMITER
						+ Double.toString(_manDelay) + SimSettings.DELIMITER
						+ Double.toString(_wanDelay) + SimSettings.DELIMITER
						+ 0 + SimSettings.DELIMITER //for future use
						+ Integer.toString(failedAppDuetoLanBw[i]) + SimSettings.DELIMITER
						+ Integer.toString(failedAppDuetoManBw[i]) + SimSettings.DELIMITER
						+ Integer.toString(failedAppDuetoWanBw[i]);

				appendToFile(genericBWs[i], genericResult1);
				appendToFile(genericBWs[i], genericResult2);
				appendToFile(genericBWs[i], genericResult3);
				appendToFile(genericBWs[i], genericResult4);
				appendToFile(genericBWs[i], genericResult5);
			}

			// close open files
			if (SimSettings.getInstance().getDeepFileLoggingEnabled()) {
				successBW.close();
				failBW.close();
			}
			vmLoadBW.close();
			locationBW.close();
			for (int i = 0; i < numOfAppTypes + 1; i++) {
				if (i < numOfAppTypes) {
					// if related app is not used in this simulation, just
					// discard it
					if (SimSettings.getInstance().getApplicationLookUpTable()[i][0] == 0)
						continue;
				}
				genericBWs[i].close();
			}
		}

		// printout important results
		printLine("Total application amount: " 
				+ (failedApplication[numOfAppTypes] + completedApplication[numOfAppTypes] + uncompletedApplication[numOfAppTypes])
				);
		printLine("Kernel-Based Application amount: " + numKernelBasedApplication);
		printLine("# of Application with only one kernel(Edge/Cloud/Mobile): "
				+ (failedAppOnEdge[numOfAppTypes] + completedAppOnEdge[numOfAppTypes]) + "/" 
				+ (failedAppOnCloud[numOfAppTypes]+ completedAppOnCloud[numOfAppTypes]) + "/" 
				+ (failedAppOnMobile[numOfAppTypes]+ completedAppOnMobile[numOfAppTypes]));
		printLine("* of kernels in Kernel-Based Application(Edge/Cloud/Mobile): "
				+ (failedKernelInKBAppOnEdge[numOfAppTypes] + completedKernelInKBAppOnEdge[numOfAppTypes]) + "/" 
				+ (failedKernelInKBAppOnCloud[numOfAppTypes]+ completedKernelInKBAppOnCloud[numOfAppTypes]) + "/" 
				+ (failedKernelInKBAppOnMobile[numOfAppTypes]+ completedKernelInKBAppOnMobile[numOfAppTypes]));
		
		printLine("# of failed Applications with only one kernel (Edge/Cloud/Mobile): "
				+ failedAppOnEdge[numOfAppTypes] + "/"
				+ failedAppOnCloud[numOfAppTypes] + "/"
				+ failedAppOnMobile[numOfAppTypes]);

		printLine("* of failed kernels in Kernel-Based Application(Edge/Cloud/Mobile): "
				+ failedKernelInKBAppOnEdge[numOfAppTypes] + "/"
				+ failedKernelInKBAppOnCloud[numOfAppTypes] + "/"
				+ failedKernelInKBAppOnMobile[numOfAppTypes]);
	
		
		printLine("# of completed Applications with only one kernel(Edge/Cloud/Mobile): "
				+ completedAppOnEdge[numOfAppTypes] + "/"
				+ completedAppOnCloud[numOfAppTypes] + "/"
				+ completedAppOnMobile[numOfAppTypes]);

		printLine("* of completed kernels in Kernel-Based Application(Edge/Cloud/Mobile): "
				+ completedKernelInKBAppOnEdge[numOfAppTypes] + "/"
				+ completedKernelInKBAppOnCloud[numOfAppTypes] + "/"
				+ completedKernelInKBAppOnMobile[numOfAppTypes]);
		
		printLine("# of uncompleted Applications with only one kernel (Edge/Cloud/Mobile): "
				+ uncompletedApplicationOnEdge[numOfAppTypes] + "/"
				+ uncompletedApplicationOnCloud[numOfAppTypes] + "/"
				+ uncompletedapplicationOnMobile[numOfAppTypes]);

		printLine("* of uncompleted kernels in Kernel-Based Applications(Edge/Cloud/Mobile): "
				+ uncompletedKernelInKBAppOnEdge[numOfAppTypes] + "/"
				+ uncompletedKernelInKBAppOnCloud[numOfAppTypes] + "/"
				+ uncompletedKernelInKBAppOnMobile[numOfAppTypes]);


		printLine("# of failed application with only one kernel due to vm capacity (Edge/Cloud/Mobile): "
				+ failedAppDueToVmCapacity[numOfAppTypes] + "("
				+ failedAppDueToVmCapacityOnEdge[numOfAppTypes] + "/"
				+ failedAppDueToVmCapacityOnCloud[numOfAppTypes] + "/"
				+ failedAppDueToVmCapacityOnMobile[numOfAppTypes] + ")");
		
		printLine("* of failed kernel in Kernel-Based Application due to vm capacity (Edge/Cloud/Mobile): "
				+ failedKernelInKBAppDueToVmCapacity[numOfAppTypes] + "("
				+ failedKernelInKBAppDueToVmCapacityOnEdge[numOfAppTypes] + "/"
				+ failedKernelInKBAppDueToVmCapacityOnCloud[numOfAppTypes] + "/"
				+ failedKernelInKBAppDueToVmCapacityOnMobile[numOfAppTypes] + ")");
	
		printLine("# of failed application with only one kernel due to Mobility/Network(WLAN/MAN/WAN): "
				+ failedAppDuetoMobility[numOfAppTypes]
				+ "/" + failedAppDuetoBw[numOfAppTypes] 
				+ "(" + failedAppDuetoLanBw[numOfAppTypes] 
				+ "/" + failedAppDuetoManBw[numOfAppTypes] 
				+ "/" + failedAppDuetoWanBw[numOfAppTypes] + ")");
		
		printLine("* of failed kernel in Kernel-Based Application to Mobility/Network(WLAN/MAN/WAN): "
				+ failedKernelInKBAppDuetoMobility[numOfAppTypes]
				+ "/" + failedKernelInKBAppDuetoBw[numOfAppTypes] 
				+ "(" + failedKernelInKBAppDuetoLanBw[numOfAppTypes] 
				+ "/" + failedKernelInKBAppDuetoManBw[numOfAppTypes] 
				+ "/" + failedKernelInKBAppDuetoWanBw[numOfAppTypes] + ")");
		
		
		printLine("percentage of failed Applications: "
				+ String.format("%.6f", ((double) failedApplication[numOfAppTypes] * (double) 100)
						/ (double) (completedApplication[numOfAppTypes] + failedApplication[numOfAppTypes]))
				+ "%");

		printLine("average service time: "
				+ String.format("%.6f", serviceTime[numOfAppTypes] / (double) completedApplication[numOfAppTypes])
				+ " seconds. (" + "on Edge: "
				+ String.format("%.6f", serviceTimeOnEdge[numOfAppTypes] / (double) completedAppOnEdge[numOfAppTypes])
				+ ", " + "on Cloud: "
				+ String.format("%.6f", serviceTimeOnCloud[numOfAppTypes] / (double) completedAppOnCloud[numOfAppTypes])
				+ ", " + "on Mobile: "
				+ String.format("%.6f", serviceTimeOnMobile[numOfAppTypes] / (double) completedAppOnMobile[numOfAppTypes])
				+ ")");

		printLine("average processing time: "
				+ String.format("%.6f", processingTime[numOfAppTypes] / (double) completedApplication[numOfAppTypes])
				+ " seconds. (" + "on Edge: "
				+ String.format("%.6f", processingTimeOnEdge[numOfAppTypes] / (double) completedAppOnEdge[numOfAppTypes])
				+ ", " + "on Cloud: " 
				+ String.format("%.6f", processingTimeOnCloud[numOfAppTypes] / (double) completedAppOnCloud[numOfAppTypes])
				+ ", " + "on Mobile: " 
				+ String.format("%.6f", processingTimeOnMobile[numOfAppTypes] / (double) completedAppOnMobile[numOfAppTypes])
				+ ")");

		printLine("average network delay: "
				+ String.format("%.6f", networkDelay[numOfAppTypes] / ((double) completedApplication[numOfAppTypes] - (double) completedAppOnMobile[numOfAppTypes]))
				+ " seconds. (" + "LAN delay: "
				+ String.format("%.6f", lanDelay[numOfAppTypes] / (double) lanUsage[numOfAppTypes])
				+ ", " + "MAN delay: "
				+ String.format("%.6f", manDelay[numOfAppTypes] / (double) manUsage[numOfAppTypes])
				+ ", " + "WAN delay: "
				+ String.format("%.6f", wanDelay[numOfAppTypes] / (double) wanUsage[numOfAppTypes]) + ")");

		printLine("average server utilization Edge/Cloud/Mobile: " 
				+ String.format("%.6f", totalVmLoadOnEdge / (double) vmLoadList.size()) + "/"
				+ String.format("%.6f", totalVmLoadOnCloud / (double) vmLoadList.size()) + "/"
				+ String.format("%.6f", totalVmLoadOnMobile / (double) vmLoadList.size()));
		
		printLine("average cost: " + cost[numOfAppTypes] / completedApplication[numOfAppTypes] + "$");

		// clear related collections (map list etc.)
		kernelMap.clear();
		vmLoadList.clear();
	}
}

class VmLoadLogItem {
	private double time;
	private double vmLoadOnEdge;
	private double vmLoadOnCloud;
	private double vmLoadOnMobile;

	VmLoadLogItem(double _time, double _vmLoadOnEdge, double _vmLoadOnCloud, double _vmLoadOnMobile) {
		time = _time;
		vmLoadOnEdge = _vmLoadOnEdge;
		vmLoadOnCloud = _vmLoadOnCloud;
		vmLoadOnMobile = _vmLoadOnMobile;
	}

	public double getEdgeLoad() {
		return vmLoadOnEdge;
	}

	public double getCloudLoad() {
		return vmLoadOnCloud;
	}
	
	public double getMobileLoad() {
		return vmLoadOnMobile;
	}
	
	public String toString() {
		return time + 
				SimSettings.DELIMITER + vmLoadOnEdge +
				SimSettings.DELIMITER + vmLoadOnCloud +
				SimSettings.DELIMITER + vmLoadOnMobile;
	}
}

class LogItem {
	private SimLogger.KERNEL_STATUS status;
	private SimLogger.NETWORK_ERRORS networkError;
	private int datacenterId;
	private int hostId;
	private int vmId;
	private int vmType;
	private int kernelType;
	private int kernelLenght;
	private int kernelInputType;
	private int kernelOutputSize;
	private double kernelStartTime;
	private double kernelEndTime;
	private double lanUploadDelay;
	private double manUploadDelay;
	private double wanUploadDelay;
	private double lanDownloadDelay;
	private double manDownloadDelay;
	private double wanDownloadDelay;
	private double bwCost;
	private double cpuCost;
	private boolean isInWarmUpPeriod;
	private int kernelId;

	LogItem(int _kernelType, int _kernelLength, int _kernelInputType, int _kernelOutputSize, 
			int _kernelId) {
		kernelType = _kernelType;
		kernelLenght = _kernelLength;
		kernelInputType = _kernelInputType;
		kernelOutputSize = _kernelOutputSize;
		networkError = NETWORK_ERRORS.NONE;
		status = SimLogger.KERNEL_STATUS.CREATED;
		kernelEndTime = 0;
		kernelId = _kernelId;
	}
	
	public void kernelStarted(double time) {
		kernelStartTime = time;
		status = SimLogger.KERNEL_STATUS.UPLOADING;
		
		if (time < SimSettings.getInstance().getWarmUpPeriod())
			isInWarmUpPeriod = true;
		else
			isInWarmUpPeriod = false;
	}
	
	public void setUploadDelay(double delay, NETWORK_DELAY_TYPES delayType) {
		if(delayType == NETWORK_DELAY_TYPES.WLAN_DELAY)
			lanUploadDelay = delay;
		else if(delayType == NETWORK_DELAY_TYPES.MAN_DELAY)
			manUploadDelay = delay;
		else if(delayType == NETWORK_DELAY_TYPES.WAN_DELAY)
			wanUploadDelay = delay;
	}
	
	public void setDownloadDelay(double delay, NETWORK_DELAY_TYPES delayType) {
		if(delayType == NETWORK_DELAY_TYPES.WLAN_DELAY)
			lanDownloadDelay = delay;
		else if(delayType == NETWORK_DELAY_TYPES.MAN_DELAY)
			manDownloadDelay = delay;
		else if(delayType == NETWORK_DELAY_TYPES.WAN_DELAY)
			wanDownloadDelay = delay;
	}
	
	public void kernelAssigned(int _datacenterId, int _hostId, int _vmId, int _vmType) {
		status = SimLogger.KERNEL_STATUS.PROCESSING;
		datacenterId = _datacenterId;
		hostId = _hostId;
		vmId = _vmId;
		vmType = _vmType;
	}

	public void kernelExecuted() {
		status = SimLogger.KERNEL_STATUS.DOWNLOADING;
	}

	public void kernelEnded(double time) {
		kernelEndTime = time;
		status = SimLogger.KERNEL_STATUS.COMLETED;
	}

	public void kernelRejectedDueToVMCapacity(double time, int _vmType) {
		vmType = _vmType;
		kernelEndTime = time;
		status = SimLogger.KERNEL_STATUS.REJECTED_DUE_TO_VM_CAPACITY;
	}

	public void kernelRejectedDueToBandwidth(double time, int _vmType, NETWORK_DELAY_TYPES delayType) {
		vmType = _vmType;
		kernelEndTime = time;
		status = SimLogger.KERNEL_STATUS.REJECTED_DUE_TO_BANDWIDTH;
		
		if(delayType == NETWORK_DELAY_TYPES.WLAN_DELAY)
			networkError = NETWORK_ERRORS.LAN_ERROR;
		else if(delayType == NETWORK_DELAY_TYPES.MAN_DELAY)
			networkError = NETWORK_ERRORS.MAN_ERROR;
		else if(delayType == NETWORK_DELAY_TYPES.WAN_DELAY)
			networkError = NETWORK_ERRORS.WAN_ERROR;
	}

	public void kernelFailedDueToBandwidth(double time, NETWORK_DELAY_TYPES delayType) {
		kernelEndTime = time;
		status = SimLogger.KERNEL_STATUS.UNFINISHED_DUE_TO_BANDWIDTH;
		
		if(delayType == NETWORK_DELAY_TYPES.WLAN_DELAY)
			networkError = NETWORK_ERRORS.LAN_ERROR;
		else if(delayType == NETWORK_DELAY_TYPES.MAN_DELAY)
			networkError = NETWORK_ERRORS.MAN_ERROR;
		else if(delayType == NETWORK_DELAY_TYPES.WAN_DELAY)
			networkError = NETWORK_ERRORS.WAN_ERROR;
	}

	public void kernelFailedDueToMobility(double time) {
		kernelEndTime = time;
		status = SimLogger.KERNEL_STATUS.UNFINISHED_DUE_TO_MOBILITY;
	}

	public void setCost(double _bwCost, double _cpuCos) {
		bwCost = _bwCost;
		cpuCost = _cpuCos;
	}

	public boolean isInWarmUpPeriod() {
		return isInWarmUpPeriod;
	}

	public double getCost() {
		return bwCost + cpuCost;
	}

	public double getNetworkUploadDelay(NETWORK_DELAY_TYPES delayType) {
		double result = 0;
		if(delayType == NETWORK_DELAY_TYPES.WLAN_DELAY)
			result = lanUploadDelay;
		else if(delayType == NETWORK_DELAY_TYPES.MAN_DELAY)
			result = manUploadDelay;
		else if(delayType == NETWORK_DELAY_TYPES.WAN_DELAY)
			result = wanUploadDelay;
		
		return result;
	}

	public double getNetworkDownloadDelay(NETWORK_DELAY_TYPES delayType) {
		double result = 0;
		if(delayType == NETWORK_DELAY_TYPES.WLAN_DELAY)
			result = lanDownloadDelay;
		else if(delayType == NETWORK_DELAY_TYPES.MAN_DELAY)
			result = manDownloadDelay;
		else if(delayType == NETWORK_DELAY_TYPES.WAN_DELAY)
			result = wanDownloadDelay;
		
		return result;
	}
	
	public double getNetworkDelay(NETWORK_DELAY_TYPES delayType){
		double result = 0;
		if(delayType == NETWORK_DELAY_TYPES.WLAN_DELAY)
			result = lanDownloadDelay + lanUploadDelay;
		else if(delayType == NETWORK_DELAY_TYPES.MAN_DELAY)
			result = manDownloadDelay + manUploadDelay;
		else if(delayType == NETWORK_DELAY_TYPES.WAN_DELAY)
			result = wanDownloadDelay + wanUploadDelay;
		
		return result;
	}
	
	public double getNetworkDelay(){
		return  lanUploadDelay +
				manUploadDelay +
				wanUploadDelay +
				lanDownloadDelay +
				manDownloadDelay +
				wanDownloadDelay;
	}
	
	public double getServiceTime() {
		return kernelEndTime - kernelStartTime;
	}

	public SimLogger.KERNEL_STATUS getStatus() {
		return status;
	}

	public SimLogger.NETWORK_ERRORS getNetworkError() {
		return networkError;
	}
	
	public int getVmType() {
		return vmType;
	}

	public int getKernelType() {
		return kernelType;
	}

	public String toString(int taskId) {
		String result = taskId + SimSettings.DELIMITER + datacenterId + SimSettings.DELIMITER + hostId
				+ SimSettings.DELIMITER + vmId + SimSettings.DELIMITER + vmType + SimSettings.DELIMITER + kernelType
				+ SimSettings.DELIMITER + kernelLenght + SimSettings.DELIMITER + kernelInputType + SimSettings.DELIMITER
				+ kernelOutputSize + SimSettings.DELIMITER + kernelStartTime + SimSettings.DELIMITER + kernelEndTime
				+ SimSettings.DELIMITER;

		if (status == SimLogger.KERNEL_STATUS.COMLETED){
			result += getNetworkDelay() + SimSettings.DELIMITER;
			result += getNetworkDelay(NETWORK_DELAY_TYPES.WLAN_DELAY) + SimSettings.DELIMITER;
			result += getNetworkDelay(NETWORK_DELAY_TYPES.MAN_DELAY) + SimSettings.DELIMITER;
			result += getNetworkDelay(NETWORK_DELAY_TYPES.WAN_DELAY);
		}
		else if (status == SimLogger.KERNEL_STATUS.REJECTED_DUE_TO_VM_CAPACITY)
			result += "1"; // failure reason 1
		else if (status == SimLogger.KERNEL_STATUS.REJECTED_DUE_TO_BANDWIDTH)
			result += "2"; // failure reason 2
		else if (status == SimLogger.KERNEL_STATUS.UNFINISHED_DUE_TO_BANDWIDTH)
			result += "3"; // failure reason 3
		else if (status == SimLogger.KERNEL_STATUS.UNFINISHED_DUE_TO_MOBILITY)
			result += "4"; // failure reason 4
		else
			result += "0"; // default failure reason
		return result;
	}
	
	public int getKernelId() {
		return kernelId;
	}
}
