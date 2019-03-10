/*
 * Title:        EdgeCloudSim - Idle/Active Load Generator implementation
 * 
 * Description: 
 * IdleActiveLoadGenerator implements basic load generator model where the
 * mobile devices generate task in active period and waits in idle period.
 * Task interarrival time (load generation period), Idle and active periods
 * are defined in the configuration file.
 * 
 * Licence:      GPL - http://www.gnu.org/copyleft/gpl.html
 * Copyright (c) 2017, Bogazici University, Istanbul, Turkey
 */

package edu.boun.edgecloudsim.app_generator;

import java.util.*;

import org.apache.commons.math3.distribution.ExponentialDistribution;

import edu.boun.edgecloudsim.core.SimSettings;
import edu.boun.edgecloudsim.core.KernelBasedApplicationStatus;
import edu.boun.edgecloudsim.utils.KernelProperty;
import edu.boun.edgecloudsim.utils.SimLogger;
import edu.boun.edgecloudsim.utils.SimUtils;
import edu.boun.edgecloudsim.utils.KernelBasedApplication;

public class IdleActiveLoadGenerator extends LoadGeneratorModel{
	private Map<Integer, Integer>kernelId2KernelPropertyListIndex;
	int applicationTypeOfDevices[];
	public IdleActiveLoadGenerator(int _numberOfMobileDevices, double _simulationTime, String _simScenario) {
		super(_numberOfMobileDevices, _simulationTime, _simScenario);
	}

