package com.cn2.communication;

import java.io.*;
import java.net.*;
import javax.sound.sampled.*;

import javax.swing.JFrame;
import javax.swing.JTextField;
import javax.swing.JButton;
import javax.swing.JTextArea;
import javax.swing.JScrollPane;

import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.*;
import java.awt.event.*;
import java.awt.Color;
import java.lang.Thread;

public class App extends Frame implements WindowListener, ActionListener {

	/*
	 * Definition of the app's fields
	 */
	static TextField inputTextField;		
	static JTextArea textArea;				 
	static JFrame frame;					
	static JButton sendButton;				
	static JTextField meesageTextField;		  
	public static Color gray;				
	final static String newline="\n";		
	static JButton callButton;	
	static JButton endButton;
	
    // Threads for call handling
    private Thread captureThread;
    private Thread receiveThread;
    private TargetDataLine getsound;
    private SourceDataLine hearsound;
    private DatagramSocket call_socket;
    private Socket new_socket;

	
	/**
	 * Construct the app's frame and initialize important parameters
	 */
	public App(String title) {
		
		/*
		 * 1. Defining the components of the GUI
		 */
		
		// Setting up the characteristics of the frame
		super(title);									
		gray = new Color(254, 254, 254);		
		setBackground(gray);
		setLayout(new FlowLayout());			
		addWindowListener(this);	
		
		// Setting up the TextField and the TextArea
		inputTextField = new TextField();
		inputTextField.setColumns(20);
		
		// Setting up the TextArea.
		textArea = new JTextArea(10,40);			
		textArea.setLineWrap(true);				
		textArea.setEditable(false);			
		JScrollPane scrollPane = new JScrollPane(textArea);
		scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
		
		//Setting up the buttons
		sendButton = new JButton("Send");			
		callButton = new JButton("Call");
		endButton = new JButton("End Call");
						
		/*
		 * 2. Adding the components to the GUI
		 */
		add(scrollPane);								
		add(inputTextField);
		add(sendButton);
		add(callButton);
		add(endButton);
		
		/*
		 * 3. Linking the buttons to the ActionListener
		 */
		sendButton.addActionListener(this);			
		callButton.addActionListener(this);	
		endButton.addActionListener(this);

		
	}
	
	/**
	 * The main method of the application. It continuously listens for
	 * new messages.
	 */
	public static void main(String[] args){
	
		/*
		 * 1. Create the app's window
		 */
		App app = new App("CN2 - AUTH");  // TODO: You can add the title that will displayed on the Window of the App here																		  
		app.setSize(500,250);				  
		app.setVisible(true);				  

		/*
		 * 2. 
		 */
		do{	
			//Receive messages
			new Thread(new Runnable() {
		        public void run() {
		        	try (ServerSocket serverSocket = new ServerSocket(5002)) {
		                textArea.append("Server is listening on port 5002...\n");
		                while (true) {
		                    Socket receiveSocket = serverSocket.accept();
		                    BufferedReader reader = new BufferedReader(new InputStreamReader(receiveSocket.getInputStream()));

		                    String receivedMessage;
		                    while ((receivedMessage = reader.readLine()) != null) {
		                        textArea.append("Received: " + receivedMessage + newline);
		                    }

		                    receiveSocket.close();
		                }
		            } catch (IOException ex) {
		                ex.printStackTrace();
		                textArea.append("Error: Unable to receive messages.\n");
		            }
		        }
		    }).start();
		}while(true);
	}
	
