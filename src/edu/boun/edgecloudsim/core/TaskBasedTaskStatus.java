package edu.boun.edgecloudsim.core;

import java.util.*;

import edu.boun.edgecloudsim.utils.TaskBasedTask;

public class TaskBasedTaskStatus {
	

	private static TaskBasedTaskStatus instance = null;
	private int simManagerId;
	private int numTaskBasedTask;
	private Map<Integer, TaskBasedTask> taskBasedTaskMap;
	private Map<Integer, Integer> mKeyMap;
	
	private TaskBasedTaskStatus() {
		taskBasedTaskMap = new HashMap<Integer, TaskBasedTask>();
		mKeyMap = new HashMap<Integer, Integer>();
	}
	
	public static TaskBasedTaskStatus getInstance() {
		if (instance == null) {
			instance = new TaskBasedTaskStatus();
		}
		return instance;
	}
	
	public void addTaskBasedTask(int num_subtask, int taskBasedTaskId) {
		taskBasedTaskMap.put(taskBasedTaskId, new TaskBasedTask(num_subtask, taskBasedTaskId));
	}
	
	/**
	 * @param id_subTask_list: the list of taskPropertyId
	 * @param taskBasedTaskId: the id of taskBasedTask
	 */
	public void addSubTaskIdList(int[] id_subTask_list, int taskBasedTaskId) {
		for (int i=0; i<id_subTask_list.length; i++) {
			mKeyMap.put(id_subTask_list[i], taskBasedTaskId);
		}
		taskBasedTaskMap.get(taskBasedTaskId).addSubTaskIdList(id_subTask_list);
	}
	
	public void addDependency(int id, int id_dependency, int taskBasedTaskId) {
		taskBasedTaskMap.get(taskBasedTaskId).addDependency(id, id_dependency);
	}
	
	public boolean checkReadySubmit(int taskPropertyId) {
		//taskBasedTaskMap.get(task)
		int taskBasedTaskId = mKeyMap.get(taskPropertyId);
		boolean result = taskBasedTaskMap.get(taskBasedTaskId).checkReadySubmit(taskPropertyId);
		return result;
	}
	
	public List<Integer> getTaskSubmit(int taskPropertyId) {
		int taskBasedTaskId = mKeyMap.get(taskPropertyId);
		List<Integer> newTaskList = taskBasedTaskMap.get(taskBasedTaskId).getTaskToSubmit(taskPropertyId);
		return newTaskList;
	}
	
	public void setTaskSubmit(int taskPropertyId) {
		int taskBasedTaskId = mKeyMap.get(taskPropertyId);
		taskBasedTaskMap.get(taskBasedTaskId).setTaskSubmit(taskPropertyId);
	}
	
	public boolean checkTaskBasedTaskEnd(int taskPropertyId) {
		int taskBasedTaskId = mKeyMap.get(taskPropertyId);
		boolean result = taskBasedTaskMap.get(taskBasedTaskId).checkTaskBasedTaskEnd();
		return result;
	}
	
	public void setTaskBasedTaskFinalStatus(int taskPropertyId, int status) {
		int taskBasedTaskId = mKeyMap.get(taskPropertyId);
		taskBasedTaskMap.get(taskBasedTaskId).setTaskFinalStatus(status);
	}
	
	public int getTaskBasedTaskFinalStatus(int taskPropertyId) {
		int taskBasedTaskId = mKeyMap.get(taskPropertyId);
		int status = taskBasedTaskMap.get(taskBasedTaskId).getTaskFinalStatus();
		return status;
	}
	
	public void setFinalStatusLogged(int taskPropertyId) {
		int taskBasedTaskId = mKeyMap.get(taskPropertyId);
		taskBasedTaskMap.get(taskBasedTaskId).setFinalStatusLogged();
	}
	
	public boolean checkFinalStatusLogged(int taskPropertyId) {
		int taskBasedTaskId = mKeyMap.get(taskPropertyId);
		return taskBasedTaskMap.get(taskBasedTaskId).checkStatusUnLogged();
	}
	

	public void checkAllSubmittedAndSetStatus() {
		for (Integer key: taskBasedTaskMap.keySet()) {
			taskBasedTaskMap.get(key).checkAllSubmittedAndSetStatus();
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
	
	public void reset() {
		instance = null;
	}
	
	/*
	public int getTaskFinalStatus(int taskPropertyId) {
		int taskBasedTaskId = mKeyMap.get(taskPropertyId);
		return taskBasedTaskMap.get(taskBasedTaskId).getTaskFinalStatus();
	}
	*/
	
	public int[] getStatics() {
		int[] taskFinalStatucs = new int[3];
		int success = 0;
		int unfinished = 0;
		int fail = 0;
		for (Integer key: taskBasedTaskMap.keySet()) {
			int status = taskBasedTaskMap.get(key).getTaskFinalStatus();
			if (status == 0) {
				success++;
			} else if (status == 1) {
				unfinished++;
			} else {
				fail++;
			}
		}
		taskFinalStatucs[0] = success;
		taskFinalStatucs[1] = unfinished;
		taskFinalStatucs[2] = fail;
		return taskFinalStatucs;
	}

}