package edu.sjsu.cmpe.cmpe283;

import java.net.URL;

import com.vmware.vim25.TaskInfoState;
import com.vmware.vim25.VirtualMachineCapability;
import com.vmware.vim25.VirtualMachineCloneSpec;
import com.vmware.vim25.VirtualMachineConfigInfo;
import com.vmware.vim25.VirtualMachineMovePriority;
import com.vmware.vim25.VirtualMachineRelocateSpec;
import com.vmware.vim25.VirtualMachineRuntimeInfo;
import com.vmware.vim25.mo.Datacenter;
import com.vmware.vim25.mo.Folder;
import com.vmware.vim25.mo.HostSystem;
import com.vmware.vim25.mo.InventoryNavigator;
import com.vmware.vim25.mo.ManagedEntity;
import com.vmware.vim25.mo.ServiceInstance;
import com.vmware.vim25.mo.Task;
import com.vmware.vim25.mo.VirtualMachine;

public class HelloVMFolder {
	public static String[] getSplitNames(String vmFolderPAth) {
		String dataCenterName = null;
		String[] splitName = vmFolderPAth.split("/");
		return splitName;

	}

	public static void main(String[] args) throws Exception

	{
		String ip = "https://" + args[0] + "/sdk";
		String login = args[1]; // cmpe283_sec5_student@vsphere.local
		String password = args[2]; // cmpe283-agc3
		String vmFolderPath = args[3]; //  ""
		Folder vmFolder = null;
		ServiceInstance si = new ServiceInstance(new URL(ip), login, password, true);
		Folder rootFolder = si.getRootFolder();
		String[] splitName = getSplitNames(vmFolderPath);
		System.out.println("args[3]= " + args[3]);
		Datacenter dataCenter = (Datacenter) si.getSearchIndex().findByInventoryPath(splitName[0]);
		System.out.println("Datacenter folder= " + dataCenter.getName());
		String name = rootFolder.getName();
		StringBuilder newPath = new StringBuilder();
		for (String split : splitName) {

			newPath.append(split).append("/");
			ManagedEntity newManagedEntity = si.getSearchIndex().findByInventoryPath(newPath.toString());

			if (newManagedEntity.getMOR().getType().equals("Datacenter")) {
				dataCenter = (Datacenter) newManagedEntity;
				newPath.append("vm/");
			} else if (newManagedEntity.getMOR().getType().equals("Folder")) {
				vmFolder = (Folder) newManagedEntity;
			}
		}
		System.out.println("vm folder= " + vmFolder.getName());
		ManagedEntity[] mes = new InventoryNavigator(vmFolder).searchManagedEntities("VirtualMachine");
		ManagedEntity[] hosts = new InventoryNavigator(dataCenter).searchManagedEntities("HostSystem");
		String hostName;
		HostSystem host;
		if (mes == null || mes.length == 0) {

			return;

		}
		System.out.println("****************** HOSTS ******************");
		for (int j = 0; j < hosts.length; j++) {

			System.out.println("Host [" + j + "] ");

			HostSystem hostObj = (HostSystem) hosts[j];

			System.out.println("Name = " + hostObj.getName());

			System.out.println("Product Full Name = " +

			hostObj.getConfig().getProduct().getFullName());

		}

		for (int i = 0; i < mes.length; i++) {
			// System.out.println("VM [ " + i + " ] ");
			VirtualMachine vm = (VirtualMachine) mes[i];

			VirtualMachineRuntimeInfo vmRuntime = vm.getRuntime();

			VirtualMachineConfigInfo vminfo = vm.getConfig();

			VirtualMachineCapability vmc = vm.getCapability();

			host = new HostSystem(vm.getServerConnection(), vm.getRuntime().getHost());
			hostName = host.getName();
			// Virtual Machine Details
			System.out.println("****************** Virtual Machine Status ******************");
			System.out.println("VM Name: " + vm.getName());

			System.out.println("GuestOS: " + vminfo.getGuestFullName());

			System.out.println("GuestOS State: " + vm.getGuest().guestState);

			System.out.println("GuestOS State: " + vmRuntime.powerState);

			System.out.println("Esxi Host: " + hostName);

			// Create snapshot Task
			System.out.println("******************Virtual Machine Snapshot Task******************");
			String vmSnapshotName = vm.getName() + "- Snapshot";
			String vmSnapshotDescription = vm.getName() + " created at" + System.currentTimeMillis();
			System.out.print("task: ");
			Task task = vm.createSnapshot_Task(vmSnapshotName, vmSnapshotDescription, false, false);
			task.waitForTask();
			System.out.print("target = " + vm.getName());
			System.out.print(", op = " + task.getTaskInfo().getName());
			System.out.print(", startTime = " + task.getTaskInfo().getStartTime().getTime());
			System.out.println(", End time: " + task.getTaskInfo().getCompleteTime().getTime());
			String status = task.waitForTask();
			if (status.equals(task.SUCCESS)) {
				System.out.println("Status = " + task.getTaskInfo().getState());
			} else {
				System.out.println("Status = " + task.getTaskInfo().getError().toString());
			}
			//
			// Virtual Machine Clone task.
			System.out.println("******************Virtual Machine Clone Task******************");
			String cloneName = vm.getName() + " clone";
			VirtualMachineCloneSpec cloneSpec = new VirtualMachineCloneSpec();
			cloneSpec.setLocation(new VirtualMachineRelocateSpec());
			cloneSpec.setPowerOn(false);
			cloneSpec.setTemplate(false);
			System.out.print("task: ");
			Task CloneTask = vm.cloneVM_Task((Folder) vm.getParent(), cloneName, cloneSpec);
			System.out.print("target = " + vm.getName());
			System.out.print(", op = " + CloneTask.getTaskInfo().getName());
			System.out.print(", startTime = " + CloneTask.getTaskInfo().getStartTime().getTime());
			String Clonestatus = CloneTask.waitForTask();
			System.out.print(" End time: " + CloneTask.getTaskInfo().getCompleteTime().getTime() + "\n");
			if (Clonestatus == Task.SUCCESS) {
				System.out.println("Status = " + CloneTask.getTaskInfo().getState());
			} else {
				System.out.println("Status = " + CloneTask.getTaskInfo().getError().getLocalizedMessage());
			}

			// Migrate Virtual machine
			System.out.println("******************Virtual Machine Migration Task****************** ");
			if (hosts.length <= 1) {
				System.out.println("Error: Only one Host is present");
			} else {
				for (int j = 0; j < hosts.length; j++) {
					HostSystem host_1 = (HostSystem) hosts[j];
					HostSystem host_2;
					if (host_1.getName().equals(hostName)) {
						if (j == hosts.length) {
							host_2 = (HostSystem) hosts[0];
						} else {
							host_2 = (HostSystem) hosts[j + 1];
						}
						Task migrationTask = vm.migrateVM_Task(vm.getResourcePool(), host_2,
								VirtualMachineMovePriority.highPriority, vm.getRuntime().powerState);
						System.out.print("target = " + vm.getName());
						System.out.print(", op = " + migrationTask.getTaskInfo().getName());
						System.out.print(", startTime = " + migrationTask.getTaskInfo().getStartTime().getTime());
						String migrateStatus = migrationTask.waitForTask();
						System.out.println(
								", End time: " + migrationTask.getTaskInfo().getCompleteTime().getTime() + "\n");
						if (migrateStatus == Task.SUCCESS) {
							System.out.print("Status =  " + migrationTask.getTaskInfo().getState());
							break;
						} else {
							System.out
									.print("Status =  " + migrationTask.getTaskInfo().getError().getLocalizedMessage());
							break;
						}
					}
				}
			}

		}

	}
}