	/**
	 * The method that corresponds to the Action Listener. Whenever an action is performed
	 * (i.e., one of the buttons is clicked) this method is executed. 
	 */
	@Override
	public void actionPerformed(ActionEvent e) {
		
	

		/*
		 * Check which button was clicked.
		 */
		if (e.getSource() == sendButton){
			
			String serverAddress = "127.0.0.1";
		    int port = 5005;

		    try (Socket socket = new Socket(serverAddress, port);
		            OutputStream out = socket.getOutputStream();
		            PrintWriter writer = new PrintWriter(out, true)) {

		           String message = inputTextField.getText();
		           textArea.append("Sent: " + message + newline);

		           writer.println(message); // Send the message over the TCP connection

		       } catch (IOException ex) {
		           ex.printStackTrace();
		           textArea.append("Error: Unable to send message\n");
		       }
			
		}else if(e.getSource() == callButton){
			
			// The "Call" button was clicked
			
			try {
			    call_socket = new DatagramSocket();
			    AudioFormat audio_format = new AudioFormat(8000, 16, 1, true, true); 
			    DataLine.Info audio_info = new DataLine.Info(TargetDataLine.class, audio_format);
			    DataLine.Info source_info = new DataLine.Info(SourceDataLine.class, audio_format);

			    getsound = (TargetDataLine) AudioSystem.getLine(audio_info);
			    getsound.open(audio_format);
			    getsound.start();

			    hearsound = (SourceDataLine) AudioSystem.getLine(source_info);
			    hearsound.open(audio_format);
			    hearsound.start();

			    captureThread = new Thread(() -> {
			        try {
			            byte[] audio_buffer = new byte[4096]; 
			            InetAddress call_address = InetAddress.getByName("127.0.0.1"); 
			            int port = 5001;

			            while (!Thread.currentThread().isInterrupted()) {
			                int bytes_read = getsound.read(audio_buffer, 0, audio_buffer.length);
			                DatagramPacket call_packet = new DatagramPacket(audio_buffer, bytes_read, call_address, port);
			                call_socket.send(call_packet);
			            }
			        } catch (Exception ex) {
			            ex.printStackTrace();
			        }
			    });

			    receiveThread = new Thread(() -> {
			        try {
			            DatagramSocket receive_socket = new DatagramSocket(5001); // Ensure it's bound to the correct port
			            byte[] receive_buffer = new byte[4096];
			            DatagramPacket receive_packet = new DatagramPacket(receive_buffer, receive_buffer.length);

			            while (!Thread.currentThread().isInterrupted()) {
			                receive_socket.receive(receive_packet);
			                hearsound.write(receive_packet.getData(), 0, receive_packet.getLength());
			            }
			        } catch (Exception ex) {
			            ex.printStackTrace();
			        }
			    });


			    captureThread.start();
			    receiveThread.start();

			} catch (LineUnavailableException | IOException ex) {
			    ex.printStackTrace();
			}
			
		} else if (e.getSource() == endButton) {
	            // The "End Call" button was clicked

	            try {
	                if (captureThread != null && captureThread.isAlive()) {
	                    captureThread.interrupt();
	                }
	                if (receiveThread != null && receiveThread.isAlive()) {
	                    receiveThread.interrupt();
	                }
	                if (getsound != null) {
	                    getsound.stop();
	                    getsound.close();
	                }
	                if (hearsound != null) {
	                    hearsound.stop();
	                    hearsound.close();
	                }
	                if (call_socket != null && !call_socket.isClosed()) {
	                    call_socket.close();
	                }
	                textArea.append("Call ended" + newline);
	            } catch (Exception ex) {
	                ex.printStackTrace();
	            }
	        }
			

	}

	/**
	 * These methods have to do with the GUI. You can use them if you wish to define
	 * what the program should do in specific scenarios (e.g., when closing the 
	 * window).
	 */
	@Override
	public void windowActivated(WindowEvent e) {
		// TODO Auto-generated method stub	
	}

	@Override
	public void windowClosed(WindowEvent e) {
		// TODO Auto-generated method stub	
	}

	@Override
	public void windowClosing(WindowEvent e) {
		// TODO Auto-generated method stub
		dispose();
        	System.exit(0);
	}

	@Override
	public void windowDeactivated(WindowEvent e) {
		// TODO Auto-generated method stub	
	}

	@Override
	public void windowDeiconified(WindowEvent e) {
		// TODO Auto-generated method stub	
	}

	@Override
	public void windowIconified(WindowEvent e) {
		// TODO Auto-generated method stub	
	}

	@Override
	public void windowOpened(WindowEvent e) {
		// TODO Auto-generated method stub	
	}
}
