/*
 *  Copyright (c) 2004, The University Scheduler Project
 *  All rights reserved.
 *
 *  Redistribution and use in source and binary forms, with or without
 *  modification, are permitted provided that the following conditions
 *  are met:
 *
 *  - Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *  - Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *  - Neither the name of the University Scheduler Project nor the names of its
 *    contributors may be used to endorse or promote products derived
 *    from this software without specific prior written permission.
 *
 *  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 *  "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 *  LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 *  FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 *  COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 *  INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 *  BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 *  LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 *  CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 *  LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 *  ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 *  POSSIBILITY OF SUCH DAMAGE.
 *
 */

package edu.rpi.scheduler.ui;

import java.util.LinkedList;
import java.util.List;

/**
 * Defines a simple means of running tasks in the background. It will run
 * forever, sleeping when there are no tasks to perform, until you kill the
 * thread itself.
 */
public class BackgroundWorker extends Thread {
    private final LinkedList<Runnable> workees = new LinkedList<Runnable>();

    /**
     * Creates a new background worker, but does not start its thread. Call
     * {@code bgWorker.start()} to begin the thread.
     */
    public BackgroundWorker() {
        setName("BackgroundWorker");
        setDaemon(true);
    }

    /**
     * Begins the background work loop.
     */
    public void run() {
        try {
            LinkedList<Runnable> workees = this.workees;
            while (true) {
                Runnable workee;
                synchronized(workees) {
                    while (workees.isEmpty()) workees.wait();
                    workee = workees.removeFirst();
                }
                if (workee == null) continue;

                workee.run();
            }
        } catch (InterruptedException e) { }
    }

    /**
     * Adds a task to be performed in the background after all previously added
     * tasks are complete.
     * @param workee the task to be executed
     */
    public synchronized void workOn(Runnable workee) {
        List<Runnable> workees = this.workees;
        synchronized(workees) {
            workees.add(workee);
            workees.notifyAll();
        }
    }

    public boolean isQueueEmpty() {
        LinkedList<Runnable> workees = this.workees;
        synchronized(workees) {
            return workees.isEmpty();
        }
    }
}
