/*
 * Title:        EdgeCloudSim - Simulation Manager
 * 
 * Description: 
 * SimManager is an singleton class providing many abstract classeses such as
 * Network Model, Mobility Model, Edge Orchestrator to other modules
 * Critical simulation related information would be gathered via this class 
 * 
 * Licence:      GPL - http://www.gnu.org/copyleft/gpl.html
 * Copyright (c) 2017, Bogazici University, Istanbul, Turkey
 */

package edu.boun.edgecloudsim.core;

import java.io.IOException;
import java.util.List;

import org.cloudbus.cloudsim.Host;
import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.core.SimEntity;
import org.cloudbus.cloudsim.core.SimEvent;

import edu.boun.edgecloudsim.edge_orchestrator.EdgeOrchestrator;
import edu.boun.edgecloudsim.edge_server.EdgeServerManager;
import edu.boun.edgecloudsim.edge_server.EdgeVmAllocationPolicy_Custom;
import edu.boun.edgecloudsim.cloud_server.CloudServerManager;
import edu.boun.edgecloudsim.edge_client.MobileDeviceManager;
import edu.boun.edgecloudsim.edge_client.mobile_processing_unit.MobileServerManager;
import edu.boun.edgecloudsim.mobility.MobilityModel;
import edu.boun.edgecloudsim.task_generator.LoadGeneratorModel;
import edu.boun.edgecloudsim.network.NetworkModel;
import edu.boun.edgecloudsim.utils.KernelProperty;
import edu.boun.edgecloudsim.utils.SimLogger;

public class SimManager extends SimEntity {
	private static final int CREATE_TASK = 0;
	private static final int CHECK_ALL_VM = 1;
	private static final int GET_LOAD_LOG = 2;
	private static final int PRINT_PROGRESS = 3;
	private static final int STOP_SIMULATION = 4;
	private static final int CREATE_KERNEL = 5;
	
	private String simScenario;
	private String orchestratorPolicy;
	private int numOfMobileDevice;
	private NetworkModel networkModel;
	private MobilityModel mobilityModel;
	private ScenarioFactory scenarioFactory;
	private EdgeOrchestrator edgeOrchestrator;
	private EdgeServerManager edgeServerManager;
	private CloudServerManager cloudServerManager;
	private MobileServerManager mobileServerManager;
	private LoadGeneratorModel loadGeneratorModel;
	private MobileDeviceManager mobileDeviceManager;
	
	private static SimManager instance = null;
	
	public SimManager(ScenarioFactory _scenarioFactory, int _numOfMobileDevice, String _simScenario, String _orchestratorPolicy) throws Exception {
		super("SimManager");
		simScenario = _simScenario;
		scenarioFactory = _scenarioFactory;
		numOfMobileDevice = _numOfMobileDevice;
		orchestratorPolicy = _orchestratorPolicy;

		SimLogger.print("Creating tasks...");
		loadGeneratorModel = scenarioFactory.getLoadGeneratorModel();
		loadGeneratorModel.initializeModel();
		SimLogger.printLine("Done, ");
		
		SimLogger.print("Creating device locations...");
		mobilityModel = scenarioFactory.getMobilityModel();
		mobilityModel.initialize();
		SimLogger.printLine("Done.");

		//Generate network model
		networkModel = scenarioFactory.getNetworkModel();
		networkModel.initialize();
		
		//Generate edge orchestrator
		edgeOrchestrator = scenarioFactory.getEdgeOrchestrator();
		edgeOrchestrator.initialize();
		
		//Create Physical Servers
		edgeServerManager = scenarioFactory.getEdgeServerManager();
		edgeServerManager.initialize();
		
		//Create Physical Servers on cloud
		cloudServerManager = scenarioFactory.getCloudServerManager();
		cloudServerManager.initialize();
		
		//Create Physical Servers on mobile devices
		mobileServerManager = scenarioFactory.getMobileServerManager();
		mobileServerManager.initialize();

		//Create Client Manager
		mobileDeviceManager = scenarioFactory.getMobileDeviceManager();
		mobileDeviceManager.initialize();
		
		
		instance = this;
	}
	
