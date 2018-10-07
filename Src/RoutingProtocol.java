import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.net.*;
import java.io.*;

import javax.swing.text.Utilities;

/**
 * class RoutingProtocol simulates the routing protocol
 */
class RoutingProtocol extends Thread {

	// routing table
	CopyOnWriteArrayList<RoutingTableEntry> routingTable = new CopyOnWriteArrayList<RoutingTableEntry>();

	// update packet buffer
	Buffer<ByteArray> buffer = new Buffer<ByteArray>("Upadate Packet Buffer",
			100);

	/**
	 * router simulated by SimRouter
	 */
	SimRouter simrouter;

	/**
	 * update timer duration
	 */
	public static int UPDATE_TIMER_VALUE = 30;

	/**
	 * invalidate timer duration
	 */
	public static int INVALID_TIMER_VALUE = 90;

	// To Do: Declare any other variables required
	// ----------------------------------------------------------------------------------------------

	Timer updateTimer;

	/**
	 * @param s
	 *            simulated router
	 */

	// ------------------------Routing
	// Function-----------------------------------------------
	/**
	 * stores the update data in a shared memory to be processed by the
	 * 'RoutingProtocol' thread later
	 * 
	 * @param p
	 *            ByteArray
	 */
	void notifyRouteUpdate(ByteArray p) {// invoked by SimRouter
		// Write code to just to stores the route update data; do not process at
		// this moment, otherwise the simrouter thread will get blocked
		try {
			synchronized (buffer) {
				if (buffer.full())
					buffer.wait();

				buffer.store(p);
				buffer.notify();
			}
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	// ----------------------------------------------------------------------------------------------
	/**
	 * update the routing table according to the changed status of an interface;
	 * if interface is UP (status=TRUE), add to routing table, if interface is
	 * down, remove from routing table
	 * 
	 * @param interfaceId
	 *            interface id of the router
	 * @param status
	 *            status denoting whether interface is on or off, true if on and
	 *            false otehrwise
	 */
	public void notifyPortStatusChange(int interfaceId, boolean status) {// invoked
																			// by
																			// SimRouter
		// To Do: Update the routing table according to the changed status of an
		// interface. If interface in UP (status=TRUE), add to routing table, if
		// interface is down, remove from routing table

		if (status) {
			RoutingTableEntry rEntry = new RoutingTableEntry(1,
					simrouter.interfaces[interfaceId].getIpAddress(),
					simrouter.interfaces[interfaceId].getSubnetMask(),
					interfaceId, null, 0);
			add(rEntry);
		} else {
			for (RoutingTableEntry rEntry : routingTable) {
				if (rEntry.interfaceId == interfaceId)
					routingTable.remove(rEntry);
			}
		}
	}

	// ---------------------Forwarding
	// Function------------------------------------------
	/**
	 * returns an NextHop object corresponding the destination IP Address,
	 * dstIP. If route in unknown, return null
	 * 
	 * @param destination
	 *            ip
	 * @return returns an NextHop object corresponding the destination IP
	 *         Address if route is known, else returns null
	 */
	NextHop getNextHop(IpAddress dstIp) {// invoked by SimRouter
		// To Do: Write code that returns an NextHop object corresponding the
		// destination IP Address: dstIP. If route in unknown, return null

		for (RoutingTableEntry rEntry : routingTable) {
			if (rEntry.getIp().sameSubnet(dstIp, rEntry.getMask())) {
				return rEntry.getNextHop();
			}
		}

		return null; // default return value
	}

	// -------------------Routing Protocol
	// Thread--------------------------------------
	public void run() {
		// To Do 1: Populate Routing Table with directly connected interfaces
		// using the SimRouter instance. Also print this initial routing table .

		System.out.println("initial configuration");

		while (!simrouter.buffer.empty()) {
			ByteArray bytes = simrouter.buffer.get();
			String line = new String(bytes.getBytes());
			String[] tokens = line.split(":");
			int interfaceId = Integer.parseInt(tokens[2]);
			IpAddress ip = new IpAddress(tokens[3]);
			int mask = Integer.parseInt(tokens[4]);
			RoutingTableEntry routingTableEntry = new RoutingTableEntry(1,
					ip.getNetworkAddress(mask), mask, interfaceId, new NextHop(
							simrouter.interfaces[interfaceId].getIpAddress(),
							interfaceId), 0);
			add(routingTableEntry);
		}

		showAllEntry();
		// To Do 2: Send constructed routing table immediately to all the
		// neighbors. Start the update timer.
		//System.out.println("sending initial update");

		sendUpdatePacketToNeighbours();
		updateTimer.schedule(new TimerTask() {

			@Override
			public void run() {
				// TODO Auto-generated method stub
				handleTimerEvent(1, null);
			}
		}, UPDATE_TIMER_VALUE * 1000);
		// To Do 3: Continuously check whether there are routing updates
		// received from other neighbours.
		// An update has been received, Now:
		// To Do 3.1: Modify routing table according to the update received.
		// To Do 3.2: Start invalidate timer for each newly added/updated route
		// if any.
		// To Do 3.3:Print the routing table if the routing table has changed
		// To Do Optional 1: Send Triggered update to all the neighbors and
		// reset update timer.

		//System.out.println("recieving update");
		while (true) {
			ByteArray bArray;
			try {
				synchronized (buffer) {
					if (buffer.empty())
						buffer.wait();
					System.out.println("updating");
					bArray = buffer.get();
					buffer.notify();
				}
				int interfaceId = bArray.getByteVal(0);

				Packet p = new Packet(bArray.getAt(1, bArray.getSize() - 1));
				// System.out.println(p.toString());
				IpAddress srcIp = p.getSrcIp();

				byte[] b = p.getPayload();
				byte[] netIpByte = new byte[4];
				byte[] hopCountByte = new byte[4];
				for (int i = 0; i < b.length / 8; i++) {
					System.arraycopy(b, i * 8 + 0, netIpByte, 0, 4);
					IpAddress netIp = new IpAddress(netIpByte);
					System.arraycopy(b, i * 8 + 4, hopCountByte, 0, 4);
					int hopCount = bytesToInt(hopCountByte);
					//System.out.println(hopCount);
					hopCount++;

					boolean found = false;
					for (int j = 0; j < routingTable.size(); j++) {

						RoutingTableEntry current = routingTable.get(j);

						//System.out.println(netIp.getString());
						//System.out.println(current.ip.getString());

						if (current.getIp()
								.sameSubnet(netIp, current.getMask())) {
							//System.out.println("same ip");
							found = true;
							if (current.getHopCount() > hopCount) {
								//System.out.println("less hopcount");
								routingTable.get(j).cancelTimer();
								routingTable.get(j).setHopCount(hopCount);
								NextHop nextHop = new NextHop(srcIp,
										interfaceId);
								routingTable.get(j).setNextHop(nextHop);
								routingTable.get(j).restartTimer();

							} else {
								routingTable.get(j).cancelTimer();
								routingTable.get(j).restartTimer();
							}
						}
					}
					if (!found) {
						//System.out.println("not found");
						NextHop nextHop = new NextHop(srcIp, interfaceId);
						RoutingTableEntry newEntry = new RoutingTableEntry(2,
								netIp,
								simrouter.interfaces[interfaceId]
										.getSubnetMask(), interfaceId, nextHop,
								hopCount);
						add(newEntry);
					}

				}
				showAllEntry();
			} catch (Exception e) {
				e.printStackTrace();
			}

		}
	}

	// ----------------------Timer
	// Handler------------------------------------------------------
	/**
	 * handles what happens when update timer and invalidate timer expires
	 * 
	 * @param type
	 *            of timer: type 1- update timer and type 2- invalid timer
	 *            expired
	 */
	public void handleTimerEvent(int type, RoutingTableEntry routingTableEntry) {
		// If update timer has expired, then:
		// To Do 1: Sent routing update to all the interfaces. Use
		// simrouter.sendPacketToInterface(update, interfaceId) function to send
		// the updates.
		// To Do Optional 1: Implement split horizon rule while sending update
		// To Do 2: Start the update timer again.
		if (type == 1) {
			//System.out.println("handle update");
			byte[] b = new byte[routingTable.size() * 8];
			for (int i = 0; i < routingTable.size(); i++) {
				System.arraycopy(routingTable.get(i).getIp().getBytes(), 0, b,
						i * 8 + 0, 4);
				int hopCount = routingTable.get(i).getHopCount();
				if (hopCount == 100)
					hopCount = 0;
				System.arraycopy(intToBytes(hopCount), 0, b, i * 8 + 4, 4);
			}

			int interfaceCount = simrouter.interfaceCount;
			for (int i = 1; i <= interfaceCount; i++) {
				if (simrouter.interfaces[i].getIpAddress() == null)
					continue;
				Packet updatePacket = new Packet(
						simrouter.interfaces[i].getIpAddress(), new IpAddress(
								simrouter.RP_MULTICAST_ADDRESS), b);
				// System.out.println(updatePacket.toString());
				int dstMac = simrouter.getMacFromArpTable(new IpAddress(
						"224.0.0.0"));

				ByteArray byteArray = new ByteArray(
						(updatePacket.getBytes()).length + 1);
				byteArray.setByteVal(0, (byte) dstMac);
				byteArray.setAt(1, updatePacket.getBytes());
				simrouter.sendPacketToInterface(byteArray, i);

			}

			updateTimer.purge();

			updateTimer.schedule(new TimerTask() {

				@Override
				public void run() {
					// TODO Auto-generated method stub
					handleTimerEvent(1, null);
				}
			}, UPDATE_TIMER_VALUE * 1000);
		}
		// Else an invalid timer has expired, then:
		// To Do 3: Delete route frodm routing table for which invalidate timer
		// has expired.
		else if (type == 2) {
			//System.out.println("handle invalid");
			if (routingTableEntry.getType() == 1)
				return;
			routingTableEntry.cancelTimer();
			routingTable.remove(routingTableEntry);
		}

	}

	// ----------------------------------------------------------------------------------------------

	public RoutingProtocol(SimRouter s) {
		simrouter = s;
		// To Do: Do other required initialization tasks.
		updateTimer = new Timer();

		start();
	}

	public void add(RoutingTableEntry rtEntry) {

		for (int i = 0; i < routingTable.size(); i++) {
			if (routingTable.get(i).getMask() < rtEntry.getMask()) {
				routingTable.add(i, rtEntry);
				return;
			}
		}
		routingTable.add(rtEntry);
	}

	public void showAllEntry() {
		for (RoutingTableEntry rtEntry : routingTable) {
			System.out.println(rtEntry.toString());
		}
	}

	public String forTableView() {
		String fullTable = new String();
		for (RoutingTableEntry rtEntry : routingTable) {
			fullTable += rtEntry.entryValues();
		}

		return fullTable;
	}

	private void sendUpdatePacketToNeighbours() {
		// TODO Auto-generated method stub
		byte[] b = new byte[routingTable.size() * 8];
		for (int i = 0; i < routingTable.size(); i++) {

			System.arraycopy(routingTable.get(i).getIp().getBytes(), 0, b,
					i * 8 + 0, 4);
			int hopCount = routingTable.get(i).getHopCount();
			if (hopCount == 100)
				hopCount = 0;
			System.arraycopy(intToBytes(hopCount), 0, b, i * 8 + 4, 4);
		}

		int interfaceCount = simrouter.interfaceCount;
		for (int i = 1; i <= interfaceCount; i++) {
			if (simrouter.interfaces[i].getIpAddress() == null)
				continue;
			Packet updatePacket = new Packet(
					simrouter.interfaces[i].getIpAddress(), new IpAddress(
							simrouter.RP_MULTICAST_ADDRESS), b);
			// System.out.println(updatePacket.toString());
			int dstMac = simrouter
					.getMacFromArpTable(new IpAddress("224.0.0.0"));

			ByteArray byteArray = new ByteArray(
					(updatePacket.getBytes()).length + 1);
			byteArray.setByteVal(0, (byte) dstMac);
			byteArray.setAt(1, updatePacket.getBytes());
			simrouter.sendPacketToInterface(byteArray, i);

		}
	}

	public byte[] intToBytes(int a) {
		return new byte[] { (byte) ((a >> 24) & 0xFF),
				(byte) ((a >> 16) & 0xFF), (byte) ((a >> 8) & 0xFF),
				(byte) (a & 0xFF) };
	}

	public int bytesToInt(byte[] b) {
		return b[3] & 0xFF | (b[2] & 0xFF) << 8 | (b[1] & 0xFF) << 16
				| (b[0] & 0xFF) << 24;
	}

	class RoutingTableEntry {
		int type;
		IpAddress ip;
		int mask;
		int interfaceId;
		NextHop nextHop;
		int hopCount;
		Timer invalidTimer;

		public int getInterfaceId() {
			return interfaceId;
		}

		public void setInterfaceId(int interfaceId) {
			this.interfaceId = interfaceId;
		}

		@Override
		public String toString() {
			return "RoutingTableEntry [type=" + type + ", ip=" + ip.getString()
					+ ", mask=" + mask + ", interfaceId=" + interfaceId
					+ ", nextHop=" + nextHop + ", hopCount=" + hopCount + "]";
		}

		public String entryValues() {
			if (nextHop != null)
				return "Entry" + type + "," + ip.getString() + "," + mask + ","
						+ interfaceId + "," + nextHop.getIp().getString() + ","
						+ hopCount;
			else
				return "Entry" + type + "," + ip.getString() + "," + mask + ","
						+ interfaceId + "," + nextHop + "," + hopCount;
		}

		public RoutingTableEntry(int type, IpAddress ip, int mask,
				int interfaceId, NextHop nextHop, int hopCount) {
			super();
			this.type = type;
			this.ip = ip;
			this.mask = mask;
			this.interfaceId = interfaceId;
			this.nextHop = nextHop;
			this.hopCount = hopCount;
			invalidTimer = new Timer();
			invalidTimer.schedule(new TimerTask() {

				@Override
				public void run() {
					// TODO Auto-generated method stub
					handleTimerEvent(2, getInstance());
				}
			}, INVALID_TIMER_VALUE * 1000);
		}

		public RoutingTableEntry getInstance() {
			return this;
		}

		public int getType() {
			return type;
		}

		public void setType(int type) {
			this.type = type;
		}

		public IpAddress getIp() {
			return ip;
		}

		public void setIp(IpAddress ip) {
			this.ip = ip;
		}

		public int getMask() {
			return mask;
		}

		public void setMask(int mask) {
			this.mask = mask;
		}

		public NextHop getNextHop() {
			return nextHop;
		}

		public void setNextHop(NextHop nextHop) {
			this.nextHop = nextHop;
		}

		public int getHopCount() {
			return hopCount;
		}

		public void setHopCount(int hopCount) {
			this.hopCount = hopCount;
		}

		public void cancelTimer() {
			invalidTimer.purge();
			invalidTimer.cancel();

		}

		public void restartTimer() {
			invalidTimer = new Timer();
			invalidTimer.schedule(new TimerTask() {

				@Override
				public void run() {
					// TODO Auto-generated method stub
					handleTimerEvent(2, getInstance());
				}
			}, INVALID_TIMER_VALUE * 1000);
		}

	}

}
