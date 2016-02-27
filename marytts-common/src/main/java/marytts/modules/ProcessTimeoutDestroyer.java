/**
 * Copyright 2000-2006 DFKI GmbH.
 * All Rights Reserved.  Use is subject to license terms.
 *
 * This file is part of MARY TTS.
 *
 * MARY TTS is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package marytts.modules;

// Log4j Logging classes
import java.io.IOException;

import marytts.util.MaryUtils;

import org.apache.log4j.Logger;

/**
 * Destroy a given process if timeout occurs. This is used to monitor whether an external module gets stuck.
 * 
 * @author Marc Schr&ouml;der
 */

public class ProcessTimeoutDestroyer extends Thread {
	private boolean listening = false;
	private boolean exit = false;
	private ExternalModule module = null;
	private Thread customer = null;
	private long timeLimit = 0;
	private boolean didDestroy = false;
	private Logger logger;

	public ProcessTimeoutDestroyer(ExternalModule module) {
		this.module = module;
		logger = MaryUtils.getLogger(module.name() + " timer");
		// The timer threads must have a higher priority than the
		// normal threads, in order to make sure they are processed
		// before any other.
		setPriority(getPriority() + 1);
	}

	public synchronized Process getProcess() {
		return module.getProcess();
	}

	public synchronized long getTimeLimit() {
		return timeLimit;
	}

	private synchronized Thread getCustomer() {
		return customer;
	}

	private synchronized boolean shouldExit() {
		return exit;
	}

	/**
	 * A timelimit &gt; 0 will cause the timeout-destroy procedure to be started. The thread setting the timelimit will be the
	 * only one whose call to <code>resetTimeLimit()</code> is taken into account. If a new thread sets a new time limit while the
	 * old one is still running, the old one is silently stopped, and the new one is started as if the old one was not there.
	 * 
	 * @see #resetTimeLimit
	 * @param timeLimit
	 *            timeLimit
	 */
	public synchronized void setTimeLimit(long timeLimit) {
		if (timeLimit <= 0)
			return; // or throw an Exception????
		this.timeLimit = timeLimit;
		this.customer = Thread.currentThread();
		notify();
	}

	/**
	 * Reset the time limit to 0. Only the thread who initiated the latest time limit can also take it back. If another thread
	 * tries to reset the time limit, it is ignored.
	 */
	public synchronized void resetTimeLimit() {
		if (customer == Thread.currentThread()) { // whoever set the time limit
			doResetTimeLimit();
			notify();
		}
		// Any other thread trying to reset the time limit
		// is ignored.
	}

	private synchronized void doResetTimeLimit() {
		timeLimit = 0;
		customer = null;
	}

	public synchronized void pleaseExit() {
		exit = true;
		notify(); // this is used here only because
		// the value of exit is verified after a
		// notification has been received.
	}

	private synchronized void doDestroy() {
		Process process = getProcess();
		if (process != null) {
			try {
				process.getOutputStream().close();
				process.getInputStream().close();
				process.getErrorStream().close();
			} catch (IOException e) {
				logger.info("Problems destroying process: ", e);
			}
			process.destroy();
		}
		// If process is null, it has already been destroyed.
		didDestroy = true;
	}

	public synchronized boolean didDestroy() {
		boolean d = didDestroy;
		didDestroy = false;
		return d;
	}

	// If this is synchronized, it can cause deadlock:
	public void makeSureWereReady() {
		while (!listening) {
			Thread.yield();
		}
	}

	public synchronized void doWait(long time) {
		try {
			listening = true; // just before the wait(), indicate were ready
			wait(time);
		} catch (InterruptedException e) {
			logger.warn("Wait interrupted: ", e);
		}
	}

	public void run() {
		while (true) {
			Thread orderingCustomer = getCustomer();
			if (orderingCustomer == null) {
				logger.info("Waiting for timer request.");
			} else {
				logger.info("Received timer request: " + getTimeLimit() + " ms.");
			}
			doWait(getTimeLimit());
			if (shouldExit()) {
				logger.info("Exiting.");
				return;
			}
			if (getCustomer() == null) { // timer was reset before timeout
				logger.info("Normal operation, timer stopped.");
			} else if (getCustomer() == orderingCustomer) {
				logger.info("Timeout occurred. Destroying Process.");
				// OK, the serious case
				doDestroy();
				doResetTimeLimit();
			}
			// Else, a different customer. Just forget about old request,
			// and deal with new one.
		}
	}
}
