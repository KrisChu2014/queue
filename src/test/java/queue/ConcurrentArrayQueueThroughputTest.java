package queue;

import java.util.Date;
import java.util.Queue;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.After;
import org.junit.Test;

/*
 * Throughput testing, using java.util.concurrent.ConcurrentLinkedQueue as benchmark.
 * 
 * */

public class ConcurrentArrayQueueThroughputTest {

	/* size of the array to use in testing */
	private final static int size = 400;

	/* number of items to use in testing */
	private final static int max = Integer.MAX_VALUE / 10;

	/* number of producers to use in testing */
	private final static int producers = 2;
	
	/* number of consumers to use in testing */
	private final static int consumers = 2;

	/* thread management */
	private final ExecutorService executorService = Executors
			.newCachedThreadPool();
	private final CompletionService<String> service = new ExecutorCompletionService<String>(
			executorService);

	@After
	public void cleanup() throws InterruptedException {

		executorService.shutdown();
		if (!executorService.awaitTermination(10, TimeUnit.SECONDS)) {
			throw new IllegalStateException();
		}
	}

	@Test
	public void testQueue() throws InterruptedException {

		System.out.println("ConcurrentArrayQueue");

		final Queue<Integer> queue = new ConcurrentArrayQueue<Integer>(size);
		final CountDownLatch latch = new CountDownLatch(1);
		final AtomicInteger produced = new AtomicInteger(0);
		final AtomicInteger consumed = new AtomicInteger(0);

		/* Multiple producer, multiple consumers */

		for (int i = 0; i < producers; ++i) {
			service.submit(new Producer(queue, latch, produced));
		}

		for (int i = 0; i < consumers; ++i) {
			service.submit(new Consumer(queue, latch, consumed));
		}

		latch.countDown();

		for (int i = producers + consumers; i != 0; --i) {

			Future<String> future = service.take();
			try {
				String msg = future.get();
				System.out.println(msg);
			} catch (ExecutionException e) {
				LaunderThrowable.launderThrowable(e.getCause());
			}
		}
	}

	@Test
	public void benchmark() throws InterruptedException {
		System.out.println("ConcurrentLinkedQueue");

		final Queue<Integer> queue = new ConcurrentLinkedQueue<Integer>();
		final CountDownLatch latch = new CountDownLatch(1);
		final AtomicInteger produced = new AtomicInteger(0);
		final AtomicInteger consumed = new AtomicInteger(0);

		/* Multiple producer, multiple consumers */

		for (int i = 0; i < producers; ++i) {
			service.submit(new Producer(queue, latch, produced));
		}

		for (int i = 0; i < consumers; ++i) {
			service.submit(new Consumer(queue, latch, consumed));
		}

		latch.countDown();

		for (int i = producers + consumers; i != 0; --i) {

			Future<String> future = service.take();
			try {
				String msg = future.get();
				System.out.println(msg);
			} catch (ExecutionException e) {
				LaunderThrowable.launderThrowable(e.getCause());
			}
		}
	}

	private static class Producer implements Callable<String> {

		private final Queue<Integer> queue;
		private final CountDownLatch event;
		private final AtomicInteger count;

		Producer(Queue<Integer> queue, CountDownLatch event, AtomicInteger count) {
			this.queue = queue;
			this.event = event;
			this.count = count;
		}

		@Override
		public String call() throws Exception {

			long begin = new Date().getTime();
			String name = Thread.currentThread().getName();
			event.await();

			while (true) {

				int num = count.getAndIncrement();
				if (num >= max) {
					break;
				}

				while (!Thread.currentThread().isInterrupted()
						&& !queue.offer(num)) {
					/* if queue is full */
					Thread.yield();
				}

				if (Thread.interrupted()) {
					/* If interrupt is set, propagate it */
					throw new InterruptedException();
				}
			}

			return name + ":" + (new Date().getTime() - begin);
		}
	}

	private static class Consumer implements Callable<String> {

		private final Queue<Integer> queue;
		private final CountDownLatch event;
		private final AtomicInteger count;

		Consumer(Queue<Integer> queue, CountDownLatch event, AtomicInteger count) {
			this.queue = queue;
			this.event = event;
			this.count = count;
		}

		@Override
		public String call() throws Exception {

			long begin = new Date().getTime();
			String name = Thread.currentThread().getName();
			event.await();

			while (!Thread.currentThread().isInterrupted()
					&& count.get() != max) {

				Integer num = queue.poll();

				if (num == null) {
					/* if queue is empty */
					Thread.yield();
					continue;
				}

				count.incrementAndGet();
			}

			if (Thread.interrupted()) {
				/* If interrupt is set, propagate it */
				throw new InterruptedException();
			}

			return name + ":" + (new Date().getTime() - begin);
		}

	}

}
