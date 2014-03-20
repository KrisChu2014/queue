package queue;

import static org.junit.Assert.assertTrue;

import java.util.Date;
import java.util.Queue;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionService;
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
 * Consistent testing of ArrayQuickQueue 
 * 
 */

public class ArrayQuickQueueConsistencyTest {

	/* size of the array to use in testing */
	private final int size = 100;

	/* number of items to use in testing */
	private final int max = Integer.MAX_VALUE / 10;

	/* for thread management */
	private final ExecutorService executorService = Executors
			.newCachedThreadPool();
	private final CompletionService<String> service = new ExecutorCompletionService<String>(
			executorService);

	private final CountDownLatch start = new CountDownLatch(1);
	private final Queue<Integer> queue = new ArrayQuickQueue<Integer>(size);

	@Test
	public void testArrayQuickQueue() throws InterruptedException {
		
		/* single producer, single consumer */
		
		service.submit(new Producer());
		service.submit(new Consumer());

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
		if (!executorService.awaitTermination(10, TimeUnit.SECONDS)) {
			throw new IllegalStateException();
		}

	}

	private class Producer implements Callable<String> {

		@Override
		public String call() throws Exception {
			int num = 0;
			String name = Thread.currentThread().getName();
			start.await();

			Long begin = new Date().getTime();

			while (num != max) {

				if (Thread.interrupted()) {
					throw new InterruptedException();
				}

				if (queue.offer(num)) {
					++num;
				} else {
					Thread.yield();
				}
			}

			return name + ":" + (new Date().getTime() - begin);
		}
	}

	/*
	 * Check the consistency of queue item. Much slower than Consumer.
	 * 
	 * Not for throughput testing.
	 */

	private class Consumer implements Callable<String> {

		@Override
		public String call() throws Exception {

			int num = 0;
			int prev = -1;

			String name = Thread.currentThread().getName();
			start.await();

			Long begin = new Date().getTime();

			while (num != max) {

				if (Thread.interrupted()) {
					throw new InterruptedException();
				}

				Integer cur = queue.poll();
				if (cur != null) {

					/* cur = prev + 1 */

					assertTrue("[" + cur + "] should be greater than [" + prev
							+ "]", cur == (prev + 1));
					prev = cur;
					++num;
				} else {
					Thread.yield();
				}

			}

			return name + ":" + (new Date().getTime() - begin);
		}
	}
}
