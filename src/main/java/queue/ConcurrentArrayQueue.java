package queue;

import java.util.AbstractQueue;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/*
 * A bounded FIFO wait-free queue. 
 * 
 * Multiple producers and consumers can access the queue concurrently.
 * 
 * == implementation ==
 * 
 * Implementation is similar to java.util.concurrent.ConcurrentLinkedQueue in that both 
 * use CAS to ensure concurrency. The difference is that ConcurrentArrayQueue CAS'es on 
 * two array indexes, takeIndex and putIndex, while java.util.ConcurrentLinkedQueue
 * CAS'es on the two list nodes, head and tail.  
 * 
 * No lock is used. 
 * 
 * 
 * == performance benchmark ==
 * 
 * Testing shows java.util.concurrent.ConcurrentLinkedQueue beats ConcurrentArrayQueue by a 
 * margin from 20% to 5%, depending on the size of array used in ConcurrentArrayQueue. 
 * 
 * The primary reason is that java.util.concurrent.ConcurrentLinkedQueue is unbounded, and 
 * thus adding items always succeeds instantly, while ConcurrentArrayQueue is bounded by array 
 * size, in which case adding items may require multiple retries before it succeeds. 
 * 
 * When array size grows bigger, the performance of ArrayQuickQueue converges to that 
 * of java.util.concurrent.ConcurrentLinkedQueue.
 * 
 * */

public class ConcurrentArrayQueue<E> extends AbstractQueue<E> implements
		Queue<E> {

	/* number of items queued */
	private final int size;

	/* queued items */
	private final AtomicReference<E>[] items;

	/* item index for next poll and remove */
	private final AtomicInteger putIndex;

	/* item index for next offer and add */
	private final AtomicInteger takeIndex;

	/* index increment */
	private int inc(int pos) {
		return (++pos == size) ? 0 : pos;
	}

	public ConcurrentArrayQueue(int size) {

		this.items = new AtomicReference[size];

		/* initialize each item of the queue to null */
		for (int i = 0; i < size; ++i) {
			items[i] = new AtomicReference<E>();
		}

		this.size = size;
		this.putIndex = new AtomicInteger(0);
		this.takeIndex = new AtomicInteger(0);
	}

	@Override
	public boolean offer(E e) {

		if (e == null) {
			throw new NullPointerException();
		}

		while (true) {

			int oldPutIndex = putIndex.get();
			int newPutIndex = inc(oldPutIndex);

			if (newPutIndex == this.takeIndex.get()) {
				/* if full */
				return false;
			}

			/*
			 * Compare if item is null and set item to e is performed
			 * atomically.
			 */

			if (items[oldPutIndex].compareAndSet(null, e)) {

				/*
				 * If item is successfully set to e, we need to further update
				 * putIndex, which may have already been updated by another
				 * producer.
				 * 
				 * CAS is used to ensure the consistent state of putIndex before
				 * and after item is set to e. And only if putIndex remains the
				 * same will we update it.
				 * 
				 * If not same, another producer has helped us by updating it.
				 */

				putIndex.compareAndSet(oldPutIndex, newPutIndex);
				return true;

			} else {

				/*
				 * If item is not null, what happens is that another producer
				 * just updated the item but has not updated putIndex.
				 * 
				 * We here help that producer by updating the putIndex. CAS is
				 * used to ensure consistency state of putIndex.
				 */

				putIndex.compareAndSet(oldPutIndex, newPutIndex);
			}
		}
	}

	@Override
	public E poll() {

		while (true) {

			int oldTakeIndex = takeIndex.get();
			int newTakeIndex = inc(oldTakeIndex);

			if (oldTakeIndex == this.putIndex.get()) {
				/* if empty */
				return null;
			}

			E e = items[oldTakeIndex].get();

			/*
			 * Compare if item is not null and set item to null is performed
			 * atomically.
			 */

			if (e != null && items[oldTakeIndex].compareAndSet(e, null)) {

				/*
				 * If the item is set to null, we need to further update
				 * takeIndex, which may have already been updated by another
				 * consumer.
				 * 
				 * CAS is used to ensure the consistent state of takeIndex
				 * before and after item is set to null. And only if takeIndex
				 * remains the same will we update it.
				 * 
				 * If not same, another consumer has already helped us by
				 * updating it.
				 */

				takeIndex.compareAndSet(oldTakeIndex, newTakeIndex);
				return e;
			} else {

				/*
				 * If item is null, what happens is that another consumer just
				 * set the item to null but has not updated takeIndex.
				 * 
				 * We here help that consumer by updating the takeIndex. CAS is
				 * used to ensure consistency state of takeIndex.
				 */

				takeIndex.compareAndSet(oldTakeIndex, newTakeIndex);
			}
		}
	}

	/* Same as poll, except that we don't modify the queue. */

	@Override
	public E peek() {
		while (true) {

			int oldTakeIndex = takeIndex.get();
			int newTakeIndex = inc(oldTakeIndex);

			if (oldTakeIndex == this.putIndex.get()) {
				return null;
			}

			E e = items[oldTakeIndex].get();
			if (e != null) {
				return e;
			} else {

				/*
				 * Another consumer has just set item to null but has not
				 * updated takeIndex.
				 * 
				 * We here help that consumer by updating takeIndex. CAS is used
				 * to ensure consistent state of takeIndex.
				 */

				takeIndex.compareAndSet(oldTakeIndex, newTakeIndex);
			}
		}
	}

	@Override
	public boolean isEmpty() {
		return putIndex.get() == takeIndex.get();
	}

	@Override
	public int size() {
		return (putIndex.get() + size - takeIndex.get()) % size;
	}

	@Override
	public Iterator<E> iterator() {
		return new Iter();
	}

	private class Iter implements Iterator<E> {

		private int curPos;
		private E curObject;

		Iter() {
			init();
		}

		private void init() {

			curPos = takeIndex.get();

			while (true) {

				if (curPos == putIndex.get()) {
					curObject = null;
					break;
				}

				curObject = items[curPos].get();
				if (curObject != null) {
					break;
				}

				curPos = takeIndex.get();
			}
		}

		@Override
		public boolean hasNext() {
			return !(curObject == null);
		}

		@Override
		public E next() {

			if (curObject == null) {
				throw new NoSuchElementException();
			}

			E obj = curObject;

			while (true) {

				curPos = inc(curPos);

				if (curPos == putIndex.get()) {
					curObject = null;
					break;
				}

				curObject = items[curPos].get();
				if (curObject != null) {
					break;
				}
			}

			return obj;
		}

		@Override
		public void remove() {
			throw new UnsupportedOperationException();
		}
	}
}
