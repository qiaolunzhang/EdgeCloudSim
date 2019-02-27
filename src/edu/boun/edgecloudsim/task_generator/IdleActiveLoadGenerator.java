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

package edu.boun.edgecloudsim.task_generator;

import java.util.ArrayList;

import org.apache.commons.math3.distribution.ExponentialDistribution;

import edu.boun.edgecloudsim.core.SimSettings;
import edu.boun.edgecloudsim.core.TaskBasedTaskStatus;
import edu.boun.edgecloudsim.utils.TaskProperty;
import edu.boun.edgecloudsim.utils.SimLogger;
import edu.boun.edgecloudsim.utils.SimUtils;
import edu.boun.edgecloudsim.utils.TaskBasedTask;

public class IdleActiveLoadGenerator extends LoadGeneratorModel{
	int taskTypeOfDevices[];
	public IdleActiveLoadGenerator(int _numberOfMobileDevices, double _simulationTime, String _simScenario) {
		super(_numberOfMobileDevices, _simulationTime, _simScenario);
	}

	@Override
	public void initializeModel() {
		taskList = new ArrayList<TaskProperty>();
		subtaskList = new ArrayList<TaskProperty>();
		taskBasedTaskList = new ArrayList<TaskBasedTask>();
		
		//exponential number generator for file input size, file output size and task length
		ExponentialDistribution[][] expRngList = new ExponentialDistribution[SimSettings.getInstance().getTaskLookUpTable().length][3];
		//exponential number generator for sub-task
		ExponentialDistribution[][] subtaskExpRngList = new ExponentialDistribution[SimSettings.getInstance().getSubtaskLookUpTable().length][3];
		//create random number generator for each place
		for(int i=0; i<SimSettings.getInstance().getTaskLookUpTable().length; i++) {
			if(SimSettings.getInstance().getTaskLookUpTable()[i][0] ==0)
				continue;
			
			expRngList[i][0] = new ExponentialDistribution(SimSettings.getInstance().getTaskLookUpTable()[i][5]);
			expRngList[i][1] = new ExponentialDistribution(SimSettings.getInstance().getTaskLookUpTable()[i][6]);
			expRngList[i][2] = new ExponentialDistribution(SimSettings.getInstance().getTaskLookUpTable()[i][7]);
		}
		// create random number generator for each place for subtask
		for(int i=0; i<SimSettings.getInstance().getSubtaskLookUpTable().length; i++) {
			if(SimSettings.getInstance().getSubtaskLookUpTable()[i][0]==0)
				continue;
			
			subtaskExpRngList[i][0] = new ExponentialDistribution(SimSettings.getInstance().getSubtaskLookUpTable()[i][5]);
			subtaskExpRngList[i][1] = new ExponentialDistribution(SimSettings.getInstance().getSubtaskLookUpTable()[i][6]);
			subtaskExpRngList[i][2] = new ExponentialDistribution(SimSettings.getInstance().getSubtaskLookUpTable()[i][7]);
		}
		
		//Each mobile device utilizes an app type (task type)
		int taskPropertyId = 0;
		int taskBasedTaskId = 0;
		taskTypeOfDevices = new int[numberOfMobileDevices];
		for(int i=0; i<numberOfMobileDevices; i++) {
			int randomTaskType = -1;
			double taskTypeSelector = SimUtils.getRandomDoubleNumber(0,100);
			double taskTypePercentage = 0;
			for (int j=0; j<SimSettings.getInstance().getTaskLookUpTable().length; j++) {
				taskTypePercentage += SimSettings.getInstance().getTaskLookUpTable()[j][0];
				if(taskTypeSelector <= taskTypePercentage){
					randomTaskType = j;
					break;
				}
			}
			if(randomTaskType == -1){
				SimLogger.printLine("Impossible is occured! no random task type!");
				continue;
			}
			
			taskTypeOfDevices[i] = randomTaskType;
			
			double poissonMean = SimSettings.getInstance().getTaskLookUpTable()[randomTaskType][2];
			double activePeriod = SimSettings.getInstance().getTaskLookUpTable()[randomTaskType][3];
			double idlePeriod = SimSettings.getInstance().getTaskLookUpTable()[randomTaskType][4];
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
				
				if (SimSettings.getInstance().isTaskBasedApplication(randomTaskType)) {
					// create an object of TaskBasedTask
					int subtaskNum = SimSettings.getInstance().getsubTaskNum(randomTaskType);

					int[][] dependency_task = SimSettings.getInstance().getsubTaskDependency(randomTaskType);
					
					TaskBasedTaskStatus.getInstance().addTaskBasedTask(subtaskNum, taskBasedTaskId);
					
					int[] id_subtask_list = new int[subtaskNum];
					
					for (int subTaskIndex=0; subTaskIndex<subtaskNum; subTaskIndex++) {
						// map the subtask to TaskBasedTask
						int subRandomTaskType = SimSettings.getInstance().getsubTaskIndex(randomTaskType, subTaskIndex);
						TaskProperty taskProperty = new TaskProperty(i, subRandomTaskType, virtualTime, subtaskExpRngList, taskPropertyId);
						//int taskId = taskProperty.getCloud
						taskList.add(taskProperty);
						id_subtask_list[subTaskIndex] = taskPropertyId;
						taskPropertyId++;
					}
					
					// add the taskPropertyId list
					TaskBasedTaskStatus.getInstance().addSubTaskIdList(id_subtask_list, taskBasedTaskId);
					// add the dependency
					for (int id=0; id<dependency_task.length; id++) {
						for (int id_dependency=0; id_dependency < dependency_task[id].length; id_dependency++)
							if (dependency_task[id][id_dependency] ==  1) {
								// pass id_subtask_list[id] because what we pass is the property_id, not the index
								TaskBasedTaskStatus.getInstance().addDependency(id_subtask_list[id], id_subtask_list[id_dependency], taskBasedTaskId);
							}
					}
					taskBasedTaskId++;
					
				}
				else {
					taskList.add(new TaskProperty(i,randomTaskType, virtualTime, expRngList, taskPropertyId));
					taskPropertyId++;
				}
			}
		}
	}

	@Override
	public int getTaskTypeOfDevice(int deviceId) {
		// TODO Auto-generated method stub
		return taskTypeOfDevices[deviceId];
	}

}
