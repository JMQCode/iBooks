
package com.greenlemonmobile.app.ebook.books.utils;

import java.util.Collection;
import java.util.Iterator;
import java.util.Vector;
import java.util.concurrent.PriorityBlockingQueue;

public class ThreadPool {

    private static final int CORE_POOL_SIZE = 2;
    private static final int MAXIMUM_POOL_SIZE = 10;

    private static ThreadPool sPool;

    public static ThreadPool getThreadPool() {
        if (sPool == null) {
            sPool = new ThreadPool(CORE_POOL_SIZE, MAXIMUM_POOL_SIZE);
            sPool.init();
        }
        return sPool;
    }

    protected int maxPoolSize;
    protected int initPoolSize;
    protected Vector<PooledThread> threads = new Vector<PooledThread>();
    protected boolean initialized = false;
    protected boolean hasIdleThread = false;
    protected PriorityBlockingQueue<ThreadTasks> queue = new PriorityBlockingQueue<ThreadTasks>();

    public ThreadPool(int initPoolSize, int maxPoolSize) {
        this.initPoolSize = initPoolSize;
        this.maxPoolSize = maxPoolSize;
    }

    public void init() {
        initialized = true;
        for (int i = 0; i < initPoolSize; i++) {
            PooledThread thread = new PooledThread(this);
            thread.start();
            threads.add(thread);
        }

        // 循环分配任务
        new Thread() {
            @Override
            public void run() {
                super.run();
                while (true) {
                    ThreadTasks pollTasks = (ThreadTasks) pollTasks();
                    if (null != pollTasks) {
                        PooledThread idleThread = getIdleThread();
                        if (idleThread != null) {
                            Object[] tasks = (Object[]) pollTasks.toArray();
                            for (Object task : tasks) {
                                idleThread.startTasks((Runnable)task);
                            }
                        }
                    }
                }
            }
        }.start();
    }

    public void setMaxPoolSize(int maxPoolSize) {
        // System.out.println("重设最大线程数，最大线程数=" + maxPoolSize);
        this.maxPoolSize = maxPoolSize;
        if (maxPoolSize < getPoolSize())
            setPoolSize(maxPoolSize);
    }

    /**
     * 重设当前线程数 若需杀掉某线程，线程不会立刻杀掉，而会等到线程中的事务处理完成 但此方法会立刻从线程池中移除该线程，不会等待事务处理结束
     * 
     * @param size
     */
    public void setPoolSize(int size) {
        if (!initialized) {
            initPoolSize = size;
            return;
        } else if (size > getPoolSize()) {
            for (int i = getPoolSize(); i < size && i < maxPoolSize; i++) {
                PooledThread thread = new PooledThread(this);
                thread.start();
                threads.add(thread);
            }
        } else if (size < getPoolSize()) {
            while (getPoolSize() > size) {
                PooledThread th = (PooledThread) threads.remove(0);
                th.kill();
            }
        }
    }

    public int getPoolSize() {
        return threads.size();
    }

    /* 给子线程调用的 */
    protected synchronized void notifyForIdleThread() {
        hasIdleThread = true;
        notify();
    }

    protected synchronized boolean waitForIdleThread() {
        hasIdleThread = false;// 标记为假，等待有线程将其改为真
        while (!hasIdleThread && getPoolSize() >= maxPoolSize) {// 如果没有空闲线程并且无法创建新线程就继续等待
            try {
                wait();
            } catch (InterruptedException e) {
                return false;
            }
        }

        return true;
    }

    public PooledThread getIdleThread() {
        while (true) {
            // 循环看有没有空闲线程，有就返回
            for (Iterator<PooledThread> itr = threads.iterator(); itr.hasNext();) {
                PooledThread th = (PooledThread) itr.next();
                if (!th.isRunning())
                    return th;
            }

            // 如果没有空闲线程，看能否创建新线程
            if (getPoolSize() < maxPoolSize) {
                PooledThread thread = new PooledThread(this);
                thread.start();
                threads.add(thread);
                return thread;
            }

            // 标记等待，并执行等待工作，如果返回假代表被打断，如果返回真从头再获取一次线程。
            if (!waitForIdleThread()) {
                return null;
            }
        }
    }

    public void offerTask(Runnable runnable) {
        ThreadTasks tasks = new ThreadTasks();
        tasks.add(runnable);
        offerTasks(tasks);
    }

    public void offerTasks(ThreadTasks tasks) {
        synchronized (queue) {
            queue.offer(tasks);
        }
    }
    
    public void clearTasks() {
        synchronized (queue) {
            queue.clear();
        }
    }

    private ThreadTasks pollTasks() {
        ThreadTasks task = null;
        try {
            task = queue.take();
        } catch (InterruptedException e) {
        }
        return task;
    }

    public static class ThreadTasks extends PriorityBlockingQueue<Runnable> implements
            Comparable<ThreadTasks> {
        /**
         * 
         */
        private static final long serialVersionUID = 4409230434021226325L;
        private int priority = -1;
        private final long when = System.nanoTime();

        public ThreadTasks() {
            super();
        }

        public ThreadTasks(Collection<? extends Runnable> collection) {
            super(collection);
        }

        public ThreadTasks(int capacity) {
            super(capacity);
        }

        public int getPriority() {
            return priority;
        }

        @Override
        public int compareTo(ThreadTasks another) {
            if (priority >= 0)
                return getPriority() > another.getPriority() ? 1 : getPriority() < another
                        .getPriority() ? -1 : 0;
            return Long.valueOf(another.when).compareTo(when);
        }

    }
}
