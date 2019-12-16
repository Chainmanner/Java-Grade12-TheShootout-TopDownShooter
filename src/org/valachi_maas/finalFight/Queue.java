package org.valachi_maas.finalFight;

import java.util.ArrayList;

/**
 * Taken straight from Unit 5, Exercise: "Queue 3".
 * Modified to make sure it works properly, in the context of this program.
 * @author Gabriel Valachi
 * @author Anthony Maas.
 */
public class Queue
{ 
	private ArrayList<Object> data;	//MOD: Uses an ArrayList instead.
	private int front, rear; //MOD: Removed maxSize. No need for it now.

	/** 
	 * constructor 
	 * pre: none 
	 * post: An empty queue that can hold up to maxItems 
	 * has been created. 
	 */
	public Queue()
	{
		data = new ArrayList<Object>();
		front = -1; 	//no items in the array 
		rear = -1;
	} 

	/** 
	 * Returns the front item without removing it from 
	 * the queue. 
	 * pre: Queue contains at least one item. 
	 * post: The front item has been returned while leaving 
	 * it in the queue. 
	 */
	public Object front() 
	{
		if ( front < 0 )
			return null;
		
		return data.get(front);
	}

	/**
	 * Removes the front item from the queue and returns it. 
	 * pre: Queue contains at least one item. 
	 * post: The front item of the queue has been removed 
	 * and returned. 
	 */

	public Object dequeue() 
	{
		Object obj; 
		obj = data.get(front); 	//get front item
		//if dequeueing last item then make empty
		if (front == rear) 
		{ 
			makeEmpty(); 
		}
		else 
		{ 
			// move pointer to next item 
			front = (front + 1);
		}
		return(obj);
	}

	/**
	 * Adds an item to the queue if there is room. 
	 * pre: none 
	 * post: A new item has been added to the queue. 
	 */ 
	public void enqueue(Object obj) 
	{
		if (isEmpty()) 
		{		//first item queued 
			rear = 0;
			front = 0;
			data.add(rear, obj); 
		}
		else 
		{ 
			rear = (rear + 1); 
			data.add(rear, obj);
		}
	}

	/** 
	 * Determines if there are items on the queue. 
	 * pre: none 
	 * post: true returned if there are items on the queue, 
	 * false returned otherwise. 
	 */ 
	public boolean isEmpty() 
	{ 
		if (front == -1 && rear == -1)
		{ 
			return(true);
		}
		else 
		{ 
			return(false);
		}
	}

	/** 
	 * Returns the number of items in the queue. 
	 * pre: none 
	 * post: The number of items in the queue is returned. 
	 */ 
	public int size() 
	{ 
		if (isEmpty()) 
		{ 
			return(0); 
		}
		
		//return data.size();
		else 
		{ 	//front item is “in front” of rear item 
			if (rear > front) 
			{ 
				return(rear - front + 1);
			}
			else 
			{ 	//front item is “behind” rear item 
				return(front - rear + 1);
			}
		}
	}

	/** 
	 * Empties the queue. 
	 * pre: none 
	 * post: There are no items in the queue. 
	 */ 
	public void makeEmpty()
	{
		front = -1; 
		rear = -1;
	}
}