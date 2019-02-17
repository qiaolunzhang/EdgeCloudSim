package edu.boun.edgecloudsim.utils;

import java.util.ArrayList;
import java.util.List;

public class TaskBasedTask {
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
	

	public TaskBasedTask(int num, int[] taskList) {
		numSubTask = num;
		tasks = new int[numSubTask];
		submitted = new boolean[numSubTask];
		for (int i=0; i<numSubTask; i++) {
			tasks[i] = taskList[i]; 
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
	public int getTaskIndex(int taskId) {
		int task_index = 0;
		for (int i=0; i<numSubTask; i++) {
			if (tasks[i] == taskId) {
				task_index = i;
				break;
			}
		}
		return task_index;
	}

	/*
	 * Add a dependency dependencyTaskId for task taskId
	 */
	public void addDependency(int taskId, int dependencyTaskId) {
		int task_index = getTaskIndex(taskId);
		int dependency_index = getTaskIndex(dependencyTaskId);
		dependency[task_index][dependency_index] = 1;
		dependency_met[task_index][dependency_index] = 0;
	}
	
	/*
	 * remove dependency when a task finished
	 */
	private void removeDependency(int taskId) {
		int task_index = getTaskIndex(taskId);
		for (int i=0; i<numSubTask; i++) {
			dependency_met[i][task_index] = 1;
		}
	}

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
				tasktoSubmit.add(tasks[index]);
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
				tasktoSubmit.add(tasks[index]);
				submitted[index] = true; 
			}
		}
		return tasktoSubmit;
	}

}