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
    private Socket call_socket;
    private OutputStream out;
    private PrintWriter writer;
    private TargetDataLine getsound;
    private SourceDataLine hearsound;
	
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
			new Thread(() -> {
		        	try (ServerSocket serverSocket = new ServerSocket(5002)) {
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
		    }).start();
	}
	
	/**
	 * The method that corresponds to the Action Listener. Whenever an action is performed
	 * (i.e., one of the buttons is clicked) this method is executed. 
	 */
	@Override
	public void actionPerformed(ActionEvent e) {
		
		String serverAddress = "127.0.0.1";

		/*
		 * Check which button was clicked.
		 */
		if (e.getSource() == sendButton){
			
		    int port = 5002;

		    try (Socket send_socket = new Socket(serverAddress, port);
		            OutputStream out = send_socket.getOutputStream();
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
			
			int port = 5001;
			
			try{
				
				call_socket = new Socket (serverAddress, port);
				out = call_socket.getOutputStream();
				writer = new PrintWriter(out, true);
				
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
			            while (!Thread.currentThread().isInterrupted()) {
			                int bytes_read = getsound.read(audio_buffer, 0, audio_buffer.length);
			                out.write(audio_buffer, 0, bytes_read);
			            }
			        } catch (Exception ex) {
			            ex.printStackTrace();
			        }
			    });

			    receiveThread = new Thread(() -> {
			        try {
			        	InputStream in = call_socket.getInputStream();
			            byte[] receive_buffer = new byte[4096];
			            int bytesRead;
			            while (!Thread.currentThread().isInterrupted()) {
			            	bytesRead = in.read(receive_buffer);
			            	hearsound.write(receive_buffer, 0, bytesRead);
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
	            	// Stop the capture thread if it is running
			        if (captureThread != null && captureThread.isAlive()) {
			            captureThread.interrupt();
			        }
			        // Stop the receive thread if it is running
			        if (receiveThread != null && receiveThread.isAlive()) {
			            receiveThread.interrupt();
			        }
			        // Stop and close the audio capture line
			        if (getsound != null) {
			            getsound.stop();
			            getsound.close(); 
			        }
			        // Stop and close the audio playback line
			        if (hearsound != null) {
			            hearsound.stop();
			            hearsound.close(); 
			        }
			        // Close the call socket if it is open
			        if (call_socket != null && !call_socket.isClosed()) {
			            call_socket.close();
			        }
			        textArea.append("Call ended" + newline); // Log the call end in the user interface

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
