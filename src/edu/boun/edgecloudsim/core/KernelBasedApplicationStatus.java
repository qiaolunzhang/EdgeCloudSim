package edu.boun.edgecloudsim.core;

import java.util.*;

import edu.boun.edgecloudsim.utils.KernelBasedApplication;

public class KernelBasedApplicationStatus {
	

	private static KernelBasedApplicationStatus instance = null;
	// used when we want to add an event to SimManager
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
	
	public void addDependency(int id, int id_dependency, int kernelBasedAppId) {
		kernelBasedApplicationMap.get(kernelBasedAppId).addDependency(id, id_dependency);
	}
	
	/**
	 * check whether a kernel in kernel-based application can be submitted(dependency)
	 * @param kernelId
	 * @return
	 */
	public boolean checkReadySubmit(int kernelId) {
		int kernelBasedAppId = mKeyMap.get(kernelId);
		boolean result = kernelBasedApplicationMap.get(kernelBasedAppId).checkReadySubmit(kernelId);
		return result;
	}
	
	/**
	 * input a kernel that has ended, and get kernels that can be submitted now
	 *  @param kernelId
	 * @return
	 */
	public List<Integer> getKernelSubmit(int kernelId) {
		int kernelBasedAppId = mKeyMap.get(kernelId);
		List<Integer> kernelListReadySubmit = kernelBasedApplicationMap.get(kernelBasedAppId).getKernelToSubmit(kernelId);
		return kernelListReadySubmit;
	}
	
	/**
	 * set a kernel as submitted
	 * @param kernelId
	 */
	public void setKernelSubmit(int kernelId) {
		int kernelBasedAppId = mKeyMap.get(kernelId);
		kernelBasedApplicationMap.get(kernelBasedAppId).setKernelSubmit(kernelId);
	}
	
	/**
	 * input a kernel id, check whether the corresponding kernel-based application has ended
	 * @param kernelId
	 * @return
	 */
	public boolean checkKernelBasedAppEnd(int kernelId) {
		int kernelBasedAppId = mKeyMap.get(kernelId);
		boolean result = kernelBasedApplicationMap.get(kernelBasedAppId).checkKernelBasedApplicationEnd();
		return result;
	}
	
	/**
	 * set the status(e.g. finished, uncompleted) for the kernel-based application 
	 * that contains the kernel with kernel ID
	 * @param kernelId
	 * @param status
	 */
	public void setKernelBasedAppFinalStatus(int kernelId, int status) {
		int kernelBasedAppId = mKeyMap.get(kernelId);
		kernelBasedApplicationMap.get(kernelBasedAppId).setKBAPPFinalStatus(status);
	}
	
	/**
	 * get the final status of the kernel-based application that contains the
	 * kernel with kernel ID
	 * @param kernelId
	 * @return
	 */
	public int getKernelBasedAppFinalStatus(int kernelId) {
		int kernelBasedAppId = mKeyMap.get(kernelId);
		int status = kernelBasedApplicationMap.get(kernelBasedAppId).getKBAPPFinalStatus();
		return status;
	}
	
	
	/**
	 * once the status of kernel-based application has been logged by SimLogger,
	 * use this function to set the kernel-based application as logged
	 * @param kernelId
	 */
	public void setFinalStatusLogged(int kernelId) {
		int kernelBasedAppId = mKeyMap.get(kernelId);
		kernelBasedApplicationMap.get(kernelBasedAppId).setFinalStatusLogged();
	}
	
	public boolean checkFinalStatusLogged(int kernelId) {
		int kernelBasedAppId = mKeyMap.get(kernelId);
		return kernelBasedApplicationMap.get(kernelBasedAppId).checkStatusUnLogged();
	}
	

	public void checkAllSubmittedAndSetStatus() {
		for (Integer key: kernelBasedApplicationMap.keySet()) {
			kernelBasedApplicationMap.get(key).checkAllSubmittedAndSetStatus();
		}
	}

	
	public boolean checkKernelInKBApp(int kernelId) {
		boolean exist = mKeyMap.containsKey(kernelId);
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