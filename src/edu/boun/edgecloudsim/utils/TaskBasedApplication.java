package edu.boun.edgecloudsim.utils;

import java.util.ArrayList;
import java.util.List;

public class TaskBasedApplication {
	private int startIndex;
	private int numSubTask;
	private int[] tasks;
	private boolean[] submitted;
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
	

	public TaskBasedApplication(int num, int subtaskIndex) {
		numSubTask = num;
		startIndex = subtaskIndex;
		tasks = new int[numSubTask];
		submitted = new boolean[numSubTask];
		for (int i=0; i<numSubTask; i++) {
			submitted[i] = false; 
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
	 * given a taskId, return the task_index`
	 */
	public int getSubTaskIndex(int index) {
		return startIndex + index;
	}
	
	
	public int getSubTaskNum() {
		return numSubTask;
	}

	/*
	 * Add a dependency dependencyTaskId for task taskId
	 */
	public void addDependency(int taskId, int dependencyTaskId) {
		//int task_index = getTaskIndex(taskId);
		//int dependency_index = getTaskIndex(dependencyTaskId);
		dependency[taskId][dependencyTaskId] = 1;
		dependency_met[taskId][dependencyTaskId] = 0;
	}
	
	/*
	 * taskId: the index in subtaskLookUpTable
	 * remove dependency when a task finished
	 */
	private void removeDependency(int taskId) {
		int task_index = taskId - startIndex;
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
	

	public List<Integer> getTaskToSubmit(int taskFinishedId) {
		List<Integer> tasktoSubmit = new ArrayList<Integer>();
		removeDependency(taskFinishedId);
		for (int index=0; index<numSubTask; index++) {
			// check if the dependencies has been met and whether the task has been submitted
			if (checkDependency(index) && (submitted[index] == false)) {
				tasktoSubmit.add(index+startIndex);
				submitted[index] = true; 
			}
		}
		
		return tasktoSubmit;
	}
	
	public List<Integer> getInitialTaskToSubmit(){
		List<Integer> tasktoSubmit = new ArrayList<>();
		for (int index=0; index<numSubTask; index++) {
			// check whether dependencies has been met and whether the task has been submitted
			if (checkDependency(index) && (submitted[index] == false)) {
				tasktoSubmit.add(index+startIndex);
				submitted[index] = true; 
			}
		}
		return tasktoSubmit;
	}
	
	public boolean checkReadySubmit(int taskId) {
		int taskIndex = taskId - startIndex;
		if (checkDependency(taskIndex) && submitted[taskIndex] == false) {
			return true;
		} else {
			return false;
		}
	}
	
	public int[][] getDependency() {
		return dependency;
	}
}