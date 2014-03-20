package queue;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionService;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.Date;

import org.junit.After;
import org.junit.Test;

/*
 * Throughput testing, using java.util.concurrent.ArrayBlockingQueue as benchmark.
 * 
 * */

public class ArrayQuickBlockingQueueThroughputTest {

	/* size of the array to use in testing */
	private final static int size = 100;

	/* number of items to use in testing */
	private final static int max = Integer.MAX_VALUE / 10;

	/* for thread management */
	private final ExecutorService executorService = Executors
			.newCachedThreadPool();
	private final CompletionService<String> service = new ExecutorCompletionService<String>(
			executorService);

	@After
	public void cleanup() throws InterruptedException {

		executorService.shutdown();
		if (!executorService.awaitTermination(10, TimeUnit.SECONDS)) {
			/* if executor failed to shutdown gracefully */
			throw new IllegalStateException();
		}
	}

	@Test
	public void testQueue() throws InterruptedException {

		System.out.println("ArrayQuickBlockingQueue");

		final CountDownLatch latch = new CountDownLatch(1);
		final BlockingQueue<Integer> queue = new ArrayQuickBlockingQueue<Integer>(
				size);

		/* single producer, single consumer */

		service.submit(new Producer(queue, latch));
		service.submit(new Consumer(queue, latch));

		latch.countDown();

		int count = 2;

		while (count != 0) {

			try {

				Future<String> future = service.take();
				String msg = future.get();
				System.out.println(msg);
				--count;

			} catch (ExecutionException e) {
				LaunderThrowable.launderThrowable(e.getCause());
			}
		}
	}

	@Test
	public void benchmark() throws InterruptedException {

		System.out.println("ArrayBlockingQueue");

		final CountDownLatch latch = new CountDownLatch(1);
		final BlockingQueue<Integer> queue = new ArrayBlockingQueue<Integer>(
				size);

		service.submit(new Producer(queue, latch));
		service.submit(new Consumer(queue, latch));

		latch.countDown();

		int count = 2;

		while (count != 0) {

			try {

				Future<String> future = service.take();
				String msg = future.get();
				System.out.println(msg);
				--count;

			} catch (ExecutionException e) {
				LaunderThrowable.launderThrowable(e.getCause());
			}
		}
	}

	private static class Producer implements Callable<String> {

		private final BlockingQueue<Integer> queue;
		private final CountDownLatch event;

		Producer(BlockingQueue<Integer> queue, CountDownLatch event) {
			this.queue = queue;
			this.event = event;
		}

		@Override
		public String call() throws Exception {

			int num = 0;
			String name = Thread.currentThread().getName();
			event.await();
			long begin = new Date().getTime();

			while (num != max) {
				queue.put(num++);
			}

			return name + ":" + (new Date().getTime() - begin);
		}
	}

	private static class Consumer implements Callable<String> {

		private final BlockingQueue<Integer> queue;
		private final CountDownLatch event;

		Consumer(BlockingQueue<Integer> queue, CountDownLatch event) {
			this.queue = queue;
			this.event = event;
		}

		@Override
		public String call() throws Exception {

			int num = 0;
			String name = Thread.currentThread().getName();
			event.await();
			long begin = new Date().getTime();

			while (num != max) {
				queue.take();
				++num;
			}

			return name + ":" + (new Date().getTime() - begin);
		}
	}
}
