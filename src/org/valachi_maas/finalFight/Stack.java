package org.valachi_maas.finalFight;

import java.util.ArrayList;

/**
 * Taken straight from Unit 5, Exercise: "Stack 3".
 * Modified to make sure that it works properly, in the context of this program.
 * @author Gabriel Valachi
 * @author Anthony Maas
 */
public class Stack
{
	private ArrayList<Object> data;	//MOD: Uses an ArrayList instead of an array.
	private int top;
	
	/** 
	 * constructor 
	 * pre: none 
	 * post: An empty stack that can hold up to maxItems 
	 * has been created. 
	 */
	public Stack()
	{
		data = new ArrayList<Object>();
		top = -1;	//no items in the array
	}
	
	/** 
	 * Returns the top item without removing it from the stack. 
	 * pre: Stack contains at least one item. 
	 * post: The top item has been returned while leaving it 
	 * on the stack. 
	 */ 
	public Object top() 
	{ 
		if (top < 0)
			return null;
		
		return data.get(top); 
	}
	
	/** 
	 * Removes the top item from the stack and returns it. 
	 * pre: Stack contains at least one item. 
	 * post: The top item of the stack has been removed 
	 * and returned. 
	 */ 
	public Object pop()
	{
		if ( top < 0 )
			return null;
		
		Object thing = data.get(top);
		data.remove(top);
		top--;
		return thing;
	}
	
	/** 
	 * Adds an item to the top of the stack if there is room. 
	 * pre: none 
	 * post: A new item has been added to the top of the stack. 
	 */ 
	public void push(Object something)
	{ 
		top++;
		data.add(top, something);
	}
	
	/** 
	 * Determines if there are items on the stack. 
	 * pre: none 
	 * post: true returned if there are items on the stack, 
	 * false returned otherwise. 
	 */ 
	public boolean isEmpty()
	{
		return data.isEmpty();
		/*if (top == -1) 
		{ 
			return(true);
		}
		else 
		{ 
			return(false);
		}*/
	}
	
	/** 
	 * Returns the number of items in the stack. 
	 * pre: none 
	 * post: The number of items in the stack is returned. 
	 */ 
	public int size() 
	{ 
		return data.size() + 1;
		/*if (isEmpty())
		{ 
			return(0); 
		}
		else 
		{ 
			return(top + 1); 
		}*/
	}
	
	/** 
	 * Empties the stack. 
	 * pre: none 
	 * post: There are no items in the stack. 
	 */ 
	public void makeEmpty() 
	{ 
		data.clear();
		top = -1;
	}
}
