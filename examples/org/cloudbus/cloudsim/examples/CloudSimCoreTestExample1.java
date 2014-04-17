package org.cloudbus.cloudsim.examples;

/*
 * Title:        CloudSim Toolkit
 * Description:  CloudSim (Cloud Simulation) Toolkit for Modeling and Simulation
 *               of Clouds
 * Licence:      GPL - http://www.gnu.org/copyleft/gpl.html
 *
 * Copyright (c) 2009, The University of Melbourne, Australia
 */

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import org.cloudbus.cloudsim.Cloudlet;
import org.cloudbus.cloudsim.CloudletSchedulerTimeShared;
import org.cloudbus.cloudsim.Datacenter;
import org.cloudbus.cloudsim.DatacenterBroker;
import org.cloudbus.cloudsim.DatacenterCharacteristics;
import org.cloudbus.cloudsim.Host;
import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.Pe;
import org.cloudbus.cloudsim.Storage;
import org.cloudbus.cloudsim.UtilizationModel;
import org.cloudbus.cloudsim.UtilizationModelFull;
import org.cloudbus.cloudsim.Vm;
import org.cloudbus.cloudsim.VmAllocationPolicySimple;
import org.cloudbus.cloudsim.VmSchedulerSpaceShared;
import org.cloudbus.cloudsim.VmSchedulerTimeShared;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.lists.CloudletList;
import org.cloudbus.cloudsim.provisioners.BwProvisionerSimple;
import org.cloudbus.cloudsim.provisioners.PeProvisionerSimple;
import org.cloudbus.cloudsim.provisioners.RamProvisionerSimple;

/**
 * A simple example showing how to create a datacenter with one host and run one
 * cloudlet on it.
 */
public class CloudSimCoreTestExample1 {

	/** The cloudlet list. */
	private static List<Cloudlet> cloudletList;

	/** The vmlist. */
	private static List<Vm> vmlist;

