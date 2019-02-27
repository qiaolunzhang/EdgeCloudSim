package edu.boun.edgecloudsim.core;

import java.util.*;

import edu.boun.edgecloudsim.utils.TaskBasedTask;

public class TaskBasedTaskStatus {
	

	private static TaskBasedTaskStatus instance = null;
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
	
	public boolean checkTaskBasedTaskEnd(int taskPropertyId) {
		int taskBasedTaskId = mKeyMap.get(taskPropertyId);
		boolean result = taskBasedTaskMap.get(taskBasedTaskId).checkTaskBasedTaskEnd();
		return result;
	}
	
	public boolean checkSubTask(int taskPropertyId) {
		boolean exist = mKeyMap.containsKey(taskPropertyId);
		return exist;
	}
	
	public void reset() {
		instance = null;
	}
}
