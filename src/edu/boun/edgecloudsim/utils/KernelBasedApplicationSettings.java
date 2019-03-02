package edu.boun.edgecloudsim.utils;

public class KernelBasedApplicationSettings {
	private int startIndex;
	private int numKernel;
	/*
	 * dependency[0][1] = 1 means task 0 is dependent on task 1
	 * 	 0 1
	 * 0 
	 * 1
	 */
	private int[][] dependency;

	public KernelBasedApplicationSettings(int num, int subtaskIndex) {
		numKernel = num;
		startIndex = subtaskIndex;
		/*
		 * Initialize the application with no dependency
		 * dependencies are added later 
		 */
		dependency = new int[numKernel][numKernel];
}
	
	/*
	 * given a index, return the kernel index in SimSettings 
	 */
	public int getKernelIndex(int index) {
		return startIndex + index;
	}
	
	
	public int getKernelNum() {
		return numKernel;
	}

	/*
	 * Add a dependency dependencyTaskId for task taskId
	 */
	public void addDependency(int taskId, int dependencyTaskId) {
		dependency[taskId][dependencyTaskId] = 1;
	}
	
	public int[][] getDependency() {
		return dependency;
	}
}