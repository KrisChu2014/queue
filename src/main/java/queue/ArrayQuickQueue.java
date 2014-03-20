package queue;

import java.util.AbstractQueue;
import java.util.Iterator;
import java.util.Queue;

/* A single-producer-single-consumer bounded queue. 
 * 
 * == implementation ==
 * 
 * At each moment, there could be only one producer and one consumer. But the 
 * producer and the consumer can overlap with each other, and that is where 
 * concurrency occurs. 
 * 
 * No lock or CAS is used.
 * 
 * Two indexes, putIndex and takeIndex are used to track the positions of producer 
 * and consumer. Both indexes are defined as "volatile int", which is the key to 
 * implement the concurrency without using lock or CAS.
 * 
 * If putIndex == takeIndex, the queue is empty. If putIndex + 1 == takeIndex, the 
 * queue is full. Either index reaches the end of the queue, the index is wrapped 
 * around to the beginning of the queue. 
 * 
 * The size of the queue can be optimized at the order of 2 to improve the speed of 
 * index increment.
 * 
 * peek() and iterator() are not supported at this point.
 *
 *
 * == performance benchmark ==
 * 
 * Testing shows ArrayQuickQueue beats ConcurrentLinkedQueue by a small margin about 5%, 
 * which could be attributed to the performance difference between CAS and volatile. 
 * 
 * */

public class ArrayQuickQueue<E> extends AbstractQueue<E> implements Queue<E> {

	/* number of items queued */
	private final int size;

	/* queued items */
	private final E[] items;

	/* item index for next poll and remove */
	private volatile int takeIndex;

	/* item index for next offer and add */
	private volatile int putIndex;

	/* index increment */
	private int inc(int pos) {
		return (++pos == size) ? 0 : pos;
	}

	public ArrayQuickQueue(int size) {
		this.size = size;
		this.items = (E[]) (new Object[size]);
		this.takeIndex = 0;
		this.putIndex = 0;
	}

	@Override
	public boolean offer(E e) {

		if (e == null) {
			throw new NullPointerException();
		}

		int index = this.takeIndex;
		if (inc(putIndex) != index) {

			/* order has to be maintained. */

			items[putIndex] = e;
			putIndex = inc(putIndex);

			return true;
		}

		return false;
	}

	@Override
	public E peek() {
		throw new UnsupportedOperationException();
	}

	@Override
	public E poll() {
		int index = this.putIndex;
		if (index != takeIndex) {

			/* order has to be maintained. */

			E e = items[takeIndex];
			takeIndex = inc(takeIndex);

			return e;
		}
		return null;
	}

	@Override
	public Iterator<E> iterator() {
		throw new UnsupportedOperationException();
	}

	@Override
	public int size() {
		return (putIndex + size - takeIndex) % size;
	}

}