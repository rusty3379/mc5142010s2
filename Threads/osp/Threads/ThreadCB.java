/*
 * Group 12
 * RA: 071552
 * RA: 080664
 * RA: 084294
 *
 * Status: Under Devel
 *
 * 09/16/10
 * 1. Implementation of do_create still not working properly
 * 09/23/10
 * 1. Only do_dispatch not implemented
 */

package osp.Threads;

import java.util.ArrayList;
//import java.util.Vector;
//import java.util.Enumeration;

import osp.Utilities.*;
import osp.IFLModules.*;
import osp.Tasks.*;
//import osp.EventEngine.*;
import osp.Hardware.*;
import osp.Devices.*;
import osp.Memory.*;
import osp.Resources.*;

/**
 * This class is responsible for actions related to threads, including creating,
 * killing, dispatching, resuming, and suspending threads.
 * 
 * @OSPProject Threads
 */
public class ThreadCB extends IflThreadCB {
	// Arraylist that will contains all threads
	private static ArrayList<ThreadCB> listThreads;

	/**
	 * The thread constructor. Must call
	 * 
	 * super();
	 * 
	 * as its first statement.
	 * 
	 * @OSPProject Threads
	 */
	public ThreadCB() {
		super();
	}

	/**
	 * This method will be called once at the beginning of the simulation. The
	 * student can set up static variables here.
	 * 
	 * @OSPProject Threads
	 */
	public static void init() {
		// Instantiate the static list that contains all threads
		listThreads = new ArrayList<ThreadCB>();
	}

	/**
	 * Sets up a new thread and adds it to the given task. The method must set
	 * the ready status and attempt to add thread to task. If the latter fails
	 * because there are already too many threads in this task, so does this
	 * method, otherwise, the thread is appended to the ready queue and
	 * dispatch() is called.
	 * 
	 * The priority of the thread can be set using the getPriority/setPriority
	 * methods. However, OSP itself doesn't care what the actual value of the
	 * priority is. These methods are just provided in case priority scheduling
	 * is required.
	 * 
	 * @return thread or null
	 * @OSPProject Threads
	 */
	public static ThreadCB do_create(TaskCB task) {

		if (task.getThreadCount() >= MaxThreadsPerTask) {
			// Cannot insert more threads, maxthreadspertask reached
			dispatch();
			return null;
		}

		ThreadCB newThreadCB = new ThreadCB();

		// set the priority and the status
		newThreadCB.setPriority(task.getPriority());
		newThreadCB.setStatus(GlobalVariables.ThreadReady);

		newThreadCB.setTask(task);
		// Verify if the thread can be add to the task
		if (task.addThread(newThreadCB) != GlobalVariables.SUCCESS) {
			dispatch();
			return null;
		}

		// add to the control list
		listThreads.add(newThreadCB);

		dispatch();
		// Still not working WHY??
		return newThreadCB;
	}

	/**
	 * Kills the specified thread.
	 * 
	 * The status must be set to ThreadKill, the thread must be removed from the
	 * task's list of threads and its pending IORBs must be purged from all
	 * device queues.
	 * 
	 * If some thread was on the ready queue, it must removed, if the thread was
	 * running, the processor becomes idle, and dispatch() must be called to
	 * resume a waiting thread.
	 * 
	 * @OSPProject Threads
	 */
	public void do_kill() {
		// Gets the task of the thread
		TaskCB task = getTask();

		// If the thread is ready just remove it
		if (getStatus() == GlobalVariables.ThreadReady) {
			if (listThreads.remove(this) == false) {
				return;
			}
		}
		//Remove the the task from processor if it is the running task
		if(getStatus() == GlobalVariables.ThreadRunning)
		{
			MMU.getPTBR().getTask().setCurrentThread(null);
		}
		// remove the thread from task
		if (task.removeThread(this) != GlobalVariables.SUCCESS) {
			return;
		}
		
		setStatus(GlobalVariables.ThreadKill);

		for (int i = 0; i < Device.getTableSize(); i++) {
			Device.get(i).cancelPendingIO(this);
		}

		// Make the thread give up for resources
		ResourceCB.giveupResources(this);
		
		dispatch();

		if (task.getThreadCount() == 0) {
			task.kill();
		}
	}

