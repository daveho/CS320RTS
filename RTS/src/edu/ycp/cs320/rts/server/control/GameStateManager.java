package edu.ycp.cs320.rts.server.control;

import java.util.ArrayList;
import java.util.List;

import edu.ycp.cs320.rts.shared.GameState;

/**
 * Manage shared {@link GameState} by allowing multiple clients
 * to propose updates to it, reconcile those updates, and
 * distribute a new state to each client.
 */
public class GameStateManager {
	public static final int UPDATE_INTERVAL_MS = 100; // update shared game state approximately every 100ms 

	/**
	 * Worker thread.  Periodically checks for proposed updates,
	 * reconciles them, and posts updated games states back
	 * to the clients. 
	 */
	private class Worker implements Runnable {
		@Override
		public void run() {
			while (!shutdownRequested) {
				try {
					Thread.sleep(UPDATE_INTERVAL_MS);
					
					List<GameState> proposedUpdates = new ArrayList<GameState>();
					
					synchronized (lock) {
						for (ClientChannel channel : channelList) {
							// Get proposed updated game states
							GameState proposed = channel.takeProposed();
							if (proposed != null) {
								proposedUpdates.add(proposed);
							}
						}
						
						// TODO: incorporate proposed updates into shared game state
						
						// Distribute new shared game state to clients
						for (ClientChannel channel : channelList) {
							GameState copy = sharedGameState.clone();
							channel.postUpdate(copy);
						}
					}
				} catch (InterruptedException e) {
					// request to shut down
				}
			}
		}
	}
	
	private GameState sharedGameState;
	private Object lock;
	private List<ClientChannel> channelList;
	private volatile boolean shutdownRequested;
	private Thread workerThread;
	
	/**
	 * Constructor.
	 * 
	 * @param gameState the shared {@link GameState} to manage
	 */
	public GameStateManager(GameState gameState) {
		this.sharedGameState = gameState;
		this.lock = new Object(); // protects accesses to channelList
		this.channelList = new ArrayList<ClientChannel>();
		this.shutdownRequested = false;
	}
	
	/**
	 * Connect to the shared {@link GameState}.
	 * 
	 * @return a {@link ClientChannel} connecting a client to the shared game state
	 */
	public ClientChannel connect() {
		synchronized (lock) {
			ClientChannel channel = new ClientChannel();
			channelList.add(channel);
			return channel;
		}
	}
	
	/**
	 * Start the worker thread.
	 */
	public void start() {
		workerThread = new Thread(new Worker());
		workerThread.start();
	}
	
	/**
	 * Shut down the worker thread.
	 */
	public void shutdown() {
		shutdownRequested = true;
		workerThread.interrupt();
	}
}
