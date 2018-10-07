import javax.swing.JFrame;
import javax.swing.JScrollPane;

import java.awt.Dimension;

import javax.swing.JTextArea;

import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

import javax.swing.JTextField;

import java.awt.Color;

import javax.swing.ScrollPaneConstants;


public class HostConsole extends JFrame implements Runnable {
	
	SimHost sHost;
	private Thread t;
	String hostId;
	private boolean con_input=false;
	String outConsoleStr;
	
	String input;
	String conName;
	JTextArea consoleOutputTextArea;
	private JTextField consoleInputTextField;
	private JTextField consoleRName;
	private JScrollPane scrollPane;
	int length;
	
	public HostConsole(SimHost s, String hId){
		sHost=s;
		hostId=hId;
		
		setSize(new Dimension(400, 300));
		getContentPane().setLayout(null);
		
		scrollPane = new JScrollPane();
		scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		scrollPane.setBounds(0, 0, 384, 218);
		getContentPane().add(scrollPane);
		
		consoleOutputTextArea = new JTextArea();
		scrollPane.setViewportView(consoleOutputTextArea);
		consoleOutputTextArea.setEditable(false);
		consoleOutputTextArea.setForeground(Color.WHITE);
		consoleOutputTextArea.setBackground(Color.BLACK);
		
		consoleInputTextField = new JTextField();
		consoleInputTextField.setForeground(Color.WHITE);
		consoleInputTextField.setBackground(Color.BLACK);
		consoleInputTextField.addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent arg0) {
				if (arg0.getKeyCode()==KeyEvent.VK_ENTER){
					input=consoleInputTextField.getText();
					con_input=true;
					consoleInputTextField.setText("");
				}
			}
		});
		consoleInputTextField.setBounds(60, 226, 324, 35);
		getContentPane().add(consoleInputTextField);
		consoleInputTextField.setColumns(10);
		
		consoleRName = new JTextField();
		consoleRName.setEditable(false);
		consoleRName.setForeground(Color.WHITE);
		consoleRName.setBackground(Color.BLACK);
		consoleRName.setBounds(0, 226, 61, 35);
		getContentPane().add(consoleRName);
		consoleRName.setColumns(10);
		
		
		
		conName=hId+":~$ ";
		consoleRName.setText(conName);
		
		//consoleTextArea.setText(conName);
		this.setTitle(hostId+" - Console");
		
		t=new Thread(this);
		t.start();
		this.setVisible(false);
		this.setDefaultCloseOperation(HIDE_ON_CLOSE);
	}
	
	public void run() {
		
		try {
			//BufferedReader inFromUser = new BufferedReader(new InputStreamReader(System.in));
			
			
			while (true) {
				//System.out.println("YESSSSSSSSS");
				
				while(con_input==true){
				
					con_input=false;
					//consoleInputTextField.setText("");
					//consoleOutputTextArea.setText(userInput);
					//System.out.println("YESSSSSSSSS dobubse");
					//userInput = inFromUser.readLine();
					// System.out.println("Input "+userInput);
					//int i=userInput.lastIndexOf(conName);
					
					if(!input.contains(":")){
						if(input.compareTo("clear")==0){
							consoleOutputTextArea.setText("");
							continue;
						}
						outConsoleStr=consoleOutputTextArea.getText();
						outConsoleStr+=input+" : Wrong Command!\n";
						consoleOutputTextArea.setText(outConsoleStr);
						final int length = consoleOutputTextArea.getText().length();
						consoleOutputTextArea.setCaretPosition(length);
						continue;
					}
					
					
					consoleOutputTextArea.append(input+"\n");
					length = consoleOutputTextArea.getText().length();
					consoleOutputTextArea.setCaretPosition(length);
					
					
							
					//String input = sHost.br.readLine();

					String[] tokens = input.split(":");
					if (tokens.length < 2) {
						//System.out.println("Input parameter missing");
						consoleOutputTextArea.append("Input parameter missing\n");
						length = consoleOutputTextArea.getText().length();
						consoleOutputTextArea.setCaretPosition(length);
						continue;
					}

					IpAddress dstIp = new IpAddress(tokens[0]);

					if (sHost.ipAddr.sameIp(dstIp)) {
						//System.out.println("Ping to own interface");
						consoleOutputTextArea.append("Ping to own interfac\n");
						length = consoleOutputTextArea.getText().length();
						consoleOutputTextArea.setCaretPosition(length);
						continue;
					}

					int dstMac;
					if (sHost.ipAddr.sameSubnet(dstIp, sHost.mask)) {
						dstMac = sHost.getMacFromArpTable(dstIp);
					} else
						dstMac = sHost.getMacFromArpTable(sHost.gateway);

					if (dstMac < 0) {
						//System.out.println("MAC Address of Destination not found");
						consoleOutputTextArea.append("MAC Address of Destination not found\n");
						length = consoleOutputTextArea.getText().length();
						consoleOutputTextArea.setCaretPosition(length);
						continue;
					}
					// Everything fine; send the packet
					String text = tokens[1];
					Packet p = new Packet(sHost.ipAddr, dstIp, text.getBytes());
					ByteArray temp = new ByteArray((p.getBytes()).length + 1);
					temp.setByteVal(0, (byte) dstMac);
					temp.setAt(1, p.getBytes());
					sHost.sendToDll(temp);
						
					
					
				}
			}
		} catch (Exception e) {
		}
	}
}