	public static SimManager getInstance(){
		return instance;
	}
	
	/**
	 * Triggering CloudSim to start simulation
	 */
	public void startSimulation() throws Exception{
		//Starts the simulation
		SimLogger.print(super.getName()+" is starting...");
		
		//Start Edge Datacenters & Generate VMs
		edgeServerManager.startDatacenters();
		edgeServerManager.createVmList(mobileDeviceManager.getId());
		
		//Start Edge Datacenters & Generate VMs
		cloudServerManager.startDatacenters();
		cloudServerManager.createVmList(mobileDeviceManager.getId());
		
		//Start Mobile Datacenters & Generate VMs
		mobileServerManager.startDatacenters();
		mobileServerManager.createVmList(mobileDeviceManager.getId());
		
		CloudSim.startSimulation();
	}

	public String getSimulationScenario(){
		return simScenario;
	}

	public String getOrchestratorPolicy(){
		return orchestratorPolicy;
	}
	
	public ScenarioFactory getScenarioFactory(){
		return scenarioFactory;
	}
	
	public int getNumOfMobileDevice(){
		return numOfMobileDevice;
	}
	
	public NetworkModel getNetworkModel(){
		return networkModel;
	}

	public MobilityModel getMobilityModel(){
		return mobilityModel;
	}
	
	public EdgeOrchestrator getEdgeOrchestrator(){
		return edgeOrchestrator;
	}
	
	public EdgeServerManager getEdgeServerManager(){
		return edgeServerManager;
	}
	
	public CloudServerManager getCloudServerManager(){
		return cloudServerManager;
	}
	
	public MobileServerManager getMobileServerManager(){
		return mobileServerManager;
	}

	public LoadGeneratorModel getLoadGeneratorModel(){
		return loadGeneratorModel;
	}
	
	public MobileDeviceManager getMobileDeviceManager(){
		return mobileDeviceManager;
	}
	
	@Override
	public void startEntity() {
		int hostCounter=0;

		for(int i= 0; i<edgeServerManager.getDatacenterList().size(); i++) {
			List<? extends Host> list = edgeServerManager.getDatacenterList().get(i).getHostList();
			for (int j=0; j < list.size(); j++) {
				mobileDeviceManager.submitVmList(edgeServerManager.getVmList(hostCounter));
				hostCounter++;
			}
		}
		
		for(int i= 0; i<SimSettings.getInstance().getNumOfCoudHost(); i++) {
			mobileDeviceManager.submitVmList(cloudServerManager.getVmList(i));
		}

		for(int i=0; i<numOfMobileDevice; i++){
			if(mobileServerManager.getVmList(i) != null)
				mobileDeviceManager.submitVmList(mobileServerManager.getVmList(i));
		}
		
		// log the SimManagerId to TaskBasedTaskStatus.java
		KernelBasedApplicationStatus.getInstance().setSimManagerId(getId());
		
		//Creation of tasks are scheduled here!
		for(int i=0; i< loadGeneratorModel.getKernelPropertyList().size(); i++) {
			int taskPropertyId = loadGeneratorModel.getKernelPropertyList().get(i).getTaskPropertyId();
			if (KernelBasedApplicationStatus.getInstance().checkSubTask(taskPropertyId)) {
				boolean ready_to_submit = KernelBasedApplicationStatus.getInstance().checkReadySubmit(taskPropertyId);
				if (ready_to_submit) {
					schedule(getId(), loadGeneratorModel.getKernelPropertyList().get(i).getStartTime(), CREATE_TASK, loadGeneratorModel.getKernelPropertyList().get(i));
					//TaskBasedTaskStatus.getInstance().setTaskSubmit(taskPropertyId);
					// cannot set the sub-task as submitted here, otherwise, all the task will be submitted
					KernelBasedApplicationStatus.getInstance().setTaskSubmit(taskPropertyId);
				}
			}
			else {
				schedule(getId(), loadGeneratorModel.getKernelPropertyList().get(i).getStartTime(), CREATE_TASK, loadGeneratorModel.getKernelPropertyList().get(i));
			}
		}
		
		//Periodic event loops starts from here!
		schedule(getId(), 5, CHECK_ALL_VM);
		schedule(getId(), SimSettings.getInstance().getSimulationTime()/100, PRINT_PROGRESS);
		schedule(getId(), SimSettings.getInstance().getVmLoadLogInterval(), GET_LOAD_LOG);
		schedule(getId(), SimSettings.getInstance().getSimulationTime(), STOP_SIMULATION);
		
		SimLogger.printLine("Done.");
	}

