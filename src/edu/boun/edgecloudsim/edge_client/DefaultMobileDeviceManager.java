/*
 * Title:        EdgeCloudSim - Mobile Device Manager
 * 
 * Description: 
 * DefaultMobileDeviceManager is responsible for submitting the tasks to the related
 * device by using the Edge Orchestrator. It also takes proper actions 
 * when the execution of the tasks are finished.
 * By default, DefaultMobileDeviceManager sends tasks to the edge servers or
 * cloud servers. If you want to use different topology, for example
 * MAN edge server, you should modify the flow defined in this class.
 * 
 * Licence:      GPL - http://www.gnu.org/copyleft/gpl.html
 * Copyright (c) 2017, Bogazici University, Istanbul, Turkey
 */

package edu.boun.edgecloudsim.edge_client;

import java.util.List;

import org.cloudbus.cloudsim.UtilizationModel;
import org.cloudbus.cloudsim.UtilizationModelFull;
import org.cloudbus.cloudsim.Vm;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.core.CloudSimTags;
import org.cloudbus.cloudsim.core.SimEvent;

import edu.boun.edgecloudsim.core.SimManager;
import edu.boun.edgecloudsim.core.SimSettings;
import edu.boun.edgecloudsim.core.SimSettings.NETWORK_DELAY_TYPES;
import edu.boun.edgecloudsim.core.KernelBasedApplicationStatus;
import edu.boun.edgecloudsim.network.NetworkModel;
import edu.boun.edgecloudsim.utils.KernelProperty;
import edu.boun.edgecloudsim.utils.Location;
import edu.boun.edgecloudsim.utils.SimLogger;

public class DefaultMobileDeviceManager extends MobileDeviceManager {
	private static final int BASE = 100000; //start from base in order not to conflict cloudsim tag!
	private static final int REQUEST_RECEIVED_BY_CLOUD = BASE + 1;
	private static final int REQUEST_RECIVED_BY_EDGE_DEVICE = BASE + 2;
	private static final int RESPONSE_RECEIVED_BY_MOBILE_DEVICE = BASE + 3;
	private int taskIdCounter=0;
	
	
	public DefaultMobileDeviceManager() throws Exception{
	}


	@Override
	public void initialize() {
	}
	
	@Override
	public UtilizationModel getCpuUtilizationModel() {
		return new CpuUtilizationModel_Custom();
	}
	
	/**
	 * Submit cloudlets to the created VMs.
	 * 
	 * @pre $none
	 * @post $none
	 */
	protected void submitCloudlets() {
		//do nothing!
	}
	
	/**
	 * Process a cloudlet return event.
	 * 
	 * @param ev a SimEvent object
	 * @pre ev != $null
	 * @post $none
	 */
	protected void processCloudletReturn(SimEvent ev) {
		NetworkModel networkModel = SimManager.getInstance().getNetworkModel();
		Kernel kernel = (Kernel) ev.getData();

		int kernelId = kernel.getKernelId();
		if (KernelBasedApplicationStatus.getInstance().checkKernelInKBApp(kernelId)) {
			int simManagerId = KernelBasedApplicationStatus.getInstance().getSimManagerId();
			// in the meanwhile set the kernel as finished 
			List<Integer> kernelListReadySubmit = KernelBasedApplicationStatus.getInstance().getKernelSubmit(kernelId);
			for (int i=0; i<kernelListReadySubmit.size(); i++) {
				// send the event to SimManager
				scheduleNow(simManagerId, 5, kernelListReadySubmit.get(i));
			}
			
			/*
			// check if the kernel-based application has ended
			boolean ended_flag = KernelBasedApplicationStatus.getInstance().checkKernelBasedAppEnd(kernelId);
			if (ended_flag) {
				//System.out.println("a new kernel in kernel-based application has ended");
			}
			*/
		}
		
		SimLogger.getInstance().kernelExecuted(kernel.getCloudletId());

		if(kernel.getAssociatedDatacenterId() == SimSettings.CLOUD_DATACENTER_ID){
			//SimLogger.printLine(CloudSim.clock() + ": " + getName() + ": task #" + task.getCloudletId() + " received from cloud");
			double WanDelay = networkModel.getDownloadDelay(SimSettings.CLOUD_DATACENTER_ID, kernel.getMobileDeviceId(), kernel);
			if(WanDelay > 0)
			{
				Location currentLocation = SimManager.getInstance().getMobilityModel().getLocation(kernel.getMobileDeviceId(),CloudSim.clock()+WanDelay);
				if(kernel.getSubmittedLocation().getServingWlanId() == currentLocation.getServingWlanId())
				{
					networkModel.downloadStarted(kernel.getSubmittedLocation(), SimSettings.CLOUD_DATACENTER_ID);
					SimLogger.getInstance().setDownloadDelay(kernel.getCloudletId(), WanDelay, NETWORK_DELAY_TYPES.WAN_DELAY);
					schedule(getId(), WanDelay, RESPONSE_RECEIVED_BY_MOBILE_DEVICE, kernel);
				}
				else
				{
					SimLogger.getInstance().failedDueToMobility(kernel.getCloudletId(), CloudSim.clock());
				}
			}
			else
			{
				SimLogger.getInstance().failedDueToBandwidth(kernel.getCloudletId(), CloudSim.clock(), NETWORK_DELAY_TYPES.WAN_DELAY);
			}
		}
		else{
			//SimLogger.printLine(CloudSim.clock() + ": " + getName() + ": task #" + task.getCloudletId() + " received from edge");
			double WlanDelay = networkModel.getDownloadDelay(kernel.getAssociatedHostId(), kernel.getMobileDeviceId(), kernel);
			if(WlanDelay > 0)
			{
				Location currentLocation = SimManager.getInstance().getMobilityModel().getLocation(kernel.getMobileDeviceId(),CloudSim.clock()+WlanDelay);
				if(kernel.getSubmittedLocation().getServingWlanId() == currentLocation.getServingWlanId())
				{
					networkModel.downloadStarted(currentLocation, SimSettings.GENERIC_EDGE_DEVICE_ID);
					SimLogger.getInstance().setDownloadDelay(kernel.getCloudletId(), WlanDelay, NETWORK_DELAY_TYPES.WLAN_DELAY);
					schedule(getId(), WlanDelay, RESPONSE_RECEIVED_BY_MOBILE_DEVICE, kernel);
				}
				else
				{
					SimLogger.getInstance().failedDueToMobility(kernel.getCloudletId(), CloudSim.clock());
				}
			}
			else
			{
				SimLogger.getInstance().failedDueToBandwidth(kernel.getCloudletId(), CloudSim.clock(), NETWORK_DELAY_TYPES.WLAN_DELAY);
			}
		}
	}
	