	/**
	 * Suspends the thread that is currenly on the processor on the specified
	 * event.
	 * 
	 * Note that the thread being suspended doesn't need to be running. It can
	 * also be waiting for completion of a pagefault and be suspended on the
	 * IORB that is bringing the page in.
	 * 
	 * Thread's status must be changed to ThreadWaiting or higher, the processor
	 * set to idle, the thread must be in the right waiting queue, and
	 * dispatch() must be called to give CPU control to some other thread.
	 * 
	 * @param event
	 *            - event on which to suspend this thread.
	 * @OSPProject Threads
	 */
	public void do_suspend(Event event) {
		int currentStatus = this.getStatus();

		// Set the new status for the thread
		if (currentStatus == GlobalVariables.ThreadRunning) 
		{
			setStatus(GlobalVariables.ThreadWaiting);
			this.getTask().setCurrentThread(null);
		}
		else if(currentStatus >= GlobalVariables.ThreadWaiting)
		{
			setStatus(currentStatus + 1);
		}
		listThreads.remove(this);
		// Add this thread to event queue
		event.addThread(this);

		dispatch();
	}

	/**
	 * Resumes the thread.
	 * 
	 * Only a thread with the status ThreadWaiting or higher can be resumed. The
	 * status must be set to ThreadReady or decremented, respectively. A ready
	 * thread should be placed on the ready queue.
	 * 
	 * @OSPProject Threads
	 */
	public void do_resume() {
		int currentStatus = getStatus();
		// if thread is waiting set as ready
		if (currentStatus == GlobalVariables.ThreadWaiting) {
			setStatus(GlobalVariables.ThreadReady);
			listThreads.add(this);
		} else {
			setStatus(currentStatus - 1);
		}

		dispatch();
	}

	/**
	 * Selects a thread from the run queue and dispatches it.
	 * 
	 * If there is just one theread ready to run, reschedule the thread
	 * currently on the processor.
	 * 
	 * In addition to setting the correct thread status it must update the PTBR.
	 * 
	 * @return SUCCESS or FAILURE
	 * @OSPProject Threads
	 */
	public static int do_dispatch() {
		// Local variables
		TaskCB currentTaskCB = null;
		ThreadCB currentThreadCB = null;
		ThreadCB newThreadCB = null;

		// Getting the current thread to be stopped.
		try {
			currentTaskCB = MMU.getPTBR().getTask();
			currentThreadCB = currentTaskCB.getCurrentThread();
		} catch (Exception e) {
		}

		if (currentThreadCB != null) {
			// To dispatch a new thread, first we need to remove the current
			// thread from the device.
			currentTaskCB.setCurrentThread(null);
			// Set the stopped thread as ready
			currentThreadCB.setStatus(ThreadReady);
			// Remove thread page from MMU
			MMU.setPTBR(null);
			// Put the stopped thread in the array.
			listThreads.add(currentThreadCB);
		}
		// Get the first thread in the array
		if (listThreads.size() > 0) {
			newThreadCB = listThreads.remove(0);
			// Set the page file to be the current thread page
			MMU.setPTBR(newThreadCB.getTask().getPageTable());
			// Set the task's thread as the new thread
			newThreadCB.getTask().setCurrentThread(newThreadCB);

			newThreadCB.setStatus(ThreadRunning);

			// Set the time quantum
			HTimer.set(100);

			return GlobalVariables.SUCCESS;
		}
		// no thread to be executed, we must clean PTBR
		MMU.setPTBR(null);
		return GlobalVariables.FAILURE;

	}

	/**
	 * Called by OSP after printing an error message. The student can insert
	 * code here to print various tables and data structures in their state just
	 * after the error happened. The body can be left empty, if this feature is
	 * not used.
	 * 
	 * @OSPProject Threads
	 */
	public static void atError() {
		// your code goes here

	}

	/**
	 * Called by OSP after printing a warning message. The student can insert
	 * code here to print various tables and data structures in their state just
	 * after the warning happened. The body can be left empty, if this feature
	 * is not used.
	 * 
	 * @OSPProject Threads
	 */
	public static void atWarning() {
		// your code goes here

	}

	/*
	 * Feel free to add methods/fields to improve the readability of your code
	 */

}

/*
 * Feel free to add local classes to improve the readability of your code
 */
