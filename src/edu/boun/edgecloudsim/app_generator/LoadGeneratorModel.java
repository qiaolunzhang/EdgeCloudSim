/*
 * Title:        EdgeCloudSim - Load Generator Model
 * 
 * Description: 
 * LoadGeneratorModel is an abstract class which is used for 
 * deciding task generation pattern via a task list. For those who
 * wants to add a custom Load Generator Model to EdgeCloudSim should
 * extend this class and provide a concreate instance via ScenarioFactory
 *               
 * Licence:      GPL - http://www.gnu.org/copyleft/gpl.html
 * Copyright (c) 2017, Bogazici University, Istanbul, Turkey
 */

package edu.boun.edgecloudsim.app_generator;

import java.util.List;

import edu.boun.edgecloudsim.utils.KernelBasedApplication;
import edu.boun.edgecloudsim.utils.KernelProperty;

public abstract class LoadGeneratorModel {
	protected List<KernelProperty> kernelPropertyList;
	protected List<KernelProperty> kernelPropertyInKernelBasedAppList;
	protected List<KernelBasedApplication> kernelBasedApplicationList;
	protected int numberOfMobileDevices;
	protected double simulationTime;
	protected String simScenario;
	
	public LoadGeneratorModel(int _numberOfMobileDevices, double _simulationTime, String _simScenario){
		numberOfMobileDevices=_numberOfMobileDevices;
		simulationTime=_simulationTime;
		simScenario=_simScenario;
	};
	
	/**
	 * @return the list of all KernelProperty
	 */
	public List<KernelProperty> getKernelPropertyList() {
		return kernelPropertyList;
	}

	/*
	 * fill kernelProperty list according to related task generation model
	 */
	public abstract void initializeModel();
	
	/*
	 * returns the Application type (index) that the mobile device uses
	 */
	public abstract int getApplicationTypeOfDevice(int deviceId);
	
	/*
	 * This function should be overwritten in IdleActiveLoadGenerator
	 */
	public int getKernelPropertyIndex(int kernelId) {
		return -1;
	};
	
}