	@Override
	public void initializeModel() {
		kernelPropertyList = new ArrayList<KernelProperty>();
		kernelPropertyInKernelBasedAppList = new ArrayList<KernelProperty>();
		kernelBasedApplicationList = new ArrayList<KernelBasedApplication>();
		kernelId2KernelPropertyListIndex = new HashMap<Integer, Integer>();
		
		//exponential number generator for file input size, file output size and kernel length
		ExponentialDistribution[][] expRngList = new ExponentialDistribution[SimSettings.getInstance().getApplicationLookUpTable().length][3];
		//exponential number generator for kernel in Kernel-based application
		ExponentialDistribution[][] kernelKBAPPExpRngList = new ExponentialDistribution[SimSettings.getInstance().getKernelLookUpTable().length][3];
		//create random number generator for each place
		for(int i=0; i<SimSettings.getInstance().getApplicationLookUpTable().length; i++) {
			if(SimSettings.getInstance().getApplicationLookUpTable()[i][0] ==0)
				continue;
			
			expRngList[i][0] = new ExponentialDistribution(SimSettings.getInstance().getApplicationLookUpTable()[i][5]);
			expRngList[i][1] = new ExponentialDistribution(SimSettings.getInstance().getApplicationLookUpTable()[i][6]);
			expRngList[i][2] = new ExponentialDistribution(SimSettings.getInstance().getApplicationLookUpTable()[i][7]);
		}
		// create random number generator for each place for kernel in kernal-based application
		for(int i=0; i<SimSettings.getInstance().getKernelLookUpTable().length; i++) {
			if(SimSettings.getInstance().getKernelLookUpTable()[i][0]==0)
				continue;
			
			kernelKBAPPExpRngList[i][0] = new ExponentialDistribution(SimSettings.getInstance().getKernelLookUpTable()[i][5]);
			kernelKBAPPExpRngList[i][1] = new ExponentialDistribution(SimSettings.getInstance().getKernelLookUpTable()[i][6]);
			kernelKBAPPExpRngList[i][2] = new ExponentialDistribution(SimSettings.getInstance().getKernelLookUpTable()[i][7]);
		}
		
		//Each mobile device utilizes an app type
		// the id of the kernel, which will be used to distinguish the kernel
		int kernelId = 0;
		int kernelBasedAppId = 0;
		applicationTypeOfDevices = new int[numberOfMobileDevices];
		for(int i=0; i<numberOfMobileDevices; i++) {
			int randomApplicationType = -1;
			double applicationTypeSelector = SimUtils.getRandomDoubleNumber(0,100);
			double applicationTypePercentage = 0;
			for (int j=0; j<SimSettings.getInstance().getApplicationLookUpTable().length; j++) {
				applicationTypePercentage += SimSettings.getInstance().getApplicationLookUpTable()[j][0];
				if(applicationTypeSelector <= applicationTypePercentage){
					randomApplicationType = j;
					break;
				}
			}
			if(randomApplicationType == -1){
				SimLogger.printLine("Impossible is occured! no random task type!");
				continue;
			}
			
			applicationTypeOfDevices[i] = randomApplicationType;
			
			double poissonMean = SimSettings.getInstance().getApplicationLookUpTable()[randomApplicationType][2];
			double activePeriod = SimSettings.getInstance().getApplicationLookUpTable()[randomApplicationType][3];
			double idlePeriod = SimSettings.getInstance().getApplicationLookUpTable()[randomApplicationType][4];
			double activePeriodStartTime = SimUtils.getRandomDoubleNumber(
					SimSettings.CLIENT_ACTIVITY_START_TIME, 
					SimSettings.CLIENT_ACTIVITY_START_TIME + activePeriod);  //active period starts shortly after the simulation started (e.g. 10 seconds)
			double virtualTime = activePeriodStartTime;

			ExponentialDistribution rng = new ExponentialDistribution(poissonMean);
			while(virtualTime < simulationTime) {
				double interval = rng.sample();

				if(interval <= 0){
					SimLogger.printLine("Impossible is occured! interval is " + interval + " for device " + i + " time " + virtualTime);
					continue;
				}
				//SimLogger.printLine(virtualTime + " -> " + interval + " for device " + i + " time ");
				virtualTime += interval;
				
				if(virtualTime > activePeriodStartTime + activePeriod){
					activePeriodStartTime = activePeriodStartTime + activePeriod + idlePeriod;
					virtualTime = activePeriodStartTime;
					continue;
				}
				
				if (SimSettings.getInstance().isKernelBasedApplication(randomApplicationType)) {
					// create an object of TaskBasedTask
					// kernelNum is the number of kernels in the kernel-based application
					int kernelNum = SimSettings.getInstance().getKernelNum(randomApplicationType);

					// stores the kernel dependency graph
					int[][] dependencyKernel = SimSettings.getInstance().getKernelBasedApplicationDependency(randomApplicationType);
					
					KernelBasedApplicationStatus.getInstance().addKernelBasedApplication(kernelNum, kernelBasedAppId);
					
					int[] kernelIdList = new int[kernelNum];
					
					for (int kernelIndex=0; kernelIndex<kernelNum; kernelIndex++) {
						// generate and store the kernelPropertyList in this class
						int kernelType = SimSettings.getInstance().getKernelIndex(randomApplicationType, kernelIndex);
						KernelProperty kernelProperty = new KernelProperty(i, kernelType, randomApplicationType, virtualTime, kernelKBAPPExpRngList, kernelId);
						int propertyListIndex = kernelPropertyList.size();
						kernelId2KernelPropertyListIndex.put(kernelId, propertyListIndex);
						kernelPropertyList.add(kernelProperty);
						// store the index of kernels in this application in kernelPropertyList
						kernelIdList[kernelIndex] = kernelId;
						kernelId++;
					}
					
					// add the kernelId list to the KernelBasedApplicationStatus
					KernelBasedApplicationStatus.getInstance().addKernelIdList(kernelIdList, kernelBasedAppId);
					// add the dependency
					for (int id=0; id<dependencyKernel.length; id++) {
						for (int id_dependency=0; id_dependency < dependencyKernel[id].length; id_dependency++)
							if (dependencyKernel[id][id_dependency] ==  1) {
								KernelBasedApplicationStatus.getInstance().addDependency(kernelIdList[id], kernelIdList[id_dependency], kernelBasedAppId);
							}
					}
					kernelBasedAppId++;
					
				}
				else {
					kernelPropertyList.add(new KernelProperty(i,randomApplicationType, virtualTime, expRngList, kernelId));
					kernelId++;
				}
			}
		}
	}

	@Override
	public int getApplicationTypeOfDevice(int deviceId) {
		// TODO Auto-generated method stub
		return applicationTypeOfDevices[deviceId];
	}
	
	public int getKernelPropertyIndex(int kernelId) {
		return kernelId2KernelPropertyListIndex.get(kernelId);
	}

}
