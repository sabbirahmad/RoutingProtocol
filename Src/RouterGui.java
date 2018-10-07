import javax.swing.JFrame;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JScrollPane;

import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.util.Vector;
import java.awt.Dimension;

import javax.swing.JTable;
import javax.swing.JDialog;
import javax.swing.table.DefaultTableModel;
import javax.swing.JPanel;


public class RouterGui extends JFrame implements Runnable{
	
	public static final String[] columnNames = { "Type", "Network Address", "Mask", "Interface Id", "Next Hop", "Hop Count" };
	
	String routerId;
	SimRouter simRouter;
	RoutingProtocol rProtocol;
	
	private JDialog routingTableDialog=null; 
	private JTable routingTableJTable=null;
	JPanel infoPanel=null;
	
	public RouterGui(SimRouter s, RoutingProtocol r, String rId) {
		routerId=rId;
		setTitle("Router: "+routerId);
		setSize(new Dimension(500, 300));
		setResizable(false);
		this.simRouter=s;
		this.rProtocol=r;
		getContentPane().setLayout(null);
		
		JButton btnShowRoutingTable = new JButton("Routing Table");
		btnShowRoutingTable.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				showRoutingTable();
			}
		});
		btnShowRoutingTable.setBounds(308, 212, 127, 23);
		getContentPane().add(btnShowRoutingTable);
		
		infoPanel = new JPanel();
		infoPanel.setBounds(0, 0, 473, 183);
		getContentPane().add(infoPanel);
		this.setVisible(false);
		this.setDefaultCloseOperation(HIDE_ON_CLOSE);
		
		setRouterInfo();
		
	}
	
	private void setRouterInfo() {
		int intId;
		int mask;
		IpAddress intIp;
		SimInterface[] simIn=simRouter.interfaces;
		System.out.println("LENGTH= "+simIn.length);
		for (int i = 1; i < simIn.length; i++) {
			if(simIn[i].getPortStatus()){
				 intId=simIn[i].interfaceId;
				 //System.out.println(intId);
				 intIp=simIn[i].getIpAddress();
				 //System.out.println(intIp.getString());
				 mask=simIn[i].subnetMask;
				 //System.out.println(mask);
				 
				 infoPanel.add(new JLabel("Interface: "+intId+"\tIP: "+intIp.getString()+"/"+mask));
			}
 
			 
		}
		
	}

	public void showRoutingTable() {
		
		DefaultTableModel model = new DefaultTableModel(columnNames,0);
		//model.addRow(columnNames);
		
		String rtString= new String(rProtocol.forTableView());
		String[] parsedTable=rtString.split("Entry");
		for(int i=0;i<parsedTable.length;i++){
			//System.out.println("\nnew entry: ");
			System.out.println("");
			Vector<String> row =new Vector();
			String[] parsedEntry=parsedTable[i].split(",");
			for(int j=0;j<parsedEntry.length;j++){
				System.out.print(parsedEntry[j]+" ");
				row.add(parsedEntry[j]);
			}
				
				
				model.addRow(row);
		}

	
		routingTableDialog=new JDialog();
		routingTableDialog.setSize(800, 300);
		routingTableDialog.setDefaultCloseOperation(DISPOSE_ON_CLOSE);
		 
		routingTableJTable=new JTable(model);
		routingTableDialog.setTitle(routerId+" - Routing Table");
		
		JScrollPane sp=new JScrollPane(routingTableJTable);
		
		routingTableDialog.getContentPane().add(sp);
		
		routingTableDialog.setVisible(true);
		
		
	}
	
	public void run(){
		while(true){
			
			
		}
	}
}
