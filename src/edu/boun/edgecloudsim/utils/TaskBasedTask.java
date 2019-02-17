package edu.boun.edgecloudsim.utils;

public class TaskBasedTask {
	private int numSubTask;
	private int[] tasks;
	private int[] submitted;
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
		for (int i=0; i<numSubTask; i++) {
			tasks[i]= taskList[i]; 
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
	public void removeDependency(int taskId) {
		int task_index = getTaskIndex(taskId);
		for (int i=0; i<numSubTask; i++) {
			dependency_met[i][task_index] = 1;
		}
	}

	public boolean checkDependency(int taskId) {
		int task_index = getTaskIndex(taskId);
		boolean flag = true;
		for (int i=0; i<numSubTask; i++) {
			if (dependency_met[task_index][i] == 0) {
				flag = false;
			}
		}

		return flag;
	}
/*	
	public int[] getTaskToSubmit(int taskFinishedId) {
	}
	*/


}