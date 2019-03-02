package edu.boun.edgecloudsim.core;

import java.util.*;

import edu.boun.edgecloudsim.utils.KernelBasedApplication;

public class KernelBasedApplicationStatus {
	

	private static KernelBasedApplicationStatus instance = null;
	private int simManagerId;
	private int numKernelBasedApplication;
	private Map<Integer, KernelBasedApplication> kernelBasedApplicationMap;
	// map kernelBasedApplication id to the key in kernelBasedApplicationMap
	private Map<Integer, Integer> mKeyMap;
	
	private KernelBasedApplicationStatus() {
		kernelBasedApplicationMap = new HashMap<Integer, KernelBasedApplication>();
		mKeyMap = new HashMap<Integer, Integer>();
	}
	
	public static KernelBasedApplicationStatus getInstance() {
		if (instance == null) {
			instance = new KernelBasedApplicationStatus();
		}
		return instance;
	}
	
	public void addKernelBasedApplication(int numKernel, int kernelBasedApplicationId) {
		kernelBasedApplicationMap.put(kernelBasedApplicationId, new KernelBasedApplication(numKernel, kernelBasedApplicationId));
		numKernelBasedApplication++;
	}
	
	/**
	 * @param id_subTask_list: the list of taskPropertyId
	 * @param taskBasedTaskId: the id of taskBasedTask
	 */
	public void addKernelIdList(int[] kernelIdList, int kernelBasedApplicationId) {
		for (int i=0; i<kernelIdList.length; i++) {
			mKeyMap.put(kernelIdList[i], kernelBasedApplicationId);
		}
		kernelBasedApplicationMap.get(kernelBasedApplicationId).addKernelIdList(kernelIdList);
	}
	
	public void addDependency(int id, int id_dependency, int taskBasedTaskId) {
		kernelBasedApplicationMap.get(taskBasedTaskId).addDependency(id, id_dependency);
	}
	
	public boolean checkReadySubmit(int taskPropertyId) {
		//taskBasedTaskMap.get(task)
		int taskBasedTaskId = mKeyMap.get(taskPropertyId);
		boolean result = kernelBasedApplicationMap.get(taskBasedTaskId).checkReadySubmit(taskPropertyId);
		return result;
	}
	
	public List<Integer> getTaskSubmit(int taskPropertyId) {
		int taskBasedTaskId = mKeyMap.get(taskPropertyId);
		List<Integer> newTaskList = kernelBasedApplicationMap.get(taskBasedTaskId).getKernelToSubmit(taskPropertyId);
		return newTaskList;
	}
	
	public void setTaskSubmit(int taskPropertyId) {
		int taskBasedTaskId = mKeyMap.get(taskPropertyId);
		kernelBasedApplicationMap.get(taskBasedTaskId).setKernelSubmit(taskPropertyId);
	}
	
	public boolean checkTaskBasedTaskEnd(int taskPropertyId) {
		int taskBasedTaskId = mKeyMap.get(taskPropertyId);
		boolean result = kernelBasedApplicationMap.get(taskBasedTaskId).checkKernelBasedApplicationEnd();
		return result;
	}
	
	public void setTaskBasedTaskFinalStatus(int taskPropertyId, int status) {
		int taskBasedTaskId = mKeyMap.get(taskPropertyId);
		kernelBasedApplicationMap.get(taskBasedTaskId).setKBAPPFinalStatus(status);
	}
	
	public int getTaskBasedTaskFinalStatus(int taskPropertyId) {
		int taskBasedTaskId = mKeyMap.get(taskPropertyId);
		int status = kernelBasedApplicationMap.get(taskBasedTaskId).getKBAPPFinalStatus();
		return status;
	}
	
	public void setFinalStatusLogged(int taskPropertyId) {
		int taskBasedTaskId = mKeyMap.get(taskPropertyId);
		kernelBasedApplicationMap.get(taskBasedTaskId).setFinalStatusLogged();
	}
	
	public boolean checkFinalStatusLogged(int taskPropertyId) {
		int taskBasedTaskId = mKeyMap.get(taskPropertyId);
		return kernelBasedApplicationMap.get(taskBasedTaskId).checkStatusUnLogged();
	}
	

	public void checkAllSubmittedAndSetStatus() {
		for (Integer key: kernelBasedApplicationMap.keySet()) {
			kernelBasedApplicationMap.get(key).checkAllSubmittedAndSetStatus();
		}
	}

	
	public boolean checkSubTask(int taskPropertyId) {
		boolean exist = mKeyMap.containsKey(taskPropertyId);
		return exist;
	}
	
	public void setSimManagerId(int id) {
		simManagerId = id;
	}
	
	public int getSimManagerId() {
		return simManagerId;
	}
	
	/*
	 * reset this class for the next scenario
	 */
	public void reset() {
		instance = null;
	}
	
	/*
	public int getTaskFinalStatus(int taskPropertyId) {
		int taskBasedTaskId = mKeyMap.get(taskPropertyId);
		return taskBasedTaskMap.get(taskBasedTaskId).getTaskFinalStatus();
	}
	*/
	
	public int getNumKernelBasedApplication() {
		return numKernelBasedApplication;
	}
	
	public int[] getStatics() {
		int[] allKBAPPFinalStatus = new int[3];
		int success = 0;
		int unfinished = 0;
		int fail = 0;
		for (Integer key: kernelBasedApplicationMap.keySet()) {
			int status = kernelBasedApplicationMap.get(key).getKBAPPFinalStatus();
			if (status == 0) {
				success++;
			} else if (status == 1) {
				unfinished++;
			} else {
				fail++;
			}
		}
		allKBAPPFinalStatus[0] = success;
		allKBAPPFinalStatus[1] = unfinished;
		allKBAPPFinalStatus[2] = fail;
		return allKBAPPFinalStatus;
	}

}