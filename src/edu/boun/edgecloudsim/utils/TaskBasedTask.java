package edu.boun.edgecloudsim.utils;

import java.util.*;

public class TaskBasedTask {
	private int taskBasedTaskId;
	private int numSubTask;
	// map from the TaskPropertyId to the inner index id
	private Map<Integer, Integer>propertyId2Index;
	private Map<Integer, Integer>index2PropertyId;
	private boolean[] submitted;
	// finished used to check if the task has ended
	private boolean[] finished;
	// -1: not tracked 0: finished 1: not finished 2: failed
	private int taskFinalStatus;
	/*
	 * dependency[0][1] = 1 means task 0 is dependent on task 1
	 * 	 0 1
	 * 0 
	 * 1
	 */
	private int[][] dependency;
	/*
	 * initial: if dependency[0][1] = 1, dependency_met[0][1] = 0
	 * 			otherwise, dependency_met[0][1] = 1
	 * dependency_met[0][1] = 1 means dependency for 0 is met considering 1
	 */
	private int[][] dependency_met;
	

	public TaskBasedTask(int _num, int _taskBasedTaskId) {
		numSubTask = _num;
		taskBasedTaskId = _taskBasedTaskId;
		propertyId2Index = new HashMap<Integer, Integer>();
		index2PropertyId = new HashMap<Integer, Integer>();
		submitted = new boolean[numSubTask];
		finished = new boolean[numSubTask];
		taskFinalStatus = -1;
		for (int i=0; i<numSubTask; i++) {
			submitted[i] = false; 
			finished[i] = false; 
		}
		/*
		 * Initialize the task with no dependency
		 * dependencies are added later 
		 */
		dependency = new int[numSubTask][numSubTask];
		dependency_met = new int[numSubTask][numSubTask];
		for (int i=0; i < numSubTask; i++) {
			for (int j=0; j<numSubTask; j++) {
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
		int taskId = propertyId2Index.get(id);
		int dependencyTaskId = propertyId2Index.get(id_dependency);
		dependency[taskId][dependencyTaskId] = 1;
		dependency_met[taskId][dependencyTaskId] = 0;
	}
	
	
	public void addSubTaskIdList(int[] subTaskIdList) {
		for (int i=0; i<subTaskIdList.length; i++) {
			propertyId2Index.put(subTaskIdList[i], i);
			index2PropertyId.put(i, subTaskIdList[i]);
		}
		
	}
	
	/*
	 * taskId: the index in subtaskLookUpTable
	 * remove dependency when a task finished
	 */
	private void removeDependency(int taskPropertyId) {
		int task_index = propertyId2Index.get(taskPropertyId);
		for (int i=0; i<numSubTask; i++) {
			dependency_met[i][task_index] = 1;
		}
	}

	/*
	 * return true: dependency has been met
	 * return false: dependencies has not been met
	 */
	private boolean checkDependency(int index) {
		int task_index = index;
		boolean flag = true;
		for (int i=0; i<numSubTask; i++) {
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
		int index_submitted = propertyId2Index.get(finishedTaskPropertyId);
		submitted[index_submitted] = true;
		finished[index_submitted] = true;
		for (int index=0; index<numSubTask; index++) {
			// check if the dependencies has been met and whether the task has been submitted
			if (checkDependency(index) && (submitted[index] == false)) {
				tasktoSubmit.add(index2PropertyId.get(index));
				//submitted[index] = true; 
			}
		}
		
		return tasktoSubmit;
	}
	
	public void setTaskSubmit(int taskPropertyId) {
		//removeDependency(taskPropertyId);
		int index_submitted = propertyId2Index.get(taskPropertyId);
		submitted[index_submitted] = true;
	}
	
	
	public List<Integer> getInitialTaskToSubmit(){
		List<Integer> tasktoSubmit = new ArrayList<>();
		for (int index=0; index<numSubTask; index++) {
			// check whether dependencies has been met and whether the task has been submitted
			if (checkDependency(index) && (submitted[index] == false)) {
				tasktoSubmit.add(index2PropertyId.get(index));
				//submitted[index] = true; 
			}
		}
		return tasktoSubmit;
	}
	
	public boolean checkReadySubmit(int taskPropertyId) {
		int taskIndex = propertyId2Index.get(taskPropertyId);
		if (checkDependency(taskIndex) && submitted[taskIndex] == false) {
			return true;
		} else {
			return false;
		}
	}
	
	public boolean checkTaskBasedTaskEnd() {
		boolean flag = true;
		for (int i=0; i<numSubTask; i++) {
			if (finished[i]== false) {
				flag = false;
			}
		}
		return flag;
	}
	
	public void checkAllSubmittedAndSetStatus() {
		boolean flag_submit = true;
		boolean flag_finished = true;
		for (int i=0; i<numSubTask; i++) {
			if (submitted[i]== false) {
				flag_submit = false;
			}
			if (finished[i] == false) {
				flag_finished = false;
			}
		}
		if (flag_submit == false) {
			// set as unfinished
			taskFinalStatus = 1;
		} else if (flag_finished == true) {
			// set as finished
			taskFinalStatus = 0;
		}
	}
	
	public void setTaskFinalStatus(int status) {
		taskFinalStatus = status;
	}
	
	public int getTaskFinalStatus() {
		return taskFinalStatus;
	}
}