	protected void processOtherEvent(SimEvent ev) {
		if (ev == null) {
			SimLogger.printLine(getName() + ".processOtherEvent(): " + "Error - an event is null! Terminating simulation...");
			System.exit(0);
			return;
		}
		
		NetworkModel networkModel = SimManager.getInstance().getNetworkModel();
		
		switch (ev.getTag()) {
			case REQUEST_RECEIVED_BY_CLOUD:
			{
				Kernel task = (Kernel) ev.getData();

				networkModel.uploadFinished(task.getSubmittedLocation(), SimSettings.CLOUD_DATACENTER_ID);

				submitTaskToVm(task,0,SimSettings.CLOUD_DATACENTER_ID);
				
				break;
			}
			case REQUEST_RECIVED_BY_EDGE_DEVICE:
			{
				Kernel task = (Kernel) ev.getData();
				
				networkModel.uploadFinished(task.getSubmittedLocation(), SimSettings.GENERIC_EDGE_DEVICE_ID);
				
				submitTaskToVm(task, 0, SimSettings.GENERIC_EDGE_DEVICE_ID);
				
				break;
			}
			case RESPONSE_RECEIVED_BY_MOBILE_DEVICE:
			{
				Kernel task = (Kernel) ev.getData();
				
				if(task.getAssociatedDatacenterId() == SimSettings.CLOUD_DATACENTER_ID)
					networkModel.downloadFinished(task.getSubmittedLocation(), SimSettings.CLOUD_DATACENTER_ID);
				else if(task.getAssociatedDatacenterId() != SimSettings.MOBILE_DATACENTER_ID)
					networkModel.downloadFinished(task.getSubmittedLocation(), SimSettings.GENERIC_EDGE_DEVICE_ID);
				
				SimLogger.getInstance().kernelEnded(task.getCloudletId(), CloudSim.clock());
				break;
			}
			default:
				SimLogger.printLine(getName() + ".processOtherEvent(): " + "Error - event unknown by this DatacenterBroker. Terminating simulation...");
				System.exit(0);
				break;
		}
	}