	@Override
	public void processEvent(SimEvent ev) {
		synchronized(this){
			switch (ev.getTag()) {
			case CREATE_TASK:
				try {
					KernelProperty edgeTask = (KernelProperty) ev.getData();
					mobileDeviceManager.submitKernel(edgeTask);						
				} catch (Exception e) {
					e.printStackTrace();
					System.exit(0);
				}
				break;
			case CHECK_ALL_VM:
				int totalNumOfVm = SimSettings.getInstance().getNumOfEdgeVMs();
				if(EdgeVmAllocationPolicy_Custom.getCreatedVmNum() != totalNumOfVm){
					SimLogger.printLine("All VMs cannot be created! Terminating simulation...");
					System.exit(0);
				}
				break;
			case GET_LOAD_LOG:
				SimLogger.getInstance().addVmUtilizationLog(
						CloudSim.clock(),
						edgeServerManager.getAvgUtilization(),
						cloudServerManager.getAvgUtilization(),
						mobileServerManager.getAvgUtilization());
				
				schedule(getId(), SimSettings.getInstance().getVmLoadLogInterval(), GET_LOAD_LOG);
				break;
			case PRINT_PROGRESS:
				int progress = (int)((CloudSim.clock()*100)/SimSettings.getInstance().getSimulationTime());
				if(progress % 10 == 0)
					SimLogger.print(Integer.toString(progress));
				else
					SimLogger.print(".");
				if(CloudSim.clock() < SimSettings.getInstance().getSimulationTime())
					schedule(getId(), SimSettings.getInstance().getSimulationTime()/100, PRINT_PROGRESS);

				break;
			case STOP_SIMULATION:
				SimLogger.printLine("100");
				CloudSim.terminateSimulation();
				try {
					SimLogger.getInstance().simStopped();
				} catch (IOException e) {
					e.printStackTrace();
					System.exit(0);
				}
				break;
			case CREATE_KERNEL:
				int kernelId = (int) ev.getData();
				//System.out.println("receive instruction to create new sub-task: " + taskPropertyId);
				boolean ready_to_submit = KernelBasedApplicationStatus.getInstance().checkReadySubmit(kernelId);
				if (ready_to_submit) {
					//System.out.println("this kernel in kernel-based application is ready to submit");
				}
				int kernelPropertyIndex = loadGeneratorModel.getKernelPropertyIndex(kernelId);
				//System.out.println("the task list index is: " + taskListIndex);
				KernelProperty kernelProperty = loadGeneratorModel.getKernelPropertyList().get(kernelPropertyIndex);
				//TODO make sure if we need to modify the starttime here
				mobileDeviceManager.submitKernel(kernelProperty);
				KernelBasedApplicationStatus.getInstance().setTaskSubmit(kernelId);
			default:
				Log.printLine(getName() + ": unknown event type");
				break;
			}
		}
	}

	@Override
	public void shutdownEntity() {
		edgeServerManager.terminateDatacenters();
		cloudServerManager.terminateDatacenters();
		mobileServerManager.terminateDatacenters();
	}
}
