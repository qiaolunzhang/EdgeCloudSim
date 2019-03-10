package edu.boun.edgecloudsim.utils;

import java.util.*;

public class KernelBasedApplication {
	private int numKernel;
	// map from the KernelId to the inner index id
	private Map<Integer, Integer>kernelId2Index;
	// map from the inner index id to KernelId
	private Map<Integer, Integer>index2KernelId;
	private boolean[] submitted;
	// finished used to check if the kernel has ended
	private boolean[] finished;
	// store the final status of the execution of all kernels in the KernelBasedApplication
	// -1: not tracked 0: finished 1: not finished 2: failed
	private int kbAPPFinalStatus;
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
		kbAPPFinalStatus = -1;
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
	
	/**
	 * Add a dependency kernel for a kernel
	 * @param kernelId
	 * @param dependencyKernelId
	 */
	public void addDependency(int kernelId, int dependencyKernelId) {
		int kernelIdIndex = kernelId2Index.get(kernelId);
		int dependencyIdIndex = kernelId2Index.get(dependencyKernelId);
		dependency[kernelIdIndex][dependencyIdIndex] = 1;
		dependency_met[kernelIdIndex][dependencyIdIndex] = 0;
	}
	
	
	public void addKernelIdList(int[] kernelList) {
		for (int i=0; i<kernelList.length; i++) {
			kernelId2Index.put(kernelList[i], i);
			index2KernelId.put(i, kernelList[i]);
		}
		
	}
	
	/**
	 * remove dependency when a kernel finished
	 * @param kernelId: the kernelId
	 */
	private void removeDependency(int kernelId) {
		int kernelIndex = kernelId2Index.get(kernelId);
		for (int i=0; i<numKernel; i++) {
			dependency_met[i][kernelIndex] = 1;
		}
	}

	/**
	 * return true: dependency has been met
	 * return false: dependencies has not been met
	 * @param index
	 * @return true if the dependency of the kernel has been met
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
	

	/**
	 * Get kernels that can be executed when a kernel ends
	 * @param finishedKernelId
	 * @return
	 */
	public List<Integer> getKernelToSubmit(int finishedKernelId) {
		List<Integer> kerneltoSubmit = new ArrayList<Integer>();
		removeDependency(finishedKernelId);
		int index_submitted = kernelId2Index.get(finishedKernelId);
		submitted[index_submitted] = true;
		finished[index_submitted] = true;
		for (int index=0; index<numKernel; index++) {
			// check if the dependencies has been met and whether the kernel has been submitted
			// cannot submit a kernel if the kernel has been submitted
			if (checkDependency(index) && (submitted[index] == false)) {
				kerneltoSubmit.add(index2KernelId.get(index));
				//submitted[index] = true; 
			}
		}
		
		return kerneltoSubmit;
	}
	
	public void setKernelSubmit(int kernelId) {
		// we cannot remove the dependency to the kernel has kernelID because the kernel has not ended now
		int index_submitted = kernelId2Index.get(kernelId);
		submitted[index_submitted] = true;
	}
	
	
	public boolean checkReadySubmit(int taskPropertyId) {
		int taskIndex = kernelId2Index.get(taskPropertyId);
		if (checkDependency(taskIndex) && submitted[taskIndex] == false) {
			return true;
		} else {
			return false;
		}
	}
	
	/**
	 * check whether the execution all the kernels in the KernelBasedApplication has end
	 * but if the data have not been sent back to user devices, the application is not completed yet
	 * @return true if all the kernels in the KernelBasedApplication has ended
	 */
	public boolean checkKernelBasedApplicationEnd() {
		boolean flag = true;
		for (int i=0; i<numKernel; i++) {
			if (finished[i]== false) {
				flag = false;
			}
		}
		return flag;
	}
	
	public void checkAllSubmittedAndSetStatus() {
		// -1: not tracked 0: finished 1: not finished 2: failed
		//boolean flag_submit = true;
		boolean flag_finished = true;
		for (int i=0; i<numKernel; i++) {
			/*
			if (submitted[i]== false) {
				flag_submit = false;
			}
			*/
			if (finished[i] == false) {
				flag_finished = false;
			}
		}
		if (flag_finished == true) {
			kbAPPFinalStatus = 0;
		} else {
			kbAPPFinalStatus = 1;
		}
}
	
	public void setKBAPPFinalStatus(int status) {
		kbAPPFinalStatus = status;
	}
	
	public int getKBAPPFinalStatus() {
		return kbAPPFinalStatus;
	}
	
	/**
	 * In SimLogger, we will loop all the kernels to calculate the simulation data
	 * To avoid log the same KernelBasedApplication multiple times, we will keep
	 * a status_logged to stand for having calculated the data of this KernelBasedApplication
	 */
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