	public void submitKernel(KernelProperty kernelProperty) {
		NetworkModel networkModel = SimManager.getInstance().getNetworkModel();
		
		//create a kernel
		Kernel kernel = createKernel(kernelProperty);
		
		Location currentLocation = SimManager.getInstance().getMobilityModel().
				getLocation(kernel.getMobileDeviceId(),CloudSim.clock());
		
		//set location of the mobile device which generates this kernel
		kernel.setSubmittedLocation(currentLocation);

		//add related kernel to log list
		SimLogger.getInstance().addLog(kernel.getCloudletId(),
				kernel.getKernelType(),
				(int)kernel.getCloudletLength(),
				(int)kernel.getCloudletFileSize(),
				(int)kernel.getCloudletOutputSize(),
				(int)kernel.getKernelId());

		int nextHopId = SimManager.getInstance().getEdgeOrchestrator().getDeviceToOffload(kernel);
		
		if(nextHopId == SimSettings.CLOUD_DATACENTER_ID){
			double WanDelay = networkModel.getUploadDelay(kernel.getMobileDeviceId(), nextHopId, kernel);
			
			if(WanDelay>0){
				networkModel.uploadStarted(currentLocation, nextHopId);
				SimLogger.getInstance().kernelStarted(kernel.getCloudletId(), CloudSim.clock());
				SimLogger.getInstance().setUploadDelay(kernel.getCloudletId(), WanDelay, NETWORK_DELAY_TYPES.WAN_DELAY);
				schedule(getId(), WanDelay, REQUEST_RECEIVED_BY_CLOUD, kernel);
			}
			else
			{
				//SimLogger.printLine("Task #" + task.getCloudletId() + " cannot assign to any VM");
				SimLogger.getInstance().rejectedDueToBandwidth(
						kernel.getCloudletId(),
						CloudSim.clock(),
						SimSettings.VM_TYPES.CLOUD_VM.ordinal(),
						NETWORK_DELAY_TYPES.WAN_DELAY);
			}
		}
		else if(nextHopId == SimSettings.GENERIC_EDGE_DEVICE_ID) {
			double WlanDelay = networkModel.getUploadDelay(kernel.getMobileDeviceId(), nextHopId, kernel);
			
			if(WlanDelay > 0){
				networkModel.uploadStarted(currentLocation, nextHopId);
				schedule(getId(), WlanDelay, REQUEST_RECIVED_BY_EDGE_DEVICE, kernel);
				SimLogger.getInstance().kernelStarted(kernel.getCloudletId(), CloudSim.clock());
				SimLogger.getInstance().setUploadDelay(kernel.getCloudletId(), WlanDelay, NETWORK_DELAY_TYPES.WLAN_DELAY);
			}
			else {
				SimLogger.getInstance().rejectedDueToBandwidth(
						kernel.getCloudletId(),
						CloudSim.clock(),
						SimSettings.VM_TYPES.EDGE_VM.ordinal(),
						NETWORK_DELAY_TYPES.WLAN_DELAY);
			}
		}
		else {
			SimLogger.printLine("Unknown nextHopId! Terminating simulation...");
			System.exit(0);
		}
	}
	
	private void submitTaskToVm(Kernel task, double delay, int datacenterId) {
		//select a VM
		Vm selectedVM = SimManager.getInstance().getEdgeOrchestrator().getVmToOffload(task, datacenterId);
		
		int vmType = 0;
		if(datacenterId == SimSettings.CLOUD_DATACENTER_ID)
			vmType = SimSettings.VM_TYPES.CLOUD_VM.ordinal();
		else
			vmType = SimSettings.VM_TYPES.EDGE_VM.ordinal();
		
		if(selectedVM != null){
			if(datacenterId == SimSettings.CLOUD_DATACENTER_ID)
				task.setAssociatedDatacenterId(SimSettings.CLOUD_DATACENTER_ID);
			else
				task.setAssociatedDatacenterId(selectedVM.getHost().getDatacenter().getId());

			//save related host id
			task.setAssociatedHostId(selectedVM.getHost().getId());
			
			//set related vm id
			task.setAssociatedVmId(selectedVM.getId());
			
			//bind task to related VM
			getCloudletList().add(task);
			bindCloudletToVm(task.getCloudletId(),selectedVM.getId());
			
			//SimLogger.printLine(CloudSim.clock() + ": Cloudlet#" + task.getCloudletId() + " is submitted to VM#" + task.getVmId());
			schedule(getVmsToDatacentersMap().get(task.getVmId()), delay, CloudSimTags.CLOUDLET_SUBMIT, task);

			SimLogger.getInstance().kernelAssigned(task.getCloudletId(),
					selectedVM.getHost().getDatacenter().getId(),
					selectedVM.getHost().getId(),
					selectedVM.getId(),
					vmType);
		}
		else{
			//SimLogger.printLine("Task #" + task.getCloudletId() + " cannot assign to any VM");
			SimLogger.getInstance().rejectedDueToVMCapacity(task.getCloudletId(), CloudSim.clock(), vmType);
		}
	}
	
	private Kernel createKernel(KernelProperty kernelProperty){
		UtilizationModel utilizationModel = new UtilizationModelFull(); /*UtilizationModelStochastic*/
		UtilizationModel utilizationModelCPU = getCpuUtilizationModel();

		Kernel kernel = new Kernel(kernelProperty.getMobileDeviceId(), ++taskIdCounter,
				kernelProperty.getLength(), kernelProperty.getPesNumber(),
				kernelProperty.getInputFileSize(), kernelProperty.getOutputFileSize(),
				utilizationModelCPU, utilizationModel, utilizationModel);
		
		//set the owner of this kernel
		kernel.setUserId(this.getId());
		kernel.setKernelType(kernelProperty.getApplicationType());
		kernel.setKernelId(kernelProperty.getKernelId());
		
		if (utilizationModelCPU instanceof CpuUtilizationModel_Custom) {
			((CpuUtilizationModel_Custom)utilizationModelCPU).setTask(kernel);
		}
		
		return kernel;
	}
}
