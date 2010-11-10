package osp.Resources;

import java.util.*;

import javax.annotation.Resource;

import osp.IFLModules.*;
import osp.Tasks.*;
import osp.Threads.*;
import osp.Utilities.*;
import osp.Memory.*;

/**
    Class ResourceCB is the core of the resource management module.
    Students implement all the do_* methods.
    @OSPProject Resources
*/
public class ResourceCB extends IflResourceCB
{
	private static int resourceCounter = 0;
	private static Hashtable HashT = new Hashtable();
	private static RRB resource = new RRB(null, null, 0);
    /**
       Creates a new ResourceCB instance with the given number of 
       available instances. This constructor must have super(qty) 
       as its first statement.

       @OSPProject Resources
    */
    public ResourceCB(int qty)
    {
    	super(qty);

    }

    /**
       This method is called once, at the beginning of the
       simulation. Can be used to initialize static variables.

       @OSPProject Resources
    */
    public static void init()
    {
    	resourceCounter = ResourceTable.getResourceCB();

    }

    /**
       Tries to acquire the given quantity of this resource.
       Uses deadlock avoidance or detection depending on the
       strategy in use, as determined by ResourceCB.getDeadlockMethod().

       @param quantity
       @return The RRB corresponding to the request.
       If the request is invalid (quantity+allocated>total) then return null.

       @OSPProject Resources
    */
    public RRB  do_acquire(int quantity) 
    {
        ThreadCB currentThread = null;
        TaskCB currentTask = null;
        
        try{
        	currentTask = MMU.getPTBR().getTask();
        	currentThread = currentTask.getCurrentThread();
        }

        catch (NullPointerException localNullPointerException){}
        
        if(quantity + getAllocated(currentThread) > getTotal())
        	return null;
        
        if(!HashT.containsKey(currentThread))
        	HashT.put(currentThread, resource);
        
        RRB currentResource = new RRB(currentThread, this, quantity);
        
        if( == GlobalVariables.Denied){
        	if( == 2){
        		currentResource.
        	}
        	if((currentResource.  == 1) && (!HashT.containsValue(currentResource))){
        		HashT.put(currentThread, currentResource);
        	}
        }
    
        if( == GlobalVariables.Suppend){
        	if(quantity <= getAvailable()){
        		currentResource.
        	}
        	else{
        		if(currentThread.getStatus() != ThreadWaiting){
        			currentResource.setStatus(GlobalVariables.Suspend);
        			currentThread.suspend(currentResource);
        		}
        		
        		if(!HashT.countainsValue(currentResource)){
        			HashT.put(currentThread, currentResource);
        		}
        	}
        }
        
        return currentResource;
    }

    /**
       Performs deadlock detection.
       @return A vector of ThreadCB objects found to be in a deadlock.

       @OSPProject Resources
    */
    public static Vector do_deadlockDetection()
    {
       

    }

    /**
       When a thread was killed, this is called to release all
       the resources owned by that thread.

       @param thread -- the thread in question

       @OSPProject Resources
    */
    public static void do_giveupResources(ThreadCB thread)
    {
    	int counter = 0;
    	
        if(!HashT.containsKey(thread))
        	return;
        
        while(counter < resourceCounter){
        	Resource currentResource = ResourceTable.getResourceCB(counter);
        	if(currentResource.  != 0){
        		currentResource.
        	}
        	
        	currentResource.
        	counter++;
        }
        
        HashT.remove(thread);
        
        RRB newRRB = null;
        

    }

    /**
        Release a previously acquired resource.

	@param quantity

        @OSPProject Resources
    */
    public void do_release(int quantity)
    {
        // your code goes here

    }

    /** Called by OSP after printing an error message. The student can
	insert code here to print various tables and data structures
	in their state just after the error happened.  The body can be
	left empty, if this feature is not used.
	
	@OSPProject Resources
    */
    public static void atError()
    {
        // your code goes here

    }

    /** Called by OSP after printing a warning message. The student
	can insert code here to print various tables and data
	structures in their state just after the warning happened.
	The body can be left empty, if this feature is not used.
     
	@OSPProject Resources
    */
    public static void atWarning()
    {
        // your code goes here

    }


    /*
       Feel free to add methods/fields to improve the readability of your code
    */

}

/*
      Feel free to add local classes to improve the readability of your code
*/
