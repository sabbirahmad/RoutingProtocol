import javax.swing.JFrame;
import javax.swing.JScrollPane;

import java.awt.Dimension;
import javax.swing.JTextArea;

import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

import javax.swing.JTextField;

import java.awt.Color;
import javax.swing.ScrollPaneConstants;


public class RouterConsole extends JFrame implements Runnable {
	
	SimRouter sRouter;
	private Thread t;
	String routerId;
	private boolean con_input=false;
	String outConsoleStr;
	
	String userInput;
	String conName;
	JTextArea consoleOutputTextArea;
	private JTextField consoleInputTextField;
	private JTextField consoleRName;
	private JScrollPane scrollPane;
	
	
	public RouterConsole(SimRouter s, String rId){
		sRouter=s;
		routerId=rId;
		
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
					userInput=consoleInputTextField.getText();
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
		
		
		
		conName=rId+":~$ ";
		consoleRName.setText(conName);
		
		//consoleTextArea.setText(conName);
		this.setTitle(routerId+" - Console");
		
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
					
					if(!userInput.contains(":")){
						if(userInput.compareTo("clear")==0){
							consoleOutputTextArea.setText("");
							continue;
						}
						outConsoleStr=consoleOutputTextArea.getText();
						outConsoleStr+=userInput+" : Wrong Command!\n";
						consoleOutputTextArea.setText(outConsoleStr);
						final int length = consoleOutputTextArea.getText().length();
						consoleOutputTextArea.setCaretPosition(length);
						continue;
					}
					
					outConsoleStr=consoleOutputTextArea.getText();
					outConsoleStr+=userInput+" : Command Executed\n";
					consoleOutputTextArea.setText(outConsoleStr);
					final int length = consoleOutputTextArea.getText().length();
					consoleOutputTextArea.setCaretPosition(length);
					
					
					int colon = userInput.indexOf(':');
					String command = userInput.substring(0, colon);
	
					
					if (command.compareTo("Down") == 0) {
						int interfaceId = Integer.parseInt(userInput
								.substring(colon + 1));
						sRouter.setPortStatus(interfaceId, false);
					}
					if (command.compareTo("Up") == 0) {
						int interfaceId = Integer.parseInt(userInput
								.substring(colon + 1));
						sRouter.setPortStatus(interfaceId, true);
					}
					
					
				}
			}
		} catch (Exception e) {
		}
	}
}