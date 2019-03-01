package edu.boun.edgecloudsim.utils;

import java.util.*;

public class KernelBasedApplication {
	private int numKernel;
	// map from the KernelId to the inner index id
	private Map<Integer, Integer>kernelId2Index;
	// map from the inner index id to KernelId
	private Map<Integer, Integer>index2KernelId;
	private boolean[] submitted;
	// finished used to check if the task has ended
	private boolean[] finished;
	// -1: not tracked 0: finished 1: not finished 2: failed
	private int kernelFinalStatus;
	/*
	 * dependency[1][0] = 1 means kernel 1 can only be executed after on kernel 0 is ended
	 * 	 0 1
	 * 0 
	 * 1
	 */
	private int[][] dependency;
	/*
	 * Stores the run-time kernel-dependency graph information
	 * initial: if dependency[0][1] = 1, dependency_met[0][1] = 0
	 * 			otherwise, dependency_met[0][1] = 1
	 * dependency_met[0][1] = 1 means dependency for 0 is met considering 1
	 */
	private int[][] dependency_met;
	
	// used in SimLogger
	private boolean status_logged;
	

	public KernelBasedApplication(int _num, int _taskBasedTaskId) {
		numKernel = _num;
		kernelId2Index = new HashMap<Integer, Integer>();
		index2KernelId = new HashMap<Integer, Integer>();
		submitted = new boolean[numKernel];
		finished = new boolean[numKernel];
		kernelFinalStatus = -1;
		status_logged = false;
		for (int i=0; i<numKernel; i++) {
			submitted[i] = false; 
			finished[i] = false; 
		}
		/*
		 * Initialize the task with no dependency
		 * dependencies are added later 
		 */
		dependency = new int[numKernel][numKernel];
		dependency_met = new int[numKernel][numKernel];
		for (int i=0; i < numKernel; i++) {
			for (int j=0; j<numKernel; j++) {
				dependency[i][j] = 0;
				dependency_met[i][j]= 1; 
			}
		}
	}
	
	/*
	/*
	 * Add a dependency dependencyTaskId for task taskId
	 */
	public void addDependency(int id, int id_dependency) {
		//int task_index = getTaskIndex(taskId);
		//int dependency_index = getTaskIndex(dependencyTaskId);
		int kernelIndexId = kernelId2Index.get(id);
		int dependencyKernelId = kernelId2Index.get(id_dependency);
		dependency[kernelIndexId][dependencyKernelId] = 1;
		dependency_met[kernelIndexId][dependencyKernelId] = 0;
	}
	
	
	public void addKernelIdList(int[] kernelList) {
		for (int i=0; i<kernelList.length; i++) {
			kernelId2Index.put(kernelList[i], i);
			index2KernelId.put(i, kernelList[i]);
		}
		
	}
	
	/*
	 * kernelId: the index in subtaskLookUpTable
	 * remove dependency when a task finished
	 */
	private void removeDependency(int kernelId) {
		int kernelIndex = kernelId2Index.get(kernelId);
		for (int i=0; i<numKernel; i++) {
			dependency_met[i][kernelIndex] = 1;
		}
	}

	/*
	 * return true: dependency has been met
	 * return false: dependencies has not been met
	 */
	private boolean checkDependency(int index) {
		int task_index = index;
		boolean flag = true;
		for (int i=0; i<numKernel; i++) {
			if (dependency_met[task_index][i] == 0) {
				flag = false;
			}
		}

		return flag;
	}
	

	public List<Integer> getTaskToSubmit(int finishedTaskPropertyId) {
		List<Integer> tasktoSubmit = new ArrayList<Integer>();
		removeDependency(finishedTaskPropertyId);
		// make sure we have set all the task to submitted
		int index_submitted = kernelId2Index.get(finishedTaskPropertyId);
		submitted[index_submitted] = true;
		finished[index_submitted] = true;
		for (int index=0; index<numKernel; index++) {
			// check if the dependencies has been met and whether the task has been submitted
			if (checkDependency(index) && (submitted[index] == false)) {
				tasktoSubmit.add(index2KernelId.get(index));
				//submitted[index] = true; 
			}
		}
		
		return tasktoSubmit;
	}
	
	public void setTaskSubmit(int taskPropertyId) {
		//removeDependency(taskPropertyId);
		int index_submitted = kernelId2Index.get(taskPropertyId);
		submitted[index_submitted] = true;
	}
	
	
	public List<Integer> getInitialTaskToSubmit(){
		List<Integer> tasktoSubmit = new ArrayList<>();
		for (int index=0; index<numKernel; index++) {
			// check whether dependencies has been met and whether the task has been submitted
			if (checkDependency(index) && (submitted[index] == false)) {
				tasktoSubmit.add(index2KernelId.get(index));
				//submitted[index] = true; 
			}
		}
		return tasktoSubmit;
	}
	
	public boolean checkReadySubmit(int taskPropertyId) {
		int taskIndex = kernelId2Index.get(taskPropertyId);
		if (checkDependency(taskIndex) && submitted[taskIndex] == false) {
			return true;
		} else {
			return false;
		}
	}
	
	public boolean checkTaskBasedTaskEnd() {
		boolean flag = true;
		for (int i=0; i<numKernel; i++) {
			if (finished[i]== false) {
				flag = false;
			}
		}
		return flag;
	}
	
	public void checkAllSubmittedAndSetStatus() {
		boolean flag_submit = true;
		boolean flag_finished = true;
		for (int i=0; i<numKernel; i++) {
			if (submitted[i]== false) {
				flag_submit = false;
			}
			if (finished[i] == false) {
				flag_finished = false;
			}
		}
		if (flag_submit == false) {
			// set as unfinished
			kernelFinalStatus = 1;
		} else if (flag_finished == true) {
			// set as finished
			kernelFinalStatus = 0;
		}
	}
	
	public void setTaskFinalStatus(int status) {
		kernelFinalStatus = status;
	}
	
	public int getTaskFinalStatus() {
		return kernelFinalStatus;
	}
	
	public void setFinalStatusLogged() {
		status_logged = true;
	}
	
	public boolean checkStatusUnLogged() {
		if (status_logged) {
			return false;
		}
		else {
			return true;
		}
	}
}