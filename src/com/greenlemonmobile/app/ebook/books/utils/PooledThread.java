
package com.greenlemonmobile.app.ebook.books.utils;

import com.greenlemonmobile.app.ebook.books.utils.ThreadPool.ThreadTasks;

public class PooledThread extends Thread {

    protected ThreadTasks tasks = new ThreadTasks();// 任务队列
    protected boolean running = false;// 运行标记
    protected boolean stopped = false;// 停止标记
    protected boolean paused = false;// 暂停标记
    protected boolean killed = false;// 结束标记
    private ThreadPool pool;// 所属线程池

    public PooledThread(ThreadPool pool) {
        this.pool = pool;
    }

    /*
     * 添加任务
     */
    public void startTasks(Runnable task) {
        tasks.add(task);
    }

    /* 移除并返回一个任务 */
    protected Runnable popTask() {
        return tasks.poll();
    }

    public boolean isRunning() {
        return running;
    }

    /* 停止任务 */
    public void stopTasks() {
        stopped = true;
    }

    /* 以不断短暂睡眠让出资源的方式来实现停止 */
    public void stopTasksSync() {
        stopTasks();
        while (isRunning()) {
            try {
                sleep(5);
            } catch (InterruptedException e) {
            }
        }
    }

    /* 暂停任务 */
    public void pauseTasks() {
        paused = true;
    }

    /* 以不断短暂睡眠让出资源的方式来实现停止 */
    public void pauseTasksSync() {
        pauseTasks();
        while (isRunning()) {
            try {
                sleep(5);
            } catch (InterruptedException e) {
            }
        }
    }

    /* 结束任务 */
    public void kill() {
        if (!running)// 如果不运行就执行打断
            interrupt();
        else
            killed = true;// 如果运行中就标记停止
    }

    /* 结束任务 */
    public void killSync() {
        kill();
        while (isAlive()) {// 如果是活着就不断地让出资源
            try {
                sleep(5);
            } catch (InterruptedException e) {
            }
        }
    }

    // /* 开始任务 */
    // public synchronized void startTasks() {
    // running = true;
    // this.notify();// 唤醒wait本对象的线程.
    // }

    public void run() {
        super.run();
        while (true) {// 要么身为空闲线程时,被打断.
            try {
                if (!running || tasks.size() == 0) {// 符合条件就是空闲线程,执行空闲线程的工作
                    pool.notifyForIdleThread();
                    Runnable task = tasks.take();
                    if (task != null)
                        tasks.add(task);
                    running = true;
                } else {// 繁忙线程
                    Runnable task;
                    while ((task = popTask()) != null) {
                        task.run();
                        if (stopped) {// 每个任务完成后检查停止标记
                            stopped = false;
                            if (tasks.size() > 0) {
                                tasks.clear();// 放弃了还没执行的任务
                                System.out.println(Thread.currentThread().getId()
                                        + ": Tasks are stopped");
                                break;
                            }
                        }
                        if (paused) {// 每个任务完成后检查暂停标记
                            paused = false;
                            if (tasks.size() > 0) {
                                System.out.println(Thread.currentThread().getId()
                                        + ": Tasks are paused");
                                break;
                            }
                        }
                    }
                    running = false;
                }

                if (killed) {// 每个任务完成后检查结束标记,
                    killed = false;
                    break;// 退出无限循环,就真的结束了.
                }

            } catch (InterruptedException e) {
            }
        }
    }
}
