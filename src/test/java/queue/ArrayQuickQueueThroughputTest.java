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

import org.junit.After;
import org.junit.Test;


/*
 * ThroughputTest using java.util.concurrent.ConcurrentLinkedQueue as benchmark
 * 
 * */

public class ArrayQuickQueueThroughputTest {

	/* size of the array to use in testing */
	private final static int size = 100;
	
	/* number of items to use in testing */
	private final static int max = Integer.MAX_VALUE / 4;

	/* for thread management */
	private final ExecutorService executorService = Executors
			.newCachedThreadPool();
	private final CompletionService<String> service = new ExecutorCompletionService<String>(
			executorService);
	
	@Test
	public void testQueue() throws InterruptedException {
		
		System.out.println("ArrayQuickQueue");
		
		final CountDownLatch start = new CountDownLatch(1);
		final Queue<Integer> queue = new ArrayQuickQueue<Integer>(size);
		
		/* single producer, single consumer */
		
		service.submit(new Producer(queue, start));
		service.submit(new Consumer(queue, start));

		start.countDown();

		int count = 2;
		while (count != 0) {

			Future<String> future = service.take();
			try {
				String msg = future.get();
				System.out.println(msg);
				--count;
			} catch (ExecutionException e) {
				throw LaunderThrowable.launderThrowable(e.getCause());
			}
		}
	}
	
	@Test
	public void benchmark() throws InterruptedException {
		
		System.out.println("ConcurrentLinkedQueue");
		
		final CountDownLatch start = new CountDownLatch(1);
		final Queue<Integer> queue = new ConcurrentLinkedQueue<Integer>();
		
		/* single producer, single consumer */
		
		service.submit(new Producer(queue, start));
		service.submit(new Consumer(queue, start));

		start.countDown();

		int count = 2;
		while (count != 0) {

			Future<String> future = service.take();
			try {
				String msg = future.get();
				System.out.println(msg);
				--count;
			} catch (ExecutionException e) {
				throw LaunderThrowable.launderThrowable(e.getCause());
			}
		}
		
	}
	
	@After
	public void cleanup() throws InterruptedException {
		
		executorService.shutdown();
		if(!executorService.awaitTermination(10, TimeUnit.SECONDS)) {
			/* if executor failed to shutdown gracefully */
			throw new IllegalStateException();
		}
	}

	private static class Producer implements Callable<String> {

		private final Queue<Integer> q;
		private final CountDownLatch event;
		
		Producer(Queue<Integer> q, CountDownLatch event) {
			this.q = q;
			this.event = event;
		}
		
		@Override
		public String call() throws Exception {
			int num = 0;
			String name = Thread.currentThread().getName();
			event.await();

			Long begin = new Date().getTime();

			while (num != max) {

				if (Thread.interrupted()) {
					throw new InterruptedException();
				}

				if (q.offer(num)) {
					++num;
				} else {
					/* If queue is full */
					Thread.yield();
				}
			}

			return name + ":" + (new Date().getTime() - begin);
		}
	}

	private static class Consumer implements Callable<String> {

		private final Queue<Integer> q;
		private final CountDownLatch event;
		
		Consumer(Queue<Integer> q, CountDownLatch event) {
			this.q = q;
			this.event = event;
		}
		
		@Override
		public String call() throws Exception {

			int num = 0;
			String name = Thread.currentThread().getName();
			event.await();

			Long begin = new Date().getTime();

			while (num != max) {

				if (Thread.interrupted()) {
					throw new InterruptedException();
				}

				if (q.poll() != null) {
					++num;
				} else {
					/* If queue is emtpy */
					Thread.yield();
				}
			}

			return name + ":" + (new Date().getTime() - begin);
		}
	}

}