	/**
	 * Creates main() to run this example.
	 * 
	 * @param args
	 *            the args
	 */
	public static void main(String[] args) {

		Log.printLine("Starting CloudSimExample1...");

		try {
			// First step: Initialize the CloudSim package. It should be called
			// before creating any entities.
			int num_user = 1; // number of cloud users
			Calendar calendar = Calendar.getInstance();
			boolean trace_flag = false; // mean trace events

			// Initialize the CloudSim library
			CloudSim.init(num_user, calendar, trace_flag);

			// Second step: Create Datacenters
			// Datacenters are the resource providers in CloudSim. We need at
			// list one of them to run a CloudSim simulation
			long startTime = System.currentTimeMillis();
			Runtime runtime = Runtime.getRuntime();
			long initMemCost = runtime.totalMemory() - runtime.freeMemory();
			Datacenter datacenter0 = createDatacenter("Datacenter_0");
			long runMemCost = runtime.totalMemory() - runtime.freeMemory();
			long endTime = System.currentTimeMillis();
			System.out.println("create datacenter has expensed "
					+ (runMemCost - initMemCost) / 1024 + " KBs");
			System.out.println("create datacenter has expensed "
					+ (endTime - startTime) + " ms");
			// Third step: Create Broker
			DatacenterBroker broker = createBroker();
			int brokerId = broker.getId();

			// Fourth step: Create one virtual machine
			vmlist = new ArrayList<Vm>();

			// VM description
			int vmid = 0;
			int mips = 1200;
			long size = 1024 * 0124; // image size (MB)
			int ram = 1024; // vm memory (MB)
			long bw = 1000;
			int pesNumber = 1; // number of cpus
			String vmm = "Xen"; // VMM name

			int vmNum = 50;
			for (int i = 0; i < vmNum; i++) {
				// create VM
				Vm vm = new Vm(vmid, brokerId, mips, pesNumber, ram, bw, size,
						vmm, new CloudletSchedulerTimeShared());

				// add the VM to the vmList
				vmlist.add(vm);
				vmid++;
			}
			// submit vm list to the broker
			broker.submitVmList(vmlist);

			// Fifth step: Create one Cloudlet
			cloudletList = new ArrayList<Cloudlet>();

			// Cloudlet properties
			int id = 0;
			long length = 1440000;
			long fileSize = 300;
			long outputSize = 300;
			UtilizationModel utilizationModel = new UtilizationModelFull();

			int CloudletNum = 300;
			MonitorThread monitor = new MonitorThread(cloudletList);
			for (int i = 0; i < CloudletNum; i++) {
				monitor.start();
				if (i % 49 == 0) {
					try {
						broker.submitCloudletList(cloudletList);
						Thread.sleep(1000);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
				Cloudlet cloudlet = new Cloudlet(id, length, pesNumber,
						fileSize, outputSize, utilizationModel,
						utilizationModel, utilizationModel);
				cloudlet.setUserId(brokerId);
				// cloudlet.setVmId(vmid);

				// add the cloudlet to the list
				cloudletList.add(cloudlet);
				// broker.bindCloudletToVm(cloudletList.get(i).getCloudletId(),((int)(
				// Math.random() * 3276)) % vmNum);
				id++;
			}
			/*
			 * for (int i = CloudletNum - 1; i >= 0; i--) { if (i % 50 == 0) {
			 * Thread.sleep(1 1000 * 60 * 10 ); }
			 * 
			 * // Log.printLine("id:"+(int) (Math.random() * 3276)); //
			 * broker.bindCloudletToVm
			 * (cloudletList.get(i).getCloudletId(),((int) // Math.random() *
			 * 3276) % vmNum); cloudletList.get(i).setVmId( ((int)
			 * (Math.random() * 32765)) % vmNum); }
			 */
			// submit cloudlet list to the broker
			broker.submitCloudletList(cloudletList);
		
			// Sixth step: Starts the simulation
			CloudSim.startSimulation();
			Log.printLine("Begin simulation");

			CloudSim.stopSimulation();
			//monitor.stop(obj)
			//monitor.destroy();
			monitor.allDone = true;

			// Final step: Print results when simulation is over
			List<Cloudlet> newList = broker.getCloudletReceivedList();
			printCloudletList(newList);

			// Print the debt of each user to each datacenter
			datacenter0.printDebts();

			Log.printLine("CloudSimExample1 finished!");
		} catch (Exception e) {
			e.printStackTrace();
			Log.printLine("Unwanted errors happen");
		}
	}

	/**
	 * Creates the datacenter.
	 * 
	 * @param name
	 *            the name
	 * 
	 * @return the datacenter
	 */
	private static Datacenter createDatacenter(String name) {

		// Here are the steps needed to create a PowerDatacenter:
		// 1. We need to create a list to store
		// our machine
		List<Host> hostList = new ArrayList<Host>();

		// 2. A Machine contains one or more PEs or CPUs/Cores.
		// In this example, it will have only one core.
		List<Pe> peList = new ArrayList<Pe>();

		int mips = 1200;

		// 3. Create PEs and add these into a list.
		peList.add(new Pe(0, new PeProvisionerSimple(mips))); // need to store
																// Pe id and
																// MIPS Rating

		// 4. Create Host with its id and list of PEs and add them to the list
		// of machines
		int hostId = 0;
		int ram = 2048; // host memory (MB)
		long storage = 2 * 1024 * 1024; // host storage,2Terabytes
		int bw = 10000;

		// the For expression just is to evaluate the experiment of varing Num
		// Of Hosts
		int HostNum = 100000;
		for (int i = 0; i < HostNum; i++) {
			hostList.add(new Host(hostId, new RamProvisionerSimple(ram),
					new BwProvisionerSimple(bw), storage, peList,
					new VmSchedulerSpaceShared(peList))); // This is our machine
			hostId++;
		}

		// 5. Create a DatacenterCharacteristics object that stores the
		// properties of a data center: architecture, OS, list of
		// Machines, allocation policy: time- or space-shared, time zone
		// and its price (G$/Pe time unit).
		String arch = "x86"; // system architecture
		String os = "Linux"; // operating system
		String vmm = "Xen";
		double time_zone = 10.0; // time zone this resource located
		double cost = 3.0; // the cost of using processing in this resource
		double costPerMem = 0.05; // the cost of using memory in this resource
		double costPerStorage = 0.001; // the cost of using storage in this
										// resource
		double costPerBw = 0.0; // the cost of using bw in this resource
		LinkedList<Storage> storageList = new LinkedList<Storage>(); // we are
																		// not
																		// adding
																		// SAN
		// devices by now

		DatacenterCharacteristics characteristics = new DatacenterCharacteristics(
				arch, os, vmm, hostList, time_zone, cost, costPerMem,
				costPerStorage, costPerBw);

		// 6. Finally, we need to create a PowerDatacenter object.
		Datacenter datacenter = null;
		try {
			datacenter = new Datacenter(name, characteristics,
					new VmAllocationPolicySimple(hostList), storageList, 0);
		} catch (Exception e) {
			e.printStackTrace();
		}

		return datacenter;
	}

	// We strongly encourage users to develop their own broker policies, to
	// submit vms and cloudlets according
	// to the specific rules of the simulated scenario
	/**
	 * Creates the broker.
	 * 
	 * @return the datacenter broker
	 */
	private static DatacenterBroker createBroker() {
		DatacenterBroker broker = null;
		try {
			broker = new DatacenterBroker("Broker");
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
		return broker;
	}

	/**
	 * Prints the Cloudlet objects.
	 * 
	 * @param list
	 *            list of Cloudlets
	 */
	private static void printCloudletList(List<Cloudlet> list) {
		int size = list.size();
		Cloudlet cloudlet;

		String indent = "    ";
		Log.printLine();
		Log.printLine("========== OUTPUT ==========");
		Log.printLine("Cloudlet ID" + indent + "STATUS" + indent
				+ "Data center ID" + indent + "VM ID" + indent + "Time"
				+ indent + "Start Time" + indent + "Finish Time");

		DecimalFormat dft = new DecimalFormat("###.##");
		for (int i = 0; i < size; i++) {
			cloudlet = list.get(i);
			Log.print(indent + cloudlet.getCloudletId() + indent + indent);

			if (cloudlet.getCloudletStatus() == Cloudlet.SUCCESS) {
				Log.print("SUCCESS");
				Log.printLine(indent + indent + cloudlet.getResourceId()
						+ indent + indent + indent + cloudlet.getVmId()
						+ indent + indent
						+ dft.format(cloudlet.getActualCPUTime()) + indent
						+ indent + dft.format(cloudlet.getExecStartTime())
						+ indent + indent
						+ dft.format(cloudlet.getFinishTime()));
			}
		}
	}
	
}

class MonitorThread extends Thread {
	public MonitorThread(List<Cloudlet> taskList){
		cloudletList = taskList;
	}
	volatile boolean allDone = false;
	protected List<Cloudlet> cloudletList;
	public void run() {
		int NotSent = 0;
		int Submitted = 0;
		int Finished = 0;
		int size = cloudletList.size();
		try {
			while (!allDone) {
				for (int i = 0; i < size; i++) {
					Cloudlet task = cloudletList.get(i);
					if (task.getCloudletStatus() == Cloudlet.CREATED) {
						NotSent++;
					}
					if (task.getCloudletStatus() == Cloudlet.QUEUED) {
						Submitted++;
					}
					if (task.getCloudletStatus() == Cloudlet.SUCCESS) {
						Finished++;
					}
				}
				String indent = "	";
				Log.printLine("Cloudlet" + indent + "Not sent" + indent
						+ "Submitted" + indent + "SUCCESS");
				Log.printLine(indent + indent + NotSent + indent + indent
						+ Submitted + indent + indent + Finished + indent);
				sleep(5000);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

}